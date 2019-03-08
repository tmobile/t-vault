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
  app.directive('restrictSpecialCharButComma', function() {
    return {
      require: 'ngModel',
      restrict: 'A',
      link: function(scope, element, attrs, modelCtrl) {
        modelCtrl.$parsers.push(function(inputValue) {
          if (inputValue == null)
            return ''
          var cleanInputValue = inputValue.replace(/[^_\w,]/gi, ''); // '\w' refers to all word characters a-z A-Z 0-9, '\s' refers to whitespace
                                                                      // '^' refers to NOT, so its like not(a-z A-Z 0-9 ' ') replace with '' 
                                                                      // Remove '\s' if you don't want to ignore the spaces in the input field
                                                                      // ':' and '/' are allowed for arn fields
          if (cleanInputValue != inputValue) {
            modelCtrl.$setViewValue(cleanInputValue);
            modelCtrl.$render();
          }
          return cleanInputValue;
        });
      }
    }
  });
})(angular.module('vault.directives.restrictSpecialCharButComma',[]));
