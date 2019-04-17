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
        usersGetData : '/v2/ldap/users?UserPrincipalName=',
        groupGetData: '/v2/ldap/groups?groupName=',
        usersGetDataUsingCorpID: '/v2/ldap/corpusers?CorpId=', 
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
            url: '/v2/serviceaccounts/edit/',
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
            url: '/v2/serviceaccounts/secret/',
            method: 'GET'
        },{
            name: 'resetPasswordForSvcacc',
            url: '/v2/serviceaccounts/password/reset',
            method: 'POST'
        },{
            name: 'offboardSvcacc',
            url: '/v2/serviceaccounts/offboard',
            method: 'POST'
        },{
            name: 'unseal',
            url: '/v2/unseal',
            method: 'POST'
        }, {
            name: 'unsealProgress',
            url: '/v2/unseal-progress?serverip=',
            method: 'GET'
        }]
    });
})( angular.module( 'vault.constants.RestEndpoints', []));
