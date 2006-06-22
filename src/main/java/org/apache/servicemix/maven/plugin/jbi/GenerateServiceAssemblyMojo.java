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
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
		} catch (JbiPluginException e) {
			throw new MojoExecutionException("Failed to inject dependencies", e);
		}
	}

	private void injectDependentServiceUnits() throws JbiPluginException {
		Set artifacts = project.getArtifacts();
		for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();

			// TODO: utilise appropriate methods from project builder
			ScopeArtifactFilter filter = new ScopeArtifactFilter(
					Artifact.SCOPE_RUNTIME);
			if (!artifact.isOptional() && filter.include(artifact)) {
				String type = artifact.getType();
				if ("jbi-service-unit".equals(type)) {
					try {								
						getLog()
								.info(
										"Copying service unit "
												+ artifact.getFile()
														.getAbsolutePath()
												+ " into working directory for packaging");
						FileUtils.copyFileToDirectory(artifact.getFile(),
								workDirectory);
					} catch (IOException e) {
						throw new JbiPluginException(
								"Unable to find service unit "
										+ artifact.getFile(), e);
					}
				}
			}
		}
	}

}
