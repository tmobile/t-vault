/*
* =========================================================================
* Copyright 2018 T-Mobile, US
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
    app.controller('ChangeSafeCtrl', function ($scope, $rootScope, Modal, $timeout, fetchData, $http, UtilityService, Notifications, $window, $state, $stateParams, $q, SessionStore, vaultUtilityService, ModifyUrl, AdminSafesManagement, AppConstant) {
        $scope.selectedGroupOption = '';            // Selected dropdown value to be used for filtering
        $rootScope.showDetails = true;              // Set true to show details view first
        $scope.similarSafes = 0;
        $rootScope.activeDetailsTab = 'details';
        $scope.typeDropdownDisable = false;         // Variable to be set to disable the dropdown button to select 'type'
        $scope.safeCreated = false;                 // Flag to indicate if a safe has been creted
        $scope.isEditSafe = false;
        $scope.awsRadioBtn = {};                    // Made an object instead of single variable, to have two way binding between
                                                    // modal and controller

        $scope.usrRadioBtnVal = 'read';             // Keep it in lowercase
        $scope.grpRadioBtnVal = 'read';             // Keep it in lowercase
        $scope.awsRadioBtn['value'] = 'read';       // Keep it in lowercase
        $scope.isEmpty = UtilityService.isObjectEmpty;
        $scope.awsConfPopupObj = {
            "role": "",
            "bound_account_id": "",
            "bound_region": "",
            "bound_vpc_id": "",
            "bound_subnet_id": "",
            "bound_ami_id": "",
            "bound_iam_instance_profile_arn": "",
            "bound_iam_role_arn": "",
            "policies": ""
        };
        $scope.tableOptions = [
            {
                "type": "User Safe"
            }, {
                "type": "Shared Safe"
            }, {
                "type": "Application Safe"
            }
        ];

        $scope.radio = {
            value: 'read',
            options: [{
                'text': 'read'
            }, {
                'text': 'write'
            }, {
                'text': 'deny'
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


        $scope.goBack = function () {
            if ($scope.goBackToAdmin !== true) {
                if ($rootScope.showDetails === true) {
                    $state.go('admin');
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
                    $state.go('admin');
            }
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
        var assignDropdownVal = function (variableChanged) {
            if (variableChanged === 'userName') {
                $scope.userNameDropdownVal = [];
            } else if (variableChanged === 'groupName') {
                $scope.groupNameDropdownVal = [];
            }
           
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

        $scope.clearInputValue = function(id) {
            document.getElementById(id).value = "";
            $scope.inputSelected.select = false;
            $scope.searchValue = {
                userName: '',
                groupName: ''
            };
            lastContent = '';
        }
        // function call on input keyup 
        $scope.onKeyUp = function(newVal, variableChanged) {
            $scope.emptyResponse = false;        
            $scope.showInputLoader.show = false;
            $scope.inputSelected.select = false;
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
                if (newLetter.length === 1) {
                    initiateAutoComplete(variableChanged, ['loading']);
                }
          delay(function(){
            duplicateFilter(newLetter, function(value){
                $scope.showInputLoader.show = true;
                $scope.getDropdownDataForPermissions(variableChanged, value);                
            });          
          }, 500 ); // delay of 500ms provided before making api call
        }

        $scope.getDropdownDataForPermissions = function (searchFieldName, searchFieldText) {      
            if (searchFieldText.length > 2) {
                vaultUtilityService.getDropdownDataForPermissions(searchFieldName, searchFieldText).then(function (res, error) {
                    var serviceData;
                    if (res) {
                        serviceData = res;
                        $scope.loadingDataFrDropdown = serviceData.loadingDataFrDropdown;
                        $scope.erroredFrDropdown = serviceData.erroredFrDropdown;
                        $scope.successFrDropdown = serviceData.successFrDropdown;
                        if (serviceData.response.data.data.values.length === 0) {
                            $scope.emptyResponse = true;
                        }
                        massageDataFrPermissionsDropdown(searchFieldName, searchFieldText, serviceData.response.data.data.values);
                        $scope.$apply();
                    } else {
                        serviceData = error;
                        $scope.commonErrorHandler(serviceData.error, serviceData.error || serviceData.response.data, "getDropdownData");

                    }
                },
                function (error) {
                    // Error handling function when api fails
                    if(error.status !== 200 && (error.xhrStatus === 'error' || error.xhrStatus === 'complete')) {
                        $scope.showInputLoader.show = false;
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
        var massageDataFrPermissionsDropdown = function (searchFieldName, searchFieldText, dataFrmApi) {
            var serviceData = vaultUtilityService.massageDataFrPermissionsDropdown(searchFieldName, searchFieldText, dataFrmApi);
            $scope.showInputLoader.show = false;
               if (searchFieldName === 'userName') {
                    $scope.userNameDropdownVal = serviceData.sort();
                    initiateAutoComplete(searchFieldName, $scope.userNameDropdownVal);
                } else if (searchFieldName === 'groupName') {
                    $scope.groupNameDropdownVal = serviceData.sort();
                    initiateAutoComplete(searchFieldName, $scope.groupNameDropdownVal);
                }        
           
            $rootScope.loadingDropDownData = false;
        }

        var initiateAutoComplete = function(searchFieldName, data) {
            var id;
            if (searchFieldName === "userName") {
                id = '#addUser';
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
                            if (searchFieldName === "userName") {
                                this.value = ui.item.value.split(' - ')[1];
                            } else if (searchFieldName === "groupName") {
                                this.value = ui.item.value.split(' - ')[0];
                            }                 
                        }
                        $scope.inputSelected.select = true;                   
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
                });
        }


        /***************************************  Functions for autosuggest end here **********************************************/

        $scope.safeEditSafe = function () {
            $scope.goBackToAdmin = true;
            var successCondition = true;
            $scope.goBack();
        }
        $scope.getPath = function () {
            var vaultType = '';

            switch ($scope.dropDownOptions.selectedGroupOption.type) {
                case "Application Safe":
                    vaultType = 'apps';
                    break;
                case "User Safe":
                    vaultType = 'users';
                    break;
                case "Shared Safe":
                    vaultType = 'shared';
                    break;
            }

            var setPath = vaultType + '/' + UtilityService.formatName($scope.safe.name);
            //   return encodeURIComponent(setPath);
            return setPath;
        }
        $scope.editPermission = function (type, editMode, user, permission) {
            if (editMode) {
                var editingPermission = true;
                $scope.deletePermission(type, editMode, editingPermission, user, permission);
            }
        }
        $scope.replaceSpaces = function () {
            $scope.safe.name = UtilityService.formatName($scope.safe.name);
        }

        $scope.deletePermission = function (type, editMode, editingPermission, key, permission) {
            if (editMode) {
                try {
                    key = key.replace($scope.domainName, '');
                    $scope.isLoadingData = true;
                    var setPath = $scope.getPath();
                    var apiCallFunction = '';
                    var reqObjtobeSent = {};
                    switch (type) {
                        case 'users' :
                            apiCallFunction = AdminSafesManagement.deleteUserPermissionFromSafe;
                            reqObjtobeSent = {
                                "path": setPath,
                                "username": key
                            };
                            break;
                        case 'groups' :
                            apiCallFunction = AdminSafesManagement.deleteGroupPermissionFromSafe;
                            reqObjtobeSent = {
                                "path": setPath,
                                "groupname": key
                            };
                            break;
                        case 'AWSPermission' :
                            apiCallFunction = AdminSafesManagement.deleteAWSPermissionFromSafe;
                            reqObjtobeSent = {
                                "path": setPath,
                                "role": key
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
                                        $scope.requestDataFrChangeSafe();
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
                    var setPath = $scope.getPath();
                    var queryParameters = rolename;
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getAwsConfigurationDetails', queryParameters);
                    AdminSafesManagement.getAWSConfigurationDetails(null, updatedUrlOfEndPoint).then(
                        function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    // $scope.awsConfPopupObj = response.data;
                                    // $scope.awsConfPopupObj['role'] = rolename;
                                    $scope.editingAwsPermission = {"status": true};
                                    $scope.awsConfPopupObj = {
                                        "role": rolename,
                                        "bound_account_id": response.data.bound_account_id,
                                        "bound_region": response.data.bound_region,
                                        "bound_vpc_id": response.data.bound_vpc_id,
                                        "bound_subnet_id": response.data.bound_subnet_id,
                                        "bound_ami_id": response.data.bound_ami_id,
                                        "bound_iam_instance_profile_arn": response.data.bound_iam_instance_profile_arn,
                                        "bound_iam_role_arn": response.data.bound_iam_role_arn,
                                        "policies": response.data.policies
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

        $scope.createSafe = function () {
            if ($scope.safeCreated === true) {
                $scope.editSafe();
            }
            else if ($scope.dropDownOptions.selectedGroupOption.type === "Select Type") {
                $rootScope.noTypeSelected = true;
            }
            else {
                try {
                    $scope.isLoadingData = true;
                    var setPath = $scope.getPath();
                    $scope.safe.name = UtilityService.formatName($scope.safe.name);
                    var payload = {"path": setPath, "data": $scope.safe};

                    AdminSafesManagement.createSafe(payload).then(function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.isLoadingData = false;
                                    $rootScope.showDetails = false;               // To show the 'permissions' and hide the 'details'
                                    $rootScope.activeDetailsTab = 'permissions';
                                    $scope.safeCreated = true;                // Flag set to indicate safe has been created
                                    $scope.typeDropdownDisable = true;
                                    var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_CREATE_SUCCESS');
                                    var currentSafesList = JSON.parse(SessionStore.getItem("allSafes"));
                                    currentSafesList.push($scope.safe.name);
                                    SessionStore.setItem("allSafes", JSON.stringify(currentSafesList));
                                    Notifications.toast($scope.safe.name + ' safe' + notification);
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
        }

        $scope.editSafe = function () {
            try {
                $scope.isLoadingData = true;
                var setPath = $scope.getPath();
                var payload = {"path": setPath, "data": $scope.safe};
                AdminSafesManagement.editSafe(payload).then(function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            // Try-Catch block to catch errors if there is any change in object structure in the response
                            try {
                                $scope.isLoadingData = false;
                                $rootScope.showDetails = false;               // To show the 'permissions' and hide the 'details'
                                $rootScope.activeDetailsTab = 'permissions';
                                var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_UPDATE_SUCCESS');
                                Notifications.toast($scope.safe.name + ' safe' + notification);
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


        $rootScope.goToPermissions = function () {
            $timeout(function () {
                if ($scope.isEditSafe) {
                    $rootScope.showDetails = false;               // To show the 'permissions' and hide the 'details'
                    $rootScope.activeDetailsTab = 'permissions';
                    if(!angular.equals($scope.safePrevious, $scope.safe)){
                        $scope.editSafe();
                    }                        
                }
                else {
                    $rootScope.noTypeSelected = false;
                    $scope.createSafe();
                }
            })
        }


        $scope.requestDataFrChangeSafe = function () {
            $scope.isLoadingData = true;
            if ($stateParams.safeObject) {

                // Prefilled values when editing
                $scope.changeSafeHeader = "EDIT SAFE";
                $scope.isEditSafe = true;
                $scope.typeDropdownDisable = true;
                try {
                    var queryParameters = "path=" + $stateParams.safeObject.safeType + '/' + $stateParams.safeObject.safe;
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSafeInfo', queryParameters);
                    AdminSafesManagement.getSafeInfo(null, updatedUrlOfEndPoint).then(
                        function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {

                                if ($rootScope.showDetails !== true) {
                                    document.getElementById('addUser').value = '';
                                    document.getElementById('addGroup').value = '';
                                }
                                $scope.inputSelected.select = false;
                                $scope.searchValue = {
                                    userName: '',
                                    groupName: ''
                                };
                                lastContent = '';
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.isLoadingData = false;
                                    var object = response.data.data;
                                    if(object && object.users && UtilityService.getAppConstant('AUTH_TYPE').toLowerCase() === "ldap1900") {
                                        var data = object.users;
                                        // get all object keys and iterate over them
                                            Object.keys(object.users).forEach(function(ele) {
                                                ele = ele.replace($scope.domainName, '');
                                                var newEle = ele + $scope.domainName;
                                                data[newEle] = data[ele];
                                                delete data[ele];
                                            })
                                            object.users = data;
                                    }
                                    $scope.UsersPermissionsData = object.users;
                                    $scope.GroupsPermissionsData = object.groups;
                                    $rootScope.AwsPermissionsData = {
                                        "data": object['aws-roles']
                                    }
                                    $scope.safe = {
                                        name: decodeURIComponent(object.name) || $stateParams.safeObject.safe,
                                        owner: decodeURIComponent(object.owner) || $stateParams.safeObject.owner || '',
                                        description: decodeURIComponent(object.description) || $stateParams.safeObject.description || '',
                                        type: decodeURIComponent(object.type) || $stateParams.safeObject.type || $scope.dropDownOptions.selectedGroupOption.type || ''
                                    }
                                    $scope.safePrevious = angular.copy($scope.safe);
                                    $scope.selectedGroupOption = $scope.safe;
                                    $scope.dropDownOptions = {
                                        'selectedGroupOption': $scope.selectedGroupOption,
                                        'tableOptions': $scope.tableOptions
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
                            if ($rootScope.showDetails !== true) {
                                document.getElementById('addUser').value = '';
                                document.getElementById('addGroup').value = '';
                            }
                            console.log(error);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');

                        })
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
                $scope.changeSafeHeader = "CREATE SAFE";
                $scope.isEditSafe = false;

                // Refreshing the data while adding/deleting/editing permissions when creating safe (not edit-safe)

                try {
                    $rootScope.AwsPermissionsData = {}
                    if (($scope.safe.name !== '') && ($scope.safe.owner !== '')) {
                        var queryParameters = "path=" + $scope.getPath();
                        var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSafeInfo', queryParameters);
                        AdminSafesManagement.getSafeInfo(null, updatedUrlOfEndPoint).then(
                            function (response) {
                                if (UtilityService.ifAPIRequestSuccessful(response)) {
                                    if ($rootScope.showDetails !== true) {
                                        document.getElementById('addUser').value = '';
                                        document.getElementById('addGroup').value = '';
                                    }
                                    $scope.inputSelected.select = false;
                                    $scope.searchValue = {
                                        userName: '',
                                        groupName: ''
                                    };
                                    lastContent = '';
                                    // Try-Catch block to catch errors if there is any change in object structure in the response
                                    try {
                                        $scope.isLoadingData = false;
                                        var object = response.data.data;
                                        if(object && object.users && UtilityService.getAppConstant('AUTH_TYPE').toLowerCase() === "ldap1900") {
                                            var data = object.users;
                                            // get all object keys and iterate over them
                                                Object.keys(object.users).forEach(function(ele) {
                                                    ele.replace($scope.domainName, '');
                                                    var newEle = ele + $scope.domainName;
                                                    data[newEle] = data[ele];
                                                    delete data[ele];
                                                })
                                                object.users = data;
                                        }
                                        $scope.UsersPermissionsData = object.users;
                                        $scope.GroupsPermissionsData = object.groups;
                                        $rootScope.AwsPermissionsData = {
                                            "data": object['aws-roles']
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

                            });
                    }
                    else {
                        $scope.isLoadingData = false;
                    }
                } catch (e) {
                    // To handle errors while calling 'fetchData' function
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }

            }
        }

        $scope.init = function () {
            $scope.safe = {
                name: '',
                owner: '',
                description: '',
                type: ''
            };
            $scope.dropDownOptions = {
                'selectedGroupOption': {"type": "Select Type"},       // As initial placeholder
                'tableOptions': $scope.tableOptions
            }
            $scope.allSafesList = JSON.parse(SessionStore.getItem("allSafes"));
            $scope.myVaultKey = SessionStore.getItem("myVaultKey");
            // $scope.getDropdownDataForPermissions('', '');
            $scope.requestDataFrChangeSafe();
            $scope.fetchUsers();
            $scope.fetchGroups();
        }

        $scope.userNameValEmpty = false;
        $scope.grpNameValEmpty = false;

        $scope.fetchUsers = function () {

        }

        $scope.fetchGroups = function () {

        }

        $scope.addPermission = function (type, key, permission, editingPermission) {
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
                    var setPath = $scope.getPath();
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
                    switch (type) {
                        case 'users' :
                            apiCallFunction = AdminSafesManagement.addUserPermissionForSafe;
                            reqObjtobeSent = {"path": setPath, "username": key, "access": permission.toLowerCase()};
                            break;
                        case 'groups' :
                            apiCallFunction = AdminSafesManagement.addGroupPermissionForSafe;
                            reqObjtobeSent = {"path": setPath, "groupname": key, "access": permission.toLowerCase()};
                            break;
                        case 'AWSPermission' :
                            apiCallFunction = AdminSafesManagement.addAWSPermissionForSafe;
                            reqObjtobeSent = {"path": setPath, "role": key, "access": permission.toLowerCase()};
                            break;
                        case 'AwsRoleConfigure' :
                            $scope.awsConfPopupObj['policies'] = "";   // Todo: Because of unavailability of edit service, this has been put
                            if ($scope.editingAwsPermission.status == true) {
                                apiCallFunction = AdminSafesManagement.updateAWSRole;
                            } else {
                                apiCallFunction = AdminSafesManagement.addAWSRole;
                            }
                            reqObjtobeSent = $scope.awsConfPopupObj
                            break;
                    }
                    apiCallFunction(reqObjtobeSent).then(function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.isLoadingData = false;
                                    if (type === 'AwsRoleConfigure') {
                                        $scope.addPermission('AWSPermission', $scope.awsConfPopupObj.role, permission, false);
                                    }
                                    else {
                                        $scope.requestDataFrChangeSafe();
                                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                                        if (key !== null && key !== undefined) {
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
                "role": "",
                "bound_account_id": "",
                "bound_region": "",
                "bound_vpc_id": "",
                "bound_subnet_id": "",
                "bound_ami_id": "",
                "bound_iam_instance_profile_arn": "",
                "bound_iam_role_arn": "",
                "policies": ""
            };
            $scope.open(size);
        }

        /* TODO: What is open, functon name should be more descriptive */
        $scope.open = function (size) {
            Modal.createModal(size, 'changeSafePopup.html', 'ChangeSafeCtrl', $scope);
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

        // TO-BE-CHECKED : Function currently not in use

        //   $scope.getUsernamesAndGroups = function(){
        //       var url = "https://example.com/get-all-ad-users";
        //       var headers = {
        //           "authorization": "",
        //           "cache-control": "",
        //           "postman-token": ""
        //       }

        //       fetchData.getActionData(null, url, headers).then(
        //           function(response){
        //             console.log("All AD Users = ");
        //             console.log(response);
        //           },
        //           function(error){
        //               console.log("error = ");
        //               $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
        //               console.log(error);
        //           }
        //       );
        //   };

        $scope.init();

    });
})(angular.module('vault.features.ChangeSafeCtrl', [
    'vault.services.AdminSafesManagement',
    'vault.services.ModifyUrl',
    'vault.constants.AppConstant'
]));
