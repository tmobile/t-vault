(function () {
    'use strict';
    angular.module('vault.features.safes')
        .config(config);

    function config($provide) {

        $provide.constant('SAFES_CONSTANTS', {
            'SAFE_TYPES': [{
                label: 'User',
                tabLabel: 'My Safes',
                key: 'users'
            },
                {
                    label: 'Shared',
                    tabLabel: 'Shared Safes',
                    key: 'shared'
                },
                {
                    label: 'Application',
                    tabLabel: 'Application Safes',
                    key: 'apps'
                }
            ]
        })
    }
})();
