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
    app.constant( 'ErrorMessage', {
        'ERROR_GENERAL': 'Please try again, if the issue persists contact Vault Administrator',
        'ERROR_WRONG_USERNAME_PASSWORD': 'Username or password is wrong, please try again',
        'ERROR_NETWORK':'Please check your network connection, if the issue persists contact Vault Administrator',
        'ERROR_CONTENT_NOT_FOUND' : 'Requested content not found!',
        'ERROR_CONTENT_EXISTS' : 'Content already exits',
        'ERROR_PROCESSING_DATA'  : 'Error processing response data. Please try again, if the issue persists contact Vault Administrator',
        'ERROR_PROCESSING_RELOAD_PAGE' : 'Error processing. Please Reload the Page',
        'ERROR_AUTOCOMPLETE_USERNAME': 'Unable to retrieve the results. Please try again with full Username',
        'ERROR_AUTOCOMPLETE_GROUPNAME': 'Unable to retrieve the results. Please try again with full Group name',
        'ERROR_ADD_OWNER_FOR_PERMISSION':'Certificate owner cannot be added as a user to the certificate owned by him'
    } );
} )( angular.module( 'vault.constants.ErrorMessage', [] ) );
