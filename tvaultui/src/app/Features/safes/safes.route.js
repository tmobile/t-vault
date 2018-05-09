 (function () {
    "use strict";
    angular.module('vault.features.safes')
        .config(config);

    function config($stateProvider) {

        $stateProvider
            .state('safes-tabs', {
                url: '/safes-tabs',
                parent: 'base',
                resolve: {
                    auth: function () {

                    }
                },
                views: {
                    'content@base': {
                        templateUrl: 'app/Features/safes/safes-tabs/safes-tabs.html',
                        controller: 'safesTabsController'
                    }
                }
            })
            .state('safes', {
            url: '/safes',
            parent: 'safes-tabs',
            params: {
                type: 'users'
            },
            resolve: {
                safes: function (SessionStore, $q, $state) {
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
                    folderContent: function (safesService, SafesManagement, $state, $stateParams, $q, $timeout) {
                        return safesService.getFolderContent($stateParams.path);
                    },
                    writeAccess: function (folderContent, SessionStore) {
                        var safeType = folderContent.id.split('/')[0];
                        var safeName = folderContent.id.split('/')[1];
                        var safesOfType = JSON.parse(SessionStore.getItem('accessSafes'))[safeType];
                        var writeAccess = safesOfType.find(function (safeObj) {
                            return safeObj[safeName];
                        })[safeName];
                        return (writeAccess === 'write');
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