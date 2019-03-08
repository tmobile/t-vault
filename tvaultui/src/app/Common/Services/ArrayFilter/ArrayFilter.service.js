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
/* This service is to filter Array by doing different operations */
( function( app ) {
    app.service( 'ArrayFilter', function() {
        // AngularJS will instantiate a singleton by calling "new" on this function
        this.searchAnObjectFromArray = function( key, value, array ) {
            var obj = array.filter( function( obj ) {
                return obj[ key ] === value;
            } )[ 0 ];

            return obj;
        }
        this.findIndexInArray = function(key, value, array) {
          var index = array.findIndex(function (obj) {
            return obj[key] === value;
          })
          return index;
        }
    } );
} )( angular.module( 'vault.services.ArrayFilter', [] ) );
