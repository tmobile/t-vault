/*
* =========================================================================
* Copyright 2018 T-Mobile, US
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
            url: '/v2/sdb/list?',
            method: 'GET'
        }, {
            name: 'deleteSafe',
            url: '/v2/sdb/delete?',
            method: 'DELETE'
        }, {
            name: 'getSafeInfo',
            url: '/v2/sdb?',
            method: 'GET'
        }, {
            name: 'createSafe',
            url: '/v2/sdb',
            method: 'POST'
        }, {
            name: 'editSafe',
            url: '/v2/sdb',
            method: 'PUT'
        }, {
            name: 'deleteUserPermission',
            url: '/v2/sdb/user',
            method: 'DELETE'
        }, {
            name: 'deleteGroupPermission',
            url: '/v2/sdb/group',
            method: 'DELETE'
        }, {
            name: 'deleteAWSPermission',
            url: '/v2/sdb/role',
            method: 'DELETE'
        }, { /* To remove the aws permission in edit*/
            name: 'detachAWSPermission',
            url: '/v2/sdb/role',
            method: 'PUT'
        },{
            name: 'addUserPermission',
            url: '/v2/sdb/user',
            method: 'POST'
        }, {
            name: 'addGroupPermission',
            url: '/v2/sdb/group',
            method: 'POST'
        }, {
            name: 'addAWSPermission',
            url: '/v2/sdb/role',
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
            url: '/v2/auth/aws/role',
            method: 'POST'
        }, {
            name: 'updateAwsRole',
            url: '/auth/aws/roles/update',
            method: 'POST'
        }, {
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
