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
    .service('vaultUtilityService', function(fetchData, UtilityService, SessionStore) {
        this.getDropdownDataForPermissions = function(searchFieldName, searchFieldText) {
            return new Promise(function(resolve, reject) {
                var data = {};
                var ADUsersDataUrl = UtilityService.getAppConstant('AD_USERS_DATA_URL');
                 try {
                     data.loadingDataFrDropdown = true;
                     fetchData.getActionData(null, ADUsersDataUrl, null).then(
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
            })
        };

        this.massageDataFrPermissionsDropdown = function(searchFieldName, searchText, dataFrmApi, dropDownValArray) {
            var data = [];
            var searchFieldName = searchFieldName.toLowerCase();
            if(dataFrmApi !== undefined) {
                var users = dataFrmApi;
                users.forEach(function(item) {
                    // console.log(item);
                    var userId = item["userId"].toLowerCase();
                    if(userId.indexOf(searchText.toLowerCase()) > -1) {
                        // var obj = item;
                        // obj["text"] = item["userId"];
                        data.push(item["userId"]);
                    }
                });
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
