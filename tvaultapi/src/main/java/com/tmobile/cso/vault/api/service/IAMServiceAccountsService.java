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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.IAMServiceAccountConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import com.tmobile.cso.vault.api.model.*;

import com.tmobile.cso.vault.api.utils.EmailUtils;
import com.tmobile.cso.vault.api.utils.IAMServiceAccountUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;


@Component
public class  IAMServiceAccountsService {

	@Value("${ad.notification.fromemail}")
	private String supportEmail;

	@Value("${iamPortal.auth.masterPolicy}")
	private String iamMasterPolicyName;

	private static Logger log = LogManager.getLogger(IAMServiceAccountsService.class);
	private static final String[] ACCESS_PERMISSIONS = { "read", IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING, "deny", "sudo" };

	@Autowired
	private AccessService accessService;

	@Autowired
	private RequestProcessor reqProcessor;

	@Autowired
	private PolicyUtils policyUtils;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	@Autowired
	private TokenUtils tokenUtils;

	@Autowired
	private EmailUtils emailUtils;

	@Autowired
	private OIDCUtil oidcUtil;

	@Autowired
	IAMServiceAccountUtils iamServiceAccountUtils;

	@Autowired
	private AppRoleService appRoleService;

	@Autowired
    private DirectoryService directoryService;
	
	@Autowired
	private AWSAuthService awsAuthService;
	
	@Autowired
	private AWSIAMAuthService awsiamAuthService;
	
	private static final String ACCOUNTSTR = "account [%s].";
	private static final String DELETEPATH = "/delete";
	private static final String PATHSTR = "{\"path\":\"";
	private static final String INVALIDVALUEERROR = "{\"errors\":[\"Invalid value specified for access. Valid values are read, rotate, deny\"]}";
	private static final String POLICYSTR = "policy is [%s]";
	private static final String GROUPPATH = "/auth/ldap/groups";
	private static final String GROUPNAME = "{\"groupname\":\"";
	private static final String READPATH = "/auth/userpass/read";
	private static final String USERPATH = "/auth/ldap/users";

	/**
	 * Onboard an IAM service account
	 *
	 * @param token
	 * @param iamServiceAccount
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> onboardIAMServiceAccount(String token, IAMServiceAccount iamServiceAccount,
			UserDetails userDetails) {

		if (!isAuthorizedForIAMOnboardAndOffboard(token)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							"Access denied. Not authorized to perform onboarding for IAM service accounts.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Access denied. Not authorized to perform onboarding for IAM service accounts.\"]}");
		}
		iamServiceAccount.setUserName(iamServiceAccount.getUserName().toLowerCase());
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

		String iamSvccAccMetaPath = IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + iamSvcAccName;

		IAMServiceAccountMetadataDetails iamServiceAccountMetadataDetails = constructIAMSvcAccMetaData(
				iamServiceAccount);

		// Create Metadata
		ResponseEntity<String> metadataCreationResponse = createIAMSvcAccMetadata(token,
				iamServiceAccountMetadataDetails, iamSvccAccMetaPath);
		if (HttpStatus.OK.equals(metadataCreationResponse.getStatusCode())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Successfully created Metadata for the IAM Service Account [%s]", iamSvccAccMetaPath))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							String.format("Creating metadata for IAM Service Account [%s] failed.", iamSvccAccMetaPath))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(
					"{\"errors\":[\"Metadata creation failed for IAM Service Account.\"]}");
		}

		// Create policies
		boolean iamSvcAccOwnerPermissionAddStatus = createIAMSvcAccPolicies(token, iamServiceAccount, userDetails, iamSvcAccName);
		if (iamSvcAccOwnerPermissionAddStatus) {
			// Add sudo permission to owner
			boolean iamSvcAccCreationStatus = addSudoPermissionToOwner(token, iamServiceAccount, userDetails, iamSvcAccName);
			if (iamSvcAccCreationStatus) {
				sendMailToIAMSvcAccOwner(iamServiceAccount, iamSvcAccName);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
						.put(LogMessage.MESSAGE, String.format("Successfully onboarded the IAM Service Account[%s]  with owner [%s].", iamServiceAccount.getUserName(),iamServiceAccount.getOwnerEmail()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.OK).body(
						"{\"messages\":[\"Successfully completed onboarding of IAM service account\"]}");
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, "Failed to onboard IAM service account. Owner association failed.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return rollBackIAMOnboardOnFailure(iamServiceAccount, iamSvcAccName, "onOwnerAssociationFailure");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE, "Failed to onboard IAM service account. Policy creation failed.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return rollBackIAMOnboardOnFailure(iamServiceAccount, iamSvcAccName, "onPolicyFailure");

	}

	/**
	 * Method to add Sudo permission to owner as part of IAM onboarding.
	 * @param token
	 * @param iamServiceAccount
	 * @param userDetails
	 * @param iamSvcAccName
	 * @return
	 */
	private boolean addSudoPermissionToOwner(String token, IAMServiceAccount iamServiceAccount, UserDetails userDetails,
											 String iamSvcAccName) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE, String.format("Trying to add sudo permission for the service account [%s] to " +
						"the user {%s]", iamSvcAccName, iamServiceAccount.getOwnerNtid()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		IAMServiceAccountUser iamServiceAccountUser = new IAMServiceAccountUser(iamServiceAccount.getUserName(),
				iamServiceAccount.getOwnerNtid(), TVaultConstants.SUDO_POLICY, iamServiceAccount.getAwsAccountId());
		//Add sudo permisson to the IAM service account owner
		ResponseEntity<String> addUserToIAMSvcAccResponse = addUserToIAMServiceAccount(token, userDetails,
				iamServiceAccountUser, true);
		if (HttpStatus.OK.equals(addUserToIAMSvcAccResponse.getStatusCode())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Successfully added owner permission to [%s] for IAM service " +
							ACCOUNTSTR, iamServiceAccount.getOwnerNtid(), iamSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return true;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE, String.format("Failed to add owner permission to [%s] for IAM service " +
						ACCOUNTSTR, iamServiceAccount.getOwnerNtid(), iamSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;

	}

	/**
	 * To check if the user/token has permission for onboarding or offboarding IAM service account.
	 * @param token
	 * @return
	 */
	private boolean isAuthorizedForIAMOnboardAndOffboard(String token) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> currentPolicies = new ArrayList<>();
		Response response = reqProcessor.process("/auth/tvault/lookup","{}", token);
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();
			try {
				currentPolicies = iamServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(objectMapper, responseJson);
				if (currentPolicies.contains(iamMasterPolicyName)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "IsAuthorizedForIAMOnboardAndOffboard")
							.put(LogMessage.MESSAGE, "The User/Token has required policies to onboard/offboard IAM Service Account.")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
					return true;
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "isAuthorizedForIAMOnboardAndOffboard")
						.put(LogMessage.MESSAGE,
								"Failed to parse policies from token")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "isAuthorizedForIAMOnboardAndOffboard")
				.put(LogMessage.MESSAGE, "The User/Token does not have required policies to onboard/offboard IAM Service Account.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;
	}

	/**
	 * Method to create IAM service account policies as part of IAM service account onboarding.
	 * @param token
	 * @param iamServiceAccount
	 * @param userDetails
	 * @param iamSvcAccName
	 * @return
	 */
	private boolean createIAMSvcAccPolicies(String token, IAMServiceAccount iamServiceAccount,
														   UserDetails userDetails, String iamSvcAccName) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE,
						String.format("Trying to create policies for IAM service account [%s].", iamSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		ResponseEntity<String> iamSvcAccPolicyCreationResponse = createIAMServiceAccountPolicies(token, iamSvcAccName);
		if (HttpStatus.OK.equals(iamSvcAccPolicyCreationResponse.getStatusCode())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							String.format("Successfully created policies for IAM service account [%s].", iamSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return true;
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Failed to create policies for IAM service account [%s].",
							iamServiceAccount))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return false;
		}
	}

