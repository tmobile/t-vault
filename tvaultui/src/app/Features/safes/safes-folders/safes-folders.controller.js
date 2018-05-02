(function () {
    'use strict';
    angular.module('vault.features.safes')
        .controller('safesFoldersController', safesFoldersController);

    function safesFoldersController($state) {
        var vm = this;
        vm.safeCategories = safesService.getSafeTabs();
        vm.tiles = [];
        vm.searchValue = '';
        SAFES_CONSTANTS.SAFE_TYPES.forEach(function (safeType) {
            vm.tiles[safeType.key] = safes[safeType.key] &&
                safesService.parseSafes(safes[safeType.key]) || [];
        });


        function clickSafeTab(tab) {
            $state.go('safes', {type: tab.id});
        }

        function goToFolders() {
            console.log('go to folder');
        }
    }
}) ();