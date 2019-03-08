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
    app.directive( 'dropDown', function($http, $state) {
        return {
            restrict: 'EA',
            templateUrl: 'app/Common/Directives/dropDown/dropDown.html',
            scope: {
	            dropDownOptions: "=",
	            row : "=",
				error: '=?',
				dropdownDisable : "=",
	            listDetails: "=",
				value: "=?"
	        },
	       link: function (scope, elem, attr) {
            	scope.status = {};
            	scope.status.isopen = false;
            	if (angular.isDefined(scope.dropDownOptions.options)) {
					if (scope.value) {
						scope.dropDownOptions.selectedGroupOption = {
							option: 'radio',
							type: scope.value.toUpperCase(),
							value: scope.value
						};
					}                    					
            		scope.dropDownOptions.tableOptions = [];
            		scope.dropDownOptions.options.forEach(function(item) {
            			var obj = {
            				option: 'radio',
            				type: item.text.toUpperCase(),
							value: item.text
						};
            			scope.dropDownOptions.tableOptions.push(obj);
					})
				}
               scope.toggleDropdown = function($event) {
                   $event.preventDefault();
                   $event.stopPropagation();
                   scope.status.isopen = !scope.status.isopen;
               };
		        scope.filterUpdate = function(option) {
					scope.error = false;
					if (option.option === 'radio') {
						scope.value = option.value;
                        scope.dropDownOptions.selectedGroupOption = {
                            option: 'radio',
                            type: option.value.toUpperCase(),
                            value: option.value
                        };
                        scope.status.isopen = !scope.status.isopen;
					}
		        	else if(option.srefValue != undefined && option.srefValue.obj != undefined && option.srefValue.url != undefined ){
						var obj = (option.srefValue.obj).toString();
						var myobj = scope.listDetails;
						var fullObj = {};
						fullObj[obj] = myobj;
						$state.go(option.srefValue.url, fullObj );
				    }
				    else if(option.srefValue === 'href'){

				    }
				    else{
						if(option.type.indexOf("All") != -1) {
							// scope.dropDownOptions.selectedGroupOption = { "type" : ""};
							scope.dropDownOptions.selectedGroupOption = option;
						}
						else {
							scope.dropDownOptions.selectedGroupOption = option;
						}
				    }

			    }



		    }
        }
    } );
})(angular.module('vault.directives.dropDown',[]));
