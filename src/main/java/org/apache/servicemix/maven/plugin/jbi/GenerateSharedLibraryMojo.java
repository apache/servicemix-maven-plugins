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
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Mojo used to build the jbi shared library zip file
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateApplicationXmlMojo.java 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal jbi-shared-library
 * @phase package
 * @requiresDependencyResolution runtime
 * @description injects additional libraries into shared library
 */
public class GenerateSharedLibraryMojo extends AbstractJbiMojo {

    /**
     * The directory for the generated JBI component.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The name of the generated war.
     * 
     * @parameter expression="${project.artifactId}-${project.version}.zip"
     * @required
     */
    private String sharedLibraryName;

    /**
     * The name of the generated war.
     * 
     * @parameter expression="${project.artifactId}-${project.version}.jar"
     * @required
     */
    private String jarName;

    /**
     * The Zip archiver.
     * 
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * Single directory for extra files to include in the JBI component.
     * 
     * @parameter expression="${basedir}/src/main/jbi"
     * @required
     */
    private File jbiSourceDirectory;

    /**
     * The maven archive configuration to use.
     * 
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().debug(" ======= GenerateInstallerMojo settings =======");
        getLog().debug("workDirectory[" + workDirectory + "]");
        getLog().debug("installerName[" + sharedLibraryName + "]");
        getLog().debug("jbiSourceDirectory[" + jbiSourceDirectory + "]");

        try {

            createUnpackedSharedLibrary();

            File installerFile = new File(outputDirectory, sharedLibraryName);
            createArchive(installerFile);

            projectHelper.attachArtifact(project, "jar", "", new File(
                    outputDirectory, jarName));

            projectHelper.attachArtifact(project, "zip", "installer", new File(
                    outputDirectory, sharedLibraryName));

        } catch (JbiPluginException e) {
            throw new MojoExecutionException("Failed to create shared library",
                    e);
        }
    }

    private void createArchive(File installerFile) throws JbiPluginException {
        try {

            // generate war file
            getLog().info(
                    "Generating shared library "
                            + installerFile.getAbsolutePath());
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(jarArchiver);
            archiver.setOutputFile(installerFile);
            jarArchiver.addDirectory(workDirectory);
            jarArchiver.addConfiguredManifest(createManifest());
            if (jbiSourceDirectory.isDirectory()) {
                jarArchiver.addDirectory(jbiSourceDirectory, null,
                        DirectoryScanner.DEFAULTEXCLUDES);
            }
            // create archive
            archiver.createArchive(getProject(), archive);

        } catch (Exception e) {
            throw new JbiPluginException("Error creating shared library: "
                    + e.getMessage(), e);
        }
    }

    private void createUnpackedSharedLibrary() throws JbiPluginException {

        if (!workDirectory.isDirectory() && !workDirectory.mkdirs()) {
            throw new JbiPluginException("Unable to create work directory: " + workDirectory);
        }

        File projectArtifact = new File(outputDirectory, project
                .getArtifactId()
                + "-" + project.getVersion() + ".jar");
        try {
            FileUtils.copyFileToDirectory(projectArtifact, new File(
                    workDirectory, LIB_DIRECTORY));

        } catch (IOException e) {
            throw new JbiPluginException("Unable to copy file "
                    + projectArtifact, e);
        }

        Set artifacts = project.getArtifacts();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();

            // TODO: utilise appropriate methods from project builder
            ScopeArtifactFilter filter = new ScopeArtifactFilter(
                    Artifact.SCOPE_RUNTIME);
            if (!artifact.isOptional() && filter.include(artifact)) {
                String type = artifact.getType();
                if ("jar".equals(type)) {
                    try {
                        FileUtils.copyFileToDirectory(artifact.getFile(),
                                new File(workDirectory, LIB_DIRECTORY));
                    } catch (IOException e) {
                        throw new JbiPluginException("Unable to copy file "
                                + artifact.getFile(), e);
                    }
                }
            }
        }
    }

}
