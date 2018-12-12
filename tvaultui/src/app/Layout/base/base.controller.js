(function () {
    'use strict';
    angular.module('vault.layout')
        .controller('baseController', baseController);

    function baseController(SessionStore) {
        var vm = this;
        vm.baseLoading = false;
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
        }, /*{
            displayName: 'MANAGE',
            navigationName: 'manage',
            addComma: false,
            show: ((SessionStore.getItem("isManager") === 'true') && (SessionStore.getItem("isAdmin") !== 'true'))
        },*/ {
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
    }
})();