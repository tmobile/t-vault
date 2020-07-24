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
    app.controller('AdminCtrl', function ($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService, RestEndpoints, filterFilter, orderByFilter, $compile) {

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
        $scope.dnsInvalid = true;
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
        var setTargetSystemServiceList = function (message, data) {
            $scope.serviceListTableOptions = data;
            $scope.dropDownServiceList = {
                'selectedGroupOption': {"type": message},
                'tableOptions': $scope.serviceListTableOptions
            }
        }

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
            $scope.existingTargetSystem = false;
            $scope.existingService = false;
            $scope.certSearchValue = "";
            $scope.certificateData = { "certificates": [] };
            $scope.targetSystemType = { "type": "new" };
            $scope.targetSystemServiceType = { "type": "new" };
            $scope.targetSystemSelected = false;
            $scope.isTargetSystemListLoading = false;
            $scope.existingTargetSystemObj = "";
            $scope.appNameSelected = false;
            $scope.certApplicationName = "";

            $scope.targetSystemServiceSelected = false;
            $scope.serviceListTableOptions = [];
            $scope.userSearchList = [];
            $scope.isUserSearchLoading = false;
            $scope.isOwnerSelected = false;
            setTargetSystemServiceList("No target system selected", []);
            $scope.certificateData.certificates = [];
            $scope.multiSan = [];
            $scope.selectedMultiSan = [];
            $scope.multiSanDnsName = { name:""};

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
            $scope.certObj = {
                'sslcertType': 'PRIVATE_SINGLE_SAN',
                'certDetails': {"certType":"internal"},
                'certName': '',
                'targetSystemType':  { "type": "new" },
                'targetSystemServiceRequestType':  { "type": "new" },
                "targetSystem": $scope.targetSystem,
                "targetSystemService": ''
            }

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

            if ($rootScope.lastVisited == "change-certificate") {
                $scope.selectedIndex = 3;
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
            resetCert();

        };

        var resetCert = function () {
            $scope.targetSystem = {
                'description': undefined,
                'address': undefined,
                'targetSystemID': undefined,
                'name': undefined
            }
            $scope.targetSystemServiceRequest = {
                'description': undefined,
                'hostname': undefined,
                'monitoringEnabled': undefined,
                'multiIpMonitoringEnabled': undefined,
                'name': undefined,
                'port': undefined
            }
            $scope.certObj = {
                'sslcertType': 'PRIVATE_SINGLE_SAN',
                'certDetails': {"certType":"internal"},
                'certName': '',
                'targetSystemType':  { "type": "new" },
                'targetSystemServiceRequestType':  { "type": "new" },
                "targetSystem": $scope.targetSystem,
                "targetSystemService": ''
            }
            $scope.isCertCollapsed = false;
            $scope.isTargetCollapsed = true;
            $scope.isTargetServiceCollapsed = true;
            $scope.existingTargetSystem = false;
            $scope.existingService = false;
            $scope.targetSystemServicesList = [];
            $scope.serviceListTableOptions = [];
            setTargetSystemServiceList("No target system selected", []);
        }

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
            resetCert();
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
            getCertificates("", null, null);
        };

        //Get ssl certificate
        var getCertificates =  function (searchCert, limit, offset) {
            $scope.numOfCertificates = 0;
            $scope.certificatesLoaded = false;
            $scope.certificateData = {"certificates": []};
            $scope.isLoadingData = true;
            $scope.isLoadingCerts = true;
            
            var limitQuery = "";
            var offsetQuery= "";
            if (limit !=null) {
                limitQuery = "&limit="+limit;
            }
            if (offset!=null) {
                offsetQuery= "&offset="+offset;
            }            
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getCertificates',"certificateName="+searchCert + limitQuery + offsetQuery);
            
            AdminSafesManagement.getCertificates(null, updatedUrlOfEndPoint).then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    if(response.data != "" && response.data != undefined) {
                        $scope.certificateData.certificates = response.data.keys;
                        $scope.numOfCertificates = $scope.certificateData.certificates.length;
                        $scope.certificateData.offset = response.data.offset;
                    }
                }
                else {
                    $scope.certificatesLoaded =  true;
                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                    error('md');
                }
                $scope.isLoadingData = false;
                $scope.isLoadingCerts = false;
            },
            function (error) {
                // Error handling function
                console.log(error);
                $scope.isLoadingData = false;
                $scope.certificatesLoaded =  true;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            });            
            $scope.certificatesLoaded =  true; 
        }

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
            	certName = cert.certificateName
            }
            
            return certName;
        }

        $scope.searchCert = function () {
        	if($scope.selectedIndex ==3){
            if ($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2 && $scope.certSearchValue != $scope.searchValue) {                
            	$scope.certSearchValue = $scope.searchValue;
                getCertificates($scope.certSearchValue, null, null);
            }
            if($scope.certSearchValue != $scope.searchValue && $scope.searchValue != undefined && $scope.searchValue.length ==1) {            	            	
                $scope.certSearchValue = $scope.searchValue;                
            }
            if($scope.certSearchValue != $scope.searchValue) {            	
                $scope.certSearchValue = $scope.searchValue;
                getCertificates("", null, null);
            }
        }
        }

        $scope.showMoreCert = function () {
            var offset = $scope.certificateData.offset;
            var limit = $scope.certificateData.limit;
            getCertificates($scope.certSearchValue, limit, limit + offset);
        }

        $scope.getTargetSystems = function () {
            $scope.targetSystemList = [];
            $scope.targetSystemSelected = false;
            $scope.showInputLoader.show = true;
            $scope.isTargetSystemListLoading = true;
            return AdminSafesManagement.getTargetSystems().then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.targetSystemList = response.data.data;
                    $scope.showInputLoader.show = false;
                    $scope.targetSystemSelected = true;
                }
                else {
                    $scope.showInputLoader.show = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }
                $scope.isTargetSystemListLoading = false;
            },
            function (error) {
                // Error handling function
                console.log(error);
                $scope.showInputLoader.show = false;
                $scope.isTargetSystemListLoading = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            });
        }

        $scope.searchTargetSystems = function (searchVal) {
            if ($scope.targetSystemList.length > 0 && searchVal.length > 2) {
                $scope.certObj.targetSystemServiceRequest = undefined;
                $scope.serviceListTableOptions = [];
                $scope.targetSystemSelected = false;
                return orderByFilter(filterFilter($scope.targetSystemList, searchVal), 'name', true);
            }
        }

        $scope.getTargetSystemService = function () {
            $scope.targetSystemServicesList = [];
            setTargetSystemServiceList("Loading services..", []);            
            var currentServicesList = [];
            $scope.isLoadingserviceData = true;
            $scope.targetSystemServiceSelected = false;
            if ($scope.targetSystemSelected == true) {
                var targetSystemId = $scope.certObj.targetSystem.targetSystemID;
                $scope.showServiceInputLoader.show = true;
                var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/sslcert/targetsystems/" + targetSystemId + "/targetsystemservices";
                return AdminSafesManagement.getTargetSystemsServices(null, updatedUrlOfEndPoint).then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.targetSystemServicesList = response.data.data;

                        for (var index = 0;index<$scope.targetSystemServicesList.length;index++) {
                            currentServicesList.push({"type":$scope.targetSystemServicesList[index].name, "index":index});
                        }
                        if ($scope.targetSystemSelected == true) {
                            if (currentServicesList.length >0) {
                                setTargetSystemServiceList("Select service", currentServicesList);
                            }
                            else {
                                setTargetSystemServiceList("No service available", []);
                            }
                        }
                        else {
                            setTargetSystemServiceList("No target system selected", []);
                        }
                        $scope.showServiceInputLoader.show = false;
                        $scope.isLoadingserviceData=false;
                        $scope.targetSystemServiceValidation();
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

        $scope.searchTargetSystemService = function (searchVal) {
            if ($scope.targetSystemServicesList.length > 0 && searchVal.length > 2) {
                $scope.targetSystemServiceSelected = false;
                return orderByFilter(filterFilter($scope.targetSystemServicesList, searchVal), 'name', true);
            }
        }

        $scope.selectTargetSystem = function (targetSystem) {
            $scope.certObj.targetSystem = {
                "name": targetSystem.name,
                "description": targetSystem.description,
                "address": targetSystem.address,
                "targetSystemID": targetSystem.targetSystemID
            }
            $scope.targetSystemSelected = true;
            $scope.getTargetSystemService();
        }

        $scope.selectTargetService = function () {
            var index = $scope.dropDownServiceList.selectedGroupOption.index;
            $scope.certObj.targetSystemServiceRequest = {
                "name": $scope.targetSystemServicesList[index].name,
                "description": $scope.targetSystemServicesList[index].description,
                "port": $scope.targetSystemServicesList[index].port,
                "hostname": $scope.targetSystemServicesList[index].hostname,
                "monitoringEnabled": $scope.targetSystemServicesList[index].monitoringEnabled,
                "multiIpMonitoringEnabled": $scope.targetSystemServicesList[index].multiIpMonitoringEnabled,
            };
            $scope.targetSystemServiceSelected = true;
        }

        $scope.selectTargetSystemService = function (targetSystemService) {
            // for live search
            $scope.certObj.targetSystemServiceRequest = targetSystemService;
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
        var certpagesShown = 1;
        var certpageSize = 50;

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
                    
        $scope.certpaginationLimit = function (data) {
            $scope.certcurrentshown = certpageSize * certpagesShown;
            if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.certcurrentshown >= $scope.numOfCertificates) {
                $scope.certcurrentshown = $scope.numOfCertificates;
            }
            return $scope.certcurrentshown;
        };
        $scope.hasMoreCertsToShow = function () {        	
                return certpagesShown < ($scope.numOfCertificates / certpageSize);
            
        };
        $scope.showMoreCertItems = function () {
        	certpagesShown = certpagesShown + 1;
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

        $scope.revocationPopUp = function (svcaccname) {
            Modal.createModal('md', 'revocationPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.certificateCreationFailedPopUp = function (svcaccname) {
            Modal.createModal('md', 'certificateCreationFailedPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.revokeReasonsPopUp = function (svcaccname) {
            Modal.createModal('md', 'revokeReasonsPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.transferFailedPopUp = function (svcaccname) {
            Modal.createModal('md', 'transferFailedPopUp.html', 'AdminCtrl', $scope);
        };
        
        $scope.renewCertificatePopUp = function (svcaccname) {
            Modal.createModal('md', 'renewCertificatePopUp.html', 'AdminCtrl', $scope);
        };
        
        $scope.renewCertificateFailedPopUp = function (svcaccname) {
            Modal.createModal('md', 'renewCertificateFailedPopUp.html', 'AdminCtrl', $scope);
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

            $scope.hostNameErrorMessage = '';
            $scope.certNameErrorMessage = '';
            $scope.targetSysErrorMessage = '';
            $scope.targetSysServiceErrorMessage = '';
            $scope.targetAddrErrorMessage = '';
            $scope.portErrorMessage = '';
            $scope.ownerEmailErrorMessage='';
            Modal.createModal(size, 'certificatePopup.html', 'AdminCtrl', $scope);
            $scope.targetSystemType = { "type": "new" };
            $scope.targetSystemServiceType = { "type": "new" };
            $scope.getTargetSystems();
        }

        $scope.replaceSpacesCertName = function () {
            if ($scope.certObj.certDetails.certName !== null && $scope.certObj.certDetails.certName !== undefined) {
                $scope.certObj.certDetails.certName = $scope.certObj.certDetails.certName.toLowerCase();
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
                    }  else if ( (certName.includes(".-")) || (certName.includes("-."))){
                        $scope.certNameErrorMessage = "Please enter a valid certificate name"
                        $scope.certInValid = true;
                    }

                }
            }
        }

        $scope.targetAddrPatternValidation = function () {
            $scope.targetAddrErrorMessage = "";
            $scope.addrInValid = false;
            if ($scope.certObj.targetSystem.address != null && $scope.certObj.targetSystem.address != undefined
                && $scope.certObj.targetSystem.address != "") {
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
            if ($scope.certObj.targetSystemServiceRequest.hostname != null && $scope.certObj.targetSystemServiceRequest.hostname != undefined 
                && $scope.certObj.targetSystemServiceRequest.hostname != "") {
                var reg = new RegExp("^[a-zA-Z0-9.-]+$")
                if (!reg.test($scope.certObj.targetSystemServiceRequest.hostname)) {
                    $scope.hostNameErrorMessage = "Hostname can have alphabets, numbers, . and - characters only."
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
        
        $scope.targetSystemAvailable = function(){        	
        	if($scope.targetSystemType.type=="new"){        		
        		return $scope.targetSystemValidation();
        	}
        	if($scope.targetSystemType.type=="existing"){        		
        		$scope.targetSystemIsAvailable = false;
        	} 
            }
        
        $scope.targetSystemValidation = function(){  
        	$scope.targetSysErrorMessage="";        	
        	$scope.targetSystemIsAvailable = false;
        	var targetSysName = $scope.certObj.targetSystem.name;       	
        	angular.forEach($scope.targetSystemList, function(item){                 
                if(item.name == targetSysName){
                	$scope.targetSysErrorMessage="Entered target system is available";
                	$scope.targetSystemIsAvailable = true;
                }
            })              
            }
        
        $scope.targetSystemServiceAvailable = function(){
        	if($scope.targetSystemServiceType.type=="new"){
        		return $scope.targetSystemServiceValidation();
        	}      	
            }
        
        $scope.targetSystemServiceValidation = function(){  
        	$scope.targetSysServiceErrorMessage="";
        	var targetSysServiceName = "";
        	
        	$scope.targetSystemServiceIsAvailable = false;
        	if($scope.certObj.targetSystemServiceRequest!=undefined){
        	targetSysServiceName = $scope.certObj.targetSystemServiceRequest.name;
        	}       	
        	if($scope.targetSystemServicesList !=null) {   	
        	angular.forEach($scope.targetSystemServicesList, function(item){
                if(item.name == targetSysServiceName){
                	$scope.targetSysServiceErrorMessage="Entered target system service is available";
                	$scope.targetSystemServiceIsAvailable = true;                	
                }                
            })              
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

        $scope.isCreateCertBtnDisabled = function () {         	
            if ($scope.certObj.targetSystem != undefined
                && $scope.certObj.targetSystem.name != undefined
                && $scope.certObj.targetSystem.address != undefined
                && $scope.certObj.targetSystem.address != ""
                && $scope.certObj.targetSystemServiceRequest != undefined
                && $scope.certObj.targetSystemServiceRequest.name != undefined
                && $scope.certObj.targetSystemServiceRequest.port != undefined
                && $scope.certObj.certDetails.certName != undefined
	        	&& $scope.certObj.certDetails.certName != ""
                && !$scope.certInValid
                && !$scope.addrInValid
                && !$scope.portInValid                
                && !$scope.ownerEmailInValid
                && $scope.certObj.certDetails.certType != undefined
                && $scope.certObj.certDetails.applicationName != undefined
                && $scope.isOwnerSelected == true
                && !$scope.targetSystemIsAvailable 
                && !$scope.targetSystemServiceIsAvailable
                && !$scope.hostNameInValid) {
                return false;
            }
            return true;
        }
        
        $scope.selectAppName = function (applicationObj) {  
            $scope.certObj.certDetails.applicationName = applicationObj.tag;
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
                       
                        $scope.appNameTableOptions.push({"type":value, "name": name, "tag": appTag, "id": appID});
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
            	$scope.targetSystemServiceValidation();
                Modal.close('');
                var sslcertType = 'PRIVATE_SINGLE_SAN';
                $scope.appNameTagValue=$scope.certObj.certDetails.applicationName;
                $scope.certObj.sslcertType = sslcertType;
                var multiSanDns = [];
                $scope.multiSan.forEach(function (dns) {
                    multiSanDns.push(dns.name);
                });
                var reqObjtobeSent =  {
                    "sslcertType": $scope.certObj.sslcertType,
                    "targetSystem": $scope.certObj.targetSystem,
                    "targetSystemServiceRequest": $scope.certObj.targetSystemServiceRequest,
                    "appName": $scope.appNameTagValue,
                    "certificateName":$scope.certObj.certDetails.certName,
                    "certType":$scope.certObj.certDetails.certType,
                    "certOwnerEmailId":$scope.certObj.certDetails.ownerEmail,
                    "certOwnerNTId":$scope.certObj.certDetails.ownerNtId,
                    "multiSan": multiSanDns
                }
                $scope.certificateCreationMessage = '';
                var url = '';
                $scope.isLoadingData = true;
                AdminSafesManagement.sslCertificateCreation(reqObjtobeSent, url).then(function (response) {

                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.certificateCreationMessage = response.data.messages[0];
                        resetCert();
                        $scope.certificateCreationPopUp();
                        $scope.searchValue = '';
                    }
                },
                function (error) {
                    resetCert();
                    var errors = error.data.errors;
                    $scope.certificateCreationMessage = errors[0];
                    $scope.certificateCreationFailedPopUp();
                    $scope.isLoadingData = false;
                    console.log(error);
                    $scope.searchValue = '';
                })
            } catch (e) {
                resetCert();
                $scope.isLoadingData = false;
                console.log(e);
                $scope.searchValue = '';
            }

            resetCert();            
        };

        $scope.cancel = function () {
            Modal.close('');
        };

        $scope.successCancel = function () {
            Modal.close('');
            getCertificates("", null, null);
        };

        $scope.collapseCertDetails = function (index) {
            if(index == 1 ) {
                $scope.isTargetCollapsed = false;
                $scope.isTargetServiceCollapsed = true;
                $scope.isCertCollapsed = true;
            } else if (index == 2 ) {
                $scope.isTargetCollapsed = true;
                $scope.isTargetServiceCollapsed = false;
                $scope.isCertCollapsed = true;
                $scope.targetSystemServiceType = { "type": "new" };
                $scope.existingService = false;
                clearTargetSystemServiceFields();
                if ($scope.serviceListTableOptions.length >0) {
                    setTargetSystemServiceList("Select service", $scope.serviceListTableOptions);
                }
                else {
                    if ($scope.showServiceInputLoader.show == true) {
                        setTargetSystemServiceList("Loading services..", []);
                    }
                    else {
                        if ($scope.targetSystemSelected == false) {
                            setTargetSystemServiceList("No target system selected", []);
                        }
                        else {
                            setTargetSystemServiceList("No service available", []);
                        }
                    }
                }

            } else if (index == 3 ) {
                $scope.isTargetCollapsed = true;
                $scope.isTargetServiceCollapsed = true;
                $scope.isCertCollapsed = false;
            } 

        }

        $scope.collapseADDetails = function() {
            $scope.isCollapsed = !$scope.isCollapsed;
        }


        //Revoke Certificate

        $scope.revokeCertificate = function (certificateDetails){
            try{
            $scope.isLoadingData = true;
            $scope.revocationMessage = '';
            $scope.revocationStatusMessage = '';
            var certificateName = $scope.getCertSubjectName(certificateDetails);
            $scope.certificateNameForRevoke = certificateName;
            var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/certificates/" + certificateDetails.certificateId + "/revocationreasons";
            $scope.revocationReasons = [];
            AdminSafesManagement.getRevocationReasons(null, updatedUrlOfEndPoint).then(function (response) {
                $scope.isLoadingData = false;
                if (UtilityService.ifAPIRequestSuccessful(response)) { 
                    for (var index = 0;index<response.data.reasons.length;index++) {
                        $scope.revocationReasons.push({"type":response.data.reasons[index].displayName,
                        "value":response.data.reasons[index].reason});
                    } 
                    $scope.revokeReasonsPopUp();
                    $scope.searchValue = '';                             
                }
            },
            function (error) {
                // Error handling function
                $scope.isLoadingData = false;
                $scope.revocationMessage = error.data.errors[0];
                $scope.revocationStatusMessage = "Revocation Reasons Failed!";
                $scope.revocationPopUp();
                console.log(error);
                $scope.searchValue = '';
            })
            }catch (e) {
                $scope.isLoadingData = false;
                console.log(e);
                $scope.searchValue = '';
            };

            $scope.dropdownRevocationReasons = {
                'selectedGroupOption': {"type": "Select Revocation Reasons","value":"Revocation Values"},       // As initial placeholder
                'tableOptions': $scope.revocationReasons
            }
            Modal.close('');            
        };

        $scope.revocationReasonSelect = function(){
           $scope.dropdownRevocationReasons.selectedGroupOption.type;
        }

        $scope.revoke = function(){
            try {
                $scope.revocationMessage = ''
                if ($scope.dropdownRevocationReasons.selectedGroupOption.type == 'Select Revocation Reasons') {
                    $scope.revocationStatusMessage = 'Revocation Failed!';
                    $scope.revocationMessage = "Select Revocation Reasons";
                    return $scope.revocationPopUp();
                }
                Modal.close('');
                var reqObjtobeSent =  {                    
                    "reason": $scope.dropdownRevocationReasons.selectedGroupOption.value
                }
               
                var url = RestEndpoints.baseURL + "/v2/certificates/" + $scope.certificateNameForRevoke + "/revocationrequest";
                $scope.isLoadingData = true;
                AdminSafesManagement.issueRevocationRequest(reqObjtobeSent, url).then(function (response) {

                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.revocationStatusMessage = 'Revocation Successfull!';
                        $scope.revocationMessage = response.data.messages[0];
                        $scope.revocationPopUp();
                        $scope.requestDataFrAdmin();
                    }
                },
                    function (error) {
                        var errors = error.data.errors;
                        $scope.revocationStatusMessage = 'Revocation Failed!';
                        if (errors[0] == "Access denied: no permission to revoke certificate") {
                            $scope.revocationMessage = "For security reasons, you need to log out and log in again for the permissions to take effect.";
                        } else {
                            $scope.revocationMessage = errors[0];
                        }
                        $scope.revocationPopUp();
                        $scope.isLoadingData = false;
                        console.log(error);
                    })
                
            } catch (e) {
                $scope.isLoadingData = false;
                console.log(e);
            }

        }

        $scope.cancelRevoke = function(){
            try{
                Modal.close('');
            }catch (e){
                console.log(e);
            }   
        }

        $scope.openExistingTargetSystem = function (e) {
            $scope.existingTargetSystem = true;
            $scope.existingService = true;
            $scope.existingService = false;  
            $scope.targetSystemIsAvailable = false;
            $scope.targetSysErrorMessage = '';
            if(angular.isDefined($scope.certObj.targetSystem) && $scope.certObj.targetSystem != null && typeof $scope.certObj.targetSystem == 'object'){
                $scope.certObj.targetSystem.name=undefined;
                $scope.certObj.targetSystem.description=undefined;
                $scope.certObj.targetSystem.address=undefined;
            }
            else {
                $scope.certObj.targetSystem = "";
            }
            setTargetSystemServiceList("No target system selected", []);
            $scope.targetSystemSelected = false;
        }

        $scope.openNewTargetSystem = function (e) {
            $scope.existingTargetSystem = false;
            $scope.existingService = false;
            $scope.targetSystemType = { "type": "new" };
            $scope.targetSystemServiceType = { "type": "new" };

            if($scope.targetSystemSelected && angular.isDefined($scope.certObj.targetSystem)){
                $scope.certObj.targetSystem.name=undefined;
                $scope.certObj.targetSystem.description=undefined;
                $scope.certObj.targetSystem.address=undefined;
            }
            setTargetSystemServiceList("No target system selected", []);
            $scope.targetSystemSelected = false;
        }

        $scope.openExistingService = function () {
            $scope.existingService = true;
            $scope.targetSystemServiceIsAvailable = false;
            $scope.targetSysServiceErrorMessage="";
            $scope.certObj.targetSystemServiceRequest.name = undefined
            }

        var clearTargetSystemServiceFields = function () {
            if(angular.isDefined($scope.certObj.targetSystemServiceRequest) && $scope.certObj.targetSystemServiceRequest != null && typeof $scope.certObj.targetSystemServiceRequest == 'object'){  
                $scope.certObj.targetSystemServiceRequest.name=undefined;
                $scope.certObj.targetSystemServiceRequest.description=undefined;
                $scope.certObj.targetSystemServiceRequest.port=undefined;
                $scope.certObj.targetSystemServiceRequest.hostname=undefined;
                $scope.certObj.targetSystemServiceRequest.monitoringEnabled=undefined;
                $scope.certObj.targetSystemServiceRequest.multiIpMonitoringEnabled=undefined;
                $scope.targetSysServiceErrorMessage="";
            }
            else {
                $scope.targetSystemServiceRequest = {
                    'description': undefined,
                    'hostname': undefined,
                    'monitoringEnabled': undefined,
                    'multiIpMonitoringEnabled': undefined,
                    'name': undefined,
                    'port': undefined
                }
            }
        }

        $scope.openNewService = function () {
            $scope.existingService = false;
            $scope.targetSystemServiceSelected = false;

            clearTargetSystemServiceFields();
            if ($scope.serviceListTableOptions.length >0) {
                setTargetSystemServiceList("Select service", $scope.serviceListTableOptions);
            }
            else {
                if ($scope.showServiceInputLoader.show == true) {
                    setTargetSystemServiceList("Loading services..", []);
                }
                else {
                    if ($scope.targetSystemSelected == false) {
                        setTargetSystemServiceList("No target system selected", []);
                    }
                    else {
                        setTargetSystemServiceList("No service available", []);
                    }
                }
            }
        }

        $scope.searchEmail = function (searchVal) {
            if (searchVal.length > 2) {
                $scope.isUserSearchLoading = true;
                searchVal = searchVal.toLowerCase();
                try {
                    $scope.userSearchList = [];

                    var queryParameters = $scope.certObj.certDetails.ownerEmail;
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('usersGetData', queryParameters);
                    return AdminSafesManagement.usersGetData(null, updatedUrlOfEndPoint).then(
                        function(response) {
                            $scope.isUserSearchLoading = false;
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                var filterdUserData = [];
                                $scope.userSearchList = response.data.data.values;
                                $scope.userSearchList.forEach(function (userData) {
                                    if (userData.userEmail != null && userData.userEmail.substring(0, searchVal.length).toLowerCase() == searchVal) {
                                        filterdUserData.push(userData);
                                    }
                                });
                                return orderByFilter(filterFilter(filterdUserData, searchVal), 'userEmail', true);
                            } else {
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                $scope.error('md');
                            }
                        },
                        function(error) {
                            // Error handling function
                            console.log(error);
                            $scope.isUserSearchLoading = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                    });
                } catch (e) {
                    console.log(e);
                    $scope.isUserSearchLoading = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }
            }
        }

        $scope.selectOwner = function (ownerEmail) {
            if (ownerEmail != null) {
                $scope.certObj.certDetails.ownerEmail = ownerEmail.userEmail;
                $scope.certObj.certDetails.ownerNtId = ownerEmail.userName;
                $scope.isOwnerSelected = true;
            }
        }

        $scope.clearOwnerEmail = function () {
            $scope.certObj.certDetails.ownerEmail = "";
            $scope.certObj.certDetails.ownerNtId = "";
            $scope.isOwnerSelected = false;
        }

        $scope.goToAddPermissions = function (certificateDetails) {            
            var obj = "certificateObject";
            var myobj = certificateDetails;
            var fullObj = {};
            fullObj[obj] = myobj;
            try {       
                $scope.isLoadingData = true;
                $scope.ispermissionData = true;               // To show the 'permissions' and hide the 'details'
                $scope.UsersPermissionsData = [];
                $state.go('change-certificate', fullObj);
                $scope.isLoadingData = false;
            } catch (e) {  
                $scope.isLoadingData = false;              
                console.log(e);
            }
        };
        
        $scope.renewCertPopup = function (certDetails) {
            $scope.fetchDataError = false;
            $rootScope.certDetails = certDetails;
            Modal.createModal('md', 'renewCertPopup.html', 'AdminCtrl', $scope);
        };
        
         $rootScope.renewCertificate = function(certificateDetails){      	
                
               	if ($rootScope.certDetails !== null && $rootScope.certDetails !== undefined) {
               		certificateDetails = $rootScope.certDetails;
                  }
                $rootScope.certDetails = null;                
                try{
                $scope.isLoadingData = true;
                Modal.close();                
                $scope.renewMessage = '';
                var certificateName = $scope.getCertSubjectName(certificateDetails);
                $scope.certificateNameForRenew = certificateName;          
                var url = RestEndpoints.baseURL + "/v2/certificates/" + certificateName + "/renew";
                $scope.isLoadingData = true;                              
                
                AdminSafesManagement.renewCertificate(null, url).then(function (response) {
                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.renewMessage = 'Certificate Renewed Successfully!';
                        $scope.renewMessage = response.data.messages[0];     
                        $scope.renewCertificatePopUp();
                        $scope.requestDataFrAdmin();
                        $scope.searchValue = '';
                    }
                },
                    function (error) {
                        var errors = error.data.errors;
                        $scope.renewMessage = 'Renew Failed';                        
                        if (errors[0] == "Access denied: No permission to renew certificate") {
                            $scope.renewMessage = "For security reasons, you need to log out and log in again for the permissions to take effect.";
                        } else {
                            $scope.renewMessage = errors[0];
                        } 
                        $scope.renewCertificateFailedPopUp();
                        $scope.isLoadingData = false;
                        console.log(error);
                        $scope.searchValue = '';
                    })
                }catch (e) {
                    $scope.isLoadingData = false;
                    console.log(e);
                    $scope.searchValue = '';
                };               
                
            };

            var clearSearchBox =  function () {
            	$scope.searchValue = '';
            }
            
            $scope.revocationReasonSelect = function(){
               $scope.dropdownRevocationReasons.selectedGroupOption.type;
            }

            var isDuplicateDns = function (multiSanDnsName) {
                $scope.certDnsErrorMessage = '';
                for (var i=0;i<$scope.multiSan.length;i++) {
                    if (multiSanDnsName == $scope.multiSan[i].name) {
                        $scope.certDnsErrorMessage = 'Duplicate DNS';
                        return true;
                    }
                }
                return false;
            }

            $scope.addDns = function (multiSanDnsName) {
                var length = $scope.multiSan.length;
                if (multiSanDnsName && multiSanDnsName.name!="") {
                    var id="dns"+length;
                    angular.element('#dnslist').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-8 col-md-8 col-sm-8 col-xs-8 ng-binding dns-name">'+multiSanDnsName.name+'</div><div class="container radio-inputs col-lg-4 col-md-4 col-sm-4 col-xs-4 dns-delete"><div class="down"><div ng-click="deleteDns(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                    $scope.multiSan.push({ "id": length, "name":multiSanDnsName.name});
                    $scope.multiSanDnsName.name = "";
                    $scope.dnsInvalid = true;
                }
            }

            $scope.deleteDns = function (id) {
                var dnsElement = angular.element( document.querySelector( '#'+id ) );
                dnsElement.remove();
                var index = id.substring(3);
                $scope.selectedMultiSan = [];
                for (var i=0;i<$scope.multiSan.length;i++) {
                    if (index != $scope.multiSan[i].id) {
                        $scope.selectedMultiSan.push($scope.multiSan[i]);
                    }
                }
                $scope.multiSan = $scope.selectedMultiSan;
            }

            $scope.replaceSpacesDnsName = function () {
                if ($scope.multiSanDnsName.name !== null && $scope.multiSanDnsName.name !== undefined) {
                    $scope.multiSanDnsName.name = $scope.multiSanDnsName.name.toLowerCase();
                    $scope.multiSanDnsName.name = $scope.multiSanDnsName.name.replace(/[ ]/g, '');
                    return $scope.dnsPatternValidation();
                }
            }

            $scope.dnsPatternValidation = function () {
                $scope.certDnsErrorMessage = '';
                $scope.dnsInvalid = false;
                if ($scope.multiSanDnsName.name != null && $scope.multiSanDnsName.name != undefined
                    && $scope.multiSanDnsName.name != "") {
                    var reg = new RegExp("^[a-zA-Z0-9.-]+$")
                    if (!reg.test($scope.multiSanDnsName.name)) {
                        $scope.certDnsErrorMessage = "Certificate Name can have alphabets, numbers, . and - characters only."
                        $scope.dnsInvalid = true;
                    } else {
                        var certName = $scope.multiSanDnsName.name.toLowerCase();
                        if (!certName.endsWith(".t-mobile.com")) {
                            $scope.certDnsErrorMessage = "Certificate name should end with .t-mobile.com"
                            $scope.dnsInvalid = true;
                        }  else if ( (certName.includes(".-")) || (certName.includes("-."))){
                            $scope.certDnsErrorMessage = "Please enter a valid certificate name"
                            $scope.dnsInvalid = true;
                        } else if (isDuplicateDns($scope.multiSanDnsName.name)) {
                            $scope.certDnsErrorMessage = "Duplicate DNS"
                            $scope.dnsInvalid = true;
                        }
                    }
                } else {
                    $scope.dnsInvalid = true;
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