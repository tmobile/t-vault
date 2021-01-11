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
(function (app) {
    /*inject:constant*/
    app.constant('AppConstant', {
        'FORGOT_PASSWORD_LINK': '',
        'AD_USERS_AUTOCOMPLETE': true,
        'AD_GROUP_AUTOCOMPLETE': true,
        'AUTH_TYPE': 'ldap',  /*'userpass' or 'ldap' or 'ldap1900' */
        'DOMAIN_NAME': '@T-Mobile.com',
        'SLACK_LINK' : '',
        'EMAIL_LINK' : '',
        'SIGN_UP_LINK' : '',
        'DOCS_LINK' : '',
        'REPO_LINK' : 'https://github.com/tmobile/t-vault/blob/master/README.md',
        'OIDC_ROLE' : 'default',
        'OIDC_REDIRECT_URL' : 'http://localhost:3000',
        'SSL_EXT_CERTIFICATE' : true,
        'VALID_RENEW_DAYS': 50,
        'PAGE_SIZE' : 2
    });
    /*endinject*/
})(angular.module('vault.constants.AppConstant', []));
