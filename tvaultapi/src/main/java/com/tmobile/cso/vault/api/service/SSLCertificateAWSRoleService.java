/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
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

package com.tmobile.cso.vault.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.CertificateAWSRole;
import com.tmobile.cso.vault.api.model.CertificateAWSRoleRequest;
import com.tmobile.cso.vault.api.model.SSLCertificateMetadataDetails;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.CertificateUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

@Component
public class SSLCertificateAWSRoleService {

	@Autowired
	private RequestProcessor reqProcessor;

	@Autowired
	private CertificateUtils certificateUtils;

	@Autowired
	private AWSAuthService awsAuthService;

	@Autowired
	private AWSIAMAuthService awsiamAuthService;

	@Autowired
	private TokenUtils tokenUtils;

	@Value("${SSLCertificateController.certificatename.text}")
	private String certificateNameTailText;

	private static Logger log = LogManager.getLogger(SSLCertificateAWSRoleService.class);

	private static final String[] PERMISSIONS = { "read", "write", "deny", "sudo" };

	/**
	 * To create AWS ec2 role for SSL
	 *
	 * @param userDetails
	 * @param token
	 * @param awsLoginRole
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createAWSRoleForSSL(UserDetails userDetails, String token, AWSLoginRole awsLoginRole)
			throws TVaultValidationException {
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		return awsAuthService.createRole(token, awsLoginRole, userDetails);
	}

	/**
	 * Create AWS IAM role for SSL
	 *
	 * @param userDetails
	 * @param token
	 * @param awsiamRole
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createIAMRoleForSSL(UserDetails userDetails, String token, AWSIAMRole awsiamRole)
			throws TVaultValidationException {
		if (!userDetails.isAdmin()) {
			token = tokenUtils.getSelfServiceToken();
		}
		return awsiamAuthService.createIAMRole(awsiamRole, token, userDetails);
	}

	/**
	 * Add AWS role to SSL Certificate
	 *
	 * @param userDetails
	 * @param token
	 * @param CertificateAWSRole
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToSSLCertificate(UserDetails userDetails, String token,
			CertificateAWSRole certificateAWSRole) {
		if (!validateAWSRoleInputs(certificateAWSRole)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Trying to add AWS Role [%s] to SSL Certificate [%s]",
								certificateAWSRole.getRolename(), certificateAWSRole.getCertificateName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String roleName = certificateAWSRole.getRolename();
		String certificateName = certificateAWSRole.getCertificateName();
		String access = certificateAWSRole.getAccess();
		String certificateType = certificateAWSRole.getCertType();

		boolean isAuthorized = true;
		if (!ObjectUtils.isEmpty(userDetails)) {
			if (!userDetails.isAdmin()) {
				token = tokenUtils.getSelfServiceToken();
			}

			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(token,
					certificateName, certificateType);

			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE, "Access denied: No permission to add AWS role to this certificate")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to add AWS Role to this certificate\"]}");
		}

		if (isAuthorized) {
			return processAWSRolePoliciesAndAddToCertificate(token, roleName, certificateName, access, certificateType);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE, "Access denied: No permission to add AWS Role to this certificate")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to add AWS Role to this certificate\"]}");
		}

	}

	/**
	 * Method to check the AWS role and add the permission to certificate
	 *
	 * @param token
	 * @param roleName
	 * @param certificateName
	 * @param access
	 * @param certificateType
	 * @return
	 */
	private ResponseEntity<String> processAWSRolePoliciesAndAddToCertificate(String token, String roleName,
			String certificateName, String access, String certificateType) {
		String certPrefix = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))
				? SSLCertificateConstants.INTERNAL_POLICY_NAME
				: SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		String policy = new StringBuffer().append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(access))
				.append(certPrefix).append("_").append(certificateName).toString();

		String metaDataPath = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))
				? SSLCertificateConstants.SSL_CERT_PATH_VALUE
				: SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		String certificatePath = metaDataPath + certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
				.put(LogMessage.MESSAGE, String.format("Policy is [%s]", policy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		String readPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();
		String writePolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();
		String denyPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();
		String ownerPolicy = new StringBuffer()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy,
								writePolicy, denyPolicy, ownerPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		Response roleResponse = reqProcessor.process("/auth/aws/roles", "{\"role\":\"" + roleName + "\"}", token);
		String responseJson = "";
		String authType = TVaultConstants.EC2;
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		String policiesString = "";
		String currentpoliciesString = "";

		if (HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
			responseJson = roleResponse.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode policiesArry = objMapper.readTree(responseJson).get("policies");
				for (JsonNode policyNode : policiesArry) {
					currentpolicies.add(policyNode.asText());
				}
				authType = objMapper.readTree(responseJson).get("auth_type").asText();
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
						.put(LogMessage.MESSAGE, e.getMessage())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
			policies.add(policy);
			policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE,
							String.format("AWS role [%s] does not exist. Please create the role and try again",
									roleName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either AWS role doesn't exist or you don't have enough permission to add this AWS role to Certificate\"]}");
		}

		Response awsRoleConfigresponse = null;
		// Call the AWS role configuration method based on the authType
		awsRoleConfigresponse = configureAWSRoleByAuthType(token, roleName, authType, policiesString);

		if (awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
			Map<String, String> params = new HashMap<>();
			params.put("type", "aws-roles");
			params.put("name", roleName);
			params.put("path", certificatePath);
			params.put("access", access);
			Response metadataResponse = ControllerUtil.updateMetadata(params, token);
			if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
					|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
						.put(LogMessage.MESSAGE,
								String.format("AWS Role [%s] successfully associated with SSL Certificate [%s] ",
										roleName, certificateName))
						.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"AWS Role successfully associated with SSL Certificate\"]}");
			}

			return revertAWSRoleConfigurationForCertificate(token, roleName, authType, currentpoliciesString,
					metadataResponse, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG);

		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE, String.format("Role [%s] configuration failed. Try Again", roleName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Role configuration failed. Try Again\"]}");
		}
	}

	/**
	 * Method to configure the certificate access policy to AWS Role based on the auth type
	 * @param token
	 * @param roleName
	 * @param authType
	 * @param policiesString
	 * @return
	 */
	private Response configureAWSRoleByAuthType(String token, String roleName, String authType, String policiesString) {
		Response awsRoleConfigresponse;
		if (TVaultConstants.IAM.equals(authType)) {
			awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(roleName, policiesString, token);
		}
		else {
			awsRoleConfigresponse = awsAuthService.configureAWSRole(roleName, policiesString, token);
		}
		return awsRoleConfigresponse;
	}

	/**
	 * Remove AWS Role from SSL Certificate
	 *
	 * @param userDetails
	 * @param token
	 * @param certificateAWSRoleRequest
	 * @return
	 */
	public ResponseEntity<String> removeAWSRoleFromSSLCertificate(UserDetails userDetails, String token,
			CertificateAWSRoleRequest certificateAWSRoleRequest) {
		if (!validateRemoveAWSRoleInputs(certificateAWSRoleRequest)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG)
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG)
				.put(LogMessage.MESSAGE, String.format("Trying to remove AWS Role [%s] from SSL Certificate [%s]",
						certificateAWSRoleRequest.getRolename(), certificateAWSRoleRequest.getCertificateName()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		String roleName = certificateAWSRoleRequest.getRolename();
		String certificateName = certificateAWSRoleRequest.getCertificateName();
		String certificateType = certificateAWSRoleRequest.getCertType();

		boolean isAuthorized = true;
		if (!ObjectUtils.isEmpty(userDetails)) {
			if (!userDetails.isAdmin()) {
				token = tokenUtils.getSelfServiceToken();
			}

			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(token,
					certificateName, certificateType);

			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG)
					.put(LogMessage.MESSAGE, "Access denied: No permission to remove AWS role from this certificate")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to remove AWS role from this certificate\"]}");
		}

		if (isAuthorized) {
			return processAWSRolePoliciesAndRemoveFromCertificate(token, roleName, certificateName, certificateType);
		} else {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
							.put(LogMessage.MESSAGE, String.format(
									"Access denied: No permission to remove AWS Role [%s] from SSL Certificate [%s]",
									roleName, certificateName))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to remove AWS Role from SSL Certificate\"]}");
		}
	}

	/**
	 * Method to check the AWS Role and delete the permission from certificate
	 *
	 * @param token
	 * @param roleName
	 * @param certificateName
	 * @param certificateType
	 * @return
	 */
	private ResponseEntity<String> processAWSRolePoliciesAndRemoveFromCertificate(String token, String roleName,
			String certificateName, String certificateType) {
		String certPrefix = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))
				? SSLCertificateConstants.INTERNAL_POLICY_NAME
				: SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		String metaDataPath = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))
				? SSLCertificateConstants.SSL_CERT_PATH_VALUE
				: SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		String certificatePath = new StringBuilder().append(metaDataPath).append(certificateName).toString();
		String readPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();
		String writePolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();
		String denyPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();
		String ownerPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(certPrefix).append("_").append(certificateName).toString();

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy,
								writePolicy, denyPolicy, ownerPolicy))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		Response roleResponse = reqProcessor.process("/auth/aws/roles", "{\"role\":\"" + roleName + "\"}", token);
		String responseJson = "";
		String authType = TVaultConstants.EC2;
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if (HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
			responseJson = roleResponse.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode policiesArry = objMapper.readTree(responseJson).get("policies");
				for (JsonNode policyNode : policiesArry) {
					currentpolicies.add(policyNode.asText());
				}
				authType = objMapper.readTree(responseJson).get("auth_type").asText();
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG)
						.put(LogMessage.MESSAGE, e.getMessage())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE, String.format("AWS role [%s] does not exist", roleName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
					.body("{\"errors\":[\"Either AWS role doesn't exist or you don't have enough permission to remove this AWS role from Certificate\"]}");
		}

		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG)
				.put(LogMessage.MESSAGE,
						"Remove AWS Role from Service account -  policy :" + policiesString + " is being configured")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response awsRoleConfigresponse = null;

		// Call the AWS role configuration method based on the authType
		awsRoleConfigresponse = configureAWSRoleByAuthType(token, roleName, authType, policiesString);

		if (awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
				|| awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
			Map<String, String> params = new HashMap<>();
			params.put("type", "aws-roles");
			params.put("name", roleName);
			params.put("path", certificatePath);
			params.put("access", "delete");
			Response metadataResponse = ControllerUtil.updateMetadata(params, token);

			if (metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())
					|| HttpStatus.OK.equals(metadataResponse.getHttpstatus()))) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG)
						.put(LogMessage.MESSAGE,
								String.format("AWS Role [%s] is successfully removed from SSL Certificate [%s]",
										roleName, certificateName))
						.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"AWS Role is successfully removed from SSL Certificate\"]}");
			}

			return revertAWSRoleConfigurationForCertificate(token, roleName, authType, currentpoliciesString,
					metadataResponse, SSLCertificateConstants.REMOVE_AWS_ROLE_FROM_CERT_MSG);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ADD_AWS_ROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE,
							String.format("Failed to remove AWS Role [%s] from SSL Certificate [%s]", roleName,
									certificateName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Failed to remove AWS Role from the SSL Certificate\"]}");
		}
	}

	/**
	 * Method to revert the AWS Role configuration if metadata update failed
	 *
	 * @param token
	 * @param roleName
	 * @param authType
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @param actionMessage
	 * @return
	 */
	private ResponseEntity<String> revertAWSRoleConfigurationForCertificate(String token, String roleName,
			String authType, String currentpoliciesString, Response metadataResponse, String actionMessage) {
		Response awsRoleConfigresponse = null;
		awsRoleConfigresponse = configureAWSRoleByAuthType(token, roleName, authType, currentpoliciesString);
		if (awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, actionMessage)
					.put(LogMessage.MESSAGE, String.format("Reverting, AWS Role [%s] policy update success", roleName))
					.put(LogMessage.RESPONSE,
							(null != metadataResponse) ? metadataResponse.getResponse() : TVaultConstants.EMPTY)
					.put(LogMessage.STATUS,
							(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
									: TVaultConstants.EMPTY)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, actionMessage)
					.put(LogMessage.MESSAGE, String.format("Reverting AWS Role [%s] policy update failed", roleName))
					.put(LogMessage.RESPONSE,
							(null != metadataResponse) ? metadataResponse.getResponse() : TVaultConstants.EMPTY)
					.put(LogMessage.STATUS,
							(null != metadataResponse) ? metadataResponse.getHttpstatus().toString()
									: TVaultConstants.EMPTY)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"AWS Role configuration failed. Contact Admin \"]}");
		}
	}

	/**
	 * Validates Certificate AWSRole inputs
	 *
	 * @param certificateAWSRole
	 * @return boolean
	 */
	private boolean validateAWSRoleInputs(CertificateAWSRole certificateAWSRole) {
		if (ObjectUtils.isEmpty(certificateAWSRole)) {
			return false;
		}
		if ((!certificateAWSRole.getCertificateName().endsWith(certificateNameTailText))
				|| (certificateAWSRole.getCertificateName().contains(".-"))
				|| (certificateAWSRole.getCertificateName().contains("-."))
				|| (!certificateAWSRole.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))) {
			return false;
		}
		boolean isValid = true;
		String access = certificateAWSRole.getAccess();
		if (!ArrayUtils.contains(PERMISSIONS, access)) {
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Validates Remove AWSRole From Certificate inputs
	 *
	 * @param certificateAWSRoleRequest
	 * @return boolean
	 */
	private boolean validateRemoveAWSRoleInputs(CertificateAWSRoleRequest certificateAWSRoleRequest) {
		boolean isValid = true;
		if (ObjectUtils.isEmpty(certificateAWSRoleRequest)) {
			return false;
		}
		if ((!certificateAWSRoleRequest.getCertificateName().endsWith(certificateNameTailText))
				|| (certificateAWSRoleRequest.getCertificateName().contains(".-"))
				|| (certificateAWSRoleRequest.getCertificateName().contains("-."))
				|| (!certificateAWSRoleRequest.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))) {
			isValid = false;
		}
		return isValid;
	}
}
