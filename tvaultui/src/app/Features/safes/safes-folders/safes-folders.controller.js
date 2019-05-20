(function () {
    'use strict';
    angular.module('vault.features.safes')
      .controller('safesFoldersController', safesFoldersController);

    function safesFoldersController(folderContent, writeAccess, safesService, SAFES_CONSTANTS, $state, $rootScope, Modal, Notifications, SessionStore) {
      var vm = this;
      vm.safeCategories = safesService.getSafeTabs();
      vm.search = '';
      vm.folderPathArray = [];
      vm.currentFolder = null;
      vm.folderContent = folderContent;
      vm.writeAccess = writeAccess;
      vm.userViewingFolder = false;
      vm.root = null;
      vm.tabIndex = 0;
      vm.loadingFlag = false;
      init();
      vm.clickSafeTab = clickSafeTab;
      vm.goToFolder = goToFolder;
      vm.goToSafeTiles = goToSafeTiles;
      vm.createFolder = createFolder;
      vm.createSecret = createSecret;
      vm.loading = loading;

      function loading(value) {
        vm.loadingFlag = value;
      }

      function createSecret(key, value) {
        var modalSettings = {
          title: 'Create Secret',
          inputValue: key || '',
          inputLabel: 'Key',
          placeholder: 'Secret key',
          passwordValue: value || '',
          passwordLabel: 'Secret',
          passwordPlaceholder: 'Enter secret value',
          submitLabel: 'CREATE',
          cancelLabel: 'CANCEL'
        };
        return Modal.createModalWithController('text-input.modal.html', modalSettings)
          .then(function (modalData) {
            var newSecret = {
              id: modalData.inputValue,
              key: modalData.inputValue,
              value: modalData.passwordValue,
              type: 'secret',
              parentId: folderContent.id
            };
            return tryToSaveSecret(newSecret);
          })
      }

      function tryToSaveSecret(newSecret) {
        return safesService.itemIsValidToSave(newSecret, -1, folderContent)
          .then(function (data) {
            vm.loading(true);
            return safesService.saveFolder(folderContent, newSecret)
              .then(function () {
                vm.loading(false);
                vm.folderContent.children = [newSecret].concat(folderContent.children);
                Notifications.toast('Added successfully');
              }).catch(catchError);
          })
          .catch(function (error) {
            return createSecret(newSecret.key, newSecret.value);
          })


      }

      function createFolder() {
        var folder;
        return Modal.createModalWithController('folder-text-input.modal.html', {
          title: 'Create Folder',
          inputLabel: 'Folder Name',
          placeholder: 'Folder name',
          submitLabel: 'CREATE'
        }).then(function (modalData) {
          folder = {
            parentId: vm.currentFolder.fullPath,
            id: vm.currentFolder.fullPath + '/' + modalData.inputValue.toLowerCase(),
            name: modalData.inputValue.toLowerCase(),
            value: null,
            type: 'folder',
            children: []
          };
          return safesService.itemIsValidToSave({
            key: folder.name,
            value: folder.name
          }, -1, folderContent)
            .then(function () {
              vm.loading(true);
              return safesService.createFolder(folder.id)
            .then(function (data) {
              vm.loading(false);
              vm.folderContent.children.push(folder);
            }).catch(catchError);
          })
          .catch(function (error) {
            return createFolder();
          })
        });
      }
     
      function clickSafeTab(tab) {
        $state.go('safes', {type: tab.id});
      }

      function goToSafeTiles() {
        $state.go('safes', {type: vm.root.fullPath});
      }

      function goToFolder(path) {
        $state.go('safes-folders', {path: path});
      }

      function init() {
        if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
            $state.go('/');
            return;
        }
        if (!folderContent.id) {
          Modal.createModalWithController('stop.modal.html', {
            title: 'Error',
            message: 'Sorry we were unable to retrieve those documents.'
          });
          return $state.go('safes');
        }

        var pathArray = folderContent.id.split('/');
        while (pathArray.length > 0) {
          vm.folderPathArray.push({
            fullPath: pathArray.join('/'),
            folderName: pathArray[pathArray.length - 1],
            pathLength: pathArray.length
          });
          pathArray.pop();
        }
        vm.root = vm.folderPathArray.pop();
        vm.tabIndex = SAFES_CONSTANTS.SAFE_TYPES.findIndex(function (type) {
          return type.key === vm.root.fullPath;
        });
        vm.folderPathArray = vm.folderPathArray.reverse();
        vm.currentFolder = vm.folderPathArray[vm.folderPathArray.length - 1];
        vm.userViewingFolder = vm.folderContent.type === 'folder';


        $rootScope.$on('search', function (event, params) {
          vm.search = params;
        });
      }

      function catchError(error) {
        vm.loading(false);
        console.log(error);
        if (error) {
          Modal.createModalWithController('stop.modal.html', {
            title: 'Error',
            message: 'Please try again. If this issue persists please contact an administrator.'
          });
        }
      }
    }
  }

)();