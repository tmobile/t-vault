/** *******************************************************************************
*  Copyright 2019 T-Mobile, US
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*  
*     http://www.apache.org/licenses/LICENSE-2.0
*  
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */

package com.tmobile.cso.vault.api.utils;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.CertificateUser;
import com.tmobile.cso.vault.api.model.SSLCertificateMetadataDetails;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.Response;

@Component
public class CertificateUtils {
	private Logger log = LogManager.getLogger(CertificateUtils.class);
			
	/**
	 * Checks whether a user can be added
	 * @param userDetails
	 * @param safeUser
	 * @return
	 */
	public boolean canAddOrRemoveUser(UserDetails userDetails, CertificateUser certificateUser, String action) {
		String token = userDetails.getSelfSupportToken();
		String certificateName = certificateUser.getCertificateName();
		
		if (StringUtils.isEmpty(certificateName)) {
			return false;
		}
		if (userDetails.isAdmin()) {
			token = userDetails.getClientToken();
		}
		SSLCertificateMetadataDetails certificateMetaData = getCertificateMetaData(token, certificateName);
		if (ObjectUtils.isEmpty(certificateMetaData)) {
			return false;
		}
		String certOwnerid = certificateMetaData.getCertCreatedBy();
		if (userDetails.isAdmin()) {
			
			return checkIfCertificateAdmin(certificateUser, action, certOwnerid);
		}
		else {
			// Prevent the owner of the certificate to be denied...
			return checkIfNonCertificateAdmin(userDetails, certificateUser, action, certOwnerid);
		}
	}

	/**
	 * @param userDetails
	 * @param certificateUser
	 * @param action
	 * @param certOwnerid
	 * @return
	 */
	private boolean checkIfNonCertificateAdmin(UserDetails userDetails, CertificateUser certificateUser, String action,
			String certOwnerid) {
		boolean hasAccess = true;
		if (userDetails.getUsername() != null && userDetails.getUsername().equalsIgnoreCase(certOwnerid)) {
			// This user is owner of the certificate...
			if (certificateUser.getUsername().equals(certOwnerid)) {
				if (TVaultConstants.READ_POLICY.equals(certificateUser.getAccess()) || TVaultConstants.WRITE_POLICY.equals(certificateUser.getAccess()) || (null==certificateUser.getAccess() && action.equals(TVaultConstants.REMOVE_USER))) {
					// certificate owner himself can set read/write permission to the certificate owner
					hasAccess = true;
				}else {
					hasAccess = false;
				}
			}
		}else {
			// other normal users will not have permission as they are not the owner
			hasAccess = false;
		}
		return hasAccess;
	}

	/**
	 * @param certificateUser
	 * @param action
	 * @param certOwnerid
	 * @return
	 */
	private boolean checkIfCertificateAdmin(CertificateUser certificateUser, String action, String certOwnerid) {
		boolean hasAccess = true;
		if (StringUtils.isEmpty(certOwnerid)) {
			// Null or empty user for owner
			// Existing certificate will not have ownerid
			// Certificate created by Certificateadmin will not have ownerid
			hasAccess = true;
		}
		else {
			// There is some owner assigned to the certificate
			if (certOwnerid.equals(certificateUser.getUsername())) {
				// Certificate admin is trying to add the owner of the certificate as some user with some permission
				// Certificate admin can add read or write permission to certificate owner
				if (TVaultConstants.READ_POLICY.equals(certificateUser.getAccess()) || TVaultConstants.WRITE_POLICY.equals(certificateUser.getAccess()) || (null==certificateUser.getAccess() && action.equals(TVaultConstants.REMOVE_USER))) {
					// Certificate admin or the certificate owner himself can set read/write permission to the certificate owner
					hasAccess = true;
				}else {
					hasAccess = false;
				}
			}
			else {
				// Certificate admin is trying to add a user, who is non-owner of the certificate with read/write/deny
				hasAccess = true;
			}
		}
		return hasAccess;
	}
	
	/**
	 * Gets the metadata associated with a given certificate, requires an token which can perform this operation
	 * or token which has certificate admin capabilities
	 * @param token
	 * @param certificateName
	 * @return certificateMetadataDetails
	 */
	public SSLCertificateMetadataDetails getCertificateMetaData(String token, String certificateName){
		String certificatePath = SSLCertificateConstants.SSL_CERT_PATH + '/' + certificateName;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "Get Certificate Info").
				put(LogMessage.MESSAGE, String.format ("Trying to get Info for [%s]", certificatePath)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		// Elevation is required in case user does not have access to the path.
		Response response = ControllerUtil.getReqProcessor().process("/certmanager","{\"path\":\""+certificatePath+"\"}",token);
		// Create the Safe bean
		SSLCertificateMetadataDetails certificateMetadataDetails = null;
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			try {
				ObjectMapper objMapper = new ObjectMapper();
				JsonNode dataNode = objMapper.readTree(response.getResponse()).get("data");
				certificateMetadataDetails = getCertificateInfo(dataNode);
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						  put(LogMessage.ACTION, "getCertificateMetaData").
					      put(LogMessage.MESSAGE, "Error while trying to get details about the certificate").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					      build()));			
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "Get Certificate metadata Info").
				put(LogMessage.MESSAGE, "Getting metaDataInfo completed").
				put(LogMessage.STATUS, response.getHttpstatus().toString()).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		return certificateMetadataDetails;
	}
	
	/**
	 * Prepare the SSLCertificateMetadataDetails object
	 * @param dataNode
	 * @return certificate
	 */
	public SSLCertificateMetadataDetails getCertificateInfo (JsonNode dataNode) {
		SSLCertificateMetadataDetails certificate = new SSLCertificateMetadataDetails();
		certificate.setCertificateName(dataNode.get("certificateName").asText());
		certificate.setCertificateId(dataNode.get("certificateId").asInt());
		if (null != dataNode.get("certType")) {
			certificate.setCertType(dataNode.get("certType").asText());
		}
		if (null != dataNode.get("certCreatedBy")) {
			certificate.setCertCreatedBy(dataNode.get("certCreatedBy").asText());
		}
		
		if (null != dataNode.get("certOwnerEmailId")) {
			certificate.setCertOwnerEmailId(dataNode.get("certOwnerEmailId").asText());
		}
		
		if (null != dataNode.get("expiryDate")) {
			certificate.setExpiryDate(dataNode.get("expiryDate").asText());
		}		
		
		return certificate;
	}	
	
}
