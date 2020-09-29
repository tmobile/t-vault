/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */

package com.tmobile.cso.vault.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

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
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.IAMServiceAccountConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.ADUserAccount;
import com.tmobile.cso.vault.api.model.AccessPolicy;
import com.tmobile.cso.vault.api.model.IAMSecrets;
import com.tmobile.cso.vault.api.model.IAMSecretsMetadata;
import com.tmobile.cso.vault.api.model.IAMServiceAccount;
import com.tmobile.cso.vault.api.model.IAMServiceAccountGroup;
import com.tmobile.cso.vault.api.model.IAMServiceAccountMetadataDetails;
import com.tmobile.cso.vault.api.model.IAMServiceAccountUser;
import com.tmobile.cso.vault.api.model.IAMSvccAccMetadata;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import com.tmobile.cso.vault.api.model.OIDCGroup;
import com.tmobile.cso.vault.api.model.OnboardedIAMServiceAccount;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.EmailUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

@Component
public class  IAMServiceAccountsService {

	@Value("${vault.port}")
	private String vaultPort;

	@Value("${ad.notification.fromemail}")
	private String supportEmail;

	@Value("${ad.notification.mail.subject}")
	private String subject;

	@Value("${ad.notification.mail.body.groupcontent}")
	private String mailAdGroupContent;

