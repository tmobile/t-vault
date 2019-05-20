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
(function (app) {
  app.service('Authentication', function (RestEndpoints, fetchData, $window, ServiceEndpoint, ErrorMessage, $q, $http, Idle, Keepalive, AppConstant, SessionStore, $state) {
    var service = {
      authenticateUser: function (reqObjtobeSent, callback) {
        try {

          return ServiceEndpoint.login
            .makeRequest(reqObjtobeSent)
            .then(function (response) {
                var leaseDuration = response.data['lease_duration'];
                if (leaseDuration == undefined) {
                    leaseDuration = 300;
                }
                Idle.setIdle(180);
                Idle.setTimeout(leaseDuration - 180);
                Keepalive.setInterval(leaseDuration - 60);
                Idle.watch();
                return response;
              },
              function (error) {
                console.log("error in login");
                console.log(error);
                return error;
              });
        } catch (e) {
          return callback({"error": e});
        }
      },
      renewAuthToken: function (vaultAPIKey) {
        return ServiceEndpoint.renewToken.makeRequest(null, null, {"vault-token": vaultAPIKey})
          .then(function (response) {
            var leaseDuration = response.data['lease_duration'];
            if (leaseDuration == undefined) {
                leaseDuration = 300;
            }
            Idle.setIdle(180);
            Idle.setTimeout(leaseDuration - 180);
            Keepalive.setInterval(leaseDuration - 60);
          }, function (error) {
            logout(true);
            console.log("error retrieving token", error);
            return error;
          });
      },
      revokeAuthToken: function () {
        var url = RestEndpoints.baseURL + '/auth/tvault/revoke';
        return $http({
          method: 'GET',
          url: url,
          headers: {
            'vault-token': SessionStore.getItem('myVaultKey')
          }
        })
          .then(function (response) {
            return response.data;
          });
      },
      getTheRightErrorMessage: function (responseObject) {
        if (responseObject.status === '500' || responseObject.statusText === 'Internal Server Error') {
          return ErrorMessage.ERROR_NETWORK;
        }
        else if (responseObject.status === '404') {
          return ErrorMessage.ERROR_WRONG_USERNAME_PASSWORD;
        }
        else {
          return ErrorMessage.ERROR_GENERAL;
        }
      },

      formatUsernameWithoutDomain: function (username) {
        var regex = /^(corp\/|corp\\)/gi;
        return username.replace(regex, '');
      },

      logout: logout

    };

    function logout(withoutRevoke) {
      var url = '/#!/';
      if (withoutRevoke) {
        window.location.replace(url)
      } else {
        return service.revokeAuthToken()
            .finally(function (error) {
            SessionStore.removeItem("myVaultKey");
            SessionStore.removeItem("isAdmin");
            SessionStore.removeItem("accessSafes");
            SessionStore.removeItem("policies");
            SessionStore.removeItem("allSafes");
            SessionStore.removeItem("feature");
            window.location.replace(url);
          });
      }
    }


    return service;
  });
})(angular.module('vault.services.Authentication', [
  'vault.services.ServiceEndpoint',
  'vault.constants.ErrorMessage'
]));
