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
    app.directive( 'headerHome', function($state, SessionStore, Authentication, RestEndpoints, AppConstant) {
        return {
            restrict: 'E',
            templateUrl: 'app/Common/Directives/headerHome/headerHome.html',
            link: function( scope ) {
                scope.swaggerLink = RestEndpoints.baseURL + '/swagger-ui.html';
                scope.documentationLink = AppConstant.DOCS_LINK;
            }
        }
    } );
})(angular.module('vault.directives.headerHome',[]));
