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
            .state('safes', {
            url: '/safes',
            parent: 'safes-tabs',
            params: {
                type: 'shared'
            },
            resolve: {
                safes: function (SessionStore, $q) {
                    return JSON.parse(SessionStore.getItem('accessSafes'));
                }
            },
            views: {
                'content@safes-tabs': {
                    templateUrl: 'app/Features/safes/safes-tiles/safes-tiles.html',
                    controller: 'safesTilesController as vm'
                }
            }
        })
            .state('safes-folders', {
                url: '/safes/folders/:path',
                parent: 'safes-tabs',
                resolve: {
                    folderContent: function (safesService, SafesManagement, $stateParams) {
                        return safesService.getFolderContent($stateParams.path)
                    },
                    writeAccess: function (folderContent, SessionStore) {
                        var safeType = folderContent.id.split('/')[0];
                        var safeName = folderContent.id.split('/')[1];
                        var admin = JSON.parse(SessionStore.getItem('isAdmin'));
                        var safesOfType = JSON.parse(SessionStore.getItem('accessSafes'))[safeType];
                        var writeAccess = safesOfType.find(function (safeObj) {
                            return safeObj[safeName];
                        })[safeName];

                        return admin || (writeAccess === 'write');
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