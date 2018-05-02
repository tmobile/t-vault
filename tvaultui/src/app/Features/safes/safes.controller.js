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
        $scope.currentItem = null;
        $scope.folders = [];

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

        $scope.createSecret = function () {
            newSecret = $scope.folders[$scope.folders.length - 1].children
                .find(function (item) {
                    return item.isNew;
                });
            if(newSecret) return;
            var newSecret = {
                isNew: true,
                type: 'secret',
                id: $scope.folders[$scope.folders.length - 1].id,
                key: '',
                value: '',
                parentId: $scope.folders[$scope.folders.length - 1].id
            }
            $scope.folders[$scope.folders.length - 1].children.splice(0, 0, newSecret);
        }

        $scope.createFolder = function(cat) {
            Modal.createModal('md', 'createNewFolderPopup.html', 'SafesCtrl', $scope);
        };

        $scope.saveNewFolder = function(newFolderName) {
            if(!newFolderName) return;
            Modal.close();
                $rootScope.isLoadingData = true;
                if($scope.folders.length) {
                    var folderList = $scope.folders[$scope.folders.length - 1].id.split('/');
                } else {
                    var folderList = [];
                }

                folderList.push(newFolderName);
                var newFolderPath = folderList.join('/');
                var promise;
                if(folderList.length > 3) {
                    promise = SafesManagement.createFolderV2(newFolderPath);
                }  else {
                    promise = SafesManagement.saveNewFolder(null, newFolderPath);
                }

                promise.then(function(res) {
                        var categoryIndex = ArrayFilter.findIndexInArray("id", $scope.currentCategory, $rootScope.categories);
                        var index = ArrayFilter.findIndexInArray("safe", $scope.currentSafe, $rootScope.categories[categoryIndex].tableData);
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
                        return $scope.navigateToFolder(newFolderPath);
                    })
                    .then(function() {
                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                        Notifications.toast(newFolderName+notification);
                    })
                    .catch(function (res) {
                        $scope.errorMessage = SafesManagement.getTheRightErrorMessage(res);
                        error('md');
                    });
        };

        $scope.close = function() {
            Modal.close();
        };

        // slider to slide the table containers left and right

        $scope.goToFolders = function(item) {
            $scope.slide = !$scope.slide;
            $scope.slideAuth = item.auth;
            $scope.slideHeader = item.safe;
            $scope.currentSafe = item.safe;
            $scope.currentItem = item;
            $scope.navigateToFolder($scope.currentCategory + '/' + item.safe);
        };

        $scope.goToSafes = function () {
            $scope.slide = false;
            $scope.folders = [];
        };

        $scope.resetSlide = function(cat) {
            $scope.currentCategory = cat.id;
            $scope.slide = false;
        };

        // Fetching data

        $scope.init = function() {
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

        $scope.goToFolderInPath = function(folder) {
            $rootScope.isLoadingData = true;
            for(var i = $scope.folders.length - 1; i >= 0; i -= 1) {
                var folderInPath = $scope.folders[i];
                if(folderInPath.id === folder.id) {
                    return SafesManagement.getFolderContents(folderInPath.id)
                        .then(function(folderData) {
                            $scope.folders.pop();
                            $scope.folders.push(folderData);
                            $rootScope.isLoadingData = false;
                        })
                }
                $scope.folders.pop();
            }
        };


        $scope.navigateToFolder = function (folderPath) {
            return SafesManagement.getFolderContents(folderPath)
                .then(function(folderData) {
                    $scope.folders.push(folderData);
                    $scope.slideItems = folderData.children;
                    $rootScope.isLoadingData = false;
                })
        };

        $scope.init();


    });
})(angular.module('vault.features.SafesCtrl', [
    'vault.services.SafesManagement',
    'vault.services.Notifications'
]));
