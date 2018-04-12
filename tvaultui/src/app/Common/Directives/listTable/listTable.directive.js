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
(function(app) {
    app.directive( 'listtable', function(CopyToClipboard, Modal, $rootScope, SafesManagement, $state, ArrayFilter, Notifications, UtilityService) {
        return {
            restrict: 'E',
            templateUrl: 'app/Common/Directives/listTable/listTable.html',
            scope: {
                columns: '=',
                data: '=',
                loading: '=',
                type: '=',
                admin: '=',
                searchValue: '=',
                searchObject: '=?',
                deleteFolder : '&',
                editFolder : '&',
                folder: '&',
                auth: "="
            },
            link: function( scope, element, attrs ) {
                // scope.showPassword = false;
                scope.expandBlock = false;
                if(scope.type === 'accordion') {
                    scope.accordion = true;
                }
                scope.expandRow = function(index) {
                  if(scope.expandBlockIndex !== index) {
                    scope.expandBlock = true;
                  }
                  else {
                    scope.expandBlock = !scope.expandBlock;
                  }                  
                  scope.expandBlockIndex = index;
                }
                scope.copy = function (value) {
                    var notification = UtilityService.getAParticularSuccessMessage('COPY_TO_CLIPBOARD');
                    Notifications.toast(notification);
                    CopyToClipboard.copy(value);
                }
                scope.loadFolderData = function (folder) {
                  if($rootScope.categories[folder.appIndex]) {
                      scope.isLoadingSecrets = true;
                      var app = $rootScope.categories[folder.appIndex].id;
                      var safe = $rootScope.categories[folder.appIndex].tableData[folder.safeIndex].safe;
                      var folderName = folder.name;
                      var path = app + "/" + safe + "/" + folderName;
                      folder.keys = []; /* Resetting keys in folder before loading it */
                      try {
                          SafesManagement.getSecrets(null, path).then(function(res, err) {
                              if(UtilityService.ifAPIRequestSuccessful(res)){
                                 try {
                                     res = res.data.data;
                                    var keys = Object.keys(res);
                                    var keysArray = [];
                                    keys.forEach(function(key) {
                                        var key = {
                                            "key": key,
                                            "value": res[key]
                                        };
                                        keysArray.push(key);
                                    });
                                    folder.keys = keysArray;
                                    scope.isLoadingSecrets = false;
                                 } catch(e) {
                                    console.log(e);
                                    scope.isLoadingSecrets = false;
                                 }
                              }
                              else {
                                console.log(err);
                                scope.isLoadingSecrets = false;
                              }
                        });
                      } catch(e) {
                         // To handle errors while calling 'fetchData' function
                        console.log(e);
                        scope.isLoadingSecrets = false;
                      }
                  }
                }

                scope.addNewSecret = function(index) {
                  scope.newSecretAdded = true;
                  var obj = {
                    "key": '',
                    "value": '',
                    "new": true
                  }
                  scope.data[index].keys.push(obj);
                }
                scope.cancelSafe = function(index) {
                  var keyData = [];
                  if(scope.data[index].keys) {
                      for (var i = 0; i < scope.data[index].keys.length; i++) {
                        if (!scope.data[index].keys[i].new) {
                          keyData.push(scope.data[index].keys[i]);
                        }
                      }
                      scope.data[index].keys = keyData;
                      scope.newSecretAdded = false;
                  }
                }

                scope.saveSafe = function(folder) {

                  //IF DUPLICATE KEY EXISTS STAY
                  var newestKey = folder.keys[folder.keys.length - 1].key;
                  var duplicateKeys = false;
                  folder.keys.forEach(function (key, index) {
                        if(newestKey === key.key && index !== folder.keys.length - 1) {
                              duplicateKeys = true;
                              Modal.createModal('lg', 'duplicateSecretError.html');
                          }
                    });
                  if(duplicateKeys) return;


                  if($rootScope.categories[folder.appIndex]) {
                      // $rootScope.isLoadingData = true;
                      if(scope.deletingSecret === true) {
                        if($rootScope.deleteSecretIndex >= 0) {
                          $rootScope.folderToSend.keys.splice($rootScope.deleteSecretIndex, 1);
                        }
                      }
                      scope.isLoadingSecrets = true;
                      var app = $rootScope.categories[folder.appIndex].id;
                      var safe = $rootScope.categories[folder.appIndex].tableData[folder.safeIndex].safe;
                      var folderName = folder.name;
                      var obj = {
                        "path": '',
                        "data": {}
                      }
                      obj.path = app + "/" + safe + "/" + folderName;
                      folder.keys.forEach(function (secret) {
                        if (secret.key !== "default" || secret.value !== "default") {
                          obj.data[secret.key] = secret.value;
                        }
                      })
                      // To avoid error while sending empty data
                      if(Object.keys(obj.data).length === 0) {
                        obj.data['default'] = 'default';
                      }
                      try {
                          SafesManagement.writeSecret(obj).then(function (res, err) {
                              if(UtilityService.ifAPIRequestSuccessful(res)){
                                  try {
                                      //TODO: reload just this page instead of whole safes
                                      scope.isLoadingModalData = false;
                                      scope.isLoadingSecrets = false;
                                      // remove the New flag from keys
                                      folder.keys.forEach(function(key) {
                                        if (key.new) {
                                          key.new = false;
                                        }
                                      })
                                      Modal.close();
                                      // Notification for adding
                                      var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_ADD_SUCCESS');
                                      if(scope.deletingSecret == true) {
                                        scope.deletingSecret = false;
                                        // Notification for deleting
                                        var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_SAFE_DELETE');
                                      }
                                      Notifications.clearToastr();
                                      Notifications.toast('Secret'+notification);
                                      scope.newSecretAdded = false;
                                      // scope.$apply();
                                  } catch(e) {
                                      console.log(e);
                                      scope.isLoadingModalData = false;
                                      scope.isLoadingSecrets = false;
                                      scope.deletingSecret = false;
                                  }
                              }
                              else {
                                  console.log(err);
                                  scope.isLoadingModalData = false;
                                  scope.isLoadingSecrets = false;
                                  scope.deletingSecret = false;
                              }
                          })
                      } catch(e) {
                          console.log(e);
                          scope.isLoadingModalData = false;
                          scope.isLoadingSecrets = false;
                          scope.deletingSecret = false;
                      }
                   }
                }

                scope.openDeleteSecretModal = function (folder, index) {
                  scope.fetchDataError = false;
                  // folder.keys.splice(index, 1);
                  $rootScope.deleteSecretIndex = index;
                  $rootScope.folderToSend = folder;
                  var size = 'md';
                  Modal.createModal(size, 'deleteSecretPopup.html', 'safesCtrl', $rootScope);
                }

                scope.openDeleteFolderModal = function (folder, event) {
                  event.stopPropagation();
                  $rootScope.folderToDelete = folder;
                  scope.folderToDelete = {'folder' : folder};
                  if(scope.admin) {
                    Modal.createModal('md', 'deleteFolderPopup.html', 'AdminCtrl', $rootScope);
                  }
                  else {
                    Modal.createModal('md', 'deleteFolderPopup.html', 'safesCtrl', $rootScope);
                  }
                }

                $rootScope.deleteFolder = function() {
                  if(scope.admin) {
                    $rootScope.deleteSafe($rootScope.folderToDelete);
                  }
                  else {
                    Modal.close();
                    scope.isLoadingModalData = true;
                    $rootScope.isLoadingData = true;
                    var folder = $rootScope.folderToDelete;
                    if($rootScope.categories[folder.appIndex]) {
                        var app = $rootScope.categories[folder.appIndex].id;
                        var safe = $rootScope.categories[folder.appIndex].tableData[folder.safeIndex].safe;
                        var folderName = folder.name;
                        var path = "path=" + app + "/" + safe + "/" + folderName;
                        try {
                          SafesManagement.deleteFolder(null, path).then(function(res) {
                              if(UtilityService.ifAPIRequestSuccessful(res)){
                                try {
                                    var index = ArrayFilter.findIndexInArray("name", folderName, scope.data);
                                    scope.data.splice(index, 1);
                                    //scope.$apply(); // not required, angular throws an error
                                    scope.isLoadingModalData = false;
                                    $rootScope.isLoadingData = false;
                                    Modal.close();
                                    var notification = UtilityService.getAParticularSuccessMessage('MESSAGE_SAFE_DELETE');
                                    Notifications.toast(folder.name+notification);
                                }
                                catch(e) {
                                    console.log(e);
                                    scope.isLoadingModalData = false;
                                    $rootScope.isLoadingData = false;
                                }
                              }
                              else {
                                console.log('error', res);
                                scope.isLoadingModalData = false;
                                $rootScope.isLoadingData = false;
                              }
                          })
                        } catch(e) {
                            console.log(e);
                            scope.isLoadingModalData = false;
                            $rootScope.isLoadingData = false;
                        }
                    }
                  }
                }

                $rootScope.deleteSecret = function() {
                  Modal.close();
                  scope.isLoadingModalData = true;
                  // scope.isLoadingSecrets = true;
                  scope.deletingSecret = true;
                  scope.saveSafe($rootScope.folderToSend);
                }

                $rootScope.close = function() {
                  Modal.close();
                }


            }
        }
    } );
})(angular.module('vault.directives.listtable',[]))
