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
    app.service( 'SafesManagement', function( $http, SessionStore, ServiceEndpoint, $q, DataCache, fetchData, $rootScope, ModifyUrl, ErrorMessage, UtilityService, Modal ) {


        this.getFolderContents = function (path) {
            return ServiceEndpoint.readAllContents.makeRequest(null,
                ServiceEndpoint.readAllContents.url + '?path=' + path)
                .then(function(response) {
                    if(UtilityService.ifAPIRequestSuccessful(response)) {
                        var contents = [];
                         response.data.children.map(function (element) {
                            if(element.type === 'secret') {
                                var secrets = JSON.parse(element.value);
                                Object.keys(secrets.data).forEach(function (key) {
                                    if(key === 'default' && secrets.data['default'] === 'default') return;

                                    contents.push({
                                        type: 'secret',
                                        id: key,
                                        key: key,
                                        value: secrets.data[key],
                                        parentId: element.parentId
                                    })
                                })
                            } else if (element.type === 'folder') {
                                contents.push(element);
                            }
                        });
                         response.data.children = contents;

                        return response.data;
                    } else {
                        return $q.reject(response);
                    }
                });
        };

        this.createFolderV2 = function (folderPath) {
            return $http({
                method: ServiceEndpoint.createFolderV2.method,
                url: ServiceEndpoint.createFolderV2.url + '?path=' + folderPath,
                headers: {
                    'vault-token': SessionStore.getItem('myVaultKey')
                }
            })
                .then(function(response) {
                    return response.data;
                })
        }

        this.writeSecretV2= function (path, data) {
            return $http({
                method: ServiceEndpoint.writeSecretV2.method,
                url: ServiceEndpoint.writeSecretV2.url,
                data: {
                    path: path,
                    data: data
                },
                headers: {
                    'vault-token': SessionStore.getItem('myVaultKey')
                }
            })
                .then(function(response) {
                    return response.data;
                })
        }

        this.saveNewFolder = function(payload, path) {
            var url = ModifyUrl.addUrlParameteres('saveNewFolder', path);
            return ServiceEndpoint.saveNewFolder.makeRequest(payload, url).then(
                function(response) {
                    return response;
                }
            );
        };

        this.getFolderData = function(payload, path) {
          var url = ModifyUrl.addUrlParameteres('safesList', path);
          return ServiceEndpoint.safesList.makeRequest(null, url).then(
              function(response) {
                  return response;
              },
              function(error){
                  return error;
              }
          );
        };

        this.getSecrets = function(payload, path) {
          var url = ModifyUrl.addUrlParameteres('getSecrets', path);
          return ServiceEndpoint.getSecrets.makeRequest(null, url).then(
              function(response) {
                return response;
              }
          );
        };

        this.writeSecret = function(payload) {
          return ServiceEndpoint.postSecrets.makeRequest(payload).then(
              function(response) {
                return response;
              }
          );
        };
        this.deleteFolder = function(payload, path) {
          var url = ModifyUrl.addUrlParameteres('deleteSafe', path);
          return ServiceEndpoint.deleteSafe.makeRequest(payload, url).then(
              function(response) {
                return response;
              }
          );
        };

        this.getTheRightErrorMessage = function(responseObject){
            if(responseObject.status===500 || responseObject.statusText==='Internal Server Error'){
                return ErrorMessage.ERROR_NETWORK;
            }
            else if(responseObject.status===404 || responseObject.statusText==="Not Found"){
                return ErrorMessage.ERROR_CONTENT_NOT_FOUND;    // TODO: show different messages for POST and GET methods
            }
            else if(responseObject.status===422 || responseObject.data.errors[0].indexOf("Exist") > 0){
                return ErrorMessage.ERROR_CONTENT_EXISTS;    
            }
            else{
                return ErrorMessage.ERROR_GENERAL;
            }
        };
    } );
})(angular.module('vault.services.SafesManagement',[]));
