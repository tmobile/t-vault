(function () {
    'use strict';
    angular.module('vault.features.safes')
        .controller('safesFoldersController', safesFoldersController);

    function safesFoldersController($state, folderContent, safesService, SAFES_CONSTANTS, $rootScope, Modal, $scope) {
        var vm = this;
        vm.safeCategories = safesService.getSafeTabs();
        vm.search = '';
        vm.folderPathArray = [];
        vm.currentFolder = null;
        vm.folderContent = folderContent;
        vm.userViewingFolder = false;
        vm.root = null;
        vm.tabIndex = 0;
        init();
        vm.clickSafeTab = clickSafeTab;
        vm.goToFolder = goToFolder;
        vm.goToSafeTiles = goToSafeTiles;
        vm.createFolder = createFolder;
        vm.createSecret = createSecret;

        function createSecret() {
            return Modal.createModalWithController('text-input.modal.html', {
                title: 'Create Secret',
                inputLabel: 'Key',
                placeholder: 'Enter secret key',
                passwordLabel: 'Secret',
                passwordPlaceholder: 'Enter secret value',
                submitLabel: 'CREATE',
                cancelLabel: 'CANCEL'
            }).result.then(function (modalData) {
                var newSecret = {
                    id: modalData.inputValue,
                    key: modalData.inputValue,
                    value: modalData.passwordValue,
                    type: 'secret',
                    parentId: folderContent.id
                }
                return safesService.createSecret(folderContent, newSecret)
                    .then(function (data) {
                        folderContent.children = [newSecret].concat(folderContent.children);
                    });
            })
        }


        function createFolder() {
            return Modal.createModalWithController('text-input.modal.html', {
                title: 'Create Folder',
                inputLabel: 'Folder Name',
                placeholder: 'Enter folder name',
                submitLabel: 'CREATE',
                cancelLabel: 'CANCEL'
            }).result.then(function (modalData) {
                var path = vm.currentFolder.fullPath + '/' + modalData.inputValue;
                return safesService.createFolder(path)
                    .then(function (data) {
                        $state.go('safes-folders', {
                            path: path
                        })
                    })
            })

        }

        function clickSafeTab(tab) {
            $state.go('safes', {type: tab.id});
        }

        function goToSafeTiles() {
            $state.go('safes', {path: vm.root.fullPath});
        }

        function goToFolder(path) {
            $state.go('safes-folders', {path: path});
        }

        function init() {
            var pathArray = folderContent.id.split('/');
            while (pathArray.length > 0) {
                vm.folderPathArray.push({
                    fullPath: pathArray.join('/'),
                    folderName: pathArray[pathArray.length - 1],
                    pathLength: pathArray.length
                });
                pathArray.pop();
            }
            vm.root = vm.folderPathArray.pop();
            vm.tabIndex = SAFES_CONSTANTS.SAFE_TYPES.findIndex(function (type) {
                return type.key === vm.root.fullPath;
            });
            vm.folderPathArray = vm.folderPathArray.reverse();
            vm.currentFolder = vm.folderPathArray[vm.folderPathArray.length - 1];
            vm.userViewingFolder = vm.currentFolder.type === 'folder';


            $rootScope.$on('search', function (event, params) {
                vm.search = params;
            });
        }
    }
})();