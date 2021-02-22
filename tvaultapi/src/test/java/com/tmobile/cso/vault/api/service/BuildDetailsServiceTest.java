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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;

import com.tmobile.cso.vault.api.model.BuildDetails;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = { "com.tmobile.cso.vault.api" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ BuildDetailsService.class})
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class BuildDetailsServiceTest {

    @InjectMocks
    BuildDetailsService buildDetailsService;
  
   
    @Test
    public void getBuildDetails() throws Exception {
    	File buildFile = getBuildFile();
    	BuildDetails expected = new BuildDetails();
    	expected.setVersion("1.3.6");
    	expected.setBuildDate("02-20-2021");
        ResponseEntity<BuildDetails> response = buildDetailsService.getBuildDetails();
        BuildDetails actual = response.getBody();
        assertNotNull(actual);
    }
    
    private File getBuildFile() throws IOException {
    	TemporaryFolder folder= new TemporaryFolder();
    	folder.create();
    	File buildFile = folder.newFile("build");
    	PrintWriter pw =  new PrintWriter(buildFile);
    	pw.write("version:1.3.6"+ System.getProperty("line.separator") + "date:02-20-2021");
    	pw.close();
    	return buildFile;
    }

   
}
