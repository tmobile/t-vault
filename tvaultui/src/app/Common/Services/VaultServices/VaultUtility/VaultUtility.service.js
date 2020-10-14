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

angular.module('vault.services.VaultUtility', [])
    .service('vaultUtilityService', function(fetchData, UtilityService, SessionStore, ModifyUrl, $q, $http, $rootScope, RestEndpoints, AppConstant) {
       var self = this;
       self.canceller = {};
       self.authType = AppConstant.AUTH_TYPE;
       self.getDropdownDataForPermissions = function(searchFieldName, searchFieldText, forOwner) {          
            return new Promise(function(resolve, reject) {
                var data = {};
                var DataUrl;
                if (forOwner) {
                    DataUrl = RestEndpoints.baseURL + RestEndpoints.usersGetData;
                } else if (searchFieldName === "userName" && self.authType.toLowerCase() === 'ldap') {
                    //DataUrl = RestEndpoints.baseURL + RestEndpoints.usersGetDataUsingCorpID;
                    DataUrl = RestEndpoints.baseURL + RestEndpoints.usersGetDataUsingNTID;
                } else if(searchFieldName === "userName" && self.authType.toLowerCase() === 'ldap1900') {
                    DataUrl = RestEndpoints.baseURL + RestEndpoints.usersGetData;
                } else if (searchFieldName === "groupName") {
                    DataUrl = RestEndpoints.baseURL + RestEndpoints.groupGetDataFromAAD;
                }                                
                DataUrl = DataUrl + searchFieldText;              
                 try {
                     data.loadingDataFrDropdown = true;
                     // Abort pending requests before making new request
                     if (Object.keys(self.canceller).length !== 0) {
                        self.canceller.resolve();
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
                           reject(response);
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
            self.canceller = $q.defer();
            var request = {
                method: "GET",
                url: url,
                timeout: self.canceller.promise
            };
            return $http(request).then(function(response){
                $rootScope.showLoadingScreen = false;
                var responseType = response.headers('x-response-type');
                if (responseType === 'ERROR') {
                    return $q.reject(response);
                }
               return response;
            },
            function (response) {
                return $q.reject(response);
            });
        }

        function massageUserNameData (data, dataFrmApi, searchText) {
            var users = dataFrmApi;
            users.forEach(function(item) {
                var userId = item["userId"].toLowerCase();
                var userEmail;
                if (item["userEmail"]) {
                    userEmail = item["userEmail"].toLowerCase();
                }
                if(userId.includes(searchText.toLowerCase()) || userEmail === searchText.toLowerCase()) {
                        // process to display name with "firstname lastname"
                        if (item["displayName"] && item["displayName"].includes(',')) {
                            userId = item["displayName"].split(',');
                            userId = userId[1] + " " + userId[0];
                        }
                    if (item["userEmail"]) {                               
                        data.push(userId + ' - ' + item["userEmail"]);
                    } else {
                        data.push(userId);
                    }                 
                }
            });
            return data;
        }

        function massageUserNameDataUsingCorpID (data, dataFrmApi, searchText) {
            var users = dataFrmApi;
            users.forEach(function(item) {
                var userName = item["userName"].toLowerCase();
                var displayName = item["displayName"].toLowerCase();
                if(displayName.includes(searchText.toLowerCase())) {
                    if (item["userName"]) {
                        data.push(item["displayName"] + " (" + item["userName"]+ ")");
                    }
                }
                else if(userName.includes(searchText.toLowerCase())) {
                    if (item["userName"]) {
                        data.push(item["displayName"] + " (" + item["userName"]+ ")");
                    }
                }
            });
            return data;
        }


        function massageOwnerEmailData (data, dataFrmApi, searchText) {
            var users = dataFrmApi;
            users.forEach(function(item) {
                var userId = item["userId"].toLowerCase();
                var userEmail;
                if (item["userEmail"]) {
                    userEmail = item["userEmail"].toLowerCase();
                }
                if(userId.includes(searchText.toLowerCase()) || userEmail === searchText.toLowerCase()) {                        
                    if (item["userEmail"]) {                               
                        data.push(item["userEmail"]);
                    }             
                }
            });
            return data;
        }

        function massageGroupNameData(data, dataFrmApi, searchText) {
            var group = dataFrmApi;                   
            group.forEach(function(item) {
                var groupId = item["groupName"].toLowerCase();
                var groupEmail;
                if (item["email"]) {
                    groupEmail = item["email"].toLowerCase();
                }
                if(groupId.includes(searchText.toLowerCase())  || groupEmail === searchText.toLowerCase()) {
                    if (item["email"]) {
                        data.push(item["groupName"] + ' - ' + item["email"]);
                    } else {
                        data.push(item["groupName"]);
                    }                      
                }
            });
            return data;
        }

        self.massageDataFrPermissionsDropdown = function(searchFieldName, searchText, dataFrmApi, forOwner) {
            var data = [];
            if(dataFrmApi !== undefined) {
                if (forOwner) {
                    data = massageOwnerEmailData(data, dataFrmApi, searchText);
                } else if (searchFieldName === 'userName' && self.authType.toLowerCase() === 'ldap') {
                    // massage data for users using corp id
                    data = massageUserNameDataUsingCorpID(data, dataFrmApi, searchText);
                } else if (searchFieldName === 'userName' && self.authType.toLowerCase() === 'ldap1900') {
                    data = massageUserNameData(data, dataFrmApi, searchText);                    
                } else if (searchFieldName === 'groupName') {
                    data = massageGroupNameData(data, dataFrmApi, searchText);                    
                }                
                return data;
            }
        };

        self.clearAllCommas = function(strngtodelete, parentString) {
            var l = parentString.indexOf(strngtodelete);
            parentString = parentString.replace(strngtodelete, "");
            if (parentString[l] === "," || parentString[l] === " ,") {
                parentString = parentString.replace(parentString[l], "");
            }
            return parentString;
        };

        self.getAllUsersDataForPermissions = function(userNames) {
            return new Promise(function(resolve, reject) {
                var data = {};
                var dataUrl = RestEndpoints.baseURL + RestEndpoints.getUsersDataUsingNTIDs;
                dataUrl = dataUrl + userNames;
                 try {
                     // Abort pending requests before making new request
                     if (Object.keys(self.canceller).length !== 0) {
                        self.canceller.resolve();
                     }
                    fetchUsersData(dataUrl)
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
                           reject(response);
                         }
                     );
                 } catch (e) {
                     data.error = e;
                     reject(data.error);
                 }
            });
        };
    });
