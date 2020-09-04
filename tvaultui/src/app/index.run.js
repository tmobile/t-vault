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

(function() {
  'use strict';
  angular
    .module('vault')
      .config(function($httpProvider){
          $httpProvider.interceptors.push( 'httpInterceptor' );
      })
    .run(runBlock);

  /** @ngInject */
  function runBlock($log, Idle, $rootScope, $state, Keepalive, SessionStore, Authentication) {
      $rootScope.$on('IdleTimeout', function () {
          Authentication.logout(false);
      });

      $rootScope.$on('Keepalive', function () {
          var vaultAPIKey = SessionStore.getItem('myVaultKey');
          if(vaultAPIKey) {
                return Authentication.renewAuthToken(vaultAPIKey);
            }
      });

      Idle.watch();
      Keepalive.ping();
    $log.debug('runBlock end');
  }

})();
