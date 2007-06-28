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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.codehaus.xfire.gen.WsdlGenTask;
import org.codehaus.xfire.spring.XFireConfigLoader;

/**
 * WsdlGen mojo.
 * <p/>
 * Implemented as a wrapper around the XFire WsdlGen Ant task.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @version $Id$
 * @goal wsdlgen
 * @requiresProject
 * @requiresDependencyResolution
 */
public class WsdlgenMojo extends AbstractMojo {
    
    /**
     * Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * xfire service.xml config files
     * If not specified, the list will contain a single value 'src/main/resources/META-INF/xfire/services.xml'
     *
     * @parameter
     */
    private List configs;

    /**
     * The directory will be added as Project's Resource.
     * @parameter expression="${outputDirectory}" default-value="${project.build.directory}/generated-sources/xfire/wsdlgen"
     * @required
     */
    private File outputDirectory;

    /**
     * The basedir of the project.
     *
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /*
     private PrintStream systemErr;
     private PrintStream systemOut;
     private final PrintStream mySystemErr = new PrintStream(new WsdlgenMojo.MyErrorStream());
     private final PrintStream mySystemOut = new PrintStream(new WsdlgenMojo.MyOutputStream());

     public void execute()
     throws MojoExecutionException
     {

     systemErr = System.err;
     systemOut = System.out;
     System.setErr(mySystemErr);
     // System.setOut(mySystemOut); // causes java.lang.OutOfMemoryError: Java heap space  on my box

     try {
     exec();
     } finally {
     System.setErr( systemErr );
     // System.setOut( systemOut );
     }
     }

     class MyErrorStream extends OutputStream {
     private StringBuffer buffer = new StringBuffer();

     public void write( final int b ) throws IOException {
     final char c = (char) b;
     // shouldn't we handle '\r' as well ??
     if (c == '\n') {
     getLog().error( buffer );
     buffer = new StringBuffer();
     } else {
     buffer.append( c );
     }
     }
     }

     class MyOutputStream extends OutputStream {
     private StringBuffer buffer = new StringBuffer();

     public void write( final int b ) throws IOException {
     final char c = (char) b;
     // shouldn't we handle '\r' as well ??
     if (c == '\n') {
     getLog().info( buffer );
     buffer = new StringBuffer();
     } else {
     buffer.append( c );
     }
     }
     }
     */

    public void execute() throws MojoExecutionException {

        if (configs == null) {
            configs = new ArrayList();
        }

        if (configs.size() == 0) {
            configs
                    .add(new File(basedir,
                            "src/main/resources/META-INF/xfire/services.xml")
                            .getPath());
        }

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            getLog()
                    .warn(
                            "the output directory "
                                    + outputDirectory
                                    + " doesn't exist and couldn't be created. The goal with probably fail.");
        }

        final Project antProject = new Project();

        antProject.addBuildListener(new WsdlgenMojo.DebugAntBuildListener());

        // add current pom dependencies to WsdlGenTask class loader
        ClassLoader parent = WsdlGenTask.class.getClassLoader();

        URL[] urls;
        try {
            Collection l = project.getArtifacts();
            List theurls = new ArrayList();
            theurls.add(new File(project.getBuild().getOutputDirectory())
                    .toURL());
            for (Iterator iterator = l.iterator(); iterator.hasNext();) {
                Artifact dep = (Artifact) iterator.next();
                theurls.add(dep.getFile().toURL());
            }
            urls = (URL[]) theurls.toArray(new URL[theurls.size()]);

            getLog().debug("classloader classpath: " + theurls);

        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }

        URLClassLoader cl = new URLClassLoader(urls, parent);
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        // displayClasspath(cl, "using classpath");
        // load("javax.servlet.ServletException", cl);
        // load("org.codehaus.xfire.transport.http.XFireServletController", cl);

        final WsdlGenTask task = new WsdlGenTask();

        task.setProject(antProject);

        task.setOutputDirectory(outputDirectory.getAbsolutePath());

        for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
            String configUrl = (String) iterator.next();

            // required for multi-modules projects
            if (!new File(configUrl).exists()) {
                getLog().warn("configUrl not found. Task will perhaps fail");
            }

            task.setConfigUrl(configUrl);

            getLog().info(
                    "Executing XFire WsdlGen task for configUrl: " + configUrl);

            try {
                task.execute();
            } catch (BuildException e) {
                throw new MojoExecutionException("command execution failed", e);
            }

            getLog().debug("generated " + task.getGeneratedFile());
        }
        Thread.currentThread().setContextClassLoader(oldCl);

        getLog().debug("Adding outputDirectory as Project's resource.");
        Resource resource = new Resource();
        resource.setDirectory(outputDirectory.getAbsolutePath());
        project.addResource(resource);
    }

    /*
     void displayClasspath(URLClassLoader cl, String message)
     {
     URL[] urls = cl.getURLs();
     for (int i = 0; i < urls.length; i++) {
     URL urL = urls[i];
     getLog().info("URL " + i + ":" +  urL);
     }
     }
     */
    private void displayClasspath(ClassLoader classLoader, String message) {
        getLog().info("------ " + message + ":" + classLoader);
        if (classLoader == null) {
            return;
        }
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader cl = (URLClassLoader) classLoader;
            URL[] urls = cl.getURLs();
            for (int i = 0; i < urls.length; i++) {
                URL urL = urls[i];
                getLog().info("URL " + i + ":" + urL);
            }
        } else if (classLoader instanceof AntClassLoader) {
            AntClassLoader cl = (AntClassLoader) XFireConfigLoader.class
                    .getClassLoader();
            String[] urls = cl.getClasspath().split(File.pathSeparator);
            for (int i = 0; i < urls.length; i++) {
                String url = urls[i];
                getLog().info("URL " + i + ":" + url);
            }
        } else {
            // not handled
        }
        displayClasspath(classLoader.getParent(), "parent->" + message);
    }

    void load(String className, ClassLoader cl) {
        try {
            Class c = Class.forName(className, true, cl);
            getLog().debug(c.toString());
        } catch (Exception e) {
            displayClasspath(cl, "using classpath");
            getLog().error(e);
        }
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
