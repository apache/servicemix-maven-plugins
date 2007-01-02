/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.maven.plugin.legal;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 * @goal copy
 * @phase generate-resources
 */
public class LegalMojo extends AbstractMojo {

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * @parameter
     */         
    protected File outputDir;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (outputDir != null) {
                copyLegalFiles(outputDir);
            } else if (project.getPackaging().equals("jar")) {
                copyLegalFiles(new File(project.getBasedir(), "target/classes/"));
            } else if (project.getPackaging().equals("maven-plugin")) {
                copyLegalFiles(new File(project.getBasedir(), "target/classes/"));
            } else if (project.getPackaging().equals("war")) {
                copyLegalFiles(new File(project.getBasedir(), "target/" + project.getArtifactId() + "-" + project.getVersion() + "/"));
            } else if (project.getPackaging().equals("jbi-shared-library")) {
                copyLegalFiles(new File(project.getBasedir(), "target/classes/"));
                copyLegalFiles(new File(project.getBasedir(), "target/" + project.getArtifactId() + "-" + project.getVersion() + "-installer/"));
            } else if (project.getPackaging().equals("jbi-component")) {
                copyLegalFiles(new File(project.getBasedir(), "target/classes/"));
                copyLegalFiles(new File(project.getBasedir(), "target/" + project.getArtifactId() + "-" + project.getVersion() + "-installer/"));
            } else if (project.getPackaging().equals("jbi-service-unit")) {
                copyLegalFiles(new File(project.getBasedir(), "target/classes/"));
            } else if (project.getPackaging().equals("jbi-service-assembly")) {
                copyLegalFiles(new File(project.getBasedir(), "target/classes/"));
                copyLegalFiles(new File(project.getBasedir(), "target/" + project.getArtifactId() + "-" + project.getVersion() + "-installer/"));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy legal files", e);
        }
  	}
  	
  	protected void copyLegalFiles(File outputDir) throws IOException {
        String[] names = { "/META-INF/DISCLAIMER", "/META-INF/NOTICE", "/META-INF/LICENSE"};
        for (int i = 0; i < names.length; i++) {
            URL res = getClass().getResource(names[i]);
            FileUtils.copyURLToFile(res, new File(outputDir, names[i]));
        }
    }

}
