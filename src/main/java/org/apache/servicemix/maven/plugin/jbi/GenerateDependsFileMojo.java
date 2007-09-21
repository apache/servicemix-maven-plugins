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
package org.apache.servicemix.maven.plugin.jbi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Generates the dependencies properties file
 *
 * @version $Id: $
 * @goal generate-depends-file
 * @phase generate-resources
 * @requiresDependencyResolution runtime
 * @description Generates the dependencies properties file
 */
public class GenerateDependsFileMojo extends AbstractJbiMojo {

    protected static final String SEPARATOR = "/";

    /**
     * The file to generate
     *
     * @parameter default-value="${project.build.directory}/target/classes/META-INF/maven/dependencies.properties"
     */
    
    private File outputFile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            Properties properties = new Properties();
            populateProperties(properties);
            String comments = "Generated Maven dependencies by the ServiceMix Maven Plugin";
            out = new FileOutputStream(outputFile);
            properties.store(out, comments);
            getLog().info("Created: " + outputFile);

        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Unable to create dependencies file: " + e, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    getLog().info("Failed to close: " + outputFile + ". Reason: " + e, e);
                }
            }
        }
    }

    protected void populateProperties(Properties properties) {
        Iterator iterator = project.getDependencies().iterator();
        while (iterator.hasNext()) {
            Dependency dependency = (Dependency) iterator.next();
            String prefix = dependency.getGroupId() + SEPARATOR + dependency.getArtifactId() + SEPARATOR;
            properties.put(prefix + "version", dependency.getVersion());
            properties.put(prefix + "type", dependency.getType());
            properties.put(prefix + "scope", dependency.getScope());

            getLog().debug("Dependency: " + dependency + " classifier: " + dependency.getClassifier() + " type: " + dependency.getType());
        }
    }
}