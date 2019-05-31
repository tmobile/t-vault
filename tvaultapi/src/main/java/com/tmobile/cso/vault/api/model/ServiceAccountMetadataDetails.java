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

import java.io.Serializable;

public class ServiceAccountMetadataDetails implements Serializable {


    /**
	 * 
	 */
	private static final long serialVersionUID = -3693598577680701656L;

	private String name;

    private String managedBy;

    private boolean initialPasswordReset;

    /**
     *
     */
    public ServiceAccountMetadataDetails() {
        super();
    }
    /**
     *
     * @param name
     */
    public ServiceAccountMetadataDetails(String name) {
        super();
        this.name = name;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the managedBy
     */
    public String getManagedBy() {
        return managedBy;
    }
    /**
     * @param managedBy the managedBy to set
     */
    public void setManagedBy(String managedBy) {
        this.managedBy = managedBy;
    }
    /**
     *
     * @return the initialPasswordReset
     */
    public boolean getInitialPasswordReset() {
        return initialPasswordReset;
    }
    /**
     *
     * @param initialPasswordReset the initialPasswordReset to set
     */
    public void setInitialPasswordReset(boolean initialPasswordReset) {
        this.initialPasswordReset = initialPasswordReset;
    }
}
