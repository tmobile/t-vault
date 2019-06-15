(function () {
    'use strict';
    angular.module('vault.features.safes')
      .controller('woSafesFoldersController', woSafesFoldersController);

    function woSafesFoldersController(folderContent, writeAccess, safesService, SAFES_CONSTANTS, $state, $rootScope, Modal, Notifications, SessionStore) {
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
      vm.createSecretWO = createSecretWO;
      vm.loading = loading;

      function loading(value) {
        vm.loadingFlag = value;
      }

      function createSecretWO(key, value) {
        var modalSettings = {
          title: 'Create Secret',
          pathValue: '',
          pathLabel: 'Folder Path',
          pathPlaceholder: 'Folder path',
          inputValue: '',
          inputLabel: 'Key',
          placeholder: 'Secret key',
          passwordValue: '',
          passwordLabel: 'Secret',
          passwordPlaceholder: 'Enter secret value',
          submitLabel: 'CREATE',
          cancelLabel: 'CANCEL'
        };
        
        return Modal.createWOModalWithController('wo-text-input.modal.html', modalSettings)
          .then(function (modalData) {
            var newSecret = {
              id: modalData.inputValue,
              key: modalData.inputValue,
              value: modalData.passwordValue,
              type: 'secret',
              parentId: folderContent.id,
              path: modalData.pathValue
            };
            return tryToSaveSecret(newSecret);
          })
      }

      function tryToSaveSecret(newSecret) {
            vm.loading(true);
            return safesService.saveFolderWO(folderContent, newSecret, folderContent.id + "/" + newSecret.path.toLowerCase())
              .then(function () {
                vm.loading(false);
                vm.folderContent.children = [];
                Notifications.toast('Added successfully');
              }).catch(catchError);
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