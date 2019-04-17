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
    app.controller('ChangeServiceAccountCtrl', function ($scope, $rootScope, Modal, $timeout, fetchData, $http, UtilityService, Notifications, $window, $state, $stateParams, $q, SessionStore, vaultUtilityService, ModifyUrl, AdminSafesManagement, AppConstant, $filter, filterFilter, orderByFilter) {
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
            show: true
        }];

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

        $scope.error = function (size) {
            Modal.createModal(size, 'error.html', 'ChangeSafeCtrl', $scope);
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

        $scope.editPermission = function (type, editMode, user, permission) {
            if (permission === "reset") {
                permission = "write";
            }
            if (editMode) {
                var editingPermission = true;
                $scope.deletePermission(type, editMode, editingPermission, user, permission);
            }
        }

        $scope.deletePermission = function (type, editMode, editingPermission, key, permission) {
            if (permission === "reset") {
                permission = "write";
            }
            if (editMode) {
                try {
                    key = key.replace($scope.domainName, '');
                    $scope.isLoadingData = true;
                    var svcaccname = $scope.svcacc.svcaccId;
                    var apiCallFunction = '';
                    var reqObjtobeSent = {};
                    switch (type) {
                        case 'users' :
                            apiCallFunction = AdminSafesManagement.deleteUserPermissionFromSvcacc;
                            if (editingPermission) {
                                reqObjtobeSent = {
                                    "svcAccName": svcaccname,
                                    "username": key,
                                    "access": permission
                                };
                            }
                            else {
                                reqObjtobeSent = {
                                    "svcAccName": svcaccname,
                                    "username": key,
                                    "access": permission
                                };
                            }
                            break;
                        case 'groups' :
                            apiCallFunction = AdminSafesManagement.deleteGroupPermissionFromSvcacc;
                            reqObjtobeSent = {
                                "svcAccName": svcaccname,
                                "groupname": key,
                                "access": permission
                            };
                            break;
                        case 'AWSPermission' :
                            apiCallFunction = AdminSafesManagement.detachAWSPermissionFromSvcacc;
                            reqObjtobeSent = {
                                "svcAccName": svcaccname,
                                "rolename": key,
                                "access": permission
                            };
                            break;
                        case 'AppRolePermission' :
                            apiCallFunction = AdminSafesManagement.detachAppRolePermissionFromSvcacc;
                            reqObjtobeSent = {
                                "svcAccName": svcaccname,
                                "approlename": key,
                                "access": permission
                            };
                            break;
                    }
                    apiCallFunction(reqObjtobeSent).then(
                        function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.isLoadingData = false;
                                    if (editingPermission) {
                                        $scope.addPermission(type, key, permission, true);  // This will be executed when we're editing permissions
                                    }
                                    else {
                                        getMetadata(svcaccname); 
                                        if (type === "users" && key === SessionStore.getItem("username")) {
                                            return Modal.createModalWithController('stop.modal.html', {
                                                title: 'Permission changed',
                                                message: 'For security reasons, if you add or modify permission to yourself, you need to log out and log in again for the added or modified permissions to take effect.'
                                              });
                                        }
                                        if (type === 'AppRolePermission') {
                                            // delete approle
                                        }
                                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_SAFE_DELETE');
                                        Notifications.toast(key + "'s permission" + notification);
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
                } catch (e) {

                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }
            }
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
                } catch (e) {

                    // To handle errors while calling 'fetchData' function
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }
            }
        }

        $scope.onboardSvcacc = function () {
            if ($scope.svcaccOnboarded === true) {
                if(!angular.equals($scope.svcaccPrevious, $scope.svcacc)) {
                    $scope.editSvcaccOnboard();
                } else {                    
                    $rootScope.showDetails = false;               // To show the 'permissions' and hide the 'details'
                    $rootScope.activeDetailsTab = 'permissions';                    
                }
            } else {
                try {
                    $scope.isLoadingData = true;
                    
                    var onboardPayload = {
                        "name": $scope.svcacc.svcaccId,
                        "autoRotate": $scope.svcacc.autoRotate,
                        "ttl": $scope.svcacc.ttl,
                        "max_ttl": $scope.svcacc.ttl,
                        "owner":  $scope.svcacc.owner || 'safeadmin'
                    }
                    AdminSafesManagement.onboardSvcacc(onboardPayload, '').then(function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            // Try-Catch block to catch errors if there is any change in object structure in the response
                            try {
                                $scope.isLoadingData = false;
                                $rootScope.showDetails = false;
                                $rootScope.activeDetailsTab = 'permissions';
                                $scope.svcaccOnboarded = true;
                                var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ONBOARD_SUCCESS');
                                Notifications.toast($scope.svcacc.svcaccId + ' Service Account' + notification);
                                $scope.svcaccPrevious = angular.copy($scope.svcacc);

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
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }
            }
        }

        $scope.editSvcaccOnboard = function () {
            try {
                $scope.isLoadingData = true;
                var queryParameters = $scope.svcacc.name;
                var payload = {"data": $scope.svcacc};
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('editSvcacc', queryParameters);
                AdminSafesManagement.editSvcacc(payload, updatedUrlOfEndPoint).then(function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            // Try-Catch block to catch errors if there is any change in object structure in the response
                            try {
                                $scope.isLoadingData = false;
                                $rootScope.showDetails = false;               // To show the 'permissions' and hide the 'details'
                                $rootScope.activeDetailsTab = 'permissions';
                                var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_UPDATE_SUCCESS');
                                Notifications.toast($scope.svcacc.name + ' Service account' + notification);
                                $scope.svcaccPrevious = angular.copy($scope.svcacc);
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

        var getMetadata = function(svcaccId) {
            $scope.isLoadingData = true;
            var queryParameters = "path=ad/roles/"+svcaccId;
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccMetadata', queryParameters);
            AdminSafesManagement.getSvcaccMetadata(null, updatedUrlOfEndPoint).then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        // Try-Catch block to catch errors if there is any change in object structure in the response
                        try {
                            $scope.isLoadingData = false;
                            if (response.data.data) {
                                var object = response.data.data;
                                $scope.permissionData.UsersPermissionsData =  object.users || '';

                                $scope.permissionData.GroupsPermissionsData =  object.groups ||'';

                                $scope.permissionData.AwsPermissionsData = {
                                    "data": object['aws-roles']
                                }
                                $scope.permissionData.AppRolePermissionsData = {
                                    "data": object['app-roles']
                                }
                                hideUserSudoPolicy();
                            }

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
                },
                function (error) {
                    // Error handling function
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                })
        }

        $rootScope.goToPermissions = function () {
            $timeout(function () {
                if ($scope.isEditSvcacc) {
                    if(!angular.equals($scope.svcaccPrevious, $scope.svcacc)){
                        $scope.editSvcaccOnboard();
                    }
                    $rootScope.showDetails = false;               // To show the 'permissions' and hide the 'details'
                    $rootScope.activeDetailsTab = 'permissions';                                           
                }
                else {
                    $scope.onboardSvcacc();
                }
            })
        }

        var getSvcaccInfo = function (svcaccId) {
            $scope.isLoadingData = true;
            $scope.isSvcaccExpired = false;
            $scope.expiredNote = '';
            var queryParameters = "serviceAccountName="+svcaccId+"&excludeOnboarded=false";
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccInfo', queryParameters);
            AdminSafesManagement.getSvcaccInfo(null, updatedUrlOfEndPoint).then(
                function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        
                        var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSvcaccOnboardInfo', svcaccId);
                        AdminSafesManagement.getSvcaccOnboardInfo(null, updatedUrlOfEndPoint).then(
                            function (onboardResponse) {
                                if (UtilityService.ifAPIRequestSuccessful(response)) {
                                    var onboardInfo = onboardResponse.data;
                                    if ($rootScope.showDetails !== true) {
                                        document.getElementById('addUser').value = '';
                                        document.getElementById('addGroup').value = '';
                                    }
                                    $scope.searchValue = {
                                        userName: '',
                                        groupName: ''
                                    };
                                    var managedBy = '';
                                    lastContent = '';
                                    try {
                                        $scope.isLoadingData = false;
                                        if (response.data.data.values.length >0) {
                                            var object = response.data.data.values[0];

                                            $scope.svcacc = {
                                                svcaccId: object.userId || '',
                                                userEmail: object.userEmail || '',
                                                displayName: object.displayName || '',
                                                givenName: object.givenName || '',
                                                userName: object.userName || '',
                                                accountExpires: object.accountExpires || '',
                                                pwdLastSet: object.pwdLastSet || '',
                                                maxPwdAge: object.maxPwdAge || '',
                                                managedBy: object.managedBy|| '',
                                                passwordExpiry: object.passwordExpiry || '',
                                                accountStatus: object.accountStatus || '',
                                                lockStatus: object.lockStatus || '',
                                                creationDate: object.creationDate || '',
                                                purpose: object.purpose || '',
                                                autoRotate: false,
                                                ttl: onboardInfo.ttl || '' ,
                                                max_ttl: '',
                                            };
                                            managedBy = object.managedBy;
                                            if (onboardInfo.ttl && onboardInfo.ttl != null) {
                                                $scope.svcacc.autoRotate = true;
                                            }
                                            $scope.autoRotate = $scope.svcacc.autoRotate;
                                            $scope.svcaccPrevious = angular.copy($scope.svcacc);
                                            if ($scope.svcacc.accountExpires == "expired") {
                                                $scope.isSvcaccExpired = true;
                                                $scope.expiredNote = "(Expired)";
                                            }
                                            getMetadata($stateParams.svcaccData.userId);
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
                                  console.log(error);
                                $scope.isLoadingData = false;
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                $scope.error('md');
                            })                        
                    }
                    else {
                        $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                        error('md');
                    }
            },
            function (error) {
                // Error handling function
                if ($rootScope.showDetails !== true) {
                    document.getElementById('addUser').value = '';
                    document.getElementById('addGroup').value = '';
                }
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
                $scope.changeSvcaccHeader = "EDIT SERVICE ACCOUNT";
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
                $scope.changeSvcaccHeader = "ONBOARD SERVICE ACCOUNT";
                $scope.isEditSvcacc = false;
                try {
                    $rootScope.AwsPermissionsData = {}
                    $rootScope.AppRolePermissionsData = {}
                    $scope.isLoadingData = false;
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
            $scope.svcInputSelected = true;
            $scope.isCollapsed = false;
            $scope.autoRotate = false;
        }

        $scope.collapseADDetails = function() {
            $scope.isCollapsed = !$scope.isCollapsed;          
        }
        
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
            };
            $scope.autoRotate = false;
            $scope.svcInputSelected = false;
            $scope.isCollapsed = true;
        }

        $scope.getSvcaccList = function(searchVal) {
            $scope.svcInputSelected = false;
            if (searchVal.length >2) {
                $scope.showInputLoader.show = true;
                var queryParameters = "serviceAccountName=svc";
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
            };
            $scope.permissionData = {
                UsersPermissionsData: '',
                GroupsPermissionsData: '',
                AwsPermissionsData: '',
                AppRolePermissionsData: ''
            }
            $scope.hideSudoPolicy = false;
            $scope.myVaultKey = SessionStore.getItem("myVaultKey");
            if(!$scope.myVaultKey){ /* Check if user is in the same session */
                $state.go('signup');
            }
            $scope.requestDataFrChangeSvcacc();
            $scope.fetchUsers();
            $scope.fetchGroups();
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

        $scope.addPermission = function (type, key, permission, editingPermission) {
            if (permission === "reset") {
                permission = "write";
            }
            if ((key != '' && key != undefined) || type == 'AwsRoleConfigure') {
                try {
                    if (type === "users" && !editingPermission) {
                        key = document.getElementById('addUser').value.toLowerCase();
                    }
                    if (type === "groups" && !editingPermission) {
                        key = document.getElementById('addGroup').value.toLowerCase();
                    }
                    Modal.close('');
                    $scope.isLoadingData = true;
                    $scope.showInputLoader.show = false;
                    $scope.showNoMatchingResults = false;
                    var svcaccname = $scope.svcacc.svcaccId;
                    var apiCallFunction = '';
                    var reqObjtobeSent = {};
                    // extract only userId/groupId from key
                    if (key.includes($scope.domainName)) {
                        key = key.split('@')[0];
                    }
                    if (key !== null && key !== undefined) {
                        key = UtilityService.formatName(key);
                    }
                    if ($scope.awsConfPopupObj.role !== null && $scope.awsConfPopupObj.role !== undefined) {
                        $scope.awsConfPopupObj.role = UtilityService.formatName($scope.awsConfPopupObj.role);
                    }
                    if ($scope.awsConfPopupObj.bound_region !== null && $scope.awsConfPopupObj.bound_region !== undefined) {
                        $scope.awsConfPopupObj.bound_region = UtilityService.formatName($scope.awsConfPopupObj.bound_region);
                    }
                    var updatedUrlOfEndPoint = "";
                    switch (type) {
                        case 'users' :
                            apiCallFunction = AdminSafesManagement.addUserPermissionForSvcacc;
                            reqObjtobeSent = {"svcAccName": svcaccname, "username": key, "access": permission.toLowerCase()};
                            break;
                        case 'groups' :
                            apiCallFunction = AdminSafesManagement.addGroupPermissionForSvcacc;
                            reqObjtobeSent = {"svcAccName": svcaccname, "groupname": key, "access": permission.toLowerCase()};
                            break;
                        case 'AWSPermission' :
                            apiCallFunction = AdminSafesManagement.addAWSPermissionForSvcacc;
                            reqObjtobeSent = {"svcAccName": svcaccname, "rolename": key, "access": permission.toLowerCase()};
                            break;
                        case 'AwsRoleConfigure' :
                            $scope.awsConfPopupObj['policies'] = "";   // Todo: Because of unavailability of edit service, this has been put
                            // Validate the input here if requried...
                            if ($scope.awsConfPopupObj.auth_type === 'ec2') {
                                $scope.awsConfPopupObj.bound_iam_principal_arn = "";
                                apiCallFunction = AdminSafesManagement.createAwsRoleSvcacc;
                            }
                            else {
                                $scope.awsConfPopupObj['policies'] = [];
                                $scope.awsConfPopupObj.bound_account_id = "";
                                $scope.awsConfPopupObj.bound_region = "";
                                $scope.awsConfPopupObj.bound_vpc_id = "";
                                $scope.awsConfPopupObj.bound_subnet_id = "";
                                $scope.awsConfPopupObj.bound_ami_id = "";
                                $scope.awsConfPopupObj.bound_iam_instance_profile_arn = "";
                                $scope.awsConfPopupObj.bound_iam_role_arn = "";
                                var arn = [];
                                arn.push($scope.awsConfPopupObj.bound_iam_principal_arn);
                                $scope.awsConfPopupObj.bound_iam_principal_arn = arn;
                                apiCallFunction = AdminSafesManagement.createAwsIAMRoleSvcacc;
                            }
                            reqObjtobeSent = $scope.awsConfPopupObj
                            break;
                        case 'AppRolePermission' :
                            apiCallFunction = AdminSafesManagement.addAppRolePermissionForSvcacc;
                            reqObjtobeSent = {"svcAccName": svcaccname, "approlename": key, "access": permission.toLowerCase()};
                            break;
                                }
                    apiCallFunction(reqObjtobeSent, updatedUrlOfEndPoint).then(function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.isLoadingData = false;
                                    if (type === 'AwsRoleConfigure') {
                                        $scope.addPermission('AWSPermission', $scope.awsConfPopupObj.role, permission, false);
                                    }
                                    else {
                                        getMetadata(svcaccname);
                                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                                        if (key !== null && key !== undefined) {
                                            document.getElementById('addUser').value = '';
                                            document.getElementById('addGroup').value = '';
                                            if (type === "users" && key === SessionStore.getItem("username")) {
                                                return Modal.createModalWithController('stop.modal.html', {
                                                    title: 'Permission changed',
                                                    message: 'For security reasons, if you add or modify permission to yourself, you need to log out and log in again for the added or modified permissions to take effect.'
                                                  });
                                            }
                                            Notifications.toast(key + "'s permission" + notification);                                            
                                        }
                                        
                                    }
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
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
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

        $scope.addApproleToSafe = function (size) {
            // To reset the aws configuration details object to create a new one
            $scope.editingApprolePermission = {"status": false};
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
            $scope.roleNameSelected = false;
            $scope.roleNameTableOptions = [];
            AdminSafesManagement.getApproles().then(function (response) {
                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    var keys = response.data.keys +'';
                    var roles = keys.split(',');
                    for (var index = 0;index<roles.length;index++) {
                        $scope.roleNameTableOptions.push({"type":roles[index]});
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

            $scope.dropDownRoleNames = {
                'selectedGroupOption': {"type": "Select Role Name"},       // As initial placeholder
                'tableOptions': $scope.roleNameTableOptions
            }
            $scope.openApprole(size);
        }

        /* TODO: What is open, functon name should be more descriptive */
        $scope.open = function (size) {
            Modal.createModal(size, 'changeSafePopup.html', 'ChangeSafeCtrl', $scope);
        };

        /* TODO: What is open, functon name should be more descriptive */
        $scope.openApprole = function (size) {
            Modal.createModal(size, 'appRolePopup.html', 'ChangeSafeCtrl', $scope);
        };

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

        

        $scope.init();

    });
})(angular.module('vault.features.ChangeServiceAccountCtrl', [
    'vault.services.AdminSafesManagement',
    'vault.services.ModifyUrl',
    'vault.constants.AppConstant'
]));
