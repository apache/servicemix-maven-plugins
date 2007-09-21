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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Creates a binary servicemix install directory that can then be referenced
 * by a spring.xml file for integration testing.
 * 
 * @version $Id: $
 * @goal createDirs
 * @execute phase="test-compile"
 * @requiresDependencyResolution runtime
 * @description Creates the ServiceMix runtime directories
 */
public class MakeServiceMixDirsMojo extends JbiProjectDeployerMojo {

    /**
     * The deploy diretory to put the service assemblies inside
     *
     * @parameter default-value="${project.build.directory}/servicemix/deploy"
     */
    private String deployDirectory;

    /**
     * @parameter default-value="true"
     */
    private boolean cleanStart;

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {

            if (cleanStart) {
                getLog().info(
                        "Cleaning ServiceMix root directory [" + deployDirectory
                                + "]");
                File rootDir = new File(deployDirectory);
                FileUtils.deleteDirectory(rootDir);
                rootDir.mkdirs();
            }

            deployProject();

            getLog().info("Project deployed");

        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Apache ServiceMix was unable to deploy project", e);
        }
    }

    //@Override
    protected void deployDependency(JbiDeployableArtifact jbiDeployable, boolean doDeferExceptions) throws MojoExecutionException {
        // do nothing
        String name = jbiDeployable.getType();
        getLog().info("deployDependency: type: " + name + " dependency: " + jbiDeployable);
        if (JBI_SERVICE_ASSEMBLY.equals(name)) {
            File assemblyFile = new File(jbiDeployable.getFile());
            File outputFile = new File(deployDirectory, assemblyFile.getName());

            getLog().info("copying service Assembly!:  " + jbiDeployable + " to: " + outputFile);
            try {
                FileUtils.copyFile(assemblyFile, outputFile);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

    //@Override
    protected void startDependency(JbiProjectDeployerMojo.JbiDeployableArtifact jbiDeployable) {
        // do nothing
    }

    //@Override
    protected void stopDependency(JbiDeployableArtifact jbiDeployable) {
        // do nothing
    }

    //@Override
    protected boolean isDeployed(JbiDeployableArtifact jbiDeployable) {
        return false;
    }
}
