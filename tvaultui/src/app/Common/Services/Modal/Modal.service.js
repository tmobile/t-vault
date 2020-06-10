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
(function(app) {
    app.service( 'Modal', function( $uibModal ) {
        var modalInstance;
        var animationsEnabled = true;   // Toggle to enable or disable animations of the modal
        return {
            createModal : function (size, template, controller, scope, $q) {
                if(modalInstance){
                    this.close();
                }
                /*if(!modalInstance) {     // to avoid infinite recreation of modal instances in case of error
                                                            // while fetching data from the servers
                    modalInstance = $uibModal.open({
                        animation: animationsEnabled,
                        templateUrl: template,              // template of the current modal window
                        // controller: controller,          // the controller to control the current instance of the modal
                        size: size,                         // size of the modal, 'sm', 'md', 'lg'
                        scope: scope
                    });
                }*/

                 modalInstance = $uibModal.open({
                 animation: animationsEnabled,
                 templateUrl: template,              // template of the current modal window
                 // controller: controller,          // the controller to control the current instance of the modal
                 size: size,                         // size of the modal, 'sm', 'md', 'lg'
                 scope: scope,
                 backdrop: 'static'
                 });
            },

            close : function () {
                if(modalInstance) {
                    modalInstance.dismiss('Close clicked');       // modalInstance.dismiss('....') is also valid way to close the modal
                    modalInstance = null;
                }                
            },
            save : function () {
                modalInstance.close('Save clicked');        // modalInstance.dismiss('....') is also valid way to close the modal
                modalInstance = null;
            },
            createModalWithController: function (templateUrl, config) {
                //Modals are stored in app/Layout/base/base.jade
                var modal = $uibModal.open({
                    templateUrl: templateUrl,
                    animationsEnabled: animationsEnabled,
                    size: config.size || 'md',
                    backdrop: 'static',
                    controller: function ($scope, $uibModalInstance) {
                        $scope.dismiss = $uibModalInstance.dismiss;
                        $scope.close = $uibModalInstance.close;
                        $scope.config = config;
                        $scope.form = {
                            inputValue: '',
                            passwordValue: ''
                        }
                    }
                });

                return modal.result;
            }

        };
    });
})(angular.module('vault.services.Modal',[]));
