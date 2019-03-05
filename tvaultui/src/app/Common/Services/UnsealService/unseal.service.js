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
(function(app){
    app.service( 'Unseal', function( ServiceEndpoint, $q, DataCache, fetchData, $rootScope, ModifyUrl, ErrorMessage ) {


        this.unseal = function(payload) {
            return ServiceEndpoint.unseal.makeRequest(payload).then(
                function(response) {
                    return response;
                }
            );
        };

        this.unsealProgress = function(payload, ip) {
            var url = ModifyUrl.addUrlParameteres('unsealProgress', ip);
            return ServiceEndpoint.unsealProgress.makeRequest(payload, url).then(
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
})(angular.module('vault.services.Unseal',[]));
