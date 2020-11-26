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
    app.controller('AzureServicePrinicipalsCtrl', function($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService, RestEndpoints, CopyToClipboard, AppConstant){

        $scope.isLoadingData = false;       // Variable to set the loader on
        $scope.adminNavTags = safesService.getSafesNavTags();
        $scope.viewPassword = false;
        $scope.ifSecret = false;
        $scope.anyRegex = /.|\s/g;
        $scope.showPassword = false;
        $scope.write = false;
        $scope.svcaccToReset = '';
        $scope.rotateSecretDetails = {};
        $scope.folderList = {};
        $scope.searchValueSvcacc = "";
        $scope.folderDisplay = false;
        var init = function () {
            $scope.loadingData = true;
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
        function getHeaders() {
            return {
                'Content-Type': 'application/json',
                'vault-token': SessionStore.getItem('myVaultKey')
            }
        }
        // Fetching Data
        $scope.filterSvcacc = function(searchValueSvcacc) {
            $scope.searchValueSvcacc = searchValueSvcacc;
        }

        $scope.requestDataFrMyAccounts = function () {
            $scope.isLoadingData = true;
            $scope.svcaccOnboardedData = { "keys": [] };
            var url = RestEndpoints.baseURL + '/v2/azureserviceaccounts/list';
            $http({
                method: 'GET',
                url: url,
                headers: getHeaders()
            }).then(function (response) {
                var accessAzureSvc = JSON.parse(JSON.stringify(response.data.azuresvcacc));
                $scope.svcaccOnboardedData.keys = accessAzureSvc.map(function (azureObject) {
                    var entry = Object.entries(azureObject);
                    return {
                        azuresvcaccname: entry[0][0],
                        permission: entry[0][1]
                    }
                });
                $scope.numOfSvcaccs = $scope.svcaccOnboardedData.keys.length;
                $scope.isLoadingData = false;
            }, function (error) {
                $scope.isLoadingData = false;
                console.log(error);
            })
                .catch(function (catchError) {
                    $scope.isLoadingData = false;
                });
              
        };

        var getPermission = function(azuresvcaccname) {
            var permission,index=0;
            var svcacct = $scope.svcaccOnboardedData.keys;
            for(index; index<$scope.numOfSvcaccs;index++) {
                if (svcacct[index].azuresvcaccname === azuresvcaccname) {
                   permission = svcacct[index].permission;
                   break;
                }
            }
            return permission;

        }


        $scope.viewFolders = function (path) {
            $scope.isLoadingData = true;
            $scope.folderList = {};
            $scope.azuresvcaccSecretData = {"servicePrinicipalName":""};
            var url = RestEndpoints.baseURL + '/v2/azureserviceaccounts/folders/secrets?path=' + 'azuresvcacc/' + path;
            return $http({
                method: 'GET',
                url: url,
                headers: getHeaders()
            })
                .then(function (response) {

                    $scope.folderDisplay = true;
                    $scope.viewPassword = true;
                    $scope.isLoadingData = false;
                    $scope.folderList = response.data;
                    $scope.azuresvcaccSecretData.servicePrinicipalName = response.data.servicePrinicipalName;
                }).catch(function (catchError) {
                    console.error(catchError);

                    $scope.isLoadingData = false;
                    $scope.viewPassword = false;
                    if (catchError.status == 403 || catchError.status == "403") {
                        var errorMsg = "Access Denied: No permission to read or reset secret for Azure service prinicipal";
                        $scope.errorMessage = errorMsg;
                    }
                    else {
                        var errorMsg = catchError.data.errors;
                        $scope.errorMessage = errorMsg[0];
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    }
                    $scope.error('md');
                });
        }

        $scope.viewSecret = function (azuresvcaccname, folderName) {
            var path = azuresvcaccname + '/' + folderName;
            $scope.isLoadingData = true;
            $scope.write = false;
            $scope.azuresvcaccSecretData = {};
            var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/azureserviceaccounts/secrets/" + path;
            AdminSafesManagement.getSecretForAzureSvcacc(null, updatedUrlOfEndPoint).then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isLoadingData = false;
                    $scope.viewPassword = true;
                    $scope.ifSecret = true;
                    $scope.azuresvcaccSecretData = response.data;
                    if (getPermission(azuresvcaccname) == "write") {
                        $scope.write = true;
                    }
                    $scope.azuresvcaccSecretData.servicePrinicipalName = azuresvcaccname + '/' + folderName;
                    $scope.folderDisplay = false;
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
                if (error.status == 403 || error.status == "403") {
                    var errorMsg = error.data.errors;
                    $scope.errorMessage = errorMsg[0];
                }
                else {
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                }
                $scope.error('md');
            });
        }

        $scope.copyToClipboard = function ($event, copyValue, messageKey) {
            $event.stopPropagation();
            var notification = UtilityService.getAParticularSuccessMessage(messageKey);
            Notifications.toast(notification);
            CopyToClipboard.copy(copyValue);
        }

        $scope.rotateAzureSecret = function(rotateSecretDetails) {
            var folderName = $scope.azuresvcaccSecretData.servicePrinicipalName.split("/")[1];
            if (folderName != '') {
                 $scope.isLoadingData = true;
                 Modal.close();
                 AdminSafesManagement.rotateAzureSecret(rotateSecretDetails, "").then(function (response) {
                     if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.viewSecret(rotateSecretDetails.azureSvcAccName, folderName);
                        var notification = UtilityService.getAParticularSuccessMessage("MESSAGE_RESET_SUCCESS");
                        Notifications.toast("Password "+notification);
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
             } else {
                $scope.isLoadingData = false;
                $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            }
        }

        $scope.rotatePasswordPopup = function(azuresvcaccSecretData) {
            $scope.fetchDataError = false;
            var servicePrinicipalName = azuresvcaccSecretData.servicePrinicipalName.split("/")[0];
            if (servicePrinicipalName != undefined) {
                $scope.rotateSecretDetails = {
                    azureSvcAccName: servicePrinicipalName,
                    secretKeyId: azuresvcaccSecretData.secretKeyId,
                    servicePrinicipalId: azuresvcaccSecretData.servicePrinicipalId,
                    tenantId: azuresvcaccSecretData.tenantId
                };
                Modal.createModal('md', 'rotatePopup.html', 'AzureServicePrinicipalsCtrl', $scope);
            }
            else {
                $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            }
        };

        $scope.goToMyServiceAccounts = function() {
            $scope.viewPassword = false;
        }
        var pagesShown = 1;
        var pageSize = AppConstant.PAGE_SIZE;
        $scope.paginationLimit = function(data) {
            $scope.currentshown = pageSize * pagesShown;
            if(($scope.searchValueSvcacc != '' && $scope.searchValueSvcacc!= undefined && $scope.searchValueSvcacc.length>2) || $scope.currentshown >= $scope.numOfSvcaccs){
                $scope.currentshown = $scope.numOfSvcaccs;
            }
            return $scope.currentshown;
        };
        $scope.hasMoreItemsToShow = function() {
            if ($scope.searchValueSvcacc != '' && $scope.searchValueSvcacc!= undefined) {
                if ($scope.searchValueSvcacc.length<3) {
                    return pagesShown < ($scope.numOfSvcaccs / pageSize);
                }
                else {
                    return false;
                }
            }
            return pagesShown < ($scope.numOfSvcaccs / pageSize);
        };
        $scope.showMoreItems = function() {
            pagesShown = pagesShown + 1;
        };
        $scope.error = function (size) {
            Modal.createModal(size, 'error.html', 'AzureServicePrinicipalsCtrl', $scope);
        };

        $rootScope.close = function () {
            Modal.close();
        };
        init();
    });
})(angular.module('vault.features.AzureServicePrinicipalsCtrl',[
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications',
    'vault.constants.RestEndpoints',
    'vault.constants.AppConstant'
]));