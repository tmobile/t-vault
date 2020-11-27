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
    app.controller('AdminCtrl', function ($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService, RestEndpoints, AppConstant, filterFilter, orderByFilter, $compile) {

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
        $scope.isCollapsedCert = true;
        $scope.isCollapsedIAM = true;
        $scope.isCollapsedAzure = true;
        $scope.existingTargetSystem = false;
        $scope.existingService = false;
        $scope.isCertCollapsed = false;
        $scope.isTargetCollapsed = true;
        $scope.isTargetServiceCollapsed = true;
        $scope.dnsInvalid = true;
        $scope.isCertificatePreview = false;
        $scope.isCertificateManagePreview = false;
        $scope.certificateDetails = [];
        $scope.appName = '';
        $scope.isSelfServiceGroupAssigned = true;
        $scope.assignedApplications = [];
        $scope.isExternalCertificateEnable = true;
        $scope.isAppNamesLoading = true;
        $scope.certificatesToOnboard = [];
        $scope.certificateToOnboard = null;
        $scope.isCertificateOnboardPreview = false;
        $scope.notificationEmails = [];
        $scope.selectedNotificationEmails = [];
        $scope.notificationEmail = { email:""};
        $scope.isNotificationEmailSelected = false;
        $scope.notificationEmailErrorMessage = "";
        $scope.applicationNameSelectMsg = "";
        $scope.isNotificationEmailSearch = false;
        $scope.numOfOnboardPendingCertificates = 0;
        $scope.isOffboardingDecommitioned = false;
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
            $scope.ifTargetServiceExisting=false;	
            $scope.ifTargetSystemExisting=false;
            $scope.enableSvcacc = true;
            $scope.enableIamSvcacc = true;
            $scope.enableAzureSvcacc=true;
            $scope.enableSelfService = true;
            $scope.isCollapsed = true;
            $scope.isCollapsedIAM = true;
            $scope.isCollapsedAzure = true;
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
            $scope.isDuplicateNotificationEmail=false;
            $scope.certApplicationName = "";

            $scope.targetSystemServiceSelected = false;
            $scope.serviceListTableOptions = [];
            $scope.userSearchList = [];
            $scope.isUserSearchLoading = false;
            $scope.isOwnerSelected = false;            
            $scope.certificateData.certificates = [];
            $scope.multiSan = [];
            $scope.selectedMultiSan = [];
            $scope.multiSanDnsName = { name:""};
            $scope.isCertificatePreview = false;
            $scope.isCertificateManagePreview = false;
            $scope.isSelfServiceGroupAssigned = true;
            $scope.assignedApplications = [];
            $scope.isExternalCertificateEnable = AppConstant.SSL_EXT_CERTIFICATE;
            $scope.certificatesToOnboard = [];
            $scope.certificateToOnboard = null;
            $scope.isCertificateOnboardPreview = false;
            $scope.notificationEmails = [];
            $scope.selectedNotificationEmails= [];
            $scope.notificationEmail = { email:""};
            $scope.isNotificationEmailSelected = false;
            $scope.notificationEmailErrorMessage = "";
            $scope.applicationNameSelectMsg = "";
            $scope.isNotificationEmailSearch = false;
            $scope.numOfOnboardPendingCertificates = 0;
            $scope.isOffboardingDecommitioned = false;
            $scope.certObj = {
                'sslcertType': 'PRIVATE_SINGLE_SAN',
                'certDetails': {"certType":"internal",},
                'certName': ''
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
            if ($rootScope.lastVisited == "change-iam-service-account") {
                $scope.selectedIndex = 3;
            }
            if ($rootScope.lastVisited == "change-azure-service-principals") {
                $scope.selectedIndex = 4;
            }
            if ($rootScope.lastVisited == "change-certificate") {
                $scope.selectedIndex = 5;
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

            if(SessionStore.getItem("isCertPermission")) {
                $scope.assignedApplications = JSON.parse(SessionStore.getItem("selfServiceAppNames"));
            }else {
                $scope.isSelfServiceGroupAssigned = false;
            }
            getWorkloadDetails();
            $scope.requestDataFrAdmin();
            resetCert();
        };

        var resetCert = function () {
            $scope.certObj = {
                'sslcertType': 'PRIVATE_SINGLE_SAN',
                'certDetails': {"certType":"internal"},
                'certName': ''
            }
            $scope.isDuplicateNotificationEmail=false;
            $scope.appNameSelected = false;
            $scope.isCertCollapsed = false;
            $scope.isTargetCollapsed = true;
            $scope.isTargetServiceCollapsed = true;
            $scope.existingTargetSystem = false;
            $scope.existingService = false;
            $scope.certObj.certDetails.ownerEmail = "";
            $scope.isCertificatePreview = false;
            $scope.isCertificateManagePreview = false;
            if($scope.appNameTableOptions!==undefined){
            	$scope.appNameTableOptionsSort = $scope.appNameTableOptions.sort(function (a, b) {
                    return (a.name > b.name ? 1 : -1);
                });    
                $scope.dropdownApplicationName = {
                        'selectedGroupOption': {"type": "Select Application Name","name":"Application Name"},       // As initial placeholder
                        'tableOptions': $scope.appNameTableOptionsSort
                    }}
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

            //IAM SERVICE ACCOUNT
            if ($scope.enableIamSvcacc == true) {
                $scope.numOfIamSvcaccs = 0;
                $scope.iamSvcaccOnboardedData = { "keys": [] };
                $scope.isLoadingData = true;
                AdminSafesManagement.getOnboardedIamServiceAccounts().then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.iamSvcaccOnboardedData = response.data;
                        $scope.numOfIamSvcaccs = $scope.iamSvcaccOnboardedData.keys.length;
                        $scope.isLoadingData = false;
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
             //AZURE SERVICE ACCOUNT
        if ($scope.enableAzureSvcacc == true) {
            $scope.numOfazureSvcaccs = 0;
            $scope.azureSvcaccOnboardedData = { "keys": [] };
            $scope.isLoadingData = true;
            AdminSafesManagement.getOnboardedAzureServiceAccounts().then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.azureSvcaccOnboardedData = response.data;
                    $scope.numOfAzureSvcaccs = $scope.azureSvcaccOnboardedData.keys.length;
                    $scope.isLoadingData = false;
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
            /*
            if($scope.selectedTab == 1){            	
           	 getCertificates("", null, null,"external");
           }else{        	   
           getCertificates("", null, null,"internal");
           }
           */
        };

        $scope.getExtCertificates = function () { 
       	 $scope.selectedTab = 1;
       	$scope.isInternalCert = false; 
       	$scope.isExternalCert = true;   
       	if($scope.certSearchValue == ""){
       		getCertificates("", null, null,"external");
       	}else{
       		getCertificates($scope.certSearchValue, null, null,"external");
       	}
       	
       }
       
       $scope.getInternalCertificates = function () { 
       	$scope.isInternalCert = true; 
       	$scope.isExternalCert = false;         	
       	 $scope.selectedTab = 0;
       	 if($scope.certSearchValue == ""){
       		 getCertificates("", null, null,"internal");
       	 }else{
        		getCertificates($scope.certSearchValue, null, null,"internal");
        	}
       }
       
       
        
        //Get ssl certificate
        var getCertificates =  function (searchCert, limit, offset, certType) {
            $scope.numOfCertificates = 0;            
            $scope.certificatesLoaded = false;
            $scope.certificateData = {"certificates": []};
            $scope.isLoadingData = true;
            $scope.isLoadingCerts = true;            
            
            var limitQuery = "";
            var offsetQuery= "";
            var certTypeQuery= "";
            if (limit !=null) {
                limitQuery = "&limit="+limit;
            }
            if (offset!=null) {
                offsetQuery= "&offset="+offset;
            }
            if (certType!=null) {
            	certTypeQuery= "&certType="+certType;
            }             
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getCertificates',"certificateName="+searchCert + limitQuery + offsetQuery+certTypeQuery);
            
            AdminSafesManagement.getCertificates(null, updatedUrlOfEndPoint).then(function (response) {            	

                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    if(response.data != "" && response.data != undefined) {
                        $scope.certificateData.certificates = response.data.keys;
                        $scope.numOfCertificates = $scope.certificateData.certificates.length;
                        $scope.certificateData.offset = response.data.offset;
                        $scope.finalFilterCertResults = $scope.certificateData.certificates;                        
                        $scope.filterCert($scope.searchValue);
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
                $scope.isLoadingCerts = false;
                $scope.certificatesLoaded =  true;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            });            
            $scope.certificatesLoaded =  true; 
        }

        $scope.tabChangeForAdminCert = function () {        	
            $scope.searchValue = '';
            $scope.finalFilterCertResults = $scope.certificateData.certificates;
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
        $scope.getCertExpirationDate = function (cert) {
            var expiryDate = "";
            if (cert.subjectAltName && cert.subjectAltName.dns && cert.subjectAltName.dns.length > 0) {
                expiryDate = cert.subjectAltName.dns[1];
            }
            if (expiryDate == "" || expiryDate == undefined) {
            	expiryDate = cert.expiryDate
            }
            
            return expiryDate;
        }
        $scope.searchCert = function () {
            if($scope.selectedIndex ==4){
            if ($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2 && $scope.certSearchValue != $scope.searchValue) {                
            	$scope.certSearchValue = $scope.searchValue;
            	if($scope.selectedTab == 1){
                    getCertificates($scope.certSearchValue, null, null,"external");
                	}else {
                		getCertificates($scope.certSearchValue, null, null,"internal");
                	}
            }
            if($scope.certSearchValue != $scope.searchValue && $scope.searchValue != undefined && $scope.searchValue.length ==1) {            	            	
                $scope.certSearchValue = $scope.searchValue;                
            }
            if($scope.certSearchValue != $scope.searchValue) {            	
                $scope.certSearchValue = $scope.searchValue;
                if($scope.selectedTab == 1){
                    getCertificates("", null, null,"external");
                	}else {
                		getCertificates("", null, null,"internal");
                	}
            }
        }
        }

        $scope.showMoreCert = function () {
            var offset = $scope.certificateData.offset;
            var limit = $scope.certificateData.limit;
            getCertificates($scope.certSearchValue, limit, limit + offset,"internal");
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

        $scope.editOnboardedIamSvcacc = function (userId, size) {
            var obj = "svcaccData";
            var fullObj = {};
            fullObj[obj] = { "userId": userId };
            $state.go('change-iam-service-account', fullObj);
        }

        $scope.onboardSvcaccAccount = function (size) {
            var obj = "svcaccList";
            var fullObj = {};
            fullObj[obj] = [];
            $state.go('change-service-account', fullObj);
        }

        $scope.onboardIamSvcaccAccount = function (size) {
            var obj = "iamsvcaccList";
            var fullObj = {};
            fullObj[obj] = [];
            $state.go('change-iam-service-account', fullObj);
        }

        $scope.editOnboardedAzureSvcacc = function (userId, size) {
            var obj = "azuresvcaccData";
            var fullObj = {};
            fullObj[obj] = { "userId": userId };
            $state.go('change-azure-service-principals', fullObj);
        }

        $scope.offboardSvcaccPopUp = function (svcaccname) {
            $scope.svcaccToOffboard = svcaccname;
            Modal.createModal('md', 'offboardSvcaccPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.offboardSvcacc = function (svcaccUserId) {
            if (svcaccUserId != '') {
                Modal.close();
                $scope.isLoadingData = true;
                $scope.isOffboardingDecommitioned = false;
                var queryParameters = "path=ad/roles/"+svcaccUserId;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccMetadata', queryParameters);
                AdminSafesManagement.getSvcaccMetadata(null, updatedUrlOfEndPoint).then(
                    function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            try {
                                if (response.data.data) {
                                    var managedBy = response.data.data.managedBy;
                                    var offboardPayload = {
                                        "owner": managedBy,
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
        var pageSize = AppConstant.PAGE_SIZE;
        var iampagesShown = 1;
        var azurepagesShown = 1;
        var iampageSize = AppConstant.PAGE_SIZE;
        var azurepageSize = AppConstant.PAGE_SIZE;
        var certpagesShown = 1;
        var certpagesShownExt = 1;
        var certpageSize = 50;
        var pagesShownOnboard = 1;

        $scope.paginationLimit = function () {
            $scope.currentshown = pageSize * pagesShown;
            if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.currentshown >= $scope.numOfSvcaccs) {
                $scope.currentshown = $scope.numOfSvcaccs;
            }
            return $scope.currentshown;
        };
        $scope.iampaginationLimit = function () {
            $scope.iamcurrentshown = iampageSize * iampagesShown;            
            if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.iamcurrentshown >= $scope.numOfIamSvcaccs) {
                $scope.iamcurrentshown = $scope.numOfIamSvcaccs;
            }        
            return $scope.iamcurrentshown;
        };
        $scope.azurepaginationLimit = function () {
            $scope.azurecurrentshown = azurepageSize * azurepagesShown;            
            if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.azurecurrentshown >= $scope.numOfAzureSvcaccs) {
                $scope.azurecurrentshown = $scope.numOfAzureSvcaccs;
            }        
            return $scope.azurecurrentshown;
        };
        $scope.hasMoreItemsToShow = function () {
            if ($scope.searchValue != '' && $scope.searchValue!= undefined) {
                if ($scope.searchValue.length<3) {
                    return pagesShown < ($scope.numOfSvcaccs / pageSize);
                }
                else {
                    return false;
                }
            }
               return pagesShown < ($scope.numOfSvcaccs / pageSize);
        };
        $scope.hasMoreIAMItemsToShow = function () {
            if ($scope.searchValue != '' && $scope.searchValue!= undefined) {
                if ($scope.searchValue.length<3) {
                    return iampagesShown < ($scope.numOfIamSvcaccs / iampageSize);
                }
                else {
                    return false;
                }
            }
               return iampagesShown < ($scope.numOfIamSvcaccs / iampageSize);
        };
        $scope.hasMoreAzureItemsToShow = function () {
            if ($scope.searchValue != '' && $scope.searchValue!= undefined) {
                if ($scope.searchValue.length<3) {
                    return azurepagesShown < ($scope.numOfAzureSvcaccs / azurepageSize);
                }
                else {
                    return false;
                }
            }
               return azurepagesShown < ($scope.numOfAzureSvcaccs / azurepageSize);
        };
        $scope.showMoreItems = function () {
            pagesShown = pagesShown + 1;
        };

        $scope.showMoreIAMItems = function () {
            iampagesShown = iampagesShown + 1;
        };
        $scope.showMoreAzureItems = function () {
            azurepagesShown = azurepagesShown + 1;
        };         
        $scope.certpaginationLimit = function (data) {
            $scope.certcurrentshown = certpageSize * certpagesShown;            
            if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.certcurrentshown >= $scope.numOfCertificates) {
                $scope.certcurrentshown = $scope.numOfCertificates;
            }        
           
            return $scope.certcurrentshown;
        };

        $scope.hasMoreCertsToShow = function () {    
        	 if ($scope.searchValue != '' && $scope.searchValue!= undefined) {
                 if ($scope.searchValue.length<3) {
                	 return certpagesShown < ($scope.numOfCertificates / certpageSize);
                 }
                 else {
                     return false;
                 }
             }
                return certpagesShown < ($scope.numOfCertificates / certpageSize);
            
        };
        $scope.showMoreCertItems = function () {
        	certpagesShown = certpagesShown + 1;
        };
        
        
        //For External
        $scope.certpaginationLimitExt = function (data) {
            $scope.certcurrentshownExt = certpageSize * certpagesShownExt;            
            if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.certcurrentshownExt >= $scope.numOfCertificates) {
                $scope.certcurrentshownExt = $scope.numOfCertificates;
            }        
           
            return $scope.certcurrentshownExt;
        };

        $scope.hasMoreCertsToShowExt = function () {    
        	 if ($scope.searchValue != '' && $scope.searchValue!= undefined) {
                 if ($scope.searchValue.length<3) {
                	 return certpagesShownExt < ($scope.numOfCertificates / certpageSize);
                 }
                 else {
                     return false;
                 }
             }
                return certpagesShownExt < ($scope.numOfCertificates / certpageSize);
            
        };
        $scope.showMoreCertItemsExt = function () {
        	certpagesShownExt = certpagesShownExt + 1;
        };
        //END For External
        

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

        $scope.collapseNoteCert = function () {
            $scope.isCollapsedCert = !$scope.isCollapsedCert;
        }

        $scope.collapseNoteIAM = function () {
            $scope.isCollapsedIAM = !$scope.isCollapsedIAM;
        }
        $scope.collapseNoteAzure = function () {
            $scope.isCollapsedAzure = !$scope.isCollapsedAzure;
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
        
        $scope.transferCertPopup = function (svcaccname) {
            Modal.createModal('md', 'transferCertPopup.html', 'AdminCtrl', $scope);
        };
        
        $scope.releasePopUp = function (svcaccname) {
            Modal.createModal('md', 'releasePopUp.html', 'AdminCtrl', $scope);
        };
        $scope.transferCertSuccessPopup = function (svcaccname) {
            Modal.createModal('md', 'transferCertSuccessPopup.html', 'AdminCtrl', $scope);
        };

        $scope.unclaimCertSuccessPopup = function (svcaccname) {
            Modal.createModal('md', 'unclaimCertSuccessPopup.html', 'AdminCtrl', $scope);
        };
        
        $scope.transferCertFailedPopup = function (svcaccname) {
            Modal.createModal('md', 'transferCertFailedPopup.html', 'AdminCtrl', $scope);
        };

        $scope.certificateOnboardPopUp = function (svcaccname) {
            Modal.createModal('md', 'certificateOnboardPopUp.html', 'AdminCtrl', $scope);
        };
        $scope.certificateOnboardFailedPopUp = function (svcaccname) {
            Modal.createModal('md', 'certificateOnboardFailedPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.transferSvcacc = function (svcaccToTransfer) {
            $scope.svcaccToOffboard = "";
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
                if (error.status == 404 || error.status == "404") {
                    var errorMsg = error.data.errors;
                    $scope.decommitionMessage = errorMsg[0];
                    $scope.svcaccToOffboard = svcaccToTransfer;
                    $scope.isOffboardingDecommitioned = true;
                    Modal.createModal('md', 'decommissionMessagePopup.html', 'AdminCtrl', $scope);
                }
                else {
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }
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
            $scope.notificationEmailErrorMessage='';
            Modal.createModal(size, 'certificatePopup.html', 'AdminCtrl', $scope);
            $scope.multiSanDnsName.name='';
            $scope.certDnsErrorMessage='';
            $scope.multiSan=[];
            $scope.addEmail();
            $scope.appNameTableOptionsSort=[]
            if($scope.appNameTableOptions!==undefined){
            	$scope.appNameTableOptionsSort = $scope.appNameTableOptions.sort(function (a, b) {
                    return (a.name > b.name ? 1 : -1);
                });    
                $scope.dropdownApplicationName = {
                        'selectedGroupOption': {"type": "Select Application Name","name":"Application Name"},       // As initial placeholder
                        'tableOptions': $scope.appNameTableOptionsSort
                    }}
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
                    $scope.certNameErrorMessage = "Certificate Name can have alphabets, numbers, . and - characters only.";
                    $scope.certInValid = true;
                } else {
                    var certName = $scope.certObj.certDetails.certName.toLowerCase();
                    if (certName.endsWith(".t-mobile.com")) {
                        $scope.certNameErrorMessage = "Please enter certificate name without .t-mobile.com";
                        $scope.certInValid = true;
                    } else if ((certName.includes(".-")) || (certName.includes("-.")) || (certName.includes(".."))){
                        $scope.certNameErrorMessage = "Please enter a valid certificate name";
                        $scope.certInValid = true;
                    } else if (certName.endsWith(".")){
                        $scope.certNameErrorMessage = "Certificate Name should not end with dot(.) character";
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
            if ($scope.certObj.certDetails.certName != undefined
	        	&& $scope.certObj.certDetails.certName != ""
                && !$scope.certInValid
                && !$scope.addrInValid
                && !$scope.portInValid                
                && !$scope.ownerEmailInValid
                && $scope.certObj.certDetails.certType != undefined
                && $scope.certObj.certDetails.applicationName != undefined
                && $scope.isOwnerSelected == true
                && $scope.appNameSelected == true
                && $scope.notificationEmails.length >0) {
                return false;
            }
            return true;
        }
        $scope.selectAppName = function (applicationObj) {         	
            $scope.certObj.certDetails.applicationName = applicationObj.tag;
            $scope.appName = applicationObj.name;
            $scope.appNameSelected = true;
            $scope.isOwnerSelected = true
        }
        
        $scope.appNameSelect = function(){
            $scope.certObj.certDetails.notificationEmails="";
            $scope.appNameSelected = false;
            $scope.selectedNotificationEmails = [];
            $scope.notificationEmails = [];
            clearNotificationEmails();
            $scope.applicationNameSelectMsg = "Fetching notification list..";
        	if($scope.dropdownApplicationName !==undefined){
                var appId = $scope.dropdownApplicationName.selectedGroupOption.id;
            $scope.dropdownApplicationName.selectedGroupOption.type;
            $scope.certObj.certDetails.applicationName = $scope.dropdownApplicationName.selectedGroupOption.tag;
            $scope.appName = $scope.dropdownApplicationName.selectedGroupOption.name;
            $scope.isOwnerSelected = true
            try{
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getApplicationDetails', "appName="+appId);
                AdminSafesManagement.getApplicationDetails(null, updatedUrlOfEndPoint).then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        var ownerEmail=$scope.certObj.certDetails.ownerEmail;
                        var brtContactEmail = response.data.spec.brtContactEmail;
                        var opsContactEmail = response.data.spec.opsContactEmail;
                        $scope.addExistingNotificationEmail(ownerEmail);
                        $scope.addExistingNotificationEmail(brtContactEmail);
                        $scope.addExistingNotificationEmail(opsContactEmail);
                        $scope.appNameSelected = true;
                        $scope.applicationNameSelectMsg = "";
                        var i = 0;
                        $scope.notificationEmails.forEach(function (email) {
                            addNotificationEmailCertString(email.email);
                            var id = "notificationemail"+ (i++);
                            angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+email.email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                        });
                    }
                },
                function (error) {
                    console.log(error);
                });
            }catch (e) {
                console.log(e);
            };
        	}
         }

        $scope.selectApplicationName = function (applicationObj) { 	
            if(applicationObj != $scope.certObj.certDetails.applicationName){	
                $scope.appNameSelected = false;	
            }	
        }
        $scope.getAppName = function (searchName) {
            return orderByFilter(filterFilter($scope.appNameTableOptions, searchName), 'name', true);
        }

        var getWorkloadDetails = function () {
            $scope.isApplicationsLoading = true;
            AdminSafesManagement.getApprolesFromCwm().then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isApplicationsLoading = false;
                    $scope.isAppNamesLoading = true;
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
                        if(JSON.parse(SessionStore.getItem("isAdmin")) == true){
                        	$scope.appNameTableOptions.push({"type":value, "name": name, "tag": appTag, "id": appID});
                        }
                        if(JSON.parse(SessionStore.getItem("isAdmin")) == false && $scope.assignedApplications.includes(appTag)  ){
                            $scope.appNameTableOptions.push({"type":value, "name": name, "tag": appTag, "id": appID});
                        }
                    }
                     $scope.isAppNamesLoading = false;
                }
                else {
                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                    $scope.error('md');
                }
            },
            function (error) {
                // Error handling function
                console.log(error);
                $scope.isAppNamesLoading = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            })
        }

        $scope.createCertPreview = function () {
            $scope.multiSanDnsName.name = '';
            $scope.certDnsErrorMessage = '';
            $scope.notificationEmail.email='';
            $scope.notificationEmailErrorMessage = '';
            $scope.isCertificatePreview = true;
            $scope.isCertificateManagePreview = true;
            var multiSanDnsPreview = [];
            $scope.multiSan.forEach(function (dns) {
                multiSanDnsPreview.push(dns.name+".t-mobile.com");
            });
            var certificateTypeVal = $scope.certObj.certDetails.certType;
            var certificateTypeName = '';
            if(certificateTypeVal.toLowerCase() === "external") {
                certificateTypeName = 'External';
            }else if(certificateTypeVal.toLowerCase() === "internal"){
                certificateTypeName = 'Internal';
            }
            $scope.certificateDetails = {
                certificateName: $scope.certObj.certDetails.certName+".t-mobile.com",
                certOwnerEmailId: $scope.certObj.certDetails.ownerEmail,
                applicationName: $scope.appName,
                notificationEmail:$scope.certObj.certDetails.notificationEmails,
                certType: certificateTypeName,
                dnsList: multiSanDnsPreview
            }
        }

        $scope.backToEdit = function () {
            $scope.isCertificatePreview = false;
            $scope.isCertificateManagePreview = false;
        }

        $scope.createCert = function () {
            try {
                clearNotificationEmails();
                Modal.close('');
                var sslcertType = 'PRIVATE_SINGLE_SAN';
                $scope.appNameTagValue=$scope.certObj.certDetails.applicationName;
                $scope.certObj.sslcertType = sslcertType;
                var multiSanDns = [];
                $scope.multiSan.forEach(function (dns) {
                    multiSanDns.push(dns.name);
                });
                var reqObjtobeSent =  { 
                    "appName": $scope.appNameTagValue,
                    "certificateName":$scope.certObj.certDetails.certName,
                    "certType":$scope.certObj.certDetails.certType,
                    "certOwnerEmailId":$scope.certObj.certDetails.ownerEmail,
                    "notificationEmail":$scope.certObj.certDetails.notificationEmails,
                    "certOwnerNTId":SessionStore.getItem("username"),
                    "dnsList": multiSanDns
                }
                $scope.certificateCreationMessage = '';
                var url = '';
                $scope.isLoadingData = true;
                $scope.isLoadingCerts = true;
                AdminSafesManagement.sslCertificateCreation(reqObjtobeSent, url).then(function (response) {

                    $scope.isLoadingData = false;
                    $scope.isLoadingCerts = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.certificateCreationMessage = response.data.messages[0];
                        resetCert();
                        $scope.certificateCreationPopUp();
                        $scope.searchValue = '';
                    }
                    $scope.multiSan=[];
                },
                function (error) {
                    resetCert();
                    var errors = error.data.errors;
                    $scope.certificateCreationMessage = errors[0];
                    $scope.certificateCreationFailedPopUp();
                    $scope.isLoadingData = false;
                    $scope.isLoadingCerts = false;
                    console.log(error);
                    $scope.searchValue = '';
                    $scope.multiSan=[];
                })
            } catch (e) {
                resetCert();
                $scope.isLoadingData = false;
                console.log(e);
                $scope.searchValue = '';
                $scope.multiSan=[];
            }

            resetCert();            
        };

        $scope.cancel = function () {
            Modal.close('');
        };

        $scope.successCancel = function () {
            Modal.close('');
            $scope.selectedTab = 0;
            if($scope.selectedTab == 1){
              	 getCertificates("", null, null,"external");
            }else{
                getCertificates("", null, null,"internal");
            }
        };

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
            $scope.certificateTypeForRevoke = certificateDetails.certType;
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
                    $scope.revocationMessage = "Select Revocation Reasons";
                    return $scope.revocationPopUp();
                }
                Modal.close('');
                var reqObjtobeSent =  {                    
                    "reason": $scope.dropdownRevocationReasons.selectedGroupOption.value
                }
               
                var url = RestEndpoints.baseURL + "/v2/certificates/" + $scope.certificateTypeForRevoke +"/" +$scope.certificateNameForRevoke   + "/revocationrequest";
                $scope.isLoadingData = true;
                AdminSafesManagement.issueRevocationRequest(reqObjtobeSent, url).then(function (response) {

                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.revocationStatusMessage = 'Revocation Successful!';
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
        
        $scope.addEmail = function () {        	
                try {
                    var userSearchList = [];

                    var queryParameters = SessionStore.getItem("username");
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('usersGetDataUsingCorpID', queryParameters);
                    return AdminSafesManagement.usersGetDataUsingCorpID(null, updatedUrlOfEndPoint).then(
                        function(response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                var filterdUserData = "";
                                userSearchList = response.data.data.values[0];
                                filterdUserData=userSearchList.userEmail;
                                $scope.certObj.certDetails.ownerEmail=filterdUserData;
                                return filterdUserData;
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

        $scope.searchEmail = function (searchVal) {        	
            if (searchVal.length > 2) {
                $scope.isUserSearchLoading = true;
                searchVal = searchVal.toLowerCase();
                try {
                    $scope.userSearchList = [];

                    var queryParameters = searchVal;
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
        	$scope.certOwnerEmailErrorMessage = '';
            $scope.certObj.certDetails.ownerEmail = "";
            $scope.certObj.certDetails.ownerNtId = "";
            $scope.isOwnerSelected = false;
        }

        $scope.validateCertificateDetailsPopUp = function (svcaccname) {
            Modal.createModal('md', 'validateCertificateDetailPopUp.html', 'AdminCtrl', $scope);
        };

        $scope.unClaimCert = function (certificateDetails) {
            Modal.close();
            $scope.isLoadingData = true;
            $scope.isLoadingCerts = true;
            var certName = certificateDetails.certificateName;
            var certificateType = certificateDetails.certType;
            var releaseReason = $scope.certObj.certDetails.releaseReason;
            var unClaimCertEndPoint = RestEndpoints.baseURL + "/v2/sslcert/unlink/" + certName+"/"+
            certificateType+"/"+releaseReason;
            AdminSafesManagement.unclaimCert(null, unClaimCertEndPoint).then(function (response) {
                    $scope.isLoadingData = true;
                    $scope.isLoadingCerts = true;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.unclaimCertMessage=response.data.messages[0];
                        $scope.unclaimCertSuccessPopup();
                    }
                },function (error) {
                          console.log("Inside Error");
                          console.log(error);
                          $scope.isLoadingData = false;
                          $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                          $scope.error('md');
                      });
        }

        $scope.goToAddPermissions = function (certificateDetails) {            
            var obj = "certificateObject";
            var myobj = certificateDetails;
            $rootScope.checkStatus = "";
            var fullObj = {};
            fullObj[obj] = myobj;
            try {       
                $scope.isLoadingData = true;
                $scope.isLoadingCerts = true;
                $scope.ispermissionData = true;               // To show the 'permissions' and hide the 'details'
                $scope.UsersPermissionsData = [];

                var certName = certificateDetails.certificateName;
                var certificateType = certificateDetails.certType;

                if(certificateType !== null && certificateType.toLowerCase() === "external") {
                    if(certificateDetails.requestStatus !== null && certificateDetails.requestStatus !== "Approved") {
                        var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/sslcert/validate/" + certName+"/"+ certificateType;
                        AdminSafesManagement.validateCertificateDetails(null, updatedUrlOfEndPoint).then(function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                                $scope.isLoadingData = false;
                                $scope.isLoadingCerts = false;
                            }
                            else {
                                $scope.isLoadingData = false;
                                $scope.isLoadingCerts = false;
                                $scope.validateCertificateDetailsPopUp();
                            }
                        },
                        function (error) {
                        if(error.status === 422){
                        	 var errors = error.data.errors;
                        	$scope.viewEditErrorMessage = errors[0];
                        	$scope.isLoadingData = false;
                        	$scope.validateCertificateDetailsPopUp();
                        } else if(error.status === 500){
                             var errors = error.data.errors;
                            $scope.viewEditErrorMessage = "Your request cannot be processed now due to some technical issue. Please try again later";
                            $scope.isLoadingData = false;
                            $scope.validateCertificateDetailsPopUp();
                          } else {
                            // Error handling function
                            console.log(error);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        }});
                    }else if(certificateDetails.certificateStatus !== null && certificateDetails.certificateStatus == "Revoked") {
                    	var updatedUrlEndPoint = RestEndpoints.baseURL + "/v2/sslcert/checkstatus/" + certName+"/"+ certificateType;
                        AdminSafesManagement.checkRevokestatus(null, updatedUrlEndPoint).then(function (responses) {
                        	if (UtilityService.ifAPIRequestSuccessful(responses)) {
                        		$rootScope.checkStatus = "Revoked";
                        		 $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                                 $scope.isLoadingData = false;
                        	}else{
                        		 $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                                 $scope.isLoadingData = false;
                        	}
                            },
                        function (error) {
                            var errors = error.data.errors;
                            if (errors[0] !== "Certificate is in Revoke Requested status") {
                            $scope.viewEditErrorMessage = 'Edit Failed';                        
                            $scope.viewEditErrorMessage = errors[0];
                            $scope.isLoadingData = false;
                            $scope.validateCertificateDetailsPopUp();
                            }
                            else{
                            	 $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                                 $scope.isLoadingData = false;
                            }
                        });
                       
                	}
                	else{
                    $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                    $scope.isLoadingData = false;
                	}
                }else {
                	if(certificateDetails.certificateStatus !== null && certificateDetails.certificateStatus == "Revoked") {
                    	var updatedUrlEndPoint = RestEndpoints.baseURL + "/v2/sslcert/checkstatus/" + certName+"/"+ certificateType;
                        AdminSafesManagement.checkRevokestatus(null, updatedUrlEndPoint).then(function (responses) {
                        	if (UtilityService.ifAPIRequestSuccessful(responses)) {
                        		$rootScope.checkStatus = "Revoked";
                        		 $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                                 $scope.isLoadingData = false;
                        	}else{
                        		 $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                                 $scope.isLoadingData = false;
                        	}
                            },
                            function (error) {
                                var errors = error.data.errors;
                                if (errors[0] !== "Certificate is in Revoke Requested status") {
                                $scope.viewEditErrorMessage = 'Edit Failed';                        
                                $scope.viewEditErrorMessage = errors[0];
                                $scope.isLoadingData = false;
                                $scope.validateCertificateDetailsPopUp();
                                }
                                else{
                                	 $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                                     $scope.isLoadingData = false;
                                }
                            });
                       
                	}
                	else{
                    $state.go('change-certificate', fullObj, $rootScope.checkStatus);
                    $scope.isLoadingData = false;
                	}
                }
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
                var certType = certificateDetails.certType;
                var url = RestEndpoints.baseURL + "/v2/certificates/" +certType+"/"+ certificateName + "/renew";
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
            
            $scope.AddorRemoveDNS= function (id) {
            	$scope.certDnsErrorMessage = '';
            	$scope.multiSanDnsName.name = "";
            	if(id==false){
            		for (var i=0;i<$scope.multiSan.length;i++) {
            			 var dnsElement = angular.element( document.querySelector( '#dns'+i ) );
                         dnsElement.remove();
            		}
                         $scope.selectedMultiSan = [];
                         $scope.multiSan = $scope.selectedMultiSan;
            		
            		}
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
                        $scope.certDnsErrorMessage = "DNS can have alphabets, numbers, . and - characters only."
                        $scope.dnsInvalid = true;
                    } else {
                        var certName = $scope.multiSanDnsName.name.toLowerCase();
                        if (certName.endsWith(".t-mobile.com")) {
                            $scope.certDnsErrorMessage = "Please enter DNS without .t-mobile.com"
                            $scope.dnsInvalid = true;
                        }  else if ( (certName.includes(".-")) || (certName.includes("-.")) || (certName.includes(".."))){
                            $scope.certDnsErrorMessage = "Please enter a valid DNS"
                            $scope.dnsInvalid = true;
                        } else if (certName.endsWith(".")){
                            $scope.certDnsErrorMessage = "DNS should not end with dot(.) character";
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
          $scope.releasePopUp = function (certDetails) {
                $scope.releaseReasonInValid = true;
                $scope.certObj.certDetails.releaseReason="";
                $scope.fetchDataError = false;
                $rootScope.certDetails = certDetails;
                if ($rootScope.certDetails.certType != null &&  $rootScope.certDetails.certType == "internal") {
                    $scope.certificateTypeVal= "Internal";
                 } else if ($rootScope.certDetails.certType != null && $rootScope.certDetails.certType ==  "external") {
                   $scope.certificateTypeVal= "External";
                 }

                Modal.createModal('md', 'releasePopUp.html', 'AdminCtrl', $scope);
            };
            
            $scope.transferCertPopup = function (certDetails) {
                $scope.fetchDataError = false;
                $rootScope.certDetails = certDetails;  
                $scope.certTransferInValid = true;
                $scope.certOwnerEmailErrorMessage = '';
                $scope.certOwnerTransferErrorMessage = '';
                if(certDetails.requestStatus!=null && certDetails.requestStatus!=undefined && certDetails.requestStatus=="Pending Approval"){	
                	$scope.isLoadingData = false;	
                	$scope.viewEditErrorMessage = "Certificate may not be approved or rejected.Please follow the instructions mentioned in email ";
                    $scope.validateCertificateDetailsPopUp();	
                }else{	
                Modal.createModal('md', 'transferCertPopup.html', 'AdminCtrl', $scope);	
            }	
            };


            $scope.reasonValidation = function () {
                $scope.releaseReasonInValid = true;
                if ($scope.certObj.certDetails.releaseReason== null || $scope.certObj.certDetails.releaseReason == ""){
                        $scope.releaseReasonInValid = true;
                } else {
                      $scope.releaseReasonInValid = false;
                }
            }
            
            $scope.ownerEmailValidation = function () {
                $scope.certOwnerEmailErrorMessage = '';
                $scope.certOwnerTransferErrorMessage = ''; 
                if ($scope.certObj.certDetails.ownerEmail == null || $scope.certObj.certDetails.ownerEmail == ""){                	
                	$scope.certTransferInValid = true;
                }
                if ($scope.certObj.certDetails.ownerEmail != null && $scope.certObj.certDetails.ownerEmail != undefined
                    && $scope.certObj.certDetails.ownerEmail != "") {
                    
                    if ($rootScope.certDetails.certOwnerEmailId==$scope.certObj.certDetails.ownerEmail) {
                        $scope.certOwnerEmailErrorMessage = "New owner email id should not be same as owner email id"
                        $scope.certTransferInValid = true;
                    } 
                }
            }
            
            $scope.selectOwnerforCert = function (ownerEmail) {
            	$scope.certOwnerEmailErrorMessage = '';            	
                if (ownerEmail != null) {
                    $scope.certObj.certDetails.ownerEmail = ownerEmail.userEmail;
                    $scope.certObj.certDetails.ownerNtId = ownerEmail.userName;
                    $scope.isOwnerSelected = true;                    
                    if ($scope.certObj.certDetails.ownerEmail != null && $scope.certObj.certDetails.ownerEmail != undefined
                            && $scope.certObj.certDetails.ownerEmail != "") {
                            
                            if ($rootScope.certDetails.certOwnerEmailId==$scope.certObj.certDetails.ownerEmail) {                            	
                                $scope.certOwnerEmailErrorMessage = "New owner email id should not be same as owner email id"
                                $scope.certTransferInValid = true;
                            } 
                            else{
                            	$scope.certTransferInValid = false;
                            }
                        }
                }
            }
            
            $scope.transferCert = function (certificateDetails) {
             	if ($rootScope.certDetails !== null && $rootScope.certDetails !== undefined) {
               		certificateDetails = $rootScope.certDetails;
                  }             	
             	$scope.certOwnerTransferErrorMessage = '';
                $rootScope.certDetails = null;                
                try{
                $scope.isLoadingData = true;
                Modal.close();                
                $scope.transferMessage = ''; 
                certificateDetails.certOwnerNtid='';
                var certificateName = $scope.getCertSubjectName(certificateDetails);
                certificateDetails.certificateName = certificateName; 
                var certOwnerEmailId = $scope.certObj.certDetails.ownerEmail;
                var certType = certificateDetails.certType;
                certificateDetails.certOwnerNtid=$scope.certObj.certDetails.ownerNtId;                                  
                certificateDetails.applicationName=certificateDetails.appNameTagValue;    
                var url = RestEndpoints.baseURL + "/v2/sslcert/" +certType+"/"+ certificateName +"/"+certOwnerEmailId +"/transferowner";
                $scope.isLoadingData = true;   
                $scope.isLoadingCerts = true;
                resetCert();
                AdminSafesManagement.transferCertificate(null, url).then(function (response) {
                    $scope.isLoadingData = false;
                    $scope.isLoadingCerts = false;
                    $scope.certObj.certDetails.ownerEmail="";
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.transferMessage = 'Certificate Owner Transferred Successfully!';
                        $scope.transferMessage = response.data.messages[0];  
                        $scope.transferCertSuccessPopup();
                        $scope.requestDataFrAdmin();
                        $scope.searchValue = '';
                    }
                },
                    function (error) {
                        var errors = error.data.errors;
                        $scope.transferMessage = 'Transfer ownership Failed';                        
                        if (errors[0] == "Access denied: No permission to transfer the ownership of this certificate") {
                            $scope.transferMessage = "For security reasons, you need to log out and log in again for the permissions to take effect.";
                        } else {
                            $scope.transferMessage = errors[0];
                        } 
                        $scope.transferCertFailedPopup();
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
            
            $scope.filterCert = function (val) {            	
                var filterSearch = $scope.searchValue;                
                if ($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2 ) {                    
                        $scope.finalFilterCertResults = $scope.certificateData.certificates.filter(function (searchValue) {
                        	if(searchValue.certificateName != undefined && searchValue.certificateName != ""){
                            return searchValue.certificateName.includes(filterSearch);
                        	}
                        });                   

                } else {                    
                    	$scope.finalFilterCertResults = $scope.certificateData.certificates.slice(0);                    
                }
                $scope.searchValue = $scope.searchValue;               
                
            }

        $scope.getCertificatesForOnboard = function() {
            $scope.isInternalCert = false;
            $scope.isExternalCert = false;
            $scope.isLoadingData = true;
            $scope.isLoadingCerts = true;
            pagesShownOnboard = 1;
            $scope.searchValue = "";

            var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/sslcert/pendingcertificates";
            AdminSafesManagement.getAllOnboardPendingCertificates(null, updatedUrlOfEndPoint).then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    if (response.data != "" && response.data != undefined) {
                        $scope.certificatesToOnboard = response.data;
                        $scope.numOfOnboardPendingCertificates = $scope.certificatesToOnboard.length;
                    }
                }else {
                    $scope.certificatesLoaded =  true;
                    if(response.status !== 404) {
                        $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                        $scope.error('md');
                    }
                }
                $scope.isLoadingData = false;
                $scope.isLoadingCerts = false;
            },
            function (error) {
                // Error handling function
                $scope.isLoadingData = false;
                $scope.isLoadingCerts = false;
                $scope.certificatesLoaded =  true;
                if (error.status !== 404) {
                    console.log(error);
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }
            });
        }

        $scope.showOnboardCertificatePopup = function(certificateToOnboard) {
            $scope.clearOnboardCert();
            $scope.certificateToOnboard = certificateToOnboard;
            $scope.certificateToOnboard.ownerEmail = "";
            $scope.certificateToOnboard.ownerNtId = "";
            $scope.certificateToOnboard.tag = "";
            $scope.certificateToOnboard.applicationName = "";
            $scope.certificateToOnboard.notificationEmails = "";
            if($scope.appNameTableOptions!==undefined){
                $scope.appNameTableOptionsSort = $scope.appNameTableOptions.sort(function (a, b) {
                    return (a.name > b.name ? 1 : -1);
                });
                $scope.dropdownApplicationName = {
                        'selectedGroupOption': {"type": "Select Application Name","name":"Application Name"},
                        'tableOptions': $scope.appNameTableOptionsSort
                }
            }
            Modal.createModal('md', 'onboardCertificatePopup.html', 'AdminCtrl', $scope);
        }

        $rootScope.cancelCertOnboard = function () {
            $scope.certificateToOnboard = null;
            resetOnBoardCert();
            Modal.close();
        };

        $scope.toogleOnboardPreview = function() {
            $scope.isCertificateOnboardPreview = !$scope.isCertificateOnboardPreview;
            $scope.notificationEmail.email = '';
            $scope.notificationEmailErrorMessage = '';
            $scope.certificateToOnboard.notificationEmails = "";
        }

        $scope.addNotificationEmail = function () {
            var length = $scope.notificationEmails.length;
            if ($scope.notificationEmail && $scope.notificationEmail.email!="" && !isDuplicateNotificationEmail($scope.notificationEmail.email)) {
                var id="dns"+length;
                angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+$scope.notificationEmail.email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                $scope.notificationEmails.push({ "id": length, "email":$scope.notificationEmail.email});
                addNotificationEmailString($scope.notificationEmail.email);
                $scope.notificationEmail.email = "";
                $scope.notificationEmailErrorMessage = '';
                $scope.isNotificationEmailSelected = false;
            }
        }

        $scope.addNotificationEmailCert = function () {
            var length = $scope.notificationEmails.length;
            $scope.notificationEmailErrorMessage = '';
            var id="notificationemail"+length;
            if ($scope.notificationEmail && $scope.notificationEmail.email!="" && !isDuplicateNotificationEmail($scope.notificationEmail.email)) {
                angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+$scope.notificationEmail.email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                $scope.notificationEmails.push({ "id": length, "email":$scope.notificationEmail.email});
                addNotificationEmailCertString($scope.notificationEmail.email);
                $scope.notificationEmail.email = "";
                $scope.isNotificationEmailSelected = false;
            }
        }

        $scope.deleteNotificationEmail = function (id) {
            var notificationEmailElement = angular.element( document.querySelector( '#'+id ) );
            notificationEmailElement.remove();
            var index = id.substring(17);
            $scope.selectedNotificationEmails = [];
            for (var i=0;i<$scope.notificationEmails.length;i++) {
                if (index != $scope.notificationEmails[i].id) {
                    $scope.selectedNotificationEmails.push($scope.notificationEmails[i]);
                }
            }
            $scope.notificationEmails = $scope.selectedNotificationEmails;
            $scope.isNotificationEmailSelected = false;
        }

        $scope.clearOnboardOwnerEmail = function () {
            $scope.selectedNotificationEmails = [];
            for (var i=0;i<$scope.notificationEmails.length;i++) {
                if ($scope.certificateToOnboard.ownerEmail !=="" && $scope.certificateToOnboard.ownerEmail.toLowerCase() !== $scope.notificationEmails[i].email.toLowerCase()) {
                    $scope.selectedNotificationEmails.push($scope.notificationEmails[i]);
                }
            }
            $scope.notificationEmails = $scope.selectedNotificationEmails;
            $scope.certificateToOnboard.ownerEmail = "";
            $scope.certificateToOnboard.ownerNtId = "";
            $scope.isOwnerSelectedForOnboard = false;
            refreshOnboardNotificationEmails();
        }

        $scope.clearNotificationEmail = function() {
            $scope.notificationEmail = { email:""};
            $scope.isNotificationEmailSelected = false;
            $scope.notificationEmailErrorMessage = '';
        }
        $scope.clearNotificationEmailmessage = function() {
            $scope.notificationEmailErrorMessage='';
        }
        $scope.clearOnboardCert = function () {
            $scope.certificateToOnboard = null;
            $scope.isOwnerSelectedForOnboard = false;
            $scope.appnameSelectedForOnboard = false;
            $scope.isNotificationEmailSelected = false;
            $scope.selectedNotificationEmails = [];
            $scope.notificationEmails = [];
            clearNotificationEmails();
        }

        $scope.selectOwnerforCertOnboard = function (ownerEmail) {
            if (ownerEmail != null) {
                $scope.certificateToOnboard.ownerEmail = ownerEmail.userEmail;
                $scope.certificateToOnboard.ownerNtId = ownerEmail.userName;
                $scope.isOwnerSelectedForOnboard = true;
                $scope.isOwnerEmailSearch = false;

                var length = $scope.notificationEmails.length;
                if ($scope.notificationEmail && !isDuplicateOwnerNotificationEmail($scope.certificateToOnboard.ownerEmail)) {
                    var id="dns"+length;
                    angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+$scope.certificateToOnboard.ownerEmail+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                    $scope.notificationEmails.push({ "id": length, "email":$scope.certificateToOnboard.ownerEmail});
                }
            }
        }

        $scope.selectNotificationEmail = function (ownerEmail) {
            if (ownerEmail != null) {
                $scope.notificationEmail.email = ownerEmail.userEmail;
                $scope.isNotificationEmailSelected = true;
                $scope.isNotificationEmailSearch = false;
                $scope.notificationEmailErrorMessage = '';
            }
        }

        var isDuplicateNotificationEmail = function (email) {
            $scope.certDnsErrorMessage = '';
            $scope.notificationEmailErrorMessage = '';
            $scope.isDuplicateNotificationEmail=false;
            for (var i=0;i<$scope.notificationEmails.length;i++) {
                if (email.toLowerCase() == $scope.notificationEmails[i].email.toLowerCase()) {
                    $scope.notificationEmailErrorMessage = 'Duplicate Email';
                    $scope.isDuplicateNotificationEmail=true;
                    return true;
                }
            }
            return false;
        }

        $scope.appNameSelectForOnboard = function () {
            $scope.appnameSelectedForOnboard = false;
            $scope.selectedNotificationEmails = [];
            $scope.notificationEmails = [];
            $scope.certificateToOnboard.notificationEmails = "";
            $scope.notificationEmailErrorMessage = '';
            clearNotificationEmails();
            $scope.applicationNameSelectMsg = "Fetching notification list..";
            if($scope.dropdownApplicationName !==undefined){
                var tag = $scope.dropdownApplicationName.selectedGroupOption.tag;
                var appId = $scope.dropdownApplicationName.selectedGroupOption.id;
                $scope.certificateToOnboard.applicationName = $scope.dropdownApplicationName.selectedGroupOption.name;
                try{
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getApplicationDetails', "appName="+appId);
                    AdminSafesManagement.getApplicationDetails(null, updatedUrlOfEndPoint).then(function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            var brtContactEmail = response.data.spec.brtContactEmail;
                            var opsContactEmail = response.data.spec.opsContactEmail;
                            $scope.addExistingNotificationEmail(brtContactEmail);
                            $scope.addExistingNotificationEmail(opsContactEmail);
                            $scope.appnameSelectedForOnboard = true;
                            $scope.applicationNameSelectMsg = "";
                            $scope.certificateToOnboard.tag = tag;
                            if($scope.certificateToOnboard.ownerEmail != null && $scope.certificateToOnboard.ownerEmail !== "" && $scope.certificateToOnboard.ownerEmail !== undefined){
                                $scope.addExistingNotificationEmail($scope.certificateToOnboard.ownerEmail);
                            }
                            var i = 0;
                            $scope.notificationEmails.forEach(function (email) {
                                var id = "dns"+ (i++);
                                angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+email.email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                            });
                        }
                    },
                    function (error) {
                        console.log(error);
                    });
                }catch (e) {
                    console.log(e);
                };
            }
        }

        var clearNotificationEmails = function () {
            angular.element('#notificationEmailList').html("");
            $scope.notificationEmail.email = "";
        }

        $scope.addExistingNotificationEmail = function(emailListAsString) {
            if (emailListAsString != undefined && emailListAsString != null) {
                emailListAsString.split(",").forEach(function (email) {
                    var id = $scope.notificationEmails.length;
                    //var duplicate = $scope.notificationEmails.find(element => element.email.toLowerCase() == email.toLowerCase());
                    var duplicate = $scope.notificationEmails.find(function (element) {                        
                        return (element.email.toLowerCase() == email.toLowerCase());
                    });
                    if (duplicate == undefined) {
                        $scope.notificationEmails.push({ "id": id, "email":email});
                    }
                });
            }
        }

        $scope.searchEmailForNotification = function (email) {
            if (!email.endsWith("\\")) {
                $scope.isNotificationEmailSearch = true;
                return $scope.searchEmail(email);
            }
        }

        $scope.searchOwnerEmailForOnboard = function (email) {
            if (!email.endsWith("\\")) {
                $scope.isOwnerEmailSearch = true;
                return $scope.searchEmail(email);
            }
        }

        $scope.isOnboardCertBtnDisabled = function () {
            if ($scope.certificateToOnboard != null
                && $scope.certificateToOnboard.certificateName != ""
                && $scope.certificateToOnboard.certificateType != ""
                && $scope.certificateToOnboard.applicationName != ""
                && $scope.certificateToOnboard.ownerEmail != ""
                && $scope.certificateToOnboard.ownerNtId != ""
                && $scope.notificationEmails.length >0
                ) {
                return false;
            }
            return true;
        }

        var addNotificationEmailString = function(email) {
            if ($scope.certificateToOnboard.notificationEmails != "") {
                $scope.certificateToOnboard.notificationEmails = $scope.certificateToOnboard.notificationEmails + ",";
            }
            $scope.certificateToOnboard.notificationEmails = $scope.certificateToOnboard.notificationEmails + email;
        }

        var addNotificationEmailCertString = function(email) {
            if ($scope.certObj.certDetails.notificationEmails != "" && $scope.certObj.certDetails.notificationEmails != undefined) {
                    $scope.certObj.certDetails.notificationEmails =    $scope.certObj.certDetails.notificationEmails + "," + email;
                }
                else{
                    $scope.certObj.certDetails.notificationEmails= email;
                 }
            }
        $scope.onboardCert = function() {
            try{
                Modal.close('');
                var sslcertType = 'PRIVATE_SINGLE_SAN';
                $scope.appNameTagValue=$scope.certObj.certDetails.applicationName;
                $scope.certObj.sslcertType = sslcertType;
                var multiSanDns = [];
                $scope.certificateToOnboard.notificationEmails = "";
                $scope.notificationEmails.forEach(function (email) {
                    addNotificationEmailString(email.email);
                });
                var onboardRequest =  {
                    "appName": $scope.certificateToOnboard.tag,
                    "certificateName":$scope.certificateToOnboard.certificateName,
                    "certType":$scope.certificateToOnboard.certType,
                    "certOwnerEmailId":$scope.certificateToOnboard.ownerEmail,
                    "certOwnerNTId":$scope.certificateToOnboard.ownerNtId,
                    "notificationEmail": $scope.certificateToOnboard.notificationEmails,
                    "dnsList": multiSanDns
                }
                $scope.certificateOnboardMessage = '';
                var url = '';
                $scope.isLoadingData = true;
                $scope.isLoadingCerts = true;
                AdminSafesManagement.onboardSslCertificates(onboardRequest, url).then(function (response) {
                    $scope.isLoadingData = false;
                    $scope.isLoadingCerts = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.certificateOnboardMessage = response.data.messages[0];
                        resetOnBoardCert();
                        $scope.certificateOnboardPopUp();
                        $scope.searchValue = '';
                    }
                    $scope.multiSan=[];
                },
                function (error) {
                    resetOnBoardCert();
                    var errors = error.data.errors;
                    $scope.certificateOnboardMessage = errors[0];
                    $scope.certificateOnboardFailedPopUp();
                    $scope.isLoadingData = false;
                    $scope.isLoadingCerts = false;
                    console.log(error);
                    $scope.searchValue = '';
                })
            } catch (e) {
                resetOnBoardCert();
                $scope.isLoadingData = false;
                console.log(e);
                $scope.searchValue = '';
            }
            resetOnBoardCert();
        }

        var resetOnBoardCert = function () {
            $scope.isCertCollapsed = false;
            $scope.certObj.certDetails.ownerEmail = "";
            $scope.isCertificateOnboardPreview = false;
            $scope.notificationEmailErrorMessage = '';
            if($scope.appNameTableOptions!==undefined){
                $scope.appNameTableOptionsSort = $scope.appNameTableOptions.sort(function (a, b) {
                    return (a.name > b.name ? 1 : -1);
                });
                $scope.dropdownApplicationName = {
                        'selectedGroupOption': {"type": "Select Application Name","name":"Application Name"},       // As initial placeholder
                        'tableOptions': $scope.appNameTableOptionsSort
                }
            }
        }

        $scope.hasMoreCertsToShowForOnboard = function () {
            if ($scope.searchValue != '' && $scope.searchValue!= undefined) {
                if ($scope.searchValue.length<3) {
                    return pagesShownOnboard < ($scope.numOfOnboardPendingCertificates / pageSize);
                }
                else {
                    return false;
                }
            }
            return pagesShownOnboard < ($scope.numOfOnboardPendingCertificates / pageSize);
       };
       $scope.showMoreCertItemsOnboard = function () {
            pagesShownOnboard = pagesShownOnboard + 1;
       };
       $scope.certpaginationLimitForOnboard = function () {
           $scope.certcurrentshownonboardlist = pageSize * pagesShownOnboard;
           if (($scope.searchValue != '' && $scope.searchValue != undefined && $scope.searchValue.length > 2) || $scope.certcurrentshownExt >= $scope.numOfOnboardPendingCertificates) {
               $scope.certcurrentshownonboardlist = $scope.numOfOnboardPendingCertificates;
           }
           return $scope.certcurrentshownonboardlist;
       };

        var isDuplicateOwnerNotificationEmail = function (email) {
            $scope.certDnsErrorMessage = '';
            for (var i=0;i<$scope.notificationEmails.length;i++) {
                if (email.toLowerCase() == $scope.notificationEmails[i].email.toLowerCase()) {
                    return true;
                }
            }
            return false;
        }

        var refreshOnboardNotificationEmails = function() {
            angular.element('#notificationEmailList').html("");
            var i = 0;
            $scope.notificationEmails.forEach(function (email) {
                var id = "dns"+ (i++);
                angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+email.email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
            });
        }

        $scope.offboardDecommissionedSvcacc = function (svcaccUserId) {
            if (svcaccUserId != '') {
                Modal.close();
                $scope.isLoadingData = true;
                $scope.isOffboardingDecommitioned = false;
                var queryParameters = "path=ad/roles/"+svcaccUserId;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccMetadata', queryParameters);
                AdminSafesManagement.getSvcaccMetadata(null, updatedUrlOfEndPoint).then(
                    function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            try {
                                if (response.data.data) {
                                    var managedBy = response.data.data.managedBy;
                                    var offboardPayload = {
                                        "owner": managedBy,
                                        "name": svcaccUserId
                                    }
                                    AdminSafesManagement.offboardDecommissionedServiceAccount(offboardPayload, '').then(
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

        init();

    });
})(angular.module('vault.features.AdminCtrl', [
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications',
    'vault.constants.RestEndpoints',
    'vault.constants.AppConstant'
]));