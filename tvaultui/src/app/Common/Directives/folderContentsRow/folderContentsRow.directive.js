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
(function() {
    angular.module('vault.directives.folderContentsRow',[])
        .controller('folderContentsRowController', folderContentsTableController)
        .directive( 'folderContentsRow', function(CopyToClipboard, Modal, $rootScope, SafesManagement, $state, ArrayFilter, Notifications, UtilityService) {
            return {
                restrict: 'E',
                templateUrl: 'app/Common/Directives/folderContentsRow/folderContentsRow.html',
                scope: {
                    item: '=',
                    parent: '='
                },
                link: function( scope, element, attrs ) {
                    if(scope.vm.item.type === 'folder') {
                       scope.vm.itemName = scope.vm.item.id.split('/').pop();
                    } else if (scope.vm.item.type === 'secret') {
                        scope.vm.editing = !scope.vm.item.key;
                        scope.vm.itemName = scope.vm.item.key;
                    }
                },
                controller: 'folderContentsRowController as vm',
                bindToController: true
            }
        } );

    function folderContentsTableController($scope, SafesManagement, Modal, UtilityService, Notifications, $rootScope, toastr) {
        var vm = this;
        $scope.close = function () {
            Modal.close();
        };
        vm.showItemPassword = false;

        vm.edit = edit;
        vm.save = save;
        vm.copyToClipboard = copyToClipboard;
        vm.deleteItem = deleteItem;
        vm.onItemNameChange = onItemNameChange;
        vm.onRowClick = onRowClick;

        function edit($event) {
            vm.editing = true;
        }

        function save ($event, isDelete) {
            Notifications.clearToastr();
            var fail = false;
            var keyValuePairs = {};
            vm.parent.children.forEach(function (item) {
                if(item.type === 'secret') {

                    if(keyValuePairs[item.key]) {
                        $scope.errorMessage = 'Duplicate keys are not allowed.';
                        fail = true;
                        return Modal.createModal('md', 'error.html', null, $scope);
                    } else if (!item.key || !item.value) {
                        $scope.errorMessage = 'Fields may not be empty.';
                        fail = true;
                        return Modal.createModal('md', 'error.html', null, $scope);
                    } else {
                        keyValuePairs[item.key] = item.value;
                    }
                }
            });
            keyValuePairs['default'] = keyValuePairs['default'] || 'default';
            if(isDelete && vm.item.isNew) {
                return removeFromParent();
            }else if (isDelete && !vm.item.isNew) {
                delete keyValuePairs[vm.item.key];
            } else if(fail) {
                return;
            }
            $event.stopPropagation();

            $rootScope.isLoadingData = true;
            vm.editing = false;
            return SafesManagement.writeSecretV2(vm.parent.id, keyValuePairs)
                .then(function(response) {
                    if(isDelete) {
                        removeFromParent();
                        Notifications.toast('delete successful');
                    } else {
                        vm.item.isNew = false;
                        Notifications.toast('save successful');
                    }
                    $rootScope.isLoadingData = false;

                })
                .catch(function (res) {
                    $rootScope.isLoadingData = false;
                    $scope.errorMessage = 'Please try again, if the issue persists contact Vault Administrator';
                    Modal.createModal('md', 'error.html', null, $scope);
                })
        }


        function copyToClipboard() {
            var notification = UtilityService.getAParticularSuccessMessage('COPY_TO_CLIPBOARD');
            Notifications.toast(notification);
            CopyToClipboard.copy(vm.item.value);
        }

        function deleteItem($event) {
            $rootScope.isLoadingData = true;
            $event.stopPropagation();
            if(vm.item.type === 'folder') {
                return SafesManagement.deleteFolder({
                    path: vm.item.id
                }, 'path=' + vm.item.id)
                    .then(function(response) {
                        var index = vm.parent.children.findIndex(function (item) {
                            return item.id === vm.item.id;
                        });
                        vm.parent.children.splice(index, 1);
                        Notifications.toast(vm.item.id.split('/').pop() + ' deleted successfully');
                        $rootScope.isLoadingData = false;
                    })
                    .catch(function (error) {
                        $rootScope.isLoadingData = false;
                        var display = error && error.data && error.data.errors && error.data.errors[0] || 'Please try again, if the issue persists contact Vault Administrator';
                        Notifications.toastError(display);

                    });
            }
        }

        function onItemNameChange() {
            var path = vm.item.id.split('/');
            path.pop();
            path = path.join('/');

            if(vm.item.type === 'folder') {
                vm.item.value = path + '/' + vm.itemName;
                vm.item.id = path + '/' + vm.itemName;
            } else if (vm.item.type === 'secret') {
                vm.item.id = vm.itemName;
                vm.item.key = vm.itemName;
            }
        }

        function onRowClick ($event) {
            if(vm.editing) {
                $event.stopPropagation();
            }
        }

        function removeFromParent() {
            var index = vm.parent.children.findIndex(function (item) {
                return item.id === vm.item.id;
            });
            vm.parent.children.splice(index, 1);
        }

    }
})();