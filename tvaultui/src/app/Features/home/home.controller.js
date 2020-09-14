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
    app.controller('HomeCtrl', function($scope, Modal, $state, Authentication, SessionStore, UtilityService,AdminSafesManagement, Idle, AppConstant, $location, $http){

        var init = function(){
            $scope.slackLink = AppConstant.SLACK_LINK;
            $scope.emailLink = AppConstant.EMAIL_LINK;    
            $scope.signUpLink = AppConstant.SIGN_UP_LINK;
            $scope.repoLink = AppConstant.REPO_LINK;
            $scope.forgotPasswordLink = UtilityService.getAppConstant('FORGOT_PASSWORD_LINK');
            // change login depending on authtype
            $scope.authType = AppConstant.AUTH_TYPE;
            $scope.domainName = AppConstant.DOMAIN_NAME;

            $scope.instanceMessage = '';
            $http.get('/app/Messages/uimessages.properties').then(function (response) {
                if (response != undefined && response.data !='') {
                    $scope.instanceMessage = response.data.home_message;
                }
            }, function(error) {
                console.log(error);
                $scope.instanceMessage = '';
            });

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

            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('code') && urlParams.get('state')) {
                $scope.isLoadingData = true;
                getSSOCallback(urlParams.get('code'), urlParams.get('state'));
                return;
            }

            if(SessionStore.getItem("myVaultKey")){
                // If no call back and token exists in session.
                $scope.isLoadingData = true;
                getAllSelfServiceGroups();
                $state.go('safes', {'fromLogin':true});
            }
            
        }

        //SSO login popup
        $scope.loginSSO = function () {
            getSSOAuthUrl();
        }

        function getSSOAuthUrl() {
            $scope.isLoadingData = true;
            var reqObjtobeSent = {
                "role": AppConstant.OIDC_ROLE,
                "redirect_uri": AppConstant.OIDC_REDIRECT_URL
              };
            Authentication.getAuthUrl(reqObjtobeSent).then(function(response){
                if(UtilityService.ifAPIRequestSuccessful(response)){
                    window.open(response.data.data.auth_url,"_self");
                } else {
                    $scope.isLoadingData = false;
                    return Modal.createModalWithController('error.html', {
                        shortMessage: 'Something went wrong, please try again later.'
                    });
                }
            })
        }

        function getSSOCallback(code, state) {
            $scope.isLoadingData = true;
            Authentication.getSSOCallback(code, state).then(function(response){
                if(UtilityService.ifAPIRequestSuccessful(response)){
                    if(response.data != undefined) {
                        // @TODO: how to get username here
                        //SessionStore.setItem("username",username);
                        
                    }
                    saveParametersInSessionStore(response.data);          
                    return;
                } else {
                    // callback process failed. Redirect to landing page. If not active token exists then will automatically redirect from landing page to login.
                    $scope.isLoadingData = false;
                    window.location.replace("/");
                    return;
                }
            })
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
                getUserName();
                //$state.go('safes', {'fromLogin':true});
            }
        }


        var getUserName = function(){
            Authentication.getUserName().then(function(response){
                if(UtilityService.ifAPIRequestSuccessful(response)){
                    var username = response.data.data.username;
                    SessionStore.setItem("username", username);
                    window.location.replace("/");
                    return; 
                } else {
                    // callback process failed. Redirect to landing page. If not active token exists then will automatically redirect from landing page to login.
                    $scope.isLoadingData = false;
                    window.location.replace("/");
                    return;
                }
            }, function (error) {
                console.log(error);
                window.location.replace("/");
            })
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

        var getAllSelfServiceGroups = function () {
            AdminSafesManagement.getAllSelfServiceGroups().then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    var assignedApplications = [];
                    var data = response.data;
                    if(data.length > 0) {
                        SessionStore.setItem("isCertPermission", true);
                        for (var index = 0;index<data.length;index++) {
                            assignedApplications.push(data[index]);
                        }
                        SessionStore.setItem("selfServiceAppNames", JSON.stringify(assignedApplications));
                    }
                }
                else {
                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                    $scope.error('md');
                }
            },
            function (error) {
                // Error handling function
                console.log(error);
            })
        };

        init();
    })
})(angular.module('vault.features.HomeCtrl',[
    'vault.services.UtilityService',
    'vault.constants.AppConstant',
    'vault.services.AdminSafesManagement'
]));
