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
            getSvcInfo: function(payload, url) {
                return ServiceEndpoint.getSvcInfo.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSvcOnboardInfo: function(payload, url) {
                return ServiceEndpoint.getSvcOnboardInfo.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            editSvc: function(payload, url) {
                return ServiceEndpoint.editSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            onboardSvc: function(payload, url) {
                return ServiceEndpoint.onboardSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addUserPermissionForSvc: function(payload, url) {
                return ServiceEndpoint.addUserPermissionForSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteUserPermissionFromSvc: function(payload, url) {
                return ServiceEndpoint.deleteUserPermissionFromSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addGroupPermissionForSvc: function(payload, url) {
                return ServiceEndpoint.addGroupPermissionForSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteGroupPermissionFromSvc: function(payload, url) {
                return ServiceEndpoint.deleteGroupPermissionFromSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAWSPermissionForSvc: function(payload, url) {
                return ServiceEndpoint.addAWSPermissionForSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            detachAWSPermissionFromSvc: function(payload, url) {
                return ServiceEndpoint.detachAWSPermissionFromSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            deleteAWSPermissionFromSvc: function(payload, url) {
                return ServiceEndpoint.deleteAWSPermissionFromSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            addAppRolePermissionForSvc: function(payload, url) {
                return ServiceEndpoint.addAppRolePermissionForSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            detachAppRolePermissionFromSvc: function(payload, url) {
                return ServiceEndpoint.detachAppRolePermissionFromSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            createAwsRoleSvc: function(payload, url) {
                return ServiceEndpoint.createAwsRoleSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            createAwsIAMRoleSvc: function(payload, url) {
                return ServiceEndpoint.createAwsIAMRoleSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            getSecretForSvc: function(payload, url) {
                return ServiceEndpoint.getSecretForSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            resetPasswordForSvc: function(payload, url) {
                return ServiceEndpoint.resetPasswordForSvc.makeRequest(payload, url).then(function(response) {
                    return response;
                });
            },
            offboardSvc: function(payload, url) {
                return ServiceEndpoint.offboardSvc.makeRequest(payload, url).then(function(response) {
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
