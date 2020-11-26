package com.tmobile.cso.vault.api.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.tmobile.cso.vault.api.common.IAMServiceAccountConstants;
import com.tmobile.cso.vault.api.model.*;
import org.apache.catalina.User;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.AzureServiceAccountConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.AccessPolicy;
import com.tmobile.cso.vault.api.model.AzureSecrets;
import com.tmobile.cso.vault.api.model.AzureSecretsMetadata;
import com.tmobile.cso.vault.api.model.AzureServiceAccount;
import com.tmobile.cso.vault.api.model.AzureServiceAccountAWSRole;
import com.tmobile.cso.vault.api.model.AzureServiceAccountGroup;
import com.tmobile.cso.vault.api.model.AzureServiceAccountMetadataDetails;
import com.tmobile.cso.vault.api.model.AzureServiceAccountNode;
import com.tmobile.cso.vault.api.model.AzureServiceAccountOffboardRequest;
import com.tmobile.cso.vault.api.model.AzureServiceAccountSecret;
import com.tmobile.cso.vault.api.model.AzureServiceAccountUser;
import com.tmobile.cso.vault.api.model.AzureSvccAccMetadata;
import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryObjectsList;
import com.tmobile.cso.vault.api.model.DirectoryUser;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import com.tmobile.cso.vault.api.model.OIDCGroup;
import com.tmobile.cso.vault.api.model.OnboardedAzureServiceAccount;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.utils.AzureServiceAccountUtils;
import com.tmobile.cso.vault.api.utils.EmailUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

@Component
public class AzureServicePrinicipalAccountsService {
	
	@Autowired
	private RequestProcessor reqProcessor;
	
	@Autowired
	private AccessService accessService;
	
	@Autowired
	private TokenUtils tokenUtils;
	
	@Autowired
    private DirectoryService directoryService;
	
	@Value("${ad.notification.fromemail}")
	private String supportEmail;
	
	@Autowired
	private EmailUtils emailUtils;
	
	@Autowired
	private PolicyUtils policyUtils;
	
	@Value("${vault.auth.method}")
	private String vaultAuthMethod;
	
	@Autowired
	private OIDCUtil oidcUtil;
	
	@Autowired
	AzureServiceAccountUtils azureServiceAccountUtils;
	
	@Autowired
	private AppRoleService appRoleService;
	
	@Autowired
	private AWSAuthService awsAuthService;

	@Autowired
	private AWSIAMAuthService awsiamAuthService;

	@Value("${azurePortal.auth.masterPolicy}")
	private String azureMasterPolicyName;
	
	private static final String[] ACCESS_PERMISSIONS = { "read", "rotate", "deny", "sudo" };
	
	private static Logger log = LogManager.getLogger(AzureServicePrinicipalAccountsService.class);

	
	
	/**
	 * Onboard an Azure service account
	 *
	 * @param token
	 * @param ServiceAccount
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> onboardAzureServiceAccount(String token, AzureServiceAccount azureServiceAccount,
			UserDetails userDetails) {

		if (!isAuthorizedForAzureOnboardAndOffboard(token)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							"Access denied. Not authorized to perform onboarding for Azure service accounts.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Access denied. Not authorized to perform onboarding for Azure service accounts.\"]}");
		}
		azureServiceAccount.setServicePrinicipalName(azureServiceAccount.getServicePrinicipalName().toLowerCase());
		List<String> onboardedList = getOnboardedAzureServiceAccountList(token);
		String azureSvcAccName = azureServiceAccount.getServicePrinicipalName();
		if (onboardedList.contains(azureSvcAccName)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							"Failed to onboard Azure Service Account. Azure Service account is already onboarded")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Failed to onboard Azure Service Account. Azure Service account is already onboarded\"]}");
		}

		String azureSvccAccMetaPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH + azureSvcAccName;

		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = constructAzureSvcAccMetaData(
				azureServiceAccount);

		// Create Metadata
		ResponseEntity<String> metadataCreationResponse = createAzureSvcAccMetadata(token, azureServiceAccountMetadataDetails, azureSvccAccMetaPath);
		if (HttpStatus.OK.equals(metadataCreationResponse.getStatusCode())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							String.format("Successfully created Metadata for the Azure Service Account [%s]",
									azureSvccAccMetaPath))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Creating metadata for Azure Service Account [%s] failed.", azureSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.MULTI_STATUS)
					.body("{\"errors\":[\"Metadata creation failed for Azure Service Account.\"]}");
		}

		// Create policies
		boolean azureSvcAccOwnerPermissionAddStatus = createAzureSvcAccPolicies(azureServiceAccount, azureSvcAccName);
		if (azureSvcAccOwnerPermissionAddStatus) {
			// Add sudo permission to owner
			boolean azureSvcAccCreationStatus = addSudoPermissionToOwner(token, azureServiceAccount, userDetails,
					azureSvcAccName);
			if (azureSvcAccCreationStatus) {
				sendMailToAzureSvcAccOwner(azureServiceAccount, azureSvcAccName);
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"Successfully completed onboarding of Azure service account\"]}");
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, "Failed to onboard Azure service account. Owner association failed.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return rollBackAzureOnboardOnFailure(azureServiceAccount, azureSvcAccName, "onOwnerAssociationFailure");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE, "Failed to onboard  service account. Policy creation failed.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return rollBackAzureOnboardOnFailure(azureServiceAccount, azureSvcAccName, "onPolicyFailure");

	}

	/**
	 * To check if the user/token has permission for onboarding or offboarding
	 *  service account.
	 * 
	 * @param token
	 * @return
	 */
	private boolean isAuthorizedForAzureOnboardAndOffboard(String token) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> currentPolicies = new ArrayList<>();
		Response response = reqProcessor.process("/auth/tvault/lookup", "{}", token);
		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();
			try {
				currentPolicies = azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(objectMapper,
						responseJson);
				if (currentPolicies.contains(azureMasterPolicyName)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
							.put(LogMessage.MESSAGE,
									"The User/Token has required policies to onboard/offboard Azure Service Account.")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
					return true;
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "isAuthorizedForAzureOnboardAndOffboard")
						.put(LogMessage.MESSAGE, "Failed to parse policies from token")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE,
						"The User/Token does not have required policies to onboard/offboard Azure Service Account.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;
	}

	/**
	 * Get onboarded Azure service account list
	 *
	 * @param token
	 * @param userDetails
	 * @return
	 */
	private List<String> getOnboardedAzureServiceAccountList(String token) {
		ResponseEntity<String> onboardedResponse = getAllOnboardedAzureServiceAccounts(token);

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
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "get onboarded  Service Account list")
					.put(LogMessage.MESSAGE, String.format("Error creating onboarded list [%s]", e.getMessage()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return onboardedList;
	}
	
	
	/**
	 * To get all Azure service accounts
	 *
	 * @param token
	 * @return
	 */
	private ResponseEntity<String> getAllOnboardedAzureServiceAccounts(String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "getAllOnboardedAzureServiceAccounts")
				.put(LogMessage.MESSAGE, "Trying to get all onboaded Azure service accounts")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		String metadataPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH;

		Response response = reqProcessor.process("/azure/onboardedlist", "{\"path\":\"" + metadataPath + "\"}", token);

		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getAllOnboardedAzureServiceAccounts")
					.put(LogMessage.MESSAGE, "Successfully retrieved the list of Azure Service Accounts")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		} else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"keys\":[]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Method to populate AzureServiceAccountMetadataDetails object
	 *
	 * @param azureServiceAccount
	 * @return
	 */
	private AzureServiceAccountMetadataDetails constructAzureSvcAccMetaData(AzureServiceAccount azureServiceAccount) {

		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = new AzureServiceAccountMetadataDetails();
		List<AzureSecretsMetadata> azureSecretsMetadatas = new ArrayList<>();
		azureServiceAccountMetadataDetails.setServicePrinicipalName(azureServiceAccount.getServicePrinicipalName());
		azureServiceAccountMetadataDetails.setServicePrinicipalId(azureServiceAccount.getServicePrinicipalId());
		azureServiceAccountMetadataDetails
				.setServicePrinicipalClientId(azureServiceAccount.getServicePrinicipalClientId());
		azureServiceAccountMetadataDetails.setApplicationId(azureServiceAccount.getApplicationId());
		azureServiceAccountMetadataDetails.setApplicationName(azureServiceAccount.getApplicationName());
		azureServiceAccountMetadataDetails.setApplicationTag(azureServiceAccount.getApplicationTag());
		azureServiceAccountMetadataDetails.setCreatedAtEpoch(azureServiceAccount.getCreatedAtEpoch());
		azureServiceAccountMetadataDetails.setOwnerEmail(azureServiceAccount.getOwnerEmail());
		azureServiceAccountMetadataDetails.setOwnerNtid(azureServiceAccount.getOwnerNtid());
		for (AzureSecrets azureSecrets : azureServiceAccount.getSecret()) {
			AzureSecretsMetadata azureSecretsMetadata = new AzureSecretsMetadata();
			azureSecretsMetadata.setSecretKeyId(azureSecrets.getSecretKeyId());
			azureSecretsMetadata.setExpiryDuration(azureSecrets.getExpiryDuration());
			azureSecretsMetadatas.add(azureSecretsMetadata);
		}
		azureServiceAccountMetadataDetails.setSecret(azureSecretsMetadatas);
		azureServiceAccountMetadataDetails.setTenantId(azureServiceAccount.getTenantId());

		return azureServiceAccountMetadataDetails;
	}
	
