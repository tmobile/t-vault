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

angular.module('vault.services.VaultUtility', [])
    .service('vaultUtilityService', function(fetchData, UtilityService, SessionStore, ModifyUrl, $q, $http, $rootScope, RestEndpoints) {
        this.canceller = {};
        this.getDropdownDataForPermissions = function(searchFieldName, searchFieldText) {          
            return new Promise(function(resolve, reject) {
                var data = {};
                var DataUrl;
                if(searchFieldName === "userName") {
                    DataUrl = RestEndpoints.baseURL + RestEndpoints.usersGetData;
                } else if (searchFieldName === "groupName") {
                    DataUrl = RestEndpoints.baseURL + RestEndpoints.groupGetData;
                }                                
                DataUrl = DataUrl + searchFieldText;              
                 try {
                     data.loadingDataFrDropdown = true;
                     // Abort pending requests before making new request
                     if (Object.keys(this.canceller).length !== 0) {
                        this.canceller.resolve();
                     }
                    fetchUsersData(DataUrl)
                     .then(
                         function(response) {
                             try {
                                 data.response = response;
                                 var dataFrmApi = response.data.data.values;

                                 if (response.statusText !== "OK") {
                                     data.loadingDataFrDropdown = false;
                                     data.erroredFrDropdown = false;
                                     data.successFrDropdown = false;
                                     resolve(data);
                                 } else {
                                     data.dataFrmApi = dataFrmApi;
                                     data.loadingDataFrDropdown = false;
                                     data.erroredFrDropdown = false;
                                     data.successFrDropdown = true;
                                     resolve(data);
                                 }

                             } catch (e) {
                                 data.error = e;
                                 reject(data.error);
                             }
                         },
                         function(response) {
                           data.error = response.data;
                           reject(data.error);
                         }
                     );
                 } catch (e) {
                     data.error = e;
                     reject(data.error);
                 }
            });
        };

    // function to make api call to fetch users from searchtext
        var fetchUsersData = function(url) {
            $rootScope.showLoadingScreen = true;
            this.canceller = $q.defer();
            var request = {
                method: "GET",
                url: url,
                timeout: this.canceller.promise
            };
            return $http(request).then(function(response){
                $rootScope.showLoadingScreen = false;
                var responseType = response.headers('x-response-type');
                if (responseType === 'ERROR') {
                    var errorData = {
                        service: name,
                        message: response.headers('x-response-message')
                    };
                    $rootScope.$broadcast('genericServiceError', errorData);
                    return $q.reject(response);
                }
               return response;
            },
            function (response) {
                var responseMsg = response.headers('x-response-message');
                var errorData = {
                    service: name,
                    message: responseMsg
                };
                $rootScope.$broadcast('genericServiceError', errorData);
                return $q.reject(response);
            });
        }

        this.massageDataFrPermissionsDropdown = function(searchFieldName, searchText, dataFrmApi) {
            var data = [];
            if(dataFrmApi !== undefined) {
                if (searchFieldName === 'userName') {
                    var users = dataFrmApi;
                    users.forEach(function(item) {
                        var userId = item["userId"].toLowerCase();
                        if(userId.indexOf(searchText.toLowerCase()) > -1) {
                            if (item["userEmail"]) {
                                data.push(item["userId"] + ' - ' + item["userEmail"]);
                            } else {
                                data.push(item["userId"]);
                            }                      
                        }
                    });
                } else if (searchFieldName === 'groupName') {
                    var group = dataFrmApi;
                    group.forEach(function(item) {
                        var groupId = item["groupName"].toLowerCase();
                        if(groupId.indexOf(searchText.toLowerCase()) > -1) {
                            if (item["email"]) {
                                data.push(item["groupName"] + ' - ' + item["email"]);
                            } else {
                                data.push(item["groupName"]);
                            }                      
                        }
                    });
                }
                
                return data;
            }
        };

        this.clearAllCommas = function(strngtodelete, parentString) {
            var l = parentString.indexOf(strngtodelete);
            parentString = parentString.replace(strngtodelete, "");
            if (parentString[l] === "," || parentString[l] === " ,") {
                parentString = parentString.replace(parentString[l], "");
            }
            return parentString;
        }
    });
