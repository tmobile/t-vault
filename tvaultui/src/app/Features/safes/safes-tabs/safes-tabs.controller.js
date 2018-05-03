(function () {
    'use strict';
    angular.module('vault.features.safes')
        .controller('safesTabsController', safesTabsController);

    function safesTabsController(SessionStore, safesService, $state, $scope) {
        var vm = this;
        vm.search = '';
        $scope.safesNavTags = [{
            displayName: 'SAFES',
            navigationName: 'safes',
            addComma: false,
            show: true
        }, {
            displayName: 'ADMIN',
            navigationName: 'admin',
            addComma: false,
            show: (SessionStore.getItem("isAdmin") === 'true')
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

        vm.isLoadingData = true;
        vm.onSearch = onSearch;

        function onSearch(searchWords) {
            console.log('search');
            vm.search = searchWords;
        }

    }
}) ();