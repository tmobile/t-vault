// =========================================================================
// Copyright 2019 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

package com.tmobile.cso.vault.api.service;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.DirectoryGroup;
import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryObjectsList;
import com.tmobile.cso.vault.api.model.DirectoryUser;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class  DirectoryService {

	@Value("${vault.port}")
	private String vaultPort;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger log = LogManager.getLogger(DirectoryService.class);

	@Autowired
	private LdapTemplate ldapTemplate;

	@Autowired
	@Qualifier(value = "adUserLdapTemplate")
	private LdapTemplate adUserLdapTemplate;

	/**
	 * Gets the list of users from Directory Server based on UPN
	 * @param UserPrincipalName
	 * @return
	 */
	public ResponseEntity<DirectoryObjects> searchByUPN(String UserPrincipalName) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new LikeFilter("userPrincipalName", UserPrincipalName+"*"));
		andFilter.and(new EqualsFilter("objectClass", "user"));

		List<DirectoryUser> allPersons = getAllPersons(andFilter);
		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(allPersons.toArray(new DirectoryUser[allPersons.size()]));
		users.setData(usersList);
		return ResponseEntity.status(HttpStatus.OK).body(users);
	}
	
	/**
	 * 
	 * @param UserPrincipalName
	 * @return
	 */
	public ResponseEntity<DirectoryObjects> searchByCorpId(String corpId) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new LikeFilter("cn", corpId+"*"));
		andFilter.and(new EqualsFilter("objectClass", "user"));

		List<DirectoryUser> allPersons = getAllPersons(andFilter);
		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(allPersons.toArray(new DirectoryUser[allPersons.size()]));
		users.setData(usersList);
		return ResponseEntity.status(HttpStatus.OK).body(users);
	}
	
	
	/**
	 * Get userDetails By CorpID
	 * @param corpId
	 * @return
	 */
	public DirectoryUser getUserDetailsByCorpId(String corpId) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new EqualsFilter("cn", corpId));
		andFilter.and(new EqualsFilter("objectClass", "user"));

		List<DirectoryUser> allPersons = getAllPersons(andFilter);
		if(CollectionUtils.isEmpty(allPersons)){
			return new DirectoryUser();
		}
		return allPersons.get(0);
	}

	/**
	 * Gets the list of users from Directory Server
	 * @param filter
	 * @return
	 */
	private List<DirectoryUser> getAllPersons(Filter filter) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "GetAllUsers").
				put(LogMessage.MESSAGE, String.format("Trying to get list of users from directory server")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ldapTemplate.search("", filter.encode(), new AttributesMapper<DirectoryUser>() {
			@Override
			public DirectoryUser mapFromAttributes(Attributes attr) throws NamingException {
				DirectoryUser person = new DirectoryUser();
				if (attr != null) {
					String mail = ""; 
					if(attr.get("mail") != null) {
						mail = ((String) attr.get("mail").get());
					}
					String userId = ((String) attr.get("name").get());
					// Assign first part of the email id for use with UPN authentication
					if (!StringUtils.isEmpty(mail)) {
						userId = mail.substring(0, mail.indexOf("@"));
					}
					person.setUserId(userId);
					if (attr.get("displayname") != null) {
						person.setDisplayName(((String) attr.get("displayname").get()));
					}
					if (attr.get("givenname") != null) {
						person.setGivenName(((String) attr.get("givenname").get()));
					}

					if (attr.get("mail") != null) {
						person.setUserEmail(((String) attr.get("mail").get()));
					}

					if (attr.get("name") != null) {
						person.setUserName(((String) attr.get("name").get()));
					}
				}
				return person;
			}
		});
	}

	/**
	 * Gets the list of LDAP groups 
	 * @param groupName
	 * @return
	 */
	public ResponseEntity<DirectoryObjects> searchByGroupName(String groupName) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new EqualsFilter("objectClass", "group"));
		andFilter.and(new LikeFilter("CN", groupName+"*"));
		List<DirectoryGroup> allGroups = getAllGroups(andFilter);
		DirectoryObjects groups = new DirectoryObjects();
		DirectoryObjectsList groupsList = new DirectoryObjectsList();
		groupsList.setValues(allGroups.toArray(new DirectoryGroup[allGroups.size()]));
		groups.setData(groupsList);
		return ResponseEntity.status(HttpStatus.OK).body(groups);
		
	}

	/**
	 * Get the list of groups
	 * @param filter
	 * @return
	 */
	private List<DirectoryGroup> getAllGroups(Filter filter) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "GetAllGroups").
				put(LogMessage.MESSAGE, String.format("Trying to get list of groups from directory server")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ldapTemplate.search("", filter.encode(), new AttributesMapper<DirectoryGroup>() {
			@Override
			public DirectoryGroup mapFromAttributes(Attributes attr) throws NamingException {
				DirectoryGroup dirGrp = new DirectoryGroup();
				if (attr.get("name") != null) {
					dirGrp.setGroupName(((String) attr.get("name").get()));
				}
				if (attr.get("displayname") != null) {
					dirGrp.setDisplayName(((String) attr.get("displayname").get()));
				}
				if (attr.get("mail") != null) {
					dirGrp.setEmail(((String) attr.get("mail").get()));
				}
				return dirGrp;
			}
		});
	}

	/**
	 * Method to query ladp with displayName.
	 * @param displayName
	 * @return
	 */
	public DirectoryObjects searchBydisplayName(String displayName) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new LikeFilter("displayName", displayName+"*"));
		andFilter.and(new EqualsFilter("objectClass", "user"));

		List<DirectoryUser> allPersons = getAllPersons(andFilter);
		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(allPersons.toArray(new DirectoryUser[allPersons.size()]));
		users.setData(usersList);
		return users;
	}

	public DirectoryObjects searchByNTId(String ntId) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new LikeFilter("cn", ntId+"*"));
		andFilter.and(new EqualsFilter("objectClass", "user"));

		List<DirectoryUser> allPersons = getAllPersons(andFilter);
		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(allPersons.toArray(new DirectoryUser[allPersons.size()]));
		users.setData(usersList);
		return users;
	}

	/**
	 * Search the LDAP with displayName.
	 * @param ntId
	 * @return
	 */
	public ResponseEntity<DirectoryObjects> searchByDisplayNameAndId(String ntId) {
		DirectoryObjects objectsByDisplayName = searchBydisplayName(ntId);
		if (objectsByDisplayName.getData().getValues().length > 0) {
			return ResponseEntity.status(HttpStatus.OK).body(objectsByDisplayName);
		}

		DirectoryObjects objectsByCN = searchByNTId(ntId);
		return ResponseEntity.status(HttpStatus.OK).body(objectsByCN);
	}

	/**
	 * Method to gets the list of users from Directory Server by ntIds
	 *
	 * @param ntIds
	 * @return
	 */
	public ResponseEntity<DirectoryObjects> getAllUsersDetailByNtIds(String ntIds) {
		AndFilter andFilter = new AndFilter();
		if (!StringUtils.isEmpty(ntIds)) {
			OrFilter orFilter = new OrFilter();
			String[] userNtIds = ntIds.split(",");
			andFilter.and(new EqualsFilter("objectClass", "user"));
			for (String ntId : userNtIds) {
				orFilter.or(new EqualsFilter("CN", ntId.trim()));
			}
			andFilter.and(orFilter);
		}
		List<DirectoryUser> allPersons = getAllPersons(andFilter);
		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(allPersons.toArray(new DirectoryUser[allPersons.size()]));
		users.setData(usersList);

		return ResponseEntity.status(HttpStatus.OK).body(users);
	}

	/**
	 * Gets the user from CORD AD Server based on email
	 * @param email
	 * @return
	 */
	public ResponseEntity<DirectoryObjects> searchByEmailInCorp(String email) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new EqualsFilter("mail", email));
		andFilter.and(new EqualsFilter("objectClass", "user"));

		List<DirectoryUser> allPersons = getAllPersonsFromCorp(andFilter);
		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(allPersons.toArray(new DirectoryUser[allPersons.size()]));
		users.setData(usersList);
		return ResponseEntity.status(HttpStatus.OK).body(users);
	}

	/**
	 * Gets the list of users from CORP AD
	 * @param filter
	 * @return
	 */
	private List<DirectoryUser> getAllPersonsFromCorp(Filter filter) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "GetAllUsers").
				put(LogMessage.MESSAGE, String.format("Trying to get list of users from directory server")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return adUserLdapTemplate.search("", filter.encode(), new AttributesMapper<DirectoryUser>() {
			@Override
			public DirectoryUser mapFromAttributes(Attributes attr) throws NamingException {
				DirectoryUser person = new DirectoryUser();
				if (attr != null) {
					String mail = "";
					if(attr.get("mail") != null) {
						mail = ((String) attr.get("mail").get());
					}
					String userId = ((String) attr.get("name").get());
					// Assign first part of the email id for use with UPN authentication
					if (!StringUtils.isEmpty(mail)) {
						userId = mail.substring(0, mail.indexOf("@"));
					}
					person.setUserId(userId);
					if (attr.get("displayname") != null) {
						person.setDisplayName(((String) attr.get("displayname").get()));
					}
					if (attr.get("givenname") != null) {
						person.setGivenName(((String) attr.get("givenname").get()));
					}

					if (attr.get("mail") != null) {
						person.setUserEmail(((String) attr.get("mail").get()));
					}

					if (attr.get("name") != null) {
						person.setUserName(((String) attr.get("name").get()));
					}
				}
				return person;
			}
		});
	}
}
