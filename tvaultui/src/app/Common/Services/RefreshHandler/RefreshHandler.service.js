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
(function(app) {
    app.service( 'RefreshHandler', function( DataCache, $window, $rootScope, $state,
        SessionStore ) {
        // AngularJS will instantiate a singleton by calling "new" on this function
        var isRefreshing = false;
        this.loadFromWebstorage = function() {
            DataCache.loadFromWebstorage();
            isRefreshing = true;
        };
        this.isRefreshing = function() {
            return isRefreshing;
        };
        this.refreshDone = function() {
            isRefreshing = false;
        };
        this.bindRefreshWarning = function( message ) {
            if ( $window.Modernizr.sessionstorage ) {
                this.setRedirectState();
                var msg = 'If you refresh then you will be taken back to the first step of and lose your progress';
                if ( $rootScope.isEcomPayment ) {
                    msg = 'If you refresh then you will be taken back to the first step of the process';
                }
                $window.onbeforeunload = function() {
                    if ( message ) {
                        msg = message;
                    }
                    return msg;
                };
            }
        };
        this.unbindRefreshWarning = function() {
            $window.onbeforeunload = null;
            SessionStore.removeItem( 'onRefreshRedirectState' );
        };
    } );
})(angular.module('vault.services.RefreshHandler',[]));