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
    app.controller('ChangeIamServiceAccountCtrl', function ($scope, $rootScope, Modal, $timeout, fetchData, $http, UtilityService, Notifications, $window, $state, $stateParams, $q, SessionStore, vaultUtilityService, ModifyUrl, AdminSafesManagement, AppConstant, $filter, filterFilter, orderByFilter, RestEndpoints, CopyToClipboard) {
        $scope.selectedGroupOption = '';            // Selected dropdown value to be used for filtering
        $rootScope.showDetails = true;              // Set true to show details view first
        $rootScope.activeDetailsTab = 'details';
        $scope.svcaccOnboarded = false;                 // Flag to indicate if a svcacc has been onboarded
        $scope.isEditSvcacc = false;
        $scope.awsRadioBtn = {};                    // Made an object instead of single variable, to have two way binding between
        $scope.approleRadioBtn = {};                                    // modal and controller
        $scope.isCollapsed = true;
        $scope.isSvcaccExpired = false;
        $scope.expiredNote = '';
        $scope.svcInputSelected = false;
        $scope.customTTL = '';
        $scope.permissionChangeInProgress = false;
        $scope.usrRadioBtnVal = 'read';             // Keep it in lowercase
        $scope.grpRadioBtnVal = 'read';             // Keep it in lowercase
        $scope.awsRadioBtn['value'] = 'read';       // Keep it in lowercase
        $scope.approleRadioBtn['value'] = 'read';
        $scope.isEmpty = UtilityService.isObjectEmpty;
        $scope.roleNameSelected = false;
        $scope.awsConfPopupObj = {
            "auth_type":"",
            "role": "",
            "bound_account_id": "",
            "bound_region": "",
            "bound_vpc_id": "",
            "bound_subnet_id": "",
            "bound_ami_id": "",
            "bound_iam_instance_profile_arn": "",
            "bound_iam_role_arn": "",
            "policies": "",
            "bound_iam_principal_arn": "",
            "resolve_aws_unique_ids":"false"
        };
        $scope.approleConfPopupObj = {
            "token_max_ttl":"",
            "token_ttl": "",
            "role_name": "",
            "policies": "",
            "bind_secret_id": "",
            "secret_id_num_uses": "",
            "secret_id_ttl": "",
            "token_num_uses": ""
        };
        $scope.tableOptions = [
            
        ];

        $scope.radio = {
            value: 'read',
            options: [{
                'text': 'read'
            }, {
                'text': 'reset'
            }, {
                'text': 'deny'
            }]
        };

        $scope.appNameTableOptions = [];

        $scope.bindSecretRadio = {
            value: 'false',
            options: [{
                'text': 'false'
            }, {
                'text': 'true'
            }]
        };

        $scope.detailsNavTags = [{
            displayName: 'DETAILS',
            navigationName: 'details',
            addComma: true,
            show: true
        }, {
            displayName: 'PERMISSIONS',
            navigationName: 'permissions',
            addComma: false,
            show: false
        }];

        $scope.inputValue = {
            "userNameVal": '',
            "grpNameVal": '',
            "userNameValEmpty": false,
            "grpNameValEmpty": false
        }

        var clearInputPermissionData = function () {
            $scope.inputValue = {
                "userNameVal": '',
                "grpNameVal": '',
                "userNameValEmpty": false,
                "grpNameValEmpty": false
            }
            $scope.permissionChangeInProgress = false;
        }

        $scope.isApproleBtnDisabled = function() {
            if ($scope.roleNameSelected){
                    return false;
            }
            return true;
        }

        $scope.goBack = function () {
            var targetState = 'manage';
            if (SessionStore.getItem("isAdmin") === 'true') {
                targetState = 'admin';
            }
            if ($scope.goBackToAdmin !== true) {
                if ($rootScope.showDetails === true) {
                    $state.go(targetState);
                }
                else {
                    $rootScope.showDetails = true;
                    $rootScope.activeDetailsTab = 'details';
                }
            }
            else {
                if ($rootScope.lastVisited) {
                    $state.go($rootScope.lastVisited);
                } else
                    $state.go(targetState);
            }
        }

        $scope.roleNameSelect = function() {
            var queryParameters = $scope.dropDownRoleNames.selectedGroupOption.type;
            $scope.roleNameSelected = true;
            $scope.approleConfPopupObj.role_name = queryParameters;
        }

        $scope.selectAppName = function (applicationObj) {
            $scope.applicationName = applicationObj;
            $scope.svcacc.appName = applicationObj.type;
            $scope.appNameSelected = true;
        }

        $scope.error = function (size) {
            Modal.createModal(size, 'error.html', 'ChangeIamServiceAccountCtrl', $scope);
        };

        /************************  Functions for autosuggest start here ***************************/
        //initialise values
        $scope.domainName = '';
        if (AppConstant.DOMAIN_NAME) {
            $scope.domainName = AppConstant.DOMAIN_NAME.toLowerCase();
        }       
        $scope.searchValue = {
            userName: '',
            groupName: ''
        };
        $scope.userNameDropdownVal = [];
        $scope.groupNameDropdownVal = [];
        
        $scope.totalDropdownVal = [];
        $rootScope.loadingDropDownData = false;
        
        $scope.showInputLoader = {
            'show':false
        };
        $scope.inputSelected = {
            'select': false
        }

        var delay = (function(){
            var timer = 0;
            return function(callback, ms){
              clearTimeout (timer);
              timer = setTimeout(callback, ms);
            };

        })(); 
        var lastContent;
        var duplicateFilter = (function(content){
          return function(content,callback){
            content=$.trim(content);
            // callback provided for content length > 2
            if(content !== lastContent && content.length > 2){
              callback(content);
            }
            lastContent = content;
          };
        })();

        //clear selected value on cross icon click
        $scope.clearInputValue = function(id) {
            document.getElementById(id).value = "";
            $scope.inputSelected.select = false;
            $scope.searchValue = {
                userName: '',
                groupName: ''
            };
            lastContent = '';
            $scope.showNoMatchingResults = false;
        }

        // function call on input keyup 
        $scope.onKeyUp = function(newVal, variableChanged, forOwner) {
            if (newVal.length === 0) {
                return;
            }
            $scope.invalidEmail = false;
            $scope.showNoMatchingResults = false;        
            $scope.showInputLoader.show = false;
            $scope.inputSelected.select = false;
            $scope.autoCompleteforOwner = false;
            //check autocomplete is for owner email id
            if(forOwner) {
                $scope.autoCompleteforOwner = true;
            }
            if (newVal.userName && variableChanged === 'userName') {
                newVal.groupName = "";    
                $scope.userNameDropdownVal = [];
            } else if (newVal.groupName &&  variableChanged === 'groupName') {
                newVal.userName = "";
                $scope.groupNameDropdownVal = [];
            } 
             if (variableChanged === 'userName') {
                if (!UtilityService.getAppConstant('AD_USERS_AUTOCOMPLETE') ) {
                    return;
                }
             } else if (variableChanged === 'groupName') {
                 if(!UtilityService.getAppConstant('AD_GROUP_AUTOCOMPLETE')) {
                    return;
                 }
             }
             var newLetter = newVal[variableChanged];
                newLetter = newLetter.replace(" ", "");
                initiateAutoComplete(variableChanged, ['loading']);
           // delay before providing api call      
          delay(function(){
              // check for duplicate values with previous value
            duplicateFilter(newLetter, function(value){
                $scope.showInputLoader.show = true;
                $scope.getDropdownDataForPermissions(variableChanged, value, forOwner);                
            });          
          }, 500 ); // delay of 500ms provided before making api call
        }

        $scope.getDropdownDataForPermissions = function (searchFieldName, searchFieldText, forOwner) {      
            if (searchFieldText.length > 2) {
                vaultUtilityService.getDropdownDataForPermissions(searchFieldName, searchFieldText, forOwner).then(function (res, error) {
                    var serviceData;
                    if (res) {
                        serviceData = res;
                        $scope.loadingDataFrDropdown = serviceData.loadingDataFrDropdown;
                        $scope.erroredFrDropdown = serviceData.erroredFrDropdown;
                        $scope.successFrDropdown = serviceData.successFrDropdown;
                        if (serviceData.response.data.data.values.length === 0) {
                            $scope.showNoMatchingResults = true;
                        }
                        massageDataFrPermissionsDropdown(searchFieldName, searchFieldText, serviceData.response.data.data.values, forOwner);
                        $scope.$apply();
                    } else {
                        serviceData = error;
                        $scope.commonErrorHandler(serviceData.error, serviceData.error || serviceData.response.data, "getDropdownData");

                    }
                },
                function (error) {
                    // Error handling function when api fails
                    $scope.showInputLoader.show = false;
                    if (error.status === 500) {
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_NETWORK');
                        $scope.error('md');
                    } else if(error.status !== 200 && (error.xhrStatus === 'error' || error.xhrStatus === 'complete')) {                        
                        if (searchFieldName === "userName" && $scope.searchValue.userName.length > 0) {
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_AUTOCOMPLETE_USERNAME');
                        } else if (searchFieldName === "groupName" && $scope.searchValue.groupName.length > 0) {
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_AUTOCOMPLETE_GROUPNAME');
                        }
                        $scope.error('md');
                    }                    
                })
            }
        };
        $scope.commonErrorHandler = function (error, response, block) {
            if (block === null) {
                $scope.loadingDataFrRoles = false;
                $scope.erroredInRoles = true;
                $scope.successInRoles = false;
                $scope.errorMsgFrRoles = "Please try again, if the issue persists contact Vault Administrator";
                console.log("Data from service is not in expected Format ", error);
                if ((response != undefined) || (response != null)) {
                    if (response.message) {
                        $scope.errorMsg = response.message;
                        console.log(response.message);
                    }
                }
            }
        }
        var massageDataFrPermissionsDropdown = function (searchFieldName, searchFieldText, dataFrmApi, forOwner) {
            var serviceData = vaultUtilityService.massageDataFrPermissionsDropdown(searchFieldName, searchFieldText, dataFrmApi, forOwner);
            $scope.showInputLoader.show = false;
               if (searchFieldName === 'userName') {
                    $scope.userNameDropdownVal = serviceData.sort();
                    initiateAutoComplete(searchFieldName, $scope.userNameDropdownVal, forOwner);
                } else if (searchFieldName === 'groupName') {
                    $scope.groupNameDropdownVal = serviceData.sort();
                    initiateAutoComplete(searchFieldName, $scope.groupNameDropdownVal, forOwner);
                }        
           
            $rootScope.loadingDropDownData = false;
        }

        var initiateAutoComplete = function(searchFieldName, data, forOwner) {
            var id;
            if (searchFieldName === "userName") {
                id = '#addUser';
                // for owner email id provide autocomplete
                if ($scope.autoCompleteforOwner) {
                    id = "#addOwnerEmail"
                }
            } else if (searchFieldName === "groupName") {
                id = '#addGroup';
            }
            $(id).focusout();
            $(id).trigger("focus");             
            $(id)
                .autocomplete({
                    source: data,
                    minLength: 3,
                    select: function(event, ui) {                        
                        var selectedName = ui.item.value.toLowerCase();
                        if (selectedName.includes(".com")) {
                            event.preventDefault();
                            if ($scope.autoCompleteforOwner) {
                                this.value = ui.item.value;
                            }else if (searchFieldName === "userName") {
                                this.value = ui.item.value.split(' - ')[1];
                            } else if (searchFieldName === "groupName") {
                                this.value = ui.item.value.split(' - ')[0];
                            }                 
                        }
                        $scope.inputSelected.select = true; 
                        $scope.showNoMatchingResults = false;  
                        $scope.invalidEmail = false;                
                        $(id).blur();                     
                        $scope.$apply();
                    },
                    focus: function(event, ui) {
                        event.preventDefault();
                    }
                })
                .focus(function() {
                    $(this).keydown();
                })
                .select(function() {
                    $scope.inputSelected.select = true;
                    $scope.showNoMatchingResults = false; 
                });
        }


        /***************************************  Functions for autosuggest end here **********************************************/

        $scope.svcaccDone = function () {
            $scope.goBackToAdmin = true;
            $scope.goBack();
        }


        //EDIT PERMISSION FUNCTION
        $scope.editPermission = function (type, editMode, user, permission) {
            
        }


        //DELETE PERMISSION FUNCTION 
        $scope.deletePermission = function (type, editMode, editingPermission, key, permission) {
           
              //After permission is deleted, call 
            getSvcaccInfo(iamsvcId) 
            //for fetching the iam service account details and permissions

        }
        $scope.editAWSConfigurationDetails = function (editMode, rolename) {
            if (editMode) {
                try {
                    $scope.isLoadingData = true;
                    var queryParameters = rolename;
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getAwsConfigurationDetails', queryParameters);
                    AdminSafesManagement.getAWSConfigurationDetails(null, updatedUrlOfEndPoint).then(
                        function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.editingAwsPermission = {"status": true};
                                    $scope.awsConfPopupObj = {
                                        "auth_type": response.data.auth_type,
                                        "role": rolename,
                                        "bound_account_id": response.data.bound_account_id,
                                        "bound_region": response.data.bound_region,
                                        "bound_vpc_id": response.data.bound_vpc_id,
                                        "bound_subnet_id": response.data.bound_subnet_id,
                                        "bound_ami_id": response.data.bound_ami_id,
                                        "bound_iam_instance_profile_arn": response.data.bound_iam_instance_profile_arn,
                                        "bound_iam_role_arn": response.data.bound_iam_role_arn,
                                        "policies": response.data.policies,
                                        "bound_iam_principal_arn": response.data.bound_iam_principal_arn,
                                        "resolve_aws_unique_ids": "false"
                                    };
                                    $scope.policies = response.data.policies;
                                    $scope.awsRadioBtn['value'] = $rootScope.AwsPermissionsData.data[rolename];
                                    $scope.open('md');   // open the AWS configuration popup with prefilled data
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

                    // To handle errors while calling 'fetchData' function
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }
            }
        }
        var hideUserSudoPolicy = function() {
            $scope.hideSudoPolicy = false;
            var flg = false;
            var count=0;
            Object.keys($scope.permissionData.UsersPermissionsData).forEach(function(key) {
                if ($scope.permissionData.UsersPermissionsData[key] == "sudo") {
                    flg = true;
                }
                count++;
            });
            if (count==1 && flg == true) {
                $scope.hideSudoPolicy = true;
            }
        }

        var getSvcaccInfo = function (svcaccId) {
            $scope.isLoadingData = true;
            $scope.isSvcaccExpired = false;
            $scope.expiredNote = '';
            $scope.iamSecretsData = { "secret": [] };

            var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/iamserviceaccounts/" + svcaccId;
            AdminSafesManagement.getSvcaccOnboardInfo(null, updatedUrlOfEndPoint).then(
                function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {

                        if ($rootScope.showDetails !== true) {
                            document.getElementById('addUser').value = '';
                            document.getElementById('addGroup').value = '';
                        } 
                        var managedBy = '';
                        lastContent = '';
                        try {
                            $scope.isLoadingData = false;
                            var object = response.data;
                            $scope.svcacc = {
                                appId: object.application_id || '',
                                appName: object.application_name || '',
                                appTag: object.application_tag || '',
                                awsAccId: object.awsAccountId || '',
                                awsAccName: object.awsAccountName || '',
                                ownerEmail: object.owner_email || '',
                                ownerNtId: object.owner_ntid || '',
                                userName: object.userName || '',
                                creationDate: object.createdDate || '',
                                isActivated: object.isActivated || '',
                                svcaccId: object.userName || '',
                            };

                            $scope.iamSecretsData = object.secret;  
                            managedBy = object.owner_email;
            
                            getUserDetails();
                            $scope.permissionData = {
                                UsersPermissionsData: object.users || '',
                                GroupsPermissionsData: object.groups || '',
                                AwsPermissionsData: '',
                                AppRolePermissionsData: ''
                            }
                            $scope.isLoadingData = false;
                            hideUserSudoPolicy();
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
                        $scope.error('md');
                    }
                },
                function (error) {
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                })                        
            }  
        
        
        $scope.pwdRotationChange = function() {
            $scope.autoRotate = !$scope.autoRotate;
            $scope.svcacc.autoRotate = $scope.autoRotate;
            $scope.svcacc.ttl = '';
        }

        $scope.requestDataFrChangeSvcacc = function () {
            $scope.isLoadingData = true;
            if ($stateParams.svcaccData) {
                // Prefilled values when editing
                $scope.changeSvcaccHeader = "EDIT IAM SERVICE ACCOUNT";
                $scope.isEditSvcacc = true;
                try {
                    getSvcaccInfo($stateParams.svcaccData.userId);
                } catch (e) {
                    // To handle errors while calling 'fetchData' function
                    if ($rootScope.showDetails !== true) {
                        document.getElementById('addUser').value = '';
                        document.getElementById('addGroup').value = '';
                    }
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }

            }
            else {
                if (JSON.parse(SessionStore.getItem("isAdmin")) == false) {
                    $state.go('manage');
                }
                $scope.changeSvcaccHeader = "ONBOARD SERVICE ACCOUNT";
                $scope.isEditSvcacc = false;
                try {
                    $rootScope.AwsPermissionsData = {}
                    $rootScope.AppRolePermissionsData = {}
                    $scope.isLoadingData = false;
                    getWorkloadDetails();
                } catch (e) {
                    // To handle errors while calling 'fetchData' function
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }
            }
        }

        $scope.getSvcaccInfo = function (svcaccObj) {
            $scope.svcacc = svcaccObj;
            $scope.svcacc.svcaccId = svcaccObj.userId;  
            $scope.svcacc.accountExpires = $scope.svcacc.accountExpiresFormatted;
            $scope.svcInputSelected = true;
            $scope.isCollapsed = false;
            $scope.autoRotate = false;
            document.getElementById('ttl').placeholder="TTL in seconds (Max: "+$scope.svcacc.maxPwdAge+")";
            $scope.isSvcaccExpired = false;
            $scope.expiredNote = "";
            if ($scope.svcacc.accountStatus.toLowerCase() == "expired") {
                $scope.isSvcaccExpired = true;
                $scope.expiredNote = "(Expired)";
            }
            getDefaultTTL();
            getUserDetails();
        }

        // $scope.collapseADDetails = function() {
        //     $scope.isCollapsed = !$scope.isCollapsed;          
        // }
        
        $scope.clearSvcaccId = function() {
            $scope.svcacc = {
                svcaccId: '',
                userEmail: '',
                displayName: '',
                givenName: '',
                userName: '',
                accountExpires: '',
                pwdLastSet: '',
                maxPwdAge: '',
                managedBy: {},
                passwordExpiry: '',
                accountStatus: '',
                lockStatus: '',
                creationDate: '',
                purpose: '',
                autoRotate: false,
                ttl: '' ,
                max_ttl: '',
                adGroup: '',
                appName: '',
            };
            $scope.autoRotate = false;
            $scope.svcInputSelected = false;
            $scope.isCollapsed = true;
            document.getElementById('ttl').placeholder="Password TTL in seconds";
            $scope.expiredNote = "";
            $scope.isSvcaccExpired = false;
            $scope.ttlToolip = '';
            $scope.defatulTTL = '';
            $scope.customTTL = '';
            $scope.isOwner = false;
            $scope.ownerName = '';
            $scope.ownerEmail = '';
            $scope.svceditnotes = '';
            $scope.isActivating = false;
            $scope.appNameSelected = false;
            $scope.isApplicationsLoading = true;
            $scope.applicationName = '';
        }

        $scope.getSvcaccList = function(searchVal) {
            $scope.svcInputSelected = false;
            if (searchVal.length >2) {
                $scope.showInputLoader.show = true;
                console.log("getServiceAccounts")
                var queryParameters = "serviceAccountName=" + searchVal;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getServiceAccounts', queryParameters);
                return AdminSafesManagement.getServiceAccounts(null, updatedUrlOfEndPoint).then(
                    function(response) {
                        if(UtilityService.ifAPIRequestSuccessful(response)){
                            $scope.svcaccList = response.data.data.values;
                            $scope.showInputLoader.show = false;
                            return orderByFilter(filterFilter($scope.svcaccList, searchVal), 'userId', true);
                        }
                        else {
                            $scope.showInputLoader.show = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        }
                    },
                    function(error) {
                        // Error handling function
                        console.log(error);
                        $scope.showInputLoader.show = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                });
            }
        }

        $scope.init = function () {
            if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            var feature = JSON.parse(SessionStore.getItem("feature"));
            if (feature.adpwdrotation == false) {
                if (JSON.parse(SessionStore.getItem("isAdmin")) == false) {
                    $state.go('manage');
                }
                else {
                    $state.go('admin');
                }
            }
            $scope.svcacc = {
                svcaccId: '',
                userEmail: '',
                displayName: '',
                givenName: '',
                userName: '',
                accountExpires: '',
                pwdLastSet: '',
                maxPwdAge: '',
                managedBy: {},
                passwordExpiry: '',
                accountStatus: '',
                lockStatus: '',
                creationDate: '',               
                purpose: '',
                autoRotate: false,
                ttl: '' ,    
                max_ttl: '',
                adGroup: '',
                appName: '',
            };
            $scope.permissionData = {
                UsersPermissionsData: '',
                GroupsPermissionsData: '',
                AwsPermissionsData: '',
                AppRolePermissionsData: ''
            }
            $scope.newPassword = '';
            $scope.hideSudoPolicy = false;
            $scope.ttlToolip = '';
            $scope.defatulTTL = '';
            $scope.ownerName = '';
            $scope.ownerEmail = '';
            $scope.customTTL = '';
            $scope.isOwner = false;
            $scope.svceditnotes = '';
            $scope.permissionChangeInProgress = false;
            $scope.isActivating = false;
            $scope.adGroupName = '';
            $scope.appNameSelected = false;
            $scope.applicationName = '';
            $scope.isApplicationsLoading = true;
            $scope.myVaultKey = SessionStore.getItem("myVaultKey");
            if(!$scope.myVaultKey){ /* Check if user is in the same session */
                $state.go('/');
            }
            $scope.appNameTableOptions = [];
            $scope.requestDataFrChangeSvcacc();
            $scope.fetchUsers();
            $scope.fetchGroups();
        }

        var getWorkloadDetails = function () {
            $scope.isApplicationsLoading = true;
            AdminSafesManagement.getApprolesFromCwm().then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    $scope.isApplicationsLoading = false;
                    var data = response.data;
                    $scope.appNameTableOptions = [];
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
                    if ($scope.applicationName =="" || $scope.applicationName ==null || $scope.applicationName == undefined) {
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

        $scope.getAppName = function (searchName) {
            return orderByFilter(filterFilter($scope.appNameTableOptions, searchName), 'name', true);
        }
        $scope.userNameValEmpty = false;
        $scope.grpNameValEmpty = false;

        $scope.fetchUsers = function () {

        }

        $scope.fetchGroups = function () {

        }
        $scope.enableEc2Controls = function (e) {
            angular.element(document.getElementById('bound_account_id'))[0].disabled = false;
            angular.element(document.getElementById('bound_region'))[0].disabled = false;
            angular.element(document.getElementById('bound_vpc_id'))[0].disabled = false;
            angular.element(document.getElementById('bound_subnet_id'))[0].disabled = false;
            angular.element(document.getElementById('bound_ami_id'))[0].disabled = false;
            angular.element(document.getElementById('bound_iam_instance_profile_arn'))[0].disabled = false;
            angular.element(document.getElementById('bound_iam_role_arn'))[0].disabled = false;
            angular.element(document.getElementById('bound_iam_principal_arn'))[0].disabled = true;
        }

        $scope.enableIamControls = function (e) {
            angular.element(document.getElementById('bound_account_id'))[0].disabled = true;
            angular.element(document.getElementById('bound_region'))[0].disabled = true;
            angular.element(document.getElementById('bound_vpc_id'))[0].disabled = true;
            angular.element(document.getElementById('bound_subnet_id'))[0].disabled = true;
            angular.element(document.getElementById('bound_ami_id'))[0].disabled = true;
            angular.element(document.getElementById('bound_iam_instance_profile_arn'))[0].disabled = true;
            angular.element(document.getElementById('bound_iam_role_arn'))[0].disabled = true;
            angular.element(document.getElementById('bound_iam_principal_arn'))[0].disabled = false;
        }

        //ADD USER FUNCTION : 
        $scope.addPermission = function (type, key, permission, editingPermission) {
            

            //After permission is added, call 
            getSvcaccInfo(iamsvcId) 
            //for fetching the iam service account details and permissions
            
        }

        $scope.newAwsConfiguration = function (size) {
            // To reset the aws configuration details object to create a new one
            $scope.editingAwsPermission = {"status": false};
            $scope.awsConfPopupObj = {
                "auth_type":"",
                "role": "",
                "bound_account_id": "",
                "bound_region": "",
                "bound_vpc_id": "",
                "bound_subnet_id": "",
                "bound_ami_id": "",
                "bound_iam_instance_profile_arn": "",
                "bound_iam_role_arn": "",
                "policies": "",
                "bound_iam_principal_arn": "",
                "resolve_aws_unique_ids": "false"
            };
            $scope.open(size);
        }


        //APPROLE FUNCTION:
        $scope.addApproleToSafe = function (size) {
           
        }

        /* TODO: What is open, functon name should be more descriptive */
        $scope.open = function (size) {
            Modal.createModal(size, 'changeSafePopup.html', 'ChangeIamServiceAccountCtrl', $scope);
        };

        /* TODO: What is open, functon name should be more descriptive */
        $scope.openApprole = function (size) {
            Modal.createModal(size, 'appRolePopup.html', 'ChangeIamServiceAccountCtrl', $scope);
        };
//
        $scope.openResetStatus = function (size) {
            Modal.createModal(size, 'resetStatus.html', 'ChangeIamServiceAccountCtrl', $scope);
        }
//
        $scope.openOneTimeResetFailedMessage = function (size) {
            Modal.createModal(size, 'openOneTimeResetFailedMessage.html', 'ChangeIamServiceAccountCtrl', $scope);
        };
//
        $scope.oneTimeResetConfirmation = function (size) {
            Modal.createModal(size, 'oneTimeResetConfirmation.html', 'ChangeIamServiceAccountCtrl', $scope);
        }
        
        /* TODO: What is ok, functon name should be more descriptive */
        $scope.ok = function () {
            Modal.close('ok');
            $scope.isLoadingData = false;
        };

        /* TODO: What is next, functon name should be more descriptive */
        $scope.next = function () {
            $scope.addAWSRoleSafe();
            // $scope.openAWSConfFinal('md');

        };

        /* TODO: What is cancel, functon name should be more descriptive */
        $scope.cancel = function () {
            Modal.close('close');
            $scope.isLoadingData = false;
        };

        $scope.onboardingDone = function () {
            Modal.close('close');
            if ($scope.isLoadingData == true) {
                Notifications.toast("Loading Service Account Details..");
            }
        }

        var getUserDetails = function () {
            $scope.ownerName = $scope.svcacc.userName;
            $scope.ownerEmail = $scope.svcacc.owner_email;
        }

        $scope.grantPermission = function (svcaccname)  {
            //getMetadata(svcaccname);
            $rootScope.showDetails = false;
            $rootScope.activeDetailsTab = 'permissions';
        }

        $scope.init();

    });
})(angular.module('vault.features.ChangeIamServiceAccountCtrl', [
    'vault.services.AdminSafesManagement',
    'vault.services.ModifyUrl',
    'vault.constants.AppConstant'
]));
