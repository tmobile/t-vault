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
(function(app) {
    app.service( 'SessionStore', function( $window ) {
        // AngularJS will instantiate a singleton by calling "new" on this function
        this.setItem = function( key, value ) {
            $window.sessionStorage.setItem( key, value );
        };
        this.getItem = function( key ) {
            return $window.sessionStorage.getItem( key );
        };
        this.clear = function() {
            $window.sessionStorage.clear();
        };
        this.removeItem = function( key ) {
            $window.sessionStorage.removeItem( key );
        };
    } );
})(angular.module('vault.services.SessionStore',[]));