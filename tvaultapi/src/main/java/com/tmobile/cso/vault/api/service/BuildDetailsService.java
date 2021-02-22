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

package com.tmobile.cso.vault.api.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.model.BuildDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;

@Component
public class BuildDetailsService {

       private static Logger log = LogManager.getLogger(BuildDetailsService.class);	
    
    /**
     * To get build details.
     * @return
     */
    public ResponseEntity<BuildDetails> getBuildDetails(){
    	BuildDetails details = new BuildDetails();
    	try {
    	Resource resource = new ClassPathResource("classpath:build_variables.txt");
        InputStream stream = resource.getInputStream();  
       	 
         if (stream == null) {
             throw new IllegalArgumentException("File build_variables.txt not found! " );
         } else {
        	 BufferedReader bufRead = new BufferedReader(new InputStreamReader(stream));
        	    String line=null;
        	    while((line=bufRead.readLine())!=null){
					if (line.startsWith("version")) {
						String version = line.substring("version=".length(), line.length());
						log.debug("Successfully read version: from build details file");
						details.setVersion(version.substring(version.lastIndexOf("_")+1));
					}
					else if (line.startsWith("date")) {
						String date = line.substring("date=".length(), line.length());
						log.debug("Successfully read date: from build details file");
						details.setBuildDate(date);
					}					
				}
				bufRead.close();
			}
         }catch (Exception e) {
			log.error(String.format("Unable to read build details file: [%s]", e.getMessage()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(details);
		}
    	return ResponseEntity.status(HttpStatus.OK).body(details);
    }
}
