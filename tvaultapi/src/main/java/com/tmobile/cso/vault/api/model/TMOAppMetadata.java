// =========================================================================
// Copyright 2021 T-Mobile, US
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TMOAppMetadata implements Serializable {


	private static final long serialVersionUID = 452505456427580623L;
	
	private String path;

	@JsonProperty("data")
	private TMOAppMetadataDetails tmoAppMetadataDetails;

	public TMOAppMetadata() {
		super();
	}
	
	 public TMOAppMetadata(String path, TMOAppMetadataDetails tmoMetadataDetails) {
	        super();
	        this.path = path;
	        this.tmoAppMetadataDetails = tmoMetadataDetails;
	    }

	public TMOAppMetadata(TMOAppMetadataDetails tmoAppMetadataDetails) {
		this.tmoAppMetadataDetails = tmoAppMetadataDetails;
	}

	public TMOAppMetadataDetails getTmoAppMetadataDetails() {
		return tmoAppMetadataDetails;
	}

	public void setTmoAppMetadataDetails(TMOAppMetadataDetails tmoAppMetadataDetails) {
		this.tmoAppMetadataDetails = tmoAppMetadataDetails;
	}

	@Override
	public String toString() {
		return "TMOAppMetadata{" +
				"tmoAppMetadataDetails=" + tmoAppMetadataDetails +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TMOAppMetadata that = (TMOAppMetadata) o;
		return tmoAppMetadataDetails.equals(that.tmoAppMetadataDetails);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tmoAppMetadataDetails);
	}
}
