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
    app.service( 'DataCache', function( SessionStore ) {
        // AngularJS will instantiate a singleton by calling "new" on this function
        var globalData = {};
        return {
            saveGlobalData: function( key, value ) {
                var copy = angular.copy( value );
                globalData[ key ] = copy;
                SessionStore.setItem( 'data', angular.toJson( globalData ) );
                return copy;
            },
            returnData: function( key ) {
                var copy = angular.copy( globalData[ key ] );
                return copy;
            },
            clearAll: function() {
                globalData = {};
                SessionStore.clear();
            },
            clearCache: function( cacheNames ) {
                if ( Array.isArray( cacheNames ) ) {
                    for ( var i = cacheNames.length - 1; i >= 0; i-- ) {
                        this.saveGlobalData( cacheNames[ i ], null );
                    }
                } else {
                    this.saveGlobalData( cacheNames, null );
                }
            },
            loadFromWebstorage: function() {
                globalData = angular.fromJson( SessionStore.getItem( 'data' ) ) || {};
            }
        };
    } );
})(angular.module('vault.services.DataCache',[]));