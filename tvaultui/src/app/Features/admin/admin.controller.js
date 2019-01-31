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
(function(app){
    app.controller('AdminCtrl', function($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService){

        $scope.filterValue = '';            // Initial search filter value kept empty
        $scope.isLoadingData = false;       // Variable to set the loader on
        $scope.fetchDataError = false;      // set when there is any error while fetching or massaging data
        $scope.dataForTable = [];           // Array of data after massaging, to be used for table display
        $scope.tilesData = {};
        $scope.tilesData["SafesData"] = [];

        // Type of safe to be filtered from the rest

        $scope.safeType = {
            "type" : ""
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

        
        $scope.adminNavTags = safesService.getSafesNavTags();

        $scope.showNotification = function() {
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
                "srefValue" : {
                    'url' : 'change-safe',
                    'obj' : 'safeObject',
                    'myobj': 'listDetails'
                }
            },{
                "type": "Delete",
                "srefValue" : 'href'
            }
            ]
        };

        var init = function () {

            $scope.myVaultKey = SessionStore.getItem("myVaultKey");
            if(!$scope.myVaultKey){ /* Check if user is in the same session */
                $state.go('signup');
            }
            else{
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.requestDataFrAdmin();
            }
        };

        // Updating the data based on type of safe, by clicking dropdown
        $scope.filterUpdate = function(option) {
            $scope.filterValue = option.value;
            if(option.value === 'All safes') {
                $scope.filterValue = '';
            }
            $scope.selectedGroupOption = option;
        };

        // massaging data from server
        $scope.massageDataForTiles = function(data,currentVaultType) {
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

                for(var i=0; i< data.keys.length; i++) {  

                    newobj["safes"][i] = {};
                    newobj["safes"][i]["safe"] = data.keys[i];
                    newobj["safes"][i]["safeType"] = currentVaultType;
                }
                obj.push(newobj);
                $scope.tilesData.SafesData = obj;
                if($scope.tilesData.SafesData.length === 3){
                    $scope.isLoadingData = false;
                    $scope.data = $scope.tilesData.SafesData;
                    $scope.massageData($scope.data);
                }
            } catch (e) {

                // To handle errors while massaging data
                console.log(e);
                $rootScope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                $scope.error('md');

            }
        };

        $scope.massageData = function(data) {
            try {
                $scope.dataForTable = [];
                for(var i=0; i< data.length; i++) {            // for each of the 'Types' of safes
                    var safes= data[i].safes;
                    var type = data[i].type;
                    for(var j=0; j<safes.length; j++) {        // for each safe in the current type of safe
                        var currentSafeObject = safes[j];
                        currentSafeObject["type"] = type;
                        currentSafeObject["safeType"] = safes[j].safeType;
                        $scope.dataForTable.push(currentSafeObject);
                    }
                }
            } catch(e) {
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
            var queryParameters = "path="+fullObj.safeObject.safeType + '/' + fullObj.safeObject.safe;
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSafeInfo',queryParameters);
            AdminSafesManagement.getSafeInfo(null, updatedUrlOfEndPoint).then(
                function(response) {
                    if(UtilityService.ifAPIRequestSuccessful(response)){
                        // Try-Catch block to catch errors if there is any change in object structure in the response
                        try {
                            $scope.isLoadingData = false;
                            var object = response.data.data;
                            if(object.name && object.owner && object.description) {
                                $state.go('change-safe', fullObj );
                            }
                            else {
                                $scope.isLoadingData = false;
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_CONTENT_NOT_FOUND');
                                $scope.error('md');
                            }
                        }
                        catch(e) {
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
                function(error) {
                    // Error handling function
                    console.log(error);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_CONTENT_NOT_FOUND');
                    $scope.error('md');

            })  
        };
        $scope.deleteSafePopup = function(safeToDelete) {
            $scope.fetchDataError = false;
            $rootScope.safeToDelete = safeToDelete;
            Modal.createModal('md', 'deleteSafePopup.html', 'AdminCtrl', $scope);
        };
        $rootScope.deleteSafe = function (listItem) {
            if($rootScope.safeToDelete !== null && $rootScope.safeToDelete !== undefined) {
                listItem = $rootScope.safeToDelete;
            }            
            $rootScope.safeToDelete = null;
            try{
                $scope.isLoadingData = true;
                Modal.close();
                var queryParameters = "path="+listItem.safeType + '/' + listItem.safe;
                var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('deleteSafe',queryParameters);
                AdminSafesManagement.deleteSafe(null, updatedUrlOfEndPoint).then(
                    function(response) {
                        if(UtilityService.ifAPIRequestSuccessful(response)){
                            $scope.isLoadingData = false;
                            var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_SAFE_DELETE');
                            Notifications.toast(listItem.safe+notification);
                            // remove deleted safe from session storage
                            var currentSafesList = JSON.parse(SessionStore.getItem("allSafes"));
                            var index = currentSafesList.indexOf(listItem.safe);
                            if (index > -1) {
                                currentSafesList.splice(index, 1);
                                SessionStore.setItem('allSafes', JSON.stringify(currentSafesList));
                            }
                            // Try-Catch block to catch errors if there is any change in object structure in the response
                            try {
                                
                                for(var i=0; i < $scope.tilesData.SafesData.length ; i++){

                                    if($scope.tilesData.SafesData[i].type == listItem.type){

                                        for(var j=0; j < $scope.tilesData.SafesData[i].safes.length ; j++){
                                            if($scope.tilesData.SafesData[i].safes[j].safe == listItem.safe){
                                                $scope.tilesData.SafesData[i].safes.splice(j, 1);
                                                $scope.data = $scope.tilesData.SafesData;
                                                $scope.massageData($scope.data);
                                            }
                                        }
                                    }
                                }
                            } 
                            catch(e) {
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
                    function(e) {
                        console.log(e);
                        // Error handling function
                        $scope.isLoadingData = false;
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        $scope.error('md');

                })
            } catch(e) {
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

        // Fetching Data

        $scope.requestDataFrAdmin = function () {

            var vaultTypes = ["apps","shared","users"];

            var responseArray = [];
            var allSafes = [];
            vaultTypes.forEach(function(currentVaultType) {
                try{

                    var queryParameters = "path="+currentVaultType;
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('safesList',queryParameters);
                    $scope.isLoadingData = true;
                    AdminSafesManagement.getCompleteSafesList(null,updatedUrlOfEndPoint).then(                        
                        function(response) {
                            if(UtilityService.ifAPIRequestSuccessful(response)){ 
                                $scope.isLoadingData = false;
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    allSafes = allSafes.concat(response.data.keys);
                                    SessionStore.setItem('allSafes', JSON.stringify(allSafes));
                                    $scope.massageDataForTiles(response.data,currentVaultType);
                                }
                                catch(e) {
                                    console.log(e);
                                    $scope.error('md');
                                }
                            }
                            else {
                                $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                                $scope.error('md');
                            }                             
                        }, 
                        function(error) {
                            // Error handling function
                            if(error.status !== 404) {
                                $scope.isLoadingData = false;
                                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                                $scope.error('md');
                            }   
                            else {
                                $scope.massageDataForTiles([], currentVaultType);
                            }
                    })
                } catch(e) {
                    // To handle errors while calling 'fetchData' function
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');

                }
            });
            
        };

        init();
        
    });
})(angular.module('vault.features.AdminCtrl',[
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications'
]));