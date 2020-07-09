/*
* =========================================================================
* Copyright 2020 T-Mobile, US
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
    app.controller('CertificatesCtrl', function($scope, $rootScope, Modal, fetchData, $http, $window, $state, SessionStore, AdminSafesManagement, ModifyUrl, UtilityService, Notifications, safesService, RestEndpoints, CopyToClipboard){

        $scope.isLoadingData = false;       // Variable to set the loader on
        $scope.adminNavTags = safesService.getSafesNavTags();
        $scope.viewCertificate = false;
        $scope.searchValueCert = "";
        $scope.certificateDetails = [];
        $scope.certIdToDownload = "";
        $scope.downloadFormats = [
            {"type": "DER - P12", "value": "pkcs12der"},
            {"type": "PEM - PFX", "value": "pembundle"},
            {"type": "PEM - OPENSSL", "value": "pkcs12pem"}
        ]
        $scope.dropdownDownload = {
            'selectedGroupOption': $scope.downloadFormats[0],       // As initial placeholder
            'tableOptions': $scope.downloadFormats
        }
        $scope.downloadRequest = {
            "certificateCred": "",
            "issuerChain": "",
            "certificateName": "",
            "format": "pkcs12der"
        }
        var init = function () {
            
            if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            else{
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.requestDataForMyCertifiates();
            }
        };

        $scope.selectDownloadFormat = function () {
            $scope.downloadRequest.format = $scope.dropdownDownload.selectedGroupOption.value;
            console.log($scope.downloadRequest);
        }

        $scope.filterCert = function(searchValueCert) {
            $scope.searchValueCert = searchValueCert;
        }

        $scope.requestDataForMyCertifiates = function () {               
            $scope.certificatesData = {"keys": []};
            var data = [];
            var accessSafes = JSON.parse(SessionStore.getItem("accessSafes"));
            if (accessSafes.cert) {
                data = accessSafes.cert.map(function (certObject) {
                    var entry = Object.entries(certObject);
                    return {
                        certname: entry[0][0],
                        permission: entry[0][1]
                    }
                });
            }
           
            $scope.certificatesData.keys = data.filter(function(cert){
                return cert.permission === "read";
              });
            $scope.numOfCertificates=$scope.certificatesData.keys.length;
        };

        
        $scope.error = function (size) {
            Modal.createModal(size, 'error.html', 'CertificatesCtrl', $scope);
        };

        $rootScope.close = function () {
            Modal.close();
        };

        var pagesShown = 1;
        var pageSize = 20;
        $scope.paginationLimit = function() {
            $scope.currentshown = pageSize * pagesShown;
            if(($scope.searchValueCert != '' && $scope.searchValueCert!= undefined && $scope.searchValueCert.length>2) || $scope.currentshown >= $scope.numOfCertificates){
                $scope.currentshown = $scope.numOfCertificates;
            }
            return $scope.currentshown;
        };

        $scope.hasMoreItemsToShow = function() {
            if ($scope.searchValueCert != '' && $scope.searchValueCert!= undefined && $scope.searchValueCert.length<3) {
                return pagesShown < ($scope.numOfCertificates / pageSize);
            }
            return false;
        };

        $scope.goToCertificates = function() {
            $scope.viewCertificate = false;
        }

        $scope.getCertificate = function (certName) {
            $scope.isLoadingData = true;
            $scope.downloadRequest.certificateName = "";
            var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getCertificates',"certificateName="+ certName + "&limit=1&offset=0");

            AdminSafesManagement.getCertificates(null, updatedUrlOfEndPoint).then(function (response) {
            	 
                if (UtilityService.ifAPIRequestSuccessful(response)) { 	
                    $scope.viewCertificate = true;
                    $scope.certificateDetails = response.data.keys[0];
                    $scope.certificateDetails.createDate = new Date($scope.certificateDetails.createDate).toDateString();
                    $scope.certificateDetails.expiryDate = new Date($scope.certificateDetails.expiryDate).toDateString();
                    $scope.certIdToDownload = $scope.certificateDetails.certificateId;

                    $scope.downloadRequest.certificateName = $scope.certificateDetails.certificateName;
                }
                else {                   
                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                    $scope.error('md');
                }
                $scope.isLoadingData = false;
            },
            function (error) {
                // Error handling function
                console.log(error);
                $scope.isLoadingData = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            });         
        }

        $scope.downloadPopup = function (certId) {
            $scope.certIdToDownload = certId;
            Modal.createModal('md', 'downloadPopup.html', 'CertificatesCtrl', $scope);
        };

        $scope.isDownloadDisabled = function () {
            if ($scope.downloadRequest.certificateCred ==undefined || $scope.downloadRequest.certificateCred.length < 8) {
                return true;
            }
            return false;
        }

        $scope.download = function () {

        }
        
        init();
        
    });
})(angular.module('vault.features.CertificatesCtrl',[
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications',
    'vault.constants.RestEndpoints'
]));