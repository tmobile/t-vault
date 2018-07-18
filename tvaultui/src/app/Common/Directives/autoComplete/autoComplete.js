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
    app.directive('autoComplete', ['$rootScope', '$timeout', 'UtilityService', function($rootScope, $timeout, UtilityService) {

    // getDropdownData
        return function(scope, iElement, iAttrs) {
            scope.$watch(iAttrs.uiItems, function(newVal, oldVal) {

                if (newVal != undefined && newVal.length > 0) {
                    //make array of source here scope based value
                    iElement
                        .bind("keydown", function(event) {
                            if (event.keyCode === $.ui.keyCode.TAB &&
                                $(this).autocomplete("instance").menu.active) {
                                event.preventDefault();
                            }
                        })
                        .autocomplete({
                            minLength: 0,
                            source: scope[iAttrs.uiItems],
                            /*** commented below code used for adding multiple imputs */
                            // source: function(request, response) {
                            //     var enteredVal = request.term.split(","); // periscopeSearch.userName
                            //     enteredVal.forEach(function(inputitem) {
                            //         var newLetter = inputitem;
                            //         newLetter = newLetter.replace(" ", "");
                            //         if (newLetter != undefined) {
                            //             newVal.forEach(function(item) {
                            //                 if (item === newLetter) {
                            //                     var index = newVal.indexOf(newLetter);
                            //                     if (index > -1) {
                            //                         newVal.splice(index, 1);
                            //                     }
                            //                 }
                            //             });
                            //         }

                            //     });
                            //     if (request.term === "") {
                            //         newVal = [];
                            //     }
                            //     response($.ui.autocomplete.filter(
                            //         newVal, UtilityService.extractLast(request.term)));
                            // },

                            focus: function(event, ui) {
                                // iElement.val(ui.item);
                                // prevent value inserted on focus
                                return false;
                            },
                            select: function(event, ui) {
                                var terms = UtilityService.split(this.value);
                                // remove the current input
                                terms.pop();
                                // add the selected item
                                terms.push(ui.item.value);
                                this.value = terms;
                                /*** commented below code used for adding multiple imputs */
                                // add placeholder to get the comma-and-space at the end                                
                                // terms.push("");
                                // var joinedCommaValue = terms.join(", ");
                                // if ($(event.target).hasClass("permission-search-input")) {
                                //     this.value = joinedCommaValue.substring(0, joinedCommaValue.length - 2);
                                // } else {
                                //     this.value = joinedCommaValue;
                                // }
                                return false;
                            }
                        }); // end of autocomplete
                } 
                // else if (iElement[0].value.length > 2) {
                //     iElement.autocomplete({
                //         source: ["Loading Data.. Please wait.."]
                //     });
                // }

            });

        };
    }]);
})(angular.module('vault.features.Autocomplete', []));
