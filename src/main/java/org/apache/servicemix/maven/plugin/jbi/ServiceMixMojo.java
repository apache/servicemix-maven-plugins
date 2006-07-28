/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jbi.JBIException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.codehaus.plexus.util.FileUtils;

/**
 * Starts a ServiceMix JBI container and them uses the deploy project MOJO to
 * push the current project and dependencies to it
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateComponentDescriptorMojo 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal servicemix
 * @requiresDependencyResolution runtime
 * @description installs the project (and all dependencies) to a local
 *              ServiceMix instance
 */
public class ServiceMixMojo extends JbiProjectDeployerMojo {

	private JBIContainer jbiContainer;

	/**
	 * @parameter default-value="${project.build.directory}/servicemix/install"
	 */
	private String installDirectory;

	/**
	 * @parameter default-value="${project.build.directory}/servicemix/deploy"
	 */
	private String deploymentDirectory;

	/**
	 * @parameter default-value="${project.build.directory}/servicemix/rootDir"
	 */
	private String rootDirectory;

	/**
	 * @parameter default-value="true"
	 */
	private boolean cleanStart;

	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			if (cleanStart) {
				getLog().info(
						"Cleaning ServiceMix root directory [" + rootDirectory
								+ "]");
				File rootDir = new File(rootDirectory);
				FileUtils.deleteDirectory(rootDir);
				rootDir.mkdirs();
			}

			startServiceMix();
			deployProject();

			getLog().info("Project deployed");
			while (true)
				;
		} catch (Exception e) {
			stopServiceMix();
			throw new MojoExecutionException(
					"Apache ServiceMix was able to deploy project", e);
		}

	}

	private void stopServiceMix() {
		getLog().info("Shutting down Apache ServiceMix");
		if (jbiContainer != null)
			try {
				jbiContainer.shutDown();
			} catch (JBIException e) {
				getLog().warn(e);
			}
	}

	private void startServiceMix() throws MojoExecutionException {
		try {
			getLog().info("Starting Apache ServiceMix");
			jbiContainer = new JBIContainer();
			jbiContainer.setRmiPort(Integer.parseInt(port));
			jbiContainer.setCreateMBeanServer(true);
			jbiContainer.setInstallationDirPath(installDirectory);
			jbiContainer.setDeploymentDirPath(deploymentDirectory);
			jbiContainer.setRootDir(rootDirectory);
			jbiContainer.init();
			jbiContainer.start();
		} catch (JBIException e) {
			throw new MojoExecutionException(
					"Unable to start the JBI container", e);
		}
	}
}
