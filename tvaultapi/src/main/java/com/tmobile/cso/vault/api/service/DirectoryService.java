// =========================================================================
// Copyright 2019 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License")
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

import java.util.ArrayList;
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
	
	private static final String OBJCLASS = "objectClass";
	private static final String DISPLAYNAME = "displayname";
	private static final String GIVENNAME = "givenname";
	private static final String GETNTIDFORUSER = "getNtidForUser";

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
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

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
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

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
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

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
				put(LogMessage.MESSAGE, "Trying to get list of users from directory server").
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
					if (attr.get(DISPLAYNAME) != null) {
						person.setDisplayName(((String) attr.get(DISPLAYNAME).get()));
					}
					if (attr.get(GIVENNAME) != null) {
						person.setGivenName(((String) attr.get(GIVENNAME).get()));
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
		andFilter.and(new EqualsFilter(OBJCLASS, "group"));
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
				put(LogMessage.MESSAGE, "Trying to get list of groups from directory server").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ldapTemplate.search("", filter.encode(), new AttributesMapper<DirectoryGroup>() {
			@Override
			public DirectoryGroup mapFromAttributes(Attributes attr) throws NamingException {
				DirectoryGroup dirGrp = new DirectoryGroup();
				if (attr.get("name") != null) {
					dirGrp.setGroupName(((String) attr.get("name").get()));
				}
				if (attr.get(DISPLAYNAME) != null) {
					dirGrp.setDisplayName(((String) attr.get(DISPLAYNAME).get()));
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
		andFilter.and(new LikeFilter(DISPLAYNAME, displayName+"*"));
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

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
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

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
			andFilter.and(new EqualsFilter(OBJCLASS, "user"));
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
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

		List<DirectoryUser> allPersons = getAllPersonsFromCorp(andFilter);
		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(allPersons.toArray(new DirectoryUser[allPersons.size()]));
		users.setData(usersList);
		return ResponseEntity.status(HttpStatus.OK).body(users);
	}

	/**
	 * Gets the user from CORD AD Server based on NTid
	 * @param userName
	 * @return
	 */
	public DirectoryUser getUserDetailsFromCorp(String userName) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new EqualsFilter("cn", userName));
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

		List<DirectoryUser> allPersons = getAllPersonsFromCorp(andFilter);
		if(CollectionUtils.isEmpty(allPersons)){
			return new DirectoryUser();
		}
		return allPersons.get(0);
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
				put(LogMessage.MESSAGE, "Trying to get list of users from directory server").
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
					if (attr.get(DISPLAYNAME) != null) {
						person.setDisplayName(((String) attr.get(DISPLAYNAME).get()));
					}
					if (attr.get(GIVENNAME) != null) {
						person.setGivenName(((String) attr.get(GIVENNAME).get()));
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
	 * Get Ntid for a user from email address.
	 * @param email
	 * @return
	 */
	public String getNtidForUser(String email) {
		String ntid = null;
		// Get NT id for given email from GSM
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, GETNTIDFORUSER).
				put(LogMessage.MESSAGE, String.format("Trying to get NT id from GSM for [%s]", email)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		ResponseEntity<DirectoryObjects> directoryObjectsResponseEntity = searchByUPN(email);
		if (directoryObjectsResponseEntity != null && HttpStatus.OK.equals(directoryObjectsResponseEntity.getStatusCode())) {
			try {
				Object[] adUser = directoryObjectsResponseEntity.getBody().getData().getValues();
				if (adUser.length > 0) {
					DirectoryUser directoryUser = (DirectoryUser) adUser[0];
					ntid = directoryUser.getUserName().toLowerCase();
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, GETNTIDFORUSER).
							put(LogMessage.MESSAGE, String.format("Owner id from GSM for [%s] is [%s]", email, ntid)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
			} catch (NullPointerException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, GETNTIDFORUSER).
						put(LogMessage.MESSAGE, "Failed to extract NTid from gsm response").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}

		// Get NT id for given email from Corp if not found in GSM
		if (org.apache.commons.lang3.StringUtils.isEmpty(ntid)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, GETNTIDFORUSER).
					put(LogMessage.MESSAGE, String.format("Trying to get NT id from corp AD for [%s]", email)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			directoryObjectsResponseEntity = searchByEmailInCorp(email);
			if (directoryObjectsResponseEntity != null && HttpStatus.OK.equals(directoryObjectsResponseEntity.getStatusCode())) {
				try {
					Object[] adUser = directoryObjectsResponseEntity.getBody().getData().getValues();
					if (adUser.length > 0) {
						DirectoryUser directoryUser = (DirectoryUser) adUser[0];
						ntid = directoryUser.getUserName().toLowerCase();
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, GETNTIDFORUSER).
								put(LogMessage.MESSAGE, String.format("Owner id from Corp AD for [%s] is [%s]", email, ntid)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
					}
				} catch (NullPointerException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "getNtidForUser5").
							put(LogMessage.MESSAGE, "Failed to extract NTid from corp response").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
			}
		}
		return ntid;
	}

	/**
	 * To search email in GSM or Corp
	 * @param UserPrincipalName
	 * @return
	 */
	public ResponseEntity<DirectoryObjects> searchByUPNInGsmAndCorp(String UserPrincipalName) {

		AndFilter andFilter = new AndFilter();
		andFilter.and(new LikeFilter("userPrincipalName", UserPrincipalName+"*"));
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));

		List<DirectoryUser> allPersons = getAllPersons(andFilter);

		andFilter = new AndFilter();
		andFilter.and(new LikeFilter("mail", UserPrincipalName+"*"));
		andFilter.and(new EqualsFilter(OBJCLASS, "user"));


		List<DirectoryUser> allPersonsFromCorp = getAllPersonsFromCorp(andFilter);
		List<DirectoryUser> filterdList = new ArrayList<>();
		filterdList.addAll(allPersons);

		if (!CollectionUtils.isEmpty(allPersonsFromCorp)) {
			for (int i=0;i< allPersonsFromCorp.size();i++) {
				DirectoryUser corpUser = allPersonsFromCorp.get(i);
				boolean isDuplicate = false;
				for (int j=0;j< allPersons.size();j++) {
					DirectoryUser user = allPersons.get(j);
					if (corpUser.getUserEmail().equalsIgnoreCase(user.getUserEmail())) {
						isDuplicate = true;
						break;
					}
				}
				if (!isDuplicate) {
					filterdList.add(corpUser);
				}
			}
		}

		DirectoryObjects users = new DirectoryObjects();
		DirectoryObjectsList usersList = new DirectoryObjectsList();
		usersList.setValues(filterdList.toArray(new DirectoryUser[filterdList.size()]));
		users.setData(usersList);
		return ResponseEntity.status(HttpStatus.OK).body(users);
	}
}
