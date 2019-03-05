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
    app.directive( 'resize', function( $window, DeviceDetector, $log, $rootScope ) {
        return function( scope, element ) {
            var windowInnerWidth;
            var maxSmallWidth = 640;
            var maxMediumWidth = 1024;
            var deviceType = '';

            var w = angular.element( $window );


            var checkDeviceSize = function( newValue ) {
                windowInnerWidth = newValue.w;
                if ( windowInnerWidth > maxMediumWidth ) {
                    deviceType = 'large';
                } else if ( windowInnerWidth >= maxSmallWidth && windowInnerWidth <= maxMediumWidth ) {
                    deviceType = 'medium';
                } else if ( windowInnerWidth < maxSmallWidth ) {
                    deviceType = 'small';
                } else {
                    $log.log( 'Unrecognised device type' );
                }
                $rootScope.modalParent.isMobilePhoneDevice = deviceType === 'small';
                $rootScope.modalParent.isMediumScrennDevice = deviceType === 'medium';
                $rootScope.modalParent.isLargeDevice = deviceType === 'large';
                $rootScope.modalParent.notLargeDevice = ( ( deviceType === 'small' ) || ( deviceType === 'medium' ) );
                return deviceType;
            };

            scope.getWindowDimensions = function() {
                return {
                    'h': $window.innerHeight,
                    'w': $window.innerWidth
                };
            };

            scope.setElementWidth = function() {
                return {
                    'width': element[ 0 ].offsetWidth + 'px'
                };
            };

            scope.setElementHeight = function() {
                return {
                    'height': element[ 0 ].offsetHeight + 'px'
                };
            };

            scope.$watch( scope.getWindowDimensions, function( newValue ) {
                scope.windowHeight = newValue.h;
                scope.windowWidth = newValue.w;
                scope.targetElementsWidth = element[ 0 ].offsetWidth + 'px';
                scope.targetElementsHeight = element[ 0 ].offsetHeight + 'px';
                scope.style = function() {
                    return {
                        'height': newValue.h + 'px',
                        'width': newValue.w + 'px'
                    };
                };
                checkDeviceSize( newValue );
                $rootScope.$broadcast( 'deviceResized' );

            }, true );

            w.bind( 'resize', function() {
                scope.$apply();
            } );
        };
    } );
})(angular.module('vault.directives.resize',[]));
