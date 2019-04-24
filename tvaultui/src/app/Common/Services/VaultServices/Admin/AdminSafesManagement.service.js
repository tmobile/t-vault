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
