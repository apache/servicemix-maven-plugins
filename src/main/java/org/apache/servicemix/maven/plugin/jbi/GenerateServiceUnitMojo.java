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
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
	 * Directory where the application.xml file will be auto-generated.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File workDirectory;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			injectDependentJars();
		} catch (JbiPluginException e) {
			throw new MojoExecutionException("Failed to inject dependencies", e);
		}
	}

	private void injectDependentJars() throws JbiPluginException {
		Set artifacts = project.getArtifacts();
		// TODO Do we need to inject anything?
		// for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
		// Artifact artifact = (Artifact) iter.next();
		//
		// // TODO: utilise appropriate methods from project builder
		// ScopeArtifactFilter filter = new
		// ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
		// if (!artifact.isOptional() && filter.include(artifact)) {
		// String type = artifact.getType();
		// if ("jar".equals(type)) {
		// try {
		// FileUtils.copyFileToDirectory(artifact.getFile(), new
		// File(workDirectory, LIB_DIRECTORY));
		// } catch (IOException e) {
		// throw new JbiPluginException("Unable to file " + artifact.getFile(),
		// e);
		// }
		// }
		// }
		// }
	}

}
