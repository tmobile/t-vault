(function () {
    'use strict';
    angular.module('vault.features.safes')
        .factory('safesService', safesService);

    function safesService(SAFES_CONSTANTS, $http, RestEndpoints, SessionStore, $q, Modal, Authentication) {
        var service = {
            getSafeTabs: getSafeTabs,
            getSafesNavTags: getSafesNavTags,
            parseSafes: parseSafes,
            createFolder: createFolder,
            saveFolder: saveFolder,
            deleteFolder: deleteFolder,
            getFolderContent: getFolderContent,
            itemIsValidToSave: itemIsValidToSave
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

        function deleteFolder(path) {
            var url = RestEndpoints.baseURL + '/v2/sdb/delete?path=' + path;
            return $http({
                method: 'DELETE',
                url: url
            })
                .then(function (response) {
                    return response.data;
                })
                .catch(catchError);
        }

        function saveFolder(folderContent, newSecret) {
            // newSecret = {key: 'string', value: 'string'}
            var url = RestEndpoints.baseURL + '/v2/write?path=' + folderContent.id;
            var content = folderContent.children.slice(0);
            if (newSecret) {
                content.push(newSecret);
            }
            var data = parseFolderContentToSecrets(content);
            return $http({
                method: 'POST',
                url: url,
                data: {
                    path: folderContent.id,
                    data: data
                },
                headers: getHeaders()
            }).catch(catchError);
        }

        function itemIsValidToSave(item, index, parent) {
            //SECRET MISSING INPUT
            if (!item.key || !item.value) {
                return Modal.createModalWithController('stop.modal.html', {
                    title: 'Unable to complete action',
                    message: 'Form is missing one or more data fields.'
                });
            }

            //SECRET DUPLICATE KEY
            var otherWithSameName = parent.children.find(function (childItem, position) {
                if (position === index) return false;
                var comparator = (childItem.type === 'folder') ?
                    childItem.id.split('/').pop() : childItem.key;
                return comparator === item.key;
            });

            if (otherWithSameName) {
                return Modal.createModalWithController('stop.modal.html', {
                    title: 'Key already exists.',
                    message: 'This folder already contains an item with the specified name. You can\'t store two secrets with the same key. Please try a different name for the key.'
                });
            }

            //SECRET CONTAINS NEWLINE
            var regex = /\n|\r/gm;
            if (!!item.value.match(regex)) {
                return Modal.createModalWithController('confirm.modal.html', {
                    title: 'Notice',
                    message: 'Line-break characters are restricted in Safe Keys and will be removed before saving.',
                    submitLabel: 'OK'
                })
                    .then(function () {
                        var newValue = item.value.replace(regex, '');
                        item.value = newValue;
                    });
            }
            return $q.when(true);
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

        function getSafesNavTags() {
            return [{
                displayName: 'SAFES',
                navigationName: 'safes',
                addComma: false,
                show: true
            }, {
                displayName: 'ADMIN',
                navigationName: 'admin',
                addComma: false,
                show: (JSON.parse(SessionStore.getItem("isAdmin")))
            }, {
                displayName: 'DOCUMENTATION',
                navigationName: 'documentation',
                addComma: false,
                show: true,
                redirectTo: function () {
                    var address = RestEndpoints.baseURL + '/swagger-ui.html';
                    var link = document.createElement('a');
                    link.setAttribute('href', address);
                    link.setAttribute('target', '_blank');
                    link.click();
                }
            }];
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
            return $q.reject(error);
        }

        return service;
    }
})();