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
    app.controller('ChangeCertificateCtrl', function ($scope, $rootScope, Modal, $timeout, fetchData, $http, UtilityService, Notifications, $window, $state, $stateParams, $q, SessionStore, vaultUtilityService, ModifyUrl, AdminSafesManagement, AppConstant) {
        
        
        $scope.selectedGroupOption = '';            // Selected dropdown value to be used for filtering
        $rootScope.showDetails = true;              // Set true to show details view first
        $scope.similarSafes = 0;
        $rootScope.activeDetailsTab = 'details';
        $scope.typeDropdownDisable = false;         // Variable to be set to disable the dropdown button to select 'type'
        $scope.safeCreated = false;                 // Flag to indicate if a safe has been creted
        $scope.isEditSafe = false;
        $scope.awsRadioBtn = {};                    // Made an object instead of single variable, to have two way binding between
        $scope.approleRadioBtn = {};                                    // modal and controller

        $scope.inputValue = {
            "userNameVal": '',
            "grpNameVal": '',
            "userNameValEmpty": false,
            "grpNameValEmpty": false
        }
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

        $scope.bindSecretRadio = {
            value: 'false',
            options: [{
                'text': 'false'
            }, {
                'text': 'true'
            }]
        };

        $scope.detailsNavTags = [{
            displayName: 'PERMISSIONS',
            navigationName: 'permissions',
            addComma: false,
            show: true
        }];

        var clearInputPermissionData = function () {
            $scope.inputValue = {
                "userNameVal": '',                
            }
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
                    $scope.checkOwnerEmailHasValue('details');
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
            Modal.createModal(size, 'error.html', 'ChangeCertificateCtrl', $scope);
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

        //clear selcted email id on cross icon click
        $scope.clearOwnerEmailInputValue = function() {
            $scope.safe.owner = '';
            $scope.inputSelected.select = false;
            lastContent = '';
            $scope.showNoMatchingResults = false;
            $scope.invalidEmail = false;
        }
        // on navigation to details page check owner email field has value, if yes highlight box. 
        $scope.checkOwnerEmailHasValue = function(navigateToDetail) {
            if(navigateToDetail === 'details' && $scope.safe.owner && $scope.safe.owner.length > 0) {
                $scope.inputSelected.select = true;
            }
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
                        $(id).trigger('change');
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

        $scope.certificateEditPermission = function () {
            $scope.goBackToAdmin = true;
            var successCondition = true;
            $scope.goBack();
        }
        $scope.getPath = function () {
            var vaultType = $scope.dropDownOptions.selectedGroupOption.type;
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
            return setPath;
        }
        $scope.editPermission = function (type, editMode, user, permission) {
            if (editMode) {
                var editingPermission = true;
                //$scope.deletePermission(type, editMode, editingPermission, user, permission);
            }
        }
        $scope.replaceSpaces = function () {
            $scope.safe.name = UtilityService.formatName($scope.safe.name);
        }

        $scope.deletePermission = function (type, editMode, editingPermission, key, permission) {
            // if (editMode) {
            //     try {
            //         key = key.replace($scope.domainName, '');
            //         $scope.isLoadingData = true;
            //         var setPath = $scope.getPath();
            //         var apiCallFunction = '';
            //         var reqObjtobeSent = {};
            //         switch (type) {
            //             case 'users' :
            //                 apiCallFunction = AdminSafesManagement.deleteUserPermissionFromSafe;
            //                 if (editingPermission) {
            //     								reqObjtobeSent = {
            //     									"path": setPath,
            //     									"username": key,
            //     									"access": permission
            //     								};
            //     							}
            //     							else {
            //     								reqObjtobeSent = {
            //     									"path": setPath,
            //     									"username": key
            //     								};
            //     							}
            //                 break;
            //             case 'groups' :
            //                 apiCallFunction = AdminSafesManagement.deleteGroupPermissionFromSafe;
            //                 reqObjtobeSent = {
            //                     "path": setPath,
            //                     "groupname": key
            //                 };
            //                 break;
            //             case 'AWSPermission' :
            //                 if (editingPermission) {
            //                     apiCallFunction = AdminSafesManagement.detachAWSPermissionFromSafe;
            //                 }
            //                 else {
            //                     apiCallFunction = AdminSafesManagement.deleteAWSPermissionFromSafe;
            //                 }
            //                 reqObjtobeSent = {
            //                     "path": setPath,
            //                     "role": key
            //                 };
            //                 break;
            //             case 'AppRolePermission' :
            //                 apiCallFunction = AdminSafesManagement.detachAppRolePermissionFromSafe;
            //                 reqObjtobeSent = {
            //                     "path": setPath,
            //                     "role_name": key
            //                 };
            //                 break;
            //         }
            //         apiCallFunction(reqObjtobeSent).then(
            //             function (response) {
            //                 if (UtilityService.ifAPIRequestSuccessful(response)) {
            //                     // Try-Catch block to catch errors if there is any change in object structure in the response
            //                     try {
            //                         $scope.isLoadingData = false;
            //                         if (editingPermission) {
            //                             $scope.addPermission(type, key, permission, true);  // This will be executed when we're editing permissions
            //                         }
            //                         else {
            //                             $scope.requestDataFrChangeSafe();
            //                             if (type === "users" && key === SessionStore.getItem("username")) {
            //                                 return Modal.createModalWithController('stop.modal.html', {
            //                                     title: 'Permission changed',
            //                                     message: 'For security reasons, if you add or modify permission to yourself, you need to log out and log in again for the added or modified permissions to take effect.'
            //                                   });
            //                             }
            //                             if (type === 'AppRolePermission') {
            //                                 // delete approle
            //                             }
            //                             var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_SAFE_DELETE');
            //                             Notifications.toast(key + "'s permission" + notification);
            //                         }
            //                     }
            //                     catch (e) {
            //                         console.log(e);
            //                         $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
            //                         $scope.error('md');
            //                     }
            //                 }
            //                 else {
            //                     $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
            //                     error('md');
            //                 }
            //             },
            //             function (error) {

            //                 // Error handling function
            //                 console.log(error);
            //                 $scope.isLoadingData = false;
            //                 $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
            //                 $scope.error('md');

            //             })
            //     } catch (e) {

            //         console.log(e);
            //         $scope.isLoadingData = false;
            //         $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
            //         $scope.error('md');

            //     }
            // }
        }       


        $rootScope.goToPermissions = function () {
            $scope.invalidEmail = false;
            $scope.showNoMatchingResults = false;
            var emailPattern = /^[a-zA-Z0-9_%+-]+[.]?[a-zA-Z0-9_%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/;
            var emailInput = document.getElementById('addOwnerEmail').value;
            if (!emailPattern.test(emailInput)) {
                $scope.invalidEmail = true;
            } else {
                $timeout(function () {
                    if ($scope.isEditSafe) {                                              
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
        }


        $scope.requestDataFrChangeSafe = function () {
            $scope.isLoadingData = true;           

            if ($stateParams.certificateObject) {

                // Prefilled values when editing
                $scope.changeSafeHeader = $stateParams.certificateObject.certificateName;
                $scope.certificateName = $stateParams.certificateObject.certificateName;
                $scope.isEditSafe = true;
                $scope.typeDropdownDisable = true;
                var certName = $stateParams.certificateObject.certificateName;
                try {

                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getCertificates',"certificateName="+certName );
                    AdminSafesManagement.getCertificates(null, updatedUrlOfEndPoint).then(function (response) {                        
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            
                            $scope.isLoadingData = false; 

                            if ($rootScope.showDetails !== true) {
                                document.getElementById('addUser').value = '';
                            }
                            $scope.inputSelected.select = false;
                            $scope.searchValue = {
                                userName: '',
                            };
                            lastContent = '';
                            // Try-Catch block to catch errors if there is any change in object structure in the response
                            try {
                                $scope.isLoadingData = false;
                                var object = response.data.keys[0];
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
                                
                               
                            }
                            catch (e) {
                                console.log(e);
                                $scope.isLoadingData = false;
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                                $scope.error('md');
                            }
                        }
                        else {
                            $scope.isLoadingData = false;
                            $scope.certificatesLoaded =  true;
                            $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                            error('md');
                        }
                    },
                    function (error) {
                        // Error handling function
                        if ($rootScope.showDetails !== true) {
                            document.getElementById('addUser').value = '';
                        }
                        console.log(error);
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');

                    });                    
                } catch (e) {
                    // To handle errors while calling 'fetchData' function
                    if ($rootScope.showDetails !== true) {
                        document.getElementById('addUser').value = '';
                    }
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }

            }
        }

        $scope.init = function () {
            if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            var feature = JSON.parse(SessionStore.getItem("feature"));
            if (feature.selfservice == false && JSON.parse(SessionStore.getItem("isAdmin")) == false) {
                $state.go('manage');
            }
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
            if(!$scope.myVaultKey){ /* Check if user is in the same session */
                $state.go('/');
            }
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
            var duplicate = false;
            if (!editingPermission && key != '' && key != undefined) {
                if (type === "users" && $scope.UsersPermissionsData!= null && $scope.UsersPermissionsData.hasOwnProperty(key.toLowerCase())) {
                    if ($scope.UsersPermissionsData[key.toLowerCase()] != "sudo") {
                        duplicate = true;
                    }
                }
            }
            if (duplicate) {
                clearInputPermissionData();
                $scope.errorMessage = 'Permission already exists! Select edit icon for update';
                $scope.error('md');
            }
            else if ((key != '' && key != undefined) || type == 'AwsRoleConfigure') {
                try {
                    if (type === "users" && !editingPermission) {
                        key = document.getElementById('addUser').value.toLowerCase();
                    }
                    
                    Modal.close('');
                    $scope.isLoadingData = true;
                    $scope.showInputLoader.show = false;
                    $scope.showNoMatchingResults = false;
                    var certName = $scope.certificateName;
                    var apiCallFunction = '';
                    var reqObjtobeSent = {};
                    // extract only userId/groupId from key
                    if (key.includes($scope.domainName)) {
                        key = key.split('@')[0];
                    }
                    if (key !== null && key !== undefined) {
                        key = UtilityService.formatName(key);
                    }
                    
                    var updatedUrlOfEndPoint = "";
                    switch (type) {
                        case 'users' :
                            apiCallFunction = AdminSafesManagement.addUserPermissionForCertificate;
                            reqObjtobeSent = {"certificateName": certName, "username": key, "access": permission.toLowerCase()};
                            break;
                        case 'groups' :                            
                            reqObjtobeSent = {"certificateName": certName, "groupname": key, "access": permission.toLowerCase()};
                            break;
                        case 'AWSPermission' :                            
                            reqObjtobeSent = {"certificateName": certName, "role": key, "access": permission.toLowerCase()};
                            break;
                        case 'AwsRoleConfigure' :
                            $scope.awsConfPopupObj['policies'] = "";   // Todo: Because of unavailability of edit service, this has been put
                            reqObjtobeSent = $scope.awsConfPopupObj
                            break;
                        case 'AppRolePermission' :                            
                            reqObjtobeSent = {"certificateName": certName, "role_name": key, "access": permission.toLowerCase()};
                            break;
                    }
                    apiCallFunction(reqObjtobeSent, updatedUrlOfEndPoint).then(function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.isLoadingData = false;
                                    $scope.requestDataFrChangeSafe();
                                    var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                                    if (key !== null && key !== undefined) {
                                        if (type === "users" && key === SessionStore.getItem("username")) {
                                            clearInputPermissionData();
                                            return Modal.createModalWithController('stop.modal.html', {
                                                title: 'Permission changed',
                                                message: 'For security reasons, if you add or modify permission to yourself, you need to log out and log in again for the added or modified permissions to take effect.'
                                                });
                                        }
                                        Notifications.toast(key + "'s permission" + notification);
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
                                $scope.error('md');
                            }
                            clearInputPermissionData();
                            $scope.roleNameSelected = false;
                        },
                        function (error) {
                            // Error handling function
                            console.log(error);
                            clearInputPermissionData();
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        })
                } catch (e) {
                    // To handle errors while calling 'fetchData' function
                    $scope.isLoadingData = false;
                    clearInputPermissionData();
                    console.log(e);
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');
                }
            }
        }        

        /* TODO: What is open, functon name should be more descriptive */
        $scope.open = function (size) {
            Modal.createModal(size, 'changeSafePopup.html', 'ChangeCertificateCtrl', $scope);
        };

        /* TODO: What is open, functon name should be more descriptive */
        $scope.openApprole = function (size) {
            Modal.createModal(size, 'appRolePopup.html', 'ChangeCertificateCtrl', $scope);
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
})(angular.module('vault.features.ChangeCertificateCtrl', [
    'vault.services.AdminSafesManagement',
    'vault.services.ModifyUrl',
    'vault.constants.AppConstant'
]));
