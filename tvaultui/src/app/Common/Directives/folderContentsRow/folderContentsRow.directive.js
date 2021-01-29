/*
* =========================================================================
* Copyright 2019 T-Mobile, US
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* See the readme.txt file for additional language around disclaimer of warranties.
* =========================================================================
*/

'use strict';
(function () {
  angular.module('vault.directives.folderContentsRow', [])
    .controller('folderContentsRowController', folderContentsTableController)
    .directive('folderContentsRow', function () {
      return {
        restrict: 'E',
        templateUrl: 'app/Common/Directives/folderContentsRow/folderContentsRow.html',
        scope: {
          index: '=',
          item: '=',
          lastChanged: '=',
          parent: '=',
          loading: '=',
          write: '='
        },
        link: function (scope) {
          var vm = scope.vm;
          vm.originalId = vm.item.id;
          vm.originalValue = vm.item.value;
          vm.isSecret = vm.item.type === 'secret';
          if (!vm.isSecret) {
            vm.folderName = vm.item.id.split('/').pop();
            vm.lastChangedDetails = vm.getLastChangeDetailsForFolder();
          }
          else {
            vm.lastChangedDetails = vm.getLastChangeDetailsForSecret();
          }
          console.log( vm.lastChangedDetails)
        },
        controller: 'folderContentsRowController as vm',
        bindToController: true
      }
    });

  function folderContentsTableController($scope, CopyToClipboard, SafesManagement, Modal, UtilityService, Notifications, $rootScope, toastr, safesService, $timeout, $state) {
    var vm = this;
    vm.anyRegex = /.|\s/g;
    vm.originalId = '';
    vm.originalValue = '';
    vm.showPassword = false;
    vm.editing = false;
    vm.onRowClick = onRowClick;
    vm.edit = edit;
    vm.deleteSecret = deleteSecret;
    vm.deleteFolder = deleteFolder;
    vm.copyToClipboard = copyToClipboard;
    vm.getLastChangeDetailsForFolder = getLastChangeDetailsForFolder;
    vm.getLastChangeDetailsForSecret = getLastChangeDetailsForSecret;

    function getLastChangeDetailsForFolder() {
      var lastChangedDetails = "";
      for (var i=0; i<vm.lastChanged.length; i++) {
        if (vm.lastChanged[i].folderPath == vm.item.id) {
          lastChangedDetails = vm.lastChanged[i];
          lastChangedDetails.folderModifiedAtFormatted = formatTime(lastChangedDetails.folderModifiedAt);
        }
      }
      return lastChangedDetails;
    }

    function getLastChangeDetailsForSecret() {
      var lastChangedDetails = "";
      for (var i=0; i<vm.lastChanged.length; i++) {
        if (vm.lastChanged[i].folderPath == vm.item.parentId && vm.lastChanged[i].secretVersions && vm.lastChanged[i].secretVersions[vm.item.id]) {
          lastChangedDetails = vm.lastChanged[i].secretVersions[vm.item.id][0];
          lastChangedDetails.modifiedAtFormatted = formatTime(lastChangedDetails.modifiedAt);
        }
      }
      return lastChangedDetails;
    }

    function formatTime(timestamp) {
      if (!isNaN(timestamp)) {
        return moment(timestamp).fromNow();
      }
      return "";
    }

    function edit() {
      editSecret(vm.item.key, vm.item.value);
      vm.editing = true;
    }

    function copyToClipboard($event, copyValue, messageKey) {
      $event.stopPropagation();
      var notification = UtilityService.getAParticularSuccessMessage(messageKey);
      Notifications.toast(notification);
      CopyToClipboard.copy(copyValue);
    }

    function onRowClick($event) {
      if (vm.editing) {
        $event.stopPropagation();
      }
    }

    function deleteSecret($event) {
      return Modal.createModalWithController('confirm.modal.html', {
        title: 'Confirmation',
        message: 'Are you sure you want to delete this secret?',
        submitLabel: 'DELETE'
      })
        .then(function () {
          vm.loading(true);
          var modifiedFolder = {
            id: vm.parent.id,
            children: vm.parent.children.slice(0)
          };
          var index = modifiedFolder.children.findIndex(function (item) {
            return item.id === vm.item.id;
          });
          modifiedFolder.children.splice(index, 1);
          return safesService.saveFolder(modifiedFolder)
            .then(function (response) {
              vm.loading(false);
              vm.parent.children.splice(index, 1);
              Notifications.toast('Deleted successfully');
            })
            .catch(catchError)
        })

    }

    function deleteFolder($event) {
      $event.stopPropagation();
      return Modal.createModalWithController('confirm.modal.html', {
        title: 'Confirmation',
        message: 'Are you sure you want to delete this folder?',
        submitLabel: 'DELETE'
      })
        .then(function () {
          vm.loading(true);
          return safesService.deleteFolder(vm.item.id)
            .then(function (response) {
              vm.loading(false);
              var index = vm.parent.children.findIndex(function (item) {
                return item.id === vm.item.id;
              });
              vm.parent.children.splice(index, 1);
              Notifications.toast('Deleted successfully');
            }).catch(catchError);
        });
    }

    function catchError(error) {
      if (error) {
        vm.item.key = vm.originalId;
        vm.item.value = vm.originalValue;
        Modal.createModalWithController('stop.modal.html', {
          title: 'Error',
          message: 'Please try again. If this issue persists please contact an administrator.'
        });
      }
      vm.loading(false);
      console.log(error);
    }

    function editSecret(key, value) {
      var modalSettings = {
        title: 'Edit Secret',
        inputValue: key || '',
        inputLabel: 'Key',
        placeholder: 'Enter secret key',
        passwordValue: value || '',
        passwordLabel: 'Secret',
        passwordPlaceholder: 'Enter secret value',
        submitLabel: 'SAVE',
        cancelLabel: 'CANCEL'
      };
      return Modal.createModalWithController('text-input.modal.html', modalSettings)
        .then(function (modalData) {
          return safesService.itemIsValidToSave({
            key: modalData.inputValue,
            value: modalData.passwordValue
          }, vm.index, vm.parent)
        .then(function () {
              vm.item.key = modalData.inputValue;
              vm.item.value = modalData.passwordValue;
              vm.item.id = modalData.inputValue;
              vm.loading(true);
          return safesService.saveFolder(vm.parent)
        .then(function (response) {
                  vm.loading(false);
                  vm.editing = false;
                  Notifications.toast('Saved successfully');
                  $state.reload();
                }).catch(catchError);
            })
        .catch(function () {
              return editSecret(key, value);
            })
        }).finally(function () {
          vm.editing = false;
        });

    }
  }
})
();