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

(function( app ) {
    app.service( 'Notifications', function( toastr, toastrConfig ) {
        
        toastrConfig.containerId = 'toast-container';
        toastrConfig.iconClass = 'toast';
        toastrConfig.tapToDismiss = false;
        toastrConfig.timeOut = 1000;
        toastrConfig.positionClass = 'custom-toast';
        toastrConfig.newestOnTop = true;
        toastrConfig.preventDuplicates = false;
        toastrConfig.preventOpenDuplicates = true;
        toastrConfig.progressBar = false;
        toastrConfig.maxOpened = 1;
        toastrConfig.templates.toast = 'directives/toast/toast.html';
        
        return {
            toast : function( message ) {
                toastr.info( message, '');
            },
            toastError : function( message ) {
                toastr.info( message, '', {
                    iconClass : 'toastError',
                    tapToDismiss : false,
                    timeOut : 3000,
                    extendedTimeOut : 1000,
                    positionClass : 'custom-toast'
                } );
            },
            clearToastr : function () {
                toastr.clear();
                toastr.clearToastr;
            }
        };
    } );
})( angular.module( 'vault.services.Notifications', [
    'toastr'
] ) );