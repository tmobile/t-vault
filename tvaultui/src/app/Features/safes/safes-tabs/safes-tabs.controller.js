(function () {
    'use strict';
    angular.module('vault.features.safes')
        .controller('safesTabsController', safesTabsController);

    function safesTabsController(SessionStore, safesService, $state) {
        var vm = this;
        vm.search = '';

        vm.safesNavTags = [{
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


        vm.goToSafesTab = goToSafesTab;

        function goToSafesTab(data) {

        }

        function onSearch(searchWords) {
            console.log('search');
            vm.search = searchWords;
        }

    }
}) ();