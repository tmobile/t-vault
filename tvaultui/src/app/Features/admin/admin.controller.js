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
    app.controller('AdminCtrl', function ($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService, RestEndpoints, filterFilter, orderByFilter) {

        $scope.filterValue = '';            // Initial search filter value kept empty
        $scope.isLoadingData = false;       // Variable to set the loader on
        $scope.fetchDataError = false;      // set when there is any error while fetching or massaging data
        $scope.dataForTable = [];           // Array of data after massaging, to be used for table display
        $scope.tilesData = {};
        $scope.tilesData["SafesData"] = [];
        $scope.svcaccToOffboard = '';
        $scope.svcaccToTransfer = '';
        $scope.searchValue = '';
        $scope.isCollapsed = true;
        $scope.existingTargetSystem = false;
        $scope.existingService = false;
        $scope.isCertCollapsed = false;
        $scope.isTargetCollapsed = true;
        $scope.isTargetServiceCollapsed = true;
        // Type of safe to be filtered from the rest

        $scope.safeType = {
            "type": ""
        };

        // Dropdown list values

        $scope.tableOptions = [
            {
                "type": "User Safe",
                "value": "User Safe"
            }, {
                "type": "Shared Safe",
                "value": "Shared Safe"
            }, {
                "type": "Application Safe",
                "value": "Application Safe"
            }
        ];
        $scope.approleConfPopupObj = {
            "token_max_ttl": "",
            "token_ttl": "",
            "role_name": "",
            "policies": "",
            "bind_secret_id": "",
            "secret_id_num_uses": "",
            "secret_id_ttl": "",
            "token_num_uses": ""
        };

        $scope.onBoardADObj = {
            "service_account_name": "",
            "auto_rotation": "",
            "password_ttl": ""
        };

        $scope.adminNavTags = safesService.getSafesNavTags();

        $scope.showNotification = function () {
            console.log('showing notify');
            Modal.createModal('md', 'notify.html', 'AdminCtrl', $scope);
        };

        $scope.selectedGroupOption = $scope.tableOptions[0];

        $scope.dropDownOptions = {
            'selectedGroupOption': $scope.selectedGroupOption,
            'tableOptions': $scope.tableOptions
        };
        $scope.actionDropDownOptions = {
            'selectedGroupOption': {
                "type": "Action"
            },
            'tableOptions': [
                {
                    "type": "Edit",
                    "srefValue": {
                        'url': 'change-safe',
                        'obj': 'safeObject',
                        'myobj': 'listDetails'
                    }
                }, {
                    "type": "Delete",
                    "srefValue": 'href'
                }
            ]
        };

        $scope.viewSecretIdAccessors = { "status": false, "value": "" };
        $rootScope.secretId = "";
        $rootScope.accessorId = "";
        $scope.accessorListToDelete = [];
        $scope.rolenameExists = false;
        var init = function () {
            if (!SessionStore.getItem("myVaultKey")) { /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            $scope.enableSvcacc = true;
            $scope.enableSelfService = true;
            $scope.isCollapsed = true;
            $scope.transferFailedMessage = '';
            $scope.selectedIndex = 0;
            $scope.isCollapsed = true;
            $scope.existingTargetSystem = false;
            $scope.existingService = false;
            $scope.certSearchValue = "";
            $scope.certificateData = { "certificates": [] };
            $scope.targetSystem = { "type": "new" };
            $scope.targetSystemService = { "type": "new" };
            $scope.targetSystemSelected = false;
            $scope.showInputLoader = {
                'show': false
            };
            $scope.showServiceInputLoader = {
                'show': false
            };
            if ($state.current.name == "manage" && JSON.parse(SessionStore.getItem("isAdmin")) == true) {
                $state.go('admin');
                return;
            }
            if ($state.current.name == "admin" && JSON.parse(SessionStore.getItem("isAdmin")) == false) {
                $state.go('manage');
                return;
            }
            if ($rootScope.lastVisited == "change-service-account") {
                $scope.selectedIndex = 2;
            }

            var feature = JSON.parse(SessionStore.getItem("feature"));
            if (feature.adpwdrotation == false) {
                $scope.enableSvcacc = false;
            }
            if (feature.selfservice == false) {
                $scope.enableSelfService = false;
            }
            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
            if ($scope.enableSvcacc == false && $scope.enableSelfService == false && JSON.parse(SessionStore.getItem("isAdmin")) == false) {
                $state.go('safes');
                return;
            }
            $scope.requestDataFrAdmin();
            getWorkloadDetails();

        };

        // Updating the data based on type of safe, by clicking dropdown
        $scope.filterUpdate = function (option) {
            $scope.filterValue = option.value;
            if (option.value === 'All safes') {
                $scope.filterValue = '';
            }
            $scope.selectedGroupOption = option;
        };

        // massaging data from server
        $scope.massageDataForTiles = function (data, currentVaultType) {
            try {
                var vaultDisplayType = '';

                switch (currentVaultType) {
                    case 'apps':
                        vaultDisplayType = "Application Safe";
                        break;
                    case 'users':
                        vaultDisplayType = "User Safe";
                        break;
                    case 'shared':
                        vaultDisplayType = "Shared Safe";
                        break;
                }

                var obj = $scope.tilesData.SafesData;
                var newobj = {};
                newobj["type"] = vaultDisplayType;
                newobj["safes"] = [];

                for (var i = 0; i < data.keys.length; i++) {

                    newobj["safes"][i] = {};
                    newobj["safes"][i]["safe"] = data.keys[i];
                    newobj["safes"][i]["safeType"] = currentVaultType;
                }
                obj.push(newobj);
                $scope.tilesData.SafesData = obj;
                if ($scope.tilesData.SafesData.length === 3) {
                    if ($scope.enableSvcacc == false) {
                        $scope.isLoadingData = false;
                    }
                    $scope.data = $scope.tilesData.SafesData;
                    $scope.massageData($scope.data);
                }
            } catch (e) {

                // To handle errors while massaging data
                console.log(e);
                if ($scope.enableSvcacc == false) {
                    $rootScope.isLoadingData = false;
                }
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                $scope.error('md');

            }
        };

        $scope.massageData = function (data) {
            try {
                $scope.dataForTable = [];
                for (var i = 0; i < data.length; i++) {            // for each of the 'Types' of safes
                    var safes = data[i].safes;
                    var type = data[i].type;
                    for (var j = 0; j < safes.length; j++) {        // for each safe in the current type of safe
                        var currentSafeObject = safes[j];
                        currentSafeObject["type"] = type;
                        currentSafeObject["safeType"] = safes[j].safeType;
                        $scope.dataForTable.push(currentSafeObject);
                    }
                }
            } catch (e) {
                // To handle errors while massaging data
                console.log(e);
                $rootScope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                $scope.error('md');
            }
        };

        /* TODO: Change the name of this function to something which includes safe instead of folder */
        $scope.adminEditFolder = function (listItem) {
            var obj = "safeObject";
            var myobj = listItem;

            var fullObj = {};
            fullObj[obj] = myobj;
            $scope.isLoadingData = true;
            var queryParameters = "path=" + fullObj.safeObject.safeType + '/' + fullObj.safeObject.safe;
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSafeInfo', queryParameters);
            AdminSafesManagement.getSafeInfo(null, updatedUrlOfEndPoint).then(
                function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        // Try-Catch block to catch errors if there is any change in object structure in the response
                        try {
                            $scope.isLoadingData = false;
                            var object = response.data.data;
                            if (object.name && object.owner && object.description) {
                                $state.go('change-safe', fullObj);
                            }
                            else {
                                $scope.isLoadingData = false;
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_CONTENT_NOT_FOUND');
                                $scope.error('md');
                            }
                        }
                        catch (e) {
                            console.log(e);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_CONTENT_NOT_FOUND');
                            $scope.error('md');
                        }
                    }
                    else {
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_CONTENT_NOT_FOUND');
                        $scope.error('md');
                    }
                },
                function (error) {
                    // Error handling function
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_CONTENT_NOT_FOUND');
                    $scope.error('md');

                })
        };
        $scope.deleteSafePopup = function (safeToDelete) {
            $scope.fetchDataError = false;
            $rootScope.safeToDelete = safeToDelete;
            Modal.createModal('md', 'deleteSafePopup.html', 'AdminCtrl', $scope);
        };
        $rootScope.deleteSafe = function (listItem) {
            if ($rootScope.safeToDelete !== null && $rootScope.safeToDelete !== undefined) {
                listItem = $rootScope.safeToDelete;
            }
            $rootScope.safeToDelete = null;
            try {
                $scope.isLoadingData = true;
                Modal.close();
                var queryParameters = "path=" + listItem.safeType + '/' + listItem.safe;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('deleteSafe', queryParameters);
                AdminSafesManagement.deleteSafe(null, updatedUrlOfEndPoint).then(
                    function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            $scope.isLoadingData = false;
                            var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_SAFE_DELETE');
                            Notifications.toast(listItem.safe + notification);
                            // remove deleted safe from session storage
                            var currentSafesList = JSON.parse(SessionStore.getItem("allSafes"));
                            var index = currentSafesList.indexOf(listItem.safe);
                            if (index > -1) {
                                currentSafesList.splice(index, 1);
                                SessionStore.setItem('allSafes', JSON.stringify(currentSafesList));
                            }
                            // Try-Catch block to catch errors if there is any change in object structure in the response
                            try {

                                for (var i = 0; i < $scope.tilesData.SafesData.length; i++) {

                                    if ($scope.tilesData.SafesData[i].type == listItem.type) {

                                        for (var j = 0; j < $scope.tilesData.SafesData[i].safes.length; j++) {
                                            if ($scope.tilesData.SafesData[i].safes[j].safe == listItem.safe) {
                                                $scope.tilesData.SafesData[i].safes.splice(j, 1);
                                                $scope.data = $scope.tilesData.SafesData;
                                                $scope.massageData($scope.data);
                                            }
                                        }
                                    }
                                }
                            }
                            catch (e) {
                                console.log(e);
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                                $scope.error('md');
                            }
                        }
                        else {
                            $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                            $scope.error('md');
                        }
                    },
                    function (e) {
                        console.log(e);
                        // Error handling function
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');

                    })
            } catch (e) {
                console.log(e);
                // To handle errors while calling 'fetchData' function
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');

            }
        };

        $scope.error = function (size) {
            Modal.createModal(size, 'error.html', 'AdminCtrl', $scope);
        };

        $rootScope.close = function () {
            Modal.close();
        };

        $rootScope.cancel = function () {
            Modal.close();
        };
        $rootScope.cancelCert = function () {
            $scope.isCertCollapsed = false;
            $scope.isTargetCollapsed = true;
            $scope.isTargetServiceCollapsed = true;
            Modal.close();
        };

        $rootScope.deleteAccessorCancel = function () {
            Modal.close();
            $scope.showAccessorsPopUp($scope.approleToShow);
        };
        // Fetching Data

        $scope.requestDataFrAdmin = function () {
            if ($scope.enableSelfService == true || JSON.parse(SessionStore.getItem("isAdmin")) == true) {
                var vaultTypes = ["apps", "shared", "users"];

                var responseArray = [];
                var allSafes = [];
                vaultTypes.forEach(function (currentVaultType) {
                    try {

                        var queryParameters = "path=" + currentVaultType;
                        var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('safesList', queryParameters);
                        $scope.isLoadingData = true;
                        AdminSafesManagement.getCompleteSafesList(null, updatedUrlOfEndPoint).then(
                            function (response) {
                                if (UtilityService.ifAPIRequestSuccessful(response)) {
                                    if ($scope.enableSvcacc == false) {
                                        $scope.isLoadingData = false;
                                    }
                                    // Try-Catch block to catch errors if there is any change in object structure in the response
                                    try {
                                        allSafes = allSafes.concat(response.data.keys);
                                        SessionStore.setItem('allSafes', JSON.stringify(allSafes));
                                        $scope.massageDataForTiles(response.data, currentVaultType);
                                    }
                                    catch (e) {
                                        console.log(e);
                                        $scope.error('md');
                                    }
                                }
                                else {
                                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                                    $scope.error('md');
                                }
                            },
                            function (error) {
                                // Error handling function
                                if (error.status !== 404) {
                                    if ($scope.enableSvcacc == false) {
                                        $scope.isLoadingData = false;
                                    }
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                    $scope.error('md');
                                }
                                else {
                                    $scope.massageDataForTiles([], currentVaultType);
                                }
                            })
                    } catch (e) {
                        // To handle errors while calling 'fetchData' function
                        if ($scope.enableSvcacc == false) {
                            $scope.isLoadingData = false;
                        }
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');

                    }
                });
                $scope.appRoleData = { "keys": [] };
                AdminSafesManagement.getApproles().then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.appRoleData = response.data;
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
            }
            if ($scope.enableSvcacc == true) {
                $scope.numOfSvcaccs = 0;
                $scope.svcaccOnboardedData = { "keys": [] };
                $scope.isLoadingData = true;
                AdminSafesManagement.getOnboardedServiceAccounts().then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.isLoadingData = false;
                        $scope.svcaccOnboardedData = response.data;
                        $scope.numOfSvcaccs = $scope.svcaccOnboardedData.keys.length;
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

        };



        $scope.tabChangeForAdminCert = function () {
            $scope.searchValue = '';
            if ($scope.certificatesLoaded == false) {
                $scope.isLoadingData = true;
            }
        }

        $scope.getCertSubjectName = function (cert) {
            var certName = "";
            if (cert.subjectAltName && cert.subjectAltName.dns && cert.subjectAltName.dns.length > 0) {
                certName = cert.subjectAltName.dns[0];
            }
            if (certName == "" || certName == undefined) {
                var names = cert.sortedSubjectName.split(',');
                if (names.length > 0) {
                    certName = names[0].substr(3)
                }
            }
            return certName;
        }

        $scope.searchCert = function () {
            if ($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2 && $scope.certSearchValue != $scope.searchValue) {
                $scope.certSearchValue = $scope.searchValue;
                getCertificates($scope.certSearchValue, null, null);
            }
            if ($scope.searchValue == '' && $scope.certSearchValue != $scope.searchValue) {
                $scope.certSearchValue = $scope.searchValue;
                getCertificates("", null, null);
            }
        }

        $scope.showMoreCert = function () {
            var offset = $scope.certificateData.offset;
            var limit = $scope.certificateData.limit;
            getCertificates($scope.certSearchValue, limit, limit + offset);
        }

        $scope.getTargetSystems = function (searchVal) {
            $scope.targetSystemList = [];
            $scope.targetSystemSelected = false;
            var queryParameters = "freeText=" + searchVal;
            if (searchVal.length > 2) {
                $scope.showInputLoader.show = true;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getTargetSystems', queryParameters);
                return AdminSafesManagement.getTargetSystems(null, updatedUrlOfEndPoint).then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.targetSystemList = response.data.targetSystems;
                        $scope.showInputLoader.show = false;
                        return orderByFilter(filterFilter($scope.targetSystemList, searchVal), 'name', true);
                    }
                    else {
                        $scope.showInputLoader.show = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    }
                },
                    function (error) {
                        // Error handling function
                        console.log(error);
                        $scope.showInputLoader.show = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    });
            }
        }

        $scope.getTargetSystemService = function (searchVal) {
            $scope.targetSystemList = [];
            $scope.targetSystemServiceSelected = false;
            if ($scope.targetSystemSelected == true) {
                var targetSystemId = $scope.cert.targetSystem.targetSystemID;
                var queryParameters = "targetSystemId=" + targetSystemId + "&freeText=" + searchVal;
                if (searchVal.length > 2) {
                    $scope.showServiceInputLoader.show = true;
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getTargetSystemsServices', queryParameters);
                    return AdminSafesManagement.getTargetSystemsServices(null, updatedUrlOfEndPoint).then(function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            $scope.targetSystemServicesList = response.data.targetsystemservices;
                            $scope.showServiceInputLoader.show = false;
                            return orderByFilter(filterFilter($scope.targetSystemServicesList, searchVal), 'name', true);
                        }
                        else {
                            $scope.showServiceInputLoader.show = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        }
                    },
                        function (error) {
                            // Error handling function
                            console.log(error);
                            $scope.showServiceInputLoader.show = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        });
                }
            }
        }

        $scope.selectTargetSystem = function (targetSystem) {
            $scope.cert["targetSystem"] = targetSystem;
            $scope.targetSystemSelected = true;
            $scope.isCollapsed = !$scope.isCollapsed;
        }

        $scope.selectTargetSystemService = function (targetSystemService) {
            $scope.cert["targetSystemService"] = targetSystemService;
            $scope.targetSystemServiceSelected = true;
        }

        $scope.newAppRoleConfiguration = function (size) {
            // To reset the aws configuration details object to create a new one
            $scope.editingApprole = { "status": false };
            $scope.roleNameSelected = false;
            $scope.approleConfPopupObj = {
                "token_max_ttl": "",
                "token_ttl": "",
                "role_name": "",
                "policies": "",
                "bind_secret_id": "",
                "secret_id_num_uses": "",
                "secret_id_ttl": "",
                "token_num_uses": ""
            };
            $scope.openApprole(size);
        }

        $scope.editOnboardedSvcacc = function (userId, size) {
            var obj = "svcaccData";
            var fullObj = {};
            fullObj[obj] = { "userId": userId };
            $state.go('change-service-account', fullObj);
        }

        $scope.onboardSvcaccAccount = function (size) {
            var obj = "svcaccList";
            var fullObj = {};
            fullObj[obj] = [];
            $state.go('change-service-account', fullObj);
        }

        $scope.offboardSvcaccPopUp = function (svcaccname) {
            $scope.svcaccToOffboard = svcaccname;
            Modal.createModal('md', 'offboardSvcaccPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.offboardSvcacc = function (svcaccUserId) {
            if (svcaccUserId != '') {
                Modal.close();
                $scope.isLoadingData = true;
                //var queryParameters = svcaccUserId;
                var queryParameters = "serviceAccountName=" + svcaccUserId + "&excludeOnboarded=false";
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccInfo', queryParameters);
                //var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccOnboardInfo', queryParameters);
                //AdminSafesManagement.getSvcaccOnboardInfo(null, updatedUrlOfEndPoint).then(
                AdminSafesManagement.getSvcaccInfo(null, updatedUrlOfEndPoint).then(
                    function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            try {
                                if (response.data.data.values.length > 0) {
                                    var object = response.data.data.values[0];
                                    var offboardPayload = {
                                        "owner": object.managedBy.userName,
                                        "name": svcaccUserId
                                    }
                                    AdminSafesManagement.offboardSvcacc(offboardPayload, '').then(
                                        function (response) {
                                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                                try {
                                                    $scope.isLoadingData = false;
                                                    var currentOnboardList = $scope.svcaccOnboardedData.keys;
                                                    for (var i = 0; i < currentOnboardList.length; i++) {
                                                        if (currentOnboardList[i] == svcaccUserId) {
                                                            currentOnboardList.splice(i, 1);
                                                            $scope.svcaccOnboardedData.keys = currentOnboardList;
                                                            break;
                                                        }
                                                    }
                                                    $scope.svcaccToOffboard = '';
                                                    Modal.createModal('md', 'offboardWarning.html', 'AdminCtrl', $scope);
                                                }
                                                catch (e) {
                                                    console.log(e);
                                                    $scope.svcaccToOffboard = '';
                                                    $scope.isLoadingData = false;
                                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                                    $scope.error('md');
                                                }
                                            }
                                            else {
                                                $scope.isLoadingData = false;
                                                $scope.svcaccToOffboard = '';
                                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                                $scope.error('md');
                                            }
                                        },
                                        function (error) {
                                            // Error handling function
                                            console.log(error);
                                            $scope.isLoadingData = false;
                                            $scope.svcaccToOffboard = '';
                                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                            $scope.error('md');

                                        });
                                } else {
                                    console.log(error);
                                    $scope.isLoadingData = false;
                                    $scope.svcaccToOffboard = '';
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                    $scope.error('md');
                                }
                            }
                            catch (e) {
                                console.log(e);
                                $scope.isLoadingData = false;
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                                $scope.error('md');
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
                    })
            }
            else {
                $scope.isLoadingData = false;
                $scope.svcaccToOffboard = '';
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            }
        }

        $scope.tabChangeForAdmin = function () {
            $scope.searchValue = '';
        }
        var pagesShown = 1;
        var pageSize = 8;

        $scope.paginationLimit = function (data) {
            $scope.currentshown = pageSize * pagesShown;
            if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.currentshown >= $scope.numOfSvcaccs) {
                $scope.currentshown = $scope.numOfSvcaccs;
            }
            return $scope.currentshown;
        };
        $scope.hasMoreItemsToShow = function () {
            if ($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length < 3) {
                return pagesShown < ($scope.numOfSvcaccs / pageSize);
            }
            return false;
        };
        $scope.showMoreItems = function () {
            pagesShown = pagesShown + 1;
        };

        $scope.createApprole = function () {
            try {
                Modal.close('');
                $scope.isLoadingData = true;
                var apiCallFunction = '';
                var reqObjtobeSent = {};
                if ($scope.editingApprole.status == true) {
                    $scope.approleConfPopupObj.policies = [];
                    $scope.approleConfPopupObj.bind_secret_id = true;
                    apiCallFunction = AdminSafesManagement.updateAppRole;
                    reqObjtobeSent = $scope.approleConfPopupObj;
                } else {
                    $scope.approleConfPopupObj.policies = [];
                    $scope.approleConfPopupObj.bind_secret_id = true;
                    apiCallFunction = AdminSafesManagement.addAppRole;
                    reqObjtobeSent = $scope.approleConfPopupObj;
                }
                var updatedUrlOfEndPoint = "";
                apiCallFunction(reqObjtobeSent, updatedUrlOfEndPoint).then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        // Try-Catch block to catch errors if there is any change in object structure in the response
                        try {
                            $scope.isLoadingData = false;
                            if ($scope.editingApprole.status == true) {
                                var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_UPDATE_SUCCESS');
                            }
                            else {
                                $scope.appRoleData.keys.push($scope.approleConfPopupObj.role_name);
                                $scope.appRoleData.keys.sort();
                                var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_CREATE_SUCCESS');
                            }
                            Notifications.toast('Approle ' + notification);
                        } catch (e) {
                            console.log(e);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                            $scope.error('md');
                        }
                    }
                    else {
                        $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                        error('md');
                    }
                    $scope.roleNameSelected = false;
                },
                    function (error) {
                        // Error handling function
                        console.log(error);
                        $scope.isLoadingData = false;
                        $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(error);
                        $scope.error('md');
                    })
            } catch (e) {
                // To handle errors while calling 'fetchData' function
                $scope.isLoadingData = false;
                console.log(e);
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            }

        }

        /* TODO: What is open, functon name should be more descriptive */
        $scope.openApprole = function (size) {
            Modal.createModal(size, 'appRolePopup.html', 'AdminCtrl', $scope);
        };

        $scope.enableTTL = function (e) {
            angular.element(document.getElementById('password_ttl'))[0].disabled = false;
        }

        $scope.disableTTL = function (e) {
            angular.element(document.getElementById('password_ttl'))[0].disabled = true;
        }

        $scope.isApproleBtnDisabled = function () {
            if ($scope.approleConfPopupObj.token_max_ttl != '' && $scope.approleConfPopupObj.token_ttl != ''
                && $scope.approleConfPopupObj.role_name.length > 0 && $scope.approleConfPopupObj.secret_id_num_uses != ''
                && $scope.approleConfPopupObj.secret_id_ttl != '' && ($scope.approleConfPopupObj.token_num_uses != ''
                    || $scope.approleConfPopupObj.token_num_uses.length != '')) {
                return false;
            }
            else if ($scope.roleNameSelected) {
                return false;
            }
            return true;
        }

        $scope.deleteAccessorPopUp = function () {
            Modal.createModal('md', 'deleteAccessorPopup.html', 'AdminCtrl', $scope);
        };

        $scope.deleteAccessor = function () {
            try {
                if ($scope.accessorListToDelete.length > 0) {
                    $scope.isLoadingData = true;
                    Modal.close();
                    var approlename = $scope.approleToShow;
                    $scope.approleToShow = '';
                    var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/ss/approle/" + approlename + "/secret_id";
                    var reqObjtobeSent = { "accessorIds": $scope.accessorListToDelete, "role_name": approlename };
                    AdminSafesManagement.deleteAccessorID(reqObjtobeSent, updatedUrlOfEndPoint).then(function (response) {
                        $scope.isLoadingData = false;
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ACCESSOR_DELETE');
                            Notifications.toast(notification);
                            $scope.showAccessorsPopUp(approlename);

                        } else {
                            console.log(error);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        }
                    },
                        function (error) {
                            // Error handling function
                            console.log(error);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        })
                }
            } catch (e) {
                console.log(e);
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');

            }
        }

        $scope.chooseAccessor = function (chooseAccessor) {
            if ($scope.accessorListToDelete.indexOf(chooseAccessor) !== -1) {
                var index = $scope.accessorListToDelete.indexOf(chooseAccessor);
                $scope.accessorListToDelete.splice(index, 1);
            }
            else {
                $scope.accessorListToDelete.push(chooseAccessor);
            }
        }
        $scope.createSecretIDPopUp = function (approlename) {
            $rootScope.createSecretIDForAppRole = approlename;
            $rootScope.secretId = "";
            $rootScope.accessorId = "";
            Modal.createModal('md', 'createSecretIDPopUp.html', 'AdminCtrl', $scope);
        }

        $scope.createSecretID = function (approlename) {
            if ($rootScope.createSecretIDForAppRole !== null && $rootScope.createSecretIDForAppRole !== undefined) {
                approlename = $rootScope.createSecretIDForAppRole;
            }
            $rootScope.createSecretIDForAppRole = null;
            try {
                $scope.isLoadingData = true;
                Modal.close();
                var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/ss/approle/" + approlename + "/secret_id";
                AdminSafesManagement.readSecretID(null, updatedUrlOfEndPoint).then(function (response) {
                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_CREATE_SUCCESS');
                        Notifications.toast("Secret ID " + notification);
                        var secretId = response.data.data.secret_id_accessor;
                        var accessorId = response.data.data.secret_id_accessor;
                        if (secretId != "" && secretId != undefined && accessorId != "" && accessorId !== undefined) {
                            saveSecretIDPopUp(response.data.data.secret_id, response.data.data.secret_id_accessor, approlename);
                        }
                        else {
                            console.log(error);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        }
                    } else {
                        console.log(error);
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    }
                },
                    function (error) {
                        // Error handling function
                        console.log(error);
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    })
            } catch (e) {
                console.log(e);
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');

            }
        }

        var saveSecretIDPopUp = function (secretId, accessorId, approlename) {
            $rootScope.secretId = secretId;
            $rootScope.accessorId = accessorId;
            $rootScope.approlename = approlename;
            Modal.createModal('md', 'notifySecretID.html', 'AdminCtrl', $scope);
        }

        $scope.deleteApprolePopUp = function (approlename) {
            $rootScope.appRoleToDelete = approlename;
            Modal.createModal('md', 'deleteApprolePopUp.html', 'AdminCtrl', $scope);
        }

        $scope.deleteAppRole = function (approlename) {
            if ($rootScope.appRoleToDelete !== null && $rootScope.appRoleToDelete !== undefined) {
                approlename = $rootScope.appRoleToDelete;
            }
            $rootScope.appRoleToDelete = null;
            try {
                $scope.isLoadingData = true;
                Modal.close();
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('deleteAppRole', approlename);
                AdminSafesManagement.deleteAppRole(null, updatedUrlOfEndPoint).then(function (response) {
                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_DELETE_SUCCESS');
                        Notifications.toast(approlename + notification);
                        var currentApproleList = $scope.appRoleData.keys;
                        var index = currentApproleList.indexOf(approlename);
                        if (index > -1) {
                            currentApproleList.splice(index, 1);
                            $scope.appRoleData.keys = currentApproleList;
                        }
                    } else {
                        console.log(error);
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    }
                },
                    function (error) {
                        // Error handling function
                        console.log(error);
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    })
            } catch (e) {
                console.log(e);
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');

            }
        }

        $scope.downloadIDs = function (secretId, showAccessorId, approlename) {
            var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/ss/approle/" + approlename + "/role_id";
            AdminSafesManagement.readRoleID(null, updatedUrlOfEndPoint).then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    var roleId = response.data.data.role_id;
                    var pom = document.createElement('a');
                    var text = "Approle,RoleID,Owner,SecretID,AccessorID\r\n" + approlename + "," + roleId + "," + SessionStore.getItem("username") + "," + secretId + "," + showAccessorId;
                    pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
                    pom.setAttribute('download', approlename + '_' + showAccessorId + '.csv');

                    if (window.navigator && window.navigator.msSaveOrOpenBlob) {
                        const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
                        window.navigator.msSaveOrOpenBlob(blob, approlename + '_' + showAccessorId + '.csv');
                    }
                    else if (document.createEvent) {
                        var event = document.createEvent('MouseEvents');
                        event.initEvent('click', true, true);
                        pom.dispatchEvent(event);
                    }
                    else {
                        pom.click();
                    }
                }
            },
                function (error) {
                    // Error handling function
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                })
        }

        $scope.showAccessorsPopUp = function (approleName) {
            try {
                $scope.accessorListToDelete = [];
                $scope.isLoadingData = true;
                var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/ss/approle/" + approleName + "/accessors";
                AdminSafesManagement.getAccessorIDs(null, updatedUrlOfEndPoint).then(function (response) {
                    $scope.isLoadingData = false;
                    $scope.approleToShow = approleName;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.appRoleData.accessors = response.data;
                        Modal.createModal('md', 'manageSecretId.html', 'AdminCtrl', $scope);
                    } else if (response.status == 404) {
                        $scope.appRoleData.accessors.keys = [];
                        Modal.createModal('md', 'manageSecretId.html', 'AdminCtrl', $scope);
                    }

                },
                    function (error) {
                        // Error handling function
                        console.log(error);
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    })

            } catch (e) {
                console.log(e);
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');

            }

        }

        $scope.editApprole = function (approleName, size) {
            $scope.approleConfPopupObj.role_name = "";
            $scope.approleConfPopupObj.token_max_ttl = "";
            $scope.approleConfPopupObj.token_ttl = "";
            $scope.approleConfPopupObj.secret_id_num_uses = "";
            $scope.approleConfPopupObj.secret_id_ttl = "";
            $scope.approleConfPopupObj.token_num_uses = "";
            $scope.approleConfPopupObj.bind_secret_id = "";
            $scope.approleConfPopupObj.policies = [];
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getApproleDetails', approleName);
            AdminSafesManagement.getApproleDetails(null, updatedUrlOfEndPoint).then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    var data = response.data.data;
                    $scope.approleConfPopupObj.role_name = approleName;
                    $scope.approleConfPopupObj.token_max_ttl = data.token_max_ttl;
                    $scope.approleConfPopupObj.token_ttl = data.token_ttl;
                    $scope.approleConfPopupObj.secret_id_num_uses = data.secret_id_num_uses;
                    var policy_array = data.policies;
                    var policies = [];
                    if (policy_array != undefined && policy_array != null) {
                        for (var index = 0; index < policy_array.length; index++) {
                            var policyName = policy_array[index].split("_", -1);
                            if (policyName.length >= 3) {
                                policies.push(policyName.slice(2, policyName.length).join("_"));
                            } else {
                                policies.push(policyName);
                            }
                        }
                    }
                    $scope.approleConfPopupObj.policies = policies;
                    // $scope.approleConfPopupObj.policies = data.policies;
                    $scope.approleConfPopupObj.secret_id_ttl = data.secret_id_ttl;
                    $scope.approleConfPopupObj.token_num_uses = data.token_num_uses;
                    $scope.approleConfPopupObj.bind_secret_id = data.bind_secret_id;
                    $scope.roleNameSelected = true;
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
                })
            $scope.editingApprole = { "status": true };
            $scope.openApprole(size);
        }

        $scope.checkRoleExists = function () {
            $scope.rolenameExists = false;
            if ($scope.appRoleData.keys.includes($scope.approleConfPopupObj.role_name)) {
                $scope.rolenameExists = true;
            }
        }

        $scope.collapseNote = function () {
            $scope.isCollapsed = !$scope.isCollapsed;
        }

        $scope.transferOwnerPopUp = function (svcaccname) {
            $scope.svcaccToTransfer = svcaccname;
            Modal.createModal('md', 'transferSvcaccPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.transferSuccessPopUp = function (svcaccname) {
            Modal.createModal('md', 'transferSuccessPopUp.html', 'AdminCtrl', $scope);
        };


        $scope.certificateCreationPopUp = function (svcaccname) {
            Modal.createModal('md', 'certificateCreationPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.certificateCreationFailedPopUp = function (svcaccname) {
            Modal.createModal('md', 'certificateCreationFailedPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.transferFailedPopUp = function (svcaccname) {
            Modal.createModal('md', 'transferFailedPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.transferSvcacc = function (svcaccToTransfer) {
            $scope.transferFailedMessage = '';
            $scope.isLoadingData = true;
            Modal.close();
            Notifications.toast('Transferring Service account owner. Please wait..');
            var queryParameters = "serviceAccountName=" + svcaccToTransfer;
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('transferSvcaccOwner', queryParameters);
            AdminSafesManagement.transferSvcaccOwner(null, updatedUrlOfEndPoint).then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isLoadingData = false;
                    $scope.svcaccToTransfer = '';
                    $scope.transferFailedMessage = response.data.messages[0];
                    $scope.transferSuccessPopUp();
                }
                else {
                    $scope.isLoadingData = false;
                    $scope.svcaccToTransfer = '';
                    $scope.transferFailedMessage = response.data.messages[0];
                    $scope.transferFailedPopUp();
                }
            },
                function (error) {
                    // Error handling function
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.svcaccToTransfer = '';
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                });
        }

        $scope.newCertificateConfiguration = function (size) {
            $scope.cert = {
                "targetSystem": '',
                "targetSystemService": ''
            }
            Modal.createModal(size, 'certificatePopup.html', 'AdminCtrl', $scope);
        }

        $scope.certObj = {
            'sslcertType': 'PRIVATE_SINGLE_SAN',
            'certName': '',
            'targetSystem': $scope.targetSystem,
            'targetSystemServiceRequest': $scope.domainName
        }

        $scope.targetSystem = {
            'description': '',
            'address': '',
            'targetSystemID': '',
            'name': ''
        }

        $scope.targetSystemServiceRequest = {
            'description': '',
            'hostname': '',
            'monitoringEnabled': '',
            'multiIpMonitoringEnabled': '',
            'name': '',
            'port': ''
        }

        $scope.replaceSpacesCertName = function () {
            if ($scope.certObj.certDetails.certName !== null && $scope.certObj.certDetails.certName !== undefined) {
                $scope.certObj.certDetails.certName = $scope.certObj.certDetails.certName.replace(/[ ]/g, '');
                return $scope.certificatePatternValidation();
            }
        }

        $scope.certificatePatternValidation = function () {
            $scope.certNameErrorMessage = '';
            $scope.certInValid = false;
            if ($scope.certObj.certDetails.certName != null && $scope.certObj.certDetails.certName != undefined
                && $scope.certObj.certDetails.certName != "") {
                var reg = new RegExp("^[a-zA-Z0-9.-]+$")
                if (!reg.test($scope.certObj.certDetails.certName)) {
                    $scope.certNameErrorMessage = "Certificate Name can have alphabets, numbers, . and - characters only."
                    $scope.certInValid = true;
                } else {
                    var certName = $scope.certObj.certDetails.certName.toLowerCase();
                    if (!certName.endsWith(".t-mobile.com")) {
                        $scope.certNameErrorMessage = "Certificate name should end with .t-mobile.com"
                        $scope.certInValid = true;
                    }

                }
            }
        }

        $scope.targetAddrPatternValidation = function () {
            $scope.targetAddrErrorMessage = '';
            $scope.addrInValid = false;
            if ($scope.certObj.targetSystem.address != null && $scope.certObj.targetSystem.address != undefined) {
                var reg = new RegExp("^[a-zA-Z0-9.-]+$")
                if (!reg.test($scope.certObj.targetSystem.address)) {
                    $scope.targetAddrErrorMessage = "IP Address can have alphabets, numbers, . and - characters only."
                    $scope.addrInValid = true;
                }
            }
        }

        $scope.hostNamePatternValidation = function () {
            $scope.hostNameErrorMessage = '';
            $scope.hostNameInValid = false;
            if ($scope.certObj.targetSystemServiceRequest.hostname != null && $scope.certObj.targetSystemServiceRequest.hostname != undefined &&  $scope.certObj.targetSystemServiceRequest.hostname != "") {
                var reg = new RegExp("^[a-zA-Z0-9.-]+$")
                if (!reg.test($scope.certObj.targetSystemServiceRequest.hostname)) {
                    $scope.hostNameErrorMessage = "HostName can have alphabets, numbers, . and - characters only."
                    $scope.hostNameInValid = true;
                }
            } 
        }

        $scope.replaceSpacesTargetAddr = function () {
            if ($scope.certObj.targetSystem.address !== null && $scope.certObj.targetSystem.address !== undefined) {
                $scope.certObj.targetSystem.address = $scope.certObj.targetSystem.address.replace(/[ ]/g, '');
                return $scope.targetAddrPatternValidation();
            }
        }
        $scope.replaceSpacesHostName = function () {
            if ($scope.certObj.targetSystemServiceRequest.hostname !== null && $scope.certObj.targetSystemServiceRequest.hostname !== undefined) {
                $scope.certObj.targetSystemServiceRequest.hostname = $scope.certObj.targetSystemServiceRequest.hostname.replace(/[ ]/g, '');
                return $scope.hostNamePatternValidation();
            }
        }

        $scope.portNumValidation = function () {
            $scope.portErrorMessage = '';
            $scope.portInValid = false;
            if ($scope.certObj.targetSystemServiceRequest.port !== null && $scope.certObj.targetSystemServiceRequest.port !== undefined) {
                if ($scope.certObj.targetSystemServiceRequest.port < 1 || $scope.certObj.targetSystemServiceRequest.port > 65535) {
                    $scope.portErrorMessage = "Please enter value between 0 and 65536."
                    $scope.portInValid = true;
                    return $scope.portErrorMessage;
                }
            }
        }

        $scope.ownerEmailPatternValidation = function () {
            $scope.ownerEmailErrorMessage = '';
            $scope.ownerEmailInValid = false;
            if ($scope.certObj.certDetails.ownerEmail != null && $scope.certObj.certDetails.ownerEmail != undefined) {
                var reg =  /^[a-zA-Z0-9_%+-]+[.]?[a-zA-Z0-9_%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/;
                if (!reg.test($scope.certObj.certDetails.ownerEmail)) {
                    $scope.ownerEmailErrorMessage = "Please enter a valid email address."
                    $scope.ownerEmailInValid = true;
                }
            } else {
                $scope.ownerEmailInValid = true;
            }
        }

        $scope.replaceSpacesCertOwnerEmail = function(){
            if ($scope.certObj.certDetails.ownerEmail !== null && $scope.certObj.certDetails.ownerEmail !== undefined) {
                $scope.certObj.certDetails.ownerEmail = $scope.certObj.certDetails.ownerEmail.replace(/[ ]/g, '');
                return $scope.ownerEmailPatternValidation();
            }
        }

        $scope.isCreateCertBtnDisabled = function () {
            if ($scope.certObj.targetSystem != undefined
                && $scope.certObj.targetSystem.name != undefined
                && $scope.certObj.targetSystem.address != undefined
                && $scope.certObj.targetSystemServiceRequest != undefined
                && $scope.certObj.targetSystemServiceRequest.name != undefined
                && $scope.certObj.targetSystemServiceRequest.port != undefined
                && $scope.certObj.certDetails.certName != undefined
                && !$scope.certInValid
                && !$scope.addrInValid
                && !$scope.portInValid                
                && !$scope.ownerEmailInValid
                && $scope.certObj.certDetails.certType != undefined
                && $scope.certObj.certDetails.applicationName != undefined
                && $scope.certObj.certDetails.ownerEmail != undefined
                && !$scope.hostNameInValid) {
                return false;
            }
            return true;
        }

        $scope.selectAppName = function (applicationObj) {  
            $scope.applicationName = applicationObj;
            $scope.svcacc.appName = applicationObj.type;
            $scope.appNameSelected = true;
        }

        $scope.getAppName = function (searchName) {
            return orderByFilter(filterFilter($scope.appNameTableOptions, searchName), 'name', true);
        }

        var getWorkloadDetails = function () {
            $scope.isApplicationsLoading = true;
            AdminSafesManagement.getApprolesFromCwm().then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isApplicationsLoading = false;
                    var data = response.data;
                    $scope.appNameTableOptions=[];
                     for (var index = 0;index<data.length;index++) {
                        var value = '';
                        var appTag = '';
                        var appID = '';
                        var name = '';
                        if (data[index].appName !='' && data[index].appName != null && data[index].appName != undefined) {
                            value = data[index].appName;
                            name = value;
                        }
                        if (data[index].appID !='' && data[index].appID != null && data[index].appID != undefined) {
                            appID = data[index].appID;
                        }
                        if (data[index].appTag !='' && data[index].appTag != null && data[index].appTag != undefined) {
                            appTag = data[index].appTag;
                        }
                        value = value + " (AppID: "+ appID + ", AppTag: " + appTag + ")";
                        $scope.appNameTableOptions.push({"type":value, "name": name, "tag": appTag, "id": appID});
                    }
                    if ($scope.certObj.certDetails.applicationName =="" || $scope.certObj.certDetails.applicationName ==null 
                    || $scope.certObj.certDetails.applicationName == undefined) {
                        document.getElementById('applicationName').value = '';
                        document.getElementById('applicationName').placeholder="Search application name";
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
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            })
        }



        $scope.createCert = function () {
            try {
                Modal.close('');
                var targetSystemID = 29;
                var sslcertType = 'PRIVATE_SINGLE_SAN';
                $scope.certObj.sslcertType = sslcertType;
                $scope.certObj.targetSystem.targetSystemID = targetSystemID;
                var reqObjtobeSent =  {                    
                    "sslcertType": $scope.certObj.sslcertType,
                    "targetSystem": $scope.certObj.targetSystem,
                    "targetSystemServiceRequest": $scope.certObj.targetSystemServiceRequest,
                    "appName":$scope.certObj.certDetails.applicationName.tag,
                    "certificateName":$scope.certObj.certDetails.certName,
                    "certType":$scope.certObj.certDetails.certType,
                    "certOwnerEmailId":$scope.certObj.certDetails.ownerEmail

                }
                $scope.certificateCreationMessage = '';
                var url = '';
                $scope.isLoadingData = true;
                AdminSafesManagement.sslCertificateCreation(reqObjtobeSent, url).then(function (response) {

                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.certificateCreationMessage = response.data.messages[0];
                        $scope.certificateCreationPopUp();

                    }
                },
                    function (error) {
                        var errors = error.data.errors;
                        $scope.certificateCreationMessage = errors[0];
                        $scope.certificateCreationFailedPopUp();
                        $scope.isLoadingData = false;
                        console.log(error);
                    })
                $scope.certObj = {};
            } catch (e) {
                $scope.certObj = {};
                $scope.isLoadingData = false;
                console.log(e);
            }
        };

        $scope.cancel = function () {
            $scope.certObj = {};
            Modal.close('');
        };

        $scope.collapseADDetails = function(index) {
            $scope.isCollapsed = !$scope.isCollapsed;
            if(index == 1 ) {
                $scope.isTargetCollapsed = false;
                $scope.isTargetServiceCollapsed = true;
                $scope.isCertCollapsed = true;
            } else if (index == 2 ) {
                $scope.isTargetCollapsed = true;
                $scope.isTargetServiceCollapsed = false;
                $scope.isCertCollapsed = true;
            } else if (index == 3 ) {
                $scope.isTargetCollapsed = true;
                $scope.isTargetServiceCollapsed = true;
                $scope.isCertCollapsed = false;
            } 
        }

        $scope.openExistingTargetSystem = function (e) {
            $scope.existingTargetSystem = true;
            $scope.existingService = true;
            $scope.targetSystemService = { "type": "existing" };
        }
        $scope.openNewTargetSystem = function (e) {
            $scope.existingTargetSystem = false;
            $scope.existingService = false;
            $scope.targetSystemService = { "type": "new" };
            $scope.cert = {
                "targetSystem": '',
                "targetSystemService": ''
            }
        }

        $scope.openExistingService = function () {
            $scope.existingService = true;
        }
        $scope.openNewService = function () {
            $scope.existingService = false;
            $scope.cert = {
                "targetSystemService": ''
            }
        }

        init();

    });
})(angular.module('vault.features.AdminCtrl', [
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications',
    'vault.constants.RestEndpoints'
]));