	/**
	 * Method to rollback IAM service account onboarding process on failure.
	 * @param iamServiceAccount
	 * @param iamSvcAccName
	 * @param onAction
	 * @return
	 */
	private ResponseEntity<String> rollBackIAMOnboardOnFailure(IAMServiceAccount iamServiceAccount,
				String iamSvcAccName, String onAction) {
		//Delete the IAM Service account policies
		deleteIAMServiceAccountPolicies(tokenUtils.getSelfServiceToken(), iamSvcAccName);
		//Deleting the IAM service account metadata
		OnboardedIAMServiceAccount iamSvcAccToRevert = new OnboardedIAMServiceAccount(
				iamServiceAccount.getUserName(), iamServiceAccount.getAwsAccountId(),
				iamServiceAccount.getOwnerNtid());
		ResponseEntity<String> iamMetaDataDeletionResponse = deleteIAMSvcAccount(tokenUtils.getSelfServiceToken(), iamSvcAccToRevert);
		if (iamMetaDataDeletionResponse != null
				&& HttpStatus.OK.equals(iamMetaDataDeletionResponse.getStatusCode())) {
			if (onAction.equals("onPolicyFailure")) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
						"{\"errors\":[\"Failed to onboard IAM service account. Policy creation failed.\"]}");
			}
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"errors\":[\"Failed to onboard IAM service account. Association of owner permission failed\"]}");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"errors\":[\"Failed to create IAM Service Account policies. Reverting IAM service account creation also failed.\"]}");
		}
	}

	/**
	 * Method to send mail to IAM Service account owner
	 *
	 * @param iamServiceAccount
	 * @param iamSvcAccName
	 */
	private void sendMailToIAMSvcAccOwner(IAMServiceAccount iamServiceAccount, String iamSvcAccName) {
		// send email notification to IAM service account owner
		DirectoryUser directoryUser = getUserDetails(iamServiceAccount.getOwnerNtid());
		if (directoryUser != null) {
			String from = supportEmail;
			List<String> to = new ArrayList<>();
			to.add(iamServiceAccount.getOwnerEmail());
			String mailSubject = String.format(IAMServiceAccountConstants.IAM_ONBOARD_EMAIL_SUBJECT, iamSvcAccName!=null?iamSvcAccName.substring(13):"");

			// set template variables
			Map<String, String> mailTemplateVariables = new HashMap<>();
			mailTemplateVariables.put("name", directoryUser.getDisplayName());
			mailTemplateVariables.put("iamSvcAccName", iamSvcAccName);

			mailTemplateVariables.put("contactLink", supportEmail);

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION,
							String.format(
									"sendEmail for IAM Service account [%s] -  User " + "email=[%s] - subject = [%s]",
									iamSvcAccName, iamServiceAccount.getOwnerEmail(), mailSubject))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			emailUtils.sendIAMSvcAccHtmlEmalFromTemplate(from, to, mailSubject, mailTemplateVariables);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "sendMailToIAMSvcAccOwner")
					.put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   "
							+ "for an user name =  [%s] ,  Emails might not send to owner for an IAM service account = [%s]",
							iamServiceAccount.getOwnerEmail(), iamSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
	}

	/**
	 * Method to get the Directory User details
	 *
	 * @param userName
	 * @return
	 */
	private DirectoryUser getUserDetails(String userName) {
		ResponseEntity<DirectoryObjects> data = directoryService.searchByCorpId(userName);
		DirectoryObjects directoryObjects = data.getBody();
		DirectoryObjectsList usersList = directoryObjects.getData();
		DirectoryUser directoryUser = null;
		for (int i = 0; i < usersList.getValues().length; i++) {
			directoryUser = (DirectoryUser) usersList.getValues()[i];
			if (directoryUser != null && directoryUser.getUserName().equalsIgnoreCase(userName)) {
				break;
			}
		}
		if (directoryUser != null) {
			String[] displayName = directoryUser.getDisplayName().split(",");
			if (displayName.length > 1) {
				directoryUser.setDisplayName(displayName[1] + "  " + displayName[0]);
			}
		}
		return directoryUser;
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
	 * Method to populate IAMServiceAccountMetadataDetails object
	 *
	 * @param iamServiceAccount
	 * @return
	 */
	private IAMServiceAccountMetadataDetails constructIAMSvcAccMetaData(IAMServiceAccount iamServiceAccount) {

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
	 * Deletes IAM Service Account policies
	 * @param token
	 * @param svcAccName
	 * @return
	 */
	private boolean deleteIAMServiceAccountPolicies(String token, String iamSvcAccName) {
		int succssCount = 0;
		boolean allPoliciesDeleted = false;
		for (String policyPrefix : TVaultConstants.getSvcAccPolicies().keySet()) {
			String accessId = new StringBuffer().append(policyPrefix).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccName).toString();
			ResponseEntity<String> policyDeleteStatus = accessService.deletePolicyInfo(token, accessId);
			if (HttpStatus.OK.equals(policyDeleteStatus.getStatusCode())) {
				succssCount++;
			}
		}
		if (succssCount == TVaultConstants.getSvcAccPolicies().size()) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "deleteIAMServiceAccountPolicies").
					put(LogMessage.MESSAGE, "Successfully removed policies for IAM service account.").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			allPoliciesDeleted = true;
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "deleteIAMServiceAccountPolicies").
					put(LogMessage.MESSAGE, "Failed to delete some of the policies for IAM service account.").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return allPoliciesDeleted;
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
			String iamCredsPath=new StringBuffer().append(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(iamSvcAccName).append("/*").toString();
			accessMap.put(iamCredsPath, TVaultConstants.getSvcAccPolicies().get(policyPrefix));
			// Attaching write permissions for owner
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.SUDO_POLICY)) {
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName + "/*",
						TVaultConstants.WRITE_POLICY);
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + iamSvcAccName,
						TVaultConstants.WRITE_POLICY);
			}
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.WRITE_POLICY)) {
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName + "/*",
						TVaultConstants.WRITE_POLICY);
				accessMap.put(IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + iamSvcAccName,
						TVaultConstants.WRITE_POLICY);
			}
			accessPolicy.setAccess(accessMap);
			ResponseEntity<String> policyCreationStatus = accessService.createPolicy(tokenUtils.getSelfServiceToken(), accessPolicy);
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
		String iamSvcAccPath = IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + iamSvcAccName;
		Response onboardingResponse = reqProcessor.process(DELETEPATH, PATHSTR + iamSvcAccPath + "\"}", token);

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
				.put(LogMessage.ACTION, "ListOnboardedIAMServiceAccounts")
				.put(LogMessage.MESSAGE, "Trying to get list of onboaded IAM service accounts")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response response = null;
		String[] latestPolicies = policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(),
				userDetails.getUsername(), userDetails);
		List<IAMServiceAccountResponse> onboardedlist = new ArrayList<>();
		for (String policy : latestPolicies) {
			if (policy.startsWith("o_iamsvcacc")) {
				IAMServiceAccountResponse iamServiceAccountResponse = new IAMServiceAccountResponse();
				String iamSvcName = policy.substring(12);
				String[] accountID = iamSvcName.split("_");
				String separator = "_";
				int sepPos = iamSvcName.indexOf(separator);
				iamServiceAccountResponse.setMetaDataName(iamSvcName);
				iamServiceAccountResponse.setUserName(iamSvcName.substring(sepPos + separator.length()));
				iamServiceAccountResponse.setAccountID(accountID[0]);
				onboardedlist.add(iamServiceAccountResponse);
			}
		}
		response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse("{\"keys\":" + JSONUtil.getJSON(onboardedlist) + "}");

		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
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
	 * Get onboarded IAM service account list
	 *
	 * @param token
	 * @param userDetails
	 * @return
	 */
	private List<String> getOnboardedIAMServiceAccountList(String token, UserDetails userDetails) {
		ResponseEntity<String> onboardedResponse = getAllOnboardedIAMServiceAccounts(token, userDetails);

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
	 * To get all iam service accounts
	 *
	 * @param token
	 * @param userDetails
	 * @return
	 */
	private ResponseEntity<String> getAllOnboardedIAMServiceAccounts(String token, UserDetails userDetails) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "listAllOnboardedIAMServiceAccounts")
				.put(LogMessage.MESSAGE, "Trying to get all onboaded IAM service accounts")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		String metadataPath = IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH;

		Response response = reqProcessor.process("/iam/onboardedlist", PATHSTR + metadataPath + "\"}", token);

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
					.body(INVALIDVALUEERROR);
		}
		if (iamServiceAccountGroup.getAccess().equalsIgnoreCase(IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING)) {
			iamServiceAccountGroup.setAccess(TVaultConstants.WRITE_POLICY);
		}

		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		}

		String iamSvcAccountName = iamServiceAccountGroup.getAwsAccountId() + "_"
				+ iamServiceAccountGroup.getIamSvcAccName();

		boolean canAddGroup = isAuthorizedToAddPermissionInIAMSvcAcc(userDetails, iamSvcAccountName, token, false);
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
	 * Method to create policies for add group to IAM service account and call the update process.
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
				.put(LogMessage.MESSAGE, String.format(POLICYSTR, policy))
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
						String.format("Group policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy,
								writePolicy, denyPolicy, sudoPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response groupResp = new Response();

		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			groupResp = reqProcessor.process(GROUPPATH,
					GROUPNAME + iamServiceAccountGroup.getGroupname() + "\"}", token);
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
				.put(LogMessage.MESSAGE, String.format("Group Response status is [%s]", groupResp.getHttpstatus()))
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
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && oidcGroup != null) {
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
	 * Method to update policies and metadata for add group to IAM service account.
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

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("After configured the group [%s] and status [%s] ", iamServiceAccountGroup.getGroupname(), ldapConfigresponse.getHttpstatus()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
			String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(iamSvcAccountName)
					.toString();
			Map<String, String> params = new HashMap<>();
			params.put("type", IAMServiceAccountConstants.IAM_GROUP_MSG_STRING);
			params.put("name", iamServiceAccountGroup.getGroupname());
			params.put("path", path);
			params.put(IAMServiceAccountConstants.IAM_ACCESS_MSG_STRING, iamServiceAccountGroup.getAccess());

			Response metadataResponse = ControllerUtil.updateMetadata(params, token);
			if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
					|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_GROUP_TO_IAMSVCACC_MSG)
						.put(LogMessage.MESSAGE,String.format("Group [%s] is successfully associated to IAM Service Account [%s] with policy [%s].",iamServiceAccountGroup.getGroupname(),iamServiceAccountGroup.getIamSvcAccName(),iamServiceAccountGroup.getAccess()))
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
					.body("{\"errors\":[\"Group configuration failed.Try Again\"]}");
		}
	}

	/**
	 * Method to revert group permission if add group to IAM service account failed.
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
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ldapRevertConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, currentpolicies, currentpolicies,
					oidcGroup != null ? oidcGroup.getId() : null);
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

		iamServiceAccountUser.setIamSvcAccName(iamServiceAccountUser.getIamSvcAccName().toLowerCase());
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Trying to add user to ServiceAccount [%s]", iamServiceAccountUser.getIamSvcAccName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		if (!isIamSvcaccPermissionInputValid(iamServiceAccountUser.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(INVALIDVALUEERROR);
		}
		if (iamServiceAccountUser.getAccess().equalsIgnoreCase(IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING)) {
			iamServiceAccountUser.setAccess(TVaultConstants.WRITE_POLICY);
		}

		String uniqueIAMSvcaccName = iamServiceAccountUser.getAwsAccountId() + "_" + iamServiceAccountUser.getIamSvcAccName();

		boolean isAuthorized = isAuthorizedToAddPermissionInIAMSvcAcc(userDetails, uniqueIAMSvcaccName, token, isPartOfOnboard);

		if (isAuthorized) {
			// Only Sudo policy can be added (as part of onbord) before activation.
			if (!isIAMSvcaccActivated(token, userDetails, uniqueIAMSvcaccName)
					&& !TVaultConstants.SUDO_POLICY.equals(iamServiceAccountUser.getAccess())) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE, String.format("Failed to add user permission to IAM Service account. [%s] is not activated.", iamServiceAccountUser.getIamSvcAccName()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to add user permission to IAM Service account. Service Account is not activated. Please activate this service account and try again.\"]}");
			}

			if (isOwnerPemissionGettingChanged(iamServiceAccountUser, userDetails.getUsername(), isPartOfOnboard)) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE, "Failed to add user permission to IAM Service account. Owner permission cannot be changed..")
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to add user permission to IAM Service account. Owner permission cannot be changed.\"]}");
			}
			return getUserPoliciesForAddUserToIAMSvcAcc(token, userDetails, iamServiceAccountUser, oidcEntityResponse,
					uniqueIAMSvcaccName);

		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("{\"errors\":[\"Access denied: No permission to add users to this IAM service account\"]}");
		}
	}

	/**
	 * Method to check if the owner permission is getting changed.
	 * @param iamServiceAccountUser
	 * @return
	 */
	private boolean isOwnerPemissionGettingChanged(IAMServiceAccountUser iamServiceAccountUser, String currentUsername, boolean isPartOfOnboard) {
		if (isPartOfOnboard) {
			// sudo as part of onboard is allowed.
			return false;
		}
		// if owner is grating read/ deny to himself, not allowed. Write is allowed as part of activation.
		if (iamServiceAccountUser.getUsername().equalsIgnoreCase(currentUsername) && !iamServiceAccountUser.getAccess().equals(TVaultConstants.WRITE_POLICY)) {
			return true;
		}
		return false;
	}

	/**
	 * Method to verify the user for add user to IAM service account.
	 * @param token
	 * @param userDetails
	 * @param iamServiceAccountUser
	 * @param oidcEntityResponse
	 * @param uniqueIAMSvcaccName
	 * @return
	 */
	private ResponseEntity<String> getUserPoliciesForAddUserToIAMSvcAcc(String token, UserDetails userDetails,
			IAMServiceAccountUser iamServiceAccountUser, OIDCEntityResponse oidcEntityResponse,
			String uniqueIAMSvcaccName) {
		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process(READPATH, IAMServiceAccountConstants.USERNAME_PARAM_STRING + iamServiceAccountUser.getUsername() + "\"}",
					token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process(USERPATH, IAMServiceAccountConstants.USERNAME_PARAM_STRING + iamServiceAccountUser.getUsername() + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC implementation changes
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, iamServiceAccountUser.getUsername(),
					userDetails, true);
			if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
							.put(LogMessage.MESSAGE, String.format("Failed to fetch OIDC user for [%s]", iamServiceAccountUser.getUsername()))
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

		return addPolicyToIAMSvcAcc(token, userDetails, iamServiceAccountUser, oidcEntityResponse, uniqueIAMSvcaccName,
				userResponse);
	}

	/**
	 * Method to create policies for add user to IAM service account and call the update process.
	 * @param token
	 * @param userDetails
	 * @param iamServiceAccountUser
	 * @param oidcEntityResponse
	 * @param uniqueIAMSvcaccName
	 * @param userResponse
	 * @return
	 */
	private ResponseEntity<String> addPolicyToIAMSvcAcc(String token, UserDetails userDetails,
		IAMServiceAccountUser iamServiceAccountUser, OIDCEntityResponse oidcEntityResponse,
		String uniqueIAMSvcaccName, Response userResponse) {

		String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(iamServiceAccountUser.getAccess())).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Trying to [%s] policy to user [%s] for the IAM service account [%s]", policy, iamServiceAccountUser.getUsername(), iamServiceAccountUser.getIamSvcAccName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String readPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
		String writePolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
		String denyPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("User policies are, read - [%s], write - [%s], deny -[%s]", readPolicy, writePolicy, denyPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

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
						groups = objMapper.readTree(responseJson).get("data").get(IAMServiceAccountConstants.IAM_GROUP_MSG_STRING).asText();
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
						.put(LogMessage.MESSAGE, String.format("Exception while creating currentpolicies or groups for [%s]", iamServiceAccountUser.getUsername()))
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
		return configureUserPoliciesForAddUserToIAMSvcAcc(token, userDetails, iamServiceAccountUser, oidcEntityResponse,
				groups, policies, currentpolicies);
	}

	/**
	 * Method to update policies for add user to IAM service account.
	 * @param token
	 * @param userDetails
	 * @param iamServiceAccountUser
	 * @param oidcEntityResponse
	 * @param groups
	 * @param policies
	 * @param currentpolicies
	 * @return
	 */
	private ResponseEntity<String> configureUserPoliciesForAddUserToIAMSvcAcc(String token, UserDetails userDetails,
			IAMServiceAccountUser iamServiceAccountUser, OIDCEntityResponse oidcEntityResponse, String groups,
			List<String> policies, List<String> currentpolicies) {
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Policies [%s] before calling configure user", policies))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response userConfigresponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userConfigresponse = ControllerUtil.configureUserpassUser(iamServiceAccountUser.getUsername(), policiesString, token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userConfigresponse = ControllerUtil.configureLDAPUser(iamServiceAccountUser.getUsername(), policiesString, groups, token);
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
						.put(LogMessage.MESSAGE, String.format("Exception while adding or updating the identity for entity [%s]", oidcEntityResponse.getEntityName()))
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
			}
		}

		if (userConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| userConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
			return updateMetadataForAddUserToIAMSvcAcc(token, userDetails, iamServiceAccountUser, oidcEntityResponse,
					groups, currentpolicies, currentpoliciesString);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to add user to the IAM Service Account\"]}");
		}
	}

	/**
	 * Method to update metadata for add user to IAM service account.
	 * @param token
	 * @param userDetails
	 * @param iamServiceAccountUser
	 * @param oidcEntityResponse
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForAddUserToIAMSvcAcc(String token, UserDetails userDetails,
			IAMServiceAccountUser iamServiceAccountUser, OIDCEntityResponse oidcEntityResponse, String groups,
			List<String> currentpolicies, String currentpoliciesString) {
		String iamUniqueSvcaccName = iamServiceAccountUser.getAwsAccountId() + "_" + iamServiceAccountUser.getIamSvcAccName();
		// User has been associated with IAM Service Account. Now metadata has to be created
		String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(iamUniqueSvcaccName)
				.toString();
		Map<String, String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name", iamServiceAccountUser.getUsername());
		params.put("path", path);
		params.put(IAMServiceAccountConstants.IAM_ACCESS_MSG_STRING, iamServiceAccountUser.getAccess());
		Response metadataResponse = ControllerUtil.updateMetadata(params, token);
		if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
				|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
							.put(LogMessage.MESSAGE, String.format("User [%s] is successfully associated to IAM Service Account [%s] with policy [%s].", iamServiceAccountUser.getUsername(), iamServiceAccountUser.getIamSvcAccName(),iamServiceAccountUser.getAccess()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully added user to the IAM Service Account\"]}");
		} else {
			return revertUserPoliciesForIAMSvcAcc(token, userDetails, oidcEntityResponse, iamServiceAccountUser.getUsername(), groups,
					currentpolicies, currentpoliciesString);
		}
	}

	/**
	 * Method to revert user policies if add user to IAM service account failed.
	 * @param token
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param userName
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> revertUserPoliciesForIAMSvcAcc(String token, UserDetails userDetails,
			OIDCEntityResponse oidcEntityResponse, String userName, String groups, List<String> currentpolicies,
			String currentpoliciesString) {
		Response configUserResponse = new Response();
		// Revert the user association when metadata fails...
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, "Metadata creation for user association with service account failed. Reverting user association")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			configUserResponse = ControllerUtil.configureUserpassUser(userName, currentpoliciesString,
					token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			configUserResponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString, groups,
					token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC changes
			try {
				configUserResponse = oidcUtil.updateOIDCEntity(currentpolicies,
						oidcEntityResponse.getEntityName());
				oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_USER_TO_IAMSVCACC_MSG)
						.put(LogMessage.MESSAGE, String.format("Exception while adding or updating the identity for entity [%s]", oidcEntityResponse.getEntityName()))
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
			}
		}
		if (configUserResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| configUserResponse.getHttpstatus().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"messages\":[\"Failed to add user to the Service Account. Metadata update failed\"]}");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"messages\":[\"Failed to revert user association on IAM Service Account\"]}");
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
		return reqProcessor.process("/sdb", PATHSTR + iamMetaDataPath + "\"}", token);
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
	public boolean isAuthorizedToAddPermissionInIAMSvcAcc(UserDetails userDetails, String serviceAccount, String token,
			boolean isPartOfOnboard) {
		// IAM admin users can add sudo policy for owner while onboarding the service account
		if (isPartOfOnboard) {
			return true;
		}
		// Owner of the service account can add/remove users, groups, aws roles and approles to service account
		String ownerPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.
				getKey(TVaultConstants.SUDO_POLICY)).append(TVaultConstants.IAM_SVC_ACC_PATH_PREFIX).
				append("_").append(serviceAccount).toString();
		String [] policies = policyUtils.getCurrentPolicies(tokenUtils.getSelfServiceToken(), userDetails.getUsername(), userDetails);
		if (ArrayUtils.contains(policies, ownerPolicy)) {
			return true;
		}
		return false;
	}

	/*
	 * 
	 * @param userDetails
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getIAMServiceAccountsList(UserDetails userDetails, String userToken) {
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
				String[] iamPolicies = policy.split("_", -1);
				if (iamPolicies.length >= 3) {
					String[] policyName = Arrays.copyOfRange(iamPolicies, 2, iamPolicies.length);
					String safeName = String.join("_", policyName);
					String safeType = iamPolicies[1];

					if (policy.startsWith("r_")) {
						safePolicy.put(safeName, "read");
					} else if (policy.startsWith("w_")) {
						safePolicy.put(safeName, "write");
					} else if (policy.startsWith("d_")) {
						safePolicy.put(safeName, "deny");
					}
					if (!safePolicy.isEmpty()) {
						if (safeType.equals(TVaultConstants.IAM_SVC_ACC_PATH_PREFIX)) {
							svcListUsers.add(safePolicy);
						}
					}
				}
			}
			safeList.put(TVaultConstants.IAM_SVC_ACC_PATH_PREFIX, svcListUsers);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeList));
	}

	/**
	 * Filter iam service accounts policies based on policy precedence.
	 * 
	 * @param policies
	 * @return
	 */
	private String[] filterPoliciesBasedOnPrecedence(List<String> policies) {
		List<String> filteredList = new ArrayList<>();
		for (int i = 0; i < policies.size(); i++) {
			String policyName = policies.get(i);
			String[] iamPolicy = policyName.split("_", -1);
			if (iamPolicy.length >= 3) {
				String itemName = policyName.substring(1);
				List<String> matchingPolicies = filteredList.stream().filter(p -> p.substring(1).equals(itemName))
						.collect(Collectors.toList());
				if (!matchingPolicies.isEmpty()) {
					/*
					 * deny has highest priority. Read and write are additive in
					 * nature Removing all matching as there might be duplicate
					 * policies from user and groups
					 */
					if (policyName.startsWith("d_") || (policyName.startsWith("w_")
							&& !matchingPolicies.stream().anyMatch(p -> p.equals("d" + itemName)))) {
						filteredList.removeAll(matchingPolicies);
						filteredList.add(policyName);
					} else if (matchingPolicies.stream().anyMatch(p -> p.equals("d" + itemName))) {
						// policy is read and deny already in the list. Then
						// deny has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("d" + itemName);
					} else if (matchingPolicies.stream().anyMatch(p -> p.equals("w" + itemName))) {
						// policy is read and write already in the list. Then
						// write has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("w" + itemName);
					} else if (matchingPolicies.stream().anyMatch(p -> p.equals("r" + itemName))
							|| matchingPolicies.stream().anyMatch(p -> p.equals("o" + itemName))) {
						// policy is read and read already in the list. Then
						// remove all duplicates read and add single read
						// permission for that iam service account.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("r" + itemName);
					}
				} else {
					filteredList.add(policyName);
				}
			}
		}
		return filteredList.toArray(new String[0]);
	}

	/**
	 * Find Iam service account from metadata.
	 * 
	 * @param token
	 * @param iamSvcaccName
	 * @return
	 */
	public ResponseEntity<String> getIAMServiceAccountDetail(String token, String iamSvcaccName) {
		String path = TVaultConstants.IAM_SVC_PATH + iamSvcaccName;
		Response response = reqProcessor.process("/iamsvcacct", PATHSTR + path + "\"}", token);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			JsonObject data = populateMetaData(response);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "get IAmservice Account details").
					put(LogMessage.MESSAGE,"IAM Service Account details fetched successfully.").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body(data.toString());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"No Iam Service Account with " + iamSvcaccName + ".\"]}");

	}

	/**
	 * Date conversion from milliseconds to yyyy-MM-dd HH:mm:ss
	 * 
	 * @param createdEpoch
	 */
	private String dateConversion(Long createdEpoch) {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(createdEpoch);
		return formatter.format(calendar.getTime());
	}

	/**
	 * Find Iam service account from metadata.
	 * 
	 * @param token
	 * @param iamSvcaccName
	 * @return
	 */
	public ResponseEntity<String> getIAMServiceAccountSecretKey(String token, String iamSvcaccName, String folderName) {
		String path = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcaccName + "/" + folderName;
		Response response = reqProcessor.process("/iamsvcacct", PATHSTR + path + "\"}", token);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			JsonParser jsonParser = new JsonParser();
			JsonObject data = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
			Long expiryDate = Long.valueOf(String.valueOf(data.get("expiryDateEpoch")));
			String formattedExpiryDt = dateConversion(expiryDate);
			data.addProperty("expiryDate", formattedExpiryDt);
			return ResponseEntity.status(HttpStatus.OK).body(data.toString());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"No Iam Service Account with " + iamSvcaccName + ".\"]}");

	}

	/**
	 * populate metadata
	 * 
	 * @param response
	 * @return
	 */
	private JsonObject populateMetaData(Response response) {
		JsonParser jsonParser = new JsonParser();
		JsonObject data = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		Long createdAtEpoch = Long.valueOf(String.valueOf(data.get("createdAtEpoch")));

		String createdDate = dateConversion(createdAtEpoch);
		data.addProperty("createdDate", createdDate);
		JsonArray dataSecret = ((JsonObject) jsonParser.parse(data.toString())).getAsJsonArray("secret");

		for (int i = 0; i < dataSecret.size(); i++) {
			JsonElement jsonElement = dataSecret.get(i);
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			String expiryDate = dateConversion(jsonObject.get("expiryDuration").getAsLong());
			jsonObject.addProperty("expiryDuration", expiryDate);
		}
		JsonElement jsonElement = dataSecret.getAsJsonArray();
		data.add("secret", jsonElement);
		return data;
	}

	/**
	 * Removes user from IAM service account
	 * @param token
	 * @param iamServiceAccountUser
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> removeUserFromIAMServiceAccount(String token, IAMServiceAccountUser iamServiceAccountUser, UserDetails userDetails) {
		iamServiceAccountUser.setIamSvcAccName(iamServiceAccountUser.getIamSvcAccName().toLowerCase());
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if (!isIamSvcaccPermissionInputValid(iamServiceAccountUser.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(INVALIDVALUEERROR);
		}
		if (iamServiceAccountUser.getAccess().equalsIgnoreCase(IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING)) {
			iamServiceAccountUser.setAccess(TVaultConstants.WRITE_POLICY);
		}

		String uniqueIAMSvcaccName = iamServiceAccountUser.getAwsAccountId() + "_" + iamServiceAccountUser.getIamSvcAccName();

		boolean isAuthorized = isAuthorizedToAddPermissionInIAMSvcAcc(userDetails, uniqueIAMSvcaccName, token, false);

		if(isAuthorized){
			// Only Sudo policy can be added (as part of onbord) before activation.
			if (!isIAMSvcaccActivated(token, userDetails, uniqueIAMSvcaccName)
					&& !TVaultConstants.SUDO_POLICY.equals(iamServiceAccountUser.getAccess())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE, String.format("Failed to remove user permission from IAM Service account. [%s] is not activated.", iamServiceAccountUser.getIamSvcAccName()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to remove user permission from IAM Service account. IAM Service Account is not activated. Please activate this IAM service account and try again.\"]}");
			}
			// Deleting owner permission is not allowed
			if (iamServiceAccountUser.getUsername().equalsIgnoreCase(userDetails.getUsername())) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE, "Failed to remove user permission to IAM Service account. Owner permission cannot be changed..")
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to remove user permission to IAM Service account. Owner permission cannot be changed.\"]}");
			}
			return processAndRemoveUserPermissionFromIAMSvcAcc(token, iamServiceAccountUser, userDetails,
					oidcEntityResponse, uniqueIAMSvcaccName);
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this IAM service account\"]}");
		}
	}

	/**
	 * Method to verify the user for removing from IAM service account.
	 * @param token
	 * @param iamServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param uniqueIAMSvcaccName
	 * @return
	 */
	private ResponseEntity<String> processAndRemoveUserPermissionFromIAMSvcAcc(String token,
			IAMServiceAccountUser iamServiceAccountUser, UserDetails userDetails, OIDCEntityResponse oidcEntityResponse,
			String uniqueIAMSvcaccName) {

		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process(READPATH, "{\"username\":\"" + iamServiceAccountUser.getUsername() + "\"}",
					token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process(USERPATH, "{\"username\":\"" + iamServiceAccountUser.getUsername() + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC implementation changes
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, iamServiceAccountUser.getUsername(), userDetails, true);
			if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG).
							put(LogMessage.MESSAGE, "Trying to fetch OIDC user policies, failed").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
			}
			oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
			oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
			userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
			userResponse.setHttpstatus(responseEntity.getStatusCode());
		}

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG).
				put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse.getHttpstatus())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		return createPoliciesAndRemoveUserFromSvcAcc(token, iamServiceAccountUser, userDetails, oidcEntityResponse,
				uniqueIAMSvcaccName, userResponse);
	}

	/**
	 * Method to create policies for removing user from IAM service account and call the metadata update.
	 * @param token
	 * @param iamServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param uniqueIAMSvcaccName
	 * @param userResponse
	 * @return
	 */
	private ResponseEntity<String> createPoliciesAndRemoveUserFromSvcAcc(String token,
			IAMServiceAccountUser iamServiceAccountUser, UserDetails userDetails, OIDCEntityResponse oidcEntityResponse,
			String uniqueIAMSvcaccName, Response userResponse) {
		String readPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
		String writePolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
		String denyPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
		String ownerPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, ownerPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();
			try {
				ObjectMapper objMapper = new ObjectMapper();
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
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG).
						put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		Response ldapConfigresponse = configureRemovedUserPermissions(token, iamServiceAccountUser, userDetails,
				oidcEntityResponse, groups, policies, policiesString);

		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){

			return updateMetadataAfterRemovePermissionFromIAMSvcAcc(token, iamServiceAccountUser, userDetails,
					oidcEntityResponse, groups, currentpolicies, currentpoliciesString);
		}
		else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remvoe the user from the IAM Service Account\"]}");
		}
	}

	/**
	 * Method to update the metadata after removed user from IAM service account
	 * @param token
	 * @param iamServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataAfterRemovePermissionFromIAMSvcAcc(String token,
			IAMServiceAccountUser iamServiceAccountUser, UserDetails userDetails, OIDCEntityResponse oidcEntityResponse,
			String groups, List<String> currentpolicies, String currentpoliciesString) {
		String iamSvcaccName = iamServiceAccountUser.getAwsAccountId() + "_" + iamServiceAccountUser.getIamSvcAccName();
		// User has been removed from this IAM Service Account. Now metadata has to be deleted
		String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(iamSvcaccName).toString();
		Map<String,String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name",iamServiceAccountUser.getUsername());
		params.put("path",path);
		params.put("access","delete");
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG).
					put(LogMessage.MESSAGE,String.format("User[%s] is successfully removed from IAM Service Account[%s]",iamServiceAccountUser.getUsername(),iamServiceAccountUser.getIamSvcAccName())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully removed user from the IAM Service Account\"]}");
		} else {
			return revertUserPermission(token, iamServiceAccountUser, userDetails, oidcEntityResponse, groups,
					currentpolicies, currentpoliciesString);
		}
	}

	/**
	 * Method to configure the user permission after removed from IAM service account.
	 * @param token
	 * @param iamServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param groups
	 * @param policies
	 * @param policiesString
	 * @return
	 */
	private Response configureRemovedUserPermissions(String token, IAMServiceAccountUser iamServiceAccountUser,
			UserDetails userDetails, OIDCEntityResponse oidcEntityResponse, String groups, List<String> policies,
			String policiesString) {
		Response ldapConfigresponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(iamServiceAccountUser.getUsername(), policiesString, token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(iamServiceAccountUser.getUsername(), policiesString, groups, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC Implementation : Entity Update
			try {
				ldapConfigresponse = oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
				oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG).
						put(LogMessage.MESSAGE, "Exception while updating the identity").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		return ldapConfigresponse;
	}

	/**
	 * Method to revert user permission for remove user from IAM service account if update failed.
	 * @param token
	 * @param iamServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> revertUserPermission(String token, IAMServiceAccountUser iamServiceAccountUser,
			UserDetails userDetails, OIDCEntityResponse oidcEntityResponse, String groups, List<String> currentpolicies,
			String currentpoliciesString) {
		Response configUserResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			configUserResponse = ControllerUtil.configureUserpassUser(iamServiceAccountUser.getUsername(), currentpoliciesString, token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			configUserResponse = ControllerUtil.configureLDAPUser(iamServiceAccountUser.getUsername(), currentpoliciesString, groups, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC changes
			try {
				configUserResponse = oidcUtil.updateOIDCEntity(currentpolicies, oidcEntityResponse.getEntityName());
				oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e2) {
				log.error(e2);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_USER_FROM_IAMSVCACC_MSG).
						put(LogMessage.MESSAGE, "Exception while updating the identity").
						put(LogMessage.STACKTRACE, Arrays.toString(e2.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		if(configUserResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || configUserResponse.getHttpstatus().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the user from the IAM Service Account. Metadata update failed\"]}");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to revert user association on IAM Service Account\"]}");
		}
	}

	/**
     * Remove Group from IAM Service Account
     *
     * @param token
     * @param iamServiceAccountGroup
     * @param userDetails
     * @return
     */
    public ResponseEntity<String> removeGroupFromIAMServiceAccount(String token, IAMServiceAccountGroup iamServiceAccountGroup, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, "Trying to remove Group from IAM Service Account")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }

        if (!isIamSvcaccPermissionInputValid(iamServiceAccountGroup.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(INVALIDVALUEERROR);
		}
		if (iamServiceAccountGroup.getAccess().equalsIgnoreCase(IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING)) {
			iamServiceAccountGroup.setAccess(TVaultConstants.WRITE_POLICY);
		}

        String iamSvcAccountName = iamServiceAccountGroup.getAwsAccountId() + "_" + iamServiceAccountGroup.getIamSvcAccName();

		boolean isAuthorized = isAuthorizedToAddPermissionInIAMSvcAcc(userDetails, iamSvcAccountName, token, false);
		if (isAuthorized) {
			return groupRemovalForIAMServiceAccount(token, iamServiceAccountGroup, userDetails, oidcGroup,
					iamSvcAccountName);
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove groups from this IAM service account\"]}");
        }

    }

	/**
	 * Remove Group from IAM Service Account after validation
	 *
	 * @param token
	 * @param iamServiceAccountGroup
	 * @param userDetails
	 * @param oidcGroup
	 * @param iamSvcAccountName
	 * @return
	 */
	private ResponseEntity<String> groupRemovalForIAMServiceAccount(String token,
			IAMServiceAccountGroup iamServiceAccountGroup, UserDetails userDetails, OIDCGroup oidcGroup,
			String iamSvcAccountName) {
		// Only Sudo policy can be added (as part of onbord) before activation.
		if (!isIAMSvcaccActivated(token, userDetails, iamSvcAccountName)
				&& !TVaultConstants.SUDO_POLICY.equals(iamServiceAccountGroup.getAccess())) {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG)
							.put(LogMessage.MESSAGE, String.format("Failed to remove group permission to IAM Service account. [%s] is not activated.", iamSvcAccountName))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Failed to remove group permission to IAM Service account. IAM Service Account is not activated. Please activate this service account and try again.\"]}");
		}

		// check for this group is associated to this IAM Service account
		String iamMetadataPath = new StringBuilder().append(TVaultConstants.IAM_SVC_PATH).append(iamSvcAccountName).toString();
		Response metadataReadResponse = reqProcessor.process("/iamsvcacct", PATHSTR + iamMetadataPath + "\"}", token);
		Map<String, Object> responseMap = null;
		boolean metaDataResponseStatus = true;
		if(metadataReadResponse != null && HttpStatus.OK.equals(metadataReadResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(metadataReadResponse.getResponse());
			if(responseMap.isEmpty()) {
				metaDataResponseStatus = false;
			}
		}
		else {
			metaDataResponseStatus = false;
		}

		if(metaDataResponseStatus) {
			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			Map<String,Object> groupsData = (Map<String,Object>)metadataMap.get(TVaultConstants.GROUPS);

			if (groupsData == null || !groupsData.containsKey(iamServiceAccountGroup.getGroupname())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
						put(LogMessage.MESSAGE, String.format ("Group [%s] is not associated to IAM service account [%s]", iamServiceAccountGroup.getGroupname(), iamMetadataPath)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove group from IAM service account. Group association to IAM service account not found\"]}");
			}
		}else {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
					put(LogMessage.MESSAGE, String.format ("Error Fetching existing IAM Service account info [%s]", iamMetadataPath)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing IAM Service account info. please check the path specified\"]}");
		}

		return validateAuthMethodAndGroupRemovalProcess(token, iamServiceAccountGroup, userDetails, oidcGroup,
				iamSvcAccountName);
	}

	/**
	 * Method to call the group removal based on the auth method
	 * @param token
	 * @param iamServiceAccountGroup
	 * @param userDetails
	 * @param oidcGroup
	 * @param iamSvcAccountName
	 * @return
	 */
	private ResponseEntity<String> validateAuthMethodAndGroupRemovalProcess(String token,
			IAMServiceAccountGroup iamServiceAccountGroup, UserDetails userDetails, OIDCGroup oidcGroup,
			String iamSvcAccountName) {
		Response groupResp = new Response();
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			groupResp = reqProcessor.process(GROUPPATH, GROUPNAME + iamServiceAccountGroup.getGroupname() + "\"}", token);
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
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
		        put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", groupResp.getHttpstatus())).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		return removePoliciesAndUpdateMetadataForIAMSvcAcc(token, iamServiceAccountGroup, userDetails, oidcGroup,
				iamSvcAccountName, groupResp);
	}

	/**
	 * Method to update policies for remove group from IAM service account.
	 * @param token
	 * @param iamServiceAccountGroup
	 * @param userDetails
	 * @param oidcGroup
	 * @param iamSvcAccountName
	 * @param groupResp
	 * @return
	 */
	private ResponseEntity<String> removePoliciesAndUpdateMetadataForIAMSvcAcc(String token,
			IAMServiceAccountGroup iamServiceAccountGroup, UserDetails userDetails, OIDCGroup oidcGroup,
			String iamSvcAccountName, Response groupResp) {
		String readPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();
		String writePolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();
		String denyPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();
		String sudoPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY)).append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(iamSvcAccountName).toString();

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
		        put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(groupResp != null && HttpStatus.OK.equals(groupResp.getHttpstatus())){
		    responseJson = groupResp.getResponse();
		    try {
				ObjectMapper objMapper = new ObjectMapper();
				// OIDC Changes
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && oidcGroup != null) {
					currentpolicies.addAll(oidcGroup.getPolicies());
				}
		    } catch (IOException e) {
		        log.error(e);
		        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
		                put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups").
		                put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
		                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                build()));
		    }

		    policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		}else {
			return deleteOrphanGroupEntriesForIAMSvcAcc(token, iamServiceAccountGroup);
		}

		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		Response ldapConfigresponse = new Response();
		// OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPGroup(iamServiceAccountGroup.getGroupname(), policiesString, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ldapConfigresponse = oidcUtil.updateGroupPolicies(token, iamServiceAccountGroup.getGroupname(), policies, currentpolicies,
					oidcGroup != null ? oidcGroup.getId() : null);
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
			return updateMetadataForRemoveGroupFromIAMSvcAcc(token, iamServiceAccountGroup, userDetails, oidcGroup,
					currentpolicies, currentpoliciesString);
		}
		else {
			String ssoToken = oidcUtil.getSSOToken();
			if (!StringUtils.isEmpty(ssoToken)) {
				String objectId = oidcUtil.getGroupObjectResponse(ssoToken, iamServiceAccountGroup.getGroupname());
				if (objectId == null || StringUtils.isEmpty(objectId)) {
					return deleteOrphanGroupEntriesForIAMSvcAcc(token, iamServiceAccountGroup);
				}
			}
		    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Try Again\"]}");
		}
	}

	/**
	 * Method to delete orphan group entries if exists for IAM service account
	 * @param token
	 * @param iamServiceAccountGroup
	 * @return
	 */
	private ResponseEntity<String> deleteOrphanGroupEntriesForIAMSvcAcc(String token, IAMServiceAccountGroup iamServiceAccountGroup) {
		// Trying to remove the orphan entries if exists
		String iamUniqueSvcAccountName = iamServiceAccountGroup.getAwsAccountId() + "_" + iamServiceAccountGroup.getIamSvcAccName();
		String path = new StringBuilder(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(iamUniqueSvcAccountName).toString();
		Map<String,String> params = new HashMap<>();
		params.put("type", "groups");
		params.put("name",iamServiceAccountGroup.getGroupname());
		params.put("path",path);
		params.put("access","delete");
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "Remove Group from IAM service account").
					put(LogMessage.MESSAGE, String.format ("Group [%s] is successfully removed from IAM service account [%s]", iamServiceAccountGroup.getGroupname(), iamUniqueSvcAccountName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"Group association is removed \"}");
		}else{
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Try again \"]}");
		}
	}

	/**
	 * Method to update metadata and revert group permission if metadata update failed.
	 *
	 * @param token
	 * @param iamServiceAccountGroup
	 * @param userDetails
	 * @param oidcGroup
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForRemoveGroupFromIAMSvcAcc(String token,
			IAMServiceAccountGroup iamServiceAccountGroup, UserDetails userDetails, OIDCGroup oidcGroup,
			List<String> currentpolicies, String currentpoliciesString) {
		String iamUniqueSvcAccountName = iamServiceAccountGroup.getAwsAccountId() + "_" + iamServiceAccountGroup.getIamSvcAccName();
		String path = new StringBuilder(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(iamUniqueSvcAccountName).toString();
		Map<String,String> params = new HashMap<>();
		params.put("type", "groups");
		params.put("name",iamServiceAccountGroup.getGroupname());
		params.put("path",path);
		params.put("access","delete");
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
					put(LogMessage.MESSAGE, String.format("Group[%s] is successfully removed from IAM Service Account [%s].", iamServiceAccountGroup.getGroupname(),iamServiceAccountGroup.getIamSvcAccName())).
					put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from IAM Service Account\"]}");
		}else {
			return revertGroupPermissionForIAMSvcAcc(token, iamServiceAccountGroup, userDetails, oidcGroup,
					currentpolicies, currentpoliciesString, metadataResponse);
		}
	}

	/**
	 * Method to revert group permission for IAM service account
	 *
	 * @param token
	 * @param iamServiceAccountGroup
	 * @param userDetails
	 * @param oidcGroup
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> revertGroupPermissionForIAMSvcAcc(String token,
			IAMServiceAccountGroup iamServiceAccountGroup, UserDetails userDetails, OIDCGroup oidcGroup,
			List<String> currentpolicies, String currentpoliciesString, Response metadataResponse) {
		Response configGroupResponse = new Response();
		// OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			configGroupResponse = ControllerUtil.configureLDAPGroup(iamServiceAccountGroup.getGroupname(), currentpoliciesString, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			configGroupResponse = oidcUtil.updateGroupPolicies(token, iamServiceAccountGroup.getGroupname(), currentpolicies,
					currentpolicies, oidcGroup != null ? oidcGroup.getId() : null);
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}
		if(configGroupResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
					put(LogMessage.MESSAGE, "Reverting, group policy update success").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_GROUP_FROM_IAMSVCACC_MSG).
					put(LogMessage.MESSAGE, "Reverting group policy update failed").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
		}
	}


	/**
	 * Read Folder details for a given iamsvcacc
	 *
	 * @param token
	 * @param path
	 * @return
	 * @throws IOException 
	 */
	public ResponseEntity<String> readFolders(String token, String path) throws IOException {
		Response response = new Response();
		ObjectMapper objMapper = new ObjectMapper();
		Response lisresp = reqProcessor.process("/iam/list", PATHSTR + path + "\"}", token);
		if(lisresp.getHttpstatus().equals(HttpStatus.OK)){
			List<String> foldersList = new ArrayList<>();
			IAMServiceAccountNode iamServiceAccountNode = new IAMServiceAccountNode();
			JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
			for (JsonNode node : folders) {
				foldersList.add(node.textValue());
			}
			iamServiceAccountNode.setFolders(foldersList);
			iamServiceAccountNode.setPath(path.substring(10));
			String separator = "_";
			int sepPos = path.indexOf(separator);
			iamServiceAccountNode.setIamsvcaccName(path.substring(sepPos + separator.length()));
			response.setSuccess(true);
			response.setHttpstatus(HttpStatus.OK);
			String res = objMapper.writeValueAsString(iamServiceAccountNode);
			return ResponseEntity.status(response.getHttpstatus()).body(res);
		}else if (lisresp.getHttpstatus().equals(HttpStatus.FORBIDDEN)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "readFolders")
					.put(LogMessage.MESSAGE, "No permission to access the folder")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			response.setSuccess(false);
			response.setHttpstatus(HttpStatus.FORBIDDEN);
			response.setResponse("{\"errors\":[\"Unable to read the given path :" + path + "\"]}");
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "readFolders")
					.put(LogMessage.MESSAGE, "Unable to readFolders")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			response.setSuccess(false);
			response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
			response.setResponse("{\"errors\":[\"Unexpected error :" + path + "\"]}");
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		} 
	}

	/**
	 * Associate Approle to Service Account
	 *
	 * @param userDetails
	 * @param token
	 * @param serviceAccountApprole
	 * @return
	 */
	public ResponseEntity<String> associateApproletoIAMsvcacc(UserDetails userDetails, String token,
			IAMServiceAccountApprole iamServiceAccountApprole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
				.put(LogMessage.MESSAGE, "Trying to add Approle to IAM Service Account")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		if (!isIamSvcaccPermissionInputValid(iamServiceAccountApprole.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(INVALIDVALUEERROR);
		}
		if (iamServiceAccountApprole.getAccess().equalsIgnoreCase(IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING)) {
			iamServiceAccountApprole.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String uniqueIAMSvcaccName = iamServiceAccountApprole.getAwsAccountId() + "_" + iamServiceAccountApprole.getIamSvcAccName();
		String approleName = iamServiceAccountApprole.getApprolename();
		String access = iamServiceAccountApprole.getAccess();
		
		if (Arrays.asList(TVaultConstants.MASTER_APPROLES).contains(iamServiceAccountApprole.getApprolename())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Access denied: no permission to associate this AppRole to any IAM Service Account\"]}");
		}
		approleName = (approleName != null) ? approleName.toLowerCase() : approleName;
		access = (access != null) ? access.toLowerCase() : access;

		boolean isAuthorized = hasAddOrRemovePermission(userDetails, uniqueIAMSvcaccName, token);
		if (isAuthorized) {
			String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();

			log.debug(
					JSONUtil.getJSON(
							ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE, String.format(POLICYSTR, policy))
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build())); 
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			String ownerPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE,
							String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]",
									readPolicy, writePolicy, denyPolicy, ownerPolicy))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			Response roleResponse = reqProcessor.process("/auth/approle/role/read",
					"{\"role_name\":\"" + approleName + "\"}", token);

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE, String.format("roleResponse status is [%s]", roleResponse.getHttpstatus()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));

			String responseJson = "";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if (HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
				responseJson = roleResponse.getResponse();
				ObjectMapper objMapper = new ObjectMapper();
				try {
					JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get(TVaultConstants.POLICIES);
					if (null != policiesArry) {
						for (JsonNode policyNode : policiesArry) {
							currentpolicies.add(policyNode.asText());
						}
					}
				} catch (IOException e) {
					log.error(e);
					log.error(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE, "Exception while creating currentpolicies")
									.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
				}
				policies.addAll(currentpolicies);
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);
				policies.add(policy);
			} else {
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
						.body("{\"errors\":[\"Either Approle doesn't exists or you dont have enough permission to add this approle to IAM Service Account\"]}");
			}
			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE, String.format("policies [%s] before calling configureApprole", policies))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));

			Response approleControllerResp = appRoleService.configureApprole(approleName, policiesString, token);

			if (approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)
					|| approleControllerResp.getHttpstatus().equals(HttpStatus.OK)) {
				String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(uniqueIAMSvcaccName)
						.toString();
				Map<String, String> params = new HashMap<>();
				params.put("type", TVaultConstants.APP_ROLES);
				params.put("name", approleName);
				params.put("path", path);
				params.put("access", access);
				Response metadataResponse = ControllerUtil.updateMetadata(params, token);
				if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
						|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
					log.debug(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE,String.format("Approle[%s] successfully associated to IAM Service Account [%s] with policy [%s].",iamServiceAccountApprole.getApprolename(),iamServiceAccountApprole.getIamSvcAccName(),iamServiceAccountApprole.getAccess()))
									.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					return ResponseEntity.status(HttpStatus.OK)
							.body("{\"messages\":[\"Approle successfully associated with IAM Service Account\"]}");
				}
				approleControllerResp = appRoleService.configureApprole(approleName, currentpoliciesString, token);
				if (approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.error(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE, "Reverting, Approle policy update success")
									.put(LogMessage.RESPONSE,
											(null != metadataResponse) ? metadataResponse.getResponse()
													: TVaultConstants.EMPTY)
									.put(LogMessage.STATUS,
											(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
													: TVaultConstants.EMPTY)
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
				} else {
					log.error(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE, "Reverting Approle policy update failed")
									.put(LogMessage.RESPONSE,
											(null != metadataResponse) ? metadataResponse.getResponse()
													: TVaultConstants.EMPTY)
									.put(LogMessage.STATUS,
											(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
													: TVaultConstants.EMPTY)
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
				}
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"Failed to add Approle to the IAM Service Account\"]}");
			}
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to add Approle to this iam service account\"]}");
		}
	}

	/**
	 * Check if user has the permission to add user/group/awsrole/approles to
	 * the IAM Service Account
	 *
	 * @param userDetails
	 * @param action
	 * @param token
	 * @return
	 */
	public boolean hasAddOrRemovePermission(UserDetails userDetails, String serviceAccount, String token) {
		// Owner of the service account can add/remove users, groups, aws roles
		// and approles to service account

		String ownerPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(serviceAccount).toString();
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);
		if (ArrayUtils.contains(policies, ownerPolicy)) {
			return true;
		}
		return false;
	}

	/**
	 * Remove approle from IAM service account
	 *
	 * @param userDetails
	 * @param token
	 * @param serviceAccountApprole
	 * @return
	 */
	public ResponseEntity<String> removeApproleFromIAMSvcAcc(UserDetails userDetails, String token,
			IAMServiceAccountApprole iamServiceAccountApprole) {
		log.debug(
				JSONUtil.getJSON(
						ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_APPROLE_TO_IAMSVCACC_MSG)
								.put(LogMessage.MESSAGE,
										String.format("Trying to remove approle from IAM Service Account [%s]",
												iamServiceAccountApprole.getApprolename()))
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		String approleName = iamServiceAccountApprole.getApprolename();
		String uniqueIAMSvcaccName = iamServiceAccountApprole.getAwsAccountId() + "_" + iamServiceAccountApprole.getIamSvcAccName();
		String access = iamServiceAccountApprole.getAccess();
		approleName = (approleName != null) ? approleName.toLowerCase() : approleName;
		access = (access != null) ? access.toLowerCase() : access;

		if (Arrays.asList(TVaultConstants.MASTER_APPROLES).contains(iamServiceAccountApprole.getApprolename())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Access denied: no permission to remove this AppRole to any Service Account\"]}");
		}
		if (!isIamSvcaccPermissionInputValid(access)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(INVALIDVALUEERROR);
		}

		boolean isAuthorized = hasAddOrRemovePermission(userDetails, uniqueIAMSvcaccName, token);

		if (isAuthorized) {
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();
			String ownerPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcaccName).toString();

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_APPROLE_TO_IAMSVCACC_MSG)
					.put(LogMessage.MESSAGE,
							String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy,
									writePolicy, denyPolicy, ownerPolicy))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			Response roleResponse = reqProcessor.process("/auth/approle/role/read",
					"{\"role_name\":\"" + approleName + "\"}", token);
			String responseJson = "";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			if (HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
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
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);
			}
			else {
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
						.body("{\"errors\":[\"Either Approle doesn't exists or you don't have enough permission to remove this approle from IAM Service Account\"]}");
			}

			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			log.info(
					JSONUtil.getJSON(
							ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE,
											"Remove approle from IAM Service account -  policy :" + policiesString
													+ " is being configured")
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
			// Update the policy for approle
			Response approleControllerResp = appRoleService.configureApprole(approleName, policiesString, token);
			if (approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)
					|| approleControllerResp.getHttpstatus().equals(HttpStatus.OK)) {
				String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(uniqueIAMSvcaccName).toString();
				Map<String, String> params = new HashMap<String, String>();
				params.put("type", "app-roles");
				params.put("name", approleName);
				params.put("path", path);
				params.put("access", "delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params, token);
				if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
						|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
					log.debug(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE,String.format("Approle [%s] is successfully removed from IAM Service Account [%s].",iamServiceAccountApprole.getApprolename(),iamServiceAccountApprole.getIamSvcAccName()))
									.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					return ResponseEntity.status(HttpStatus.OK)
							.body("{\"messages\":[\"Approle is successfully removed(if existed) from IAM Service Account\"]}");
				}
				approleControllerResp = appRoleService.configureApprole(approleName, currentpoliciesString, token);
				if (approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.error(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE, "Reverting, approle policy update success")
									.put(LogMessage.RESPONSE,
											(null != metadataResponse) ? metadataResponse.getResponse()
													: TVaultConstants.EMPTY)
									.put(LogMessage.STATUS,
											(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
													: TVaultConstants.EMPTY)
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
				} else {
					log.error(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_APPROLE_TO_IAMSVCACC_MSG)
									.put(LogMessage.MESSAGE, "Reverting approle policy update failed")
									.put(LogMessage.RESPONSE,
											(null != metadataResponse) ? metadataResponse.getResponse()
													: TVaultConstants.EMPTY)
									.put(LogMessage.STATUS,
											(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
													: TVaultConstants.EMPTY)
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
				}
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"Failed to remove approle from the Service Account\"]}");
			}
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to remove approle from Service Account\"]}");
		}
	}

	/**
	 * Activate IAM Service Account.
	 * @param token
	 * @param userDetails
	 * @param iamServiceAccountName
	 * @param awsAccountId
	 * @return
	 */
	public ResponseEntity<String> activateIAMServiceAccount(String token, UserDetails userDetails, String iamServiceAccountName, String awsAccountId) {
		iamServiceAccountName = iamServiceAccountName.toLowerCase();

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "activateIAMServiceAccount").
				put(LogMessage.MESSAGE, String.format ("Trying to activate ServiceAccount [%s]", iamServiceAccountName)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String uniqueIAMSvcaccName = awsAccountId + "_" + iamServiceAccountName;

		boolean isAuthorized = true;
		if (userDetails != null) {
			isAuthorized = isAuthorizedToAddPermissionInIAMSvcAcc(userDetails, uniqueIAMSvcaccName, token, false);
		}
		if (isAuthorized) {
			if (isIAMSvcaccActivated(token, userDetails, uniqueIAMSvcaccName)) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "activateIAMServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Failed to activate IAM Service account. [%s] is already activated.", uniqueIAMSvcaccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Service Account is already activated. You can now grant permissions from Permissions menu\"]}");
			}

			JsonObject iamMetadataJson = getIAMMetadata(token, uniqueIAMSvcaccName);

			if (null!= iamMetadataJson && iamMetadataJson.has("secret")) {
				if (!iamMetadataJson.get("secret").isJsonNull()) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "activateIAMServiceAccount").
							put(LogMessage.MESSAGE, String.format ("Trying to rotate secret for the IAM Service account [%s]", uniqueIAMSvcaccName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));

					JsonArray svcSecretArray = null;
					try {
						svcSecretArray = iamMetadataJson.get("secret").getAsJsonArray();
					} catch (IllegalStateException e) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, "activateIAMServiceAccount").
								put(LogMessage.MESSAGE, String.format ("Failed to activate IAM Service account. Invalid metadata for [%s].", uniqueIAMSvcaccName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to activate IAM Service account. Invalid metadata.\"]}");
					}

					if (null != svcSecretArray) {
						int secretSaveCount = 0;
						for (int i=0;i<svcSecretArray.size();i++) {

							JsonObject iamSecret = (JsonObject) svcSecretArray.get(i);
							if (iamSecret.has(IAMServiceAccountConstants.ACCESS_KEY_ID)) {
								String accessKeyId = iamSecret.get(IAMServiceAccountConstants.ACCESS_KEY_ID).getAsString();
								log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
										put(LogMessage.ACTION, "activateIAMServiceAccount").
										put(LogMessage.MESSAGE, String.format ("Trying to rotate secret for the IAM Service account [%s] access key id: [%s]", uniqueIAMSvcaccName, accessKeyId)).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
										build()));
								// Rotate IAM service account secret for each access key id in metadata
								if (rotateIAMServiceAccountByAccessKeyId(token, awsAccountId, iamServiceAccountName, accessKeyId, i+1)) {
									secretSaveCount++;
								}
							}
						}
						if (secretSaveCount == svcSecretArray.size()) {
							// Update status to activated.
							Response metadataUpdateResponse = iamServiceAccountUtils.updateActivatedStatusInMetadata(token, iamServiceAccountName, awsAccountId);
							if(metadataUpdateResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataUpdateResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataUpdateResponse.getHttpstatus()))){
								log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
										put(LogMessage.ACTION, "activateIAMServiceAccount").
										put(LogMessage.MESSAGE, String.format("Metadata updated Successfully for IAM service account [%s].", iamServiceAccountName)).
										put(LogMessage.STATUS, metadataUpdateResponse.getHttpstatus().toString()).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
										build()));
								// Add rotate permission for owner
								String ownerNTId = getOwnerNTIdFromMetadata(token, uniqueIAMSvcaccName );
								if (StringUtils.isEmpty(ownerNTId)) {
									log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
											put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
											put(LogMessage.ACTION, "activateIAMServiceAccount").
											put(LogMessage.MESSAGE, String.format("Failed to add rotate permission for owner for IAM service account [%s]. Owner NT id not found in metadata", iamServiceAccountName)).
											put(LogMessage.STATUS, HttpStatus.BAD_REQUEST.toString()).
											put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
											build()));
									return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"Failed to activate IAM Service account. IAM secrets are rotated and saved in T-Vault. However failed to add permission to owner. Owner info not found in Metadata.\"]}");
								}
								IAMServiceAccountUser iamServiceAccountUser = new IAMServiceAccountUser(iamServiceAccountName, ownerNTId, IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING, awsAccountId);

								ResponseEntity<String> addUserToIAMSvcAccResponse = addUserToIAMServiceAccount(token, userDetails, iamServiceAccountUser, false);
								if (HttpStatus.OK.equals(addUserToIAMSvcAccResponse.getStatusCode())) {
									log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
											put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
											put(LogMessage.ACTION, "activateIAMServiceAccount").
											put(LogMessage.MESSAGE, String.format ("IAM Service account [%s] activated successfully", iamServiceAccountName)).
											put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
											build()));
									return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"IAM Service account activated successfully\"]}");

								}
								log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
										put(LogMessage.ACTION, "activateIAMServiceAccount").
										put(LogMessage.MESSAGE, String.format("Failed to add rotate permission to owner as part of IAM service account activation for [%s].", iamServiceAccountName)).
										put(LogMessage.STATUS, addUserToIAMSvcAccResponse!=null?addUserToIAMSvcAccResponse.getStatusCode().toString():HttpStatus.BAD_REQUEST.toString()).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
										build()));
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate IAM Service account. IAM secrets are rotated and saved in T-Vault. However owner permission update failed.\"]}");

							}
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate IAM Service account. IAM secrets are rotated and saved in T-Vault. However metadata update failed.\"]}");
						}
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, "activateIAMServiceAccount").
								put(LogMessage.MESSAGE, String.format ("IAM Service account [%s] activated successfully", iamServiceAccountName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate IAM Service account. Failed to rotate secrets for one or more AccessKeyIds.\"]}");
					}
				}
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "activateIAMServiceAccount").
						put(LogMessage.MESSAGE, String.format ("Failed to activate IAM Service account. Invalid metadata for [%s].", uniqueIAMSvcaccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to activate IAM Service account. Invalid metadata.\"]}");
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "activateIAMServiceAccount").
						put(LogMessage.MESSAGE, String.format ("AccessKey information not found in metadata for IAM Service account [%s]", uniqueIAMSvcaccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"AccessKey information not found in metadata for this IAM Service account\"]}");
			}

		}
		else{
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: No permission to activate this IAM service account\"]}");
		}
	}

	/**
	 * To get owner NT id from metadata.
	 * @param token
	 * @param uniqueIAMSvcaccName
	 * @return
	 */
	private String getOwnerNTIdFromMetadata(String token, String uniqueIAMSvcaccName) {
		JsonObject iamMetadataJson = getIAMMetadata(token, uniqueIAMSvcaccName);
		if (null != iamMetadataJson && iamMetadataJson.has(IAMServiceAccountConstants.OWNER_NT_ID)) {
			return iamMetadataJson.get(IAMServiceAccountConstants.OWNER_NT_ID).getAsString();
		}
		return null;
	}

	/**
	 * To get IAM Service Account metadata as JsonObject.
	 * @param token
	 * @param uniqueIAMSvcaccName
	 * @return
	 */
	private JsonObject getIAMMetadata(String token, String uniqueIAMSvcaccName) {
		String path = TVaultConstants.IAM_SVC_PATH + uniqueIAMSvcaccName;
		Response response = reqProcessor.process("/read", PATHSTR + path + "\"}", token);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			return populateMetaData(response);
		}
		return null;
	}

	/**
	 * To rotate an IAM Service Account secret.
	 * @param token
	 * @param userDetails
	 * @param iamServiceAccountRotateRequest
	 * @param secretFolder
	 * @return
	 */
	public ResponseEntity<String> rotateIAMServiceAccount(String token, IAMServiceAccountRotateRequest iamServiceAccountRotateRequest) {
		boolean rotationStatus = false;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
				put(LogMessage.MESSAGE, String.format ("Trying to rotate secret for the IAM Service account [%s] " +
								"access key id: [%s]", iamServiceAccountRotateRequest.getUserName(),
						iamServiceAccountRotateRequest.getAccessKeyId())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String accessKeyId = iamServiceAccountRotateRequest.getAccessKeyId();
		String awsAccountId = iamServiceAccountRotateRequest.getAccountId();
		String iamSvcName = iamServiceAccountRotateRequest.getUserName().toLowerCase();
		String uniqueIAMSvcaccName = awsAccountId + "_" + iamSvcName;

		if (!hasResetPermissionForIAMServiceAccount(token, uniqueIAMSvcaccName)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
					put(LogMessage.MESSAGE, String.format("Access denited. No permisison to rotate IAM secret for [%s].", uniqueIAMSvcaccName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: No permission to rotate secret for IAM service account.\"]}");
		}

		// Get metadata to check the accesskeyid
		JsonObject iamMetadataJson = getIAMMetadata(token, uniqueIAMSvcaccName);

		if (null!= iamMetadataJson && iamMetadataJson.has("secret")) {
			if (!iamMetadataJson.get("secret").isJsonNull()) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
						put(LogMessage.MESSAGE, String.format("Trying to rotate secret for the IAM Service account [%s]", uniqueIAMSvcaccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));

				JsonArray svcSecretArray = null;
				try {
					svcSecretArray = iamMetadataJson.get("secret").getAsJsonArray();
				} catch (IllegalStateException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
							put(LogMessage.MESSAGE, String.format("Failed to rotate IAM secret. Invalid metadata for [%s].", uniqueIAMSvcaccName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to rotate secret for IAM Service account. Invalid metadata.\"]}");
				}

				if (null != svcSecretArray) {
					for (int i = 0; i < svcSecretArray.size(); i++) {

						JsonObject iamSecret = (JsonObject) svcSecretArray.get(i);
						if (iamSecret.has(IAMServiceAccountConstants.ACCESS_KEY_ID) && accessKeyId
								.equals(iamSecret.get(IAMServiceAccountConstants.ACCESS_KEY_ID).getAsString())) {
							log.debug(
									JSONUtil.getJSON(ImmutableMap.<String, String> builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
											.put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE)
											.put(LogMessage.MESSAGE,
													String.format(
															"Trying to rotate secret for the IAM Service account [%s] access key id: [%s]",
															uniqueIAMSvcaccName, accessKeyId))
											.put(LogMessage.APIURL,
													ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
											.build()));
							// Rotate IAM service account secret for each access
							// key id in metadata
							rotationStatus = rotateIAMServiceAccountByAccessKeyId(token, awsAccountId, iamSvcName,
									accessKeyId, i + 1);
							break;
						}
					}
				}
			}
		}
		else {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE)
							.put(LogMessage.MESSAGE,
									String.format(
											"Failed to rotate secret for AccesskeyId [%s] for IAM Service "
													+ "account [%s]",
											iamServiceAccountRotateRequest.getAccessKeyId(),
											iamServiceAccountRotateRequest.getUserName()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"AccessKey information not found in metadata for this IAM Service account\"]}");
		}

		if (rotationStatus) {
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
					put(LogMessage.MESSAGE, String.format ("IAM Service account [%s] rotated successfully for " +
									"AccessKeyId [%s]", iamServiceAccountRotateRequest.getUserName(),
							iamServiceAccountRotateRequest.getAccessKeyId())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"IAM Service account secret rotated successfully\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
				put(LogMessage.MESSAGE, String.format ("Failed to rotate secret for AccesskeyId [%s] for IAM Service " +
								"account [%s]", iamServiceAccountRotateRequest.getAccessKeyId(),
						iamServiceAccountRotateRequest.getUserName())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to rotate secret for IAM Service account Access Key Id\"]}");
	}

	/**
	 * Method to check if the user/approle has reset permission.
	 * @param token
	 * @param uniqueIAMSvcaccName
	 * @return
	 */
	private boolean hasResetPermissionForIAMServiceAccount(String token, String uniqueIAMSvcaccName) {
		String resetPermission = "w_"+ IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX + uniqueIAMSvcaccName;
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> currentPolicies = new ArrayList<>();
		List<String> identityPolicies = new ArrayList<>();
		Response response = reqProcessor.process("/auth/tvault/lookup","{}", token);
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();
			try {
				currentPolicies = iamServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(objectMapper, responseJson);
				identityPolicies = iamServiceAccountUtils.getIdentityPoliciesAsListFromTokenLookupJson(objectMapper, responseJson);
				if (currentPolicies.contains(resetPermission) || identityPolicies.contains(resetPermission)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "hasResetPermissionForIAMServiceAccount")
							.put(LogMessage.MESSAGE, "User has reset permission on this IAM service account.")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
					return true;
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "hasResetPermissionForIAMServiceAccount")
						.put(LogMessage.MESSAGE,
								"Failed to parse policies from token")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "hasResetPermissionForIAMServiceAccount")
				.put(LogMessage.MESSAGE, "Access denied. User is not permitted for IAM secret rotation.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;
	}

	/**
	 * Rotate secret for an accessKeyID in an IAM Service Account.
	 * @param token
	 * @param awsAccountId
	 * @param iamServiceAccountName
	 * @param accessKeyId
	 * @return
	 */
	private boolean rotateIAMServiceAccountByAccessKeyId(String token,  String awsAccountId, String iamServiceAccountName, String accessKeyId, int accessKeyIndex) {
		String uniqueIAMSvcaccName = awsAccountId + "_" + iamServiceAccountName;
		IAMServiceAccountRotateRequest iamServiceAccountRotateRequest = new IAMServiceAccountRotateRequest(accessKeyId, iamServiceAccountName, awsAccountId);

		IAMServiceAccountSecret iamServiceAccountSecret = iamServiceAccountUtils.rotateIAMSecret(iamServiceAccountRotateRequest);

		if (null != iamServiceAccountSecret) {
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "rotate IAM Service Account By AccessKeyId").
					put(LogMessage.MESSAGE, String.format ("IAM Service account [%s] rotated successfully for " +
									"AccessKeyId [%s]", iamServiceAccountRotateRequest.getUserName(),
							iamServiceAccountRotateRequest.getAccessKeyId())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			// Save secret in iamavcacc mount
			String path = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + uniqueIAMSvcaccName + "/" + IAMServiceAccountConstants.IAM_SECRET_FOLDER_PREFIX + (accessKeyIndex);
			if (iamServiceAccountUtils.writeIAMSvcAccSecret(token, path, iamServiceAccountName, iamServiceAccountSecret)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "rotate IAM Service Account By AccessKeyId").
						put(LogMessage.MESSAGE, "Secret saved to IAM service account mount").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				Response metadataUdpateResponse = iamServiceAccountUtils.updateIAMSvcAccNewAccessKeyIdInMetadata(token, awsAccountId, iamServiceAccountName, accessKeyId, iamServiceAccountSecret);
				if (null != metadataUdpateResponse && HttpStatus.NO_CONTENT.equals(metadataUdpateResponse.getHttpstatus())) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "rotate IAM Service Account By AccessKeyId").
							put(LogMessage.MESSAGE, "Updated IAM service account metadata with new AccessKeyId and expiry").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Read Secrets.
	 * 
	 * @param token
	 * @param awsAccountID
	 * @param iamSvcName
	 * @param accessKey
	 * @return
	 * @throws IOException
	 */
	public ResponseEntity<String> readSecrets(String token, String awsAccountID, String iamSvcName, String accessKey)
			throws IOException {

		iamSvcName = iamSvcName.toLowerCase();
		String iamSvcNamePath = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + awsAccountID + '_' + iamSvcName;
		ResponseEntity<String> response = readFolders(token, iamSvcNamePath);
		ObjectMapper mapper = new ObjectMapper();
		String secret = "";
		if (HttpStatus.OK.equals(response.getStatusCode())) {
			IAMServiceAccountNode iamServiceAccountNode = mapper.readValue(response.getBody(),
					IAMServiceAccountNode.class);
			if (iamServiceAccountNode.getFolders() != null) {
				for (String folderName : iamServiceAccountNode.getFolders()) {
					ResponseEntity<String> responseEntity = getIAMServiceAccountSecretKey(token,
							awsAccountID + '_' + iamSvcName, folderName);
					if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
						IAMServiceAccountSecret iamServiceAccountSecret = mapper.readValue(responseEntity.getBody(),
								IAMServiceAccountSecret.class);
						if (accessKey.equals(iamServiceAccountSecret.getAccessKeyId())) {
							secret = iamServiceAccountSecret.getAccessKeySecret();
							break;
						}
					} else {
						return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":"
								+ JSONUtil.getJSON("No secret found for the accesskeyID :" + accessKey + "") + "}");
					}
				}
				if (StringUtils.isEmpty(secret)) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":"
							+ JSONUtil.getJSON("No secret found for the accesskeyID :" + accessKey + "") + "}");
				}
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"accessKeySecret\":" + JSONUtil.getJSON(secret) + "}");
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
						"{\"error\":" + JSONUtil.getJSON("No secret found for the accesskeyID :" + accessKey + "") + "}");
			}
		} else if (HttpStatus.FORBIDDEN.equals(response.getStatusCode())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\":"
					+ JSONUtil.getJSON("Access denied: No permission to read secret for IAM service account") + "}");
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					"{\"error\":" + JSONUtil.getJSON("aws_account_id or iam_svc_name not found") + "}");
		}
	}

	/**
	 * Method to offboard IAM service account.
	 * @param token
	 * @param iamServiceAccount
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> offboardIAMServiceAccount(String token, IAMServiceAccountOffboardRequest iamServiceAccount,
															UserDetails userDetails) {
		String managedBy = "";
		String iamSvcName = iamServiceAccount.getIamSvcAccName().toLowerCase();
		String awsAcountId = iamServiceAccount.getAwsAccountId();
		String uniqueIAMSvcName = awsAcountId + "_" + iamSvcName;
		String selfSupportToken = tokenUtils.getSelfServiceToken();
		if (!isAuthorizedForIAMOnboardAndOffboard(token)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							"Access denied. Not authorized to perform offboarding of IAM service accounts.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
					"{\"errors\":[\"Access denied. Not authorized to perform offboarding of IAM service accounts.\"]}");
		}

		boolean policyDeleteStatus = deleteIAMServiceAccountPolicies(selfSupportToken, uniqueIAMSvcName);
		if (!policyDeleteStatus) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Failed to delete some of the policies for iam service " +
							"account [%s]", iamSvcName))
					.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"errors\":[\"Failed to Offboard IAM service account. Policy deletion failed.\"]}");
		}

		// delete users,groups,aws-roles,app-roles from service account
		String path = IAMServiceAccountConstants.IAM_SVCC_ACC_META_PATH + uniqueIAMSvcName;
		Response metaResponse = reqProcessor.process("/sdb", PATHSTR + path + "\"}", token);
		Map<String, Object> responseMap = null;
		try {
			responseMap = new ObjectMapper().readValue(metaResponse.getResponse(),
					new TypeReference<Map<String, Object>>() {
					});
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Error Fetching metadata for iam service account " +
							" [%s]", iamSvcName))
					.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to Offboard IAM service account. Error Fetching metadata for iam service account\"]}");
		}
		if (responseMap != null && responseMap.get("data") != null) {
			Map<String, Object> metadataMap = (Map<String, Object>) responseMap.get("data");
			Map<String, String> approles = (Map<String, String>) metadataMap.get("app-roles");
			Map<String, String> groups = (Map<String, String>) metadataMap.get("groups");
			Map<String, String> users = (Map<String, String>) metadataMap.get("users");
            Map<String,String> awsroles = (Map<String, String>)metadataMap.get("aws-roles");
			// always add owner to the users list whose policy should be updated
			managedBy = (String) metadataMap.get(IAMServiceAccountConstants.OWNER_NT_ID);
			if (!org.apache.commons.lang3.StringUtils.isEmpty(managedBy)) {
				if (null == users) {
					users = new HashMap<>();
				}
				users.put(managedBy, "sudo");
			}

			updateUserPolicyAssociationOnIAMSvcaccDelete(uniqueIAMSvcName, users, selfSupportToken, userDetails);
			updateGroupPolicyAssociationOnIAMSvcaccDelete(uniqueIAMSvcName, groups, selfSupportToken, userDetails);
			updateApprolePolicyAssociationOnIAMSvcaccDelete(uniqueIAMSvcName, approles, selfSupportToken);
            deleteAwsRoleonOnIAMOffboard(iamSvcName, awsroles, selfSupportToken);
		}

		OnboardedIAMServiceAccount iamSvcAccToOffboard = new OnboardedIAMServiceAccount(iamSvcName,
				awsAcountId, managedBy);
		//delete iam service account secrets and mount details
		ResponseEntity<String> secretDeletionResponse = deleteIAMSvcAccountSecrets(token, iamSvcAccToOffboard);
		if (HttpStatus.OK.equals(secretDeletionResponse.getStatusCode())) {
			// Remove metadata...
			ResponseEntity<String> metadataResponse = deleteIAMSvcAccount(token, iamSvcAccToOffboard);
			if(HttpStatus.OK.equals(metadataResponse.getStatusCode())){
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_OFFBOARD_CREATION_TITLE).
						put(LogMessage.MESSAGE, String.format("Successfully offboarded IAM service account [%s]from T-Vault.", iamServiceAccount.getIamSvcAccName())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body(
						"{\"messages\":[\"Successfully offboarded IAM service account (if existed) from T-Vault\"]}");
			}else{
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(
						"{\"errors\":[\"Failed to offboard IAM service account from TVault\"]}");
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, IAMServiceAccountConstants.IAM_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Failed to offboard IAM service account [%s] from TVault",
							iamSvcName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(
					"{\"errors\":[\"Failed to offboard IAM service account from TVault\"]}");
		}
	}

    /**
     * Aws role deletion as part of IAM Service account Offboarding
     * @param iamSvcAccName
     * @param acessInfo
     * @param token
     */
    private void deleteAwsRoleonOnIAMOffboard(String iamSvcAccName, Map<String,String> acessInfo, String token) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "deleteAwsRoleonOnIAMOffboard").
                put(LogMessage.MESSAGE, String.format ("Trying to delete AwsRole on offboarding of IAM Service Account [%s]", iamSvcAccName)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        if(acessInfo!=null){
            Set<String> roles = acessInfo.keySet();
            for(String role : roles){
                Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);
                if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "deleteAwsRoleonOnIAMOffboard").
                            put(LogMessage.MESSAGE, String.format ("%s, AWS Role is deleted as part of offboarding IAM Service account [%s].", role, iamSvcAccName)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                }else{
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "deleteAwsRoleonOnIAMOffboard").
                            put(LogMessage.MESSAGE, String.format ("%s, AWS Role deletion as part of offboarding IAM Service account [%s] failed.", role, iamSvcAccName)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                }
            }
        }
    }

	/**
	 * Update User policy on IAM Service account offboarding
	 * @param iamSvcAccName
	 * @param acessInfo
	 * @param token
	 * @param userDetails
	 */
	private void updateUserPolicyAssociationOnIAMSvcaccDelete(String iamSvcAccName, Map<String, String> acessInfo,
															  String token, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "updateUserPolicyAssociationOnIAMSvcaccDelete")
				.put(LogMessage.MESSAGE, String.format("Trying to delete user policies on IAM service account delete " +
						"of [%s]", iamSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (acessInfo != null) {
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

			Set<String> users = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for (String userName : users) {

				Response userResponse = new Response();
				if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
					userResponse = reqProcessor.process(READPATH, "{\"username\":\"" + userName + "\"}",
							token);
				} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					userResponse = reqProcessor.process(USERPATH, "{\"username\":\"" + userName + "\"}",
							token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					// OIDC implementation changes
					ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName,
							null, true);
					if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
						if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "updateUserPolicyAssociationOnIAMSvcaccDelete")
									.put(LogMessage.MESSAGE, String.format("Failed to fetch OIDC user policies for [%s]"
											, userName))
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
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
				String responseJson = "";
				String groups = "";
				List<String> policies = new ArrayList<>();
				List<String> currentpolicies = new ArrayList<>();

				if (HttpStatus.OK.equals(userResponse.getHttpstatus())) {
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
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateUserPolicyAssociationOnIAMSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("updateUserPolicyAssociationOnIAMSvcaccDelete " +
												"failed [%s]", e.getMessage()))
								.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					}
					policies.addAll(currentpolicies);
					policies.remove(readPolicy);
					policies.remove(writePolicy);
					policies.remove(denyPolicy);
					policies.remove(ownerPolicy);

					String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");

					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "updateUserPolicyAssociationOnIAMSvcaccDelete")
							.put(LogMessage.MESSAGE, String.format("Current policies [%s]", policies))
							.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
					if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateUserPolicyAssociationOnIAMSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("Current policies userpass [%s]", policies))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						ControllerUtil.configureUserpassUser(userName, policiesString, token);
					} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateUserPolicyAssociationOnIAMSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("Current policies ldap [%s]", policies))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						ControllerUtil.configureLDAPUser(userName, policiesString, groups, token);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						// OIDC Implementation : Entity Update
						try {
							oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
							oidcUtil.renewUserToken(userDetails.getClientToken());
						} catch (Exception e) {
							log.error(e);
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "updateUserPolicyAssociationOnIAMSvcaccDelete")
									.put(LogMessage.MESSAGE, "Exception while adding or updating the identity ")
									.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
									.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
						}
					}
				}
			}
		}
	}

	/**
	 * Update Group policy on IAM Service account offboarding
	 *
	 * @param iamSvcAccountName
	 * @param acessInfo
	 * @param token
	 * @param userDetails
	 */
	private void updateGroupPolicyAssociationOnIAMSvcaccDelete(String iamSvcAccountName, Map<String, String> acessInfo,
															   String token, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnIAMSvcaccDelete")
				.put(LogMessage.MESSAGE, String.format("trying to delete group policies on IAM service account delete " +
						"for [%s]", iamSvcAccountName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnIAMSvcaccDelete")
					.put(LogMessage.MESSAGE, "Inside userpass of updateGroupPolicyAssociationOnIAMSvcaccDelete...Just Returning...")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return;
		}
		if (acessInfo != null) {
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

			Set<String> groups = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for (String groupName : groups) {
				Response response = new Response();
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					response = reqProcessor.process(GROUPPATH, GROUPNAME + groupName + "\"}",
							token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					// call read api with groupname
					oidcGroup = oidcUtil.getIdentityGroupDetails(groupName, token);
					if (oidcGroup != null) {
						response.setHttpstatus(HttpStatus.OK);
						response.setResponse(oidcGroup.getPolicies().toString());
					} else {
						response.setHttpstatus(HttpStatus.BAD_REQUEST);
					}
				}

				String responseJson = TVaultConstants.EMPTY;
				List<String> policies = new ArrayList<>();
				List<String> currentpolicies = new ArrayList<>();
				if (HttpStatus.OK.equals(response.getHttpstatus())) {
					responseJson = response.getResponse();
					try {
						// OIDC Changes
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
						} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && oidcGroup != null) {
							currentpolicies.addAll(oidcGroup.getPolicies());
						}
					} catch (IOException e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnIAMSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("updateGroupPolicyAssociationOnIAMSvcaccDelete " +
												"failed [%s]", e.getMessage()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					}
					policies.addAll(currentpolicies);
					policies.remove(readPolicy);
					policies.remove(writePolicy);
					policies.remove(denyPolicy);
					policies.remove(sudoPolicy);
					String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnIAMSvcaccDelete")
							.put(LogMessage.MESSAGE, String.format("Current policies [%s]", policies))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies, oidcGroup != null ? oidcGroup.getId() : null);
						oidcUtil.renewUserToken(userDetails.getClientToken());
					}
				}
			}
		}
	}

	/**
	 * Approle policy update as part of offboarding
	 *
	 * @param iamSvcAccountName
	 * @param acessInfo
	 * @param token
	 */
	private void updateApprolePolicyAssociationOnIAMSvcaccDelete(String iamSvcAccountName,
																 Map<String, String> acessInfo, String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "updateApprolePolicyAssociationOnIAMSvcaccDelete")
				.put(LogMessage.MESSAGE, String.format("trying to update approle policies on IAM service account " +
						"delete for [%s]", iamSvcAccountName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (acessInfo != null) {
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
			Set<String> approles = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for (String approleName : approles) {
				Response roleResponse = reqProcessor.process("/auth/approle/role/read",
						"{\"role_name\":\"" + approleName + "\"}", token);
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
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateApprolePolicyAssociationOnIAMSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("%s, Approle removal as part of offboarding " +
												"Service account failed.", approleName))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					}
					policies.addAll(currentpolicies);
					policies.remove(readPolicy);
					policies.remove(writePolicy);
					policies.remove(denyPolicy);
					policies.remove(sudoPolicy);

					String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
					log.info( JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "updateApprolePolicyAssociationOnIAMSvcaccDelete")
									.put(LogMessage.MESSAGE, "Current policies :" + policiesString + " is being configured")
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					appRoleService.configureApprole(approleName, policiesString, token);
				}
			}
		}
	}

	/**
	 * Deletes the IAMSvcAccount secret
	 *
	 * @param token
	 * @param iamServiceAccount
	 * @return
	 */
	private ResponseEntity<String> deleteIAMSvcAccountSecrets(String token, OnboardedIAMServiceAccount iamServiceAccount) {
		String iamSvcAccName = iamServiceAccount.getAwsAccountId() + "_" + iamServiceAccount.getUserName();
		String iamSvcAccPath = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + iamSvcAccName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "deleteIAMSvcAccountSecrets")
				.put(LogMessage.MESSAGE, String.format("Trying to delete secret folder for IAM service " +
						ACCOUNTSTR, iamSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		boolean secretDeleteStatus = deleteIAMSvcAccountSecretFolders(token, iamServiceAccount.getAwsAccountId(),
				iamServiceAccount.getUserName());
		if (secretDeleteStatus) {
			Response onboardingResponse = reqProcessor.process(DELETEPATH, PATHSTR + iamSvcAccPath + "\"}", token);

			if (onboardingResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
					|| onboardingResponse.getHttpstatus().equals(HttpStatus.OK)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "deleteIAMSvcAccountSecrets")
						.put(LogMessage.MESSAGE, "Successfully deleted IAM service account Secrets.")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"Successfully deleted IAM service account Secrets.\"]}");
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteIAMSvcAccountSecrets")
					.put(LogMessage.MESSAGE, "Failed to delete IAM service account Secrets.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Failed to delete IAM service account Secrets.\"]}");
		}

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "deleteIAMSvcAccountSecrets")
				.put(LogMessage.MESSAGE, "Failed to delete one or more IAM service account Secret folders.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Failed to delete IAM service account Secrets.\"]}");
	}

	/**
	 * To delete IAM secret folders as part of Offboarding.
	 * @param token
	 * @param awsAccountId
	 * @param iamSvcAccName
	 * @return
	 */
	private boolean deleteIAMSvcAccountSecretFolders(String token, String awsAccountId, String iamSvcAccName) {

		String uniqueSvcAccName = awsAccountId + "_" + iamSvcAccName;
		JsonObject iamMetadataJson = getIAMMetadata(token, uniqueSvcAccName);

		if (null!= iamMetadataJson && iamMetadataJson.has("secret")) {
			if (!iamMetadataJson.get("secret").isJsonNull()) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "deleteIAMSvcAccountSecretFolders").
						put(LogMessage.MESSAGE, String.format("Trying to delete secret folders for the IAM Service " +
								"account [%s]", uniqueSvcAccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));

				JsonArray svcSecretArray = null;
				try {
					svcSecretArray = iamMetadataJson.get("secret").getAsJsonArray();
				} catch (IllegalStateException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "deleteIAMSvcAccountSecretFolders").
							put(LogMessage.MESSAGE, String.format("Failed to get secret folders. Invalid metadata " +
									"for [%s].", uniqueSvcAccName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return false;
				}

				if (null != svcSecretArray) {
					int deleteCount = 0;
					for (int i = 0; i < svcSecretArray.size(); i++) {
						String folderPath = IAMServiceAccountConstants.IAM_SVCC_ACC_PATH + uniqueSvcAccName + "/secret_" + (i+1);
						Response deleteFolderResponse = reqProcessor.process(DELETEPATH,
								PATHSTR + folderPath + "\"}", token);
						if (deleteFolderResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
								|| deleteFolderResponse.getHttpstatus().equals(HttpStatus.OK)) {
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, "deleteIAMSvcAccountSecretFolders").
									put(LogMessage.MESSAGE, String.format("Deleted secret folder [%d] for the IAM Service " +
											"account [%s]", (i+1), uniqueSvcAccName)).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							deleteCount++;
						}
					}
					if (deleteCount == svcSecretArray.size()) {
						return true;
					}
					else {
						return false;
					}
				}
			}
		}
		return true;
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
	 * Add AWS role to IAM Service Account
	 * @param userDetails
	 * @param token
	 * @param serviceAccountAWSRole
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToIAMSvcacc(UserDetails userDetails, String token, IAMServiceAccountAWSRole iamServiceAccountAWSRole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_AWS_ROLE_MSG).
				put(LogMessage.MESSAGE, "Trying to add AWS Role to IAM Service Account").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if(!isIamSvcaccPermissionInputValid(iamServiceAccountAWSRole.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(INVALIDVALUEERROR);
		}
		if (iamServiceAccountAWSRole.getAccess().equalsIgnoreCase(IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING)) {
			iamServiceAccountAWSRole.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String roleName = iamServiceAccountAWSRole.getRolename();
		String iamSvcName = iamServiceAccountAWSRole.getIamSvcAccName().toLowerCase();
		String awsAcountId = iamServiceAccountAWSRole.getAwsAccountId();
		String uniqueIAMSvcName = awsAcountId + "_" + iamSvcName;
		String access = iamServiceAccountAWSRole.getAccess();

		roleName = (roleName !=null) ? roleName.toLowerCase() : roleName;
		access = (access != null) ? access.toLowerCase(): access;

		boolean isAuthorized = hasAddOrRemovePermission(userDetails, uniqueIAMSvcName, token);
		if(isAuthorized){
			String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();


			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_AWS_ROLE_MSG).
					put(LogMessage.MESSAGE, String.format (POLICYSTR, policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			String ownerPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_AWS_ROLE_MSG).
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, ownerPolicy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

			Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+roleName+"\"}",token);
			String responseJson="";
			String authType = TVaultConstants.EC2;
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
					authType = objMapper.readTree(responseJson).get("auth_type").asText();
				} catch (IOException e) {
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_AWS_ROLE_MSG).
                            put(LogMessage.MESSAGE, e.getMessage()).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
				}
				policies.addAll(currentpolicies);
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);
				policies.add(policy);
				policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
				currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			} else{
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"AWS role doesn't exist you don't have enough permission to add this AWS role to IAM Service account!\"]}");
			}
			Response awsRoleConfigresponse = null;
			if (TVaultConstants.IAM.equals(authType)) {
				awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,policiesString,token);
			}
			else {
				awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,policiesString,token);
			}
			if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(uniqueIAMSvcName).toString();
				Map<String,String> params = new HashMap<>();
				params.put("type", "aws-roles");
				params.put("name",roleName);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE,String.format("AWS Role [%s] successfully associated to IAM Service Account [%s] with policy [%s].",iamServiceAccountAWSRole.getRolename(),iamServiceAccountAWSRole.getIamSvcAccName(),iamServiceAccountAWSRole.getAccess())).
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with IAM Service Account\"]}");
				}
				if (TVaultConstants.IAM.equals(authType)) {
					awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,currentpoliciesString,token);
				}
				else {
					awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,currentpoliciesString,token);
				}
				if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE, "Reverting, AWS Role policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
				} else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.ADD_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE, "Reverting AWS Role policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Contact Admin \"]}");
				}
			} else{
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed. Try Again\"]}");
			}
		} else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add AWS Role to this IAM service account\"]}");
		}
	}
	
	/**
	 * Remove AWS Role from IAM service account
	 * @param userDetails
	 * @param token
	 * @param iamServiceAccountAWSRole
	 * @return
	 */
	public ResponseEntity<String> removeAWSRoleFromIAMSvcacc(UserDetails userDetails, String token, IAMServiceAccountAWSRole iamServiceAccountAWSRole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_AWS_ROLE_MSG).
				put(LogMessage.MESSAGE, String.format ("Trying to remove AWS Role from IAM Service Account [%s]", iamServiceAccountAWSRole.getRolename())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if(!isIamSvcaccPermissionInputValid(iamServiceAccountAWSRole.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(INVALIDVALUEERROR);
		}
		if (iamServiceAccountAWSRole.getAccess().equalsIgnoreCase(IAMServiceAccountConstants.IAM_ROTATE_MSG_STRING)) {
			iamServiceAccountAWSRole.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String roleName = iamServiceAccountAWSRole.getRolename();
		String iamSvcName = iamServiceAccountAWSRole.getIamSvcAccName().toLowerCase();
		String awsAcountId = iamServiceAccountAWSRole.getAwsAccountId();
		String uniqueIAMSvcName = awsAcountId + "_" + iamSvcName;

		roleName = (roleName !=null) ? roleName.toLowerCase() : roleName;
		boolean isAuthorized = hasAddOrRemovePermission(userDetails, uniqueIAMSvcName,token);

		if (isAuthorized) {
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			String ownerPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(IAMServiceAccountConstants.IAMSVCACC_POLICY_PREFIX).append(uniqueIAMSvcName).toString();
			
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_AWS_ROLE_MSG).
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, ownerPolicy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

			Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+roleName+"\"}",token);
			String responseJson="";
			String authType = TVaultConstants.EC2;
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
					authType = objMapper.readTree(responseJson).get("auth_type").asText();
				} catch (IOException e) {
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_AWS_ROLE_MSG).
                            put(LogMessage.MESSAGE, e.getMessage()).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
				}
				policies.addAll(currentpolicies);
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);
			} else{
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either AWS role doesn't exist or you don't have enough permission to remove this AWS role from IAM Service account\"]}");
			}

			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_AWS_ROLE_MSG).
					put(LogMessage.MESSAGE, "Remove AWS Role from IAM Service account -  policy :" + policiesString + " is being configured" ).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			Response awsRoleConfigresponse = null;
			if (TVaultConstants.IAM.equals(authType)) {
				awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,policiesString,token);
			}
			else {
				awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,policiesString,token);
			}
			if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(uniqueIAMSvcName).toString();
				Map<String,String> params = new HashMap<>();
				params.put("type", "aws-roles");
				params.put("name",roleName);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE, String.format("AWS Role [%s] is successfully removed from IAM Service Account [%s].", iamServiceAccountAWSRole.getRolename(),iamServiceAccountAWSRole.getIamSvcAccName())).
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from IAM Service Account\"]}");
				}
				if (TVaultConstants.IAM.equals(authType)) {
					awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,currentpoliciesString,token);
				}
				else {
					awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,currentpoliciesString,token);
				}
				if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE, "Reverting, AWS Role policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.REMOVE_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE, "Reverting approle policy update failed").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Contact Admin \"]}");
				}
			}
			else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove AWS Role from the IAM Service Account\"]}");
			}
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove AWS Role from IAM Service Account\"]}");
		}
	}

}
