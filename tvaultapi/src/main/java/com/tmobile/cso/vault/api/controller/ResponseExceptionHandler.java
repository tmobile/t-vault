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

package com.tmobile.cso.vault.api.controller;

import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Objects;

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


	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<Object> handleException(MethodArgumentNotValidException ex, WebRequest request) {
		log.debug(ex.getMessage());
		StringBuilder errorMessage = new StringBuilder();
		if(Objects.nonNull(ex.getBindingResult())){
			errorMessage.append(ex.getBindingResult().getFieldErrors().get(0).getField());
			errorMessage.append(" ");
			errorMessage.append(ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
		} else {
			errorMessage.append("Invalid input values");
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"" + errorMessage+ "\"]}");
	}


	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected ResponseEntity<Object> handleException(HttpMessageNotReadableException ex, WebRequest request) {
		log.debug(ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
	}

	 @ExceptionHandler(Exception.class)
	 protected ResponseEntity<Object> handleException(Exception ex, WebRequest request) {
		 log.debug(ex.getMessage());
	   	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Unexpected error :"+ ex.getMessage()+"\"]}");
	 }


}