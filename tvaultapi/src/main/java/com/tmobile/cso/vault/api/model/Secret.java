// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License");
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

package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Sarath
 *
 */
public class Secret implements Serializable {

    private static final long serialVersionUID = 5801186298788991628L;
    private String path;
    @JsonProperty("data")
    private HashMap<String, String> details;

    public Secret() { super(); }

    /**
     *
     * @param path
     * @param details
     */
    public Secret(String path, HashMap<String, String> details) {
        super();
        this.path = path;
        this.details = details;
    }

    /**
     *
     * @return path
     */
    @ApiModelProperty(example="shared/mysafe01", position=1)
    public String getPath() {
        return path;
    }

    /**
     *
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     *
     * @return details
     */
    @ApiModelProperty(example="{\r\n" +
            "     \"secret1\":\"value1\",\r\n" +
            "    \"secret2\":\"value2\"\r\n" +
            "  }", position=2, required=true)
    public HashMap<String, String> getDetails() {
        return details;
    }

    /**
     *
     * @param details the details to set
     */
    public void setDetails(HashMap<String, String> details) {
        this.details = details;
    }
}
