(function () {
    'use strict';
    angular.module('vault.features.safes')
        .controller('safesTilesController', safesTilesController);

    function safesTilesController(safesService, safes, SAFES_CONSTANTS, $stateParams, $state, $rootScope, SessionStore) {
        var vm = this;
        vm.safeCategories = safesService.getSafeTabs();
        vm.searchValue = '';
        vm.tabIndex = 0;
        vm.tiles = [];
        init();
        vm.goToFolders = goToFolders;

        function goToFolders(data) {
            var currentSafeType = SAFES_CONSTANTS.SAFE_TYPES[vm.tabIndex].key;
            $state.go('safes-folders', {path: currentSafeType + '/' + data.safe});
        }

        function init() {
            if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            $rootScope.$on('search', function (event, searchValue) {
                vm.searchValue = searchValue;
            });

            vm.tabIndex = SAFES_CONSTANTS.SAFE_TYPES.findIndex(function (type) {
                return type.key === $stateParams.type;
            });

            SAFES_CONSTANTS.SAFE_TYPES.forEach(function (safeType) {
                vm.tiles[safeType.key] = safes[safeType.key] &&
                    safesService.parseSafes(safes[safeType.key]) || [];
            });

        }

    }
}) ();