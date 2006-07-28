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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Mojo used to build the jbi component installer file.
 * 
 * @author <a href="gnodet@apache.org">Guillaume Nodet</a>
 * @version $Id: GenerateApplicationXmlMojo.java 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal jbi-component
 * @phase package
 * @requiresDependencyResolution runtime
 * @description generates the component installer
 */
public class GenerateComponentMojo extends AbstractJbiMojo {

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
	 * @parameter expression="${project.artifactId}-${project.version}-installer.zip"
	 * @required
	 */
	private String installerName;

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
		getLog().debug("installerName[" + installerName + "]");
		getLog().debug("jbiSourceDirectory[" + jbiSourceDirectory + "]");

		try {

			createUnpackedInstaller();

			File installerFile = new File(outputDirectory, installerName);
			createArchive(installerFile);

			projectHelper.attachArtifact(project, "zip", "installer", new File(
					outputDirectory, installerName));

		} catch (JbiPluginException e) {
			throw new MojoExecutionException("Failed to create installer", e);
		}
	}

	private void createArchive(File installerFile) throws JbiPluginException {
		try {

			// generate war file
			getLog().info(
					"Generating installer " + installerFile.getAbsolutePath());
			MavenArchiver archiver = new MavenArchiver();
			archiver.setArchiver(jarArchiver);
			archiver.setOutputFile(installerFile);
			jarArchiver.addDirectory(workDirectory);
			if (jbiSourceDirectory.isDirectory()) {
				jarArchiver.addDirectory(jbiSourceDirectory, null,
						DirectoryScanner.DEFAULTEXCLUDES);
			}
			// create archive
			archiver.createArchive(getProject(), archive);

		} catch (ArchiverException e) {
			throw new JbiPluginException("Error creating assembly: "
					+ e.getMessage(), e);
		} catch (ManifestException e) {
			throw new JbiPluginException("Error creating assembly: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			throw new JbiPluginException("Error creating assembly: "
					+ e.getMessage(), e);
		} catch (DependencyResolutionRequiredException e) {
			throw new JbiPluginException("Error creating assembly: "
					+ e.getMessage(), e);
		}

	}

	private void createUnpackedInstaller() throws JbiPluginException {

		if (!workDirectory.isDirectory()) {
			if (!workDirectory.mkdirs()) {
				throw new JbiPluginException(
						"Unable to create work directory: " + workDirectory);
			}
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

		ScopeArtifactFilter filter = new ScopeArtifactFilter(
				Artifact.SCOPE_RUNTIME);		

		JbiResolutionListener listener = resolveProject();
		// print(listener.getRootNode(), "");

		Set sharedLibraries = new HashSet();
		Set includes = new HashSet();
		for (Iterator iter = project.getArtifacts().iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();
			if (!artifact.isOptional() && filter.include(artifact)) {
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
				String type = project != null ? project.getPackaging()
						: artifact.getType();
				if ("jbi-shared-library".equals(type)) {
					removeBranch(listener, artifact);
				} else if ("jar".equals(type)) {
					includes.add(artifact);
				}
			}
		}
		// print(listener.getRootNode(), "");

		for (Iterator iter = retainArtifacts(includes, listener).iterator(); iter
				.hasNext();) {
			Artifact artifact = (Artifact) iter.next();
			try {
				getLog().info("Including: " + artifact);
				FileUtils.copyFileToDirectory(artifact.getFile(), new File(
						workDirectory, LIB_DIRECTORY));
			} catch (IOException e) {
				throw new JbiPluginException("Unable to copy file "
						+ artifact.getFile(), e);
			}
		}
	}
}
