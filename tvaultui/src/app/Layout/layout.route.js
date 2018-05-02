(function () {
    'use strict';

    angular.module('vault.layout')
        .config(function ($stateProvider) {

                $stateProvider
                    .state('base', {
                        url: '/home',
                        templateUrl: 'app/Layout/base/base.html',
                        controller: 'baseController as vm'
                    })
            });
})();