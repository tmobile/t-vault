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
    app.controller('SafesCtrl', function($scope, fetchData, SafesManagement, ModifyUrl, AdminSafesManagement,
          Modal, CopyToClipboard,  $rootScope, $http, $log, $state, SessionStore, UtilityService, ArrayFilter, $stateParams, Notifications){

        $scope.searchValue = ''; // Initial search filter value kept empty
        $rootScope.isLoadingData = false; // Variable to set the loader on
        $scope.isLoadingModalData = false;
        $scope.fetchDataError = false; // set when there is any error while fetching or massaging data
        $scope.slide = false; // Variable to slide the table containers
        $scope.slideItems = []; // the array whose content will be used to render data in slider page
        $scope.slideHeader = ''; // header of the slide page
        $scope.slideHeaderDescription = ''; // description of the slide page
        $scope.currentCategory = 'users';
        $rootScope.categories = [{
                "name": "My Safes",
                "id": "users"
            },
            {
                "name": "Shared Safe",
                "id": "shared"
            },
            {
                "name": "Applications Safe",
                "id": "apps"
            }
        ];

        $scope.radio = {
            value: 'Write',
            options: [{
                'text': 'Read'
            }, {
                'text': 'Write'
            }, {
                'text': 'Admin'
            }]
        };

        $scope.safesNavTags = [{
            displayName: 'SAFES',
            navigationName: 'safes',
            addComma: false,
            show: true
        }, {
            displayName: 'ADMIN',
            navigationName: 'admin',
            addComma: false,
            show: SessionStore.getItem("isAdmin") == 'true'
        }, {
            displayName: 'HEALTH',
            navigationName: 'health',
            addComma: false,
            show: false                    // Hidden temporarily
        }, {
            displayName: 'ALERTS',
            navigationName: 'alerts',
            addComma: false,
            show: false                    // Hidden temporarily
        }, {
            displayName: 'DOCUMENTATION',
            navigationName: 'documentation',
            addComma: false,
            show: true                    
        }];

        // Accordion table

        $scope.actionDropDownOptions = {
            'selectedGroupOption': {
                "type": "Action"
            },
            'tableOptions': [{
                "type": "Edit",
                "srefValue": {
                    'url': 'change-safe',
                    'obj': 'safeObject',
                    'myobj': 'listDetails'
                }
            }, {
                "type": "Delete",
                "srefValue": 'href'
            }]
        };

        // modal popup

        var error = function(size) {
            Modal.createModal(size, 'error.html', 'SafesCtrl', $scope);
        };

        $scope.createFolder = function(cat) {
            Modal.createModal('md', 'createNewFolderPopup.html', 'SafesCtrl', $scope);
        };

        $scope.saveNewFolder = function(newFolderName) {
            Modal.close();
            if (newFolderName) {
                $scope.isLoadingData = true;
                try {
                    var safeName = $scope.currentSafe;
                    newFolderName = UtilityService.formatName(newFolderName);
                    var path = $scope.currentCategory + "/" + safeName + "/" + newFolderName;                    
                    SafesManagement.saveNewFolder(null, path).then(
                        function(res) {
                            if(UtilityService.ifAPIRequestSuccessful(res)){
                                $scope.isLoadingData = false;
                                var categoryIndex = ArrayFilter.findIndexInArray("id", $scope.currentCategory, $rootScope.categories);
                                var index = ArrayFilter.findIndexInArray("safe", safeName, $rootScope.categories[categoryIndex].tableData);
                                var obj = {
                                    "name": newFolderName,
                                    "keys": [{
                                        "key": "default",
                                        "value": "default"
                                    }],
                                    "appIndex" : categoryIndex,
                                    "safeIndex": index
                                };

                                $rootScope.categories[categoryIndex].tableData[index].folders.push(obj);
                                $scope.slideItems = $rootScope.categories[categoryIndex].tableData[index].folders;
                                // Modal.save();
                                var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                                Notifications.toast(newFolderName+notification);
                            }
                            else{
                                $scope.isLoadingData = false;
                                $scope.errorMessage = SafesManagement.getTheRightErrorMessage(res);
                                error('md');
                            }
                            
                        },
                        function(error) {

                            // Error handling function
                            console.log(error);
                            $scope.isLoadingData = false;
                            $scope.errorMessage = SafesManagement.getTheRightErrorMessage(error);
                            // $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                            $scope.error('md');
                        });
                } catch(e) {
                    console.log(e);
                    $scope.isLoadingData = false;
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    $scope.error('md');                    
                }
                // Modal.save();
            } 
        };

        $scope.close = function() {
            Modal.close();
        };

        // slider to slide the table containers left and right

        $scope.sliderFunction = function(item) {
            $scope.slide = !$scope.slide;
            if (item != null && item != undefined) {
                $scope.slideAuth = item.auth;
                $scope.slideHeader = item.safe;     
                $scope.currentSafe = item.safe; 
                $scope.requestDataFrSafes(item.safe); //This function is not required to be called
                //getFolderDetailsOfSafe
                $state.transitionTo('safes', {safe: item.safe}, { notify: false });
                try{   
                    $scope.slideHeaderDescription = '';
                    $rootScope.isLoadingData = true;             
                    var queryParameters = "path=users" + '/' + item.safe;
                    var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getSafeInfo',queryParameters);
                    AdminSafesManagement.getSafeInfo(null, updatedUrlOfEndPoint).then(
                        function(response) {    
                            if(UtilityService.ifAPIRequestSuccessful(response)){                                   
                                // Try-Catch block to catch errors if there is any change in object structure in the response
                                try {
                                    $rootScope.isLoadingData = false;  
                                    $scope.slideHeaderDescription = response.data.data.description;
                                }
                                catch(e) {
                                    console.log(e);
                                    $rootScope.isLoadingData = false;  
                                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                                    // $scope.error('md');
                                }
                            }
                            else {
                                $rootScope.isLoadingData = false;  
                                $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                                // error('md');
                            }
                        },
                        function(error) {
                            // Error handling function
                            console.log(error);
                            $rootScope.isLoadingData = false;  
                            $scope.errorMessage = SafesManagement.getTheRightErrorMessage(error);
                            // $scope.error('md');

                    })
                } catch(e) {
                    // To handle errors while calling 'fetchData' function
                    console.log(e);
                    $rootScope.isLoadingData = false;  
                    $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                    // $scope.error('md');

                }                          
            }
        };

        $scope.resetSlide = function(cat) {
            $scope.currentCategory = cat.id;
            $scope.slide = false;
        };

        // Fetching data

        $scope.init = function() {
            if(!SessionStore.getItem('myVaultKey')){ /* Check if user is in the same session */
                $state.go('signup');
            }
            else{ /* If user is not in the same session, redirect him to the login screen */
                $rootScope.access = JSON.parse(SessionStore.getItem('accessSafes'));
                $scope.massageSafesList();
            }
        };

        // Fetching Data

        $scope.massageSafesList = function() {
            try {

                $rootScope.categories[0].tableData = [];
                $rootScope.categories[1].tableData = [];
                $rootScope.categories[2].tableData = [];

                // User Safes

                if ($rootScope.access.users && $rootScope.access.users.length !== 0) {
                  $rootScope.access.users.forEach(function(key) {
                      var keyName = Object.keys(key)[0];
                      var obj = {
                          "safe": keyName,
                          "folders": [],
                          "auth": key[keyName]
                      };
                      $rootScope.categories[0].tableData.push(obj);
                  })
                }

                // Share Safes

                if ($rootScope.access.shared && $rootScope.access.shared.length !== 0) {
                  $rootScope.access.shared.forEach(function(key) {
                      var keyName = Object.keys(key)[0];
                      var obj = {
                          "safe": keyName,
                          "folders": [],
                          "auth": key[keyName]
                      };
                      $rootScope.categories[1].tableData.push(obj);
                  })
                }
                if ($rootScope.access.apps && $rootScope.access.apps.length !== 0) {
                  $rootScope.access.apps.forEach(function(key) {
                      var keyName = Object.keys(key)[0];
                      var obj = {
                          "safe": keyName,
                          "folders": [],
                          "auth": key[keyName]
                      };
                      $rootScope.categories[2].tableData.push(obj);
                  })
                }

            } catch (e) {

                // To handle errors while massaging data
                console.log(e);
                $rootScope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_PROCESSING_DATA');
                $scope.error('md');

            }
        };

        $scope.error = function (size) {
            Modal.createModal(size, 'error.html', 'SafesCtrl', $scope);
        };
        if(SessionStore.getItem("allSafes") === null || SessionStore.getItem("allSafes") === undefined) {
            $rootScope.safes = [];
            SafesManagement.getFolderData(null, 'path=apps').then(function(response) {
                if(String(response.status) !== "404" && (response.data.keys !== undefined && response.data.keys !== null)) {
                    response.data.keys.forEach(function(item, index) {
                        $rootScope.safes.push(item);
                    } );
                }                
                SafesManagement.getFolderData(null, 'path=users').then(function(response) {
                    if(String(response.status) !== "404" && (response.data.keys !== undefined && response.data.keys !== null)) {
                        response.data.keys.forEach(function(item, index) {
                            $rootScope.safes.push(item);
                        } );
                    }
                    SafesManagement.getFolderData(null, 'path=shared').then(function(response) {
                        if(String(response.status) !== "404" && (response.data.keys !== undefined && response.data.keys !== null)) {
                            response.data.keys.forEach(function(item, index) {
                                $rootScope.safes.push(item);
                            } );
                        }
                        SessionStore.setItem("allSafes", JSON.stringify($rootScope.safes));
                    });
                });
            });
        }         
        
        $scope.requestDataFrSafes = function(safe) {
            try {
                $rootScope.isLoadingData = true;
                var path = "path=" + $scope.currentCategory + "/" + safe;

                return SafesManagement.getFolderContents($scope.currentCategory + '/' + safe)
                    .then(function(folderContents) {
                        $rootScope.isLoadingData = false;
                            $scope.slideItems = folderContents.children;

                    })
                    .catch(function (error) {
                        console.log(e);
                        $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                        error('md');
                        $rootScope.isLoadingData = false;
                    });

                // path = $scope.currentCategory.
                return SafesManagement.getFolderData(null, path).then(
                    function(response) {
                            var data = response.data;
                            $rootScope.isLoadingData = false;
                            if (data.keys) {
                                var index;
                                switch ($scope.currentCategory) {
                                    case "users":
                                    index = 0;
                                    break;
                                    case "shared":
                                    index = 1;
                                    break;
                                    case "apps":
                                    index = 2;
                                    break;
                                    default:
                                    index = 0;
                                }

                                var safeIndex = ArrayFilter.findIndexInArray("safe", safe, $rootScope.categories[index].tableData);
                                var folders = []
                                data.keys.forEach(function(folder) {
                                    var folderObj = {
                                        "name": folder,
                                        "appIndex": index,
                                        "safeIndex": safeIndex
                                    }
                                    folders.push(folderObj);
                                })
                                $rootScope.categories[index].tableData[safeIndex].folders = folders;
                                $scope.slideItems = $rootScope.categories[index].tableData[safeIndex].folders;
                            }
                        // }
                        // else{
                        //     $scope.slideItems = [];
                        //     $rootScope.isLoadingData = false;
                        //     $scope.errorMessage = SafesManagement.getTheRightErrorMessage(response);
                        //     if($scope.errorMessage !=='Requested content not found!'){
                        //         error('md');
                        //     }
                        // }
                    })
                    .catch(function (error) {
                        console.log(error);
                    })// wraps the first request

            } catch (e) {
                // To handle errors while calling 'fetchData' function
                console.log(e);
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                error('md');
                $rootScope.isLoadingData = false;
            } // wraps outer catch block
        } // wraps the whole function


        $scope.navigateToFolder = function (folder) {
            $rootScope.isLoadingData = true;
            return SafesManagement.getFolderContents(folder.id)
                .then(function(data) {
                    $rootScope.isLoadingData = false;
                    $scope.slideItems = data.children;
                })
        };

        $scope.init();


    });
})(angular.module('vault.features.SafesCtrl', [
    'vault.services.SafesManagement',
    'vault.services.Notifications'
]));
