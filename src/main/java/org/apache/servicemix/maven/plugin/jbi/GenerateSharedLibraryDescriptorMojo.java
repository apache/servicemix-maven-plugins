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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Mojo used to build the jbi.xml file for a shared library
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateComponentDescriptorMojo 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal generate-jbi-shared-library-descriptor
 * @phase generate-resources
 * @requiresDependencyResolution runtime
 * @description generates the jbi.xml deployment descriptor for a shared library
 */
public class GenerateSharedLibraryDescriptorMojo extends AbstractJbiMojo {

	public static final String UTF_8 = "UTF-8";

	/**
	 * Whether the jbi.xml should be generated or not.
	 * 
	 * @parameter
	 */
	private Boolean generateJbiDescriptor = Boolean.TRUE;

	/**
	 * The shared library name.
	 * 
	 * @parameter expression="${project.artifactId}"
	 */
	private String name;

	/**
	 * The shared library description.
	 * 
	 * @parameter expression="${project.name}"
	 */
	private String description;

	/**
	 * The shared library version.
	 * 
	 * @parameter expression="${project.version}"
	 */
	private String version;

	/**
	 * The shared library class loader delegate
	 * 
	 * @parameter expression="self-first"
	 */
	private String classLoaderDelegate;

	/**
	 * Character encoding for the auto-generated application.xml file.
	 * 
	 * @parameter
	 */
	private String encoding = UTF_8;

	/**
	 * Directory where the application.xml file will be auto-generated.
	 * 
	 * @parameter expression="${project.build.directory}"
	 */
	private String generatedDescriptorLocation;

	/**
	 * @component
	 */
	private MavenProjectBuilder pb;

	/**
	 * @parameter default-value="${localRepository}"
	 */
	private ArtifactRepository localRepo;

	/**
	 * @parameter default-value="${project.remoteArtifactRepositories}"
	 */
	private List remoteRepos;

	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog()
				.debug(
						" ======= GenerateSharedLibraryDescriptorMojo settings =======");
		getLog().debug("workDirectory[" + workDirectory + "]");
		getLog().debug("generateJbiDescriptor[" + generateJbiDescriptor + "]");
		getLog().debug("name[" + name + "]");
		getLog().debug("description[" + description + "]");
		getLog().debug("encoding[" + encoding + "]");
		getLog().debug("generatedDescriptorLocation[" + generatedDescriptorLocation	+ "]");
		getLog().debug("version[" + version + "]");

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
	 */
	protected void generateJbiDescriptor() throws JbiPluginException {
		File outputDir = new File(generatedDescriptorLocation);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		File descriptor = new File(outputDir, JBI_DESCRIPTOR);

		List embeddedLibraries = new ArrayList();

		DependencyInformation info = new DependencyInformation();
		info.setFilename(LIB_DIRECTORY + "/" + project.getArtifactId() + "-"
				+ project.getVersion() + ".jar");
		info.setVersion(project.getVersion());
		info.setName(project.getArtifactId());
		info.setType("jar");
		embeddedLibraries.add(info);

		Set artifacts = project.getArtifacts();
		for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();

			// TODO: utilise appropriate methods from project builder
			ScopeArtifactFilter filter = new ScopeArtifactFilter(
					Artifact.SCOPE_RUNTIME);
			if (!artifact.isOptional() && filter.include(artifact)) {
				String type = artifact.getType();
				if ("jar".equals(type)) {
					info = new DependencyInformation();
					info.setFilename(LIB_DIRECTORY + "/" + artifact.getFile().getName());
					embeddedLibraries.add(info);
				}
			}
		}

		JbiSharedLibraryDescriptorWriter writer = new JbiSharedLibraryDescriptorWriter(
				encoding);
		writer.write(descriptor, name, description, version,
				classLoaderDelegate, embeddedLibraries);
	}
}
