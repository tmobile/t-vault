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
        $scope.svcaccToReset = '';
        $scope.searchValueSvcacc = "";
        var init = function () {
            
            if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            else{
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                var feature = JSON.parse(SessionStore.getItem("feature"));
                if (feature.adpwdrotation == false) {
                    $state.go('safes', {'fromLogin':true});
                }
                $scope.requestDataFrMyAccounts();
            }
        };

        // Fetching Data
        $scope.filterSvcacc = function(searchValueSvcacc) {
            $scope.searchValueSvcacc = searchValueSvcacc;
        }

        $scope.requestDataFrMyAccounts = function () {               
            $scope.svcaccOnboardedData = {"keys": []};
            var accessSafes = JSON.parse(SessionStore.getItem("accessSafes"));
            if (accessSafes.svcacct) {
                $scope.svcaccOnboardedData.keys = accessSafes.svcacct.map(function (safeObject) {
                    var entry = Object.entries(safeObject);
                    return {
                        svcaccname: entry[0][0],
                        permission: entry[0][1]
                    }
                });
            }
            $scope.numOfSvcaccs=$scope.svcaccOnboardedData.keys.length;
        };

        $scope.viewSecret = function (svcaccname) {
            $scope.isLoadingData = true;
            $scope.write = false;
            $scope.svcaccSecretData = {"secret":"", "svcaccname":svcaccname, "permission":""};
            var queryParameters = svcaccname;
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSecretForSvcacc',queryParameters);
            AdminSafesManagement.getSecretForSvcacc(null, updatedUrlOfEndPoint).then(function (response) {                
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isLoadingData = false;
                    $scope.viewPassword = true;
                    $scope.ifSecret = true;
                    $scope.svcaccSecretData.secret = response.data.secret;  
                    if ($scope.svcaccSecretData.secret.permission == "write") {
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

        $scope.resetPasswordForSvcacc = function() {           
            if ($scope.svcaccToReset != '') {
                $scope.isLoadingData = true;
                Modal.close();
                var queryParameters = $scope.svcaccToReset;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('resetPasswordForSvcacc',queryParameters);
                AdminSafesManagement.resetPasswordForSvcacc(null, updatedUrlOfEndPoint).then(function (response) {                
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.isLoadingData = false;
                        var notification = UtilityService.getAParticularSuccessMessage("MESSAGE_RESET_SUCCESS");
                        Notifications.toast("Password "+notification);
                        $scope.svcaccToReset = '';
                    }
                    else {
                        $scope.isLoadingData = false;
                        $scope.svcaccToReset = '';
                        $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                        error('md');
                    }
                },
                function (error) {
                    // Error handling function
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.svcaccToReset = '';
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                });    
            } else {
                $scope.isLoadingData = false;
                $scope.svcaccToReset = '';
                $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage('ERROR_GENERAL');
                error('md');
            }
        }

        $scope.resetPasswordPopup = function(svcaccname) {
            $scope.fetchDataError = false;
            $scope.svcaccToReset = svcaccname;
            Modal.createModal('md', 'resetPopup.html', 'ServiceAccountsCtrl', $scope);
        };

        $scope.goToMyServiceAccounts = function() {
            $scope.viewPassword = false;
        }
        var pagesShown = 1;
        var pageSize = 20;
        $scope.paginationLimit = function(data) {
            $scope.currentshown = pageSize * pagesShown;
            if($scope.searchValueSvcacc.length>2 || $scope.currentshown >= $scope.numOfSvcaccs){
                $scope.currentshown = $scope.numOfSvcaccs;
            }
            return $scope.currentshown;
        };
        $scope.hasMoreItemsToShow = function() {
            if ($scope.searchValueSvcacc.length<3) {
                return pagesShown < ($scope.numOfSvcaccs / pageSize);
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