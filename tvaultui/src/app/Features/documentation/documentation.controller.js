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
(function(app){
    app.controller('DocumentationCtrl', function($scope, $rootScope, SessionStore, ListOfApi){

        $scope.init = function() {

        }
        $scope.expandBlock = false;
        $scope.currentBlock = 0;
        $scope.filteredApiList = [];
        $scope.expand = function(index) {
            if(index !== $scope.currentBlock) {
                $scope.currentBlock = index;
                $scope.expandBlock = true;
            }
            else {
                $scope.expandBlock = !$scope.expandBlock;  
            }                      
        }
        $scope.apiData = ListOfApi.api_details;
        $scope.init();        
        $scope.documentationNavTags = [{
            displayName: 'SAFES',
            navigationName: 'safes',
            addComma: false,
            show: true
        }, {
            displayName: 'ADMIN',
            navigationName: 'admin',
            addComma: false,
            show: SessionStore.getItem("isAdmin") == 'true'
        }, {
            displayName: 'HEALTH',
            navigationName: 'health',
            addComma: false,
            show: false                    // Hidden temporarily
        }, {
            displayName: 'ALERTS',
            navigationName: 'alerts',
            addComma: false,
            show: false                    // Hidden temporarily
        }, {
            displayName: 'DOCUMENTATION',
            navigationName: 'documentation',
            addComma: false,
            show: true                    
        }];        

    });
})(angular.module('vault.features.DocumentationCtrl', []));
