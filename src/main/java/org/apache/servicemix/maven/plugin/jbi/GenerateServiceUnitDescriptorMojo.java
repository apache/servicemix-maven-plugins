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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.servicemix.common.packaging.ServiceUnitAnalyzer;

/**
 * A Mojo used to build the jbi.xml file for a service unit.
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateComponentDescriptorMojo 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal generate-jbi-service-unit-descriptor
 * @phase generate-resources
 * @requiresDependencyResolution runtime
 * @description generates the jbi.xml deployment descriptor for a service unit
 */
public class GenerateServiceUnitDescriptorMojo extends AbstractJbiMojo {

	public static final String UTF_8 = "UTF-8";

	/**
	 * Whether the jbi.xml should be generated or not.
	 * 
	 * @parameter
	 */
	private Boolean generateJbiDescriptor = Boolean.TRUE;

	/**
	 * The component name.
	 * 
	 * @parameter expression="${project.artifactId}"
	 */
	private String name;

	/**
	 * The component description.
	 * 
	 * @parameter expression="${project.name}"
	 */
	private String description;

	/**
	 * Character encoding for the auto-generated application.xml file.
	 * 
	 * @parameter
	 */
	private String encoding = UTF_8;

	/**
	 * Directory where the application.xml file will be auto-generated.
	 * 
	 * @parameter expression="${project.build.directory}/classes/META-INF"
	 */
	private String generatedDescriptorLocation;

	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog().debug(
				" ======= GenerateServiceUnitDescriptorMojo settings =======");
		getLog().debug("workDirectory[" + workDirectory + "]");
		getLog().debug("generateDescriptor[" + generateJbiDescriptor + "]");
		getLog().debug("name[" + name + "]");
		getLog().debug("description[" + description + "]");
		getLog().debug("encoding[" + encoding + "]");
		getLog().debug(
				"generatedDescriptorLocation[" + generatedDescriptorLocation
						+ "]");

		if (!generateJbiDescriptor.booleanValue()) {
			getLog().debug("Generation of jbi.xml is disabled");
			return;
		}

		// Generate jbi descriptor and copy it to the build directory
		getLog().info("Generating jbi.xml");
		try {
			generateJbiDescriptor();
		} catch (JbiPluginException e) {
			throw new MojoExecutionException("Failed to generate jbi.xml", e);
		}

	}

	private void buildConnections() {

	}

	/**
	 * Set up a classloader for the execution of the main class.
	 * 
	 * @return
	 * @throws MojoExecutionException
	 */
	private URLClassLoader getClassLoader() throws MojoExecutionException {
		try {
			Set urls = new HashSet();

			URL mainClasses = new File(project.getBuild().getOutputDirectory())
					.toURL();
			getLog().debug("Adding to classpath : " + mainClasses);
			urls.add(mainClasses);

			URL testClasses = new File(project.getBuild()
					.getTestOutputDirectory()).toURL();
			getLog().debug("Adding to classpath : " + testClasses);
			urls.add(testClasses);

			Set dependencies = project.getArtifacts();
			Iterator iter = dependencies.iterator();
			while (iter.hasNext()) {
				Artifact classPathElement = (Artifact) iter.next();
				getLog().debug(
						"Adding artifact: " + classPathElement.getArtifactId()
								+ " to classpath");
				urls.add(classPathElement.getFile().toURL());
			}
			URLClassLoader appClassloader = new URLClassLoader((URL[]) urls
					.toArray(new URL[urls.size()]), this.getClass()
					.getClassLoader());
			return appClassloader;
		} catch (MalformedURLException e) {
			throw new MojoExecutionException(
					"Error during setting up classpath", e);
		}
	}

	/**
	 * Generates the deployment descriptor if necessary.
	 */
	protected void generateJbiDescriptor() throws JbiPluginException {
		File outputDir = new File(generatedDescriptorLocation);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		File descriptor = new File(outputDir, JBI_DESCRIPTOR);

		List uris = new ArrayList();

		JbiServiceUnitDescriptorWriter writer = new JbiServiceUnitDescriptorWriter(
				encoding);

		ServiceUnitAnalyzer serviceUnitAnalyzer = getServiceUnitAnalyzer();
		List consumes = new ArrayList();
		List provides = new ArrayList();
		if (serviceUnitAnalyzer != null) {
			consumes.addAll(serviceUnitAnalyzer.getConsumes());
			provides.addAll(serviceUnitAnalyzer.getProvides());
		}

		writer.write(descriptor, name, description, uris);
	}

	private ServiceUnitAnalyzer getServiceUnitAnalyzer() {
		// TODO Auto-generated method stub
		return null;
	}
}
