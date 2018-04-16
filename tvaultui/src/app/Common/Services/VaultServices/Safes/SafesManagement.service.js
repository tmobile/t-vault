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
    app.service( 'SafesManagement', function( ServiceEndpoint, $q, DataCache, fetchData, $rootScope, ModifyUrl, ErrorMessage ) {

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
                  if(response.data.data) {
                      var decoded = {};
                      Object.keys(response.data.data).forEach(function (key, index) {
                          decoded[key] = atob(response.data.data[key]);
                      })
                      response.data.data = decoded;
                  }
                return response;
              }
          );
        };

        this.writeSecret = function(payload) {
            var base64Payload = {};
            Object.keys(payload.data).forEach(function (key, index) {
                base64Payload[key] = btoa(payload.data[key]);
            });
            payload.data = base64Payload;


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
