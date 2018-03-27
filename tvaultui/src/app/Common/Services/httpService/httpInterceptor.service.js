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

( function( app ) {
    app.service( 'httpInterceptor', function( SessionStore ) {

        return {
            request: function( config ) {
                config.headers = config.headers || {};
                // Token to be sent with header
                if(config.url.indexOf('ad/users') > -1) {
                    //If the call is to get ADUsers and Groups, then don't add vault-token in the header
                    return config;
                    
                } else {
                    var vaultToken = SessionStore.getItem("myVaultKey");
                    config.headers['vault-token'] = vaultToken;
                    return config;
                }             
            }
        };
    } );
} )( angular.module( 'vault.services.httpInterceptor', [] ) );
