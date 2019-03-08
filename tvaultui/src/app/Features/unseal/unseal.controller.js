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
(function(app){
    app.controller('UnsealCtrl', function($scope, fetchData, Unseal, ModifyUrl,
          Modal,  $rootScope, $http, $log, $state, SessionStore, UtilityService) {

        $rootScope.isLoadingData = false; // Variable to set the loader on
        $scope.isLoadingModalData = false;
        $scope.fetchDataError = false; // set when there is any error while fetching or massaging data
        $scope.payload = {};
        $scope.keySubmitted = false;
        $scope.showProgress = false;

        // modal popup

        var error = function(size) {
            Modal.createModal(size, 'error.html', 'UnsealCtrl', $scope);
        };

        $scope.close = function() {
            Modal.close();
        };

        // Fetching Data
        
        $scope.checkUnsealProgress = function() {
            try {
                $rootScope.isLoadingData = true;
                $scope.keySubmitted = false;
                $scope.payload.key = "";
                Unseal.unsealProgress(null, $scope.payload.serverip).then(
                    function(response) {
                        if(UtilityService.ifAPIRequestSuccessful(response)){
                            console.log('response', response);
                            $rootScope.isLoadingData = false;
                            var data = response.data;
                            $scope.showProgress = true;
                            if (data.sealed === false) {
                                $scope.progress = 3;
                            } else {
                                $scope.progress = data.progress;
                                console.log('initial -->', $scope.progress);
                                SessionStore.setItem("currentProgress", JSON.stringify($scope.progress));
                            }
                        } else {
                            $rootScope.isLoadingData = false;
                            $scope.errorMessage = Unseal.getTheRightErrorMessage(response);
                            if($scope.errorMessage !=='Requested content not found!'){
                                error('md');
                            }
                        }
                    }, function(err) {
                        $rootScope.isLoadingData = false;
                        $scope.errorMessage = Unseal.getTheRightErrorMessage(err);
                        if($scope.errorMessage !=='Requested content not found!'){
                            error('md');
                        }
                    })
            } catch (e) {
                // To handle errors while calling 'fetchData' function
                console.log(e);
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                error('md');
                $rootScope.isLoadingData = false;
            }
        };

        $scope.submitKey = function() {
            try {
                $rootScope.isLoadingData = true;
                Unseal.unseal($scope.payload).then(
                    function(response) {
                        if(UtilityService.ifAPIRequestSuccessful(response)){
                            $rootScope.isLoadingData = false;
                            var data = response.data;
                            $scope.showProgress = true;
                            var previousProgress = JSON.parse(SessionStore.getItem("currentProgress"));
                            if (data.sealed === false) {
                                $scope.progress = 3;
                                $scope.keySubmitted = true;
                            } else if (data.progress == previousProgress) {
                                $scope.errorMessage = "Key has already been entered, please try a different key.";
                                error('md');
                            }
                            else {
                                $scope.progress = data.progress;
                                $scope.keySubmitted = true;
                            }
                        }
                        else{
                            $rootScope.isLoadingData = false;
                            $scope.checkUnsealProgress();
                            $scope.errorMessage = Unseal.getTheRightErrorMessage(response);
                            if($scope.errorMessage !=='Requested content not found!'){
                                error('md');
                            }
                        }
                    }, function(err) {
                        $rootScope.isLoadingData = false;
                        $scope.checkUnsealProgress();
                        $scope.errorMessage = err.data.errors[0];
                        if($scope.errorMessage !=='Requested content not found!'){
                            error('md');
                        }
                    })
            } catch (e) {
                // To handle errors while calling 'fetchData' function
                console.log(e);
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                error('md');
                $rootScope.isLoadingData = false;
            } // wraps outer catch block
        };


    });
})(angular.module('vault.features.UnsealCtrl', [
    'vault.services.SafesManagement',
    'vault.services.Notifications'
]));
