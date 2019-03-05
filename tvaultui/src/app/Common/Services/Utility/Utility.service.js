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
    app.service('UtilityService', function (AppConstant, ErrorMessage, SuccessMessage) {
        this.split = function (val) {
            return val.split(/,\s*/);
        };
        this.extractLast = function (term) {
            return this.split(term).pop();
        };

        this.formatName = function (name) {
            if (name !== null && name !== undefined) {
                name = name.toLowerCase();
                return name.replace(/[ ]/g, '-');
            }
        }

        this.getAppConstant = function (key) {
            return AppConstant[key];
        }

        this.ifAPIRequestSuccessful = function (responseObject) {
            return (responseObject.status === '200' || responseObject.statusText === 'OK' || responseObject.status === 200);
        };

        this.getAParticularErrorMessage = function (errorKey) {
            return ErrorMessage[errorKey];
        };

        this.getAParticularSuccessMessage = function (successKey) {
            return SuccessMessage[successKey];
        };

        this.isObjectEmpty = function (obj) {
            if (obj) {
                return Object.keys(obj).length === 0 && obj.constructor === Object;
            } else {
                return true;
            }
        };

    });
})(angular.module('vault.services.UtilityService', [
    'vault.constants.AppConstant',
    'vault.constants.ErrorMessage',
    'vault.constants.SuccessMessage'
]));
