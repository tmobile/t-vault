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
/* Service includes all functions related to Admin Safe Management feature */
(function(app){
    app.service( 'AdminSafesManagement', function( ServiceEndpoint, $q, DataCache, fetchData, $rootScope, ErrorMessage ) {

        return {
            getCompleteSafesList: function(payload, url) {
                return ServiceEndpoint.safesList.makeRequest(payload,url).then(function(response) {
                    return response;
                });
            },
            deleteSafe: function(payload, url) {
                return ServiceEndpoint.deleteSafe.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSafeInfo: function(payload, url) {
                return ServiceEndpoint.getSafeInfo.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            createSafe: function(payload, url) {
                return ServiceEndpoint.createSafe.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            editSafe: function(payload, url) {
                return ServiceEndpoint.editSafe.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteUserPermissionFromSafe: function(payload, url) {
                return ServiceEndpoint.deleteUserPermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteGroupPermissionFromSafe: function(payload, url) {
                return ServiceEndpoint.deleteGroupPermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteAWSPermissionFromSafe: function(payload, url) {
                return ServiceEndpoint.deleteAWSPermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            detachAWSPermissionFromSafe: function(payload, url) {
                return ServiceEndpoint.detachAWSPermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addUserPermissionForSafe: function(payload, url) {
                return ServiceEndpoint.addUserPermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addGroupPermissionForSafe: function(payload, url) {
                return ServiceEndpoint.addGroupPermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAWSPermissionForSafe: function(payload, url) {
                return ServiceEndpoint.addAWSPermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getApproles: function(payload, url) {
                return ServiceEndpoint.getApproles.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getApproleDetails: function(payload, url) {
                return ServiceEndpoint.getApproleDetails.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAppRolePermissionForSafe: function(payload, url) {
                return ServiceEndpoint.addApprolePermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getAccessorIDs: function(payload, url) {
                return ServiceEndpoint.getAccessorIDs.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteAccessorID: function(payload, url) {
                return ServiceEndpoint.deleteAccessorID.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            }, 
            readSecretID: function(payload, url) {
                return ServiceEndpoint.readSecretID.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            readRoleID: function(payload, url) {
                return ServiceEndpoint.readRoleID.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteAppRole: function(payload, url) {
                return ServiceEndpoint.deleteAppRole.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },            
            detachAppRolePermissionFromSafe: function(payload, url) {
                return ServiceEndpoint.detachAppRolePermission.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getAWSConfigurationDetails: function(payload, url) {
                return ServiceEndpoint.getAwsConfigurationDetails.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            }, 
            addAWSRole: function(payload, url) {
                return ServiceEndpoint.createAwsRole.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            updateAWSRole: function(payload, url) {
                return ServiceEndpoint.updateAwsRole.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAWSIAMRole: function(payload, url) {
                return ServiceEndpoint.createAwsIAMRole.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            updateAWSIAMRole: function(payload, url) {
                return ServiceEndpoint.updateAwsIAMRole.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAppRole: function(payload, url) {
                return ServiceEndpoint.createAppRole.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            updateAppRole: function(payload, url) {
                return ServiceEndpoint.updateAppRole.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getOnboardedServiceAccounts: function(payload, url) {
                return ServiceEndpoint.getOnboardedServiceAccounts.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getServiceAccounts: function(payload, url) {
                return ServiceEndpoint.getServiceAccounts.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSvcaccInfo: function(payload, url) {
                return ServiceEndpoint.getSvcaccInfo.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSvcaccOnboardInfo: function(payload, url) {
                return ServiceEndpoint.getSvcaccOnboardInfo.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            editSvcacc: function(payload, url) {
                return ServiceEndpoint.editSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            onboardSvcacc: function(payload, url) {
                return ServiceEndpoint.onboardSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addUserPermissionForSvcacc: function(payload, url) {
                return ServiceEndpoint.addUserPermissionForSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteUserPermissionFromSvcacc: function(payload, url) {
                return ServiceEndpoint.deleteUserPermissionFromSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addGroupPermissionForSvcacc: function(payload, url) {
                return ServiceEndpoint.addGroupPermissionForSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteGroupPermissionFromSvcacc: function(payload, url) {
                return ServiceEndpoint.deleteGroupPermissionFromSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAWSPermissionForSvcacc: function(payload, url) {
                return ServiceEndpoint.addAWSPermissionForSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            detachAWSPermissionFromSvcacc: function(payload, url) {
                return ServiceEndpoint.detachAWSPermissionFromSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAppRolePermissionForSvcacc: function(payload, url) {
                return ServiceEndpoint.addAppRolePermissionForSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            detachAppRolePermissionFromSvcacc: function(payload, url) {
                return ServiceEndpoint.detachAppRolePermissionFromSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            createAwsRoleSvcacc: function(payload, url) {
                return ServiceEndpoint.createAwsRoleSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            createAwsIAMRoleSvcacc: function(payload, url) {
                return ServiceEndpoint.createAwsIAMRoleSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSecretForSvcacc: function(payload, url) {
                return ServiceEndpoint.getSecretForSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            resetPasswordForSvcacc: function(payload, url) {
                return ServiceEndpoint.resetPasswordForSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            offboardSvcacc: function(payload, url) {
                return ServiceEndpoint.offboardSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSvcaccMetadata: function(payload, url) {
                return ServiceEndpoint.getSvcaccMetadata.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getApprolesFromCwm: function(payload, url) {
                return ServiceEndpoint.getApprolesFromCwm.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            transferSvcaccOwner: function(payload, url) {
                return ServiceEndpoint.transferSvcaccOwner.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getCertificates: function (payload, url) {
                return ServiceEndpoint.getCertificates.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getRevocationReasons: function (payload, url) {
                return ServiceEndpoint.getRevocationReasons.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getTargetSystems:function (payload, url) {
                return ServiceEndpoint.getTargetSystems.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getTargetSystemsServices: function (payload, url) {
                return ServiceEndpoint.getTargetSystemsServices.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            sslCertificateCreation: function (payload, url) {
                return ServiceEndpoint.createSslCertificates.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            usersGetData: function (payload, url) {
                return ServiceEndpoint.usersGetData.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            usersGetDataUsingCorpID: function (payload, url) {
                return ServiceEndpoint.usersGetDataUsingCorpID.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            issueRevocationRequest: function (payload, url) {
                return ServiceEndpoint.issueRevocationRequest.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addUserPermissionForCertificate: function(payload, url) {
                return ServiceEndpoint.addUserToCertificate.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addGroupPermissionForCertificate: function(payload, url){
                return ServiceEndpoint.addGroupToCertificate.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getCertificateDetails: function (payload, url) {
                return ServiceEndpoint.getCertificateDetails.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addApprolePermissionForCertificate: function(payload, url) {
                return ServiceEndpoint.addApproleToCertificate.makeRequest(payload, url).then(function(response) {
                	 return response;
                });
            },
            renewCertificate: function (payload, url) {
                return ServiceEndpoint.renewCertificate.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteUserPermissionFromCertificate: function(payload, url) {
                return ServiceEndpoint.deleteUserPermissionFromCertificate.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteGroupPermissionFromCertificate: function(payload, url) {
                return ServiceEndpoint.deleteGroupPermissionFromCertificate.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            listCertificatesByCertificateType: function (payload, url) {
                return ServiceEndpoint.listCertificatesByCertificateType.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            transferCertificate: function (payload, url) {
                return ServiceEndpoint.transferCertificate.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            validateCertificateDetails: function (payload, url) {
                return ServiceEndpoint.validateCertificateDetails.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteCertificate: function (payload, url) {
                return ServiceEndpoint.deleteCertificate.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getAuthUrl: function (payload, url) {
                return ServiceEndpoint.getAuthUrl.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getCallback: function (payload, url) {
                return ServiceEndpoint.getCallback.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            checkRevokestatus: function (payload, url) {	
                return ServiceEndpoint.checkRevokestatus.makeRequest(payload, url).then(function(response) {	
                    return response;	
                });	
            },
            getAllSelfServiceGroups: function (payload, url) {
                return ServiceEndpoint.getAllSelfServiceGroups.makeRequest(payload, url).then(function(response) {	
                    return response;
                });
            },
            getOnboardedIamServiceAccounts: function(payload, url) {
                return ServiceEndpoint.getOnboardedIamServiceAccounts.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getIamServiceAccount: function(payload, url) {
                return ServiceEndpoint.getIamServiceAccount.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSecretForIamSvcacc: function(payload, url) {
                return ServiceEndpoint.getSecretForIamSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addUserPermissionForIAMSvcacc: function(payload, url) {
                return ServiceEndpoint.addUserPermissionForIAMSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteUserPermissionFromIAMSvcacc: function(payload, url) {
                return ServiceEndpoint.deleteUserPermissionFromIAMSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addGroupPermissionForIAMSvcacc: function(payload, url) {
                return ServiceEndpoint.addGroupPermissionForIAMSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteGroupPermissionFromIAMSvcacc: function(payload, url) {
                return ServiceEndpoint.deleteGroupPermissionFromIAMSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAppRolePermissionForIAMSvcacc: function(payload, url) {
                return ServiceEndpoint.addAppRolePermissionForIAMSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            detachAppRolePermissionFromIAMSvcacc: function(payload, url) {
                return ServiceEndpoint.detachAppRolePermissionFromIAMSvcacc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getTheRightErrorMessage : function(responseObject){
                if(responseObject.status===500 || responseObject.statusText==='Internal Server Error'){
                    return ErrorMessage.ERROR_NETWORK;
                }
                else if(responseObject.status===404){
                    return ErrorMessage.ERROR_CONTENT_NOT_FOUND;    // TODO: show different messages for POST and GET methods
                }
                else if(responseObject.status === 422){
                    if(responseObject.data && responseObject.data.errors) {
                        var error = responseObject.data.errors;
                        if (error instanceof Array && error.length > 0 ) {
                            return error[0];
                        } else if (error.length > 0) {
                            return error;
                        } else {
                            return ErrorMessage.ERROR_GENERAL;
                        }
                    } else {
                        return ErrorMessage.ERROR_GENERAL;
                    }                    
                } else if(responseObject.status === 400){
                    var error = responseObject.data.errors;
                    if (error instanceof Array && error.length > 0 ) {
                        return error[0];
                    } else if (error.length > 0) {
                        return error;
                    } else {
                        return ErrorMessage.ERROR_GENERAL;
                    }
                }
                else{
                    return ErrorMessage.ERROR_GENERAL;
                }
            }
        }

    } );
})(angular.module('vault.services.AdminSafesManagement',[]));
