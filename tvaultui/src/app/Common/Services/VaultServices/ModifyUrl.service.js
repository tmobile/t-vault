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
/* This service will modify the url's based on dynamic query parameters that needs to be passed */
(function(app){
    app.service('ModifyUrl', function(RestEndpoints, ArrayFilter){

        return {
            addUrlParameteres: function(endPointName, additionalParameters){

                var newUrl;
                var baseURL = RestEndpoints.baseURL;
                var serviceEndpointsList = RestEndpoints.endpoints;
                var endPointObject = ArrayFilter.searchAnObjectFromArray('name',endPointName,serviceEndpointsList);

                if(endPointObject.url){
                    newUrl = baseURL+endPointObject.url+additionalParameters;
                }

                return newUrl;
            }
        }
    });
})(angular.module('vault.services.ModifyUrl',[
    'vault.constants.RestEndpoints',
    'vault.services.ArrayFilter'
]));

