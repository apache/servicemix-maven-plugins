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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.servicemix.common.packaging.ServiceUnitAnalyzer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A Mojo used to build the jbi.xml file for a service unit.
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateComponentDescriptorMojo 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal generate-jbi-service-unit-descriptor
 * @phase process-classes
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
	 * Determines whether to use the service unit analyzer
	 * 
	 * @parameter
	 */
	private Boolean useServiceUnitAnalyzer = Boolean.TRUE;

	/**
	 * Single directory for extra files to include in the JBI component.
	 * 
	 * @parameter expression="${basedir}/src/main/resources/jbi-services.xml"
	 * @required
	 */
	private File jbiServicesFile;

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

	/**
	 * Directory where artifacts for the service unit are held
	 * 
	 * @parameter expression="${basedir}/src/main/resources"
	 */
	private File serviceUnitArtifactsDir;

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

	/**
	 * Generates the deployment descriptor if necessary.
	 */
	protected void generateJbiDescriptor() throws JbiPluginException {
		File outputDir = new File(generatedDescriptorLocation);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		ClassLoader old = Thread.currentThread().getContextClassLoader();

		try {
			URLClassLoader newClassLoader = getClassLoader();
			Thread.currentThread().setContextClassLoader(newClassLoader);
			File descriptor = new File(outputDir, JBI_DESCRIPTOR);
			List uris = new ArrayList();
			JbiServiceUnitDescriptorWriter writer = new JbiServiceUnitDescriptorWriter(
					encoding);

			List consumes = new ArrayList();
			List provides = new ArrayList();

			String serviceUnitAnalyzerClazzName = getServiceUnitAnalyzer();
			// The ServiceUnitAnalyzer should give us the consumes and
			// provides
			if (serviceUnitAnalyzerClazzName != null) {
				ServiceUnitAnalyzer serviceUnitAnalyzer = (ServiceUnitAnalyzer) newClassLoader
						.loadClass(serviceUnitAnalyzerClazzName).newInstance();
				getLog().info(
						"Created Service Unit Analyzer " + serviceUnitAnalyzer);
				serviceUnitAnalyzer.init(serviceUnitArtifactsDir);

				// Need to determine whether we are using the dummy analyzer
				// if so we need to give it the services file
				if (serviceUnitAnalyzer instanceof JbiServiceFileAnalyzer) {
					((JbiServiceFileAnalyzer) serviceUnitAnalyzer)
							.setJbiServicesFile(jbiServicesFile);
				}
				consumes.addAll(serviceUnitAnalyzer.getConsumes());
				provides.addAll(serviceUnitAnalyzer.getProvides());
			}

			getLog().info(
					"generated : consumes " + consumes + " provides "
							+ provides);

			boolean bc = false;
			// TODO: find if the component target is a BC ?
			writer.write(descriptor, bc, name, description, uris, consumes,
					provides);
		} catch (Exception e) {
			throw new JbiPluginException(
					"Unable to generate service unit descriptor!", e);
		} finally {
			Thread.currentThread().setContextClassLoader(old);
		}
	}

	private String getServiceUnitAnalyzer() {
		// We need to work out here whether we should use a dummy service unit
		// analyzer that will examine a local services file or whether
		// to look for the service unit analyzer from the component
		if (jbiServicesFile.exists()) {
			return JbiServiceFileAnalyzer.class.getCanonicalName();
		}
		if (useServiceUnitAnalyzer.booleanValue()) {
			MavenProject project = getComponentProject();
			if (project != null) {
				List plugins = project.getBuild().getPlugins();
				for (Iterator iterator = plugins.iterator(); iterator.hasNext();) {
					Plugin plugin = (Plugin) iterator.next();
					if ("org.apache.servicemix.tooling".equals(plugin
							.getGroupId())
							&& "jbi-maven-plugin"
									.equals(plugin.getArtifactId())) {
						Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();
						if (o != null
								&& o.getChild("serviceUnitAnalyzer") != null) {
							String clazzName = o
									.getChild("serviceUnitAnalyzer").getValue();
							return clazzName;
						}
					}
				}
			}
		}
		return null;
	}

	private MavenProject getComponentProject() {
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
						&& (project.getPackaging().equals("jbi-component"))) {
					return project;
				}

			}
		}
		return null;
	}

}
