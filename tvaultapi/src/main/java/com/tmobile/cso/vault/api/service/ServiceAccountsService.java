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

import java.io.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

@Component
public class  ServiceAccountsService {

	@Value("${vault.port}")
	private String vaultPort;

	@Value("${ad.notification.fromemail}")
	private String supportEmail;

	@Value("${ad.notification.mail.subject}")
	private String subject;

	@Value("${ad.notification.mail.body.groupcontent}")
	private String mailAdGroupContent;

	private static Logger log = LogManager.getLogger(ServiceAccountsService.class);
	private final static String[] permissions = {"read", "reset", "deny", "sudo"};

	@Autowired
	@Qualifier(value = "svcAccLdapTemplate")
	private LdapTemplate ldapTemplate;

	@Autowired
	@Qualifier(value = "adUserLdapTemplate")
	private LdapTemplate adUserLdapTemplate;

	@Autowired
	private AccessService accessService;

	@Autowired
	private RequestProcessor reqProcessor;

	@Autowired
	private AppRoleService appRoleService;

	@Autowired
	private AWSAuthService awsAuthService;

	@Autowired
	private AWSIAMAuthService awsiamAuthService;

	@Autowired
	private PolicyUtils policyUtils;

	@Value("${ad.svc.acc.suffix:clouddev.corporate.t-mobile.com}")
	private String serviceAccountSuffix;
	
	@Value("${vault.auth.method}")
	private String vaultAuthMethod;
	
	@Value("${ad.username}")
	private String adMasterServiveAccount;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
	private EmailUtils emailUtils;
    
