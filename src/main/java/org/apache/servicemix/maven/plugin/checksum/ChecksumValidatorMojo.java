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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Validates the checksums of the dependencies of the project
 * against the checksums.txt file.
 * 
 * @goal validate-checksums
 * @phase process-classes 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a> 
 */
public class ChecksumValidatorMojo extends AbstractMojo {

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Remote repositories which will be searched for source attachments.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    protected List remoteArtifactRepositories;

    /**
     * Local maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * Artifact factory, needed to download source jars for inclusion in classpath.
     *
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     * @required
     * @readonly
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;
    
    
    /**
     * The file that holds dependency checksums.
     * 
     * @parameter default-value="${basedir}/checksums.txt"
     */
    protected File checksums;

    /**
     * The checksum algorithm used to in the checksums. 
     * 
     * @parameter default-value="SHA-1"
     */
    private String checksumAlgorithm;

    public void execute() throws MojoExecutionException {
        
        LinkedHashMap checksums = getCheckSums();
        
        boolean failed = false;
        
        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            Artifact pom = getPomArtifact( artifact );
            
            
            String sum = checksum(pom.getFile());
            String key = key(pom);
            List list = (List)checksums.get(key);
            if( list == null ) {
                list = (List)checksums.get(keyAnyVersion(pom));
            }
            if( list == null ) {
                getLog().error("No checksum specified for "+key+" in "+this.checksums+" ("+sum+")" );
                failed = true;
            } else if ( !list.contains(sum) && !list.contains("*") ) {
                getLog().error("Checksum mismatch for "+key+" in "+this.checksums+" expected one of "+list+" but was "+sum );
                failed = true;
            }
        }
        
        if( failed ) {
            throw new MojoExecutionException("Invalid checksum(s) found.. see previous error messages for more details.");
        }
    }

    protected String key(Artifact pom) {
        StringBuffer sb = new StringBuffer();
        sb.append(pom.getGroupId());
        sb.append("/");
        sb.append(pom.getArtifactId());
        sb.append("-");
        sb.append(pom.getVersion());
        sb.append(".");
        sb.append(pom.getType());
        return sb.toString();
    }
    
    protected String keyAnyVersion(Artifact pom) {
        StringBuffer sb = new StringBuffer();
        sb.append(pom.getGroupId());
        sb.append("/");
        sb.append(pom.getArtifactId());
        sb.append("-");
        sb.append("*");
        sb.append(".");
        sb.append(pom.getType());
        return sb.toString();
    }

    protected LinkedHashMap getCheckSums() throws MojoExecutionException {
        LinkedHashMap rc = new LinkedHashMap();
        
        if( !checksums.canRead() ) {
            throw new MojoExecutionException("Cannot read checksum file: "+checksums);
        }
        
        Properties sums = new Properties();
        FileInputStream is=null;
        try {
            is = new FileInputStream(checksums);
            sums.load(is);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not load checksum file: "+checksums);
        } finally {
            try {
                is.close();
            } catch (Throwable e) {
            }
        }
        
        for (Iterator iterator = sums.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry i = (Map.Entry)iterator.next();            
            String value = (String)i.getValue();
            String[] t = value.split("\\|");
            List values = new ArrayList(t.length);
            for (int j = 0; j < t.length; j++) {
                values.add(t[j].toLowerCase().trim());
            }
            rc.put((String)i.getKey(), values);
        }
        
        return rc;
    }
    
    protected Artifact getPomArtifact(Artifact artifact) {
        Artifact resolvedArtifact = artifactFactory.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        try {
            artifactResolver.resolve(resolvedArtifact, remoteArtifactRepositories, localRepository);
        } catch (ArtifactNotFoundException e) {
            // ignore, the jar has not been found
        } catch (ArtifactResolutionException e) {
            getLog().warn("Could not get pom for " + artifact);
        }
        if (resolvedArtifact.isResolved()) {
            return resolvedArtifact;
        }
        return null;
    }

    String checksum(File file) throws MojoExecutionException {
        try {
            MessageDigest md = MessageDigest.getInstance(checksumAlgorithm);
            FileInputStream is=null;
            try {
                is = new FileInputStream(file);
                byte buffer[] = new byte[1024*4];
                int c;
                while( (c=is.read(buffer)) >= 0 ) {
                    md.update(buffer,0, c);
                }
                byte[] digest = md.digest();
                
                return toString(digest);
                
            } catch (IOException e) {
                throw new MojoExecutionException("Could read file: "+checksums);
            } finally {
                try {
                    is.close();
                } catch (Throwable e) {
                }
            }

        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException("Invalid checksum algorithm: "+checksumAlgorithm, e);
        }
    }

    static char hexTable[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    
    static String toString(byte[] digest) {
        StringBuilder rc = new StringBuilder(digest.length*2);
        for (int i = 0; i < digest.length; i++) {
            rc.append( hexTable[ ((digest[i]>>4) & 0x0F) ] ) ;
            rc.append( hexTable[ (digest[i] & 0x0F) ] ) ;
        }
        return rc.toString();
    }

}
