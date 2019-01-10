// =========================================================================
// Copyright 2018 T-Mobile, US
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

package com.tmobile.cso.vault.api.controller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.tmobile.cso.vault.api.exception.TVaultValidationException;

@ControllerAdvice
public class ResponseExceptionHandler {

	 private static Logger log = LogManager.getLogger(ResponseExceptionHandler.class);
	 @ExceptionHandler(ServletRequestBindingException.class)
	 protected ResponseEntity<Object> handleException(ServletRequestBindingException ex, WebRequest request) {
	   	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+ ex.getMessage()+"\"]}");
	 }
	 
	 @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	 protected ResponseEntity<Object> handleException(HttpMediaTypeNotSupportedException ex, WebRequest request) {
	   	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+ ex.getMessage()+"\"]}");
	 }
	 
	 @ExceptionHandler(NoHandlerFoundException.class)
	 protected ResponseEntity<Object> handleException(NoHandlerFoundException ex, WebRequest request) {
	   	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+ ex.getMessage()+"\"]}");
	 }
	 
	 @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	 protected ResponseEntity<Object> handleException(HttpRequestMethodNotSupportedException ex, WebRequest request) {
	   	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+ ex.getMessage()+"\"]}");
	 }
	 
	 @ExceptionHandler(TVaultValidationException.class)
	 protected ResponseEntity<Object> handleException(TVaultValidationException ex, WebRequest request) {
		    log.debug(ex.getMessage());
		   	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+ ex.getMessage()+"\"]}");
		 }
	 @ExceptionHandler(Exception.class)
	 protected ResponseEntity<Object> handleException(Exception ex, WebRequest request) {
		 log.debug(ex.getMessage());
	   	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Unexpected error :"+ ex.getMessage()+"\"]}");
	 }
}