	/**
	 * To create Metadata for the Azure Service Account
	 *
	 * @param token
	 * @param azureServiceAccountMetadataDetails
	 * @param azureSvccAccMetaPath
	 * @return
	 */
	private ResponseEntity<String> createAzureSvcAccMetadata(String token,
			AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails, String azureSvccAccMetaPath) {

		AzureSvccAccMetadata azureSvccAccMetadata = new AzureSvccAccMetadata(azureSvccAccMetaPath, azureServiceAccountMetadataDetails);

		String jsonStr = JSONUtil.getJSON(azureSvccAccMetadata);
		Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path", azureSvccAccMetaPath);
		String azureSvcAccDataJson = ControllerUtil.convetToJson(rqstParams);

		boolean azureSvcAccMetaDataCreationStatus = ControllerUtil.createMetadata(azureSvcAccDataJson, token);
		if (azureSvcAccMetaDataCreationStatus) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "createAzureSvcAccMetadata")
					.put(LogMessage.MESSAGE,
							String.format("Successfully created metadata for the Azure Service Account [%s]",
									azureServiceAccountMetadataDetails.getServicePrinicipalName()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created Metadata for the Azure Service Account\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "createAzureSvcAccMetadata")
					.put(LogMessage.MESSAGE, "Unable to create Metadata for the Azure Service Account")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Failed to create Metadata for the Azure Service Account\"]}");
	}
	
	/**
	 * Method to create Azure service account policies as part of Azure service account onboarding.
	 * @param azureServiceAccount
	 * @param azureSvcAccName
	 * @return
	 */
	private boolean createAzureSvcAccPolicies(AzureServiceAccount azureServiceAccount, String azureSvcAccName) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE,
						String.format("Trying to create policies for Azure service account [%s].", azureSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		ResponseEntity<String> SvcAccPolicyCreationResponse = createAzureServiceAccountPolicies(azureSvcAccName);
		if (HttpStatus.OK.equals(SvcAccPolicyCreationResponse.getStatusCode())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							String.format("Successfully created policies for Azure service account [%s].", azureSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return true;
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Failed to create policies for Azure service account [%s].",
							azureServiceAccount))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return false;
		}
	}
	
	/**
	 * Create policies for Azure service account
	 *
	 * @param azureSvcAccName
	 * @return
	 */
	private ResponseEntity<String> createAzureServiceAccountPolicies(String azureSvcAccName) {
		int succssCount = 0;
		for (String policyPrefix : TVaultConstants.getSvcAccPolicies().keySet()) {
			AccessPolicy accessPolicy = new AccessPolicy();
			String accessId = new StringBuffer().append(policyPrefix)
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccName).toString();
			accessPolicy.setAccessid(accessId);
			HashMap<String, String> accessMap = new HashMap<>();
			String CredsPath=new StringBuffer().append(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(azureSvcAccName).append("/*").toString();
			accessMap.put(CredsPath, TVaultConstants.getSvcAccPolicies().get(policyPrefix));
			// Attaching write permissions for owner
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.SUDO_POLICY)) {
				accessMap.put(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName + "/*",
						TVaultConstants.WRITE_POLICY);
				accessMap.put(AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH + azureSvcAccName,
						TVaultConstants.WRITE_POLICY);
			}
			if (TVaultConstants.getSvcAccPolicies().get(policyPrefix).equals(TVaultConstants.WRITE_POLICY)) {
				accessMap.put(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName + "/*",
						TVaultConstants.WRITE_POLICY);
				accessMap.put(AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH + azureSvcAccName,
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
					.put(LogMessage.ACTION, "createAzureServiceAccountPolicies")
					.put(LogMessage.MESSAGE, "Successfully created policies for Azure service account.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "createAzureServiceAccountPolicies")
				.put(LogMessage.MESSAGE, "Failed to create some of the policies for Azure service account.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return ResponseEntity.status(HttpStatus.MULTI_STATUS)
				.body("{\"messages\":[\"Failed to create some of the policies for Azure service account\"]}");
	}
	
	/**
	 * Method to send mail to Azure Service account owner
	 *
	 * @param ServiceAccount
	 * @param SvcAccName
	 */
	private void sendMailToAzureSvcAccOwner(AzureServiceAccount azureServiceAccount, String azureSvcAccName) {
		// send email notification to Azure service account owner
		DirectoryUser directoryUser = getUserDetails(azureServiceAccount.getOwnerNtid());
		if (!ObjectUtils.isEmpty(directoryUser)) {
			String from = supportEmail;
			List<String> to = new ArrayList<>();
			to.add(azureServiceAccount.getOwnerEmail());
			String mailSubject = String.format(AzureServiceAccountConstants.AZURE_ONBOARD_EMAIL_SUBJECT, azureSvcAccName);

			// set template variables
			Map<String, String> mailTemplateVariables = new HashMap<>();
			mailTemplateVariables.put("name", directoryUser.getDisplayName());
			mailTemplateVariables.put("azureSvcAccName", azureSvcAccName);

			mailTemplateVariables.put("contactLink", supportEmail);

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION,
							String.format(
									"sendEmail for Azure Service account [%s] -  User " + "email=[%s] - subject = [%s]",
									azureSvcAccName, azureServiceAccount.getOwnerEmail(), mailSubject))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			emailUtils.sendAzureSvcAccHtmlEmalFromTemplate(from, to, mailSubject, mailTemplateVariables);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "sendMailToAzureSvcAccOwner")
					.put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   "
							+ "for an user name =  [%s] ,  Emails might not send to owner for an Azure service account = [%s]",
							azureServiceAccount.getOwnerEmail(), azureSvcAccName))
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
			if (directoryUser.getUserName().equalsIgnoreCase(userName)) {
				break;
			}
		}
		if (!ObjectUtils.isEmpty(directoryUser)) {
			String[] displayName = directoryUser.getDisplayName().split(",");
			if (displayName.length > 1) {
				directoryUser.setDisplayName(displayName[1] + "  " + displayName[0]);
			}
		}
		return directoryUser;
	}
	
	/**
	 * Method to rollback Azure service account onboarding process on failure.
	 * @param azureServiceAccount
	 * @param azureSvcAccName
	 * @param onAction
	 * @return
	 */
	private ResponseEntity<String> rollBackAzureOnboardOnFailure(AzureServiceAccount azureServiceAccount,
				String azureSvcAccName, String onAction) {
		//Delete the Azure Service account policies
		deleteAzureServiceAccountPolicies(tokenUtils.getSelfServiceToken(), azureSvcAccName);
		//Deleting the Azure service account metadata
		OnboardedAzureServiceAccount azureSvcAccToRevert = new OnboardedAzureServiceAccount(
				azureServiceAccount.getServicePrinicipalName(), azureServiceAccount.getOwnerNtid());
		ResponseEntity<String> azureMetaDataDeletionResponse = deleteAzureSvcAccount(tokenUtils.getSelfServiceToken(), azureSvcAccToRevert);
		if (azureMetaDataDeletionResponse != null
				&& HttpStatus.OK.equals(azureMetaDataDeletionResponse.getStatusCode())) {
			if (onAction.equals("onPolicyFailure")) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
						"{\"errors\":[\"Failed to onboard Azure service account. Policy creation failed.\"]}");
			}
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"errors\":[\"Failed to onboard Azure service account. Association of owner permission failed\"]}");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"errors\":[\"Failed to create Azure Service Account policies. Reverting Azure service account creation also failed.\"]}");
		}
	}
	
	/**
	 * Deletes Azure Service Account policies
	 * @param token
	 * @param azureSvcAccName
	 * @return
	 */
	private boolean deleteAzureServiceAccountPolicies(String token, String azureSvcAccName) {
		int succssCount = 0;
		boolean allPoliciesDeleted = false;
		for (String policyPrefix : TVaultConstants.getSvcAccPolicies().keySet()) {
			String accessId = new StringBuffer().append(policyPrefix).append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccName).toString();
			ResponseEntity<String> policyDeleteStatus = accessService.deletePolicyInfo(token, accessId);
			if (HttpStatus.OK.equals(policyDeleteStatus.getStatusCode())) {
				succssCount++;
			}
		}
		if (succssCount == TVaultConstants.getSvcAccPolicies().size()) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "deleteAzureServiceAccountPolicies").
					put(LogMessage.MESSAGE, "Successfully removed policies for Azure service account.").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			allPoliciesDeleted = true;
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "deleteAzureServiceAccountPolicies").
					put(LogMessage.MESSAGE, "Failed to delete some of the policies for Azure service account.").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return allPoliciesDeleted;
	}
	
	/**
	 * Deletes the AzureSvcAccount
	 *
	 * @param token
	 * @param azureServiceAccount
	 * @return
	 */
	private ResponseEntity<String> deleteAzureSvcAccount(String token, OnboardedAzureServiceAccount azureServiceAccount) {
		String azureSvcAccName = azureServiceAccount.getServicePrinicipalName();
		String azureSvcAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH + azureSvcAccName;
		Response onboardingResponse = reqProcessor.process("/delete", "{\"path\":\"" + azureSvcAccPath + "\"}", token);

		if (onboardingResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| onboardingResponse.getHttpstatus().equals(HttpStatus.OK)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteAzureSvcAccount")
					.put(LogMessage.MESSAGE, "Successfully deleted Azure service account.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully deleted Azure service account.\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteAzureSvcAccount")
					.put(LogMessage.MESSAGE, "Failed to delete Azure service account.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Failed to delete Azure service account.\"]}");
		}
	}
	
	/**
	 * Method to add Sudo permission to owner as part of Azure onboarding.
	 * @param token
	 * @param azureServiceAccount
	 * @param userDetails
	 * @param azureSvcAccName
	 * @return
	 */
	private boolean addSudoPermissionToOwner(String token, AzureServiceAccount azureServiceAccount, UserDetails userDetails,
											 String azureSvcAccName) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE, String.format("Trying to add sudo permission for the service account [%s] to " +
						"the user {%s]", azureSvcAccName, azureServiceAccount.getOwnerNtid()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		AzureServiceAccountUser azureServiceAccountUser = new AzureServiceAccountUser(azureServiceAccount.getServicePrinicipalName(),
				azureServiceAccount.getOwnerNtid(), TVaultConstants.SUDO_POLICY);
		//Add sudo permisson to the Azure service account owner
		ResponseEntity<String> addUserToAzureSvcAccResponse = addUserToAzureServiceAccount(token, userDetails,
				azureServiceAccountUser, true);
		if (HttpStatus.OK.equals(addUserToAzureSvcAccResponse.getStatusCode())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							String.format(
									"Successfully added owner permission to [%s] for Azure service " + "account [%s].",
									azureServiceAccount.getOwnerNtid(), azureSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return true;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE,
						String.format("Failed to add owner permission to [%s] for Azure service " + "account [%s].",
								azureServiceAccount.getOwnerNtid(), azureSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;

	}
	
	/**
	 * Add user to Azure Service principal.
	 *
	 * @param token
	 * @param userDetails
	 * @param azureServiceAccountUser
	 * @param isPartOfOnboard
	 * @return
	 */
	public ResponseEntity<String> addUserToAzureServiceAccount(String token, UserDetails userDetails,
			AzureServiceAccountUser azureServiceAccountUser, boolean isPartOfOnboard) {

		azureServiceAccountUser.setAzureSvcAccName(azureServiceAccountUser.getAzureSvcAccName().toLowerCase());
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Trying to add user to Azure Service principal [%s]", azureServiceAccountUser.getAzureSvcAccName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		if (!isAzureSvcaccPermissionInputValid(azureServiceAccountUser.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Invalid value specified for access. Valid values are read, rotate, deny\"]}");
		}
		if (azureServiceAccountUser.getAccess().equalsIgnoreCase(AzureServiceAccountConstants.AZURE_ROTATE_MSG_STRING)) {
			azureServiceAccountUser.setAccess(TVaultConstants.WRITE_POLICY);
		}

		boolean isAuthorized = isAuthorizedToAddPermissionInAzureSvcAcc(userDetails, azureServiceAccountUser.getAzureSvcAccName(), isPartOfOnboard);

		if (isAuthorized) {
			// Only Sudo policy can be added (as part of onbord) before activation.
			if (!isAzureSvcaccActivated(token, userDetails, azureServiceAccountUser.getAzureSvcAccName())
					&& !TVaultConstants.SUDO_POLICY.equals(azureServiceAccountUser.getAccess())) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
								.put(LogMessage.MESSAGE, String.format("Failed to add user permission to Azure Service account. [%s] is not activated.", azureServiceAccountUser.getAzureSvcAccName()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to add user permission to Azure Service account. Service Account is not activated. Please activate this service account and try again.\"]}");
			}

			if (isOwnerPemissionGettingChanged(azureServiceAccountUser, userDetails.getUsername(), isPartOfOnboard)) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
								.put(LogMessage.MESSAGE, "Failed to add user permission to Azure Service account. Owner permission cannot be changed..")
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to add user permission to Azure Service account. Owner permission cannot be changed.\"]}");
			}
			return getUserPoliciesForAddUserToAzureSvcAcc(token, userDetails, azureServiceAccountUser, oidcEntityResponse,
					azureServiceAccountUser.getAzureSvcAccName());

		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("{\"errors\":[\"Access denied: No permission to add users to this Azure service account\"]}");
		}
	}
	
	/**
	 * Validates Azure Service Account permission inputs
	 *
	 * @param access
	 * @return
	 */
	public static boolean isAzureSvcaccPermissionInputValid(String access) {
		boolean isValidAccess = true;
		if (!org.apache.commons.lang3.ArrayUtils.contains(ACCESS_PERMISSIONS, access)) {
			isValidAccess = false;
		}
		return isValidAccess;
	}
	
	/**
	 * Check if user has the permission to add user to the Azure Service Account.
	 *
	 * @param userDetails
	 * @param serviceAccount
	 * @param access
	 * @param token
	 * @return
	 */
	public boolean isAuthorizedToAddPermissionInAzureSvcAcc(UserDetails userDetails, String serviceAccount,
			boolean isPartOfOnboard) {
		// Admin users can add sudo policy for owner while onboarding the azure service principal
		if (isPartOfOnboard) {
			return true;
		}
		// Owner of the service account can add/remove users, groups, aws roles and approles to service account
		String ownerPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.
				getKey(TVaultConstants.SUDO_POLICY)).append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(serviceAccount).toString();
		String [] policies = policyUtils.getCurrentPolicies(tokenUtils.getSelfServiceToken(), userDetails.getUsername(), userDetails);

		return ArrayUtils.contains(policies, ownerPolicy);
	}

	
	/**
	 * To check if the Azure service principal is activated.
	 *
	 * @param token
	 * @param userDetails
	 * @param SvcAccName
	 * @return
	 */
	private boolean isAzureSvcaccActivated(String token, UserDetails userDetails, String azureSvcAccName) {
		String azureAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;
		boolean activationStatus = false;
		Response metaResponse = getMetadata(token, userDetails, azureAccPath);
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
						.put(LogMessage.ACTION, "isAzureSvcaccActivated")
						.put(LogMessage.MESSAGE,
								String.format("Failed to get Activation status for the Azure Service account [%s]",
										azureSvcAccName))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "isAzureSvcaccActivated")
					.put(LogMessage.MESSAGE,
							String.format("Metadata not found for Azure Service account [%s]", azureSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return activationStatus;
	}
	
	/**
	 * Get metadata for Azure service account.
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
		String azureMetaDataPath = "metadata/" + path;
		return reqProcessor.process("/sdb", "{\"path\":\"" + azureMetaDataPath + "\"}", token);
	}
	
	/**
	 * Method to verify the user for add user to Azure service account.
	 * @param token
	 * @param userDetails
	 * @param azureServiceAccountUser
	 * @param oidcEntityResponse
	 * @param azureSvcaccName
	 * @return
	 */
	private ResponseEntity<String> getUserPoliciesForAddUserToAzureSvcAcc(String token, UserDetails userDetails,
			AzureServiceAccountUser azureServiceAccountUser, OIDCEntityResponse oidcEntityResponse,
			String azureSvcaccName) {
		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read", AzureServiceAccountConstants.USERNAME_PARAM_STRING + azureServiceAccountUser.getUsername() + "\"}",
					token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/ldap/users", AzureServiceAccountConstants.USERNAME_PARAM_STRING + azureServiceAccountUser.getUsername() + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC implementation changes
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, azureServiceAccountUser.getUsername(),
					userDetails);
			if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
							.put(LogMessage.MESSAGE, String.format("Failed to fetch OIDC user for [%s]", azureServiceAccountUser.getUsername()))
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

		return addPolicyToAzureSvcAcc(token, userDetails, azureServiceAccountUser, oidcEntityResponse, azureSvcaccName,
				userResponse);
	}
	
	/**
	 * Method to create policies for add user to Azure service account and call the update process.
	 * @param token
	 * @param userDetails
	 * @param azureServiceAccountUser
	 * @param oidcEntityResponse
	 * @param azureSvcaccName
	 * @param userResponse
	 * @return
	 */
	private ResponseEntity<String> addPolicyToAzureSvcAcc(String token, UserDetails userDetails,
		AzureServiceAccountUser azureServiceAccountUser, OIDCEntityResponse oidcEntityResponse,
		String azureSvcaccName, Response userResponse) {

		String policy = new StringBuilder().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(azureServiceAccountUser.getAccess())).append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Trying to [%s] policy to user [%s] for the Azure service principal [%s]", policy, azureServiceAccountUser.getUsername(), azureServiceAccountUser.getAzureSvcAccName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String readPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		String writePolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		String denyPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("User policies are, read - [%s], write - [%s], deny -[%s]", readPolicy, writePolicy, denyPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String responseJson = "";
		String groups = "";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if (userResponse != null && HttpStatus.OK.equals(userResponse.getHttpstatus())) {
			responseJson = userResponse.getResponse();
			try {
				ObjectMapper objMapper = new ObjectMapper();
				// OIDC Changes
				if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					currentpolicies.addAll(oidcEntityResponse.getPolicies());
				} else {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						groups = objMapper.readTree(responseJson).get("data").get(AzureServiceAccountConstants.AZURE_GROUP_MSG_STRING).asText();
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE, String.format("Exception while creating currentpolicies or groups for [%s]", azureServiceAccountUser.getUsername()))
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
		return configureUserPoliciesForAddUserToAzureSvcAcc(token, userDetails, azureServiceAccountUser, oidcEntityResponse,
				groups, policies, currentpolicies);
	}

	/**
	 * Method to update policies for add user to Azure service principal.
	 * @param token
	 * @param userDetails
	 * @param azureServiceAccountUser
	 * @param oidcEntityResponse
	 * @param groups
	 * @param policies
	 * @param currentpolicies
	 * @return
	 */
	private ResponseEntity<String> configureUserPoliciesForAddUserToAzureSvcAcc(String token, UserDetails userDetails,
			AzureServiceAccountUser azureServiceAccountUser, OIDCEntityResponse oidcEntityResponse, String groups,
			List<String> policies, List<String> currentpolicies) {
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Policies [%s] before calling configure user", policies))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response userConfigresponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userConfigresponse = ControllerUtil.configureUserpassUser(azureServiceAccountUser.getUsername(), policiesString, token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userConfigresponse = ControllerUtil.configureLDAPUser(azureServiceAccountUser.getUsername(), policiesString, groups, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC Implementation : Entity Update
			try {
				userConfigresponse = oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
				oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE, String.format("Exception while adding or updating the identity for entity [%s]", oidcEntityResponse.getEntityName()))
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
			}
		}

		if (userConfigresponse != null && (userConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| userConfigresponse.getHttpstatus().equals(HttpStatus.OK))) {
			return updateMetadataForAddUserToAzureSvcAcc(token, userDetails, azureServiceAccountUser, oidcEntityResponse,
					groups, currentpolicies, currentpoliciesString);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to add user to the Azure Service Account\"]}");
		}
	}
	
	/**
	 * Method to update metadata for add user to Azure service account.
	 * @param token
	 * @param userDetails
	 * @param azureServiceAccountUser
	 * @param oidcEntityResponse
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForAddUserToAzureSvcAcc(String token, UserDetails userDetails,
			AzureServiceAccountUser azureServiceAccountUser, OIDCEntityResponse oidcEntityResponse, String groups,
			List<String> currentpolicies, String currentpoliciesString) {
		// User has been associated with Azure Service Account. Now metadata has to be created
		String path = new StringBuffer(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(azureServiceAccountUser.getAzureSvcAccName())
				.toString();
		Map<String, String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name", azureServiceAccountUser.getUsername());
		params.put("path", path);
		params.put(AzureServiceAccountConstants.AZURE_ACCESS_MSG_STRING, azureServiceAccountUser.getAccess());
		Response metadataResponse = ControllerUtil.updateMetadata(params, token);
		if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
				|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
							.put(LogMessage.MESSAGE, String.format("User [%s] is successfully associated with Azure Service Account - [%s]", azureServiceAccountUser.getUsername(), azureServiceAccountUser.getAzureSvcAccName()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully added user to the Azure Service Account\"]}");
		} else {
			return revertUserPoliciesForAzureSvcAcc(token, userDetails, oidcEntityResponse, azureServiceAccountUser.getUsername(), groups,
					currentpolicies, currentpoliciesString);
		}
	}
	
	/**
	 * Method to revert user policies if add user to Azure service account failed.
	 * @param token
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param userName
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> revertUserPoliciesForAzureSvcAcc(String token, UserDetails userDetails,
			OIDCEntityResponse oidcEntityResponse, String userName, String groups, List<String> currentpolicies,
			String currentpoliciesString) {
		Response configUserResponse = new Response();
		// Revert the user association when metadata fails...
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
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
						.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
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
					.body("{\"messages\":[\"Failed to revert user association on Azure Service Account\"]}");
		}
	}
	
	/**
	 * Method to check if the owner permission is getting changed.
	 * @param azureServiceAccountUser
	 * @param currentUsername
	 * @param isPartOfOnboard
	 * @return
	 */
	private boolean isOwnerPemissionGettingChanged(AzureServiceAccountUser azureServiceAccountUser, String currentUsername, boolean isPartOfOnboard) {
		if (isPartOfOnboard) {
			// sudo as part of onboard is allowed.
			return false;
		}
		boolean isPermissionChanged = false;
		// if owner is grating read/ deny to himself, not allowed. Write is allowed as part of activation.
		if (azureServiceAccountUser.getUsername().equalsIgnoreCase(currentUsername) && !azureServiceAccountUser.getAccess().equals(TVaultConstants.WRITE_POLICY)) {
			isPermissionChanged = true;
		}
		return isPermissionChanged;
	}
	
	
	/*
	 * getAzureServicePrinicipalList
	 * @param userDetails
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getAzureServicePrinicipalList(UserDetails userDetails) {
		oidcUtil.renewUserToken(userDetails.getClientToken());
		String token = userDetails.getClientToken();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);

		policies = filterPoliciesBasedOnPrecedence(Arrays.asList(policies));

		List<Map<String, String>> azureListUsers = new ArrayList<>();
		Map<String, List<Map<String, String>>> azureList = new HashMap<>();
		if (policies != null) {
			for (String policy : policies) {
				Map<String, String> azurePolicy = new HashMap<>();
				String[] Policies = policy.split("_", -1);
				if (Policies.length >= 3) {
					String[] policyName = Arrays.copyOfRange(Policies, 2, Policies.length);
					String azureName = String.join("_", policyName);
					String azureType = Policies[1];

					if (policy.startsWith("r_")) {
						azurePolicy.put(azureName, "read");
					} else if (policy.startsWith("w_")) {
						azurePolicy.put(azureName, "write");
					} else if (policy.startsWith("d_")) {
						azurePolicy.put(azureName, "deny");
					}
					if (!azurePolicy.isEmpty()) {
						if (azureType.equals(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH_PREFIX)) {
							azureListUsers.add(azurePolicy);
						}
					}
				}
			}
			azureList.put(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH_PREFIX, azureListUsers);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(azureList));
	}
	
	/**
	 * Filter azure service accounts policies based on policy precedence.
	 * 
	 * @param policies
	 * @return
	 */
	private String[] filterPoliciesBasedOnPrecedence(List<String> policies) {
		List<String> filteredList = new ArrayList<>();
		for (int i = 0; i < policies.size(); i++) {
			String policyName = policies.get(i);
			String[] Policy = policyName.split("_", -1);
			if (Policy.length >= 3) {
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
						// permission for that azure service account.
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
	 * Read Folder details for a given Azure service prinicipal
	 *
	 * @param token
	 * @param path
	 * @return
	 * @throws IOException 
	 */
	public ResponseEntity<String> readFolders(String token, String path) throws IOException {
		Response response = new Response();
		ObjectMapper objMapper = new ObjectMapper();
		Response lisresp = reqProcessor.process("/azure/list", "{\"path\":\"" + path + "\"}", token);
		if(lisresp.getHttpstatus().equals(HttpStatus.OK)){
			List<String> foldersList = new ArrayList<>();
			AzureServiceAccountNode azureServiceAccountNode = new AzureServiceAccountNode();
			JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
			for (JsonNode node : folders) {
				foldersList.add(node.textValue());
			}
			azureServiceAccountNode.setFolders(foldersList);
			azureServiceAccountNode.setPath(path);
			String separator = "/";
			int sepPos = path.indexOf(separator);
			azureServiceAccountNode.setServicePrinicipalName(path.substring(sepPos + separator.length()));
			response.setSuccess(true);
			response.setHttpstatus(HttpStatus.OK);
			String res = objMapper.writeValueAsString(azureServiceAccountNode);
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
	 * Find Azure service account from metadata.
	 * 
	 * @param token
	 * @param azureSvcaccName
	 * @return
	 */
	public ResponseEntity<String> getAzureServiceAccountSecretKey(String token, String azureSvcaccName, String folderName) {
		String path = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcaccName + "/" + folderName;
		Response response = reqProcessor.process("/azuresvcacct", "{\"path\":\"" + path + "\"}", token);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			JsonParser jsonParser = new JsonParser();
			JsonObject data = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
			Long expiryDate = Long.valueOf(String.valueOf(data.get("expiryDateEpoch")));
			String formattedExpiryDt = dateConversion(expiryDate);
			data.addProperty("expiryDate", formattedExpiryDt);
			return ResponseEntity.status(HttpStatus.OK).body(data.toString());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"No Azure Service Prinicipal with " + azureSvcaccName + ".\"]}");

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
	 * Read Secret.
	 * @param token
	 * @param azureSvcName
	 * @param secretKey
	 * @return
	 * @throws IOException
	 */
	public ResponseEntity<String> readSecret(String token, String azureSvcName, String secretKey)
			throws IOException {

		azureSvcName = azureSvcName.toLowerCase();
		String azureSvcNamePath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcName;
		ResponseEntity<String> response = readFolders(token, azureSvcNamePath);
		ObjectMapper mapper = new ObjectMapper();
		String secret = "";
		if (HttpStatus.OK.equals(response.getStatusCode())) {
			AzureServiceAccountNode azureServiceAccountNode = mapper.readValue(response.getBody(),
					AzureServiceAccountNode.class);
			if (azureServiceAccountNode.getFolders() != null) {
				for (String folderName : azureServiceAccountNode.getFolders()) {
					ResponseEntity<String> responseEntity = getAzureServiceAccountSecretKey(token, azureSvcName,
							folderName);
					if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
						AzureServiceAccountSecret azureServiceAccountSecret = mapper.readValue(responseEntity.getBody(),
								AzureServiceAccountSecret.class);
						if (secretKey.equals(azureServiceAccountSecret.getSecretKeyId())) {
							secret = azureServiceAccountSecret.getSecretText();
							break;
						}
					} else {
						return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":"
								+ JSONUtil.getJSON("No secret found for the secretKey :" + secretKey + "") + "}");
					}
				}
				if (StringUtils.isEmpty(secret)) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":"
							+ JSONUtil.getJSON("No secret found for the secretKey :" + secretKey + "") + "}");
				}
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"accessKeySecret\":" + JSONUtil.getJSON(secret) + "}");
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
						"{\"error\":" + JSONUtil.getJSON("No secret found for the secretKey :" + secretKey + "") + "}");
			}
		} else if (HttpStatus.FORBIDDEN.equals(response.getStatusCode())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\":"
					+ JSONUtil.getJSON("Access denied: No permission to read secret for Azure service account") + "}");
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					"{\"error\":" + JSONUtil.getJSON("azure_svc_name not found") + "}");
		}
	}
	
	
	/**
	 * Method to offboard  service account.
	 * @param token
	 * @param azureOffboardRequest
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> offboardAzureServiceAccount(String token, AzureServiceAccountOffboardRequest azureOffboardRequest,
															UserDetails userDetails) {
		String managedBy = "";
		String azureSvcName = azureOffboardRequest.getAzureSvcAccName().toLowerCase();
		String selfSupportToken = tokenUtils.getSelfServiceToken();
		if (!isAuthorizedForAzureOnboardAndOffboard(token)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							"Access denied. Not authorized to perform offboarding of Azure service accounts.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
					"{\"errors\":[\"Access denied. Not authorized to perform offboarding of Azure service accounts.\"]}");
		}

		boolean policyDeleteStatus = deleteAzureServiceAccountPolicies(selfSupportToken, azureSvcName);
		if (!policyDeleteStatus) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE,
							String.format("Failed to delete some of the policies for azure service " + "account [%s]",
									azureSvcName))
					.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"errors\":[\"Failed to Offboard Azure service account. Policy deletion failed.\"]}");
		}

		// delete users,groups,aws-roles,app-roles from azure service account
		String path = AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH + azureSvcName;
		Response metaResponse = reqProcessor.process("/sdb", "{\"path\":\"" + path + "\"}", token);
		Map<String, Object> responseMap = null;
		try {
			responseMap = new ObjectMapper().readValue(metaResponse.getResponse(),
					new TypeReference<Map<String, Object>>() {
					});
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Error Fetching metadata for azure service account " +
							" [%s]", azureSvcName))
					.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to Offboard Azure service account. Error Fetching metadata for azure service account\"]}");
		}
		if (responseMap != null && responseMap.get("data") != null) {
			Map<String, Object> metadataMap = (Map<String, Object>) responseMap.get("data");
			Map<String, String> approles = (Map<String, String>) metadataMap.get("app-roles");
			Map<String, String> groups = (Map<String, String>) metadataMap.get("groups");
			Map<String, String> users = (Map<String, String>) metadataMap.get("users");
			Map<String,String> awsroles = (Map<String, String>)metadataMap.get("aws-roles");
			// always add owner to the users list whose policy should be updated
			managedBy = (String) metadataMap.get(AzureServiceAccountConstants.OWNER_NT_ID);
			if (!org.apache.commons.lang3.StringUtils.isEmpty(managedBy)) {
				if (null == users) {
					users = new HashMap<>();
				}
				users.put(managedBy, "sudo");
			}

			updateUserPolicyAssociationOnAzureSvcaccDelete(azureSvcName, users, selfSupportToken, userDetails);
			updateGroupPolicyAssociationOnAzureSvcaccDelete(azureSvcName, groups, selfSupportToken, userDetails);
			updateApprolePolicyAssociationOnAzureSvcaccDelete(azureSvcName, approles, selfSupportToken);
			deleteAwsRoleonOnAzureSvcaccDelete(awsroles, selfSupportToken);
		}

		OnboardedAzureServiceAccount azureSvcAccToOffboard = new OnboardedAzureServiceAccount(azureSvcName, managedBy);
		//delete azure service account secrets and mount details
		ResponseEntity<String> secretDeletionResponse = deleteAzureSvcAccountSecrets(token, azureSvcAccToOffboard);
		if (HttpStatus.OK.equals(secretDeletionResponse.getStatusCode())) {
			// Remove metadata...
			ResponseEntity<String> metadataResponse = deleteAzureSvcAccount(token, azureSvcAccToOffboard);
			if(HttpStatus.OK.equals(metadataResponse.getStatusCode())){
				return ResponseEntity.status(HttpStatus.OK).body(
						"{\"messages\":[\"Successfully offboarded Azure service account (if existed) from T-Vault\"]}");
			}else{
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(
						"{\"errors\":[\"Failed to offboard Azure service account from TVault\"]}");
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_OFFBOARD_CREATION_TITLE)
					.put(LogMessage.MESSAGE, String.format("Failed to offboard Azure service account [%s] from TVault",
							azureSvcName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(
					"{\"errors\":[\"Failed to offboard Azure service account from TVault\"]}");
		}
	}


	/**
	 * Update User policy on Azure Service account offboarding
	 * @param azureSvcAccName
	 * @param acessInfo
	 * @param token
	 * @param userDetails
	 */
	private void updateUserPolicyAssociationOnAzureSvcaccDelete(String azureSvcAccName, Map<String, String> acessInfo,
															  String token, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "updateUserPolicyAssociationOnAzureSvcaccDelete")
				.put(LogMessage.MESSAGE, String.format("Trying to delete user policies on Azure service account delete " +
						"of [%s]", azureSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (acessInfo != null) {
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccName).toString();
			String ownerPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccName).toString();

			Set<String> users = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for (String userName : users) {

				Response userResponse = new Response();
				if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
					userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}",
							token);
				} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}",
							token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					// OIDC implementation changes
					ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName,
							null);
					if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
						if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "updateUserPolicyAssociationOnAzureSvcaccDelete")
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
								.put(LogMessage.ACTION, "updateUserPolicyAssociationOnAzureSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("updateUserPolicyAssociationOnAzureSvcaccDelete " +
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
							.put(LogMessage.ACTION, "updateUserPolicyAssociationOnAzureSvcaccDelete")
							.put(LogMessage.MESSAGE, String.format("Current policies [%s]", policies))
							.put(LogMessage.APIURL,	ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
					if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateUserPolicyAssociationOnAzureSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("Current policies userpass [%s]", policies))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						ControllerUtil.configureUserpassUser(userName, policiesString, token);
					} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateUserPolicyAssociationOnAzureSvcaccDelete")
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
									.put(LogMessage.ACTION, "updateUserPolicyAssociationOnAzureSvcaccDelete")
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
	 * Update Group policy on Azure Service account offboarding
	 *
	 * @param azureSvcAccountName
	 * @param acessInfo
	 * @param token
	 * @param userDetails
	 */
	private void updateGroupPolicyAssociationOnAzureSvcaccDelete(String azureSvcAccountName, Map<String, String> acessInfo,
															   String token, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnAzureSvcaccDelete")
				.put(LogMessage.MESSAGE, String.format("trying to delete group policies on Azure service account delete " +
						"for [%s]", azureSvcAccountName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnAzureSvcaccDelete")
					.put(LogMessage.MESSAGE, "Inside userpass of updateGroupPolicyAssociationOnAzureSvcaccDelete...Just Returning...")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return;
		}
		if (acessInfo != null) {
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();
			String sudoPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();

			Set<String> groups = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for (String groupName : groups) {
				Response response = new Response();
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					response = reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"" + groupName + "\"}",
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
						} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
							currentpolicies.addAll(oidcGroup.getPolicies());
						}
					} catch (IOException e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnAzureSvcaccDelete")
								.put(LogMessage.MESSAGE, String.format("updateGroupPolicyAssociationOnAzureSvcaccDelete " +
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
							.put(LogMessage.ACTION, "updateGroupPolicyAssociationOnAzureSvcaccDelete")
							.put(LogMessage.MESSAGE, String.format("Current policies [%s]", policies))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
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
	 * Approle policy update as part of offboarding
	 *
	 * @param azureSvcAccountName
	 * @param acessInfo
	 * @param token
	 */
	private void updateApprolePolicyAssociationOnAzureSvcaccDelete(String azureSvcAccountName,
																 Map<String, String> acessInfo, String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "updateApprolePolicyAssociationOnAzureSvcaccDelete")
				.put(LogMessage.MESSAGE, String.format("trying to update approle policies on Azure service account " +
						"delete for [%s]", azureSvcAccountName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (acessInfo != null) {
			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();
			String sudoPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcAccountName).toString();
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
								.put(LogMessage.ACTION, "updateApprolePolicyAssociationOnAzureSvcaccDelete")
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
									.put(LogMessage.ACTION, "updateApprolePolicyAssociationOnAzureSvcaccDelete")
									.put(LogMessage.MESSAGE, "Current policies :" + policiesString + " is being configured")
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
					appRoleService.configureApprole(approleName, policiesString, token);
				}
			}
		}
	}
	
	/**
	 * Deletes the Azure SvcAccount secret
	 * @param token
	 * @param azureServiceAccount
	 * @return
	 */
	private ResponseEntity<String> deleteAzureSvcAccountSecrets(String token, OnboardedAzureServiceAccount azureServiceAccount) {
		String azureSvcAccName = azureServiceAccount.getServicePrinicipalName();
		String azureSvcAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "deleteAzureSvcAccountSecrets")
				.put(LogMessage.MESSAGE, String.format("Trying to delete secret folder for Azure service " +
						"account [%s].", azureSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		boolean secretDeleteStatus = deleteAzureSvcAccountSecretFolders(token, azureServiceAccount.getServicePrinicipalName());
		if (secretDeleteStatus) {
			Response onboardingResponse = reqProcessor.process("/delete", "{\"path\":\"" + azureSvcAccPath + "\"}", token);

			if (onboardingResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
					|| onboardingResponse.getHttpstatus().equals(HttpStatus.OK)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "deleteAzureSvcAccountSecrets")
						.put(LogMessage.MESSAGE, "Successfully deleted Azure service account Secrets.")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"Successfully deleted Azure service account Secrets.\"]}");
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteAzureSvcAccountSecrets")
					.put(LogMessage.MESSAGE, "Failed to delete Azure service account Secrets.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Failed to delete Azure service account Secrets.\"]}");
		}

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "deleteAzureSvcAccountSecrets")
				.put(LogMessage.MESSAGE, "Failed to delete one or more Azure service account Secret folders.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Failed to delete Azure service account Secrets.\"]}");
	}
	
	/**
	 * To delete Azure secret folders as part of Offboarding.
	 * @param token
	 * @param azureSvcAccName
	 * @return
	 */
	private boolean deleteAzureSvcAccountSecretFolders(String token, String azureSvcAccName) {

		JsonObject azureMetadataJson = getAzureMetadata(token, azureSvcAccName);

		if (null!= azureMetadataJson && azureMetadataJson.has(TVaultConstants.SECRET)) {
			if (!azureMetadataJson.get("secret").isJsonNull()) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "deleteAzureSvcAccountSecretFolders").
						put(LogMessage.MESSAGE, String.format("Trying to delete secret folders for the Azure Service " +
								"account [%s]", azureSvcAccName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));

				JsonArray svcSecretArray = null;
				try {
					svcSecretArray = azureMetadataJson.get(TVaultConstants.SECRET).getAsJsonArray();
				} catch (IllegalStateException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "deleteAzureSvcAccountSecretFolders").
							put(LogMessage.MESSAGE, String.format("Failed to get secret folders. Invalid metadata " +
									"for [%s].", azureSvcAccName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return false;
				}

				if (null != svcSecretArray) {
					int deleteCount = 0;
					for (int i = 0; i < svcSecretArray.size(); i++) {
						String folderPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName + "/secret_" + (i+1);
						Response deleteFolderResponse = reqProcessor.process("/delete",
								"{\"path\":\"" + folderPath + "\"}", token);
						if (deleteFolderResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
								|| deleteFolderResponse.getHttpstatus().equals(HttpStatus.OK)) {
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, "deleteAzureSvcAccountSecretFolders").
									put(LogMessage.MESSAGE, String.format("Deleted secret folder [%d] for the Azure Service " +
											"account [%s]", (i+1), azureSvcAccName)).
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
	 * To get Azure Service Account metadata as JsonObject.
	 * @param token
	 * @param azureSvcaccName
	 * @return
	 */
	private JsonObject getAzureMetadata(String token, String azureSvcaccName) {
		String path = AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH + azureSvcaccName;
		Response response = reqProcessor.process("/read", "{\"path\":\"" + path + "\"}", token);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			return populateMetaData(response);
		}
		return null;
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
     * Aws role deletion as part of Offboarding
     * @param acessInfo
     * @param token
     */
    private void deleteAwsRoleonOnAzureSvcaccDelete(Map<String,String> acessInfo, String token) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "deleteAwsRoleonOnAzureSvcaccDelete").
                put(LogMessage.MESSAGE, "Trying to delete AwsRole On Azure Service Account offboarding").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        if(acessInfo!=null){
            Set<String> roles = acessInfo.keySet();
            for(String role : roles){
                Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);
                if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "deleteAwsRoleonOnAzureSvcaccDelete").
                            put(LogMessage.MESSAGE, String.format ("%s, AWS Role is deleted as part of offboarding Azure Service account.", role)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                }else{
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "deleteAwsRoleonOnAzureSvcaccDelete").
                            put(LogMessage.MESSAGE, String.format ("%s, AWS Role deletion as part of offboarding Azure Service account failed.", role)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                }
            }
        }
    }

	/**
	 * Removes user from Azure service account
	 *
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromAzureServiceAccount(String token,
			AzureServiceAccountUser azureServiceAccountUser, UserDetails userDetails) {
		azureServiceAccountUser.setAzureSvcAccName(azureServiceAccountUser.getAzureSvcAccName().toLowerCase());
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		if (!isAzureSvcaccPermissionInputValid(azureServiceAccountUser.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Invalid value specified for access. Valid values are read, rotate, deny\"]}");
		}
		if (azureServiceAccountUser.getAccess()
				.equalsIgnoreCase(AzureServiceAccountConstants.AZURE_ROTATE_MSG_STRING)) {
			azureServiceAccountUser.setAccess(TVaultConstants.WRITE_POLICY);
		}

		String azureSvcaccName = azureServiceAccountUser.getAzureSvcAccName();

		boolean isAuthorized = isAuthorizedToAddPermissionInAzureSvcAcc(userDetails, azureSvcaccName, false);

		if (isAuthorized) {
			// Only Sudo policy can be added (as part of onbord) before
			// activation.
			if (!isAzureSvcaccActivated(token, userDetails, azureSvcaccName)
					&& !TVaultConstants.SUDO_POLICY.equals(azureServiceAccountUser.getAccess())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE,
								String.format(
										"Failed to remove user permission from Azure Service account. [%s] is not activated.",
										azureServiceAccountUser.getAzureSvcAccName()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to remove user permission from Azure Service account. Azure Service Account is not activated. Please activate this Azure service account and try again.\"]}");
			}
			// Deleting owner permission is not allowed
			if (azureServiceAccountUser.getUsername().equalsIgnoreCase(userDetails.getUsername())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE,
								"Failed to remove user permission to Azure Service account. Owner permission cannot be changed..")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to remove user permission to Azure Service account. Owner permission cannot be changed.\"]}");
			}
			return processAndRemoveUserPermissionFromAzureSvcAcc(token, azureServiceAccountUser, userDetails,
					oidcEntityResponse, azureSvcaccName);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Access denied: No permission to remove user from this Azure service account\"]}");
		}
	}
	/**
	 * To get list of azure service principal onboarded
	 * 
	 * @param token
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getOnboardedAzureServiceAccounts(String token, UserDetails userDetails) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "listOnboardedIAzureerviceAccounts")
				.put(LogMessage.MESSAGE, "Trying to get list of onboaded Azure service accounts")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response response = null;
		String[] latestPolicies = policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(),
				userDetails.getUsername(), userDetails);
		List<String> onboardedlist = new ArrayList<>();
		for (String policy : latestPolicies) {
			
			if (policy.startsWith("o_azuresvcacc")) {
				onboardedlist.add(policy.substring(14));
			}
		}
		response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse("{\"keys\":" + JSONUtil.getJSON(onboardedlist) + "}");
		
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "listOnboardedAzureServiceAccounts")
					.put(LogMessage.MESSAGE, "Successfully retrieved the list of Azure Service Accounts")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Find Azure service principal from metadata.
	 *
	 * @param token
	 * @param azureSvcName
	 * @return
	 */
	public ResponseEntity<String> getAzureServicePrincipalDetail(String token, String azureSvcName) {
		String path = AzureServiceAccountConstants.AZURE_SVCC_ACC_META_PATH + azureSvcName;
		Response response = reqProcessor.process("/azuresvcacct", "{\"path\":\"" + path + "\"}", token);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			JsonObject data = populateMetaData(response);
			return ResponseEntity.status(HttpStatus.OK).body(data.toString());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"No Azure Service Principal with " + azureSvcName + ".\"]}");
	}
	/*
	 * Method to verify the user for removing from Azure service account.
	 *
	 * @param token
	 * @param azureServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param azureSvcaccName
	 * @return
	 */
	private ResponseEntity<String> processAndRemoveUserPermissionFromAzureSvcAcc(String token,
			AzureServiceAccountUser azureServiceAccountUser, UserDetails userDetails,
			OIDCEntityResponse oidcEntityResponse, String azureSvcaccName) {

		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read",
					"{\"username\":\"" + azureServiceAccountUser.getUsername() + "\"}", token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/ldap/users",
					"{\"username\":\"" + azureServiceAccountUser.getUsername() + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC implementation changes
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token,
					azureServiceAccountUser.getUsername(), userDetails);
			if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
					log.error(
							JSONUtil.getJSON(ImmutableMap.<String, String> builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION,
											AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
									.put(LogMessage.MESSAGE, "Trying to fetch OIDC user policies, failed")
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
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

		log.debug(
				JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE,
								String.format("userResponse status is [%s]", userResponse.getHttpstatus()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		return createPoliciesAndRemoveUserFromAzureSvcAcc(token, azureServiceAccountUser, userDetails,
				oidcEntityResponse, azureSvcaccName, userResponse);
	}

	/**
	 * Method to create policies for removing user from Azure service account
	 * and call the metadata update.
	 *
	 * @param token
	 * @param azureServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param azureSvcaccName
	 * @param userResponse
	 * @return
	 */
	private ResponseEntity<String> createPoliciesAndRemoveUserFromAzureSvcAcc(String token,
			AzureServiceAccountUser azureServiceAccountUser, UserDetails userDetails,
			OIDCEntityResponse oidcEntityResponse, String azureSvcaccName, Response userResponse) {
		String readPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		String writePolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		String denyPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		String ownerPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy,
								writePolicy, denyPolicy, ownerPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String responseJson = "";
		String groups = "";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		if (HttpStatus.OK.equals(userResponse.getHttpstatus())) {
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
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups")
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		Response ldapConfigresponse = configureRemovedUserPermissions(token, azureServiceAccountUser, userDetails,
				oidcEntityResponse, groups, policies, policiesString);

		if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {

			return updateMetadataAfterRemovePermissionFromAzureSvcAcc(token, azureServiceAccountUser, userDetails,
					oidcEntityResponse, groups, currentpolicies, currentpoliciesString);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to remvoe the user from the Azure Service Account\"]}");
		}
	}

	/**
	 * Method to configure the user permission after removed from Azure service
	 * account.
	 *
	 * @param token
	 * @param azureServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param groups
	 * @param policies
	 * @param policiesString
	 * @return
	 */
	private Response configureRemovedUserPermissions(String token, AzureServiceAccountUser azureServiceAccountUser,
			UserDetails userDetails, OIDCEntityResponse oidcEntityResponse, String groups, List<String> policies,
			String policiesString) {
		Response ldapConfigresponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(azureServiceAccountUser.getUsername(),
					policiesString, token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(azureServiceAccountUser.getUsername(), policiesString,
					groups, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC Implementation : Entity Update
			try {
				ldapConfigresponse = oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
				oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE, "Exception while updating the identity")
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
		return ldapConfigresponse;
	}

	/**
	 * Method to update the metadata after removed user from Azure service
	 * account
	 *
	 * @param token
	 * @param azureServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataAfterRemovePermissionFromAzureSvcAcc(String token,
			AzureServiceAccountUser azureServiceAccountUser, UserDetails userDetails,
			OIDCEntityResponse oidcEntityResponse, String groups, List<String> currentpolicies,
			String currentpoliciesString) {
		String azureSvcaccName = azureServiceAccountUser.getAzureSvcAccName();
		// User has been removed from this Azure Service Account. Now metadata
		// has to be deleted
		String path = new StringBuffer(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(azureSvcaccName)
				.toString();
		Map<String, String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name", azureServiceAccountUser.getUsername());
		params.put("path", path);
		params.put("access", "delete");
		Response metadataResponse = ControllerUtil.updateMetadata(params, token);
		if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
				|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
					.put(LogMessage.MESSAGE, "User is successfully Removed from Azure Service Account")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully removed user from the Azure Service Account\"]}");
		} else {
			return revertUserPermission(token, azureServiceAccountUser, userDetails, oidcEntityResponse, groups,
					currentpolicies, currentpoliciesString);
		}
	}

	/**
	 * Method to revert user permission for remove user from Azure service
	 * account if update failed.
	 *
	 * @param token
	 * @param azureServiceAccountUser
	 * @param userDetails
	 * @param oidcEntityResponse
	 * @param groups
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> revertUserPermission(String token, AzureServiceAccountUser azureServiceAccountUser,
			UserDetails userDetails, OIDCEntityResponse oidcEntityResponse, String groups, List<String> currentpolicies,
			String currentpoliciesString) {
		Response configUserResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			configUserResponse = ControllerUtil.configureUserpassUser(azureServiceAccountUser.getUsername(),
					currentpoliciesString, token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			configUserResponse = ControllerUtil.configureLDAPUser(azureServiceAccountUser.getUsername(),
					currentpoliciesString, groups, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC changes
			try {
				configUserResponse = oidcUtil.updateOIDCEntity(currentpolicies, oidcEntityResponse.getEntityName());
				oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e2) {
				log.error(e2);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.REMOVE_USER_FROM_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE, "Exception while updating the identity")
						.put(LogMessage.STACKTRACE, Arrays.toString(e2.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
		if (configUserResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| configUserResponse.getHttpstatus().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"{\"errors\":[\"Failed to remove the user from the Azure Service Account. Metadata update failed\"]}");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to revert user association on Azure Service Account\"]}");
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
	 * Add AWS role to Azure Service Account
	 * @param userDetails
	 * @param token
	 * @param azureServiceAccountAWSRole
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToAzureSvcacc(UserDetails userDetails, String token, AzureServiceAccountAWSRole azureServiceAccountAWSRole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_AWS_ROLE_MSG).
				put(LogMessage.MESSAGE, "Trying to add AWS Role to Azure Service Account").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
        if (!userDetails.isAdmin()) {
            token = tokenUtils.getSelfServiceToken();
        }
		if(!isAzureSvcaccPermissionInputValid(azureServiceAccountAWSRole.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, rotate, deny\"]}");
		}
		if (azureServiceAccountAWSRole.getAccess().equalsIgnoreCase(AzureServiceAccountConstants.AZURE_ROTATE_MSG_STRING)) {
			azureServiceAccountAWSRole.setAccess(TVaultConstants.WRITE_POLICY);
		}
		String roleName = azureServiceAccountAWSRole.getRolename();
		String azureSvcName = azureServiceAccountAWSRole.getAzureSvcAccName().toLowerCase();
		String access = azureServiceAccountAWSRole.getAccess();

		roleName = (roleName !=null) ? roleName.toLowerCase() : roleName;
		access = (access != null) ? access.toLowerCase(): access;

		boolean isAuthorized = hasAddOrRemovePermission(userDetails, azureSvcName, token);
		if(isAuthorized){
			String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcName).toString();


			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_AWS_ROLE_MSG).
					put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

			String readPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcName).toString();
			String writePolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcName).toString();
			String denyPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcName).toString();
			String ownerPolicy = new StringBuffer()
					.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
					.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcName).toString();

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_AWS_ROLE_MSG).
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
                            put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_AWS_ROLE_MSG).
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
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"AWS role '"+roleName+"' does not exist. Please create the role and try again!\"]}");
			}
			Response awsRoleConfigresponse = null;
			if (TVaultConstants.IAM.equals(authType)) {
				awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName,policiesString,token);
			}
			else {
				awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName,policiesString,token);
			}
			if(awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
				String path = new StringBuffer(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(azureSvcName).toString();
				Map<String,String> params = new HashMap<>();
				params.put("type", "aws-roles");
				params.put("name",roleName);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE, "AWS Role configuration Success.").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with Azure Service Account\"]}");
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
							put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_AWS_ROLE_MSG).
							put(LogMessage.MESSAGE, "Reverting, AWS Role policy update success").
							put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
				} else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_AWS_ROLE_MSG).
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
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add AWS Role to this Azure service account\"]}");
		}
	}

	/**
	 * Check if user has the permission to add user/group/awsrole/approles to
	 * the Azure Service Account
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
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(serviceAccount).toString();
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);
		if (ArrayUtils.contains(policies, ownerPolicy)) {
			return true;
		}
		return false;
	}

	/**
	 * Add Group to Azure service principal
	 *
	 * @param token
	 * @param azureServiceAccountGroup
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> addGroupToAzureServiceAccount(String token,
			AzureServiceAccountGroup azureServiceAccountGroup, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, "Trying to add Group to Azure Service Principal")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		if (!isAzureSvcaccPermissionInputValid(azureServiceAccountGroup.getAccess())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Invalid value specified for access. Valid values are read, rotate, deny\"]}");
		}
		if (azureServiceAccountGroup.getAccess().equalsIgnoreCase(AzureServiceAccountConstants.AZURE_ROTATE_MSG_STRING)) {
			azureServiceAccountGroup.setAccess(TVaultConstants.WRITE_POLICY);
		}

		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		}

		boolean canAddGroup = isAuthorizedToAddPermissionInAzureSvcAcc(userDetails, azureServiceAccountGroup.getAzureSvcAccName(), false);
		if (canAddGroup) {
			// Only Sudo policy can be added (as part of onbord) before activation.
			if (!isAzureSvcaccActivated(token, userDetails, azureServiceAccountGroup.getAzureSvcAccName())
					&& !TVaultConstants.SUDO_POLICY.equals(azureServiceAccountGroup.getAccess())) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
								.put(LogMessage.MESSAGE, String.format(
										"Failed to add group permission to Azure service principal. [%s] is not activated.",
										azureServiceAccountGroup.getAzureSvcAccName()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Failed to add group permission to Azure service principal. Azure service principal is not activated. Please activate this account and try again.\"]}");
			}

			return processRequestAndCallMetadataUpdateToAzureSvcAcc(token, userDetails, oidcGroup, azureServiceAccountGroup);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to add groups to this Azure service principal\"]}");
		}
	}

	/**
	 * Method to process AzureServiceAccountGroup request and call the update metadata and policy creations.
	 * @param token
	 * @param userDetails
	 * @param oidcGroup
	 * @param azureServiceAccountGroup
	 * @return
	 */
	private ResponseEntity<String> processRequestAndCallMetadataUpdateToAzureSvcAcc(String token, UserDetails userDetails,
			OIDCGroup oidcGroup, AzureServiceAccountGroup azureServiceAccountGroup) {
		String policy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(azureServiceAccountGroup.getAccess()))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureServiceAccountGroup.getAzureSvcAccName()).toString();

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("policy is [%s]", policy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		String readPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureServiceAccountGroup.getAzureSvcAccName()).toString();
		String writePolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureServiceAccountGroup.getAzureSvcAccName()).toString();
		String denyPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureServiceAccountGroup.getAzureSvcAccName()).toString();
		String sudoPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureServiceAccountGroup.getAzureSvcAccName()).toString();

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Group policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy,
								writePolicy, denyPolicy, sudoPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response groupResp = new Response();

		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			groupResp = reqProcessor.process("/auth/ldap/groups",
					"{\"groupname\":\"" + azureServiceAccountGroup.getGroupname() + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// call read api with groupname
			oidcGroup = oidcUtil.getIdentityGroupDetails(azureServiceAccountGroup.getGroupname(), token);
			if (oidcGroup != null) {
				groupResp.setHttpstatus(HttpStatus.OK);
				groupResp.setResponse(oidcGroup.getPolicies().toString());
			} else {
				groupResp.setHttpstatus(HttpStatus.BAD_REQUEST);
			}
		}

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
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
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && !ObjectUtils.isEmpty(oidcGroup)) {
					currentpolicies.addAll(oidcGroup.getPolicies());
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
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
		return configureGroupAndUpdateMetadataForAzureSvcAcc(token, userDetails, oidcGroup, azureServiceAccountGroup,
				policies, currentpolicies);
	}

	/**
	 * Method to update policies and metadata for add group to Azure service principal.
	 * @param token
	 * @param userDetails
	 * @param oidcGroup
	 * @param azureServiceAccountGroup
	 * @param policies
	 * @param currentpolicies
	 * @return
	 */
	private ResponseEntity<String> configureGroupAndUpdateMetadataForAzureSvcAcc(String token, UserDetails userDetails,
			OIDCGroup oidcGroup, AzureServiceAccountGroup azureServiceAccountGroup, List<String> policies,
			List<String> currentpolicies) {
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("policies [%s] before calling configureLDAPGroup", policies))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		Response ldapConfigresponse = new Response();
		// OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPGroup(azureServiceAccountGroup.getGroupname(),
					policiesString, token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ldapConfigresponse = oidcUtil.updateGroupPolicies(token, azureServiceAccountGroup.getGroupname(), policies,
					currentpolicies, oidcGroup != null ? oidcGroup.getId() : null);
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("After configured the group [%s] and status [%s] ", azureServiceAccountGroup.getGroupname(), ldapConfigresponse.getHttpstatus()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
			String path = new StringBuffer(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(azureServiceAccountGroup.getAzureSvcAccName())
					.toString();
			Map<String, String> params = new HashMap<>();
			params.put("type", AzureServiceAccountConstants.AZURE_GROUP_MSG_STRING);
			params.put("name", azureServiceAccountGroup.getGroupname());
			params.put("path", path);
			params.put(AzureServiceAccountConstants.AZURE_ACCESS_MSG_STRING, azureServiceAccountGroup.getAccess());

			Response metadataResponse = ControllerUtil.updateMetadata(params, token);
			if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
					|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
						.put(LogMessage.MESSAGE, "Group configuration Success.")
						.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"Group is successfully associated with Azure Service Principal\"]}");
			} else {
				return revertGroupPermissionForAzureSvcAcc(token, userDetails, oidcGroup,
						azureServiceAccountGroup.getGroupname(), currentpolicies, currentpoliciesString,
						metadataResponse);
			}
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to add group to the Azure Service Principal\"]}");
		}
	}

	/**
	 * Method to revert group permission if add group to Azure service principal failed.
	 * @param token
	 * @param userDetails
	 * @param oidcGroup
	 * @param groupName
	 * @param currentpolicies
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> revertGroupPermissionForAzureSvcAcc(String token, UserDetails userDetails,
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
					.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
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
					.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_GROUP_TO_AZURESVCACC_MSG)
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
	 * Activate Azure Service Principal.
	 * @param token
	 * @param userDetails
	 * @param servicePrinicipalName
	 * @return
	 */
	public ResponseEntity<String> activateAzureServicePrinicipal(String token, UserDetails userDetails, String servicePrinicipalName) {

		servicePrinicipalName = servicePrinicipalName.toLowerCase();
		String servicePrinicipalId;
		String tenantId;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
				put(LogMessage.MESSAGE, String.format ("Trying to activate Azure Service Principal [%s]", servicePrinicipalName)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		boolean isAuthorized = true;
		if (userDetails != null) {
			isAuthorized = isAuthorizedToAddPermissionInAzureSvcAcc(userDetails, servicePrinicipalName, false);
		}
		if (isAuthorized) {
			if (isAzureSvcaccActivated(token, userDetails, servicePrinicipalName)) {
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION)
								.put(LogMessage.MESSAGE, String.format("Failed to activate Azure Service Principal. [%s] is already activated", servicePrinicipalName))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						"{\"errors\":[\"Azure Service Principal is already activated. You can now grant permissions from Permissions menu\"]}");
			}

			JsonObject azureMetadataJson = getAzureMetadata(token, servicePrinicipalName);

			if (null!= azureMetadataJson && azureMetadataJson.has("secret")) {
				if (!azureMetadataJson.get("secret").isJsonNull()) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
							put(LogMessage.MESSAGE, String.format ("Trying to rotate secret for the Azure Service Principal [%s]", servicePrinicipalName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));

					JsonArray svcSecretArray = null;
					try {
						svcSecretArray = azureMetadataJson.get("secret").getAsJsonArray();
						servicePrinicipalId = azureMetadataJson.get("servicePrinicipalId").getAsString();
						tenantId = azureMetadataJson.get("tenantId").getAsString();
					} catch (IllegalStateException e) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
								put(LogMessage.MESSAGE, String.format ("Failed to activate Azure Service Principal. Invalid metadata for [%s].", servicePrinicipalName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to activate Azure Service Principal. Invalid metadata.\"]}");
					}

					if (null != svcSecretArray) {
						int secretSaveCount = 0;
						for (int i=0;i<svcSecretArray.size();i++) {

							JsonObject azureSecret = (JsonObject) svcSecretArray.get(i);

							if (azureSecret.has(AzureServiceAccountConstants.SECRET_KEY_ID)) {
								String secretKeyId = azureSecret.get(AzureServiceAccountConstants.SECRET_KEY_ID).getAsString();
								log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
										put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
										put(LogMessage.MESSAGE, String.format ("Trying to rotate secret for the Azure Service Principal [%s] secret key id: [%s]", servicePrinicipalName, secretKeyId)).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
										build()));
								// Rotate IAM service account secret for each secret key id in metadata
								if (rotateAzureServicePrincipalSecret(token, servicePrinicipalName, secretKeyId, servicePrinicipalId, tenantId, i+1)) {
									secretSaveCount++;
								}
							}
						}
						if (secretSaveCount == svcSecretArray.size()) {
							// Update status to activated.
							Response metadataUpdateResponse = azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrinicipalName);
							if(metadataUpdateResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataUpdateResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataUpdateResponse.getHttpstatus()))){
								log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
										put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
										put(LogMessage.MESSAGE, String.format("Metadata updated Successfully for Azure Service Principal [%s].", servicePrinicipalName)).
										put(LogMessage.STATUS, metadataUpdateResponse.getHttpstatus().toString()).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
										build()));
								// Add rotate permission for owner
								String ownerNTId = getOwnerNTIdFromMetadata(token, servicePrinicipalName );
								if (StringUtils.isEmpty(ownerNTId)) {
									log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
											put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
											put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
											put(LogMessage.MESSAGE, String.format("Failed to add rotate permission for owner for Azure Service Principal [%s]. Owner NT id not found in metadata", servicePrinicipalName)).
											put(LogMessage.STATUS, HttpStatus.BAD_REQUEST.toString()).
											put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
											build()));
									return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"Failed to activate Azure Service Prinicipal. Azure secrets are rotated and saved in T-Vault. However failed to add permission to owner. Owner info not found in Metadata.\"]}");
								}

								AzureServiceAccountUser azureServiceAccountUser = new AzureServiceAccountUser(servicePrinicipalName,
										ownerNTId, AzureServiceAccountConstants.AZURE_ROTATE_MSG_STRING);

								ResponseEntity<String> addUserToIAMSvcAccResponse = addUserToAzureServiceAccount(token, userDetails, azureServiceAccountUser, false);
								if (HttpStatus.OK.equals(addUserToIAMSvcAccResponse.getStatusCode())) {
									log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
											put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
											put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
											put(LogMessage.MESSAGE, String.format ("Azure Service Principal [%s] activated successfully", servicePrinicipalName)).
											put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
											build()));
									return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Azure Service Principal activated successfully\"]}");

								}
								log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
										put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
										put(LogMessage.MESSAGE, String.format("Failed to add rotate permission to owner as part of Azure Service Principal activation for [%s].", servicePrinicipalName)).
										put(LogMessage.STATUS, addUserToIAMSvcAccResponse!=null?addUserToIAMSvcAccResponse.getStatusCode().toString():HttpStatus.BAD_REQUEST.toString()).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
										build()));
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate Azure Service Principal. Azure secrets are rotated and saved in T-Vault. However owner permission update failed.\"]}");

							}
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate Azure Service Principal. Azure secrets are rotated and saved in T-Vault. However metadata update failed.\"]}");
						}
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
								put(LogMessage.MESSAGE, String.format ("IAM Service account [%s] activated successfully", servicePrinicipalName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate Azure Service Principal. Failed to rotate secrets for one or more SecretKeyIds.\"]}");
					}
				}
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
						put(LogMessage.MESSAGE, String.format ("Failed to activate activate Azure Service Principal. Invalid metadata for [%s].", servicePrinicipalName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to activate Azure Service Principal. Invalid metadata.\"]}");
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, AzureServiceAccountConstants.ACTIVATE_ACTION).
						put(LogMessage.MESSAGE, String.format ("SecretKey information not found in metadata for Azure Service Principal [%s]", servicePrinicipalName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"SecretKey information not found in metadata for this Azure Service Principal\"]}");
			}

		}
		else{
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: No permission to activate this Azure Service Prinicipal\"]}");
		}
	}


	/**
	 * Rotate Azure Service Principal secret by secretKeyId.
	 * @param token
	 * @param azureServicePrinicipalRotateRequest
	 * @return
	 */
	public ResponseEntity<String> rotateSecret(String token, AzureServicePrinicipalRotateRequest azureServicePrinicipalRotateRequest) {
		boolean rotationStatus = false;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_ACTION).
				put(LogMessage.MESSAGE, String.format ("Trying to rotate secret for the Azure Service Principal [%s] " +
								"secret key id: [%s]", azureServicePrinicipalRotateRequest.getAzureSvcAccName(),
						azureServicePrinicipalRotateRequest.getSecretKeyId())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String secretKeyId = azureServicePrinicipalRotateRequest.getSecretKeyId();
		String servicePrinicipalName = azureServicePrinicipalRotateRequest.getAzureSvcAccName().toLowerCase();

		if (!hasResetPermissionForAzureServicePrincipal(token, servicePrinicipalName)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
					put(LogMessage.MESSAGE, String.format("Access denited. No permisison to rotate Azure Service Principal secret for [%s].", servicePrinicipalName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: No permission to rotate secret for this Azure Service Principal.\"]}");
		}

		// Get metadata to check the secretkeyid
		JsonObject azureMetadataJson = getAzureMetadata(token, servicePrinicipalName);

		if (null!= azureMetadataJson && azureMetadataJson.has("secret")) {
			if (!azureMetadataJson.get("secret").isJsonNull()) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
						put(LogMessage.MESSAGE, String.format("Trying to rotate secret for the Azure Service Principal [%s]", servicePrinicipalName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));

				JsonArray azureSecretArray = null;
				try {
					azureSecretArray = azureMetadataJson.get("secret").getAsJsonArray();
				} catch (IllegalStateException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
							put(LogMessage.MESSAGE, String.format("Failed to rotate Azure Service Principal. Invalid metadata for [%s].", servicePrinicipalName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to rotate secret for Azure Service Principal. Invalid metadata.\"]}");
				}

				if (null != azureSecretArray) {
					for (int i = 0; i < azureSecretArray.size(); i++) {

						JsonObject azureSecret = (JsonObject) azureSecretArray.get(i);
						if (azureSecret.has(AzureServiceAccountConstants.SECRET_KEY_ID) && secretKeyId
								.equals(azureSecret.get(AzureServiceAccountConstants.SECRET_KEY_ID).getAsString())) {
							log.debug(
									JSONUtil.getJSON(ImmutableMap.<String, String> builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
											.put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE)
											.put(LogMessage.MESSAGE,
													String.format(
															"Trying to rotate secret for the Azure Service Principal [%s] secret key id: [%s]",
															servicePrinicipalName, secretKeyId))
											.put(LogMessage.APIURL,
													ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
											.build()));
							// Rotate Azure Service Principal secret for each secret key id in metadata
							rotationStatus = rotateAzureServicePrincipalSecret(token, servicePrinicipalName,
									secretKeyId, azureServicePrinicipalRotateRequest.getServicePrinicipalId(),
									azureServicePrinicipalRotateRequest.getTenantId(), i+1);
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
											"Failed to rotate secret for SecretkeyId [%s] for Azure Service Principal "
													+ "[%s]",
											azureServicePrinicipalRotateRequest.getSecretKeyId(),
											azureServicePrinicipalRotateRequest.getAzureSvcAccName()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Secret Key information not found in metadata for this Azure Service Principal\"]}");
		}

		if (rotationStatus) {
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
					put(LogMessage.MESSAGE, String.format ("Azure Service Principal [%s] rotated successfully for " +
									"SecretKeyId [%s]", azureServicePrinicipalRotateRequest.getAzureSvcAccName(),
							azureServicePrinicipalRotateRequest.getSecretKeyId())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Azure Service Principal secret rotated successfully\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, IAMServiceAccountConstants.ROTATE_IAM_SVCACC_TITLE).
				put(LogMessage.MESSAGE, String.format ("Failed to rotate secret for SecretkeyId [%s] for Azure Service Principal " +
								"[%s]", azureServicePrinicipalRotateRequest.getSecretKeyId(),
						azureServicePrinicipalRotateRequest.getAzureSvcAccName())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to rotate secret for Azure Service Principal\"]}");
	}

	/**
	 * Method to check if the user/approle has reset permission.
	 * @param token
	 * @param servicePrinicipalName
	 * @return
	 */
	private boolean hasResetPermissionForAzureServicePrincipal(String token, String servicePrinicipalName) {
		String resetPermission = "w_"+ AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX + servicePrinicipalName;
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> currentPolicies = new ArrayList<>();
		List<String> identityPolicies = new ArrayList<>();
		Response response = reqProcessor.process("/auth/tvault/lookup","{}", token);
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();
			try {
				currentPolicies = azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(objectMapper, responseJson);
				identityPolicies = policyUtils.getIdentityPoliciesAsListFromTokenLookupJson(objectMapper, responseJson);
				if (currentPolicies.contains(resetPermission) || identityPolicies.contains(resetPermission)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "hasResetPermissionForAzureServicePrincipal")
							.put(LogMessage.MESSAGE, "User has reset permission on this Azure Service principal.")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
					return true;
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "hasResetPermissionForAzureServicePrincipal")
						.put(LogMessage.MESSAGE,
								"Failed to parse policies from token")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "hasResetPermissionForAzureServicePrincipal")
				.put(LogMessage.MESSAGE, "Access denied. User is not permitted to rotate secret for Azure Service principal")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;
	}

	/**
	 * To get owner NT id from metadata for Azure Service Principal.
	 * @param token
	 * @param servicePrinicipalName
	 * @return
	 */
	private String getOwnerNTIdFromMetadata(String token, String servicePrinicipalName) {
		JsonObject getAzureMetadata = getAzureMetadata(token, servicePrinicipalName);
		if (null != getAzureMetadata && getAzureMetadata.has(AzureServiceAccountConstants.OWNER_NT_ID)) {
			return getAzureMetadata.get(AzureServiceAccountConstants.OWNER_NT_ID).getAsString();
		}
		return null;
	}

	/**
	 * Rotate secret for a secretKeyID in an Azure Service Principal.
	 * @param token
	 * @param servicePrinicipalName
	 * @param secretKeyId
	 * @param servicePrinicipalId
	 * @param tenantId
	 * @param secretKeyIndex
	 * @return
	 */
	private boolean rotateAzureServicePrincipalSecret(String token, String servicePrinicipalName, String secretKeyId, String servicePrinicipalId, String tenantId, int secretKeyIndex) {
		AzureServicePrinicipalRotateRequest azureServicePrinicipalRotateRequest  = new AzureServicePrinicipalRotateRequest(servicePrinicipalName, secretKeyId, servicePrinicipalId, tenantId);

		// @TODO: This is a mock response. This needs to be change to call the actual api (below commented) once the Azure secret api is live.
		AzureServiceAccountSecret azureServiceAccountSecret = azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(azureServicePrinicipalRotateRequest);
		//AzureServiceAccountSecret azureServiceAccountSecret = azureServiceAccountUtils.rotateAzureServicePrincipalSecret(azureServicePrinicipalRotateRequest);


		if (null != azureServiceAccountSecret) {
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
					put(LogMessage.MESSAGE, String.format ("Azure Service Principal [%s] rotated successfully for " +
									"Secret key id [%s]", servicePrinicipalName,
							secretKeyId)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			// Save secret in iamavcacc mount
			String path = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + servicePrinicipalName + "/" + AzureServiceAccountConstants.AZURE_SP_SECRET_FOLDER_PREFIX + (secretKeyIndex);
			if (azureServiceAccountUtils.writeAzureSPSecret(token, path, servicePrinicipalName, azureServiceAccountSecret)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
						put(LogMessage.MESSAGE, "Secret saved to Azure Service Principal mount").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				Response metadataUdpateResponse = azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(token, servicePrinicipalName, secretKeyId, azureServiceAccountSecret);
				if (null != metadataUdpateResponse && HttpStatus.NO_CONTENT.equals(metadataUdpateResponse.getHttpstatus())) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
							put(LogMessage.MESSAGE, "Updated Azure Service Principal metadata with secretKeyId and expiry").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return true;
				}
			}
		}
		return false;
	}
}
