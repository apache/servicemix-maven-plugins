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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Mojo used to build the jbi.xml file for a service unit.
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateComponentDescriptorMojo 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal generate-jbi-service-assembly-descriptor
 * @phase generate-resources
 * @requiresDependencyResolution runtime
 * @description generates the jbi.xml deployment descriptor for a service unit
 */
public class GenerateServiceAssemblyDescriptorMojo extends AbstractJbiMojo {

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

	/**
	 * Dependency graph
	 */
	private JbiResolutionListener listener;

	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog()
				.debug(
						" ======= GenerateServiceAssemlbyDescriptorMojo settings =======");
		getLog().debug("workDirectory[" + workDirectory + "]");
		getLog().debug("generateJbiDescriptor[" + generateJbiDescriptor + "]");
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
			listener = resolveProject();
			generateJbiDescriptor();
		} catch (JbiPluginException e) {
			throw new MojoExecutionException("Failed to generate jbi.xml", e);
		}

		try {
			FileUtils.copyFileToDirectory(new File(generatedDescriptorLocation,
					JBI_DESCRIPTOR), new File(getWorkDirectory(), META_INF));
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Unable to copy jbi.xml to final destination", e);
		}
	}

	/**
	 * Generates the deployment descriptor if necessary.
	 * 
	 * @throws MojoExecutionException
	 */
	protected void generateJbiDescriptor() throws JbiPluginException,
			MojoExecutionException {
		File outputDir = new File(generatedDescriptorLocation);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		File descriptor = new File(outputDir, JBI_DESCRIPTOR);

		List serviceUnits = new ArrayList();

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
					DependencyInformation info = new DependencyInformation();
					info.setName(artifact.getArtifactId());
					String name = artifact.getFile().getName();
					name = name.substring(0, name.lastIndexOf('.')) + ".zip";
					info.setFilename(name);
					info.setComponent(getComponentName(project, artifacts,
							artifact));
					info.setDescription(project.getDescription());
					serviceUnits.add(info);
				}

			}
		}

		List orderedServiceUnits = reorderServiceUnits(serviceUnits);

		JbiServiceAssemblyDescriptorWriter writer = new JbiServiceAssemblyDescriptorWriter(
				encoding);
		writer.write(descriptor, name, description, orderedServiceUnits);
	}

	/**
	 * Re-orders the service units to match order in the dependencies section of
	 * the pom
	 * 
	 * @param serviceUnits
	 * @throws MojoExecutionException
	 */
	private List reorderServiceUnits(List serviceUnits)
			throws MojoExecutionException {

		// TODO Currently we get the model back by re-parsing it however in the
		// future we should be able to use the getModel() - there should be a
		// fix post 2.0.4

		// Iterator dependencies =
		// project.getModel().getDependencies().iterator();

		// For now we will need to reparse the pom without processing
		Iterator dependencies = getReparsedDependencies();		
		
		List orderedServiceUnits = new ArrayList();
		while (dependencies.hasNext()) {
			Dependency dependency = (Dependency) dependencies.next();
			for (Iterator it = serviceUnits.iterator(); it.hasNext();) {
				DependencyInformation serviceUnitInfo = (DependencyInformation) it
						.next();
				if (dependency.getArtifactId()
						.equals(serviceUnitInfo.getName())) {
					System.out.println("Adding "
							+ serviceUnitInfo.getFilename());
					orderedServiceUnits.add(serviceUnitInfo);
				}

			}
		}

		return orderedServiceUnits;
	}

	private Iterator getReparsedDependencies() throws MojoExecutionException {
		MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
		try {
			Model model = mavenXpp3Reader.read(new FileReader(new File(project
					.getBasedir(), "pom.xml")), false);
			return model.getDependencies().iterator();
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to reparse the pom.xml");
		}
	}

	private String getComponentName(MavenProject project, Set artifacts,
			Artifact suArtifact) throws MojoExecutionException {

		getLog().info(
				"Determining component name for service unit "
						+ project.getArtifactId());
		if (project.getProperties().getProperty("componentName") != null) {
			return project.getProperties().getProperty("componentName");
		}

		JbiResolutionListener.Node n = listener.getNode(suArtifact);
		for (Iterator it = n.getChildren().iterator(); it.hasNext();) {
			JbiResolutionListener.Node child = (JbiResolutionListener.Node) it
					.next();
			MavenProject artifactProject = null;
			try {
				artifactProject = projectBuilder.buildFromRepository(child
						.getArtifact(), remoteRepos, localRepo);
			} catch (ProjectBuildingException e) {
				getLog().warn(
						"Unable to determine packaging for dependency : "
								+ child.getArtifact().getArtifactId()
								+ " assuming jar");
			}
			getLog().info(
					"Project " + artifactProject + " packaged "
							+ artifactProject.getPackaging());
			if ((artifactProject != null)
					&& (artifactProject.getPackaging().equals("jbi-component"))) {
				return child.getArtifact().getArtifactId();
			}
		}

		throw new MojoExecutionException(
				"The service unit "
						+ project.getArtifactId()
						+ " does not have a dependency which is packaged as a jbi-component or a project property 'componentName'");
	}

}
