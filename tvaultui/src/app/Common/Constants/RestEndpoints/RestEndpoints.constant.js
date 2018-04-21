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

this.readTextFile("../apiUrls.json");

(function(app){
    app.constant('RestEndpoints', {
        baseURL: JSON.parse(sessionStorage.getItem('ApiUrls')).baseURL,
        //baseURL : '/vault'                                                   (URL for local testing)
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
            url: '/auth/tvault/renew',
            method: 'POST'
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

            {/* To enable ldap insert this to "url" : '/auth/ldap/login' */
            name: 'login',
            url: '/auth/tvault/login',
            method: 'POST'
        }, { /* Get the list of full safes for Admin */
            name: 'safesList',
            url: '/sdb/list?',
            method: 'GET'
        }, {
            name: 'deleteSafe',
            url: '/sdb/delete?',
            method: 'DELETE'
        }, {
            name: 'getSafeInfo',
            url: '/sdb?',
            method: 'GET'
        }, {
            name: 'createSafe',
            url: '/sdb/create',
            method: 'POST'
        }, {
            name: 'editSafe',
            url: '/sdb/update',
            method: 'POST'
        }, {
            name: 'deleteUserPermission',
            url: '/sdb/deleteuser',
            method: 'POST'
        }, {
            name: 'deleteGroupPermission',
            url: '/sdb/deletegroup',
            method: 'POST'
        }, {
            name: 'deleteAWSPermission',
            url: '/sdb/deleterole',
            method: 'POST'
        }, {
            name: 'addUserPermission',
            url: '/sdb/adduser',
            method: 'POST'
        }, {
            name: 'addGroupPermission',
            url: '/sdb/addgroup',
            method: 'POST'
        }, {
            name: 'addAWSPermission',
            url: '/sdb/addrole',
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
            url: '/auth/aws/roles/create',
            method: 'POST'
        }, {
            name: 'updateAwsRole',
            url: '/auth/aws/roles/update',
            method: 'POST'
        }, {
            name: 'unseal',
            url: '/unseal',
            method: 'POST'
        }, {
            name: 'unsealProgress',
            url: '/unseal-progress?serverip=',
            method: 'GET'
        }]
    } );
})( angular.module( 'vault.constants.RestEndpoints', [

] ) );
