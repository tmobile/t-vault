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
    app.controller('ChangeCertificateCtrl', function ($scope, $rootScope, Modal, $timeout, fetchData, $http, UtilityService, Notifications, $window, $state, $stateParams, $q, SessionStore, vaultUtilityService, ModifyUrl, AdminSafesManagement, AppConstant,RestEndpoints,filterFilter, orderByFilter, $compile) {
        
        
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
        $scope.isCertificateOwner = false;
        $scope.renewButtonShow = true;
        $scope.hideSudoPolicy = false;
        $scope.revokeButtonShow = true;
        $scope.userAutoCompleteEnabled = false;
        $scope.groupAutoCompleteEnabled = false;
        $scope.disableAddBtn = true;
        $scope.selectionValue = 'email';
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
            show: false
        }];

        var init = function () {
        	$scope.isUserSearchLoading = false;
        	$scope.isOwnerSearchLoading = false;
        	$scope.isLeadSearchLoading = false;
        	$scope.isNotificationSearchLoading = false;
        	$scope.certificateToOnboard = null;
        	 $scope.notificationEmails = [];
             $scope.selectedNotificationEmails= [];
             $scope.notificationEmail = { email:""};
             $scope.isNotificationEmailSelected = false;
             $scope.notificationEmailErrorMessage = "";             
             $scope.isNotificationEmailSearch = false;
             $scope.notificationEmailErrorMessage = "";
             $scope.ownerEmailErrorMessage = "";
             $scope.leadEmailErrorMessage = "";
             $scope.isadmin=false;
        };
        
        var clearInputPermissionData = function () {
            $scope.inputValue = {
                "userNameVal": '',                
            }
            $scope.disableAddBtn = true;
            $scope.clearInputValue("addUser");
            $scope.clearInputValue("addGroup");
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
                    $scope.requestDataFrChangeCertificate();
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
            $scope.disableAddBtn = true;
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
             if (variableChanged != 'userName'  && variableChanged != 'groupName') {
               newLetter = newLetter.replace(" ", "");
             }
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
                        $scope.disableAddBtn = false;
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
            $scope.permissionChangeInProgress = true;
            if (editMode) {
                try {
                    key = key.replace($scope.domainName, '');
                    $scope.isLoadingData = true;
                    var certName = $scope.certificateName;
                    var certficateType = $scope.certificateType;
                    var apiCallFunction = '';
                    var reqObjtobeSent = {};
                    switch (type) {
                        case 'users' :
                            apiCallFunction = AdminSafesManagement.deleteUserPermissionFromCertificate;
                            if (editingPermission) {
                                reqObjtobeSent = {
                                    "certificateName": certName,
                                    "username": key,
                                    "access": permission,
                                    "certType": certficateType
                                };
                            }
                            else {
                                reqObjtobeSent = {
                                    "certificateName": certName,
                                    "username": key,
                                    "access": permission,
                                    "certType": certficateType
                                };
                            }
                            break;
                        case 'groups' :
                            apiCallFunction = AdminSafesManagement.deleteGroupPermissionFromCertificate;
                            reqObjtobeSent = {
                                "certificateName": certName,
                                "groupname": key,
                                "access": permission,
                                "certType": certficateType
                            };
                            break;
                        case 'AWSPermission' :
                            apiCallFunction = AdminSafesManagement.detachAWSPermissionFromCertificate;
                            reqObjtobeSent = {
                                "certificateName": certName,
                                "rolename": key,
                                "certType": certficateType
                            };
                            break;
                        case 'AppRolePermission' :
                            apiCallFunction = AdminSafesManagement.deleteAppRolePermissionFromCertificate;
                            reqObjtobeSent = {
                                "certificateName": certName,
                                "approleName": key,
                                "access": permission,
                                "certType": certficateType
                            };
                            break;
                    }
                    apiCallFunction(reqObjtobeSent).then(
                        function (response) {
                            if (UtilityService.ifAPIRequestSuccessful(response)) {
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $scope.isLoadingData = false;
                                    $scope.permissionChangeInProgress = false;
                                    if (editingPermission) {
                                        $scope.addPermission(type, key, permission, true);  // This will be executed when we're editing permissions
                                    }
                                    else {                                       
                                        $scope.requestDataFrChangeCertificate();
                                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                                        
                                        if (type === "users" && key === SessionStore.getItem("username")) {
                                            clearInputPermissionData();
                                            return Modal.createModalWithController('stop.modal.html', {
                                                title: 'Permission changed',
                                                message: 'For security reasons, if you add or modify permission to yourself, you need to log out and log in again for the added or modified permissions to take effect.'
                                                });
                                        }
                                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_SAFE_DELETE');
                                        Notifications.toast(key + "'s permission" + notification); 
                                    }
                                }
                                catch (e) {
                                    console.log(e);
                                    $scope.permissionChangeInProgress = false;
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                                    $scope.error('md');
                                }
                            }
                            else {
                                $scope.permissionChangeInProgress = false;
                                $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                                $scope.error('md');
                            }
                        },
                        function (error) {

                            // Error handling function
                            console.log(error);
                            $scope.permissionChangeInProgress = false;
                            $scope.isLoadingData = false;
                            $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');

                        })
                } catch (e) {

                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.permissionChangeInProgress = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }
            }
        }       

        $rootScope.goToCertPermissions = function () {
        	$scope.requestDataFrChangeCertificate();
        	$scope.ownerEmailErrorMessage ="";
            if($scope.notificationEmail!=undefined){
                $scope.notificationEmail.email = "";
                }
            $scope.notificationEmailErrorMessage = '';
            $scope.leadEmailErrorMessage ="";
            $scope.isLoadingData = true;
            $rootScope.showDetails = false;               // To show the 'permissions' and hide the 'details'
            $rootScope.activeDetailsTab = 'permissions';
            $scope.isLoadingData = false;
            
        } 
        
        $scope.showcertdetails = function () {
        	$scope.requestDataFrChangeCertificate();
        } 

        $scope.requestDataFrChangeCertificate = function () {
            $scope.isLoadingData = true;           

            if ($stateParams.certificateObject) {

                // Prefilled values when editing
                $scope.changeCertificateHeader = "EDIT CERTIFICATE";
                $scope.certificateName = $stateParams.certificateObject.certificateName;
                $scope.certificateType = $stateParams.certificateObject.certType;
                $scope.isEditSafe = true;
                $scope.typeDropdownDisable = true;
                var certName = $stateParams.certificateObject.certificateName;
                var certificateType = $stateParams.certificateObject.certType;
                $scope.certificateTypeVal = $stateParams.certificateObject.certType;
                $scope.appName = $stateParams.certificateObject.applicationName;

                try {

                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getCertificates',"certificateName="+certName+"&certType="+certificateType );
                    AdminSafesManagement.getCertificates(null, updatedUrlOfEndPoint).then(function (response) {                        
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            
                            $scope.isLoadingData = false; 

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

                                var object;
                                if(response.data.keys.length > 1) {
                                    for (var k=0;k<response.data.keys.length;k++) {
                                        var certObject = response.data.keys[k];
                                        if(certObject.certificateName === $scope.certificateName){
                                            object = certObject;
                                        }
                                    }
                                }else{
                                    object = response.data.keys[0];
                                } 
                                
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
                                var certOwner = object.certOwnerNtid;
                                if(SessionStore.getItem("username").toLowerCase() === certOwner.toLowerCase()){
                                    $scope.isCertificateOwner = true;
                                    $scope.detailsNavTags[1].show = true;
                                }else {                                    
                                    $scope.detailsNavTags[1].show = false;                                    
                                }

                                $rootScope.AppRolePermissionsData = {
                                    "data": object['app-roles']
                                }

                                $scope.AwsPermissionsData = {
                                    "data": object['aws-roles']
                                }

                                $scope.certificate = {
                                    certificateName: object.certificateName || $stateParams.certificateObject.certificateName,
                                    ownerEmail: object.certOwnerEmailId || $stateParams.certificateObject.certOwnerEmailId || '',
                                    applicationName: object.applicationName || $stateParams.certificateObject.applicationName || '',
                                    applicationTag: object.applicationTag || $stateParams.certificateObject.applicationTag || '',
                                    certType: object.certType || $stateParams.certificateObject.certType || '',
                                    createDate: object.createDate || $stateParams.certificateObject.createDate || '',
                                    expiryDate: object.expiryDate || $stateParams.certificateObject.expiryDate || '',
                                    certificateStatus: object.certificateStatus || $stateParams.certificateObject.certificateStatus || '',
                                    certificateId: object.certificateId || $stateParams.certificateObject.certificateId || '',
                                    dnsNames: object.dnsNames || $stateParams.certificateObject.dnsNames || '',
                                    leadEmail: object.projectLeadEmailId || $stateParams.certificateObject.projectLeadEmailId || '',
                                    appOwnerEmail: object.applicationOwnerEmailId || $stateParams.certificateObject.applicationOwnerEmailId || '',
                                    notificationEmails: object.notificationEmails || $stateParams.certificateObject.notificationEmails || ''
                                }
                                

                                if(JSON.parse(SessionStore.getItem("isAdmin")) == false){
                               	 $scope.isadmin = false;
                                }else{
                                	$scope.isadmin = true; 
                                }
                                
                                
                                	$scope.notificationEmails = [];
                                    var notificationStr = $scope.certificate.notificationEmails; 
                                    
                                    angular.element('#notificationEmailList').empty();
//                                    notificationStr = addOwnerTonotificationList(notificationStr);
                                    $scope.notificationList  = notificationStr.split(',')
                                    $scope.certificate.notificationEmails = $scope.notificationList;
                                    var i = 0;
                                    if(notificationStr!="" && notificationStr!=undefined){
                                    $scope.certificate.notificationEmails.forEach(function (email) {
                                    var id = "dns"+ (i++);
                                    $scope.notificationEmails.push({ "id": id, "email":email});       
                                    angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                                    });
                            }
                                    $scope.certificate.notificationEmails = notificationStr;
                                

                                if($scope.certificate.certType.toLowerCase() === "internal"){
                                    $scope.certificateTypeVal = "Internal";
                                }else if($scope.certificate.certType.toLowerCase() === "external"){
                                    $scope.certificateTypeVal = "External";
                                    $scope.dnsListExt = [];
                                    var string = $scope.certificate.dnsNames;
                                    if(string != undefined && string != ""){
                                         if(typeof string === 'object'){
                                              $scope.certificate.dnsNames = string;
                                         } else {
                                            var dnsStr = string.substring(1, string.length-1);
                                            $scope.dnsListExt  = dnsStr.split(',')
                                            $scope.certificate.dnsNames = $scope.dnsListExt;
                                        }
                                    }
                                }

                                if($scope.certificate.applicationTag !== null && $scope.certificate.applicationTag !== undefined  && $scope.certificate.applicationTag !== ""){
                                    $scope.appName = $scope.certificate.applicationTag;
                                }else {
                                    $scope.appName = $scope.certificate.applicationName;
                                }

                                $scope.GroupsPermissionsData = object.groups;
                                if(object.requestStatus !== null && object.requestStatus !== undefined && object.requestStatus !== "Approved") {
                                    $scope.renewButtonShow = false;
                                }else {
                                    $scope.renewButtonShow = true;
                                }
                                
                                $scope.dnsList = [];
                                $scope.dnsStr = "";
                                for (var i=0;i<$scope.certificate.dnsNames.length;i++) {
                                	$scope.dnsList.push($scope.certificate.dnsNames[i]);
                                    }
                                $scope.dnsStr = $scope.dnsList.join('\n');   
                                
                                $scope.notificationList = [];
                                $scope.notificationStr = "";
                                for (var i=0;i<$scope.certificate.notificationEmails.length;i++) {
                                	$scope.notificationList.push($scope.certificate.notificationEmails[i]);
                                    }
                                $scope.notificationStr = $scope.notificationList.join('\n'); 
                                                                                       
                                if($rootScope.checkStatus=="Revoked"){
                                	$scope.revokeButtonShow = false;	
                                }                                
                                    hideUserSudoPolicy();
                                    getUserDisplayNameDetails();
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

        var getUserDisplayNameDetails = function () {
            $scope.isLoadingData = true;
            $scope.userNames = [];
            $scope.UsersPermissionsDetails = [];
            $scope.UsersDisplayNameData = [];
            for (var key in $scope.UsersPermissionsData) {
                $scope.userNames.push(key);
            }
            if ($scope.userNames !== undefined && $scope.userNames.length > 0) {
                vaultUtilityService.getAllUsersDataForPermissions($scope.userNames.join()).then(function (res, error) {
                    var serviceData;
                    if (res) {
                        $scope.isLoadingData = false;
                        serviceData = res;
                        $scope.UsersDisplayNameData = serviceData.response.data.data.values;
                        for (var i=0;i<$scope.UsersDisplayNameData.length;i++) {
                            var userNameKey = $scope.UsersDisplayNameData[i].userName.toLowerCase();
                            var userDisplayName = $scope.UsersDisplayNameData[i].displayName + " ("+$scope.UsersDisplayNameData[i].userName+")";
                            var permissionVal = "";
                            for (var key in $scope.UsersPermissionsData) {
                                if(key.toLowerCase() === userNameKey) {
                                    permissionVal = $scope.UsersPermissionsData[key.toLowerCase()];
                                }
                            }
                            $scope.UsersPermissionsDetails.push({"key":userNameKey, "value":permissionVal, "displayName":userDisplayName});
                        }
                        $scope.$apply();
                    } else {
                        $scope.isLoadingData = false;
                        serviceData = error;
                        $scope.commonErrorHandler(serviceData.error, serviceData.error || serviceData.response.data, "getDropdownData");

                    }
                },
                function (error) {
                    $scope.isLoadingData = false;
                    // Error handling function when api fails
                    $scope.showInputLoader.show = false;
                    if (error.status === 500) {
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_NETWORK');
                        $scope.error('md');
                    } else if(error.status !== 200 && (error.xhrStatus === 'error' || error.xhrStatus === 'complete')) {
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_AUTOCOMPLETE_USERNAME');
                        $scope.error('md');
                    }
                });
            }else{
                $scope.isLoadingData = false;
            }
        }

        var hideUserSudoPolicy = function() {
            $scope.hideSudoPolicy = false;
            var flg = false;
            var count=0;
            Object.keys($scope.UsersPermissionsData).forEach(function(key) {
                if (($scope.UsersPermissionsData[key] === "sudo") || ($scope.UsersPermissionsData[key] === "write")) {
                    flg = true;
                }
                count++;
            });
            if (count==1 && flg == true) {
                $scope.hideSudoPolicy = true;
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
            $scope.disableAddBtn = true;
            $scope.userAutoCompleteEnabled = false;
            $scope.groupAutoCompleteEnabled = false;
            if (AppConstant.AD_USERS_AUTOCOMPLETE == true) {
                $scope.userAutoCompleteEnabled = true;
            }
            if (AppConstant.AD_GROUP_AUTOCOMPLETE == true) {
                $scope.groupAutoCompleteEnabled = true;
            }
            $scope.requestDataFrChangeCertificate();
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
            if (key !== null && key !== undefined) {
                if (type === "users" && !editingPermission) {
                    key = document.getElementById('addUser').value.toLowerCase();
                }

                if (type === "groups" && !editingPermission) {
                    key = document.getElementById('addGroup').value;
                }
                // extract only userId/groupId from key
                if (key.includes($scope.domainName)) {
                    key = key.split('@')[0];
                }
                if (type === "users" && key.includes("(")) {
                    key = key.substring(key.lastIndexOf("(") + 1, key.lastIndexOf(")"));
                }
            }
            if (!editingPermission && key != '' && key != undefined) {
                if (type === "users" && $scope.UsersPermissionsData!= null && $scope.UsersPermissionsData.hasOwnProperty(key.toLowerCase())) {
                    if ($scope.UsersPermissionsData[key.toLowerCase()] != "write") {
                        duplicate = true;
                    }
                }
                if (type === "groups" && $scope.GroupsPermissionsData!= null) {
                    var groupIndex = Object.keys($scope.GroupsPermissionsData).findIndex(function (groupName) {
                        return groupName.toLowerCase() === key.toLowerCase();
                    });
                    if(groupIndex > -1){
                        duplicate = true;
                    }
                }
                if (type === "AWSPermission" && $scope.AwsPermissionsData.data!= null && $scope.AwsPermissionsData.data.hasOwnProperty(key.toLowerCase())) {
                    duplicate = true;
                }
                if (type === "AppRolePermission" && $scope.AppRolePermissionsData.data!= null && $scope.AppRolePermissionsData.data.hasOwnProperty(key.toLowerCase())) {
                    duplicate = true;
                }
            }
            if (duplicate) {
                clearInputPermissionData();
                $scope.errorMessage = 'Permission already exists! Select edit icon for update';
                $scope.error('md');
            }
            else if ((key != '' && key != undefined) || type == 'AwsRoleConfigure') {
                try {
                    Modal.close('');
                    $scope.isLoadingData = true;
                    $scope.showInputLoader.show = false;
                    $scope.showNoMatchingResults = false;
                    var certName = $scope.certificateName;
                    var certficateType = $scope.certificateType;
                    var apiCallFunction = '';
                    var reqObjtobeSent = {};
                    if ($scope.awsConfPopupObj.role !== null && $scope.awsConfPopupObj.role !== undefined) {
                        $scope.awsConfPopupObj.role = UtilityService.formatName($scope.awsConfPopupObj.role);
                    }
                    if ($scope.awsConfPopupObj.bound_region !== null && $scope.awsConfPopupObj.bound_region !== undefined) {
                        $scope.awsConfPopupObj.bound_region = UtilityService.formatName($scope.awsConfPopupObj.bound_region);
                    }
                    var updatedUrlOfEndPoint = "";
                    switch (type) {
                        case 'users' :
                            apiCallFunction = AdminSafesManagement.addUserPermissionForCertificate;
                            reqObjtobeSent = {"certificateName": certName, "username": key, "access": permission.toLowerCase(), "certType":certficateType};
                            break;
                        case 'groups' : 
                            apiCallFunction = AdminSafesManagement.addGroupPermissionForCertificate;                           
                            reqObjtobeSent = {"certificateName": certName, "groupname": key, "access": permission.toLowerCase(), "certType":certficateType};
                            break;
                        case 'AWSPermission' :
                            apiCallFunction = AdminSafesManagement.addAWSPermissionForCertificate;
                            reqObjtobeSent = {"certificateName": certName, "rolename": key, "access": permission.toLowerCase(), "certType":certficateType};
                            break;
                        case 'AwsRoleConfigure' :
                            $scope.awsConfPopupObj['policies'] = "";   // Todo: Because of unavailability of edit service, this has been put
                            // Validate the input here if requried...
                            if ($scope.awsConfPopupObj.auth_type === 'ec2') {
                                $scope.awsConfPopupObj.bound_iam_principal_arn = "";
                                apiCallFunction = AdminSafesManagement.createAwsRoleCertificate;
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
                                apiCallFunction = AdminSafesManagement.createAwsIAMRoleCertificate;
                            }
                            reqObjtobeSent = $scope.awsConfPopupObj
                            break;
                        case 'AppRolePermission' : 
                            apiCallFunction = AdminSafesManagement.addApprolePermissionForCertificate;
                            reqObjtobeSent = {"certificateName": certName, "approleName": key, "access": permission.toLowerCase(), "certType":certficateType};
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
                                        $scope.requestDataFrChangeCertificate();
                                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                                        if (key !== null && key !== undefined) {
                                            document.getElementById('addGroup').value = '';
                                            document.getElementById('addUser').value = '';
                                            if (type === "users" && key === SessionStore.getItem("username")) {
                                                clearInputPermissionData();
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
        };
        
        $scope.addApproleToCertificate = function (size) {
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
        };

        $scope.openAWSRoleCreatePopup = function (size) {
            Modal.createModal(size, 'createAWSRolePopup.html', 'ChangeCertificateCtrl', $scope);
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
        
        $scope.cancelDelete = function () {        	
            Modal.close('close');
            $scope.isLoadingData = false;
            $scope.goBack();
        };

        $scope.renewCertPopup = function (certDetails) {
            $scope.fetchDataError = false;
            $rootScope.certDetails = certDetails;  
            $scope.ownerEmailErrorMessage ="";
            if($scope.notificationEmail!=undefined){
                $scope.notificationEmail.email = "";
                }
            $scope.notificationEmailErrorMessage = '';
            $scope.leadEmailErrorMessage ="";            
            if((certDetails.certType === "external")){
                var Difference_In_Time =  new Date().getTime()  - new Date(certDetails.createDate).getTime();
                var Difference_In_Days = Math.round(Difference_In_Time / (1000 * 3600 * 24));

                var Difference_In_Time_Total = new Date(certDetails.expiryDate).getTime() - new Date().getTime() ;
                var Difference_In_Days_Total = Math.round(Difference_In_Time_Total / (1000 * 3600 * 24));

                if(Difference_In_Days <= 30){
                    $scope.renewExternalConfirmMessage = "External certificate can be renewed only after a month of  certificate  creation ";
                    Modal.createModal('md', 'renewExternalCertPopup.html', 'ChangeCertificateCtrl', $scope);
                } else {
                   $scope.renewConfirmMessage = "Certificate expiring in  " +Difference_In_Days_Total + " Days . Do you want to renew this certificate?" ;
                    Modal.createModal('md', 'renewCertPopup.html', 'ChangeCertificateCtrl', $scope);
                }
           } else{
                var Difference_In_Time = new Date(certDetails.expiryDate).getTime() - new Date().getTime() ;
                var Difference_In_Days = Math.round(Difference_In_Time / (1000 * 3600 * 24));
                if(Difference_In_Days > AppConstant.VALID_RENEW_DAYS){
                    $scope.renewConfirmMessage = "Certificate expiring in  " +Difference_In_Days + " Days . Do you want to renew this certificate?" ;
                } else {
                     $scope.renewConfirmMessage = "Are you sure you want to renew this certificate? ";
                }
                Modal.createModal('md', 'renewCertPopup.html', 'ChangeCertificateCtrl', $scope);
            }
        };

        $scope.revokeReasonsPopUp = function (certificate) {
            Modal.createModal('md', 'revokeReasonsPopUp.html', 'ChangeCertificateCtrl', $scope);
        };

        $scope.revocationPopUp = function (certificate) {
            Modal.createModal('md', 'revocationPopUp.html', 'ChangeCertificateCtrl', $scope);
        };

        $scope.renewCertificatePopUp = function (certificate) {
            Modal.createModal('md', 'renewCertificatePopUp.html', 'ChangeCertificateCtrl', $scope);
        };

        $scope.renewCertificateFailedPopUp = function (certificate) {
            Modal.createModal('md', 'renewCertificateFailedPopUp.html', 'ChangeCertificateCtrl', $scope);
        };
        
        $scope.deleteCertPopup = function (certDetails) {
            $scope.fetchDataError = false;
            $rootScope.certDetails = certDetails;
            if($scope.notificationEmail!=undefined){
            $scope.notificationEmail.email = "";
            }
            $scope.notificationEmailErrorMessage = '';
            $rootScope.certDetails = certDetails;
            Modal.createModal('md', 'deleteCertPopup.html', 'ChangeCertificateCtrl', $scope);
        };
        
        $scope.deleteCertificatePopUp = function (certificate) {
            Modal.createModal('md', 'deleteCertificateSuccessPopUp.html', 'ChangeCertificateCtrl', $scope);
        };

        $scope.deleteCertificateFailedPopUp = function (certificate) {
            Modal.createModal('md', 'deleteCertificateFailedPopUp.html', 'ChangeCertificateCtrl', $scope);
        };        

        $scope.updateCertificatePopUp = function (certificate) {
            Modal.createModal('md', 'updateCertificateSuccessPopUp.html', 'ChangeCertificateCtrl', $scope);
        };
        $scope.updateCertificateFailedPopUp = function (certificate) {
            Modal.createModal('md', 'updateCertificateFailedPopUp.html', 'ChangeCertificateCtrl', $scope);
        };

        $scope.updateCertPopup = function (certDetails) {
            $scope.fetchDataError = false;
            $rootScope.certDetails = certDetails;
            if($scope.notificationEmail!=undefined){
            $scope.notificationEmail.email = "";
            }
            $scope.notificationEmailErrorMessage = '';
            if(certDetails.appOwnerEmail==null || certDetails.appOwnerEmail==""){
            	$scope.ownerEmailErrorMessage = "Application Owner should not be empty";
            }else if(certDetails.leadEmail==null || certDetails.leadEmail=="" || certDetails.leadEmail==undefined ){
            	$scope.leadEmailErrorMessage = "Project Lead should not be empty";
            }else if($scope.certificate.notificationEmails==null || $scope.certificate.notificationEmails=="" || $scope.certificate.notificationEmails==undefined ){
            	$scope.notificationEmailErrorMessage = "Notification Emails should not be empty";
            }else{
            Modal.createModal('md', 'updateCertPopup.html', 'ChangeCertificateCtrl', $scope);
            }};

         //Revoke Certificate
        $scope.revokeCertificate = function (certificateDetails){
            try{
            $scope.isLoadingData = true;
            $scope.revocationMessage = '';
            $scope.revocationStatusMessage = '';
            $scope.ownerEmailErrorMessage ="";
            if($scope.notificationEmail!=undefined){
                $scope.notificationEmail.email = "";
                }
            $scope.notificationEmailErrorMessage = '';
            $scope.leadEmailErrorMessage ="";
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
        };

        $scope.revoke = function(){
            try {
                $scope.revocationMessage = ''
                $scope.ownerEmailErrorMessage ="";
                if($scope.notificationEmail!=undefined){
                    $scope.notificationEmail.email = "";
                    }
                $scope.notificationEmailErrorMessage = '';
                $scope.leadEmailErrorMessage ="";
                angular.element('#notificationEmailList').empty();
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
                        $scope.requestDataFrChangeCertificate();
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
        };

        $scope.cancelRevoke = function(){
            try{
                Modal.close('');
            }catch (e){
                console.log(e);
            }
        };

        $scope.getCertSubjectName = function (cert) {
            var certName = "";
            if (cert.subjectAltName && cert.subjectAltName.dns && cert.subjectAltName.dns.length > 0) {
                certName = cert.subjectAltName.dns[0];
            }
            if (certName == "" || certName == undefined) {
                certName = cert.certificateName
            }
            return certName;
        };

        $rootScope.renewCertificate = function(certificateDetails){
            if ($rootScope.certDetails !== null && $rootScope.certDetails !== undefined) {
                certificateDetails = $rootScope.certDetails;
            }
            $rootScope.certDetails = null;
            $scope.ownerEmailErrorMessage ="";
            if($scope.notificationEmail!=undefined){
                $scope.notificationEmail.email = "";
                }
            $scope.notificationEmailErrorMessage = '';
            $scope.leadEmailErrorMessage ="";            
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
                        $scope.requestDataFrChangeCertificate();
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
        
        $rootScope.deleteCertificate = function(certificateDetails){
            if ($rootScope.certDetails !== null && $rootScope.certDetails !== undefined) {
                certificateDetails = $rootScope.certDetails;
            }
            $rootScope.certDetails = null;
            try{
                $scope.isLoadingData = true;
                Modal.close();
                $scope.deleteMessage = '';
                var certificateName = $scope.getCertSubjectName(certificateDetails);
                $scope.certificateNameForDelete = certificateName;
                var certType = certificateDetails.certType;                
                var url = RestEndpoints.baseURL + "/v2/certificates/" +certificateName+"/"+certType ;
                $scope.isLoadingData = true;

                AdminSafesManagement.deleteCertificate(null, url).then(function (response) {
                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.deleteMessage = 'Certificate Deleted Successfully!';
                        $scope.deleteMessage = response.data.messages[0];
                        $scope.deleteCertificatePopUp();
                        $scope.searchValue = '';
                    }
                },
                function (error) {
                    var errors = error.data.errors;
                    $scope.deleteMessage = 'Delete Failed';                    
                    if (errors[0] == "Access denied: No permission to delete certificate") {
                        $scope.deleteMessage = "For security reasons, you need to log out and log in again for the permissions to take effect.";
                    } else {
                        $scope.deleteMessage = errors[0];
                    }
                    
                    $scope.deleteCertificateFailedPopUp();
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
            $scope.openAWSRoleCreatePopup(size);
        }
        
        $rootScope.updateCertificate = function(certificateDetails){
            if ($rootScope.certDetails !== null && $rootScope.certDetails !== undefined) {
                certificateDetails = $rootScope.certDetails;
            }
            $rootScope.certDetails = null;
            try{
                $scope.isLoadingData = true;
                Modal.close();
                $scope.updateMessage = '';
                var certificateName = $scope.getCertSubjectName(certificateDetails);
                $scope.certificateNameForDelete = certificateName;
                var certType = certificateDetails.certType;                 
                var url = RestEndpoints.baseURL + "/v2/sslcert/";
                $scope.isLoadingData = true;
                var reqObjtobeSent =  { 
                        "certificateName":certificateName,
                        "certType":certType,
                        "projectLeadEmail":certificateDetails.leadEmail,
                        "applicationOwnerEmail":certificateDetails.appOwnerEmail,
                        "notificationEmail": $scope.certificate.notificationEmails
                    }
                AdminSafesManagement.updateCertificate(reqObjtobeSent, url).then(function (response) {
                    $scope.isLoadingData = false;
                    if (UtilityService.ifAPIRequestSuccessful(response)) {
                        $scope.updateMessage = 'Certificate Updated Successfully!'; 
                        $scope.updateMessage = response.data.messages[0];
                        $scope.updateCertificatePopUp();
                        $scope.searchValue = '';
                    }
                },
                function (error) {
                    var errors = error.data.errors;
                    $scope.updateMessage = 'Update Failed';                    
                    if (errors[0] == "Access denied: No permission to update certificate") {
                        $scope.updateMessage = "For security reasons, you need to log out and log in again for the permissions to take effect.";
                    } else {
                        $scope.updateMessage = errors[0];
                    }

                    $scope.updateCertificateFailedPopUp();
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


        //editable fields
        $scope.selectLeadforCert = function (ownerEmail) {
            if (ownerEmail != null) {
                $scope.certificate.leadEmail = ownerEmail.userEmail;                
                $scope.isLeadSelectedForOnboard = true;
                $scope.isLeadEmailSearch = false;
            }
        }

        $scope.selectOwnerforCert = function (ownerEmail) {
            if (ownerEmail != null) {
            	$scope.certificate.appOwnerEmail = ownerEmail.userEmail;                
                $scope.isOwnerSelectedForOnboard = true;
                $scope.isOwnerEmailSearch = false;
            }
        }

        $scope.selectNotificationEmail = function (ownerEmail) {
        	$scope.notificationEmail = { email:""};
        	$scope.notificationEmailErrorMessage = '';
            if (ownerEmail != null) {
                $scope.notificationEmail.email = ownerEmail.userEmail;
                if(ownerEmail.userEmail==""||ownerEmail.userEmail==undefined||ownerEmail.userEmail==null){
                    $scope.notificationEmail.email = ownerEmail.email;
                }
                $scope.isNotificationEmailSelected = true;
                $scope.isNotificationEmailSearch = false;
            }
        }


        $scope.searchLeadEmailForCert = function (email) {
            if (!email.endsWith("\\")) {
            	$scope.isOwnerEmailSearch = false;
            	$scope.isNotificationEmailSearch = false;
                $scope.isLeadEmailSearch = true;
                return $scope.searchEmail(email,"lead");
            }
        }

        $scope.searchOwnerEmailForCert = function (email) {
            if (!email.endsWith("\\")) {            	
            	$scope.isLeadEmailSearch = false;
            	$scope.isNotificationEmailSearch = false;
                $scope.isOwnerEmailSearch = true;
                return $scope.searchEmail(email,"owner");
            }
        }

        $scope.searchEmailForNotification = function (email,selectionValue) {
            if (!email.endsWith("\\")) {
                $scope.isNotificationEmailSearch = true;
                $scope.isLeadEmailSearch = false;
                $scope.isOwnerEmailSearch = false;
                $scope.selectionValue=selectionValue;
                if($scope.selectionValue == "email"){
                    return $scope.searchEmail(email,"notification");
                }
                if($scope.selectionValue == "ntid"){
                    return $scope.searchNtid(email,"notification");
                }
                if($scope.selectionValue == "lastname"){
                    return $scope.searchLastname(email,"notification");
                }
                if($scope.selectionValue == "groupemail"){
                    return $scope.searchGroupName(email,"notification");
                }
               // return $scope.searchEmail(email,"notification");
            }
        }

            $scope.searchEmail = function (searchVal,val) {        	
                if (searchVal.length > 2) {

                    if(val=="notification"){
                    	$scope.isNotificationSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isLeadSearchLoading = false;
                    }
                    if(val=="owner"){
                    	$scope.isOwnerSearchLoading = true;
                    	$scope.isLeadSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    if(val=="lead"){
                    	$scope.isLeadSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    searchVal = searchVal.toLowerCase();
                    try {
                        $scope.userSearchList = [];

                        var queryParameters = searchVal;
                        var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('usersGetData', queryParameters);
                        return AdminSafesManagement.usersGetData(null, updatedUrlOfEndPoint).then(
                            function(response) {
                            	if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }

                                if (UtilityService.ifAPIRequestSuccessful(response)) {
                                    var filterdUserData = [];
                                    $scope.userSearchList = response.data.data.values;
                                    $scope.userSearchList.forEach(function (userData) {
                                        if (userData.userEmail != null && userData.userEmail.substring(0, searchVal.length).toLowerCase() == searchVal) {
                                            filterdUserData.push(userData);
                                        }
                                    });
                                    if(val=="notification"){
                                    	$scope.isNotificationSearchLoading = false;
                                    }
                                    if(val=="owner"){
                                    	$scope.isOwnerSearchLoading = false;
                                    }
                                    if(val=="lead"){
                                    	$scope.isLeadSearchLoading = false;
                                    }
                                    return orderByFilter(filterFilter(filterdUserData, searchVal), 'userEmail', true);
                                } else {
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                    $scope.error('md');
                                }
                            },
                            function(error) {
                                // Error handling function
                                console.log(error);
                                if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                $scope.error('md');
                        });
                    } catch (e) {
                        console.log(e);
                        if(val=="notification"){
                        	$scope.isNotificationSearchLoading = false;
                        }
                        if(val=="owner"){
                        	$scope.isOwnerSearchLoading = false;
                        }
                        if(val=="lead"){
                        	$scope.isLeadSearchLoading = false;
                        }
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    }
                }
            }
            $scope.searchNtid = function (searchVal,val) {        	
                if (searchVal.length > 2) {

                    if(val=="notification"){
                    	$scope.isNotificationSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isLeadSearchLoading = false;
                    }
                    if(val=="owner"){
                    	$scope.isOwnerSearchLoading = true;
                    	$scope.isLeadSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    if(val=="lead"){
                    	$scope.isLeadSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    searchVal = searchVal.toLowerCase();
                    try {
                        $scope.userSearchList = [];

                        var queryParameters = searchVal;
                        var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('usersGetDataUsingCorpID', queryParameters);
                        return AdminSafesManagement.usersGetDataUsingCorpID(null, updatedUrlOfEndPoint).then(
                            function(response) {
                            	if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }

                                if (UtilityService.ifAPIRequestSuccessful(response)) {
                                    var filterdUserData = [];
                                    $scope.userSearchList = response.data.data.values;
                                    $scope.userSearchList.forEach(function (userData) {
                                        if (userData.userName != null && userData.userName.substring(0, searchVal.length).toLowerCase() == searchVal) {
                                            filterdUserData.push(userData);
                                        }
                                    });
                                    if(val=="notification"){
                                    	$scope.isNotificationSearchLoading = false;
                                    }
                                    if(val=="owner"){
                                    	$scope.isOwnerSearchLoading = false;
                                    }
                                    if(val=="lead"){
                                    	$scope.isLeadSearchLoading = false;
                                    }
                                    return orderByFilter(filterFilter(filterdUserData, searchVal), 'userName', true);
                                } else {
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                    $scope.error('md');
                                }
                            },
                            function(error) {
                                // Error handling function
                                console.log(error);
                                if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                $scope.error('md');
                        });
                    } catch (e) {
                        console.log(e);
                        if(val=="notification"){
                        	$scope.isNotificationSearchLoading = false;
                        }
                        if(val=="owner"){
                        	$scope.isOwnerSearchLoading = false;
                        }
                        if(val=="lead"){
                        	$scope.isLeadSearchLoading = false;
                        }
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    }
                }
            }

            $scope.searchLastname = function (searchVal,val) {        	
                if (searchVal.length > 2) {

                    if(val=="notification"){
                    	$scope.isNotificationSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isLeadSearchLoading = false;
                    }
                    if(val=="owner"){
                    	$scope.isOwnerSearchLoading = true;
                    	$scope.isLeadSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    if(val=="lead"){
                    	$scope.isLeadSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    searchVal = searchVal.toLowerCase();
                    try {
                        $scope.userSearchList = [];

                        var queryParameters = searchVal;
                        var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('usersGetDataUsingNTID', queryParameters);
                        return AdminSafesManagement.usersGetDataUsingNTID(null, updatedUrlOfEndPoint).then(
                            function(response) {
                            	if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }

                                if (UtilityService.ifAPIRequestSuccessful(response)) {
                                    var filterdUserData = [];
                                    $scope.userSearchList = response.data.data.values;
                                    $scope.userSearchList.forEach(function (userData) {
                                        if (userData.displayName != null && userData.displayName.substring(0, searchVal.length).toLowerCase() == searchVal) {
                                            filterdUserData.push(userData);
                                        }
                                    });
                                    if(val=="notification"){
                                    	$scope.isNotificationSearchLoading = false;
                                    }
                                    if(val=="owner"){
                                    	$scope.isOwnerSearchLoading = false;
                                    }
                                    if(val=="lead"){
                                    	$scope.isLeadSearchLoading = false;
                                    }
                                    return orderByFilter(filterFilter(filterdUserData, searchVal), 'displayName', true);
                                } else {
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                    $scope.error('md');
                                }
                            },
                            function(error) {
                                // Error handling function
                                console.log(error);
                                if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                $scope.error('md');
                        });
                    } catch (e) {
                        console.log(e);
                        if(val=="notification"){
                        	$scope.isNotificationSearchLoading = false;
                        }
                        if(val=="owner"){
                        	$scope.isOwnerSearchLoading = false;
                        }
                        if(val=="lead"){
                        	$scope.isLeadSearchLoading = false;
                        }
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    }
                }
            }
            $scope.searchGroupName = function (searchVal,val) {        	
                if (searchVal.length > 2) {

                    if(val=="notification"){
                    	$scope.isNotificationSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isLeadSearchLoading = false;
                    }
                    if(val=="owner"){
                    	$scope.isOwnerSearchLoading = true;
                    	$scope.isLeadSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    if(val=="lead"){
                    	$scope.isLeadSearchLoading = true;
                    	$scope.isOwnerSearchLoading = false;
                    	$scope.isNotificationSearchLoading = false;
                    }
                    searchVal = searchVal.toLowerCase();
                    try {
                        $scope.userSearchList = [];

                        var queryParameters = searchVal;
                        var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('groupMailGetDataFromAAD', queryParameters);
                        return AdminSafesManagement.groupMailGetDataFromAAD(null, updatedUrlOfEndPoint).then(
                            function(response) {
                            	if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }

                                if (UtilityService.ifAPIRequestSuccessful(response)) {
                                    var filterdUserData = [];
                                    $scope.userSearchList = response.data.data.values;
                                    $scope.userSearchList.forEach(function (userData) {
                                        if (userData.email != null && userData.email.substring(0, searchVal.length).toLowerCase() == searchVal) {
                                            filterdUserData.push(userData);
                                        }
                                    });
                                    if(val=="notification"){
                                    	$scope.isNotificationSearchLoading = false;
                                    }
                                    if(val=="owner"){
                                    	$scope.isOwnerSearchLoading = false;
                                    }
                                    if(val=="lead"){
                                    	$scope.isLeadSearchLoading = false;
                                    }
                                    return orderByFilter(filterFilter(filterdUserData, searchVal), 'email', true);
                                } else {
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                    $scope.error('md');
                                }
                            },
                            function(error) {
                                // Error handling function
                                console.log(error);
                                if(val=="notification"){
                                	$scope.isNotificationSearchLoading = false;
                                }
                                if(val=="owner"){
                                	$scope.isOwnerSearchLoading = false;
                                }
                                if(val=="lead"){
                                	$scope.isLeadSearchLoading = false;
                                }
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                $scope.error('md');
                        });
                    } catch (e) {
                        console.log(e);
                        if(val=="notification"){
                        	$scope.isNotificationSearchLoading = false;
                        }
                        if(val=="owner"){
                        	$scope.isOwnerSearchLoading = false;
                        }
                        if(val=="lead"){
                        	$scope.isLeadSearchLoading = false;
                        }
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');
                    }
                }
            }
            $scope.clearLeadEmail = function () {
                $scope.certificate.leadEmail = "";                
                $scope.isLeadSelectedForOnboard = false;
                $scope.leadEmailErrorMessage="";
            }

            $scope.clearOwnerEmail = function () {
                $scope.certificate.appOwnerEmail = "";                
                $scope.isOwnerSelectedForOnboard = false;
                $scope.ownerEmailErrorMessage="";
            }

            $scope.clearNotificationEmail = function() {
                $scope.notificationEmail = { email:""};
                $scope.notificationEmail.email = "";
                $scope.notificationEmailErrorMessage = '';
                $scope.isNotificationEmailSelected = false;
            }

            $scope.addNotificationEmail = function () {
            	event.preventDefault();
            	if($scope.notificationEmails==undefined){
            		$scope.notificationEmails = [];
            	}
                var length = $scope.notificationEmails.length;
                if ($scope.notificationEmail && $scope.notificationEmail.email!="" && !isDuplicateNotificationEmail($scope.notificationEmail.email)) {
                    var id="dns"+length;
                    angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+$scope.notificationEmail.email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                    $scope.notificationEmails.push({ "id": id, "email":$scope.notificationEmail.email});
                    addNotificationEmailString($scope.notificationEmail.email);
                    $scope.notificationEmail.email = "";
                    $scope.isNotificationEmailSelected = false;
                }else{
                    $scope.isNotificationEmailSelected = false;
                }
            }

            var addNotificationEmailString = function(email) {
                if ($scope.certificate.notificationEmails != "") {
                    $scope.certificate.notificationEmails = $scope.certificate.notificationEmails + ",";
                }
                $scope.certificate.notificationEmails = $scope.certificate.notificationEmails + email;
            }

            $scope.deleteNotificationEmail = function (id) {
                var index = id.substring(3);
                $scope.notificationEmailstr = "";
                $scope.selectedNotificationEmails = [];
                var deletedEmail = "";
                for (var i=0;i<$scope.notificationEmails.length;i++) {
                    if ($scope.notificationEmails[i]!=undefined && id != $scope.notificationEmails[i].id) {
                        var notifyId="dns"+$scope.selectedNotificationEmails.length;
                        $scope.selectedNotificationEmails.push({ "id": notifyId, "email":$scope.notificationEmails[i].email});
                        if($scope.selectedNotificationEmails.length==1){
                        	$scope.notificationEmailstr=$scope.notificationEmails[i].email;
                        }else{
                        	$scope.notificationEmailstr=$scope.notificationEmailstr+","+$scope.notificationEmails[i].email;
                        }
                    }
                    if (id == $scope.notificationEmails[i].id) {
                        deletedEmail = $scope.notificationEmails[i].email;
                    }
                }
                $scope.notificationEmails = $scope.selectedNotificationEmails;
                $scope.certificate.notificationEmails = $scope.notificationEmailstr;
                angular.element('#notificationEmailList').html("");
                var j = 0;
                $scope.notificationEmails.forEach(function (email) {
                    var id = "dns"+ (j++);
                    angular.element('#notificationEmailList').append($compile('<div class="row change-data item ng-scope" id="'+id+'"><div class="container name col-lg-10 col-md-10 col-sm-10 col-xs-10 ng-binding dns-name">'+email.email+'</div><div class="container radio-inputs col-lg-2 col-md-2 col-sm-2 col-xs-2 dns-delete"><div class="down"><div ng-click="deleteNotificationEmail(&quot;'+id+'&quot;)" class="list-icon icon-delete" role="button" tabindex="0"></div></div></div></div>')($scope));
                });
                $scope.isNotificationEmailSelected = false;
                if($scope.notificationEmail && $scope.notificationEmail.email != "" && deletedEmail.toLowerCase() == $scope.notificationEmail.email.toLowerCase()) {
                    $scope.notificationEmailErrorMessage = '';
                    $scope.isNotificationEmailSelected = true;
                }
            }


            var isDuplicateNotificationEmail = function (email) {
                $scope.certDnsErrorMessage = '';
                for (var i=0;i<$scope.notificationEmails.length;i++) {
                	if($scope.notificationEmails[i]!=undefined && $scope.notificationEmails[i].email){
                    if (email.toLowerCase() == $scope.notificationEmails[i].email.toLowerCase().trim()) {
                        $scope.notificationEmailErrorMessage = 'Duplicate Email';
                        return true;
                    }
                }
                }
                return false;
            }
            
            var addOwnerTonotificationList = function (email) {
            	$scope.notifList  = email.split(',')            	
            	var count =0;
            	if(email!=null && email!="" && email!=undefined){
                for (var i=0;i<$scope.notifList.length;i++) {
                    if ($scope.certificate.ownerEmail.toLowerCase() == $scope.notifList[i].toLowerCase()) {
                    	count++;
                    }
                }
                    if(count ==0){
                    	email = email+","+$scope.certificate.ownerEmail;
                    }
            	}else{
            		email = $scope.certificate.ownerEmail;
            	}
                return email;
            }


            $scope.onboardCert = function() {
                var onboardRequest = {
                    certificateName: $scope.certificateToOnboard.certificateName,
                    certificateType:$scope.certificateToOnboard.certificateType,
                    applicationName: $scope.certificateToOnboard.tag,
                    notificationEmails: $scope.certificateToOnboard.notificationEmails,
                    ownerEmail: $scope.certificateToOnboard.ownerEmail,
                    ownerNtId: $scope.certificateToOnboard.ownerNtId
                }
                console.log(onboardRequest);
            }

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
    'vault.constants.AppConstant',
    'vault.constants.RestEndpoints'
]));