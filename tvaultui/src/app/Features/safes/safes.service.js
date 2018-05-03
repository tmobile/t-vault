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
            getFolderContent: getFolderContent,
            createSecret: createSecret
        };

        function getFolderContent(path) {
            var url = RestEndpoints.baseURL + '/readAll?path=' + path;
            return $http({
                method: 'GET',
                url: url,
                headers: getHeaders()
            })
                .then(function (response) {
                    var contents = [];
                    response.data.children.map(function (element) {
                        if (element.type === 'secret') {
                            var secrets = JSON.parse(element.value);
                            Object.keys(secrets.data).forEach(function (key) {
                                if (key === 'default' && secrets.data['default'] === 'default') return;
                                contents.push({
                                    type: 'secret',
                                    id: key,
                                    key: key,
                                    value: secrets.data[key],
                                    parentId: element.parentId
                                })
                            })
                        } else if (element.type === 'folder') {
                            contents.push(element);
                        }
                    });
                    response.data.children = contents;
                    return response.data;
                }).catch(catchError);
        }


        function createFolder(path) {
            var url = RestEndpoints.baseURL + '/v2/sdb/createfolder?path=' + path;
            return $http({
                method: 'POST',
                url: url,
                headers: getHeaders()
            })
                .then(function (response) {
                    return response.data;
                })
                .catch(catchError);
        }

        function saveFolder() {

        }

        function deleteFolder(path) {

        }

        function createSecret(folderContent, newSecret) {
            // newSecret = {key: 'string', value: 'string'}
            var url = RestEndpoints.baseURL + '/v2/write?path=' +folderContent.id;
            var content = folderContent.children.slice(0);
            content.push(newSecret);
            var data = parseFolderContentToSecrets(content);
            return $http({
                method: 'POST',
                url: url,
                data: {
                    path: folderContent.id,
                    data: data
                },
                headers: getHeaders()

            })
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

        function parseFolderContentToSecrets(folderContentChildren) {
            var secretsData = {};
            folderContentChildren.forEach(function (item) {
                if (item.type === 'secret') {
                    var key = item.key;
                    var value = item.value;
                    secretsData[key] = value;
                }
            });
            return secretsData;
        }

        function getHeaders() {
            return {
                'Content-Type': 'application/json',
                'vault-token': SessionStore.getItem('myVaultKey')
            }
        }

        function catchError(error) {
            console.log(error);
        }

        return service;
    }
})();