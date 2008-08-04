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

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Validates the checksums of the dependencies of the project
 * against the checksums.txt file.
 * 
 * This plugin can also be used to add all the checksums of the 
 * dependencies of the current build to the checksum.txt file.
 * 
 * @requiresDependencyResolution
 * @goal validate
 * @phase validate 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a> 
 */
public class ChecksumValidatorMojo extends AbstractMojo {

    static char hexTable[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

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

    /**
     * Should we generate the checksum file instead of validating against it? 
     * 
     * @parameter default-value="false"
     */
    private boolean generate;

    /**
     * Should the dependency artifacts be included in the checksum validation? 
     * 
     * @parameter default-value="true"
     */
    private boolean includeDependencyArtifacts;

    /**
     * Should the plugin artifacts be included in the checksum validation? 
     * 
     * @parameter default-value="true"
     */
    private boolean includePluginArtifacts;

    /**
     * Should the report artifacts be included in the checksum validation? 
     * 
     * @parameter default-value="true"
     */
    private boolean includeReportArtifacts;

    protected String key(Artifact pom) {
        StringBuffer sb = new StringBuffer();
        sb.append(pom.getGroupId());
        sb.append("/");
        sb.append(pom.getArtifactId());
        sb.append("/");
        sb.append(pom.getType());
        sb.append("/");
        sb.append(pom.getVersion());
        return sb.toString();
    }
    
    protected String keyAnyVersion(Artifact pom) {
        StringBuffer sb = new StringBuffer();
        sb.append(pom.getGroupId());
        sb.append("/");
        sb.append(pom.getArtifactId());
        sb.append("/");
        sb.append(pom.getType());
        sb.append("/");
        sb.append("*");
        return sb.toString();
    }
    
    protected Artifact getPomArtifact(Artifact artifact) {
        return artifactFactory.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    protected File resolve(Artifact artifact) throws MojoExecutionException {
        if( !artifact.isResolved() ) {
            try {
                artifactResolver.resolve(artifact, remoteArtifactRepositories, localRepository);
            } catch (Throwable e) {
                throw new MojoExecutionException("Could not resolve the artifact for " + artifact+": "+e.getMessage(), e);
            }
        }
        return artifact.getFile();
    }

    protected String checksum(File file) throws MojoExecutionException {
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
    
    static String toString(byte[] digest) {
        StringBuilder rc = new StringBuilder(digest.length*2);
        for (int i = 0; i < digest.length; i++) {
            rc.append( hexTable[ ((digest[i]>>4) & 0x0F) ] ) ;
            rc.append( hexTable[ (digest[i] & 0x0F) ] ) ;
        }
        return rc.toString();
    }

    public void execute() throws MojoExecutionException {
        if( generate ) {
            generate();
        } else {
            validate();
        }
    }

    private void validate() throws MojoExecutionException {
        LinkedHashMap checksums = loadChecksums();
        
        boolean failed = false;
        
        for ( Iterator it = getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            Artifact pom = getPomArtifact( artifact );
            failed |= validateArtifact(checksums, pom);
            failed |= validateArtifact(checksums, artifact);
        }
        
        if( failed ) {
            throw new MojoExecutionException("Invalid checksum(s) found.. see previous error messages for more details.");
        }
    }

    /**
     * 
     * @param checksums
     * @param artifact
     * @return - true if validation failed.
     * @throws MojoExecutionException
     */
    private boolean validateArtifact(LinkedHashMap checksums, Artifact artifact) throws MojoExecutionException {
        File file = resolve(artifact);        
        String sum = checksum(file);
        String key = key(artifact);
        List list = (List)checksums.get(key);
        if( list == null ) {
            list = (List)checksums.get(keyAnyVersion(artifact));
        }
        if( list == null ) {
            getLog().error("No checksum specified for "+key+" in "+this.checksums+" ("+sum+")" );
            return true;
        } else if ( !list.contains(sum) && !list.contains("*") ) {
            getLog().error("Checksum mismatch for "+key+" in "+this.checksums+" expected one of "+list+" but was "+sum );
            return true;
        }
        return false;
    }

    public void generate() throws MojoExecutionException {
        
        LinkedHashMap checksums = new LinkedHashMap();
        
        boolean modified=true;
        try { 
            checksums = loadChecksums();
            modified=false;
        } catch ( MojoExecutionException e) {
        }
        
        
        for ( Iterator it = getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            Artifact pom = getPomArtifact( artifact );

            modified |= generateArtifact(checksums, pom);
            modified |= generateArtifact(checksums, artifact);
            
        }
        
        if( modified ) {
            storeChecksums(checksums);
        }
    }

    private Set getArtifacts() {
        HashSet rc = new HashSet();
        if( includeDependencyArtifacts ) { 
            rc.addAll(project.getDependencyArtifacts());
        }
        if( includePluginArtifacts ) { 
            rc.addAll(project.getPluginArtifacts());
        }
        if( includeReportArtifacts ) { 
            rc.addAll(project.getReportArtifacts());
        }
        return rc;
    }

    /**
     * 
     * @param checksums
     * @param artifact
     * @return true if this method modified the checksums
     * @throws MojoExecutionException
     */
    private boolean generateArtifact(HashMap checksums, Artifact artifact) throws MojoExecutionException {
        File file = resolve(artifact);
        String sum = checksum(file);
        List sums = (List)checksums.get(key(artifact));
        if( sums == null ) {
            sums = (List)checksums.get(keyAnyVersion(artifact));
        }
        if( sums == null ) {
            sums = new ArrayList();
            sums.add(sum);
            checksums.put(key(artifact), sums);
            return true;
        } else {
            if ( !sums.contains(sum) && !sums.contains("*") ) {
                sums.add(sum);
                return true;
            }
        }
        return false;
    }

    
    protected LinkedHashMap loadChecksums() throws MojoExecutionException {
        LinkedHashMap rc = new LinkedHashMap();
        
        if( !checksums.canRead() ) {
            throw new MojoExecutionException("Cannot read checksum file: "+checksums);
        }
        
        InputStream is=null;
        try {
            is = new FileInputStream(checksums);
            CSVReader reader = new CSVReader(new InputStreamReader(is, "UTF-8"), '=');
            String [] line;
            while ((line = reader.readNext()) != null) {
                if( line.length > 0 ) {
                    String key = line[0].trim();
                    List values = new ArrayList(2);
                    if( line.length > 1 ) {
                        String[] t = line[1].split("\\|");
                        for (int j = 0; j < t.length; j++) {
                            values.add(t[j].toLowerCase().trim());
                        }
                    }
                    rc.put(key, values);
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not load checksum file: "+checksums);
        } finally {
            try {
                is.close();
            } catch (Throwable e) {
            }
        }
                
        return rc;
    }
    
    private void storeChecksums(LinkedHashMap checksums) throws MojoExecutionException {
        // Store it.
        FileOutputStream os=null;
        try {
            boolean exists = this.checksums.exists();
            os = new FileOutputStream(this.checksums);
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(os, "UTF-8"), '=', CSVWriter.NO_QUOTE_CHARACTER);

            if( !exists ) {
                writer.writeNext(new String[]{"# This file uses a 'property file like' syntax"});
                writer.writeNext(new String[]{"# Entries are in the following format: 'artifact","checksum-1|...|checksum-n'"});
                writer.writeNext(new String[]{"# Where artifact follows the following format: 'group/id/type/version'"});
                writer.writeNext(new String[]{"# You can use '*' for the version or checksum"});
                writer.writeNext(new String[]{""});
            }
            
            for (Iterator iterator = checksums.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry i = (Map.Entry)iterator.next();            
                StringBuffer b = new StringBuffer();
                for (Iterator iterator2 = ((List)i.getValue()).iterator(); iterator2.hasNext();) {
                    String s = (String)iterator2.next();            
                    if( b.length()!=0 ) {
                        b.append("|");
                    }
                    b.append(s);
                }
                String key = (String)i.getKey();
                String value = b.toString();
                if( value.length()!=0 ) {
                    writer.writeNext(new String[]{key,value});
                } else { 
                    writer.writeNext(new String[]{key});
                }
            }
            writer.close();
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
