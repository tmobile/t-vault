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
( function( app ) {
    app.constant( 'SuccessMessage', {        
        'MESSAGE_ADD_SUCCESS'      : ' added successfully!',
        'MESSAGE_SAFE_DELETE'      : ' deleted successfully!',
        'MESSAGE_CREATE_SUCCESS'   : ' created successfully!',
        'MESSAGE_UPDATE_SUCCESS'   : ' updated successfully!',
        'COPY_TO_CLIPBOARD'        : 'Secret copied to clipboard',
        'COPY_KEY_TO_CLIPBOARD'    : 'Key copied to clipboard',
        'MESSAGE_ACCESSOR_DELETE'  : 'Accessor ID deleted Successfully',
        'MESSAGE_DELETE_SUCCESS'   : ' deleted Successfully',
        'MESSAGE_RESET_SUCCESS'   : ' reset Successfully',
        'MESSAGE_OFFBOARD_SUCCESS'   : ' offboarded Successfully',
        'MESSAGE_ONBOARD_SUCCESS'   : ' onboarded Successfully',
        'COPY_ACCESSKEY_TO_CLIPBOARD': 'Access key copied to clipboard',
        'COPY_SECRETKEY_TO_CLIPBOARD': 'Secret key copied to clipboard',
        'COPY_SECRETTEXT_TO_CLIPBOARD': 'Secret text copied to clipboard'       
    } );
} )( angular.module( 'vault.constants.SuccessMessage', [] ) );
