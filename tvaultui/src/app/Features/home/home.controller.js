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
(function(app){
    app.controller('HomeCtrl', function($scope, Modal, $state, Authentication, SessionStore, UtilityService, Idle, AppConstant){

        var init = function(){
            $scope.slackLink = AppConstant.SLACK_LINK;
            $scope.emailLink = AppConstant.EMAIL_LINK;    
            $scope.forgotPasswordLink = UtilityService.getAppConstant('FORGOT_PASSWORD_LINK');
            // change login depending on authtype
            $scope.authType = AppConstant.AUTH_TYPE;
            $scope.domainName = AppConstant.DOMAIN_NAME;
            $scope.userID = 'Username';
            Idle.unwatch();
            if ($scope.authType.toLowerCase() === 'ldap') {
                $scope.userID = 'Corp ID';
            } else if ($scope.authType.toLowerCase() === 'ldap1900') {
                $scope.userID = 'Email ID';
            }
            $scope.usernameInvalid = false;
            $scope.loginPopupObj = {
                'username': '',
                'password': '',
                'userID': $scope.userID,
                'domainName': $scope.domainName,
                'authType': $scope.authType,
                'usernameInvalid': $scope.usernameInvalid,
                'forgotPasswordLink': $scope.forgotPasswordLink
            }

        }       

        $scope.goToLogin = function(size) {
            $scope.loginPopupObj = {
                'username': '',
                'password': '',
                'userID': $scope.userID,
                'domainName': $scope.domainName,
                'authType': $scope.authType,
                'usernameInvalid': $scope.usernameInvalid,
                'forgotPasswordLink': $scope.forgotPasswordLink
            }
            return Modal.createModal(size, 'login.html', 'HomeCtrl', $scope);
        }
        var saveParametersInSessionStore = function(loginResponseData){
            if(loginResponseData != undefined){
                var currentVaultKey = loginResponseData.client_token;
                var isAdmin = loginResponseData.admin.toLowerCase() != 'no';
                var isManager = true;
                var accessSafes = loginResponseData.access;
                var policies = loginResponseData.policies;
                SessionStore.setItem("myVaultKey",currentVaultKey);
                SessionStore.setItem("isAdmin", isAdmin);
                SessionStore.setItem("isManager", isManager);
                SessionStore.setItem("accessSafes", JSON.stringify(accessSafes));
                SessionStore.setItem("policies",policies);
                SessionStore.setItem("feature",JSON.stringify(loginResponseData.feature));
                $state.go('safes', {'fromLogin':true});
            }
        }
        var error = function (size) {
            Modal.createModal(size, 'error.html', 'HomeCtrl', $scope);
        };

        $scope.close = function () {
            Modal.close();
        };

        $scope.$watch( 'username', function( newValue ) {
            $scope.usernameInvalid = false;
            if (newValue && $scope.authType === 'ldap1900') {
                var username  = newValue.toLowerCase();
            if (username.includes('@')) {
                $scope.usernameInvalid = true;
                }
            }
            
         });

        $scope.login = function() {
            if ($scope.usernameInvalid) {
                return;
            }
            Modal.close('');
            $scope.isLoadingData = true;
            var username  = $scope.loginPopupObj.username.toLowerCase();    
            username = Authentication.formatUsernameWithoutDomain(username);
            var reqObjtobeSent = {"username":username,"password":$scope.loginPopupObj.password};
            Authentication.authenticateUser(reqObjtobeSent).then(function(response){
                $scope.isLoadingData = false;
                if(UtilityService.ifAPIRequestSuccessful(response)){
                    if(response.data != undefined){
                        SessionStore.setItem("username",username);
                    }
                    saveParametersInSessionStore(response.data);
                } else if (response.data && response.data.errors){
                    var errors = response.data.errors;
                    return Modal.createModalWithController('error.html', {
                    shortMessage: errors[0] || 'There was an error. Please try again, if the problem persists contact an administrator',
                    longMessage: errors[1]
                    });
                } else {
                    return Modal.createModalWithController('error.html', {
                    shortMessage: 'Something went wrong, please try again later.'
                    })
                }
            })
        };

        init();
    })
})(angular.module('vault.features.HomeCtrl',[
    'vault.services.UtilityService',
    'vault.constants.AppConstant'
]));
