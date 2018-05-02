(function () {
    "use strict";
    angular.module('vault.features.safes')
        .config(config);

    function config($stateProvider) {

        $stateProvider
            .state('safes-tabs', {
                url: '/safes-tabs',
                parent: 'base',
                resolve: {},
                views: {
                    'content@base': {
                        templateUrl: 'app/Features/safes/safes-tabs/safes-tabs.html',
                        controller: 'safesTabsController as vm'
                    }
                }
            })
        //     .state('safes', {
        //     url: '/safes',
        //     parent: 'safes-tabs',
        //     params: {
        //         type: 'shared'
        //     },
        //     resolve: {
        //         safes: function (SessionStore) {
        //             return JSON.parse(SessionStore.getItem('accessSafes'))
        //         }
        //     },
        //     views: {
        //         'content@safes-tabs': {
        //             templateUrl: 'app/Features/safes/safes-tiles/safes-tiles.html',
        //             controller: 'safesTilesController as vm'
        //         }
        //     }
        // })
            .state('safes-folders', {
                url: '/safes/folders/:path',
                parent: 'safes-tabs',
                resolve: {
                    folder: function (safesService, SafesManagement, $stateParams) {
                        // return safesService.getFolders($stateParams.path)
                        return SafesManagement.getFolderContents($stateParams.path);
                    }
                },
                params: {
                  path: ''
                },
                views: {
                    'content@safes-tabs': {
                        templateUrl: 'app/Features/safes/safes-folders/safes-folders.html',
                        controller: 'safesFoldersController as vm'
                    }
                }
            })
    }
})();