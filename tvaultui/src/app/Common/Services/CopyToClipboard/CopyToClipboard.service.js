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
    app.service( 'CopyToClipboard', function() {
        // AngularJS will instantiate a singleton by calling "new" on this function
        var globalData = {};
        return {
            // Implementation of 'copy to clipboard' feature

            copy : function (keyValue) {
                var key = keyValue;

                // create temp element
                var copyElement = document.createElement("span");
                copyElement.appendChild(document.createTextNode(key));
                copyElement.id = 'tempCopyToClipboard';
                angular.element(document.body.append(copyElement));

                // select the text
                var range = document.createRange();
                range.selectNode(copyElement);
                window.getSelection().removeAllRanges();
                window.getSelection().addRange(range);

                // copy & cleanup
                document.execCommand('copy');
                window.getSelection().removeAllRanges();
                copyElement.remove();
            }

        };
    } );
})(angular.module('vault.services.CopyToClipboard',[]));