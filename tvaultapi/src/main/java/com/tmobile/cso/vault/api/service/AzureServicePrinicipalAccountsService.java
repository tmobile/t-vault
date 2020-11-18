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
import java.util.stream.Collectors;

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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.AzureServiceAccountConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.model.AccessPolicy;
import com.tmobile.cso.vault.api.model.AzureSecrets;
import com.tmobile.cso.vault.api.model.AzureSecretsMetadata;
import com.tmobile.cso.vault.api.model.AzureServiceAccount;
import com.tmobile.cso.vault.api.model.AzureServiceAccountMetadataDetails;
import com.tmobile.cso.vault.api.model.AzureServiceAccountNode;
import com.tmobile.cso.vault.api.model.AzureServiceAccountSecret;
import com.tmobile.cso.vault.api.model.AzureServiceAccountUser;
import com.tmobile.cso.vault.api.model.AzureSvccAccMetadata;
import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryObjectsList;
import com.tmobile.cso.vault.api.model.DirectoryUser;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
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
	
	@Value("${azurePortal.auth.masterPolicy}")
	private String azureMasterPolicyName;
	
	private static final String[] ACCESS_PERMISSIONS = { "read", AzureServiceAccountConstants.AZURE_ROTATE_MSG_STRING, "deny", "sudo" };
	
	private static Logger log = LogManager.getLogger(AzureServicePrinicipalAccountsService.class);

	
	
	/**
	 * Onboard an Azure service account
	 *
	 * @param token
	 * @param iamServiceAccount
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
				.put(LogMessage.MESSAGE, "Failed to onboard IAM service account. Policy creation failed.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return rollBackAzureOnboardOnFailure(azureServiceAccount, azureSvcAccName, "onPolicyFailure");

	}

	/**
	 * To check if the user/token has permission for onboarding or offboarding
	 * IAM service account.
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
					.put(LogMessage.ACTION, "get onboarded IAM Service Account list")
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
		ResponseEntity<String> iamSvcAccPolicyCreationResponse = createAzureServiceAccountPolicies(azureSvcAccName);
		if (HttpStatus.OK.equals(iamSvcAccPolicyCreationResponse.getStatusCode())) {
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
			String iamCredsPath=new StringBuffer().append(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(azureSvcAccName).append("/*").toString();
			accessMap.put(iamCredsPath, TVaultConstants.getSvcAccPolicies().get(policyPrefix));
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
	 * @param iamServiceAccount
	 * @param iamSvcAccName
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
	 * @param iamServiceAccount
	 * @param iamSvcAccName
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
					.put(LogMessage.MESSAGE, String.format("Successfully added owner permission to [%s] for Azure service " +
							"account [%s].", azureServiceAccount.getOwnerNtid(), azureSvcAccName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return true;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SVCACC_CREATION_TITLE)
				.put(LogMessage.MESSAGE, String.format("Failed to add owner permission to [%s] for Azure service " +
						"account [%s].", azureServiceAccount.getOwnerNtid(), azureSvcAccName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;

	}
	
	/**
	 * Add user to Azure Service account.
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
				.put(LogMessage.MESSAGE, String.format("Trying to add user to ServiceAccount [%s]", azureServiceAccountUser.getAzureSvcAccName()))
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
		// IAM admin users can add sudo policy for owner while onboarding the service account
		if (isPartOfOnboard) {
			return true;
		}
		// Owner of the service account can add/remove users, groups, aws roles and approles to service account
		String ownerPolicy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.
				getKey(TVaultConstants.SUDO_POLICY)).append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).
				append("_").append(serviceAccount).toString();
		String [] policies = policyUtils.getCurrentPolicies(tokenUtils.getSelfServiceToken(), userDetails.getUsername(), userDetails);
		if (ArrayUtils.contains(policies, ownerPolicy)) {
			return true;
		}
		return false;
	}

	
	/**
	 * To check if the IAM service account is activated.
	 *
	 * @param token
	 * @param userDetails
	 * @param iamSvcAccName
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

		String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(azureServiceAccountUser.getAccess())).append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, AzureServiceAccountConstants.ADD_USER_TO_AZURESVCACC_MSG)
				.put(LogMessage.MESSAGE, String.format("Trying to [%s] policy to user [%s] for the IAM service account [%s]", policy, azureServiceAccountUser.getUsername(), azureServiceAccountUser.getAzureSvcAccName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String readPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		String writePolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(AzureServiceAccountConstants.AZURE_SVCACC_POLICY_PREFIX).append(azureSvcaccName).toString();
		String denyPolicy = new StringBuffer()
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

		if (userConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| userConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
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
		// if owner is grating read/ deny to himself, not allowed. Write is allowed as part of activation.
		if (azureServiceAccountUser.getUsername().equalsIgnoreCase(currentUsername) && !azureServiceAccountUser.getAccess().equals(TVaultConstants.WRITE_POLICY)) {
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
	public ResponseEntity<String> getAzureServicePrinicipalList(UserDetails userDetails) {
		oidcUtil.renewUserToken(userDetails.getClientToken());
		String token = userDetails.getClientToken();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);

		policies = filterPoliciesBasedOnPrecedence(Arrays.asList(policies));

		List<Map<String, String>> azureListUsers = new ArrayList<>();
		Map<String, List<Map<String, String>>> safeList = new HashMap<>();
		if (policies != null) {
			for (String policy : policies) {
				Map<String, String> azurePolicy = new HashMap<>();
				String[] iamPolicies = policy.split("_", -1);
				if (iamPolicies.length >= 3) {
					String[] policyName = Arrays.copyOfRange(iamPolicies, 2, iamPolicies.length);
					String safeName = String.join("_", policyName);
					String azureType = iamPolicies[1];

					if (policy.startsWith("r_")) {
						azurePolicy.put(safeName, "read");
					} else if (policy.startsWith("w_")) {
						azurePolicy.put(safeName, "write");
					} else if (policy.startsWith("d_")) {
						azurePolicy.put(safeName, "deny");
					}
					if (!azurePolicy.isEmpty()) {
						if (azureType.equals(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH_PREFIX)) {
							azureListUsers.add(azurePolicy);
						}
					}
				}
			}
			safeList.put(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH_PREFIX, azureListUsers);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeList));
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
	 * @param accessKey
	 * @return
	 * @throws IOException
	 */
	public ResponseEntity<String> readSecret(String token, String azureSvcName, String accessKey)
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
						if (accessKey.equals(azureServiceAccountSecret.getSecretKeyId())) {
							secret = azureServiceAccountSecret.getSecretText();
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
					+ JSONUtil.getJSON("Access denied: No permission to read secret for Azure service account") + "}");
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					"{\"error\":" + JSONUtil.getJSON("azure_svc_name not found") + "}");
		}
	}


}