	private static Logger log = LogManager.getLogger(IAMServiceAccountsService.class);
	private static final String[] ACCESS_PERMISSIONS = { "read", IAMServiceAccountConstants.IAM_RESET_MSG_STRING, "deny", "sudo" };

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
	 * Onboard an IAM service account into TVault for password rotation
	 *
	 * @param token
	 * @param iamServiceAccount
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> onboardIAMServiceAccount(String token, IAMServiceAccount iamServiceAccount,
			UserDetails userDetails) {
		List<String> onboardedList = getOnboardedIAMServiceAccountList(token, userDetails);
		String iamSvcAccName = iamServiceAccount.getAwsAccountId() + "_" + iamServiceAccount.getUserName();
		if (onboardedList.contains(iamSvcAccName)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							"Failed to onboard IAM Service Account. IAM Service account is already onboarded")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Failed to onboard IAM Service Account. IAM Service account is already onboarded\"]}");
		}

		String iamSvccAccPath = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName;
		String iamSvccAccMetaPath = IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + iamSvcAccName;

		IAMServiceAccountMetadataDetails iamServiceAccountMetadataDetails = populateIAMSvcAccMetaData(
				iamServiceAccount);

		boolean accountRoleCreationResponse = createIAMServiceAccount(token, iamServiceAccountMetadataDetails,
				iamSvccAccPath);
		if (accountRoleCreationResponse) {
			// Create Metadata
			ResponseEntity<String> metadataCreationResponse = createIAMSvcAccMetadata(token,
					iamServiceAccountMetadataDetails, iamSvccAccMetaPath);
			if (HttpStatus.OK.equals(metadataCreationResponse.getStatusCode())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
						.put(LogMessage.MESSAGE, "Successfully created Metadata for the IAM Service Account")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
						.put(LogMessage.MESSAGE,
								"Successfully created Service Account. However creation of Metadata failed.")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(
						"{\"errors\":[\"Successfully created IAM Service Account. However creation of Metadata failed.\"]}");
			}

			return createIAMSvcAccPoliciesAndAddUserToAccount(token, iamServiceAccount, userDetails, iamSvcAccName);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, "Failed to onboard IAM service account into TVault for password rotation.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Failed to onboard IAM service account into TVault for password rotation.\"]}");
		}
	}

	/**
	 * @param token
	 * @param iamServiceAccount
	 * @param userDetails
	 * @param iamSvcAccName
	 * @return
	 */
	private ResponseEntity<String> createIAMSvcAccPoliciesAndAddUserToAccount(String token,
			IAMServiceAccount iamServiceAccount, UserDetails userDetails, String iamSvcAccName) {
		ResponseEntity<String> iamSvcAccPolicyCreationResponse = createIAMServiceAccountPolicies(token, iamSvcAccName);
		if (HttpStatus.OK.equals(iamSvcAccPolicyCreationResponse.getStatusCode())) {
			IAMServiceAccountUser iamServiceAccountUser = new IAMServiceAccountUser(iamServiceAccount.getUserName(),
					iamServiceAccount.getOwnerNtid(), TVaultConstants.SUDO_POLICY, iamServiceAccount.getAwsAccountId());
			ResponseEntity<String> addUserToIAMSvcAccResponse = addUserToIAMServiceAccount(token, userDetails,
					iamServiceAccountUser, true);
			if (HttpStatus.OK.equals(addUserToIAMSvcAccResponse.getStatusCode())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
						.put(LogMessage.MESSAGE,
								"Successfully completed onboarding of IAM service account into TVault for password rotation.")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

				sendMailToIAMSvcAccOwner(iamServiceAccount, iamSvcAccName);

				return ResponseEntity.status(HttpStatus.OK).body(
						"{\"messages\":[\"Successfully completed onboarding of IAM service account into TVault for password rotation.\"]}");
			} else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
						.put(LogMessage.MESSAGE,
								"Successfully created IAM Service Account and policies. However the association of owner information failed.")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(
						"{\"messages\":[\"Successfully created IAM Service Account and policies. However the association of owner information failed.\"]}");
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, "Failed to onboard IAM service account into TVault for password rotation.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			OnboardedIAMServiceAccount iamSvcAccToRevert = new OnboardedIAMServiceAccount(
					iamServiceAccount.getUserName(), iamServiceAccount.getAwsAccountId(),
					iamServiceAccount.getOwnerNtid());
			ResponseEntity<String> accountRoleDeletionResponse = deleteIAMSvcAccount(token, iamSvcAccToRevert);
			if (accountRoleDeletionResponse != null
					&& HttpStatus.OK.equals(accountRoleDeletionResponse.getStatusCode())) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
						"{\"errors\":[\"Failed to onboard IAM service account into TVault for password rotation.\"]}");
			} else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
						"{\"errors\":[\"Failed to create IAM Service Account policies. Revert IAM service account creation failed.\"]}");
			}
		}
	}

	/**
	 * @param iamServiceAccount
	 * @param iamSvcAccName
	 */
	private void sendMailToIAMSvcAccOwner(IAMServiceAccount iamServiceAccount, String iamSvcAccName) {
		// send email notification to service account owner
		// get service account owner email
		String filterQuery = "(&(objectclass=user)(|(cn=" + iamServiceAccount.getOwnerNtid() + ")))";
		List<ADUserAccount> managerDetails = getServiceAccountManagerDetails(filterQuery);
		if (!managerDetails.isEmpty() && !StringUtils.isEmpty(managerDetails.get(0).getUserEmail())) {
			String from = supportEmail;
			List<String> to = new ArrayList<>();
			to.add(managerDetails.get(0).getUserEmail());
			String mailSubject = String.format(subject, iamSvcAccName);

			// set template variables
			Map<String, String> mailTemplateVariables = new HashMap<>();
			mailTemplateVariables.put("name", managerDetails.get(0).getDisplayName());
			mailTemplateVariables.put("iamSvcAccName", iamSvcAccName);

			mailTemplateVariables.put("contactLink", supportEmail);
			emailUtils.sendHtmlEmalFromTemplate(from, to, mailSubject, mailTemplateVariables);
		}
	}

	/**
	 * To create Metadata for the IAM Service Account
	 *
	 * @param token
	 * @param iamServiceAccountMetadata
	 * @return
	 */
	private ResponseEntity<String> createIAMSvcAccMetadata(String token,
			IAMServiceAccountMetadataDetails iamServiceAccountMetadata, String iamSvccAccMetaPath) {

		IAMSvccAccMetadata iamSvccAccMetadata = new IAMSvccAccMetadata(iamSvccAccMetaPath, iamServiceAccountMetadata);

		String jsonStr = JSONUtil.getJSON(iamSvccAccMetadata);
		Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path", iamSvccAccMetaPath);
		String iamSvcAccDataJson = ControllerUtil.convetToJson(rqstParams);

		boolean iamSvcAccMetaDataCreationStatus = ControllerUtil.createMetadata(iamSvcAccDataJson, token);
		if (iamSvcAccMetaDataCreationStatus) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "createIAMSvcAccMetadata")
					.put(LogMessage.MESSAGE,
							String.format("Successfully created metadata for the IAM Service Account [%s]",
									iamServiceAccountMetadata.getUserName()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created Metadata for the IAM Service Account\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "createIAMSvcAccMetadata")
					.put(LogMessage.MESSAGE, "Unable to create Metadata for the IAM Service Account")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Failed to create Metadata for the IAM Service Account\"]}");
	}

	/**
	 * method to populate IAMServiceAccountMetadataDetails object
	 *
	 * @param iamServiceAccount
	 * @return
	 */
	private IAMServiceAccountMetadataDetails populateIAMSvcAccMetaData(IAMServiceAccount iamServiceAccount) {

		IAMServiceAccountMetadataDetails iamServiceAccountMetadataDetails = new IAMServiceAccountMetadataDetails();
		List<IAMSecretsMetadata> iamSecretsMetadatas = new ArrayList<>();
		iamServiceAccountMetadataDetails.setUserName(iamServiceAccount.getUserName());
		iamServiceAccountMetadataDetails.setAwsAccountId(iamServiceAccount.getAwsAccountId());
		iamServiceAccountMetadataDetails.setAwsAccountName(iamServiceAccount.getAwsAccountName());
		iamServiceAccountMetadataDetails.setApplicationId(iamServiceAccount.getApplicationId());
		iamServiceAccountMetadataDetails.setApplicationName(iamServiceAccount.getApplicationName());
		iamServiceAccountMetadataDetails.setApplicationTag(iamServiceAccount.getApplicationTag());
		iamServiceAccountMetadataDetails.setCreatedAtEpoch(iamServiceAccount.getCreatedAtEpoch());
		iamServiceAccountMetadataDetails.setOwnerEmail(iamServiceAccount.getOwnerEmail());
		iamServiceAccountMetadataDetails.setOwnerNtid(iamServiceAccount.getOwnerNtid());

		for (IAMSecrets iamSecrets : iamServiceAccount.getSecret()) {
			IAMSecretsMetadata iamSecretsMetadata = new IAMSecretsMetadata();
			iamSecretsMetadata.setAccessKeyId(iamSecrets.getAccessKeyId());
			iamSecretsMetadata.setExpiryDuration(iamSecrets.getExpiryDuration());
			iamSecretsMetadatas.add(iamSecretsMetadata);
		}
		iamServiceAccountMetadataDetails.setSecret(iamSecretsMetadatas);

		return iamServiceAccountMetadataDetails;
	}

	/**
	 * Method to create IAM service account metadata in iamsvcacc mount
	 *
	 * @param iamServiceAccount
	 * @param token
	 * @param iamSvccAccPath
	 * @return
	 */
	private boolean createIAMServiceAccount(String token, IAMServiceAccountMetadataDetails iamServiceAccount,
			String iamSvccAccPath) {
		IAMSvccAccMetadata iamSvccAccMetadata = new IAMSvccAccMetadata(iamSvccAccPath, iamServiceAccount);
		String jsonStr = JSONUtil.getJSON(iamSvccAccMetadata);
		Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path", iamSvccAccPath);
		String iamSvcAccDataJson = ControllerUtil.convetToJson(rqstParams);

		Response response = reqProcessor.process("/write", iamSvcAccDataJson, token);

		boolean isIamSvcAccDataCreated = false;

		if (response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, "IAM Service account metadata creation success for policy creation")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			isIamSvcAccDataCreated = true;
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, "IAM Service account metadata creation failed for policy creation")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return isIamSvcAccDataCreated;
	}

	/**
	 * Create policies for IAM service account
	 *
	 * @param token
	 * @param iamSvcAccName
	 * @return
	 */
	private ResponseEntity<String> createIAMServiceAccountPolicies(String token, String iamSvcAccName) {
		int succssCount = 0;
		for (String policyPrefix : TVaultConstants.getSvcAccPolicies().keySet()) {
			AccessPolicy accessPolicy = new AccessPolicy();
			String accessId = new StringBuffer().append(policyPrefix)
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccName).toString();
			accessPolicy.setAccessid(accessId);
			HashMap<String, String> accessMap = new HashMap<>();

			// Attaching write permissions for owner
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.SUDO_POLICY)) {
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName,
						TVaultConstants.WRITE_POLICY);
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + iamSvcAccName,
						TVaultConstants.WRITE_POLICY);
			}
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.WRITE_POLICY)) {
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName,
						TVaultConstants.WRITE_POLICY);
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + iamSvcAccName,
						TVaultConstants.WRITE_POLICY);
			}
			accessPolicy.setAccess(accessMap);
			ResponseEntity<String> policyCreationStatus = accessService.createPolicy(token, accessPolicy);
			if (HttpStatus.OK.equals(policyCreationStatus.getStatusCode())) {
				succssCount++;
			}
		}
		if (succssCount == TVaultConstants.getSvcAccPolicies().size()) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "createIAMServiceAccountPolicies")
					.put(LogMessage.MESSAGE, "Successfully created policies for IAM service account.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created policies for IAM service account\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "createIAMServiceAccountPolicies")
				.put(LogMessage.MESSAGE, "Failed to create some of the policies for IAM service account.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return ResponseEntity.status(HttpStatus.MULTI_STATUS)
				.body("{\"messages\":[\"Failed to create some of the policies for IAM service account\"]}");
	}

	/**
	 * Deletes the IAMSvcAccount
	 *
	 * @param token
	 * @param iamServiceAccount
	 * @return
	 */
	private ResponseEntity<String> deleteIAMSvcAccount(String token, OnboardedIAMServiceAccount iamServiceAccount) {
		String iamSvcAccName = iamServiceAccount.getAwsAccountId() + "_" + iamServiceAccount.getUserName();
		String iamSvcAccPath = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName;
		Response onboardingResponse = reqProcessor.process("/delete", "{\"path\":\"" + iamSvcAccPath + "\"}", token);

		if (onboardingResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| onboardingResponse.getHttpstatus().equals(HttpStatus.OK)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteIAMSvcAccount")
					.put(LogMessage.MESSAGE, "Successfully deleted IAM service account.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully deleted IAM service account.\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteIAMSvcAccount")
					.put(LogMessage.MESSAGE, "Failed to delete IAM service account.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Failed to delete IAM service account.\"]}");
		}
	}

	/**
	 * To get list of iam service accounts
	 *
	 * @param token
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getOnboardedIAMServiceAccounts(String token, UserDetails userDetails) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "listOnboardedIAMServiceAccounts")
				.put(LogMessage.MESSAGE, "Trying to get list of onboaded IAM service accounts")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response response = null;

		String[] latestPolicies = policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(),
				userDetails.getUsername(), userDetails);
		List<String> onboardedlist = new ArrayList<>();
		for (String policy : latestPolicies) {
			if (policy.startsWith("o_iamsvcacc")) {
				onboardedlist.add(policy.substring(12));
			}
		}
		response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse("{\"keys\":" + JSONUtil.getJSON(onboardedlist) + "}");

		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "listOnboardedIAMServiceAccounts")
					.put(LogMessage.MESSAGE, "Successfully retrieved the list of IAM Service Accounts")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		} else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"keys\":[]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Validates IAM Service Account permission inputs
	 *
	 * @param access
	 * @return
	 */
	public static boolean isIamSvcaccPermissionInputValid(String access) {
		boolean isValidAccess = true;
		if (!org.apache.commons.lang3.ArrayUtils.contains(ACCESS_PERMISSIONS, access)) {
			isValidAccess = false;
		}
		return isValidAccess;
	}

	/**
	 * Get Manager details for service account
	 *
	 * @param filter
	 * @return
	 */
	private List<ADUserAccount> getServiceAccountManagerDetails(String filter) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "getServiceAccountManagerDetails")
				.put(LogMessage.MESSAGE, "Trying to get manager details")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
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
	 * Get onboarded IAM service account list
	 *
	 * @param token
	 * @param userDetails
	 * @return
	 */
	private List<String> getOnboardedIAMServiceAccountList(String token, UserDetails userDetails) {
		ResponseEntity<String> onboardedResponse = getOnboardedIAMServiceAccounts(token, userDetails);

		ObjectMapper objMapper = new ObjectMapper();
		List<String> onboardedList = new ArrayList<>();
		Map<String, String[]> requestMap = null;
		try {
			requestMap = objMapper.readValue(onboardedResponse.getBody(), new TypeReference<Map<String, String[]>>() {
			});
			if (requestMap != null && null != requestMap.get("keys")) {
				onboardedList = new ArrayList<>(Arrays.asList(requestMap.get("keys")));
			}
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "get onboarded IAM Service Account list")
					.put(LogMessage.MESSAGE, String.format("Error creating onboarded list [%s]", e.getMessage()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return onboardedList;
	}

	/**
	 * Add Group to IAM Service Account
	 *
	 * @param token
	 * @param iamServiceAccountGroup
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> addGroupToIAMServiceAccount(String token,
			IAMServiceAccountGroup iamServiceAccountGroup, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, "Trying to add Group to IAM Service Account")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		if (!isIamSvcaccPermissionInputValid(iamServiceAccountGroup.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
		}
		if (iamServiceAccountGroup.getAccess().equalsIgnoreCase("reset")) {
			iamServiceAccountGroup.setAccess(TVaultConstants.WRITE_POLICY);
		}

		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		}

		String iamSvcAccountName = iamServiceAccountGroup.getAwsAccountId() + "_"
				+ iamServiceAccountGroup.getIamSvcAccName();

		boolean canAddGroup = hasAddOrRemovePermissionForUser(userDetails, iamSvcAccountName, token);
		if (canAddGroup) {
			// Only Sudo policy can be added (as part of onbord) before activation.
			if (!isIAMSvcaccActivated(token, userDetails, iamSvcAccountName)
					&& !TVaultConstants.SUDO_POLICY.equals(iamServiceAccountGroup.getAccess())) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE, String.format(
										"Failed to add group permission to IAM Service account. [%s] is not activated.",
										iamSvcAccountName))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to add group permission to IAM Service account. IAM Service Account is not activated. Please activate this service account and try again.\"]}");
			}

			return processAndAddGroupPoliciesToIAMSvcAcc(token, userDetails, oidcGroup, iamServiceAccountGroup,
					iamSvcAccountName);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to add groups to this IAM service account\"]}");
		}
	}

	/**
	 * @param token
	 * @param userDetails
	 * @param oidcGroup
	 * @param groupName
	 * @param iamSvcAccName
	 * @param access
	 * @return
	 */
	private ResponseEntity<String> processAndAddGroupPoliciesToIAMSvcAcc(String token, UserDetails userDetails,
			OIDCGroup oidcGroup, IAMServiceAccountGroup iamServiceAccountGroup, String iamSvcAccountName) {
		String policy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(iamServiceAccountGroup.getAccess()))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("policy is [%s]", policy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		String readPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();
		String writePolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();
		String denyPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();
		String sudoPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy,
								writePolicy, denyPolicy, sudoPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response groupResp = new Response();

		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			groupResp = reqProcessor.process("/auth/ldap/groups",
					"{\"groupname\":\"" + iamServiceAccountGroup.getGroupname() + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// call read api with groupname
			oidcGroup = oidcUtil.getIdentityGroupDetails(iamServiceAccountGroup.getGroupname(), token);
			if (oidcGroup != null) {
				groupResp.setHttpstatus(HttpStatus.OK);
				groupResp.setResponse(oidcGroup.getPolicies().toString());
			} else {
				groupResp.setHttpstatus(HttpStatus.BAD_REQUEST);
			}
		}

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("userResponse status is [%s]", groupResp.getHttpstatus()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String responseJson = "";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if (HttpStatus.OK.equals(groupResp.getHttpstatus())) {
			responseJson = groupResp.getResponse();
			try {
				ObjectMapper objMapper = new ObjectMapper();
				// OIDC Changes
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && !ObjectUtils.isEmpty(oidcGroup)) {
					currentpolicies.addAll(oidcGroup.getPolicies());
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
						.put(LogMessage.MESSAGE, "Exception while creating currentpolicies")
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}

			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
			policies.add(policy);
		} else {
			// New group to be configured
			policies.add(policy);
		}
		return configureGroupAndUpdateMetadataForIAMSvcAcc(token, userDetails, oidcGroup, iamServiceAccountGroup,
				policies, currentpolicies, iamSvcAccountName);
	}

	/**
	 * @param token
	 * @param userDetails
	 * @param oidcGroup
	 * @param groupName
	 * @param iamSvcAccName
	 * @param access
	 * @param policies
	 * @param currentpolicies
	 * @return
	 */
	private ResponseEntity<String> configureGroupAndUpdateMetadataForIAMSvcAcc(String token, UserDetails userDetails,
			OIDCGroup oidcGroup, IAMServiceAccountGroup iamServiceAccountGroup, List<String> policies,
			List<String> currentpolicies, String iamSvcAccountName) {
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("policies [%s] before calling configureLDAPGroup", policies))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		Response ldapConfigresponse = new Response();
		// OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPGroup(iamServiceAccountGroup.getGroupname(),
					policiesString, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ldapConfigresponse = oidcUtil.updateGroupPolicies(token, iamServiceAccountGroup.getGroupname(), policies,
					currentpolicies, oidcGroup != null ? oidcGroup.getId() : null);
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}
		if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
			String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(iamSvcAccountName)
					.toString();
			Map<String, String> params = new HashMap<>();
			params.put("type", "groups");
			params.put("name", iamServiceAccountGroup.getGroupname());
			params.put("path", path);
			params.put("access", iamServiceAccountGroup.getAccess());

			Response metadataResponse = ControllerUtil.updateMetadata(params, token);
			if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
					|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
						.put(LogMessage.MESSAGE, "Group configuration Success.")
						.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"Group is successfully associated with IAM Service Account\"]}");
			} else {
				return revertGroupPermissionForIAMSvcAcc(token, userDetails, oidcGroup,
						iamServiceAccountGroup.getGroupname(), currentpolicies, currentpoliciesString,
						metadataResponse);
			}
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to add group to the IAM Service Account\"]}");
		}
	}

	/**
	 * @param token
	 * @param userDetails
	 * @param oidcGroup
	 * @param groupName
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> revertGroupPermissionForIAMSvcAcc(String token, UserDetails userDetails,
			OIDCGroup oidcGroup, String groupName, List<String> currentpolicies, String currentpoliciesString,
			Response metadataResponse) {
		Response ldapRevertConfigresponse = new Response();
		// OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapRevertConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && !ObjectUtils.isEmpty(oidcGroup)) {
			ldapRevertConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, currentpolicies, currentpolicies,
					oidcGroup.getId());
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}
		if (ldapRevertConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE, "Reverting, group policy update success")
					.put(LogMessage.RESPONSE,
							(null != metadataResponse) ? metadataResponse.getResponse() : TVaultConstants.EMPTY)
					.put(LogMessage.STATUS,
							(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
									: TVaultConstants.EMPTY)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE, "Reverting group policy update failed")
					.put(LogMessage.RESPONSE,
							(null != metadataResponse) ? metadataResponse.getResponse() : TVaultConstants.EMPTY)
					.put(LogMessage.STATUS,
							(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
									: TVaultConstants.EMPTY)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
		}
	}

	/**
	 * Check if user has the permission to add user/group/approles to the IAM
	 * Service Account
	 *
	 * @param userDetails
	 * @param action
	 * @param token
	 * @return
	 */
	public boolean hasAddOrRemovePermissionForUser(UserDetails userDetails, String iamServiceAccount, String token) {
		boolean hasPermission = false;
		// Owner of the IAM service account can add/remove users, groups and approles to
		// IAM service account
		String sudoPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamServiceAccount).toString();
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);
		if (ArrayUtils.contains(policies, sudoPolicy)) {
			hasPermission = true;
		}
		return hasPermission;
	}

	/**
	 * Add user to IAM Service account.
	 *
	 * @param token
	 * @param userDetails
	 * @param iamServiceAccountUser
	 * @param isPartOfOnboard
	 * @return
	 */
	public ResponseEntity<String> addUserToIAMServiceAccount(String token, UserDetails userDetails,
			IAMServiceAccountUser iamServiceAccountUser, boolean isPartOfOnboard) {

		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Trying to add user to ServiceAccount [%s]",
								iamServiceAccountUser.getIamSvcAccName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		if (!isIamSvcaccPermissionInputValid(iamServiceAccountUser.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
		}
		if (iamServiceAccountUser.getAccess().equalsIgnoreCase("reset")) {
			iamServiceAccountUser.setAccess(TVaultConstants.WRITE_POLICY);
		}

		String userName = iamServiceAccountUser.getUsername().toLowerCase();
		String iamSvcAccName = iamServiceAccountUser.getIamSvcAccName().toLowerCase();
		String access = iamServiceAccountUser.getAccess().toLowerCase();
		String uniqueIAMSvcaccName = iamServiceAccountUser.getAwsAccountId() + "_" + iamSvcAccName;

		boolean isAuthorized = true;
		if (userDetails != null) {
			isAuthorized = hasGrantPermissionForIAMSvcAcc(userDetails, uniqueIAMSvcaccName, token, isPartOfOnboard);
		}

		if (isAuthorized) {
			// Only Sudo policy can be added (as part of onbord) before activation.
			if (!isIAMSvcaccActivated(token, userDetails, uniqueIAMSvcaccName)
					&& !TVaultConstants.SUDO_POLICY.equals(access)) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE, String.format(
										"Failed to add user permission to IAM Service account. [%s] is not activated.",
										iamSvcAccName))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to add user permission to IAM Service account. Service Account is not activated. Please activate this service account and try again.\"]}");
			}

			String policy;
			policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE,
							String.format("Trying to [%s] policy to user [%s] for the IAM service account [%s]", policy,
									userName, iamSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccName).toString();
			String ownerPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE,
							String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]",
									readPolicy, writePolicy, denyPolicy, ownerPolicy))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			Response userResponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}",
						token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}", token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// OIDC implementation changes
				ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName,
						userDetails);
				if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
					if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE, String.format("Failed to fetch OIDC user for [%s]", userName))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
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

			String responseJson = "";
			String groups = "";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if (HttpStatus.OK.equals(userResponse.getHttpstatus())) {
				responseJson = userResponse.getResponse();
				try {
					ObjectMapper objMapper = new ObjectMapper();
					// OIDC Changes
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
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
							.put(LogMessage.MESSAGE,
									String.format("Exception while creating currentpolicies or groups for [%s]",
											userName))
							.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
				}

				policies.addAll(currentpolicies);
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);
				policies.add(policy);
			} else {
				// New user to be configured
				policies.add(policy);
			}
			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE, String.format("Policies [%s] before calling configure user", policies))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			Response userConfigresponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				userConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				userConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// OIDC Implementation : Entity Update
				try {
					userConfigresponse = oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				} catch (Exception e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
							.put(LogMessage.MESSAGE,
									String.format("Exception while adding or updating the identity for entity [%s]",
											oidcEntityResponse.getEntityName()))
							.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
				}
			}

			if (userConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
					|| userConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
				// User has been associated with IAM Service Account. Now metadata has to be
				// created
				String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(uniqueIAMSvcaccName)
						.toString();
				Map<String, String> params = new HashMap<>();
				params.put("type", "users");
				params.put("name", userName);
				params.put("path", path);
				params.put("access", access);
				Response metadataResponse = ControllerUtil.updateMetadata(params, token);
				if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
						|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
					log.debug(
							JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE, String.format(
											"User [%s] is successfully associated with IAM Service Account - [%s]",
											userName, iamSvcAccName))
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					return ResponseEntity.status(HttpStatus.OK)
							.body("{\"messages\":[\"Successfully added user to the IAM Service Account\"]}");
				} else {
					// Revert the user association when metadata fails...
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
							.put(LogMessage.MESSAGE,
									"Metadata creation for user association with service account failed. Reverting user association")
							.put(LogMessage.APIURL,
									ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
							.build()));
					if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
						userConfigresponse = ControllerUtil.configureUserpassUser(userName, currentpoliciesString,
								token);
					} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						userConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString, groups,
								token);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						// OIDC changes
						try {
							userConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies,
									oidcEntityResponse.getEntityName());
							oidcUtil.renewUserToken(userDetails.getClientToken());
						} catch (Exception e) {
							log.error(e);
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE,
											String.format(
													"Exception while adding or updating the identity for entity [%s]",
													oidcEntityResponse.getEntityName()))
									.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
						}
					}
					if (userConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
							|| userConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
								"{\"messages\":[\"Failed to add user to the Service Account. Metadata update failed\"]}");
					} else {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
								.body("{\"messages\":[\"Failed to revert user association on IAM Service Account\"]}");
					}
				}
			} else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body("{\"errors\":[\"Failed to add user to the IAM Service Account\"]}");
			}

		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("{\"errors\":[\"Access denied: No permission to add users to this IAM service account\"]}");
		}
	}

	/**
	 * Get metadata for IAM service account.
	 *
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
			path = path.substring(0, path.length() - 1);
		}
		String iamMetaDataPath = "metadata/" + path;
		return reqProcessor.process("/sdb", "{\"path\":\"" + iamMetaDataPath + "\"}", token);
	}

	/**
	 * To check if the IAM service account is activated.
	 *
	 * @param token
	 * @param userDetails
	 * @param iamSvcAccName
	 * @return
	 */
	private boolean isIAMSvcaccActivated(String token, UserDetails userDetails, String iamSvcAccName) {
		String iamAccPath = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName;
		boolean activationStatus = false;
		Response metaResponse = getMetadata(token, userDetails, iamAccPath);
		if (metaResponse != null && HttpStatus.OK.equals(metaResponse.getHttpstatus())) {
			try {
				JsonNode status = new ObjectMapper().readTree(metaResponse.getResponse()).get("data")
						.get("isActivated");
				if (status != null) {
					activationStatus = Boolean.parseBoolean(status.asText());
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "isIAMSvcaccActivated")
						.put(LogMessage.MESSAGE,
								String.format("Failed to get Activation status for the IAM Service account [%s]",
										iamSvcAccName))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "isIAMSvcaccActivated")
					.put(LogMessage.MESSAGE,
							String.format("Metadata not found for IAM Service account [%s]", iamSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return activationStatus;
	}

	/**
	 * Check if user has the permission to add user to the IAM Service Account.
	 *
	 * @param userDetails
	 * @param serviceAccount
	 * @param access
	 * @param token
	 * @return
	 */
	public boolean hasGrantPermissionForIAMSvcAcc(UserDetails userDetails, String serviceAccount, String token,
			boolean isPartOfOnboard) {
		// IAM admin users can add sudo policy for owner while onboarding the service
		// account
		if (userDetails.isAdmin() && isPartOfOnboard) {
			return true;
		}
		boolean hasAccess = false;
		// Owner of the service account can add/remove users, groups, aws roles and
		// approles to service account
		String ownerPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(serviceAccount).toString();
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);
		if (ArrayUtils.contains(policies, ownerPolicy)) {
			hasAccess = true;
		}
		return hasAccess;
	}
}
