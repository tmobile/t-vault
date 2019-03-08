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
angular
    .module('vault' )
    .filter( 'numFilter', function( $filter ) {
        return function( number ) {
            var abs;
            if ( number !== void 0 ) {
                abs = Math.abs( number );
                if ( abs >= Math.pow( 10, 12 ) ) {
                    number = "<span class='target-text'>" + ( number / Math.pow( 10, 12 ) ).toFixed( 1 ) + "</span><span class='text-suffix'>T</span>";
                } else if ( abs < Math.pow( 10, 12 ) && abs >= Math.pow( 10, 9 ) ) {
                    number = "<span class='target-text'>" + ( number / Math.pow( 10, 9 ) ).toFixed( 1 ) + "</span><span class='text-suffix'>B</span>";
                } else if ( abs < Math.pow( 10, 9 ) && abs >= Math.pow( 10, 6 ) ) {
                    number = ( number / Math.pow( 10, 6 ) ).toFixed( 1 ) + "M";
                    //number = "<span class='target-text'>" + ( number / Math.pow( 10, 6 ) ).toFixed( 1 ) + "</span><span class='text-suffix'>M</span>";
                } else if ( abs < Math.pow( 10, 6 ) && abs >= Math.pow( 10, 3 ) ) {
                    number = ( number / Math.pow( 10, 3 ) ).toFixed( 1 ) + "K";
                    //number = "<span class='target-text'>" + ( number / Math.pow( 10, 3 ) ).toFixed( 1 ) + "</span><span class='text-suffix'>K</span>";
                }
                return $filter( 'trustAsHtml' )( number );
            }
        };
    } );
