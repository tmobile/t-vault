package com.tmobile.cso.vault.api.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AzureServiceAccountConstants {
	
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
	
	public static final String USERNAME_PARAM_STRING = "{\"username\":\"";
	
	public static final Map<String, String> AZURE_EMAIL_TEMPLATE_IMAGE_IDS;
	static {
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS = Collections.synchronizedMap(new HashMap<String, String>());
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("iammanagetab", "templates/images/iammanagetab.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("iamviewlink", "templates/images/iamviewlink.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("update", "templates/images/update.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("azureactivate", "templates/images/azureactivate.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("iampermission", "templates/images/iampermission.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("permissiontab", "templates/images/permissiontab.png");
		AZURE_EMAIL_TEMPLATE_IMAGE_IDS.put("adduser", "templates/images/adduser.png");
	}

}
