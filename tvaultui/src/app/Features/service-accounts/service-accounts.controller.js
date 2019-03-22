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

        $scope.isLoadingData = false;       // Variable to set the loader on
        $scope.adminNavTags = safesService.getSafesNavTags();
        $scope.viewPassword = false;
        $scope.ifSecret = false;
        $scope.anyRegex = /.|\s/g;
        $scope.showPassword = false;
        $scope.write = false;
        $scope.svcToReset = '';
        $scope.searchValueSvc = "";
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
        $scope.filterSvc = function(searchValueSvc) {
            $scope.searchValueSvc = searchValueSvc;
        }

        $scope.requestDataFrMyAccounts = function () {               
            $scope.svcOnboardedData = {"keys": []};
            var accessSafes = JSON.parse(SessionStore.getItem("accessSafes"));
            if (accessSafes.svcacct) {
                $scope.svcOnboardedData.keys = accessSafes.svcacct.map(function (safeObject) {
                    var entry = Object.entries(safeObject);
                    return {
                        svcname: entry[0][0],
                        permission: entry[0][1]
                    }
                });
            }
            $scope.numOfSvcs=$scope.svcOnboardedData.keys.length;
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

        $scope.resetPasswordForSvc = function() {           
            if ($scope.svcToReset != '') {
                $scope.isLoadingData = true;
                Modal.close();
                var queryParameters = $scope.svcToReset;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('resetPasswordForSvc',queryParameters);
                AdminSafesManagement.resetPasswordForSvc(null, updatedUrlOfEndPoint).then(function (response) {                
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.isLoadingData = false;
                        var notification = UtilityService.getAParticularSuccessMessage("MESSAGE_RESET_SUCCESS");
                        Notifications.toast("Password "+notification);
                        $scope.svcToReset = '';
                    }
                    else {
                        $scope.isLoadingData = false;
                        $scope.svcToReset = '';
                        $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                        error('md');
                    }
                },
                function (error) {
                    // Error handling function
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.svcToReset = '';
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                });    
            } else {
                $scope.isLoadingData = false;
                $scope.svcToReset = '';
                $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage('ERROR_GENERAL');
                error('md');
            }
        }

        $scope.resetPasswordPopup = function(svcname) {
            $scope.fetchDataError = false;
            $scope.svcToReset = svcname;
            Modal.createModal('md', 'resetPopup.html', 'ServiceAccountsCtrl', $scope);
        };

        $scope.goToMyServiceAccounts = function() {
            $scope.viewPassword = false;
        }
        var pagesShown = 1;
        var pageSize = 20;
        $scope.paginationLimit = function(data) {
            $scope.currentshown = pageSize * pagesShown;
            if($scope.searchValueSvc.length>2 || $scope.currentshown >= $scope.numOfSvcs){
                $scope.currentshown = $scope.numOfSvcs;
            }
            return $scope.currentshown;
        };
        $scope.hasMoreItemsToShow = function() {
            if ($scope.searchValueSvc.length<3) {
                return pagesShown < ($scope.numOfSvcs / pageSize);
            }
            return false;
        };
        $scope.showMoreItems = function() {
            pagesShown = pagesShown + 1;
        };
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