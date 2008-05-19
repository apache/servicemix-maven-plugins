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
package org.apache.servicemix.maven.plugin.xsd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Mojo to add schema location mapping for earlier versions to the spring.schemas file
 * 
 * @goal spring.schemas
 * @phase process-resources
 */
public class SpringSchemasMojo extends AbstractMojo {

    /**
     * Maven project property containing previous releases
     */
    private static final String PREVIOUS_RELEASES = "previous.releases";

    /**
     * Previously released versions of ServiceMix
     */
    private String[] previous = {};

    /**
     * Location URIs
     */
    @SuppressWarnings("serial")
    private List<String> locations = new ArrayList<String>() {
        @Override
        public boolean add(String element) {
            getLog().info("Adding location " + element);
            return super.add(element);
        }
    };

    /**
     * Name of the XSD file
     */
    private String schema;

    /**
     * A reference to the Maven project
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!getSpringSchemas().exists()) {
            getLog().info("Skipping - spring.schemas file does not exist in " + project.getArtifactId());
            return;
        }
        readOriginalSpringSchemas();
        getLog().info("Adding spring.schemas entries for earlier versions");
        if (project.getProperties().containsKey(PREVIOUS_RELEASES)) {
            previous = ((String) project.getProperties().get(PREVIOUS_RELEASES)).split(",");
        } else {
            getLog().warn("No previous version information found");
            for (Object key : project.getProperties().keySet()) {
                getLog().info("'" + key.toString() + "'" + " = " + project.getProperties().get(key));
            }
        }
        for (String version : previous) {
            addVersion(version);
        }
        getLog().info("Adding spring.schemas entry for this version");
        addVersion(project.getVersion());
        writeNewSpringSchemas();
    }

    /**
     * Write the new spring.schemas file
     * 
     * @throws MojoExecutionException
     */
    private void writeNewSpringSchemas() throws MojoExecutionException {
        getLog().info("Writing new spring.schemas file");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(getSpringSchemas())));
            for (String location : locations) {
                writer.println(location + "=" + schema);
            }
            writer.flush();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read spring.schemas file", e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Read the original spring.schemas file
     * 
     * @throws MojoExecutionException
     */
    private void readOriginalSpringSchemas() throws MojoExecutionException {
        getLog().info("Reading information from spring.schemas");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(getSpringSchemas()));
            String line = reader.readLine();
            while (line != null) {
                //skip any lines that don't contain a mapping
                if (line.contains("=")) {
                    String[] info = line.split("=");
                    if (schema == null) {
                        getLog().info("Schema name is " + info[1]);
                        schema = info[1];
                    }
                    locations.add(info[0]);
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Unable to read spring.schemas file", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read spring.schemas file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new MojoExecutionException("Unable to close file reader", e);
                }
            }
        }
    }

    /**
     * @return the spring.schemas file generated by xbean
     */
    private File getSpringSchemas() {
        return new File(project.getBasedir().toString() + File.separatorChar + "target" + File.separatorChar + "classes" + File.separatorChar + "META-INF", "spring.schemas");
    }

    /**
     * Add a version of the XSD to the spring.schemas file
     * 
     * @param version the version to be added
     */
    private void addVersion(String version) {
        locations.add("http\\://servicemix.apache.org/schema/" + project.getArtifactId() + "-" + version + ".xsd");
    }

}
