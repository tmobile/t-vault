package com.tmobile.cso.vault.api.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AzureServiceAccountConstants {
	
	private AzureServiceAccountConstants(){	
	}
	
	public static final String AZURE_SVCC_ACC_META_PATH = "metadata/azuresvcacc/";
	public static final String AZURE_SVCC_ACC_PATH = "azuresvcacc/";
	public static final String AZURE_SVCC_ACC_PATH_PREFIX = "azuresvcacc";
	public static final String AZURE_SVCACC_POLICY_PREFIX = "azuresvcacc_";
	public static final String AZURE_ROTATE_MSG_STRING = "rotate";
	
	public static final String AZURE_SVCACC_CREATION_TITLE = "Onboard Azure Service Account";
	public static final String AZURE_ONBOARD_EMAIL_SUBJECT="Onboarding Azure Service account %s is successful";
	public static final String AZURE_EMAIL_TEMPLATE_NAME = "AzureEmailTemplate";
	public static final String AZURE_ACCESS_MSG_STRING = "access";
	public static final String AZURE_GROUP_MSG_STRING = "groups";
	
	public static final String ADD_USER_TO_AZURESVCACC_MSG = "Add User to Azure Service Account";
	public static final String AZURE_SVCACC_OFFBOARD_CREATION_TITLE = "Offboard Azure Service Account";
	public static final String REMOVE_USER_FROM_AZURESVCACC_MSG = "Remove User from Azure Service Account";
	public static final String ADD_AWS_ROLE_MSG = "Add AWS Role to Azure Service Account";
	public static final String REMOVE_AWS_ROLE_AZURE_MSG = "Remove AWS Role from Azure Service Principal";

	public static final String ADD_GROUP_TO_AZURESVCACC_MSG = "Add Group to Azure Service Principal";
	public static final String REMOVE_GROUP_FROM_AZURESVCACC_MSG = "Remove Group from Azure Service Principal";
	public static final String ADD_APPROLE_TO_AZURESVCACC_MSG = "Add Approle to Azure Service Principal";
	public static final String FETCH_AZURE_DETAILS="Fetch Azure Service Principal details";

	public static final String REMOVE_APPROLE_TO_AZURESVCACC_MSG = "Remove Approle from Azure Service Principal";

	
	public static final String USERNAME_PARAM_STRING = "{\"username\":\"";
	
	public static final Map<String, String> AZURE_EMAIL_TEMPLATE_IMAGE_IDS;
	static {
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS = Collections.synchronizedMap(new HashMap<String, String>());
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("azuremanagetab", "templates/images/azuremanagetab.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("azureviewlink", "templates/images/azureviewlink.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("update", "templates/images/update.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("azureactivate", "templates/images/azureactivate.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("iampermission", "templates/images/iampermission.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("permissiontab", "templates/images/permissiontab.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("adduser", "templates/images/adduser.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("azurerotate", "templates/images/azurerotate.png");
	}
	
	public static final String OWNER_NT_ID = "owner_ntid";

	public static final String ACTIVATE_ACTION = "activateAzureServicePrincipal";
	public static final String AZURE_SP_ROTATE_SECRET_ACTION = "rotateAzureServicePrincipalSecret";
	public static final String AZURE_SP_ROTATE_ACTION = "rotateSecret";
	public static final String SECRET_KEY_ID = "secretKeyId";
	public static final String EXPIRY_DURATION = "expiryDurationMs";
	public static final String AZURE_SP_SECRET_FOLDER_PREFIX = "secret_";
	public static final String AZURE_AUTH_TOKEN_PREFIX = "cloud-iam";

}
