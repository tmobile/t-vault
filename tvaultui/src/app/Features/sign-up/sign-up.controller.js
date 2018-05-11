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
(function(app){
    app.controller('SignUpCtrl', function($scope, Modal, $state, Authentication, SessionStore, UtilityService, Idle){

        var init = function(){
            $scope.forgotPasswordLink = UtilityService.getAppConstant('FORGOT_PASSWORD_LINK');
            Idle.unwatch();
        }

        var saveParametersInSessionStore = function(loginResponseData){
            if(loginResponseData != undefined){
                var currentVaultKey = loginResponseData.client_token;
                var isAdmin = loginResponseData.admin.toLowerCase() != 'no';
                var accessSafes = loginResponseData.access;
                var policies = loginResponseData.policies;
                SessionStore.setItem("myVaultKey",currentVaultKey);
                SessionStore.setItem("isAdmin",isAdmin);
                SessionStore.setItem("accessSafes", JSON.stringify(accessSafes));
                SessionStore.setItem("policies",policies);
                $state.go('safes');
            }
        }
        var error = function (size) {
            Modal.createModal(size, 'error.html', 'SignUpCtrl', $scope);
        };

        $scope.close = function () {
            Modal.close();
        };

        $scope.login = function() {
          $scope.isLoadingData = true;
          var username  = $scope.username.toLowerCase();
          username = Authentication.formatUsernameWithoutDomain(username);
          var reqObjtobeSent = {"username":username,"password":$scope.password};
          Authentication.authenticateUser(reqObjtobeSent).then(function(response){
              $scope.isLoadingData = false;
              if(UtilityService.ifAPIRequestSuccessful(response)){
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
})(angular.module('vault.features.SignUpCtrl',[
    'vault.services.UtilityService'
]));
