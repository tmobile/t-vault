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
(function () {
    angular.module('vault.directives.folderContentsTable', [])
        .controller('folderContentsTableController', folderContentsTableController)
        .directive('folderContentsTable', function () {
            return {
                restrict: 'E',
                templateUrl: 'app/Common/Directives/folderContentsTable/folderContentsTable.html',
                scope: {
                    folderContent: '=',
                    folderLastChangedDetails: '=',
                    loading: '=',
                    write: '='
                },
                controller: 'folderContentsTableController as vm',
                bindToController: true
            }
        });

    function folderContentsTableController($scope, $state, $rootScope) {
        var vm = this;
        vm.clickRow = clickRow;
        vm.filter = filter;
        vm.search = '';
        $rootScope.$on('search', function (event, args) {
            vm.search = args;
        });

        function filter(item) {
            if (!vm.search) return item;
            if (item.type === 'folder') {
                return !!~item.id.split('/').pop().indexOf(vm.search);
            } else {
                return !!~item.id.indexOf(vm.search);
            }
        }

        function clickRow(item) {
            if (item.type === 'folder') {
                $state.go('safes-folders', {
                    path: item.id
                });
            }
        }


    }
})();