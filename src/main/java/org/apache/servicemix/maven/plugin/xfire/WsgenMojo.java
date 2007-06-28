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
package org.apache.servicemix.maven.plugin.xfire;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.codehaus.xfire.gen.WsGenTask;

/**
 * WsGen mojo. <p/> Implemented as a wrapper around the XFire WsGen Ant task.
 * 
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @version $Id$
 * @goal wsgen
 * @phase generate-sources
 * @requiresProject
 * @requiresDependencyResolution
 */
public class WsgenMojo extends AbstractMojo {

    /**
     * Project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * URLs
     * 
     * @parameter
     * @required
     */
    private List wsdls;

    /**
     * @parameter expression="${package}" alias="package"
     */
    private String thePackage; // reserved keyword...

    /**
     * @parameter expression="${profile}"
     */
    private String profile;

    /**
     * @parameter expression="${binding}"
     */
    private String binding;

    /**
     * Will be added to the compileSourceRoot
     * 
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}/generated-sources/xfire/wsgen"
     * @required
     */
    private File outputDirectory;

    private PrintStream systemErr;

    //private PrintStream systemOut;

    private final PrintStream mySystemErr = new PrintStream(new MyErrorStream());

    //private final PrintStream mySystemOut = new PrintStream(new MyOutputStream());

    public void execute() throws MojoExecutionException {

        systemErr = System.err;
        //systemOut = System.out;
        System.setErr(mySystemErr);
        // System.setOut(mySystemOut); // causes java.lang.OutOfMemoryError:
        // Java heap space on my box

        try {
            exec();
        } finally {
            System.setErr(systemErr);
            // System.setOut( systemOut );
        }
    }

    class MyErrorStream extends OutputStream {
        private StringBuffer buffer = new StringBuffer();

        public void write(final int b) throws IOException {
            final char c = (char) b;
            // shouldn't we handle '\r' as well ??
            if (c == '\n') {
                getLog().error(buffer);
                buffer = new StringBuffer();
            } else {
                buffer.append(c);
            }
        }
    }

    class MyOutputStream extends OutputStream {
        private StringBuffer buffer = new StringBuffer();

        public void write(final int b) throws IOException {
            final char c = (char) b;
            // shouldn't we handle '\r' as well ??
            if (c == '\n') {
                getLog().info(buffer);
                buffer = new StringBuffer();
            } else {
                buffer.append(c);
            }
        }
    }

    private void exec() throws MojoExecutionException {

        if (wsdls.size() == 0) {
            return;
        }

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            getLog()
                    .warn(
                            "the output directory "
                                    + outputDirectory
                                    + " doesn't exist and couldn't be created. The goal with probably fail.");
        }

        final Project antProject = new Project();

        antProject.addBuildListener(new DebugAntBuildListener());

        final WsGenTask task = new WsGenTask();

        task.setProject(antProject);

        if (binding != null) {
            task.setBinding(binding);
        }

        if (profile != null) {
            task.setProfile(profile);
        }

        if (thePackage != null) {
            task.setPackage(thePackage);
        }

        task.setOutputDirectory(outputDirectory.getAbsolutePath());

        for (Iterator iterator = wsdls.iterator(); iterator.hasNext();) {
            String wsdlUrl = (String) iterator.next();

            if (!wsdlUrl.contains("://")) {
                wsdlUrl = new File(wsdlUrl).toURI().toString();
            }

            task.setWsdl(wsdlUrl);

            getLog().info("Executing XFire WsGen task with url: " + wsdlUrl);

            try {
                task.execute();
            } catch (BuildException e) {
                throw new MojoExecutionException("command execution failed", e);
            }
        }

        getLog().debug(
                "Adding outputDirectory to source root: " + outputDirectory);

        this.project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    }

    private class DebugAntBuildListener implements BuildListener {
        public void buildStarted(final BuildEvent buildEvent) {
            getLog().debug(buildEvent.getMessage());
        }

        public void buildFinished(final BuildEvent buildEvent) {
            getLog().debug(buildEvent.getMessage());
        }

        public void targetStarted(final BuildEvent buildEvent) {
            getLog().debug(buildEvent.getMessage());
        }

        public void targetFinished(final BuildEvent buildEvent) {
            getLog().debug(buildEvent.getMessage());
        }

        public void taskStarted(final BuildEvent buildEvent) {
            getLog().debug(buildEvent.getMessage());
        }

        public void taskFinished(final BuildEvent buildEvent) {
            getLog().debug(buildEvent.getMessage());
        }

        public void messageLogged(final BuildEvent buildEvent) {
            getLog().debug(buildEvent.getMessage());
        }
    }
}
