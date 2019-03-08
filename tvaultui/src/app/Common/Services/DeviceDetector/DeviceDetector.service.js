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
    app.service( 'DeviceDetector', function( $window, $log ) {
        var windowInnerWidth;
        var maxSmallWidth = 640;
        var maxMediumWidth = 1024;
        var deviceType = '';
        var userAgent = $window.navigator.userAgent;
        var iosDeviceType;
        return {
            checkDevice: function() {
                windowInnerWidth = $window.innerWidth;
                if ( windowInnerWidth > maxMediumWidth ) {
                    deviceType = 'large';
                } else if ( windowInnerWidth >= maxSmallWidth && windowInnerWidth <= maxMediumWidth ) {
                    deviceType = 'medium';
                } else if ( windowInnerWidth < maxSmallWidth ) {
                    deviceType = 'small';
                } else {
                    $log.log( 'Unrecognised device type' );
                }
                return deviceType;
            },
            isIphone: function() {
                if ( userAgent.indexOf( 'iPhone' ) >= 0 ) {
                    iosDeviceType = true;
                }
                return iosDeviceType;
            },
            isIpad: function() {
                if ( userAgent.indexOf( 'iPad' ) >= 0 ) {
                    iosDeviceType = true;
                }
                return iosDeviceType;
            },
            getDeviceheight: function() {
                return $window.innerHeight;
            },
            getDeviceWidth: function() {
                return $window.innerWidth;
            }

        };
    } );
})(angular.module('vault.services.DeviceDetector',[]));
