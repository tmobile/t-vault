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
        $scope.viewExternalCertificate = false;
        $scope.searchValueCert = "";
        $scope.certificateDetails = [];
        $scope.certIdToDownload = "";
        $scope.certificateType = "";
        $scope.isInternalCertificateTab = true;
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
            "format": ""
        }
        $scope.isDownloadClicked = false;
        var init = function () {
            if(!SessionStore.getItem("myVaultKey")){ /* Check if user is in the same session */
                $state.go('/');
                return;
            }
            else{
                $scope.viewCertificate = false;
                $scope.viewExternalCertificate = false;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                if (JSON.parse(SessionStore.getItem("isAdmin")) == true) {
                    $scope.certificateType = "internal";
                    $scope.requestDataForMyCertifiatesAdmin();
                }
                else {
                    $scope.requestDataForMyCertifiates();
                }
            }
        };

        $scope.selectDownloadFormat = function () {
            $scope.downloadRequest.format = $scope.dropdownDownload.selectedGroupOption.value;
        }

        $scope.filterCert = function(searchValueCert) {
            $scope.searchValueCert = searchValueCert;
        }

        $scope.requestDataForMyCertifiates = function () {
            $scope.isLoadingData = true;
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

            var policies = SessionStore.getItem("policies");
            if (policies !="" && policies !=null && policies != undefined) {
                var policiesArray = policies.split(",");
                var ownerPolicies = policiesArray.filter(function(policy){
                    return policy.substring(0, 7) == "o_cert_";
                });

                ownerPolicies.forEach(function (sudoPolicy) {
                    $scope.certificatesData.keys.push({"certname": sudoPolicy.substring(7), "permission": "sudo"});
                });
            }

            $scope.certificatesData.keys = data.filter(function(cert){
                return cert.permission === "read";
            });

            $scope.numOfCertificates=$scope.certificatesData.keys.length;
            $scope.isLoadingData = false;
        };

        $scope.isInternalCertificate = function(){
            $scope.certificateType = "internal";
            $scope.isInternalCertificateTab = true;
            $scope.viewExternalCertificate = false;
            $scope.requestDataForMyCertifiatesAdmin();
        }

        $scope.isExternalCertificate = function(){
            $scope.certificateType = "external";
            $scope.searchValueCert = "";
            document.getElementById('searchValueId').value = '';
            $scope.isInternalCertificateTab = false;
            $scope.viewCertificate = false;
            $scope.requestDataForMyCertifiatesAdmin();
        }

        $scope.requestDataForMyCertifiatesAdmin = function () {
            $scope.certificatesData = {"keys": []};
            $scope.isLoadingData = true;
             

            var updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/sslcert/certificates/" + $scope.certificateType;

          //  var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('listCertificatesByCertificateType', Math.random());
            AdminSafesManagement.listCertificatesByCertificateType(null, updatedUrlOfEndPoint).then(function (response) {

                if (UtilityService.ifAPIRequestSuccessful(response)) {
                    if (response.data != "" && response.data != undefined) {
                        angular.forEach(response.data.data.keys, function(value, key) {
                            $scope.certificatesData.keys.push({"certname": value, "permission": "read"});
                          });
                        $scope.numOfCertificates=$scope.certificatesData.keys.length;
                    }
                }
                else {
                    $scope.certificatesLoaded =  true;
                    $scope.errorMessage = AdminSafesManagement.getTheRightErrorMessage(response);
                    $scope.error('md');
                }
                $scope.isLoadingData = false;
            },
            function (error) {
                // Error handling function
                console.log(error);
                $scope.isLoadingData = false;
                $scope.certificatesLoaded =  true;
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
            });

        }
        
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
            if ($scope.searchValueCert != '' && $scope.searchValueCert!= undefined) {
                if ($scope.searchValueCert.length<3) {
                    return pagesShown < ($scope.numOfCertificates / pageSize);
                }
                else {
                    return false;
                }
            }
            return pagesShown < ($scope.numOfCertificates / pageSize);
        };

        $scope.showMoreItems = function() {
            pagesShown = pagesShown + 1;
        };

        $scope.goToCertificates = function() {
            $scope.viewCertificate = false;
            $scope.viewExternalCertificate = false;
        }

        $scope.getCertificate = function (certName) {
            $scope.isDownloadClicked = false;
            $scope.isLoadingData = true;
            $scope.downloadRequest.certificateName = "";
            var updatedUrlOfEndPoint = "";
            if($scope.isInternalCertificateTab){
                updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/sslcert/certificate/" + "internal" + "?certificate_name="+ certName;
            }else{
                updatedUrlOfEndPoint = RestEndpoints.baseURL + "/v2/sslcert/certificate/" + "external" + "?certificate_name="+ certName;
            }
            //var updatedUrlOfEndPoint = ModifyUrl.addUrlParameteres('getCertificateDetails',);

            AdminSafesManagement.getCertificateDetails(null, updatedUrlOfEndPoint).then(function (response) {

                if (UtilityService.ifAPIRequestSuccessful(response)) { 	
                    if($scope.isInternalCertificateTab){
                        $scope.viewCertificate = true;
                    }else{
                        $scope.viewExternalCertificate = true;
                    }
                    $scope.certificateDetails = response.data;
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

        $scope.downloadPopup = function () {
            $scope.isDownloadClicked = false;
            $scope.downloadRequest.certificateCred = "";
            $scope.downloadRequest.issuerChain = true;
            $scope.downloadRequest.format = "pkcs12der";
            $scope.dropdownDownload.selectedGroupOption = $scope.downloadFormats[0];
            Modal.createModal('md', 'downloadPopup.html', 'CertificatesCtrl', $scope);
        };

        $scope.downloadPopupWitoutKey = function () {
            $scope.isDownloadClicked = false;
            Modal.createModal('md', 'downloadPopupWitoutKey.html', 'CertificatesCtrl', $scope);
        };

        $scope.isDownloadDisabled = function () {
            if ($scope.downloadRequest.certificateCred ==undefined || $scope.downloadRequest.certificateCred.length < 8) {
                return true;
            }
            return false;
        }

        function getCertWithKey(reqObjtobeSent) {
            var url = RestEndpoints.baseURL + '/v2/sslcert/certificates/download'
            return $http({
                method: 'POST',
                url: url,
                data: reqObjtobeSent,
                headers: {
                    'Content-type': 'application/json',
                    'vault-token': SessionStore.getItem('myVaultKey')
                },
                responseType: 'blob',
            }).then(function (response) {
                return response;
            }).catch(function(error) {
                console.log(error);
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
                return error;
            });
        }


        $scope.download = function () {
            try {
                Modal.close('');
                $scope.isLoadingData = true;
                var reqObjtobeSent = $scope.downloadRequest;
                var fileType = ".p12";
                switch (reqObjtobeSent.format) {
                    case "pkcs12der": fileType=".p12"; break;
                    case "pembundle": fileType=".pem"; break;
                    case "pkcs12pem": fileType=".pfx"; break;
                }
                getCertWithKey(reqObjtobeSent, null).then(function (response) {
                    if (UtilityService.ifAPIRequestSuccessful(response)) {

                        var file = new Blob([response.data], { type: 'application/octet-stream' });
                        var fileURL = URL.createObjectURL(file);
                        var downloadlink = document.createElement('a');
                        downloadlink.href = fileURL;
                        downloadlink.target = '_blank';
                        downloadlink.download = reqObjtobeSent.certificateName+fileType;
                        document.body.appendChild(downloadlink);
                        downloadlink.click();
                        document.body.removeChild(downloadlink);
                        $scope.isLoadingData = false;
                    }
                    else {
                        $scope.isLoadingData = false;
                        console.log(response.status);
                    }
                },
                function (error) {
                    $scope.isLoadingData = false;
                    console.log(error);
                })
            } catch (e) {
                $scope.isLoadingData = false;
                console.log(e);
            }
        }

        function getCert(certificateName, format) {
            var url = RestEndpoints.baseURL + '/v2/sslcert/certificates/'+certificateName+'/'+format
            return $http({
                method: 'GET',
                url: url,
                headers: {
                    'vault-token': SessionStore.getItem('myVaultKey')
                },
                responseType: 'blob',
            }).then(function (response) {
                return response;
            }).catch(function(error) {
                console.log(error);
                $scope.errorMessage = UtilityService.getAParticularErrorMessage('ERROR_GENERAL');
                $scope.error('md');
                return error;
            });
        }

        $scope.downloadPemDer = function(format) {
            try {
                Modal.close('');
                $scope.isLoadingData = true;
                var certName = $scope.downloadRequest.certificateName;
                if (certName != "") {
                    getCert(certName, format, null).then(function (response) {
                        if (UtilityService.ifAPIRequestSuccessful(response)) {
                            var file = new Blob([response.data], { type: 'application/octet-stream' });
                            var fileURL = URL.createObjectURL(file);
                            var downloadlink = document.createElement('a');
                            downloadlink.href = fileURL;
                            downloadlink.target = '_blank';
                            downloadlink.download = certName+'.'+format;
                            document.body.appendChild(downloadlink);
                            downloadlink.click();
                            document.body.removeChild(downloadlink);
                            $scope.isLoadingData = false;
                        }
                        else {
                            $scope.isLoadingData = false;
                            console.log(response.status);
                        }
                    },
                    function (error) {
                        $scope.isLoadingData = false;
                        console.log(error);
                    })
                }
            } catch (e) {
                $scope.isLoadingData = false;
                console.log(e);
            }
        }

        $scope.showDownload = function () {
            $scope.isDownloadClicked = !$scope.isDownloadClicked;
        }

        $scope.hideDownload = function () {
            $scope.isDownloadClicked = false;
        }

        init();
        
    });
})(angular.module('vault.features.CertificatesCtrl',[
    'vault.services.fetchData',
    'vault.services.ModifyUrl',
    'vault.services.Notifications',
    'vault.constants.RestEndpoints'
]));