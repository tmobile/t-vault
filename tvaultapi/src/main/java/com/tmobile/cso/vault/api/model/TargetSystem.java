// =========================================================================
// Copyright 2020 T-Mobile, US
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

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import java.io.Serializable;

public class TargetSystem implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5517417657503144127L;
    @NotNull
    @NotEmpty
    private String name;
    private String description;
    @NotNull
    @NotEmpty
    @Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "Address can have alphabets, numbers, . and - characters only")
    private String address;


    public TargetSystem() {
    }

    public TargetSystem(int targetSystemID, String name, String description, String address) {
        super();
        this.name = name;
        this.description = description;
        this.address = address;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }


    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }


    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }


    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "TargetSystem{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
