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
    app.constant( 'ListOfApi', {
        'api_details' : [
            {
                "title" : "Login LDAP",
                "url" : "auth/ldap/login",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"username":"<corp user id>","password":"<password>"}',
                "sampleResponse" : '{success: 200} {"client_token":"2e8a4d39-8bba-6067-9f33-98a632165a6f",”admin”:”yes”,"access":{"shared":[{"cso":"read"}],"users":[{"achandr13":"write"}],"apps":[{"app1":"read"},{"app3":"write"}]},"policies":["default","r_apps_app1","r_shared_cso","w_apps_app3","w_users_achandr13"]}',
                "note" : 'Use the client_token returned should be used as “vault-token” in request header for all other API calls. If user is an administrator, “admin”:”yes” will be populated'
            },
            {
                "title" : "AWS Role Configure",
                "url" : "/auth/aws/roles/update",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"role":"testawsrole","policies":"","bound_ami_id":"","bound_account_id":"ddd","bound_region":"","bound_vpc_id":"","bound_subnet_id":"","bound_iam_instance_profile_arn":"","bound_iam_role_arn":""}',
                "sampleResponse" : '{success: 200} {"Messages":["AWS Role created "]}}',
                "note" : 'At least one bound conditions is mandatory'
            },
            {
                "title" : "Fetch AWS Role Info",
                "url" : "auth/aws/roles/<rolename>",
                "methodType" : "GET",
                "description" : " ",
                "sampleRequest" : '',
                "sampleResponse" : '{success: 200} {"bound_ami_id":"testami","bound_region":"","bound_subnet_id":"","policies":["default"],"bound_account_id":"","bound_iam_instance_profile_arn":"","bound_iam_role_arn":"","bound_vpc_id":""}'
            },
            {
                "title" : "Create Safe",
                "url" : "/sdb/create",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app5","data":{"owner":"cso","createdby":"anil"}}',
                "sampleResponse" : '{success: 200} {"Message":"SDB and associated read/write/deny policies created "}',
                "note" : 'You can add any key value pairs in side data.'
            },
            {
                "title" : "Get Safe Information",
                "url" : "/sdb?path=<path to safe>",
                "methodType" : "GET",
                "description" : " ",
                "sampleRequest" : '/sdb?path=apps/app3',
                "sampleResponse" : '{success: 200} {"data":{"createdby":"anil","owner":"cso","aws-roles":{“role1”:”read”,”role2”:”write”}, "groups":{"r_win_opr_dev_rdp":"write"},"users":{"achandr1":"deny","achandr13":"deny"}}}'
            },
            {
                "title" : "List safes",
                "url" : "/sdb/list?path=<path name> // path would be apps/shared/users",
                "methodType" : "GET",
                "description" : " ",
                "sampleRequest" : '/sdb/list?path=apps',
                "sampleResponse" : '{success: 200, Incorrect path : 403}, {"keys":["app1","app2”,"app3","app5"]}',
                "note" : 'The same api could be used to list the folders under a safe. The path needs to be provided accordingly.'
            },
            {
                "title" : "Creating Folder under a safe",
                "url" : "/sdb/createfolder?path=<path with folder name>",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '/sdb/createfolder?path=apps/app1',
                "sampleResponse" : '{success: 200} {"messages":["Folder created “]}',
                "note" : 'When a new folder is created an entry “default”:”default” will be added. UI needs to ignore “default”:”default”  returned while showing the secrets under a folder.'
            },
            {
                "title" : "Deleting a Safe/Folder",
                "url" : "/sdb/delete?path=<full path>",
                "methodType" : "DELETE",
                "description" : " ",
                "sampleRequest" : '/sdb/delete?path=apps/app1/secrets',
                "sampleResponse" : '{ success: 200 } {"messages":["SDB deleted"]}'
            },
            {
                "title" : "Associating user to safe",
                "url" : "/sdb/adduser",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app1","username":"achandr13","access":"read"}',
                "sampleResponse" : '{success: 200} {"Message":"User is successfully associated "}'
            },
            {
                "title" : "Associating Group to safe",
                "url" : "/sdb/addgroup",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app1","groupname":"vault-admin","access":"write"}',
                "sampleResponse" : '{success: 200} {"Message":"Group is successfully associated with SDB"}'
            },
            {
                "title" : "Associating Aws Role to safe",
                "url" : "/sdb/addrole",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app1"," role":"aws-sample-role","access":"write"}',
                "sampleResponse" : '{success: 200} {"Message":"Role is successfully associated "}'
            },
            {
                "title" : "Removing user association to safe",
                "url" : "/sdb/deleteuser",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app1","username":"achandr13"}',
                "sampleResponse" : '{success: 200} {"Message":"User association is removed "}'
            },
            {
                "title" : "Removing Group association to safe",
                "url" : "/sdb/deletegroup",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app1","groupname":"vault-admin"}',
                "sampleResponse" : '{success: 200} {"Message":"Group association is removed "}'
            },
            {
                "title" : "Removing AWS-Role association to safe",
                "url" : "/sdb/deleterole",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app1","role":"1_a04f33f6-74ad-b7ad"}',
                "sampleResponse" : '{success: 200} {"Message":"Role association is removed "}'
            },
            {
                "title" : "Read Secret",
                "url" : "read?path=<Path>",
                "methodType" : "GET",
                "description" : " ",
                "sampleRequest" : '/read?path=apps/app1/secrets',
                "sampleResponse" : '{success: 200} {"data":{"key1":"value1","key2":"value2"}}'
            },
            {
                "title" : "Write Secret",
                "url" : "/write",
                "methodType" : "POST",
                "description" : " ",
                "sampleRequest" : '{"path":"apps/app1/secrets","data": {"key1":"value1","key2":"value2"}}',
                "sampleResponse" : '{success: 200} {"Message":"Secret saved to vault"}'
            }
        ]
    } );
} )( angular.module( 'vault.constants.ListOfApi', [] ) );
