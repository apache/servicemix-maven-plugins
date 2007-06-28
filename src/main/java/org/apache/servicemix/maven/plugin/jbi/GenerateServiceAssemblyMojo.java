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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Mojo used to build the jbi service assembly zip file
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateApplicationXmlMojo.java 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal jbi-service-assembly
 * @phase package
 * @requiresDependencyResolution runtime
 * @description injects additional libraries into service assembly
 */
public class GenerateServiceAssemblyMojo extends AbstractJbiMojo {

    /**
     * Directory where the application.xml file will be auto-generated.
     * 
     * @parameter expression="${project.build.directory}/classes"
     * @required
     */
    private File workDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            injectDependentServiceUnits();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to inject dependencies", e);
        }
    }

    private void injectDependentServiceUnits() throws JbiPluginException,
            ArtifactResolutionException, ArtifactNotFoundException {
        Set artifacts = project.getArtifacts();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();

            // TODO: utilise appropriate methods from project builder
            ScopeArtifactFilter filter = new ScopeArtifactFilter(
                    Artifact.SCOPE_RUNTIME);
            if (!artifact.isOptional() && filter.include(artifact)
                    && (artifact.getDependencyTrail().size() == 2)) {
                MavenProject project = null;
                try {
                    project = projectBuilder.buildFromRepository(artifact,
                            remoteRepos, localRepo);
                } catch (ProjectBuildingException e) {
                    getLog().warn(
                            "Unable to determine packaging for dependency : "
                                    + artifact.getArtifactId()
                                    + " assuming jar");
                }
                if ((project != null)
                        && (project.getPackaging().equals("jbi-service-unit"))) {
                    try {
                        String path = artifact.getFile().getAbsolutePath();
                        path = path.substring(0, path.lastIndexOf('.'))
                                + ".zip";
                        FileUtils.copyFileToDirectory(new File(path),
                                workDirectory);
                    } catch (IOException e) {
                        throw new JbiPluginException(e);
                    }
                }
            }
        }
    }

}
