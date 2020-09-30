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

( function() {
    'use strict';
    angular.module('vault', [
        'ngAnimate',
        'ngCookies',
        'ngTouch',
        'ngSanitize',
        'ngMessages',
        'ngResource',
        'ui.router',
        'ui.bootstrap',
        'toastr',
        'counter',
        'ngMaterial',
        'ngTable',
        'ui.select',
        'vault.features',
        'vault.services',
        'vault.constants',
        'vault.directives',
        'vault.factories',
        'vault.layout',
        'vault.core',
        'ngIdle'
    ]);

    angular.module('vault.core', [
        'ngAnimate',
        'ngCookies',
        'ngTouch',
        'ngSanitize',
        'ngMessages',
        'ngResource',
        'ui.router',
        'ui.bootstrap',
        'toastr',
        'counter',
        'ngMaterial',
        'ngTable',
        'ui.select',
        'ngIdle',
        'vault.directives'
    ]);

    // Module names categorized for better understanding and to
    // track the modules being used in the code


    angular.module( 'vault.features', [
        'vault.features.SignUpCtrl',
        'vault.features.AdminCtrl',
        'vault.features.ChangeSafeCtrl',
        'vault.features.UnsealCtrl',
        'vault.features.safes',
        'vault.features.ChangeServiceAccountCtrl',
        'vault.features.ServiceAccountsCtrl',
        'vault.features.HomeCtrl',
        'vault.features.ChangeCertificateCtrl',
        'vault.features.CertificatesCtrl',
        'vault.features.ChangeIamServiceAccountCtrl',
        'vault.features.IamServiceAccountsCtrl'
    ]);
    angular.module( 'vault.services', [
        'vault.services.CopyToClipboard',
        'vault.services.DataCache',
        'vault.services.DeviceDetector',
        'vault.services.Modal',
        'vault.services.RefreshHandler',
        'vault.services.ServiceEndpoint',
        'vault.services.SessionStore',
        'vault.services.fetchData',
        'vault.services.SafesManagement',
        'vault.services.UtilityService',
        'vault.services.Authentication',
        'vault.services.AdminSafesManagement',
        'vault.services.ModifyUrl',
        'vault.services.ArrayFilter',
        'vault.services.httpInterceptor',
        'vault.services.Notifications',
        'vault.services.Unseal',
        'vault.services.VaultUtility'
    ]);

    angular.module( 'vault.constants', [
        'vault.constants.RestEndpoints',
        'vault.constants.AppConstant',
        'vault.constants.ErrorMessage',
        'vault.constants.ListOfApi',
        'vault.constants.MockData'
    ]);
    angular.module( 'vault.filters', [
        'vault.filters.ToArray',
        'vault.filters.CustomFilter'
    ]);
    angular.module( 'vault.directives', [
        'vault.features.ElementProperties',
        'vault.features.Scroll',
        'vault.directives.dateTimeFilter',
        'vault.directives.dropDown',
        'vault.directives.footer',
        'vault.directives.header',
        'vault.directives.listtable',
        'vault.directives.loadingState',
        'vault.directives.resize',
        'vault.directives.sidebar',
        'vault.directives.tiles',
        'vault.directives.radioButtons',
        'vault.directives.restrictSpecialChar',
        'vault.directives.dropDown',
        'vault.directives.navBar',
        'vault.directives.folderContentsTable',
        'vault.directives.folderContentsRow',
        'vault.directives.searchbar',
        'vault.directives.restrictSecretSpecialChar',
        'vault.directives.restrictArnSpecialChar',
        'vault.directives.restrictToNumbers',
        'vault.directives.restrictConfigChar',
        'vault.directives.restrictSpecialCharButComma',
        'vault.directives.restrictSpecialCharForTime',
        'vault.directives.headerHome',
        'vault.directives.restrictConfigCharApprole',
        'vault.directives.restrictSpecialCharWithSpace',
    ]);

    angular.module( 'vault.factories', [
    ]);

} )();
