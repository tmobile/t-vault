(function () {
    'use strict';
    angular.module('vault.features.safes')
        .factory('safesService', safesService);

    function safesService(SAFES_CONSTANTS, $http, RestEndpoints, SessionStore) {
        var service = {
            getSafeTabs: getSafeTabs,
            parseSafes: parseSafes,
            createFolder: createFolder,
            saveFolder: saveFolder,
            deleteFolder: deleteFolder,
            getFolders: getFolders
        };

        function getFolders(path) {

            var url = RestEndpoints.baseURL + '/readAll?path=' + path;
            return $http({
                method: 'GET',
                url: url,
                headers: getHeaders()
            })
                .then(function (response) {
                    return response.data;
                });
        }


        function createFolder(path) {
            var url = RestEndpoints.baseURL + '/v2/sdb/write';
            return $http({
                method: 'POST',
                url: url,
                params: {
                    path: path
                },
                headers: getHeaders()
            })
                .then(function (response) {
                    return response.data;
                });
        }

        function saveFolder() {

        }

        function deleteFolder(path) {

        }


        function getSafeTabs() {
            return SAFES_CONSTANTS.SAFE_TYPES
                .map(function (safeType) {
                    return {
                        name: safeType.tabLabel,
                        id: safeType.key
                    }
                })
        }


        function parseSafes(safeListObject) {
            return safeListObject
                .map(function (safeObject) {
                    var entry = Object.entries(safeObject);
                    return {
                        safe: entry[0][0],
                        name: entry[0][0],
                        auth: entry[0][1]
                    }
                })
        }

        function getHeaders() {
            return {
                'Content-Type': 'application/json',
                'vault-token': SessionStore.getItem('myVaultKey')
            }
        }

        return service;
    }
})();