    @Autowired
	private OIDCUtil oidcUtil;
	/**
	 * Gets the list of users from Directory Server based on UPN
	 * @param UserPrincipalName
	 * @return
	 */
	public ResponseEntity<ADServiceAccountObjects> getADServiceAccounts(String token, UserDetails userDetails, String UserPrincipalName, boolean excludeOnboarded) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new LikeFilter("userPrincipalName", UserPrincipalName+"*"));
		andFilter.and(new EqualsFilter("objectClass", "user"));
		andFilter.and(new NotFilter(new EqualsFilter("CN", adMasterServiveAccount)));
		if (excludeOnboarded) {
			ResponseEntity<String> responseEntity = getOnboardedServiceAccounts(token, userDetails);
			if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
				String response = responseEntity.getBody();
				List<String> onboardedSvcAccs = new ArrayList<String>();
				try {
					Map<String, Object> requestParams = new ObjectMapper().readValue(response, new TypeReference<Map<String, Object>>(){});
					onboardedSvcAccs = (ArrayList<String>) requestParams.get("keys");
				}
				catch(Exception ex) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "getADServiceAccounts").
							put(LogMessage.MESSAGE, String.format("There are no service accounts currently onboarded or error in retrieving onboarded service accounts")).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
				for (String onboardedSvcAcc: onboardedSvcAccs) {
					andFilter.and(new NotFilter(new EqualsFilter("CN", onboardedSvcAcc)));
				}
			}
		}
		List<ADServiceAccount> allServiceAccounts = getADServiceAccounts(andFilter);
		// get the managed_by details
		if (allServiceAccounts != null && !allServiceAccounts.isEmpty()) {
			List<String> ownerlist = allServiceAccounts.stream().map(m -> m.getManagedBy().getUserName()).collect(Collectors.toList());
			// remove duplicate usernames
			ownerlist = new ArrayList<>(new HashSet<>(ownerlist));

			// remove empty manager names if any
            ownerlist.removeAll(Collections.singleton(TVaultConstants.EMPTY));
            ownerlist.removeAll(Collections.singleton(null));

			// build the search query
			if (!ownerlist.isEmpty()) {
				StringBuffer filterQuery = new StringBuffer();
				filterQuery.append("(&(objectclass=user)(|");
				for (String owner : ownerlist) {
					filterQuery.append("(cn=" + owner + ")");
				}
				filterQuery.append("))");
				List<ADUserAccount> managedServiceAccounts = getServiceAccountManagerDetails(filterQuery.toString());

				// Update the managedBy withe ADUserAccount object
				for (ADServiceAccount adServiceAccount : allServiceAccounts) {
					if (!StringUtils.isEmpty(adServiceAccount.getManagedBy().getUserName())) {
						List<ADUserAccount> adUserAccount = managedServiceAccounts.stream().filter(f -> (f.getUserName()!=null && f.getUserName().equalsIgnoreCase(adServiceAccount.getManagedBy().getUserName()))).collect(Collectors.toList());
						if (!adUserAccount.isEmpty()) {
							adServiceAccount.setManagedBy(adUserAccount.get(0));
						}
					}
				}
			}
		}
		ADServiceAccountObjects adServiceAccountObjects = new ADServiceAccountObjects();
		ADServiceAccountObjectsList adServiceAccountObjectsList = new ADServiceAccountObjectsList();
		Object[] values = new Object[] {};
		if (allServiceAccounts !=null && !CollectionUtils.isEmpty(allServiceAccounts)) {
			values = allServiceAccounts.toArray(new ADServiceAccount[allServiceAccounts.size()]);
		}
		adServiceAccountObjectsList.setValues(values);
		adServiceAccountObjects.setData(adServiceAccountObjectsList);
		return ResponseEntity.status(HttpStatus.OK).body(adServiceAccountObjects);
	}

	/**
	 * Gets the list of ADAccounts from AD Server
	 * @param filter
	 * @return
	 */
	private List<ADServiceAccount> getADServiceAccounts(Filter filter) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "getAllAccounts").
				put(LogMessage.MESSAGE, String.format("Trying to get list of user accounts from AD server")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ldapTemplate.search("", filter.encode(), new AttributesMapper<ADServiceAccount>() {
			@Override
			public ADServiceAccount mapFromAttributes(Attributes attr) throws NamingException {
				ADServiceAccount adServiceAccount = new ADServiceAccount();
				if (attr != null) {
					String userId = ((String) attr.get("name").get());
					adServiceAccount.setUserId(userId);
					if (attr.get("displayname") != null) {
						adServiceAccount.setDisplayName(((String) attr.get("displayname").get()));
					}
					if (attr.get("givenname") != null) {
						adServiceAccount.setGivenName(((String) attr.get("givenname").get()));
					}

					if (attr.get("mail") != null) {
						adServiceAccount.setUserEmail(((String) attr.get("mail").get()));
					}
					if (attr.get("name") != null) {
						adServiceAccount.setUserName((String) attr.get("name").get());
					}
					if (attr.get("whenCreated") != null) {
						String rawDateTime = (String) attr.get("whenCreated").get();
						DateTimeFormatter fmt = DateTimeFormatter.ofPattern ( "uuuuMMddHHmmss[,S][.S]X" );
						OffsetDateTime odt = OffsetDateTime.parse (rawDateTime, fmt);
						Instant instant = odt.toInstant();
						adServiceAccount.setWhenCreated(instant);
					}
					ADUserAccount adUserAccount = new ADUserAccount();
					adServiceAccount.setManagedBy(adUserAccount);
					adServiceAccount.setOwner(null);
					if (attr.get("manager") != null) {
						String managedBy = "";
						String managedByStr = (String) attr.get("manager").get();
						if (!StringUtils.isEmpty(managedByStr)) {
							managedBy= managedByStr.substring(3, managedByStr.indexOf(","));
						}
						adUserAccount.setUserName(managedBy);
						adServiceAccount.setOwner(managedBy.toLowerCase());
					}
					if (attr.get("accountExpires") != null) {
						String rawExpDateTime = (String) attr.get("accountExpires").get();
						adServiceAccount.setAccountExpires(rawExpDateTime);
					}
					if (attr.get("pwdLastSet") != null) {
						String pwdLastSet = (String) attr.get("pwdLastSet").get();
						adServiceAccount.setPwdLastSet(pwdLastSet);
					}
					if (attr.get("memberof") != null) {
						String memberof = (String) attr.get("memberof").get();
						adServiceAccount.setMemberOf(memberof);
					}

					if (attr.get("lockedout") != null) {
						String memberof = (String) attr.get("lockedout").get();
						adServiceAccount.setMemberOf(memberof);
					}
					// lock status
					adServiceAccount.setLockStatus("unlocked");
					if (attr.get("lockedout") != null) {
						boolean lockedOut = (boolean) attr.get("lockedout").get();
						if (lockedOut) {
							adServiceAccount.setLockStatus("locked");
						}
					}
					if (attr.get("description") != null) {
						adServiceAccount.setPurpose((String) attr.get("description").get());
					}
				}
				return adServiceAccount;
			}
		});
	}

	/**
	 * Onboards an AD service account into TVault for password rotation
	 * @param serviceAccount
	 * @return
	 */
	public ResponseEntity<String> onboardServiceAccount(String token, ServiceAccount serviceAccount, UserDetails userDetails) {
		List<String> onboardedList = getOnboardedServiceAccountList(token, userDetails);
		if (onboardedList.contains(serviceAccount.getName())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "onboardServiceAccount").
					put(LogMessage.MESSAGE, "Failed to onboard Service Account. Service account is already onboarded").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to onboard Service Account. Service account is already onboarded\"]}");
		}
        // get the maxPwdAge for this service account
        List<ADServiceAccount> allServiceAccounts = getADServiceAccount(serviceAccount.getName());
        if (allServiceAccounts == null || allServiceAccounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to onboard Service Account. Unable to read Service account details\"]}");
        }
        if (TVaultConstants.EXPIRED.toLowerCase().equals(allServiceAccounts.get(0).getAccountStatus().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to onboard Service Account. Service account expired\"]}");
        }
        int maxPwdAge = allServiceAccounts.get(0).getMaxPwdAge();
		serviceAccount.setOwner(allServiceAccounts.get(0).getOwner());
		if (serviceAccount.isAutoRotate()) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "onboardServiceAccount").
					put(LogMessage.MESSAGE, String.format ("Auto-Rotate of password has been turned on")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
            if (null == serviceAccount.getMax_ttl()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid or no value has been provided for MAX_TTL\"]}");
            }
            if (null == serviceAccount.getTtl()) {
				serviceAccount.setTtl(maxPwdAge - 1L);
			}
			if (serviceAccount.getTtl() > maxPwdAge) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "onboardServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Password Expiration Time [%s] is greater the Maximum expiration time (MAX_TTL) [%s]", serviceAccount.getTtl(), maxPwdAge)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value provided for Password Expiration Time. This can't be more than "+maxPwdAge+" for this Service Account\"]}");
            }
			if (serviceAccount.getTtl() > serviceAccount.getMax_ttl()) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "onboardServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Password Expiration Time [%s] is greater the Maximum expiration time (MAX_TTL) [%s]", serviceAccount.getTtl(), serviceAccount.getMax_ttl())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Password Expiration Time can't be more than Maximum expiration time (MAX_TTL) for this Service Account\"]}");
			}
		}
		else {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "onboardServiceAccount").
					put(LogMessage.MESSAGE, String.format ("Auto-Rotate of password has been turned off")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			// ttl defaults to configuration ttl
			serviceAccount.setTtl(TVaultConstants.MAX_TTL);
		}
		ResponseEntity<String> accountRoleCreationResponse = createAccountRole(token, serviceAccount);
		if(accountRoleCreationResponse.getStatusCode().equals(HttpStatus.OK)) {
			// Create Metadata
			ResponseEntity<String> metadataCreationResponse = createMetadata(token, serviceAccount);
			if (HttpStatus.OK.equals(metadataCreationResponse.getStatusCode())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "onboardServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Successfully created Metadata for the Service Account")).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "onboardServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Successfully created Service Account Role. However creation of Metadata failed.")).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"errors\":[\"Successfully created Service Account Role. However creation of Metadata failed.\"]}");
			}
			String svcAccName = serviceAccount.getName();
			ResponseEntity<String> svcAccPolicyCreationResponse = createServiceAccountPolicies(token, svcAccName);
			if (HttpStatus.OK.equals(svcAccPolicyCreationResponse.getStatusCode())) {
				ServiceAccountUser serviceAccountUser = new ServiceAccountUser(svcAccName, serviceAccount.getOwner(), TVaultConstants.SUDO_POLICY);
				ResponseEntity<String> addUserToServiceAccountResponse = addUserToServiceAccount(token, serviceAccountUser, userDetails, true);
				if (HttpStatus.OK.equals(addUserToServiceAccountResponse.getStatusCode())) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "onboardServiceAccount").
							put(LogMessage.MESSAGE, String.format ("Successfully completed onboarding of AD service account into TVault for password rotation.")).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));

					// send email notification to service account owner
					// get service account owner email
					String filterQuery = "(&(objectclass=user)(|(cn=" + serviceAccount.getOwner() + ")))";
					List<ADUserAccount> managerDetails = getServiceAccountManagerDetails(filterQuery);
					if (!managerDetails.isEmpty() && !StringUtils.isEmpty(managerDetails.get(0).getUserEmail())) {
						String from = supportEmail;
						List<String> to = new ArrayList<>();
						to.add(managerDetails.get(0).getUserEmail());
						String mailSubject = String.format(subject, svcAccName);
						String groupContent = TVaultConstants.EMPTY;

						// set template variables
						Map<String, String> mailTemplateVariables = new Hashtable<>();
						mailTemplateVariables.put("name", managerDetails.get(0).getDisplayName());
						mailTemplateVariables.put("svcAccName", svcAccName);
						if (serviceAccount.getAdGroup() != null && serviceAccount.getAdGroup() != "") {
							groupContent = String.format(mailAdGroupContent, serviceAccount.getAdGroup());
						}
						mailTemplateVariables.put("groupContent", groupContent);
						mailTemplateVariables.put("contactLink", supportEmail);
						emailUtils.sendHtmlEmalFromTemplate(from, to, mailSubject, mailTemplateVariables);
					}
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully completed onboarding of AD service account into TVault for password rotation.\"]}");
				}
				else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "onboardServiceAccount").
							put(LogMessage.MESSAGE, String.format ("Successfully created Service Account Role and policies. However the association of owner information failed.")).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Successfully created Service Account Role and policies. However the association of owner information failed.\"]}");
				}
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "onboardServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Failed to onboard AD service account into TVault for password rotation.")).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				OnboardedServiceAccount serviceAccountToRevert = new OnboardedServiceAccount(serviceAccount.getName(),serviceAccount.getOwner());
				ResponseEntity<String> accountRoleDeletionResponse = deleteAccountRole(token, serviceAccountToRevert);
				if (accountRoleDeletionResponse!=null && HttpStatus.OK.equals(accountRoleDeletionResponse.getStatusCode())) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to onboard AD service account into TVault for password rotation.\"]}");
				} else {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to create Service Account policies. Revert service account role creation failed.\"]}");
				}
			}
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "onboardServiceAccount").
					put(LogMessage.MESSAGE, String.format ("Failed to onboard AD service account into TVault for password rotation.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to onboard AD service account into TVault for password rotation.\"]}");
		}
	}

    /**
     * Get the AD details for a given service account
     * @param serviceAccount
     * @return
     */
    private List<ADServiceAccount> getADServiceAccount(String serviceAccount) {
        AndFilter andFilter = new AndFilter();
        andFilter.and(new LikeFilter("userPrincipalName", serviceAccount+"*"));
        andFilter.and(new EqualsFilter("objectClass", "user"));
        andFilter.and(new NotFilter(new EqualsFilter("CN", adMasterServiveAccount)));
        return getADServiceAccounts(andFilter);
    }

	/**
	 * To create Metadata for the Service Account
	 * @param token
	 * @param serviceAccount
	 * @return
	 */
	private ResponseEntity<String> createMetadata(String token, ServiceAccount serviceAccount) {
		String svcAccMetaDataJson = populateSvcAccMetaJson(serviceAccount);
		boolean svcAccMetaDataCreationStatus = ControllerUtil.createMetadata(svcAccMetaDataJson, token);
		if(svcAccMetaDataCreationStatus){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createMetadata").
					put(LogMessage.MESSAGE, String.format("Successfully created metadata for the Service Account [%s]", serviceAccount.getName())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created Metadata for the Service Account\"]}");
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createAccountRole").
					put(LogMessage.MESSAGE, "Unable to create Metadata for the Service Account").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to create Metadata for the Service Account\"]}");
	}
	/**
	 * To delete metadata for the service account
	 * @param token
	 * @param serviceAccount
	 * @return
	 */
	private ResponseEntity<String> deleteMetadata(String token, OnboardedServiceAccount serviceAccount) {
		String svcAccMetaDataJson = populateSvcAccMetaJson(serviceAccount.getName(), serviceAccount.getOwner());
		Response svcAccMetaDataJsonDeletionResponse = reqProcessor.process("/delete",svcAccMetaDataJson,token);
		if(HttpStatus.OK.equals(svcAccMetaDataJsonDeletionResponse.getHttpstatus()) || HttpStatus.NO_CONTENT.equals(svcAccMetaDataJsonDeletionResponse.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "deleteMetadata").
					put(LogMessage.MESSAGE, String.format("Successfully deleted metadata for the Service Account [%s]", serviceAccount.getName())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully deleted Metadata for the Service Account\"]}");
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "deleteMetadata").
					put(LogMessage.MESSAGE, "Unable to delete Metadata for the Service Account").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to delete Metadata for the Service Account\"]}");
	}

	/**
	 * Helper to generate input JSON for Service Account metadata
	 * @param svcAccName
	 * @param username
	 * @return
	 */
	private String populateSvcAccMetaJson(String svcAccName, String username) {
		String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + svcAccName;
		ServiceAccountMetadataDetails serviceAccountMetadataDetails = new ServiceAccountMetadataDetails(svcAccName);
		serviceAccountMetadataDetails.setManagedBy(username);
		serviceAccountMetadataDetails.setInitialPasswordReset(false);
		return populateSvcAccJsonString(_path, serviceAccountMetadataDetails);
	}

	/**
	 * Helper to generate input JSON for Service Account metadata
	 * @param serviceAccount
	 * @return
	 */
	private String populateSvcAccMetaJson(ServiceAccount serviceAccount) {
		String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + serviceAccount.getName();
		ServiceAccountMetadataDetails serviceAccountMetadataDetails = new ServiceAccountMetadataDetails(serviceAccount.getName());
		serviceAccountMetadataDetails.setManagedBy(serviceAccount.getOwner());
		serviceAccountMetadataDetails.setInitialPasswordReset(false);
		serviceAccountMetadataDetails.setAdGroup(serviceAccount.getAdGroup());
		serviceAccountMetadataDetails.setAppName(serviceAccount.getAppName());
		serviceAccountMetadataDetails.setAppID(serviceAccount.getAppID());
		serviceAccountMetadataDetails.setAppTag(serviceAccount.getAppTag());
		return populateSvcAccJsonString(_path, serviceAccountMetadataDetails);
	}

	/**
	 * To generate json string for service account metadata
	 * @return
	 */
	String populateSvcAccJsonString(String path, ServiceAccountMetadataDetails serviceAccountMetadataDetails) {
		ServiceAccountMetadata serviceAccountMetadata =  new ServiceAccountMetadata(path, serviceAccountMetadataDetails);
		String jsonStr = JSONUtil.getJSON(serviceAccountMetadata);
		Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path",path);
		return ControllerUtil.convetToJson(rqstParams);
	}

	/**
	 * Offboards an AD service account from TVault for password rotation
	 * @param token
	 * @param serviceAccount
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> offboardServiceAccount(String token, OnboardedServiceAccount serviceAccount, UserDetails userDetails) {
		String managedBy = "";
		String svcAccName = serviceAccount.getName();
		ResponseEntity<String> svcAccPolicyDeletionResponse = deleteServiceAccountPolicies(token, svcAccName);
		if (!HttpStatus.OK.equals(svcAccPolicyDeletionResponse.getStatusCode())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "offboardServiceAccount").
					put(LogMessage.MESSAGE, String.format ("Failed to delete some of the policies for service account")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		// delete users,groups,aws-roles,app-roles from service account
		String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/" + svcAccName;
		Response metaResponse = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
		Map<String, Object> responseMap = null;
		try {
			responseMap = new ObjectMapper().readValue(metaResponse.getResponse(), new TypeReference<Map<String, Object>>(){});
		} catch (IOException e) {
			log.error(e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing service account info \"]}");
		}
		if(responseMap!=null && responseMap.get("data")!=null){
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			Map<String,String> awsroles = (Map<String, String>)metadataMap.get("aws-roles");
			Map<String,String> approles = (Map<String, String>)metadataMap.get("app-roles");
			Map<String,String> groups = (Map<String, String>)metadataMap.get("groups");
			Map<String,String> users = (Map<String, String>) metadataMap.get("users");
			// always add owner to the users list whose policy should be updated
			managedBy = (String) metadataMap.get("managedBy");
			if (!org.apache.commons.lang3.StringUtils.isEmpty(managedBy)) {
                if (null == users) {
                    users = new HashMap<>();
                }
				users.put(managedBy, "sudo");
			}
			updateUserPolicyAssociationOnSvcaccDelete(svcAccName, users, token, userDetails);
			updateGroupPolicyAssociationOnSvcaccDelete(svcAccName,groups,token, userDetails);
            deleteAwsRoleonOnSvcaccDelete(svcAccName,awsroles,token);
            updateApprolePolicyAssociationOnSvcaccDelete(svcAccName,approles,token);
		}
		ResponseEntity<String> accountRoleDeletionResponse = deleteAccountRole(token, serviceAccount);
		if (HttpStatus.OK.equals(accountRoleDeletionResponse.getStatusCode())) {
			// Remove metadata...
			serviceAccount.setOwner(managedBy);
			ResponseEntity<String> metadataUpdateResponse =  deleteMetadata(token, serviceAccount);
			if (HttpStatus.OK.equals(metadataUpdateResponse.getStatusCode())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "offboardServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Successfully completed offboarding of AD service account from TVault for password rotation.")).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully completed offboarding of AD service account from TVault for password rotation.\"]}");
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "offboardServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Unable to delete Metadata for the Service Account")).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"errors\":[\"Failed to offboard AD service account from TVault for password rotation.\"]}");
			}
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "offboardServiceAccount").
					put(LogMessage.MESSAGE, String.format ("Failed to offboard AD service account from TVault for password rotation.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"errors\":[\"Failed to offboard AD service account from TVault for password rotation.\"]}");
		}
	}

	/**
	 * Create Service Account Role
	 * @param token
	 * @param serviceAccount
	 * @return
	 */
	private ResponseEntity<String> createAccountRole(String token, ServiceAccount serviceAccount) {
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@"+ serviceAccountSuffix) ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = JSONUtil.getJSON(serviceAccountTTL);
		Response onboardingResponse = reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token);
		if(onboardingResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || onboardingResponse.getHttpstatus().equals(HttpStatus.OK)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createAccountRole").
					put(LogMessage.MESSAGE, String.format ("Successfully created service account role.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created service account role.\"]}");
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createAccountRole").
					put(LogMessage.MESSAGE, String.format ("Failed to create service account role.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to create service account role.\"]}");
		}
	}
	/**
	 * Deletes the Account Role
	 * @param token
	 * @param serviceAccount
	 * @return
	 */
	private ResponseEntity<String> deleteAccountRole(String token, OnboardedServiceAccount serviceAccount) {
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@"+ serviceAccountSuffix) ;
		String svc_account_payload = JSONUtil.getJSON(serviceAccountTTL);
		Response onboardingResponse = reqProcessor.process("/ad/serviceaccount/offboard", svc_account_payload, token);
		if(onboardingResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || onboardingResponse.getHttpstatus().equals(HttpStatus.OK)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "deleteAccountRole").
					put(LogMessage.MESSAGE, String.format ("Successfully deleted service account role.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully deleted service account role.\"]}");
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "deleteAccountRole").
					put(LogMessage.MESSAGE, String.format ("Failed to delete service account role.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to delete service account role.\"]}");
		}
	}

	/**
	 * Create policies for service account
	 * @param token
	 * @param svcAccName
	 * @return
	 */
	private  ResponseEntity<String> createServiceAccountPolicies(String token, String svcAccName) {
		int succssCount = 0;
		for (String policyPrefix : TVaultConstants.getSvcAccPolicies().keySet()) {
			AccessPolicy accessPolicy = new AccessPolicy();
			String accessId = new StringBuffer().append(policyPrefix).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			accessPolicy.setAccessid(accessId);
			HashMap<String,String> accessMap = new HashMap<String,String>();
			String svcAccCredsPath=new StringBuffer().append(TVaultConstants.SVC_ACC_CREDS_PATH).append(svcAccName).toString();
			accessMap.put(svcAccCredsPath, TVaultConstants.getSvcAccPolicies().get(policyPrefix));
			// Attaching write permissions for owner
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.SUDO_POLICY)) {
				accessMap.put(TVaultConstants.SVC_ACC_ROLES_PATH + svcAccName, TVaultConstants.WRITE_POLICY);
				accessMap.put(TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + svcAccName, TVaultConstants.WRITE_POLICY);
			}
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.WRITE_POLICY)) {
				accessMap.put(TVaultConstants.SVC_ACC_ROLES_PATH + svcAccName, TVaultConstants.WRITE_POLICY);
				accessMap.put(TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + svcAccName, TVaultConstants.WRITE_POLICY);
			}
			accessPolicy.setAccess(accessMap);
			ResponseEntity<String> policyCreationStatus = accessService.createPolicy(token, accessPolicy);
			if (HttpStatus.OK.equals(policyCreationStatus.getStatusCode())) {
				succssCount++;
			}
		}
		if (succssCount == TVaultConstants.getSvcAccPolicies().size()) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createServiceAccountPolicies").
					put(LogMessage.MESSAGE, String.format ("Successfully created policies for service account.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created policies for service account\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "createServiceAccountPolicies").
				put(LogMessage.MESSAGE, String.format ("Failed to create some of the policies for service account.")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Failed to create some of the policies for service account\"]}");
	}
	/**
	 * Deletes Service Account policies
	 * @param token
	 * @param svcAccName
	 * @return
	 */
	private  ResponseEntity<String> deleteServiceAccountPolicies(String token, String svcAccName) {
		int succssCount = 0;
		for (String policyPrefix : TVaultConstants.getSvcAccPolicies().keySet()) {
			String accessId = new StringBuffer().append(policyPrefix).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			ResponseEntity<String> policyCreationStatus = accessService.deletePolicyInfo(token, accessId);
			if (HttpStatus.OK.equals(policyCreationStatus.getStatusCode())) {
				succssCount++;
			}
		}
		if (succssCount == TVaultConstants.getSvcAccPolicies().size()) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "deleteServiceAccountPolicies").
					put(LogMessage.MESSAGE, String.format ("Successfully created policies for service account.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully removed policies for service account\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "deleteServiceAccountPolicies").
				put(LogMessage.MESSAGE, String.format ("Failed to delete some of the policies for service account.")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Failed to delete some of the policies for service account\"]}");
	}
	/**
	 * Adds the user to a service account
	 * @param token
	 * @param serviceAccount
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> addUserToServiceAccount(String token, ServiceAccountUser serviceAccountUser, UserDetails userDetails, boolean isPartOfSvcAccOnboard) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Add User to ServiceAccount").
				put(LogMessage.MESSAGE, String.format ("Trying to add user to ServiceAccount")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));

		if(!isSvcaccPermissionInputValid(serviceAccountUser.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
		}
		if (serviceAccountUser.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountUser.setAccess(TVaultConstants.WRITE_POLICY);
		}

		String userName = serviceAccountUser.getUsername();
		String svcAccName = serviceAccountUser.getSvcAccName();
		String access = serviceAccountUser.getAccess();

		// TODO: Validation for String expectedPath = TVaultConstants.SVC_ACC_CREDS_PATH+svcAccName;

		userName = (userName !=null) ? userName.toLowerCase() : userName;
		access = (access != null) ? access.toLowerCase(): access;

		boolean isAuthorized = true;
		if (userDetails != null) {
			isAuthorized = hasAddUserPermission(userDetails, svcAccName, token, isPartOfSvcAccOnboard);
		}

		if(isAuthorized){
			if (!ifInitialPwdReset(token, userDetails, serviceAccountUser.getSvcAccName()) && !TVaultConstants.SUDO_POLICY.equals(serviceAccountUser.getAccess())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Add User to ServiceAccount").
						put(LogMessage.MESSAGE, "Failed to add user permission to Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add user permission to Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
			String policy = TVaultConstants.EMPTY;
			policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add User to Service Account").
					put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add User to Service Account").
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			Response userResponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);	
			}
			else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
				userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)){
				// OIDC implementation changes
				ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, userDetails);
				if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
					if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Add User to SDB")
								.put(LogMessage.MESSAGE,
										String.format("Trying to fetch OIDC user policies, failed"))
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
						return ResponseEntity.status(HttpStatus.FORBIDDEN)
								.body("{\"messages\":[\"User configuration failed. Please try again.\"]}");
					}
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
				}
				oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
				oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
				userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
				userResponse.setHttpstatus(responseEntity.getStatusCode());
			}

			String responseJson="";
			String groups="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					ObjectMapper objMapper = new ObjectMapper();
					//OIDC Changes
					if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcEntityResponse.getPolicies());
					} else {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
						}
					}
				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add User to ServiceAccount").
							put(LogMessage.MESSAGE, String.format ("Exception while creating currentpolicies or groups")).
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}

				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
				policies.add(policy);
			}else{
				// New user to be configured
				policies.add(policy);
			}
			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add User to ServiceAccount").
					put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureUserpassUser/configureLDAPUser", policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			Response ldapConfigresponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				//OIDC Implementation : Entity Update
				try {

					ldapConfigresponse = oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				}catch (Exception e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Add User to SDB").
							put(LogMessage.MESSAGE, String.format ("Exception while adding or updating the identity ")).
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				// User has been associated with Service Account. Now metadata has to be created
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "users");
				params.put("name",serviceAccountUser.getUsername());
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add User to ServiceAccount").
							put(LogMessage.MESSAGE, "User is successfully associated with Service Account").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully added user to the Service Account\"]}");
				} else{
					//Revert the user association...
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add User to ServiceAccount").
							put(LogMessage.MESSAGE, "Metadata creation for user association with service account failed").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
						ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, currentpoliciesString,
								token);
					} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString, groups,
								token);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						//OIDC changes
						try {
							ldapConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies,
									oidcEntityResponse.getEntityName());
                            oidcUtil.renewUserToken(userDetails.getClientToken());
						} catch (Exception e) {
							log.error(e);
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "Add User to SDB")
									.put(LogMessage.MESSAGE,
											String.format("Exception while adding or updating the identity "))
									.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
						}
					}
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Failed to add user to the Service Account. Metadata update failed\"]}");
					} else {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Failed to revert user association on Service Account\"]}");
					}
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to add user to the Service Account\"]}");
			}
			
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to users to this service account\"]}");
		}
	}

    /**
     * To check the initial password reset status
     * @param token
     * @param userDetails
     * @param svcAccName
     * @return
     */
	private boolean ifInitialPwdReset(String token, UserDetails userDetails, String svcAccName) {
		String _path = TVaultConstants.SVC_ACC_ROLES_PATH + svcAccName;
		boolean initialResetStatus = false;
		Response metaResponse = getMetadata(token, userDetails, _path);
		try {
            JsonNode resetStatus = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("initialPasswordReset");
            if (resetStatus != null) {
                initialResetStatus = Boolean.parseBoolean(resetStatus.asText());
            }
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "ifInitialPwdReset").
					put(LogMessage.MESSAGE, String.format ("Failed to get Initial password status for the Service account [%s]", svcAccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return initialResetStatus;
	}

	/**
	 * Removes user from service account
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromServiceAccount(String token, ServiceAccountUser serviceAccountUser, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if(!isSvcaccPermissionInputValid(serviceAccountUser.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
		}
		if (serviceAccountUser.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountUser.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String userName = serviceAccountUser.getUsername().toLowerCase();
		String svcAccName = serviceAccountUser.getSvcAccName();
		String access = serviceAccountUser.getAccess();

		boolean isAuthorized = true;
		if (userDetails != null) {
			isAuthorized = hasAddOrRemovePermission(userDetails, serviceAccountUser.getSvcAccName(), token);
		}
		if(isAuthorized){
			if (!ifInitialPwdReset(token, userDetails, serviceAccountUser.getSvcAccName())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Remove User from ServiceAccount").
						put(LogMessage.MESSAGE, "Failed to remove user permission from Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove user permission from Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove user from Service Account").
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			Response userResponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}",
						token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}", token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// OIDC implementation changes
				ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, userDetails);
				if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
					if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
						log.error(
								JSONUtil.getJSON(
										ImmutableMap.<String, String> builder()
												.put(LogMessage.USER,
														ThreadLocalContext.getCurrentMap().get(LogMessage.USER)
																.toString())
												.put(LogMessage.ACTION, "removeUserFromSafe")
												.put(LogMessage.MESSAGE,
														String.format("Trying to fetch OIDC user policies, failed"))
												.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap()
														.get(LogMessage.APIURL).toString())
												.build()));
					}
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
				}
				oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
				oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
				userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
				userResponse.setHttpstatus(responseEntity.getStatusCode());
			}

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove user from ServiceAccount").
					put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse.getHttpstatus())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));

			String responseJson="";
			String groups="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					ObjectMapper objMapper = new ObjectMapper();
					if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcEntityResponse.getPolicies());
						//groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
					} else {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
						}
					}
				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove User from ServiceAccount").
							put(LogMessage.MESSAGE, String.format ("Exception while creating currentpolicies or groups")).
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
				policies.addAll(currentpolicies);
				//policies.remove(policy);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
			}
			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			Response ldapConfigresponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// OIDC Implementation : Entity Update
				try {
					ldapConfigresponse = oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				} catch (Exception e) {
					log.error(e);
					log.error(
							JSONUtil.getJSON(
									ImmutableMap.<String, String> builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
											.put(LogMessage.ACTION, "Remove User to SVC")
											.put(LogMessage.MESSAGE,
													String.format("Exception while updating the identity"))
											.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
											.put(LogMessage.APIURL,
													ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
											.build()));
				}
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				// User has been associated with Service Account. Now metadata has to be deleted
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "users");
				params.put("name",userName);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove User from ServiceAccount").
							put(LogMessage.MESSAGE, "User is successfully Removed from Service Account").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully removed user from the Service Account\"]}");
				} else {
					if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
						ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, currentpoliciesString,
								token);
					} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString, groups,
								token);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						// OIDC changes
						try {
							ldapConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies,
									oidcEntityResponse.getEntityName());
							oidcUtil.renewUserToken(userDetails.getClientToken());
						} catch (Exception e2) {
							log.error(e2);
							log.error(
									JSONUtil.getJSON(ImmutableMap.<String, String> builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
											.put(LogMessage.ACTION, "Remove User to SDB")
											.put(LogMessage.MESSAGE,
													String.format("Exception while updating the identity"))
											.put(LogMessage.STACKTRACE, Arrays.toString(e2.getStackTrace()))
											.put(LogMessage.APIURL,
													ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
											.build()));
						}
					}
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the user from the Service Account. Metadata update failed\"]}");
					} else {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to revert user association on Service Account\"]}");
					}
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remvoe the user from the Service Account\"]}");
			}	
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this service account\"]}");
		}
	}
	/**
	 * Temporarily sleep for a second
	 */
	private void sleep() {
		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * To reset Service Account Password
	 * @param token
	 * @param svcAccName
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> resetSvcAccPassword(String token, String svcAccName, UserDetails userDetails){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "resetSvcAccPassword").
				put(LogMessage.MESSAGE, String.format("Trying to reset service account password [%s]", svcAccName)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		OnboardedServiceAccountDetails onbSvcAccDtls = getOnboarderdServiceAccountDetails(token, svcAccName);
		if (onbSvcAccDtls == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "resetSvcAccPassword").
					put(LogMessage.MESSAGE, String.format("Unable to reset password for [%s]", svcAccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account\"]}");
		}

		long ttl = onbSvcAccDtls.getTtl();
		ServiceAccount serviceAccount = new ServiceAccount();
		serviceAccount.setName(svcAccName);
		serviceAccount.setAutoRotate(true);
		serviceAccount.setTtl(1L); // set ttl to 1 second to temporarily so that it can be rotated immediately

		ResponseEntity<String> roleCreationResetResponse = createAccountRole(token, serviceAccount);
		if(roleCreationResetResponse.getStatusCode().equals(HttpStatus.OK)) {
			sleep();
			//Reset the password now...
			Response resetResponse = reqProcessor.process("/ad/serviceaccount/resetpwd","{\"role_name\":\""+svcAccName+"\"}",token);
			if(HttpStatus.OK.equals(resetResponse.getHttpstatus())) {
				//Reset ttl to 90 days (or based on password policy) long ttl = 7776000L; // 90 days...
				serviceAccount.setTtl(ttl);
				ResponseEntity<String> roleCreationResponse = createAccountRole(token, serviceAccount);
				if(roleCreationResponse.getStatusCode().equals(HttpStatus.OK)) {
					// Read the password to get the updated one.
					Response response = reqProcessor.process("/ad/serviceaccount/readpwd","{\"role_name\":\""+svcAccName+"\"}",token);
					if(HttpStatus.OK.equals(response.getHttpstatus())) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "resetSvcAccPassword").
								put(LogMessage.MESSAGE, String.format("Successfully reset service account password for [%s]", svcAccName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						try {
							ADServiceAccountCreds adServiceAccountCreds = new ADServiceAccountCreds();
							Map<String, Object> requestParams = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
							if (requestParams.get("current_password") != null) {
								adServiceAccountCreds.setCurrent_password((String) requestParams.get("current_password"));
							}
							if (requestParams.get("username") != null) {
								adServiceAccountCreds.setUsername((String) requestParams.get("username"));
							}
							if (requestParams.get("last_password") != null ) {
								adServiceAccountCreds.setLast_password((String) requestParams.get("last_password"));
							}

							// Check metadata to get the owner information
							Response metaDataResponse = getMetadata(token, userDetails, TVaultConstants.SVC_ACC_ROLES_PATH + svcAccName);
							if (metaDataResponse!=null) {
								try {
									JsonNode metaNode = new ObjectMapper().readTree(metaDataResponse.getResponse()).get("data").get("initialPasswordReset");
									if (metaNode != null) {
										boolean initialResetStatus = false;

										initialResetStatus = Boolean.parseBoolean(metaNode.asText());
										if (!initialResetStatus) {

											// update metadata for initial password reset
											String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
											Map<String,String> params = new Hashtable<>();
											params.put("type", "initialPasswordReset");
											params.put("path",path);
											params.put("value","true");
											Response metadataResponse = ControllerUtil.updateMetadataOnSvcaccPwdReset(params,token);
											if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
												log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
														put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
														put(LogMessage.ACTION, "Update metadata on password reset").
														put(LogMessage.MESSAGE, "Metadata update Success.").
														put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
														put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
														build()));
											}
											else {
												log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
														put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
														put(LogMessage.ACTION, "Update metadata on password reset").
														put(LogMessage.MESSAGE, "Metadata update failed.").
														put(LogMessage.STATUS, metadataResponse!=null?metadataResponse.getHttpstatus().toString():HttpStatus.BAD_REQUEST.toString()).
														put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
														build()));
											}

											metaNode = new ObjectMapper().readTree(metaDataResponse.getResponse()).get("data").get("managedBy");
											String svcOwner = metaNode.asText();
											// Adding read and reset permisison to Service account by default. (At the time of initial password reset)
											ServiceAccountUser serviceAccountOwner = new ServiceAccountUser(svcAccName, svcOwner, TVaultConstants.RESET_POLICY);
											ResponseEntity<String> addOwnerWriteToServiceAccountResponse = addUserToServiceAccount(token, serviceAccountOwner, userDetails, false);
											if (addOwnerWriteToServiceAccountResponse!= null && HttpStatus.NO_CONTENT.equals(addOwnerWriteToServiceAccountResponse.getStatusCode())) {
												log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
														put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
														put(LogMessage.ACTION, "readSvcAccPassword").
														put(LogMessage.MESSAGE, "Updated write permission to Service account owner as part of initial reset.").
														put(LogMessage.STATUS, addOwnerWriteToServiceAccountResponse.getStatusCode().toString()).
														put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
														build()));
											}
										}

									}
								} catch (IOException e) {
									log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
											put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
											put(LogMessage.ACTION, "resetSvcAccPassword").
											put(LogMessage.MESSAGE, String.format ("Failed to get metadata for the Service account [%s]", svcAccName)).
											put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
											build()));
								}
							}
							else {
								log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
										put(LogMessage.ACTION, "resetSvcAccPassword").
										put(LogMessage.MESSAGE, String.format ("Failed to get metadata for the Service account [%s]", svcAccName)).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
										build()));
							}
							return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(adServiceAccountCreds));
						}
						catch(Exception ex) {
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
									put(LogMessage.ACTION, "readSvcAccPassword").
									put(LogMessage.MESSAGE, String.format("There are no service accounts currently onboarded or error in retrieving credentials for the onboarded service account [%s]", svcAccName)).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
									build()));

						}
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to get password details for the given service account\"]}");
					}
					else {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "resetSvcAccPassword").
								put(LogMessage.MESSAGE, String.format("Unable to reset password for [%s]", svcAccName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password.\"]}");
					}
				}
				else {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "resetSvcAccPassword").
							put(LogMessage.MESSAGE, String.format("Unable to reset password for [%s]", svcAccName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account. Failed to reset the service account with original ttl.\"]}");
				}
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "resetSvcAccPassword").
						put(LogMessage.MESSAGE, String.format("Unable to reset password for [%s]", svcAccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				//Reset ttl to 90 days (or based on password policy) long ttl = 7776000L; // 90 days...
				serviceAccount.setTtl(ttl);
				ResponseEntity<String> roleCreationResponse = createAccountRole(token, serviceAccount);
				if(roleCreationResponse.getStatusCode().equals(HttpStatus.OK)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "resetSvcAccPassword").
							put(LogMessage.MESSAGE, String.format("Unable to reset password for [%s]. Role updated back to the correct ttl", svcAccName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
				if (HttpStatus.FORBIDDEN.equals(resetResponse.getHttpstatus())) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: Unable to reset password details for the given service account.\"]}");
				}
				else {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password after setting ttl to 1 second.\"]}");
				}
			}
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "resetSvcAccPassword").
				put(LogMessage.MESSAGE, String.format("Unable to reset password for [%s]", svcAccName)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account\"]}");
	}

	/**
	 * Gets service account password
	 * @param token
	 * @param svcAccName
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readSvcAccPassword(String token, String svcAccName, UserDetails userDetails){

		// Restricting owner from reading password before activation. Owner can read/reset password after activation.
		ServiceAccountMetadataDetails metadataDetails = getServiceAccountMetadataDetails(token, userDetails, svcAccName);
		if (userDetails.getUsername().equalsIgnoreCase(metadataDetails.getManagedBy()) && !metadataDetails.getInitialPasswordReset()) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "readSvcAccPassword").
					put(LogMessage.MESSAGE, "Failed to read service account password. Initial password reset is pending for this Service Account.").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to read service account password. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
		}

		Response response = reqProcessor.process("/ad/serviceaccount/readpwd","{\"role_name\":\""+svcAccName+"\"}",token);
		ADServiceAccountCreds adServiceAccountCreds = null;
		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "readSvcAccPassword").
					put(LogMessage.MESSAGE, String.format("Successfully read the password details for the service account [%s]", svcAccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			try {
				adServiceAccountCreds = new ADServiceAccountCreds();
				Map<String, Object> requestParams = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
				if (requestParams.get("current_password") != null) {
					adServiceAccountCreds.setCurrent_password((String) requestParams.get("current_password"));
				}
				if (requestParams.get("username") != null) {
					adServiceAccountCreds.setUsername((String) requestParams.get("username"));
				}
				if (requestParams.get("last_password") != null ) {
					adServiceAccountCreds.setLast_password((String) requestParams.get("last_password"));
				}
				return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(adServiceAccountCreds));
			}
			catch(Exception ex) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "readSvcAccPassword").
						put(LogMessage.MESSAGE, String.format("There are no service accounts currently onboarded or error in retrieving credentials for the onboarded service account [%s]", svcAccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));

			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to get password details for the given service account\"]}");
		}
		else if (HttpStatus.FORBIDDEN.equals(response.getHttpstatus())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "readSvcAccPassword").
					put(LogMessage.MESSAGE, String.format("Permission denied to read password for [%s]", svcAccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to read the password details for the given service account\"]}");

		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "readSvcAccPassword").
					put(LogMessage.MESSAGE, String.format("Unable to read password for [%s]", svcAccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to get password details for the given service account\"]}");
		}
	}

	/**
	 * Gets the details of a service account that is already onboarded into TVault
	 * @param token
	 * @param svcAccName
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getOnboarderdServiceAccount(String token, String svcAccName, UserDetails userDetails){
		OnboardedServiceAccountDetails onbSvcAccDtls = getOnboarderdServiceAccountDetails(token, svcAccName);
		if (onbSvcAccDtls != null) {
			String onbSvcAccDtlsJson = JSONUtil.getJSON(onbSvcAccDtls);
			return ResponseEntity.status(HttpStatus.OK).body(onbSvcAccDtlsJson);
		}
		else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"errors\":[\"Either Service Account is not onbaorderd or you don't have enough permission to read\"]}");
		}
	}
	/**
	 * Gets the details of an onboarded service account
	 * @return
	 */
	private OnboardedServiceAccountDetails getOnboarderdServiceAccountDetails(String token, String svcAccName) {
		OnboardedServiceAccountDetails onbSvcAccDtls = null;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "getOnboardedServiceAccountDetails").
			      put(LogMessage.MESSAGE, String.format("Trying to get onboaded service account details")).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response svcAccDtlsresponse = reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token);
		if (HttpStatus.OK.equals(svcAccDtlsresponse.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "getOnboardedServiceAccountDetails").
				      put(LogMessage.MESSAGE, "Successfully retrieved the Service Account details").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			try {
				String response = svcAccDtlsresponse.getResponse();
				Map<String, Object> requestParams = new ObjectMapper().readValue(response, new TypeReference<Map<String, Object>>(){});
				
				String accName =  (String) requestParams.get("service_account_name");
				Integer accTTL = (Integer)requestParams.get("ttl");
				String accLastPwdRotation = (String) requestParams.get("last_vault_rotation");
				String accLastPwd = (String) requestParams.get("password_last_set");
				onbSvcAccDtls = new OnboardedServiceAccountDetails();
				onbSvcAccDtls.setName(accName);
				try {
					onbSvcAccDtls.setTtl(new Long(accTTL));
				} catch (Exception e) {
				}
				onbSvcAccDtls.setLastVaultRotation(accLastPwdRotation);
				onbSvcAccDtls.setPasswordLastSet(accLastPwd);
			}
			catch(Exception ex) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "getADServiceAccounts").
						put(LogMessage.MESSAGE, String.format("There are no service accounts currently onboarded or error in retrieving onboarded service accounts")).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				
			}
			return onbSvcAccDtls;
		}
		else {
			return onbSvcAccDtls;
		}
	}
	/**
	 * 
	 * @param userDetails
	 * @param serviceAccountUser
	 * @param action
	 * @return
	 */
	public boolean canAddOrRemoveUser(UserDetails userDetails, ServiceAccountUser serviceAccountUser, String action) {
		if (userDetails != null && userDetails.isAdmin()) {
			// Admin is always authorized to add/remove user
			return true;
		}
		else {
			//TODO: Implementation to be completed...
			// Get the policies for the current users
			// Get the serviceAccountName from serviceAccountUser
			// If there is owner policy for the serviceAccountName, then this owner can add/remove user
			return false;
		}
	}
	/**
	 * To get list of service accounts
	 * @param token
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getOnboardedServiceAccounts(String token,  UserDetails userDetails) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "listOnboardedServiceAccounts").
			      put(LogMessage.MESSAGE, String.format("Trying to get list of onboaded service accounts")).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = null;
		if (userDetails.isAdmin()) {
			response = reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token);
		}
		else {
			String[] latestPolicies = policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails);
			List<String> onboardedlist = new ArrayList<>();
			for (String policy: latestPolicies) {
				if (policy.startsWith("o_svcacct")) {
					onboardedlist.add(policy.substring(10));
				}
			}
			response = new Response();
			response.setHttpstatus(HttpStatus.OK);
			response.setSuccess(true);
			response.setResponse("{\"keys\":"+JSONUtil.getJSON(onboardedlist)+"}");
		}

		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "listOnboardedServiceAccounts").
				      put(LogMessage.MESSAGE, "Successfully retrieved the list of Service Accounts").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"keys\":[]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

    /**
     * Check if user has the permission to add user/group/awsrole/approles to the Service Account
     * @param userDetails
     * @param action
     * @param token
     * @return
     */
    public boolean hasAddOrRemovePermission(UserDetails userDetails, String serviceAccount, String token) {
		// Owner of the service account can add/remove users, groups, aws roles and approles to service account
        String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(serviceAccount).toString();
        String [] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);
        if (ArrayUtils.contains(policies, o_policy)) {
            return true;
        }
        return false;
    }

	/**
	 * Check if user has the permission to add user to the Service Account
	 * @param userDetails
	 * @param serviceAccount
	 * @param access
	 * @param token
	 * @return
	 */
	public boolean hasAddUserPermission(UserDetails userDetails, String serviceAccount, String token, boolean isPartOfSvcAccOnboard) {
		// Admin user can add sudo policy for owner while onboarding the service account
		if (userDetails.isAdmin() && isPartOfSvcAccOnboard) {
			return true;
		}
		// Owner of the service account can add/remove users, groups, aws roles and approles to service account
		String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(serviceAccount).toString();
		String [] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);
		if (ArrayUtils.contains(policies, o_policy)) {
			return true;
		}
		return false;
	}
	/**
	 * Validates Service Account permission inputs
	 * @param access
	 * @return
	 */
	public static boolean isSvcaccPermissionInputValid(String access) {
		if (!org.apache.commons.lang3.ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}

    /**
     * Add Group to Service Account
     * @param token
     * @param serviceAccountGroup
     * @param userDetails
     * @return
     */
	public ResponseEntity<String> addGroupToServiceAccount(String token, ServiceAccountGroup serviceAccountGroup, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Add Group to Service Account").
				put(LogMessage.MESSAGE, String.format ("Trying to add Group to Service Account")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
        if(!isSvcaccPermissionInputValid(serviceAccountGroup.getAccess())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
        }
		if (serviceAccountGroup.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountGroup.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String groupName = serviceAccountGroup.getGroupname();
		String svcAccName = serviceAccountGroup.getSvcAccName();
		String access = serviceAccountGroup.getAccess();

		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		}

		groupName = (groupName !=null) ? groupName.toLowerCase() : groupName;
		access = (access != null) ? access.toLowerCase(): access;

		boolean canAddGroup = hasAddOrRemovePermission(userDetails, svcAccName, token);
		if(canAddGroup){
			if (!ifInitialPwdReset(token, userDetails, serviceAccountGroup.getSvcAccName())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Add Group to ServiceAccount").
						put(LogMessage.MESSAGE, "Failed to add group permission to Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add group permission to Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
			String policy = TVaultConstants.EMPTY;
			policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add Group to Service Account").
					put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add Group to Service Account").
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			Response groupResp = new Response();
			
			if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				groupResp = reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"" + groupName + "\"}", token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// call read api with groupname
				oidcGroup = oidcUtil.getIdentityGroupDetails(groupName, token);
				if (oidcGroup != null) {
					groupResp.setHttpstatus(HttpStatus.OK);
					groupResp.setResponse(oidcGroup.getPolicies().toString());
				} else {
					groupResp.setHttpstatus(HttpStatus.BAD_REQUEST);
				}
			}

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add Group to ServiceAccount").
					put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", groupResp.getHttpstatus())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));

			String responseJson="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(groupResp.getHttpstatus())){
				responseJson = groupResp.getResponse();
				try {
					ObjectMapper objMapper = new ObjectMapper();
					//OIDC Changes
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcGroup.getPolicies());
					}
				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Group to ServiceAccount").
							put(LogMessage.MESSAGE, String.format ("Exception while creating currentpolicies")).
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}

				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
				policies.add(policy);
			}else{
				// New group to be configured
				policies.add(policy);
			}
			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add Group to ServiceAccount").
					put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureLDAPGroup", policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));

			Response ldapConfigresponse = new Response();
			//OIDC Changes
			if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies,
						oidcGroup != null ? oidcGroup.getId() : null);
				oidcUtil.renewUserToken(userDetails.getClientToken());
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "groups");
				params.put("name",groupName);
				params.put("path",path);
				params.put("access",access);

				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Group to Service Account").
							put(LogMessage.MESSAGE, "Group configuration Success.").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Service Account\"]}");
				}
				// OIDC Changes
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString, token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, currentpolicies,
							currentpolicies, oidcGroup.getId());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				}
				if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Group to Service Account").
							put(LogMessage.MESSAGE, "Reverting, group policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Group to Service Account").
							put(LogMessage.MESSAGE, "Reverting group policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to add group to the Service Account\"]}");
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this service account\"]}");
		}
	}

    /**
     * Remove Group Service Account
     * @param token
     * @param serviceAccountGroup
     * @param userDetails
     * @return
     */
    public ResponseEntity<String> removeGroupFromServiceAccount(String token, ServiceAccountGroup serviceAccountGroup, UserDetails userDetails) {
    	OIDCGroup oidcGroup = new OIDCGroup();
    	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "Remove Group from Service Account").
                put(LogMessage.MESSAGE, String.format ("Trying to remove Group from Service Account")).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
        if(!isSvcaccPermissionInputValid(serviceAccountGroup.getAccess())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
        }
		if (serviceAccountGroup.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountGroup.setAccess(TVaultConstants.WRITE_POLICY);
		}
        String groupName = serviceAccountGroup.getGroupname().toLowerCase();
        String svcAccName = serviceAccountGroup.getSvcAccName();
        String access = serviceAccountGroup.getAccess();


        boolean isAuthorized = true;
        if (userDetails != null) {
            isAuthorized = hasAddOrRemovePermission(userDetails, svcAccName, token);
        }

        if(isAuthorized){
			if (!ifInitialPwdReset(token, userDetails, serviceAccountGroup.getSvcAccName())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Remove Group from ServiceAccount").
						put(LogMessage.MESSAGE, "Failed to remove group permission from Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove group permission from Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
            String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "Remove group from Service Account").
                    put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            Response groupResp = new Response();      
			if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				groupResp = reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"" + groupName + "\"}", token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// call read api with groupname
				oidcGroup = oidcUtil.getIdentityGroupDetails(groupName, token);
				if (oidcGroup != null) {
					groupResp.setHttpstatus(HttpStatus.OK);
					groupResp.setResponse(oidcGroup.getPolicies().toString());
				} else {
					groupResp.setHttpstatus(HttpStatus.BAD_REQUEST);
				}
			}
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "Remove group from ServiceAccount").
                    put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", groupResp.getHttpstatus())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));

            String responseJson="";
            List<String> policies = new ArrayList<>();
            List<String> currentpolicies = new ArrayList<>();
            String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

            if(HttpStatus.OK.equals(groupResp.getHttpstatus())){
                responseJson = groupResp.getResponse();
                try {
					ObjectMapper objMapper = new ObjectMapper();
					// OIDC Changes
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcGroup.getPolicies());
					}
                } catch (IOException e) {
                    log.error(e);
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, "Remove group from ServiceAccount").
                            put(LogMessage.MESSAGE, String.format ("Exception while creating currentpolicies or groups")).
                            put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                            build()));
                }

                policies.addAll(currentpolicies);
                //policies.remove(policy);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
            }
            String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			Response ldapConfigresponse = new Response();
			// OIDC Changes
			if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies,
						oidcGroup.getId());
				oidcUtil.renewUserToken(userDetails.getClientToken());
			}
            if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "groups");
				params.put("name",groupName);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove Group to Service Account").
							put(LogMessage.MESSAGE, "Group configuration Success.").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from Service Account\"]}");
				}
				// OIDC Changes
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString, token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, currentpolicies,
							currentpolicies, oidcGroup.getId());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				}
				if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Group to Service Account").
							put(LogMessage.MESSAGE, "Reverting, group policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Group to Service Account").
							put(LogMessage.MESSAGE, "Reverting group policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
				}
            }
            else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the group from the Service Account\"]}");
            }
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove groups from this service account\"]}");
        }

    }

    /**
     * Associate Approle to Service Account
     * @param userDetails
     * @param token
     * @param serviceAccountApprole
     * @return
     */
    public ResponseEntity<String> associateApproletoSvcAcc(UserDetails userDetails, String token, ServiceAccountApprole serviceAccountApprole) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "Add Approle to Service Account").
                put(LogMessage.MESSAGE, String.format ("Trying to add Approle to Service Account")).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
        if(!isSvcaccPermissionInputValid(serviceAccountApprole.getAccess())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
        }
		if (serviceAccountApprole.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountApprole.setAccess(TVaultConstants.WRITE_POLICY);
		}
        String approleName = serviceAccountApprole.getApprolename();
        String svcAccName = serviceAccountApprole.getSvcAccName();
        String access = serviceAccountApprole.getAccess();

        if (serviceAccountApprole.getApprolename().equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Service Account\"]}");
        }
        approleName = (approleName !=null) ? approleName.toLowerCase() : approleName;
        access = (access != null) ? access.toLowerCase(): access;

        boolean isAuthorized = hasAddOrRemovePermission(userDetails, svcAccName, token);
        if(isAuthorized){
			if (!ifInitialPwdReset(token, userDetails, serviceAccountApprole.getSvcAccName())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Add approle to ServiceAccount").
						put(LogMessage.MESSAGE, "Failed to add approle permission to Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add approle permission to Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
            String policy = TVaultConstants.EMPTY;
            policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "Add Approle to Service Account").
                    put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "Add Approle to Service Account").
                    put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));

            Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approleName+"\"}",token);

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "Add Approle to ServiceAccount").
                    put(LogMessage.MESSAGE, String.format ("roleResponse status is [%s]", roleResponse.getHttpstatus())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));

            String responseJson="";
            List<String> policies = new ArrayList<>();
            List<String> currentpolicies = new ArrayList<>();

            if(HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
				responseJson = roleResponse.getResponse();
				ObjectMapper objMapper = new ObjectMapper();
				try {
					JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
					if (null != policiesArry) {
						for (JsonNode policyNode : policiesArry) {
							currentpolicies.add(policyNode.asText());
						}
					}
				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Approle to ServiceAccount").
							put(LogMessage.MESSAGE, String.format("Exception while creating currentpolicies")).
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
				policies.add(policy);
			} else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Non existing role name. Please configure approle as first step\"]}");
            }
			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add Approle to ServiceAccount").
					put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureApprole", policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));

			Response approleControllerResp = appRoleService.configureApprole(approleName,policiesString,token);

			if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT) || approleControllerResp.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "app-roles");
				params.put("name",approleName);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Approle to Service Account").
							put(LogMessage.MESSAGE, "Approle successfully associated with Service Account").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Service Account\"]}");
				}
				approleControllerResp = appRoleService.configureApprole(approleName,currentpoliciesString,token);
				if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Approle to Service Account").
							put(LogMessage.MESSAGE, "Reverting, Approle policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Approle to Service Account").
							put(LogMessage.MESSAGE, "Reverting Approle policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add Approle to the Service Account\"]}");
			}
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this service account\"]}");
        }
    }

	public ResponseEntity<String> getServiceAccountMeta(String token, UserDetails userDetails, String path) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Get metadata for Service Account").
				put(LogMessage.MESSAGE, String.format ("Trying to get metadata for Service Account")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		Response response = getMetadata(token, userDetails, path);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Get metadata for service account
	 * @param token
	 * @param userDetails
	 * @param path
	 * @return
	 */
	private Response getMetadata(String token, UserDetails userDetails, String path) {
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		if (path != null && path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		if (path != null && path.endsWith("/")) {
			path = path.substring(0, path.length()-1);
		}
		String _path = "metadata/"+path;
		return reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
	}

	/**
	 * Update User policy on Service account offboarding
	 * @param svcAccName
	 * @param acessInfo
	 * @param token
	 * @param userDetails
	 */
	private void updateUserPolicyAssociationOnSvcaccDelete(String svcAccName,Map<String,String> acessInfo,String token, UserDetails userDetails){
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "updateUserPolicyAssociationOnSvcaccDelete").
				put(LogMessage.MESSAGE, String.format ("trying updateUserPolicyAssociationOnSvcaccDelete")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		log.debug ("updateUserPolicyAssociationOnSvcaccDelete...for auth method " + vaultAuthMethod);
		if(acessInfo!=null){
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			Set<String> users = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for(String userName : users){

				Response userResponse = new Response();
				if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
					userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);
				}
				else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
					userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
				}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					// OIDC implementation changes
					ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, null);
					if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
						if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
									.put(LogMessage.ACTION, "Add User to SDB")
									.put(LogMessage.MESSAGE,
											String.format("Trying to fetch OIDC user policies, failed"))
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
									.build()));
							ResponseEntity.status(HttpStatus.FORBIDDEN)
									.body("{\"messages\":[\"User configuration failed. Please try again.\"]}");
						}
						ResponseEntity.status(HttpStatus.NOT_FOUND)
								.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
					}
					oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
					oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
					userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
					userResponse.setHttpstatus(responseEntity.getStatusCode());
				}
				String responseJson="";
				String groups="";
				List<String> policies = new ArrayList<>();
				List<String> currentpolicies = new ArrayList<>();

				if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
					responseJson = userResponse.getResponse();
					try {
						// OIDC implementation changes
						if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
							currentpolicies.addAll(oidcEntityResponse.getPolicies());
						} else {
							currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
							if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
								groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
							}
						}	
					} catch (IOException e) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "updateUserPolicyAssociationOnSvcaccDelete").
								put(LogMessage.MESSAGE, String.format ("updateUserPolicyAssociationOnSvcaccDelete failed [%s]", e.getMessage())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
					}
					policies.addAll(currentpolicies);
					policies.remove(r_policy);
					policies.remove(w_policy);
					policies.remove(d_policy);
					policies.remove(o_policy);

					String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");

					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "updateUserPolicyAssociationOnSvcaccDelete").
							put(LogMessage.MESSAGE, String.format ("Current policies [%s]", policies )).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "updateUserPolicyAssociationOnSvcaccDelete").
								put(LogMessage.MESSAGE, String.format ("Current policies userpass [%s]", policies )).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						ControllerUtil.configureUserpassUser(userName,policiesString,token);
					}
					else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "updateUserPolicyAssociationOnSvcaccDelete").
								put(LogMessage.MESSAGE, String.format ("Current policies ldap [%s]", policies )).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
					}
					else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						//OIDC Implementation : Entity Update
						try {
							oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
							oidcUtil.renewUserToken(userDetails.getClientToken());
						} catch (Exception e) {
							log.error(e);
							log.error(
									JSONUtil.getJSON(
											ImmutableMap.<String, String> builder()
													.put(LogMessage.USER,
															ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
													.put(LogMessage.ACTION, "Add User to SDB")
													.put(LogMessage.MESSAGE,
															"Exception while adding or updating the identity ")
													.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
													.put(LogMessage.APIURL,
															ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
													.build()));
						}
					}
				}
			}
		}
	}

	/**
	 * Update Group policy on Service account offboarding
	 * @param svcAccName
	 * @param acessInfo
	 * @param token
	 * @param userDetails
	 */
	private void updateGroupPolicyAssociationOnSvcaccDelete(String svcAccName,Map<String,String> acessInfo,String token, UserDetails userDetails){
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "updateGroupPolicyAssociationOnSvcaccDelete").
				put(LogMessage.MESSAGE, String.format ("trying updateGroupPolicyAssociationOnSvcaccDelete")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			log.debug ("Inside userpass of updateGroupPolicyAssociationOnSvcaccDelete...Just Returning...");
			return;
		}
		if(acessInfo!=null){
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			Set<String> groups = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for(String groupName : groups){
				Response response = new Response();
				if(TVaultConstants.LDAP.equals(vaultAuthMethod)){
				    response = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);	
				}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					//call read api with groupname
					oidcGroup = oidcUtil.getIdentityGroupDetails(groupName, token);
					if (oidcGroup != null) {
						response.setHttpstatus(HttpStatus.OK);
						response.setResponse(oidcGroup.getPolicies().toString());
					} else {
						response.setHttpstatus(HttpStatus.BAD_REQUEST);
					}
				}
				
				String responseJson=TVaultConstants.EMPTY;
				List<String> policies = new ArrayList<>();
				List<String> currentpolicies = new ArrayList<>();
				if(HttpStatus.OK.equals(response.getHttpstatus())){
					responseJson = response.getResponse();
					try {
						//OIDC Changes
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
						} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
							currentpolicies.addAll(oidcGroup.getPolicies());
						}
					} catch (IOException e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "updateGroupPolicyAssociationOnSvcaccDelete").
								put(LogMessage.MESSAGE, String.format ("updateGroupPolicyAssociationOnSvcaccDelete failed [%s]", e.getMessage())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
					}
					policies.addAll(currentpolicies);
					policies.remove(r_policy);
					policies.remove(w_policy);
					policies.remove(d_policy);
					String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "updateGroupPolicyAssociationOnSvcaccDelete").
							put(LogMessage.MESSAGE, String.format ("Current policies [%s]", policies )).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies, oidcGroup.getId());
						oidcUtil.renewUserToken(userDetails.getClientToken());
					}
				}
			}
		}
	}

    /**
     * Aws role deletion as part of Offboarding
     * @param svcAccName
     * @param acessInfo
     * @param token
     */
    private void deleteAwsRoleonOnSvcaccDelete(String svcAccName, Map<String,String> acessInfo, String token) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "deleteAwsRoleAssociateionOnSvcaccDelete").
                put(LogMessage.MESSAGE, String.format ("Trying to delete AwsRole On Service Account offboarding")).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        if(acessInfo!=null){
            Set<String> roles = acessInfo.keySet();
            for(String role : roles){
                Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);
                if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, "deleteAwsRoleAssociateionOnSvcaccDelete").
                            put(LogMessage.MESSAGE, String.format ("%s, AWS Role is deleted as part of offboarding Service account.", role)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                            build()));
                }else{
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, "deleteAwsRoleAssociateionOnSvcaccDelete").
                            put(LogMessage.MESSAGE, String.format ("%s, AWS Role deletion as part of offboarding Service account failed.", role)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                            build()));
                }
            }
        }
    }

    /**
     * Approle policy update as part of offboarding
     * @param svcAccName
     * @param acessInfo
     * @param token
     */
    private void updateApprolePolicyAssociationOnSvcaccDelete(String svcAccName, Map<String,String> acessInfo, String token) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "updateApprolePolicyAssociationOnSvcaccDelete").
                put(LogMessage.MESSAGE, String.format ("trying updateApprolePolicyAssociationOnSvcaccDelete")).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        if(acessInfo!=null) {
            String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
            String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

            Set<String> approles = acessInfo.keySet();
            ObjectMapper objMapper = new ObjectMapper();
            for(String approleName : approles) {
                Response roleResponse = reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"" + approleName + "\"}", token);
                String responseJson = "";
                List<String> policies = new ArrayList<>();
                List<String> currentpolicies = new ArrayList<>();
                if (HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
                    responseJson = roleResponse.getResponse();
                    try {
                        JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
						if (null != policiesArry) {
							for (JsonNode policyNode : policiesArry) {
								currentpolicies.add(policyNode.asText());
							}
						}
                    } catch (IOException e) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "updateApprolePolicyAssociationOnSvcaccDelete").
								put(LogMessage.MESSAGE, String.format ("%s, Approle removal as part of offboarding Service account failed.", approleName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
                    }
                    policies.addAll(currentpolicies);
                    policies.remove(r_policy);
                    policies.remove(w_policy);
                    policies.remove(d_policy);

                    String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, "updateApprolePolicyAssociationOnSvcaccDelete").
                            put(LogMessage.MESSAGE, "Current policies :" + policiesString + " is being configured").
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                            build()));
                    appRoleService.configureApprole(approleName, policiesString, token);
                }
            }
        }
    }

    /**
     * Get Manager details for service account
     * @param filter
     * @return
     */
    private List<ADUserAccount> getServiceAccountManagerDetails(String filter) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "getServiceAccountManagerDetails").
                put(LogMessage.MESSAGE, String.format("Trying to get manager details")).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        return adUserLdapTemplate.search("", filter, new AttributesMapper<ADUserAccount>() {
            @Override
            public ADUserAccount mapFromAttributes(Attributes attr) throws NamingException {
				ADUserAccount person = new ADUserAccount();
				if (attr != null) {
					String userId = ((String) attr.get("name").get());
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
	 * Remove approle from service account
	 * @param userDetails
	 * @param token
	 * @param serviceAccountApprole
	 * @return
	 */
	public ResponseEntity<String> removeApproleFromSvcAcc(UserDetails userDetails, String token, ServiceAccountApprole serviceAccountApprole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Remove Approle from Service Account").
				put(LogMessage.MESSAGE, String.format ("Trying to remove approle from Service Account [%s]", serviceAccountApprole.getApprolename())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if (serviceAccountApprole.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountApprole.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String approleName = serviceAccountApprole.getApprolename();
		String svcAccName = serviceAccountApprole.getSvcAccName();
		String access = serviceAccountApprole.getAccess();

		if (serviceAccountApprole.getApprolename().equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to remove this AppRole to any Service Account\"]}");
		}
		approleName = (approleName !=null) ? approleName.toLowerCase() : approleName;
		access = (access != null) ? access.toLowerCase(): access;
		if(StringUtils.isEmpty(access)){
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access. Valid values are read, reset, deny \"]}");
		}
		boolean isAuthorized = hasAddOrRemovePermission(userDetails, svcAccName, token);

		if (isAuthorized) {
			if (!ifInitialPwdReset(token, userDetails, serviceAccountApprole.getSvcAccName())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Remove Approle from ServiceAccount").
						put(LogMessage.MESSAGE, "Failed to remove approle permission from Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove approle permission from Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove approle from Service Account").
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approleName+"\"}",token);
			String responseJson="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();
				ObjectMapper objMapper = new ObjectMapper();
				try {
					JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
					if (null != policiesArry) {
						for (JsonNode policyNode : policiesArry) {
							currentpolicies.add(policyNode.asText());
						}
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				//policies.remove(policy);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);

			}

			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove AppRole from Service account").
					put(LogMessage.MESSAGE, "Remove approle from Service account -  policy :" + policiesString + " is being configured" ).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			//Update the policy for approle
			Response approleControllerResp = appRoleService.configureApprole(approleName,policiesString,token);
			if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT) || approleControllerResp.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "app-roles");
				params.put("name",approleName);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove AppRole from Service Account").
							put(LogMessage.MESSAGE, "Approle is successfully removed from Service Account").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle is successfully removed from Service Account\"]}");
				}
				approleControllerResp = appRoleService.configureApprole(approleName,currentpoliciesString,token);
				if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove AppRole from Service Account").
							put(LogMessage.MESSAGE, "Reverting, approle policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove AppRole from Service Account").
							put(LogMessage.MESSAGE, "Reverting approle policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove approle from the Service Account\"]}");
			}
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove approle from Service Account\"]}");
		}
	}

	/**
	 * Add AWS role to Service Account
	 * @param userDetails
	 * @param token
	 * @param serviceAccountAWSRole
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToSvcacc(UserDetails userDetails, String token, ServiceAccountAWSRole serviceAccountAWSRole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Add AWS Role to Service Account").
				put(LogMessage.MESSAGE, "Trying to add AWS Role to Service Account").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if(!isSvcaccPermissionInputValid(serviceAccountAWSRole.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
		}
		if (serviceAccountAWSRole.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountAWSRole.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String roleName = serviceAccountAWSRole.getRolename();
		String svcAccName = serviceAccountAWSRole.getSvcAccName();
		String access = serviceAccountAWSRole.getAccess();

		roleName = (roleName !=null) ? roleName.toLowerCase() : roleName;
		access = (access != null) ? access.toLowerCase(): access;

		boolean isAuthorized = hasAddOrRemovePermission(userDetails, svcAccName, token);
		if(isAuthorized){
			if (!ifInitialPwdReset(token, userDetails, serviceAccountAWSRole.getSvcAccName())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Add AWS Role to Service Account").
						put(LogMessage.MESSAGE, "Failed to add awsrole permission to Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add awsrole permission to Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
			String policy = TVaultConstants.EMPTY;
			policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add AWS Role to Service Account").
					put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Add AWS Role to Service Account").
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));

			Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+roleName+"\"}",token);
			String responseJson="";
			String auth_type = TVaultConstants.EC2;
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			String policiesString = "";
			String currentpoliciesString = "";

			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();
				ObjectMapper objMapper = new ObjectMapper();
				try {
					JsonNode policiesArry =objMapper.readTree(responseJson).get("policies");
					for(JsonNode policyNode : policiesArry){
						currentpolicies.add(policyNode.asText());
					}
					auth_type = objMapper.readTree(responseJson).get("auth_type").asText();
				} catch (IOException e) {
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, "Add AWS Role to Service Account").
                            put(LogMessage.MESSAGE, e.getMessage()).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                            build()));
				}
				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
				policies.add(policy);
				policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
				currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			} else{
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"AWS role '"+roleName+"' does not exist. Please create the role and try again!\"]}");
			}
			Response awsRoleConfigresponse = null;
			if (TVaultConstants.IAM.equals(auth_type)) {
				awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,policiesString,token);
			}
			else {
				awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,policiesString,token);
			}
			if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "aws-roles");
				params.put("name",roleName);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add AWS Role to Service Account").
							put(LogMessage.MESSAGE, "AWS Role configuration Success.").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with Service Account\"]}");
				}
				if (TVaultConstants.IAM.equals(auth_type)) {
					awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,currentpoliciesString,token);
				}
				else {
					awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,currentpoliciesString,token);
				}
				if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add AWS Role to Service Account").
							put(LogMessage.MESSAGE, "Reverting, AWS Role policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
				} else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add AWS Role to Service Account").
							put(LogMessage.MESSAGE, "Reverting AWS Role policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Contact Admin \"]}");
				}
			} else{
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed. Try Again\"]}");
			}
		} else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add AWS Role to this service account\"]}");
		}

	}

	/**
	 * Remove AWS Role from service account
	 * @param userDetails
	 * @param token
	 * @param serviceAccountAWSRole
	 * @return
	 */
	public ResponseEntity<String> removeAWSRoleFromSvcacc(UserDetails userDetails, String token, ServiceAccountAWSRole serviceAccountAWSRole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Remove AWS Role from Service Account").
				put(LogMessage.MESSAGE, String.format ("Trying to remove AWS Role from Service Account [%s]", serviceAccountAWSRole.getRolename())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if(!isSvcaccPermissionInputValid(serviceAccountAWSRole.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
		}
		if (serviceAccountAWSRole.getAccess().equalsIgnoreCase("reset")) {
			serviceAccountAWSRole.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String roleName = serviceAccountAWSRole.getRolename();
		String svcAccName = serviceAccountAWSRole.getSvcAccName();
		String access = serviceAccountAWSRole.getAccess();

		roleName = (roleName !=null) ? roleName.toLowerCase() : roleName;
		access = (access != null) ? access.toLowerCase(): access;
		boolean isAuthorized = hasAddOrRemovePermission(userDetails, svcAccName, token);

		if (isAuthorized) {
			if (!ifInitialPwdReset(token, userDetails, serviceAccountAWSRole.getSvcAccName())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Remove AWSRole from ServiceAccount").
						put(LogMessage.MESSAGE, "Failed to remove awsrole permission from Service account. Initial password reset is pending for this Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove awsrole permission from Service account. Initial password reset is pending for this Service Account. Please reset the password and try again.\"]}");
			}
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove AWS Role from Service Account").
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", r_policy, w_policy, d_policy, o_policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));

			Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+roleName+"\"}",token);
			String responseJson="";
			String auth_type = TVaultConstants.EC2;
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();
				ObjectMapper objMapper = new ObjectMapper();
				try {
					JsonNode policiesArry =objMapper.readTree(responseJson).get("policies");
					for(JsonNode policyNode : policiesArry){
						currentpolicies.add(policyNode.asText());
					}
					auth_type = objMapper.readTree(responseJson).get("auth_type").asText();
				} catch (IOException e) {
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, "Remove AWS Role from Service Account").
                            put(LogMessage.MESSAGE, e.getMessage()).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                            build()));
				}
				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
			} else{
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"AppRole doesn't exist\"]}");
			}

			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove AWS Role from Service account").
					put(LogMessage.MESSAGE, "Remove AWS Role from Service account -  policy :" + policiesString + " is being configured" ).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			Response awsRoleConfigresponse = null;
			if (TVaultConstants.IAM.equals(auth_type)) {
				awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,policiesString,token);
			}
			else {
				awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,policiesString,token);
			}
			if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "aws-roles");
				params.put("name",roleName);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove AWS Role from Service Account").
							put(LogMessage.MESSAGE, "AWS Role configuration Success.").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from Service Account\"]}");
				}
				if (TVaultConstants.IAM.equals(auth_type)) {
					awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,currentpoliciesString,token);
				}
				else {
					awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,currentpoliciesString,token);
				}
				if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove AWS Role from Service Account").
							put(LogMessage.MESSAGE, "Reverting, AWS Role policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Remove AppRole from Service Account").
							put(LogMessage.MESSAGE, "Reverting approle policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Contact Admin \"]}");
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove AWS Role from the Service Account\"]}");
			}
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove AWS Role from Service Account\"]}");
		}
	}

	/**
	 * To create aws ec2 role
	 * @param userDetails
	 * @param token
	 * @param awsLoginRole
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createAWSRole(UserDetails userDetails, String token, AWSLoginRole awsLoginRole) throws TVaultValidationException {
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		return awsAuthService.createRole(token, awsLoginRole, userDetails);
	}

	/**
	 * Create aws iam role
	 * @param userDetails
	 * @param token
	 * @param awsiamRole
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createIAMRole(UserDetails userDetails, String token, AWSIAMRole awsiamRole) throws TVaultValidationException {
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		return awsiamAuthService.createIAMRole(awsiamRole, token, userDetails);
	}

	/**
	 * Update TTL for onboarded service account
	 * @param token
	 * @param serviceAccount
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> updateOnboardedServiceAccount(String token, ServiceAccount serviceAccount, UserDetails userDetails) {

		List<String> onboardedList = getOnboardedServiceAccountList(token, userDetails);

		if (!onboardedList.contains(serviceAccount.getName())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Update onboarded Service Account").
					put(LogMessage.MESSAGE, "Failed to update onboarded Service Account. Service account not onboarded").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to update onboarded Service Account. Please onboard this Service Account first and try again.\"]}");
		}

		log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Update onboarded Service Account").
				put(LogMessage.MESSAGE, String.format("Update onboarded Service Account [%s]", serviceAccount.getName())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		// get the maxPwdAge for this service account
		List<ADServiceAccount> allServiceAccounts = getADServiceAccount(serviceAccount.getName());
		if (allServiceAccounts == null || allServiceAccounts.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to update onboarded Service Account. Unable to read Service account details\"]}");
		}
		int maxPwdAge = allServiceAccounts.get(0).getMaxPwdAge();
        if (serviceAccount.isAutoRotate()) {
            if (null == serviceAccount.getMax_ttl()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid or no value has been provided for MAX_TTL\"]}");
            }
			if (null == serviceAccount.getTtl()) {
				serviceAccount.setTtl(maxPwdAge-0L);
			}
            if (serviceAccount.getTtl() > maxPwdAge) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, "Update onboarded Service Account").
                        put(LogMessage.MESSAGE, String.format ("Password Expiration Time [%s] is greater the Maximum expiration time (MAX_TTL) [%s]", serviceAccount.getTtl(), maxPwdAge)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                        build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value provided for Password Expiration Time. This can't be more than "+maxPwdAge+" for this Service Account\"]}");
            }
            if (serviceAccount.getTtl() > serviceAccount.getMax_ttl()) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, "Update onboarded Service Account").
                        put(LogMessage.MESSAGE, String.format ("Password Expiration Time [%s] is greater the Maximum expiration time (MAX_TTL) [%s]", serviceAccount.getTtl(), serviceAccount.getMax_ttl())).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Password Expiration Time must be less than Maximum expiration time (MAX_TTL) for this Service Account\"]}");
            }
        }
		if (!serviceAccount.isAutoRotate()) {
			serviceAccount.setTtl(TVaultConstants.MAX_TTL);
		}
		ResponseEntity<String> accountRoleDeletionResponse = createAccountRole(token, serviceAccount);
		if (accountRoleDeletionResponse!=null && HttpStatus.OK.equals(accountRoleDeletionResponse.getStatusCode())) {

			String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(serviceAccount.getName()).toString();
			Response metadataResponse = ControllerUtil.updateMetadataOnSvcUpdate(path, serviceAccount,token);
			if(metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "pdate onboarded Service Account").
						put(LogMessage.MESSAGE, "Successfully updated onboarded Service Account.").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully updated onboarded Service Account.\"]}");
			}
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully updated onboarded Service Account. However metadata update failed\"]}");

		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Update onboarded Service Account").
					put(LogMessage.MESSAGE, "Failed to update onboarded Service Account.").
					put(LogMessage.STATUS, accountRoleDeletionResponse!=null?accountRoleDeletionResponse.getStatusCode().toString():HttpStatus.MULTI_STATUS.toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"errors\":[\"Failed to update onboarded Service Account.\"]}");
		}
	}

	/**
	 * Get onboarded service account list
	 * @param token
	 * @param userDetails
	 * @return
	 */
	private List<String> getOnboardedServiceAccountList(String token, UserDetails userDetails) {
		ResponseEntity<String> onboardedResponse = getOnboardedServiceAccounts(token, userDetails);

		ObjectMapper objMapper = new ObjectMapper();
		List<String> onboardedList = new ArrayList<>();
		Map<String,String[]> requestMap = null;
		try {
			requestMap = objMapper.readValue(onboardedResponse.getBody(), new TypeReference<Map<String,String[]>>() {});
			if (requestMap != null && null != requestMap.get("keys")) {
				onboardedList = new ArrayList<>(Arrays.asList((String[]) requestMap.get("keys")));
			}
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Update onboarded Service Account").
					put(LogMessage.MESSAGE, String.format ("Error creating onboarded list [%s]", e.getMessage())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return onboardedList;
	}

	/**
	 * Change service account owner
	 * @param userDetails
	 * @param token
	 * @param svcAccName
	 * @return
	 */
	public ResponseEntity<String> transferSvcAccountOwner(UserDetails userDetails, String token, String svcAccName) {
		if (!userDetails.isAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied. No permission to transfer service account.\"]}");
		}
		boolean isSvcAccOwnerChanged = false;
		ServiceAccountMetadataDetails serviceAccountMetadataDetails = getServiceAccountMetadataDetails(token, userDetails, svcAccName);
		OnboardedServiceAccountDetails onbSvcAccDtls = getOnboarderdServiceAccountDetails(token, svcAccName);
		String oldOwner = serviceAccountMetadataDetails.getManagedBy();
		ADServiceAccount adServiceAccount = null;
		if (onbSvcAccDtls != null) {
			List<ADServiceAccount> allServiceAccounts = getADServiceAccount(svcAccName);
			if (!CollectionUtils.isEmpty(allServiceAccounts)) {
				adServiceAccount = allServiceAccounts.get(0);
				if (!oldOwner.equals(adServiceAccount.getOwner())) {
					isSvcAccOwnerChanged=true;
				}
			}
			else {
					return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"errors\":[\"Failed to transfer service account ownership. Unable to read Service account details\"]}");
			}
			if (isSvcAccOwnerChanged) {
				String svcOwner = adServiceAccount.getOwner();

				ServiceAccount serviceAccount = new ServiceAccount();
				serviceAccount.setName(svcAccName);
				serviceAccount.setTtl(onbSvcAccDtls.getTtl());
				serviceAccount.setMax_ttl(adServiceAccount.getMaxPwdAge()+0L);
				boolean autoRotate = false;
				if (onbSvcAccDtls.getTtl() <= adServiceAccount.getMaxPwdAge()) {
					autoRotate =  true;
				}
				serviceAccount.setAutoRotate(autoRotate);


				serviceAccount.setAdGroup(serviceAccountMetadataDetails.getAdGroup());
				serviceAccount.setAppName(serviceAccountMetadataDetails.getAppName());
				serviceAccount.setAppID(serviceAccountMetadataDetails.getAppID());
				serviceAccount.setAppTag(serviceAccountMetadataDetails.getAppTag());

				serviceAccount.setOwner(svcOwner);

				ResponseEntity<String> svcAccOwnerUpdateResponse = updateOnboardedServiceAccount(token, serviceAccount, userDetails);
				if (HttpStatus.OK.equals(svcAccOwnerUpdateResponse.getStatusCode())) {
					// Add sudo permission to new owner
					ServiceAccountUser serviceAccountNewOwner = new ServiceAccountUser(svcAccName, svcOwner, TVaultConstants.SUDO_POLICY);
					ResponseEntity<String> addOwnerSudoToServiceAccountResponse = addUserToServiceAccount(token, serviceAccountNewOwner, userDetails, true);

					// Add default reset permission to new owner. If initial password reset is not done, then reset permission will be added during initial reset.
					if (serviceAccountMetadataDetails.getInitialPasswordReset()) {
						serviceAccountNewOwner = new ServiceAccountUser(svcAccName, svcOwner, TVaultConstants.RESET_POLICY);
						ResponseEntity<String> addOwnerWriteToServiceAccountResponse = addUserToServiceAccount(token, serviceAccountNewOwner, userDetails, true);
					}

					removeOldUserPermissions(oldOwner, token, svcAccName, userDetails);

					if (HttpStatus.OK.equals(addOwnerSudoToServiceAccountResponse.getStatusCode())) {
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Service account ownership transferred successfully from " + oldOwner + " to " + svcOwner + ".\"]}");
					}
					else {
						return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Failed to transfer service account ownership. Adding new user to service account failed\"]}");
					}
				}
				else {
					return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Failed to transfer service account ownership. Update service account failed\"]}");
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Ownership transfer not required for this service account.\"]}");
			}
		}
		else {
			return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Failed to get service account details. Service account is not onboarded.\"]}");
		}
	}


	/**
	 * To remove old owner permissions and metadata
	 * @param userName
	 * @param token
	 * @param svcAccName
	 */
	private void removeOldUserPermissions(String userName, String token, String svcAccName, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		// Remove metadata directly as removeUserFromServiceAccount() will need refreshed token
		String path = new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append(svcAccName).toString();
		Map<String,String> params = new HashMap<String,String>();
		params.put("type", "users");
		params.put("name", userName);
		params.put("path",path);
		params.put("access", "delete");
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove old owner from ServiceAccount").
					put(LogMessage.MESSAGE, String.format("Owner %s is successfully removed from Service Account %s", userName, svcAccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		else {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Remove old owner from ServiceAccount").
					put(LogMessage.MESSAGE,String.format("Failed to remove Owner %s from Service Account %s", userName, svcAccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}

		// Remove old owner sudo and reset permissions
		ObjectMapper objMapper = new ObjectMapper();

		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}", token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
			userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC implementation changes
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, userDetails);
			if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
					log.error(
							JSONUtil.getJSON(
									ImmutableMap.<String, String> builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER)
															.toString())
											.put(LogMessage.ACTION, "removeUserFromSafe")
											.put(LogMessage.MESSAGE,
													String.format("Trying to fetch OIDC user policies, failed"))
											.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap()
													.get(LogMessage.APIURL).toString())
											.build()));
				}
			}
			oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
			oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
			userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
			userResponse.setHttpstatus(responseEntity.getStatusCode());
			}
		String responseJson = "";
		String groups = "";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if (HttpStatus.OK.equals(userResponse.getHttpstatus())) {
			responseJson = userResponse.getResponse();
			try {
				//OIDC changes
				if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					currentpolicies.addAll(oidcEntityResponse.getPolicies());
					//groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
				} else {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
					}
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "updateUserPolicyAssociationOnSvcaccDelete").
						put(LogMessage.MESSAGE, String.format("updateUserPolicyAssociationOnSvcaccDelete failed [%s]", e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			policies.addAll(currentpolicies);
			String r_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String w_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String d_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();
			String o_policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.SVC_ACC_PATH_PREFIX).append("_").append(svcAccName).toString();

			policies.remove(r_policy);
			policies.remove(w_policy);
			policies.remove(d_policy);
			policies.remove(o_policy);

			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "removeOldUserPermissions").
					put(LogMessage.MESSAGE, String.format("Current policies [%s]", policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "removeOldUserPermissions").
						put(LogMessage.MESSAGE, String.format("Current policies userpass [%s]", policies)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				ControllerUtil.configureUserpassUser(userName, policiesString, token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "removeOldUserPermissions").
						put(LogMessage.MESSAGE, String.format("Current policies ldap [%s]", policies)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				ControllerUtil.configureLDAPUser(userName, policiesString, groups, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "removeOldUserPermissions").
						put(LogMessage.MESSAGE, String.format("Current policies oidc [%s]", policies)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				// OIDC changes
				try {
					oidcUtil.updateOIDCEntity(policies,
							oidcEntityResponse.getEntityName());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				} catch (Exception e2) {
					log.error(e2);
					log.error(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "removeOldUserPermissions")
									.put(LogMessage.MESSAGE,
											String.format("Exception while updating the identity"))
									.put(LogMessage.STACKTRACE, Arrays.toString(e2.getStackTrace()))
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
				}
			}
		}
	}

	/**
	 * To get ServiceAccountMetadataDetails
	 *
	 * @param token
	 * @param userDetails
	 * @param svcAccName
	 * @return
	 */
	private ServiceAccountMetadataDetails getServiceAccountMetadataDetails(String token, UserDetails userDetails, String svcAccName) {
		String _path = TVaultConstants.SVC_ACC_ROLES_PATH + svcAccName;
		Response metaResponse = getMetadata(token, userDetails, _path);
		ServiceAccountMetadataDetails serviceAccountMetadataDetails = new ServiceAccountMetadataDetails();
		if (metaResponse !=null && metaResponse.getHttpstatus().equals(HttpStatus.OK)) {
			try {
				JsonNode jsonNode = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("adGroup");
				if (jsonNode != null) {
					serviceAccountMetadataDetails.setAdGroup(jsonNode.asText());
				}
				jsonNode = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("appID");
				if (jsonNode != null) {
					serviceAccountMetadataDetails.setAppID(jsonNode.asText());
				}
				jsonNode = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("appName");
				if (jsonNode != null) {
					serviceAccountMetadataDetails.setAppName(jsonNode.asText());
				}
				jsonNode = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("appTag");
				if (jsonNode != null) {
					serviceAccountMetadataDetails.setAppTag(jsonNode.asText());
				}
				jsonNode = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("managedBy");
				if (jsonNode != null) {
					serviceAccountMetadataDetails.setManagedBy(jsonNode.asText());
				}
				jsonNode = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("initialPasswordReset");
				if (jsonNode != null) {
					serviceAccountMetadataDetails.setInitialPasswordReset(Boolean.parseBoolean(jsonNode.asText()));
				}
				jsonNode = new ObjectMapper().readTree(metaResponse.getResponse()).get("data").get("name");
				if (jsonNode != null) {
					serviceAccountMetadataDetails.setName(jsonNode.asText());
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "getServiceAccountMetadataDetails").
						put(LogMessage.MESSAGE, String.format ("Failed to parse service account metadata [%s]", svcAccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
		}
		return serviceAccountMetadataDetails;
	}
	
	/**
	 * 
	 * @param userDetails
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getServiceAccounts(UserDetails userDetails, String userToken) {
		oidcUtil.renewUserToken(userDetails.getClientToken());
		String token = userDetails.getClientToken();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);

		policies = filterPoliciesBasedOnPrecedence(Arrays.asList(policies));

		List<Map<String, String>> svcListUsers = new ArrayList<>();
		Map<String, List<Map<String, String>>> safeList = new HashMap<>();
		if (policies != null) {
			for (String policy : policies) {
				Map<String, String> safePolicy = new HashMap<>();
				String[] _policies = policy.split("_", -1);
				if (_policies.length >= 3) {
					String[] policyName = Arrays.copyOfRange(_policies, 2, _policies.length);
					String safeName = String.join("_", policyName);
					String safeType = _policies[1];

					if (policy.startsWith("r_")) {
						safePolicy.put(safeName, "read");
					} else if (policy.startsWith("w_")) {
						safePolicy.put(safeName, "write");
					} else if (policy.startsWith("d_")) {
						safePolicy.put(safeName, "deny");
					}
					if (!safePolicy.isEmpty()) {
						if (safeType.equals(TVaultConstants.SVC_ACC_PATH_PREFIX)) {
							svcListUsers.add(safePolicy);
						} 
					}
				}
			}
			safeList.put(TVaultConstants.SVC_ACC_PATH_PREFIX, svcListUsers);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeList));
	}
	
	/**
	 * Filter service accounts policies based on policy precedence.
	 * @param policies
	 * @return
	 */
	private String [] filterPoliciesBasedOnPrecedence(List<String> policies) {
		List<String> filteredList = new ArrayList<>();
		for (int i = 0; i < policies.size(); i++ ) {
			String policyName = policies.get(i);
			String[] _policy = policyName.split("_", -1);
			if (_policy.length >= 3) {
				String itemName = policyName.substring(1);
				List<String> matchingPolicies = filteredList.stream().filter(p->p.substring(1).equals(itemName)).collect(Collectors.toList());
				if (!matchingPolicies.isEmpty()) {
					/* deny has highest priority. Read and write are additive in nature
						Removing all matching as there might be duplicate policies from user and groups
					*/
					if (policyName.startsWith("d_") || (policyName.startsWith("w_") && !matchingPolicies.stream().anyMatch(p-> p.equals("d"+itemName)))) {
						filteredList.removeAll(matchingPolicies);
						filteredList.add(policyName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("d"+itemName))) {
						// policy is read and deny already in the list. Then deny has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("d"+itemName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("w"+itemName))) {
						// policy is read and write already in the list. Then write has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("w"+itemName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("r"+itemName)) || matchingPolicies.stream().anyMatch(p-> p.equals("o"+itemName))) {
						// policy is read and read already in the list. Then remove all duplicates read and add single read permission for that servcie account.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("r"+itemName);
					}
				}
				else {
					filteredList.add(policyName);
				}
			}
		}
		return filteredList.toArray(new String[0]);
	}
}
