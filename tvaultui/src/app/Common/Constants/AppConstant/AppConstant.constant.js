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
(function (app) {
    /*inject:constant*/
    app.constant('AppConstant', {
        'FORGOT_PASSWORD_LINK': '',
        'AD_USERS_DATA_URL': 'https://10.65.45.193/vault/v2/ldap/users?UserPrincipalName=',
        'AD_GROUP_DATA_URL': 'https://10.65.45.193/vault/v2/ldap/groups?groupName=',
        'AUTH_TYPE': 'gsm1900'  /*'userpass' or 'ldap' or 'gsm1900' */
    });
    /*endinject*/
})(angular.module('vault.constants.AppConstant', []));
