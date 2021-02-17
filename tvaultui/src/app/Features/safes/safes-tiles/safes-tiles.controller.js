(function () {
    'use strict';
    angular.module('vault.features.safes')
        .controller('safesTilesController', safesTilesController);

    function safesTilesController(safesService, SAFES_CONSTANTS, $stateParams, $state, $rootScope, SessionStore, $http, RestEndpoints) {
        var vm = this;
        vm.safeCategories = safesService.getSafeTabs();
        vm.searchValue = '';
        vm.tabIndex = 0;
        vm.tiles = [];
        vm.safesLoaded = false;
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

            $rootScope.loadingData = true;
            var url = RestEndpoints.baseURL + '/v2/ss/sdb/safes';
            var newSafes = [];
            $http({
                method: 'GET',
                url: url,
                headers: getHeaders()
            }).then(function (response) {
                vm.safesLoaded = true;
                SessionStore.setItem('accessSafes', JSON.stringify(response.data));
                newSafes = JSON.parse(JSON.stringify(response.data));
                SAFES_CONSTANTS.SAFE_TYPES.forEach(function (safeType) {
                    vm.tiles[safeType.key] = newSafes[safeType.key] &&
                        safesService.parseSafes(newSafes[safeType.key]) || [];
                });
                $rootScope.loadingData = false;
            })
            .catch(function(catchError) {
                $rootScope.loadingData = false;
            });
        }
        function getHeaders() {
            return {
                'Content-Type': 'application/json',
                'vault-token': SessionStore.getItem('myVaultKey')
            }
        }

    }
}) ();