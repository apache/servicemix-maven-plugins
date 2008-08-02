/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.maven.plugin.checksum;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Adds all the checksums of the artifact dependencies from that are dependencies
 * of the current build to a checksum.txt file.
 * 
 * Once generated, the build can validate the checksums of the artifacts against that
 * checksums.txt 
 * 
 * @goal generate-checksums
 * @phase compile 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a> 
 */
public class ChecksumGeneratorMojo extends ChecksumValidatorMojo {

    public void execute() throws MojoExecutionException {
        
        LinkedHashMap checksums = new LinkedHashMap();
        
        boolean modified=true;
        try { 
            checksums = getCheckSums();
            modified=false;
        } catch ( MojoExecutionException e) {
        }
        
        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            Artifact pom = getPomArtifact( artifact );

            modified |= processArtifact(checksums, pom);
            modified |= processArtifact(checksums, artifact);
            
        }
        
        if( modified ) {
            
            // put it back to property file format
            Properties p = new Properties();

            for (Iterator iterator = checksums.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry i = (Map.Entry)iterator.next();            
                StringBuffer b = new StringBuffer();
                for (Iterator iterator2 = ((List)i.getValue()).iterator(); iterator.hasNext();) {
                    String s = (String)iterator2.next();            
                    if( b.length()!=0 ) {
                        b.append("|");
                    }
                    b.append(s);
                }
                p.put(i.getKey(), b.toString());
            }
            
            // Store it.
            FileOutputStream os=null;
            try {
                os = new FileOutputStream(this.checksums);
                p.store(os, "");
            } catch (Throwable e) {
                throw new MojoExecutionException("Could not write: "+this.checksums);
            } finally {
                try {
                    os.close();
                } catch (Throwable ignore ) {
                }
            }
        }
    }

    /**
     * 
     * @param checksums
     * @param pom
     * @return true if this method modified the checksums
     * @throws MojoExecutionException
     */
    private boolean processArtifact(HashMap checksums, Artifact pom) throws MojoExecutionException {
        String sum = checksum(pom.getFile());
        List sums = (List)checksums.get(pom.getId());
        if( sums == null ) {
            sums = new ArrayList();
            sums.add(sum);
            checksums.put(pom.getId(), sums);
            return true;
        } else {
            if ( !sums.contains(sum) ) {
                sums.add(sum);
                return true;
            }
        }
        return false;
    }


}
