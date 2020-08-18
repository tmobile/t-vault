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

package com.tmobile.cso.vault.api.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.bidimap.DualHashBidiMap;

public class TVaultConstants {
    public static final String READ_POLICY="read";
    public static final String WRITE_POLICY="write";
    public static final String DENY_POLICY="deny";
    public static final String RESET_POLICY="reset";
    public static final String OWNER_POLICY="owner";
    public static final String SUDO_POLICY="sudo";
    public static final String CREATE_POLICY="create";
    public static final String ADD_USER = "addUser";
    public static final String FALSE = "false";
    public static final String REMOVE_USER = "removeUser";
    public static final String SAFE = "safe";
    public static final String FOLDER = "folder";
    public static final String APPS = "apps";
    public static final String SHARED = "shared";
    public static final String USERS = "users";
    public static final String GROUPS = "groups";
    public static final String AWS_ROLES = "aws-roles";
    public static final String EMPTY = "";
    public static final String USERPASS = "userpass";
    public static final String LDAP = "ldap";
    public static final String OIDC = "oidc";
    public static final String DELETE = "delete";
    public static final String IAM = "iam";
    public static final String CREATE ="create";
    public static final String UPDATE ="update";
    public static final String UNKNOWN = "unknown";
    public static final String EMPTY_JSON = "{}";
    public static final String APPROLE = "approle";
    public static final String APPROLE_METADATA_MOUNT_PATH = "metadata/approle";
    public static final String APPROLE_USERS_METADATA_MOUNT_PATH = "metadata/approle_users";
    public static final String AWSROLE_METADATA_MOUNT_PATH = "metadata/awsrole";
    public static final String SELF_SERVICE_APPROLE_NAME = "selfservicesupportrole";
    public static final String SECRET = "secret";
    public static final boolean HIDEMASTERAPPROLE = true;
    public static final String APPROLE_DELETE_OPERATION="DELETE";
    public static final String APPROLE_READ_OPERATION="READ";
    public static final String APPROLE_UPDATE_OPERATION="UPDATE";
    public static final String SVC_ACC_PATH_PREFIX="svcacct";
    protected static final Map<String, String> SVC_ACC_POLICIES;
    public static final DualHashBidiMap SVC_ACC_POLICIES_PREFIXES ;
    static {
    	SVC_ACC_POLICIES = Collections.synchronizedMap(new HashMap<String, String>());
    	SVC_ACC_POLICIES.put("r_", TVaultConstants.READ_POLICY);
    	SVC_ACC_POLICIES.put("w_", TVaultConstants.WRITE_POLICY);
    	SVC_ACC_POLICIES.put("o_", TVaultConstants.SUDO_POLICY);
    	SVC_ACC_POLICIES.put("d_", TVaultConstants.DENY_POLICY);
    	SVC_ACC_POLICIES_PREFIXES = new DualHashBidiMap(SVC_ACC_POLICIES);
    }
    public static final String SVC_ACC_CREDS_PATH="ad/creds/";
	/**
	 * @return the svcAccPolicies
	 */
	public static Map<String, String> getSvcAccPolicies() {
		return SVC_ACC_POLICIES;
	}

    public static final String ADD_GROUP = "addGroup";
    public static final String REMOVE_GROUP = "removeGroup";
    public static final String SVC_ACC_ROLES_METADATA_MOUNT_PATH = "metadata/ad/roles/";
    public static final String SVC_ACC_ROLES_PATH = "ad/roles/";
    public static final Long PASSWORD_AUTOROTATE_TTL_MAX_VALUE = 1590897977L;
    public static final String EC2 = "ec2";
    public static final String SELFSERVICE = "selfservice";
    public static final String ADAUTOROTATION = "adpwdrotation";
    public static final String SELFSERVICE_URI_PREFIX = "/vault/v2/ss/";
    public static final String SVC_ACC_AD_URI_PREFIX = "/vault/v2/ad/";
    public static final String SVC_ACC_URI_PREFIX = "/vault/v2/serviceaccounts";
    public static final String SVC_ACC_EXCEPTION = "r_adds_svcacct_exception";
    public static final String SVC_ACC_STANDARD = "r_adds_svcacct_standard";
    public static final Integer SVC_ACC_EXCEPTION_MAXLIFE = 90;
    public static final Integer SVC_ACC_STANDARD_MAXLIFE = 365;
    public static final Long SVC_ACC_NEVER_EXPIRE_VALUE = 9223372036854775807L;
    public static final Long FILETIME_EPOCH_DIFF = 11644473600000L;
    public static final long DAY_IN_MILLISECONDS = 86400000L;
    public static final String TIME_ZONE_PDT = "America/Los_Angeles";
    public static final String ACCOUNT_EXPIRES = "accountExpires";
    public static final String NEVER_EXPIRE = "Never";
    public static final String EXPIRED = "Expired";
    public static final Long MAX_TTL = 1590897977L;
    public static final String EMAIL_TEMPLATE_PREFIX = "templates/";
    public static final String EMAIL_TEMPLATE_SUFFIX = ".html";
    public static final String EMAIL_TEMPLATE_NAME = "emailtemplate";
    public static final String IMAGE_FORMAT_PNG = "png";
    public static final String IMAGE_TYPE_PNG = "image/png";

    public static final Map<String, String> EMAIL_TEMPLATE_IMAGE_IDS;
    static {
        EMAIL_TEMPLATE_IMAGE_IDS = Collections.synchronizedMap(new HashMap<String, String>());
        EMAIL_TEMPLATE_IMAGE_IDS.put("managetab", "templates/images/managetab.png");
        EMAIL_TEMPLATE_IMAGE_IDS.put("viewlink", "templates/images/viewlink.png");
        EMAIL_TEMPLATE_IMAGE_IDS.put("update", "templates/images/update.png");
        EMAIL_TEMPLATE_IMAGE_IDS.put("activate", "templates/images/activate.png");
        EMAIL_TEMPLATE_IMAGE_IDS.put("permission", "templates/images/permission.png");
        EMAIL_TEMPLATE_IMAGE_IDS.put("permissiontab", "templates/images/permissiontab.png");
        EMAIL_TEMPLATE_IMAGE_IDS.put("adduser", "templates/images/adduser.png");
    }
    public static final String HTTP_CONTENT_TYPE_JSON = "application/json";
    public static final String HTTP_CONTENT_TYPE_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String ALIAS_MOUNT_ACCESSOR = "accessor";
    public static final String ENTITY_NAME = "name";
    public static final String POLICIES = "policies";
    public static final String EXTERNAL_TYPE = "external";
    public static final String IDENTITY_POLICIES = "identity_policies";
    public static final String ENTITY_DISPLAY_NAME = "display_name";
}
