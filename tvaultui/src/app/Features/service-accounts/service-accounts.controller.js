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
    app.controller('ServiceAccountsCtrl', function($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService, RestEndpoints, CopyToClipboard){

        $scope.filterValue = '';            // Initial search filter value kept empty
        $scope.isLoadingData = false;       // Variable to set the loader on
        $scope.adminNavTags = safesService.getSafesNavTags();
        $scope.viewPassword = false;
        $scope.ifSecret = false;
        $scope.anyRegex = /.|\s/g;
        $scope.showPassword = false;
        $scope.write = false;
        var init = function () {
            
            $scope.myVaultKey = SessionStore.getItem("myVaultKey");
            if(!$scope.myVaultKey){ /* Check if user is in the same session */
                $state.go('signup');
            }
            else{
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.requestDataFrMyAccounts();
            }
        };

        // Fetching Data

        $scope.requestDataFrMyAccounts = function () {      
            $scope.svcOnboardedData = {"keys": []};
            AdminSafesManagement.getMyServiceAccounts().then(function (response) {                
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.svcOnboardedData = response.data;
                    for(var i=0; i < $scope.svcOnboardedData.keys.length ; i++){
                        var svc = $scope.svcOnboardedData.keys[i];
                        var expiry = new Date(svc.expiry); 
                        var dayDif = (expiry - new Date())/1000/60/60/24;
                        if (dayDif >= 0) {
                            svc.expiry = Math.floor(dayDif) + " days";
                        } else {
                            svc.expiry = "Expired";
                        }                        
                        $scope.svcOnboardedData.keys[i] = svc;
                    }
                }
                else {
                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                    error('md');
                }
            },
            function (error) {
                // Error handling function
                console.log(error);
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            });
        };

        $scope.viewSecret = function (svcname) {
            $scope.isLoadingData = true;
            $scope.write = false;
            $scope.svcSecretData = {"secret":"", "svcname":svcname, "permission":""};
            var queryParameters = svcname;
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSecretForSvc',queryParameters);
            AdminSafesManagement.getSecretForSvc(null, updatedUrlOfEndPoint).then(function (response) {                
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isLoadingData = false;
                    $scope.viewPassword = true;
                    $scope.ifSecret = true;
                    $scope.svcSecretData.secret = response.data.secret;  
                    if ($scope.svcSecretData.secret.permission == "write") {
                        $scope.write = true;
                    }
                }
                else {
                    $scope.isLoadingData = false;
                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                    error('md');
                }
            },
            function (error) {
                // Error handling function
                console.log(error);
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            });            
        }

        $scope.copyToClipboard = function ($event, copyValue, messageKey) {
            $event.stopPropagation();
            var notification = UtilityService.getAParticularSuccessMessage(messageKey);
            Notifications.toast(notification);
            CopyToClipboard.copy(copyValue);
        }

        $scope.goToMyServiceAccounts = function() {
            $scope.viewPassword = false;
        }

        $scope.error = function (size) {
            Modal.createModal(size, 'error.html', 'ServiceAccountsCtrl', $scope);
        };

        $rootScope.close = function () {
            Modal.close();
        };
        init();
        
    });
})(angular.module('vault.features.ServiceAccountsCtrl',[
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications',
    'vault.constants.RestEndpoints'
]));