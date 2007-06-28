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
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Mojo used to build the jbi service unit zip file
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateApplicationXmlMojo.java 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal jbi-service-unit
 * @phase package
 * @requiresDependencyResolution runtime
 * @description injects additional libraries into service unit
 */
public class GenerateServiceUnitMojo extends AbstractJbiMojo {

    /**
     * The name of the generated war.
     * 
     * @parameter expression="${project.artifactId}-${project.version}.zip"
     * @required
     */
    private String serviceUnitName;

    /**
     * The directory for the generated JBI component.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The Zip archiver.
     * 
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The maven archive configuration to use.
     * 
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Directory where the application.xml file will be auto-generated.
     * 
     * @parameter expression="${project.build.directory}/classes"
     */
    private File serviceUnitLocation;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            createUnpackedInstaller();

            File serviceUnitFile = new File(outputDirectory, serviceUnitName);
            createArchive(serviceUnitFile);

            projectHelper.attachArtifact(project, "zip", "", new File(
                    outputDirectory, serviceUnitName));

        } catch (JbiPluginException e) {
            throw new MojoExecutionException("Failed to create service unit", e);
        }

    }

    private void createArchive(File installerFile) throws JbiPluginException {
        try {

            // generate war file
            getLog().info(
                    "Generating service unit "
                            + installerFile.getAbsolutePath());
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(jarArchiver);
            archiver.setOutputFile(installerFile);
            jarArchiver.addDirectory(workDirectory);

            // create archive
            archiver.createArchive(getProject(), archive);

        } catch (ArchiverException e) {
            throw new JbiPluginException("Error creating service unit: "
                    + e.getMessage(), e);
        } catch (ManifestException e) {
            throw new JbiPluginException("Error creating service unit: "
                    + e.getMessage(), e);
        } catch (IOException e) {
            throw new JbiPluginException("Error creating service unit: "
                    + e.getMessage(), e);
        } catch (DependencyResolutionRequiredException e) {
            throw new JbiPluginException("Error creating service unit: "
                    + e.getMessage(), e);
        }

    }

    private void createUnpackedInstaller() throws JbiPluginException {

        if (!workDirectory.isDirectory() && !workDirectory.mkdirs()) {
            throw new JbiPluginException("Unable to create work directory: " + workDirectory);
        }

        try {
            FileUtils.copyDirectoryStructure(serviceUnitLocation, workDirectory);
        } catch (IOException e) {
            throw new JbiPluginException("Unable to copy directory "
                    + serviceUnitLocation, e);
        }

        ScopeArtifactFilter filter = new ScopeArtifactFilter(
                Artifact.SCOPE_RUNTIME);

        JbiResolutionListener listener = resolveProject();
        // print(listener.getRootNode(), "");

        Set includes = new HashSet();
        for (Iterator iter = project.getArtifacts().iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            if (!artifact.isOptional() && filter.include(artifact)) {
                MavenProject project = null;
                getLog().info("Resolving " + artifact);
                try {
                    project = projectBuilder.buildFromRepository(artifact,
                            remoteRepos, localRepo);
                } catch (ProjectBuildingException e) {
                    getLog().warn(
                            "Unable to determine packaging for dependency : "
                                    + artifact.getArtifactId()
                                    + " assuming jar");
                }
                String type = project != null ? project.getPackaging()
                        : artifact.getType();
                if ("jbi-component".equals(type)) {
                    removeBranch(listener, artifact);
                } else if ("jbi-shared-library".equals(type)) {
                    removeBranch(listener, artifact);
                } else {
                    includes.add(artifact);
                }
            }
        }
        // print(listener.getRootNode(), "");

        for (Iterator iter = retainArtifacts(includes, listener).iterator(); iter
                .hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            try {
                getLog().info("Including: " + artifact);
                FileUtils.copyFileToDirectory(artifact.getFile(), new File(
                        workDirectory, LIB_DIRECTORY));
            } catch (IOException e) {
                throw new JbiPluginException("Unable to copy file "
                        + artifact.getFile(), e);
            }
        }
    }
}
