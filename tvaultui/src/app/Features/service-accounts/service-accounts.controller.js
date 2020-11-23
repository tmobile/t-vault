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
    app.controller('ServiceAccountsCtrl', function($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService, RestEndpoints, CopyToClipboard, AppConstant){

        $scope.isLoadingData = false;       // Variable to set the loader on
        $scope.adminNavTags = safesService.getSafesNavTags();
        $scope.viewPassword = false;
        $scope.ifSecret = false;
        $scope.anyRegex = /.|\s/g;
        $scope.showPassword = false;
        $scope.write = false;
        $scope.svcaccToReset = '';
        $scope.searchValueSvcacc = "";
        $scope.decommitionMessage = "";
        $scope.svcaccToOffboard = "";
        $scope.isOffboarding = false;
        var init = function () {
            $scope.loadingData = true;
            $scope.decommitionMessage = "";
            $scope.svcaccToOffboard = "";
            $scope.isOffboarding = false;
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
            var url = RestEndpoints.baseURL + '/v2/serviceaccounts/list';
            $http({
                method: 'GET',
                url: url,
                headers: getHeaders()
            }).then(function (response) {
                var accessSafes = JSON.parse(JSON.stringify(response.data.svcacct));
                $scope.svcaccOnboardedData.keys = accessSafes.map(function (safeObject) {
                    var entry = Object.entries(safeObject);
                    return {
                        svcaccname: entry[0][0],
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

        var getPermission = function(svcaccname) {
            var permission,index=0;
            var svcacct = $scope.svcaccOnboardedData.keys;
            for(index; index<$scope.numOfSvcaccs;index++) {
                if (svcacct[index].svcaccname === svcaccname) {
                   permission = svcacct[index].permission;
                   break;
                }
            }
            return permission;

        }

        $scope.viewSecret = function (svcaccname) {
            $scope.isLoadingData = true;
            $scope.decommitionMessage = "";
            $scope.write = false;
            $scope.svcaccSecretData = {"secret":"", "svcaccname":svcaccname, "permission":""};
            var queryParameters = "serviceAccountName="+svcaccname;
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSecretForSvcacc',queryParameters);
            AdminSafesManagement.getSecretForSvcacc(null, updatedUrlOfEndPoint).then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isLoadingData = false;
                    $scope.viewPassword = true;
                    $scope.ifSecret = true;
                    $scope.svcaccSecretData.secret = response.data.current_password;
                    if (getPermission(svcaccname) == "write") {
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
                if (error.status == 403 || error.status == "403") {
                    var errorMsg = error.data.errors;
                    $scope.errorMessage = errorMsg[0];
                    $scope.error('md');
                }
                else if (error.status == 404 || error.status == "404") {
                    var errorMsg = error.data.errors;
                    $scope.decommitionMessage = errorMsg[0];
                    $scope.svcaccToOffboard = svcaccname;
                    $scope.isOffboarding = true;
                    Modal.createModal('md', 'decommissionMessagePopup.html', 'ServiceAccountsCtrl', $scope);
                }
                else {
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }
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
                var queryParameters = "serviceAccountName="+$scope.svcaccToReset;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('resetPasswordForSvcacc',queryParameters);
                AdminSafesManagement.resetPasswordForSvcacc(null, updatedUrlOfEndPoint).then(function (response) {                
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.isLoadingData = false;
                        $scope.svcaccSecretData.secret = response.data.current_password;
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
            Modal.createModal(size, 'error.html', 'ServiceAccountsCtrl', $scope);
        };

        $rootScope.close = function () {
            Modal.close();
        };

        $scope.cancelOffboard =  function () {
            $scope.isOffboarding = false;
            Notifications.toast("Loading service accounts..");
            Modal.close('close');
            $scope.requestDataFrMyAccounts();
        }

        $scope.offboardNow = function (svcaccUserId) {
            if (svcaccUserId != '') {
                $scope.isOffboarding = true;
                Modal.close();
                $scope.isLoadingData = true;
                var queryParameters = "path=ad/roles/"+svcaccUserId;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccMetadata', queryParameters);
                AdminSafesManagement.getSvcaccMetadata(null, updatedUrlOfEndPoint).then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        try {
                            if (response.data.data) {
                                var managedBy = response.data.data.managedBy;
                                if (SessionStore.getItem("username").toLowerCase() == managedBy.toLowerCase()) {
                                    Notifications.toast("Offboarding decommissioned service account..");
                                    var offboardPayload = {
                                        "owner": managedBy,
                                        "name": svcaccUserId
                                    }
                                    AdminSafesManagement.offboardSvcacc(offboardPayload, '').then(
                                        function (response) {
                                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                                $scope.isLoadingData = false;
                                                Modal.createModal('md', 'offboardWarning.html', 'ChangeServiceAccountCtrl', $scope);
                                            }
                                            else {
                                                $scope.isLoadingData = false;
                                                $scope.isOffboarding = false;
                                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                                $scope.error('md');
                                            }
                                            $scope.svcaccToOffboard = "";
                                        },
                                        function (error) {
                                            // Error handling function
                                            console.log(error);
                                            $scope.svcaccToOffboard = '';
                                            $scope.isLoadingData = false;
                                            $scope.isOffboarding = false;
                                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                            $scope.error('md');
                                        });
                                }
                                else {
                                    $scope.isLoadingData = false;
                                    $scope.isOffboarding = false;
                                    $scope.svcaccToOffboard = '';
                                }
                            }
                        } catch (e) {
                            console.log(e);
                            $scope.svcaccToOffboard = '';
                            $scope.isLoadingData = false;
                            $scope.isOffboarding = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                            $scope.error('md');
                        }
                    }
                    else {
                        $scope.svcaccToOffboard = '';
                        $scope.isLoadingData = false;
                        $scope.isOffboarding = false;
                        $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                        $scope.error('md');
                    }
                },
                function (error) {
                    // Error handling function
                    console.log(error);
                    $scope.svcaccToOffboard = '';
                    $scope.isLoadingData = false;
                    $scope.isOffboarding = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                });
            }
        }

        init();

    });
})(angular.module('vault.features.ServiceAccountsCtrl',[
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications',
    'vault.constants.RestEndpoints',
    'vault.constants.AppConstant'
]));