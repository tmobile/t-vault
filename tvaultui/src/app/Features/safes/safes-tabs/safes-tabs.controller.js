(function () {
    'use strict';
    angular.module('vault.features.safes')
        .controller('safesTabsController', safesTabsController);

    function safesTabsController(SessionStore, safesService, $state, $scope, $rootScope) {
        /*
        NOTE THIS CONTROLLER DOES NOT BIND TO vm
        workaround: use scope instead;
         */
        $scope.search = '';
        $scope.loadingSafeTabs = false;
        $scope.safesNavTags = [];
        init();
        $scope.onSearch = onSearch;

        function onSearch(searchWords) {
            console.log('search');
            vm.search = searchWords;
        }

        function init() {
            if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            $scope.safesNavTags = safesService.getSafesNavTags();

            $rootScope.$on('$stateChangeStart', function (event) {
                $scope.loadingSafeTabs = true;
            });

            $rootScope.$on('$stateChangeSuccess', function (event) {
                $scope.loadingSafeTabs = false;
            });
        }

    }
})();