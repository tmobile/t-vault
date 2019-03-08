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
    app.filter('customFilter', ["$filter", function($filter) {
        return function(input, filter) {
            if (input == Infinity) return null;

            var digits = 2;

            if (filter != undefined && filter.indexOf(':') > -1) {
                digits = parseInt(filter.split(':')[1].trim());
                filter = filter.split(':')[0].trim();
            }

            switch (filter) {
                case 'text':
                    return input;
                case 'number':
                    return $filter(filter)(input, digits);
                case 'percentage':
                    return $filter('number')(input, digits) + '%';
                case 'date':
                    return $filter('date')(input, 'MM/dd/yyyy');
                default:
                    return input;
            }
        }
    }]);
})(angular.module('vault.filters.CustomFilter', []));
