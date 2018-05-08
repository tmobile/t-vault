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
    app.directive( 'navBar', function($rootScope, $state, $timeout) {
        return {
            restrict: 'EA',
            templateUrl: 'app/Common/Directives/navBar/navBar.html',
            scope: {
	            activeTab: "@",
                navTags:"="
	        },
	        link: function (scope, elem, attr) {     
		        scope.goTo=function(navTag){
		            var tab = navTag.navigationName;
		            if(navTag.redirectTo) {
		                return navTag.redirectTo();
                    }

                    if(tab !== 'details' && tab !== 'permissions') {
                        try {
                            $state.go(tab);
                        } catch (e) {
                            console.log(e);
                        }
                    }   
                    else{
                        $timeout(function() {
                            if(tab === 'details'){
                                $rootScope.activeDetailsTab = tab;
                                $rootScope.showDetails = true;
                            }  
                            else {
                                var createSafeButton = $(".btn-primary");
                                if($rootScope.showDetails === true) {
                                    createSafeButton.click(); 
                                }                                
                            } 
                        });                                             
                    }                 
                };
		    }
        }
    } );
})(angular.module('vault.directives.navBar',[]))
