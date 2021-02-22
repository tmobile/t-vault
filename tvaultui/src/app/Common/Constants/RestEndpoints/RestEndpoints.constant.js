/*
* =========================================================================
* Copyright 2019 T-Mobile, US
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* See the readme.txt file for additional language around disclaimer of warranties.
* =========================================================================
*/

'use strict';

function readTextFile(file)
{
    var rawFile = new XMLHttpRequest();
    rawFile.open("GET", file, false);
    rawFile.onreadystatechange = function ()
    {
        if(rawFile.readyState === 4)
        {
            if(rawFile.status === 200 || rawFile.status == 0)
            {
                // console.log(rawFile.responseText);
                var allText = rawFile.responseText;      // The full response as a string
                var responseObjectJson = JSON.parse(allText);
                sessionStorage.setItem('ApiUrls', JSON.stringify(responseObjectJson));
            }
        }
    }
    rawFile.send(null);
}

readTextFile("../apiUrls.json");

(function(app){
    app.constant('RestEndpoints', {
        baseURL: JSON.parse(sessionStorage.getItem('ApiUrls')).baseURL,
        // written below separately as request requires timeout promise 
//        usersGetData : '/v2/ldap/users?UserPrincipalName=',
        usersGetData : '/v2/tmo/users?UserPrincipalName=',
        groupGetDataFromAAD: '/v2/azure/groups?name=',
        groupMailGetDataFromAAD: '/v2/azure/email?mail=',
        usersGetDataUsingCorpID: '/v2/ldap/corpusers?CorpId=',
        usersGetDataUsingNTID: '/v2/ldap/ntusers?displayName=',
        getUsersDataUsingNTIDs: '/v2/ldap/getusersdetail/',
        //baseURL : '/vault'
        endpoints: [{
            name: 'postAction',
            url: '/postAction',
            method: 'POST'
        }, {
            name: 'getAction',
            url: '/getAction',
            method: 'GET'
        },
        {
            name: 'renewToken',
            url: '/v2/auth/tvault/renew',
            method: 'GET'
        },
        {
            name: 'lookupToken',
            url: '/auth/tvault/lookup',
            method: 'POST'
        },
        {
            name: 'revokeToken',
            url: '/auth/tvault/revoke',
            method: 'POST'
        },
            {
                name: 'writeSecretV2',
                url: '/v2/write',
                method: 'POST'
            },
            {
                name: 'createFolderV2',
                url: '/v2/sdb/createfolder',
                method: 'POST'
            },
            {
                name: 'readAllContents',
                url: '/readAll',
                method: 'GET'
            },
            {
                name: 'readAllContentsRecursive',
                url: '/readfull',
                method: 'GET'
            },

            {/* To enable ldap insert this to "url" : '/auth/ldap/login' */
            name: 'login',
            url: '/v2/auth/tvault/login',
            method: 'POST'
        }, { /* Get the list of full safes for Admin */
            name: 'safesList',
            url: '/v2/ss/sdb/list?',
            method: 'GET'
        }, {
            name: 'deleteSafe',
            url: '/v2/ss/sdb/delete?',
            method: 'DELETE'
        }, {
            name: 'getSafeInfo',
            url: '/v2/ss/sdb?',
            method: 'GET'
        }, {
            name: 'createSafe',
            url: '/v2/ss/sdb',
            method: 'POST'
        }, {
            name: 'editSafe',
            url: '/v2/ss/sdb',
            method: 'PUT'
        }, {
            name: 'deleteUserPermission',
            url: '/v2/ss/sdb/user',
            method: 'DELETE'
        }, {
            name: 'deleteGroupPermission',
            url: '/v2/ss/sdb/group',
            method: 'DELETE'
        }, {
            name: 'deleteAWSPermission',
            url: '/v2/ss/sdb/role',
            method: 'DELETE'
        }, { /* To remove the aws permission in edit*/
            name: 'detachAWSPermission',
            url: '/v2/ss/sdb/role',
            method: 'PUT'
        },{
            name: 'addUserPermission',
            url: '/v2/ss/sdb/user',
            method: 'POST'
        }, {
            name: 'addGroupPermission',
            url: '/v2/ss/sdb/group',
            method: 'POST'
        }, {
            name: 'addAWSPermission',
            url: '/v2/ss/sdb/role',
            method: 'POST'
        }, {
            name: 'getAwsConfigurationDetails',
            url: '/auth/aws/roles/',
            method: 'GET'
        },{
            name: 'saveNewFolder',
            url: '/sdb/createfolder?path=',
            method: 'POST'
        }, {
            name: 'postSecrets',
            url: '/write',
            method: 'POST'
        }, {
            name: 'getSecrets',
            url: '/read?path=',
            method: 'GET'
        }, {
            name: 'createAwsRole',
            url: '/v2/ss/auth/aws/role?',
            method: 'POST'
        }, {
            name: 'updateAwsRole',
            url: '/auth/aws/roles/update',
            method: 'POST'
        }, {
            name: 'createAwsIAMRole',
            url: '/v2/ss/auth/aws/iam/role?',
            method: 'POST'
        }, {
            name: 'updateAwsIAMRole',
            url: '/v2/auth/aws/iam/role',
            method: 'PUT'
        },{
            name: 'createAppRole',
            url: '/v2/ss/auth/approle/role',
            method: 'POST'
        },{
            name: 'addApprolePermission',
            url: '/v2/ss/sdb/approle',
            method: 'POST'
        },{ /* To remove the approle permission in edit*/
            name: 'detachAppRolePermission',
            url: '/v2/ss/sdb/approle',
            method: 'DELETE'
        },{ /* To remove the approle permission for certificate*/
            name: 'deleteAppRolePermission',
            url: '/v2/sslcert/approle',
            method: 'DELETE'
        },{
            name: 'getApproles',
            url: '/v2/ss/approle',
            method: 'GET'
        },{
            name: 'getAccessorIDs',
            url: 'v2/ss/approle/{role_name}/accessors',
            method: 'GET'
        },{
            name: 'deleteAccessorID',
            url: '/v2/ss/approle/{role_name}/secret_id',
            method: 'DELETE'
        },{
            name: 'readSecretID',
            url: '/v2/ss/auth/approle/role/{role_name}/secret_id',
            method: 'GET'
        },{
            name: 'readRoleID',
            url: '/v2/ss/approle/{role_name}/role_id',
            method: 'GET'
        },{
            name: 'deleteAppRole',
            url: '/v2/ss/auth/approle/role/',
            method: 'DELETE'
        },{
            name: 'updateAppRole',
            url: '/v2/ss/approle',
            method: 'PUT'
        },{
            name: 'getApproleDetails',
            url: '/v2/ss/approle/role/',
            method: 'GET'
        },{
            name: 'getOnboardedServiceAccounts',
            url: '/v2/serviceaccounts',
            method: 'GET'
        },{
            name: 'getServiceAccounts',
            url: '/v2/ad/serviceaccounts?',
            method: 'GET'
        },{
            name: 'getSvcaccInfo',
            url: '/v2/ad/serviceaccounts?',
            method: 'GET'
        },{
            name: 'getSvcaccOnboardInfo',
            url: '/v2/serviceaccounts/',
            method: 'GET'
        },{
            name: 'editSvcacc',
            url: '/v2/serviceaccounts/onboard',
            method: 'PUT'
        },{
            name: 'onboardSvcacc',
            url: '/v2/serviceaccounts/onboard',
            method: 'POST'
        },{
            name: 'getSvcaccMetadata',
            url: '/v2/serviceaccounts/meta?',
            method: 'GET'
        },{
            name: 'addUserPermissionForSvcacc',
            url: '/v2/serviceaccounts/user',
            method: 'POST'
        },{
            name: 'deleteUserPermissionFromSvcacc',
            url: '/v2/serviceaccounts/user',
            method: 'DELETE'
        },{
            name: 'addGroupPermissionForSvcacc',
            url: '/v2/serviceaccounts/group',
            method: 'POST'
        },{
            name: 'deleteGroupPermissionFromSvcacc',
            url: '/v2/serviceaccounts/group',
            method: 'DELETE'
        },{
            name: 'addAWSPermissionForSvcacc',
            url: '/v2/serviceaccounts/role',
            method: 'POST'
        },{
            name: 'detachAWSPermissionFromSvcacc',
            url: '/v2/serviceaccounts/role',
            method: 'DELETE'
        },{
            name: 'addAppRolePermissionForSvcacc',
            url: '/v2/serviceaccounts/approle',
            method: 'POST'
        },{
            name: 'detachAppRolePermissionFromSvcacc',
            url: '/v2/serviceaccounts/approle',
            method: 'DELETE'
        },{
            name: 'createAwsRoleSvcacc',
            url: '/v2/serviceaccounts/aws/role',
            method: 'POST'
        },{
            name: 'createAwsIAMRoleSvcacc',
            url: '/v2/serviceaccounts/aws/iam/role',
            method: 'POST'
        },{
            name: 'getSecretForSvcacc',
            url: '/v2/serviceaccounts/password?',
            method: 'GET'
        },{
            name: 'resetPasswordForSvcacc',
            url: '/v2/serviceaccounts/password?',
            method: 'PUT'
        },{
            name: 'offboardSvcacc',
            url: '/v2/serviceaccounts/offboard',
            method: 'POST'
        },{
            name: 'offboardDecommissionedServiceAccount',
            url: '/v2/serviceaccounts/offboarddecommissioned',
            method: 'POST'
        },{
            name: 'getApprolesFromCwm',
            url: '/v2/serviceaccounts/cwm/approles',
            method: 'GET'
        },{
            name: 'transferSvcaccOwner',
            url: '/v2/serviceaccounts/transfer?',
            method: 'POST'
        }, {
            name: 'getCertificates',
            url: '/v2/sslcert?',
            method: 'GET'
        }, {
            name: 'getTargetSystems',
            url: '/v2/sslcert/{certType}/targetsystems',
            method: 'GET'
        }, {
            name: 'getTargetSystemsServices',
            url: '/v2/sslcert/targetsystems/{targetsystem_id}/targetsystemservices',
            method: 'GET'
        }, {
            name: 'unseal',
            url: '/v2/unseal',
            method: 'POST'
        }, {
            name: 'unsealProgress',
            url: '/v2/unseal-progress?serverip=',
            method: 'GET'
        }, {
            name: 'createSslCertificates',
            url: '/v2/sslcert',
            method: 'POST'
        }, {
            name: 'usersGetData',
            url: '/v2/tmo/users?UserPrincipalName=',
            method: 'GET'
        },{
            name: 'usersGetDataUsingCorpID',
            url: '/v2/ldap/corpusers?CorpId=',
            method: 'GET'
        }, {
            name: 'usersGetDataUsingNTID',
            url: '/v2/ldap/ntusers?displayName=',
            method: 'GET'
        }, {
            name: 'groupMailGetDataFromAAD',
            url: '/v2/azure/email?mail=',
            method: 'GET'
        }, {
            name: 'getRevocationReasons',
            url: '/v2/certificates/{certificateId}/revocationreasons',
            method: 'GET'
        }, {
            name: 'issueRevocationRequest',
            url: '/v2/certificates/{certType}/{certName}/revocationrequest',
            method: 'POST'
        }, {
            name: 'addUserToCertificate',
            url: '/v2/sslcert/user',
            method: 'POST'
        },{
            name: 'getCertificateDetails',
            url: '/v2/sslcert/certificate/{certificate_type}',
            method: 'GET'
        }, {
            name: 'addApproleToCertificate',
            url: '/v2/sslcert/approle',
            method: 'POST'
        }, {
            name: 'renewCertificate',
            url: '/v2/certificates/{certType}/{certName}/renew',
            method: 'POST'
        }, {
            name: 'addGroupToCertificate',
            url: '/v2/sslcert/group',
            method: 'POST'
        }, {
            name: 'deleteUserPermissionFromCertificate',
            url: '/v2/sslcert/user',
            method: 'DELETE'
        }, {
            name: 'deleteGroupPermissionFromCertificate',
            url: '/v2/sslcert/group',
            method: 'DELETE'
        },{
            name: 'listCertificatesByCertificateType',
            url: '/v2/sslcert/certificates{certificate_type}',
            method: 'GET'
        }, {
            name: 'transferCertificate',
            url: '/v2/sslcert/{certType}/{certName}/{certOwnerEmailId}/transferowner',
            method: 'PUT'
        } ,{
            name: 'validateCertificateDetails',
            url: '/v2/sslcert/validate/{certificate_name}/{certificate_type}',
            method: 'GET'
        }, {
            name: 'deleteCertificate',
            url: '/v2/certificates/{certName}/{certType}',
            method: 'DELETE'
        }, {
             name: 'getAuthUrl',
             url: '/v2/auth/oidc/auth_url',
             method: 'POST'
         }, {
             name: 'getCallback',
             url: '/v2/auth/oidc/callback',
             method: 'GET'
         },{	
             name: 'checkRevokestatus',	
             url: '/v2/sslcert/checkstatus/{certificate_name}/{certificate_type}',	
             method: 'GET'	
         }, {
            name: 'getAllSelfServiceGroups',
            url: '/v2/sslcert/grouplist',
            method: 'GET'
        },{
            name: 'getOnboardedIamServiceAccounts',
            url: '/v2/iamserviceaccounts',
            method: 'GET'
        },{
            name: 'getIamServiceAccount',
            url: '/v2/iamserviceaccounts/{iam_svc_name}',
            method: 'GET'
        },{
            name: 'getSecretForIamSvcacc',
            url: '/v2/iamserviceaccounts/secrets/{iam_svc_name}',
            method: 'GET'
        },{
            name: 'addUserPermissionForIAMSvcacc',
            url: '/v2/iamserviceaccounts/user',
            method: 'POST'
        },{
            name: 'deleteUserPermissionFromIAMSvcacc',
            url: '/v2/iamserviceaccounts/user',
            method: 'DELETE'
        },{
            name: 'addGroupPermissionForIAMSvcacc',
            url: '/v2/iamserviceaccounts/group',
            method: 'POST'
        },{
            name: 'deleteGroupPermissionFromIAMSvcacc',
            url: '/v2/iamserviceaccounts/group',
            method: 'DELETE'
        },{
            name: 'addAppRolePermissionForIAMSvcacc',
            url: '/v2/iamserviceaccounts/approle',
            method: 'POST'
        },{
            name: 'detachAppRolePermissionFromIAMSvcacc',
            url: '/v2/iamserviceaccounts/approle',
            method: 'DELETE'
        },{
            name: 'activateIAMSvcacc',
            url: '/v2/iamserviceaccount/activate?',
            method: 'POST'
        },{
            name: 'rotateIAMSvcaccSecret',
            url: '/v2/iamserviceaccount/rotate',
            method: 'POST'
        },{
            name: 'activateAzureServicePrincipal',
            url: '/v2/azureserviceaccounts/activateAzureServicePrincipal?',
            method: 'POST'
        },{
            name: 'rotateAzureSecret',
            url: '/v2/azureserviceaccounts/rotate',
            method: 'POST'
        },{
            name: 'addUserPermissionForIAMSvcacc',
            url: '/v2/iamserviceaccounts/user',
            method: 'POST'
        },{
            name: 'createAwsRoleCertificate',
            url: '/v2/sslcert/aws/role',
            method: 'POST'
        },{
            name: 'createAwsIAMRoleCertificate',
            url: '/v2/sslcert/aws/iam/role',
            method: 'POST'
        },{
            name: 'addAWSPermissionForCertificate',
            url: '/v2/sslcert/aws',
            method: 'POST'
        },{
            name: 'detachAWSPermissionFromCertificate',
            url: '/v2/sslcert/aws',
            method: 'DELETE'
        },{
            name: 'createAwsRoleIAMSvcacc',
            url: '/v2/iamserviceaccounts/aws/role',
            method: 'POST'
        },{
            name: 'createAwsIAMRoleIAMSvcacc',
            url: '/v2/iamserviceaccounts/aws/iam/role',
            method: 'POST'
        },{
            name: 'addAWSPermissionForIAMSvcacc',
            url: '/v2/iamserviceaccounts/role',
            method: 'POST'
        },{
            name: 'detachAWSPermissionFromIAMSvcacc',
            url: '/v2/iamserviceaccounts/role',
            method: 'DELETE'
        },{
            name: 'getApplicationDetails',
            url: '/v2/serviceaccounts/cwm/appdetails/appname?',
            method: 'GET'
        },{
            name: 'unclaimCert',
            url: '/v2/sslcert/unlink/{certificate_name}/{certificate_type}',
            method: 'POST'
        },{
            name: 'getAllOnboardPendingCertificates',
            url: '/v2/sslcert/pendingcertificates',
            method: 'GET'
        },{
            name: 'onboardSslCertificates',
            url: '/v2/sslcert/onboardSSLcertificate',
            method: 'POST'
        },{
            name: 'getSecretForAzureSvcacc',
            url: '/v2/azureserviceaccounts/secrets/{azure_svc_name}/{folderName}',
            method: 'GET'
        }, {
            name: 'updateCertificate',
            url: '/v2/sslcert',
            method: 'PUT' 
        }, {
            name: 'getOnboardedAzureServiceAccounts',
            url: '/v2/azureserviceaccounts',
            method: 'GET' 
        },{
            name: 'getAzureSvcaccOnboardInfo',
            url: '/v2/azureserviceaccounts/',
            method: 'GET'
        },{
            name: 'addUserPermissionForAzureSvcacc',
            url: '/v2/azureserviceaccounts/user',
            method: 'POST'
        },{
            name: 'deleteUserPermissionFromAzureSvcacc',
            url: '/v2/azureserviceaccounts/user',
            method: 'DELETE'
        },{
            name: 'addAWSPermissionForAzureSvcacc',
            url: '/v2/azureserviceaccounts/role',
            method: 'POST'
        },{
            name: 'createAwsRoleAzureSvcacc',
            url: '/v2/azureserviceaccounts/aws/role',
            method: 'POST'
        },{
            name: 'createAwsIAMRoleAzureSvcacc',
            url: '/v2/azureserviceaccounts/aws/iam/role',
            method: 'POST'
        },{
            name: 'addGroupPermissionForAzureSvcacc',
            url: '/v2/azureserviceaccounts/group',
            method: 'POST'
        },{
            name: 'detachAwsRoleFromAzureSvcacc',
            url: '/v2/azureserviceaccounts/role',
            method: 'DELETE'
        },{
            name: 'deleteGroupPermissionFromAzureSvcacc',
            url: '/v2/azureserviceaccounts/group',
            method: 'DELETE'
        },{
            name: 'addAppRolePermissionForAzureSvcacc',
            url: '/v2/azureserviceaccounts/approle',
            method: 'POST'
        },{
            name: 'detachAppRolePermissionFromAzureSvcacc',
            url: '/v2/azureserviceaccounts/approle',
            method: 'DELETE'
        },{
            name: 'transferSafe',
            url: '/v2/ss/transfersafe',
            method: 'POST'
        },{
            name: 'searchByUPNInGsmAndCorp',
            url: '/v2/tmo/users?UserPrincipalName=',
            method: 'GET'
        }
    ]
    });
})( angular.module( 'vault.constants.RestEndpoints', []));
