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

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class CertificateDownloadRequest implements Serializable {

    private static final long serialVersionUID = 3599498297619361438L;
    private String certificateName;
    private String certificateCred;
    private String format;


    public CertificateDownloadRequest() {

    }

    public CertificateDownloadRequest(String certificateName, String certificateCred, String format) {
        this.certificateName = certificateName;
        this.certificateCred = certificateCred;
        this.format = format;
    }

    public String getCertificateCred() {
        return certificateCred;
    }

    public void setCertificateCred(String certificateCred) {
        this.certificateCred = certificateCred;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }
} 