// =========================================================================
// Copyright 2020 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License")
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

package com.tmobile.cso.vault.api.v2.controller;

import com.tmobile.cso.vault.api.model.OidcRequest;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.service.OidcAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@Api(description = "Manage OIDC Authentication", position = 19)
public class OidcAuthControllerV2 {

    @Autowired
    private OidcAuthService oidcAuthService;

    /**
     * Login to TVault
     * @returnC
     */
    @PostMapping(value="/v2/auth/oidc/auth_url",produces="application/json")
    @ApiOperation(value = "${OidcAuthControllerV2.getAuthUrl.value}", notes = "${OidcAuthControllerV2.getAuthUrl.notes}")
    public ResponseEntity<String> getAuthUrl(@RequestBody OidcRequest oidcRequest){
        return oidcAuthService.getAuthUrl(oidcRequest);
    }

    @GetMapping(value="/v2/auth/oidc/callback",produces="application/json")
    @ApiOperation(value = "${OidcAuthControllerV2.processCallback.value}", notes = "${OidcAuthControllerV2.processCallback.notes}")
    public ResponseEntity<String> processCallback(@RequestParam(name="state", required = false) String state, @RequestParam(name="code", required = false) String code){
        return oidcAuthService.processCallback(state, code);

    }
}