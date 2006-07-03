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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.servicemix.jbi.management.task.DeployServiceAssemblyTask;
import org.apache.servicemix.jbi.management.task.InstallComponentTask;
import org.apache.servicemix.jbi.management.task.InstallSharedLibraryTask;
import org.apache.servicemix.jbi.management.task.StartComponentTask;

/**
 * A Mojo that can take any project and determine its JBI dependencies and then
 * install it and its dependencies using the JBI deployment tasks
 * 
 * @author <a href="pdodds@apache.org">Philip Dodds</a>
 * @version $Id: GenerateComponentDescriptorMojo 314956 2005-10-12 16:27:15Z
 *          brett $
 * @goal projectDeploy
 * @requiresDependencyResolution runtime
 * @description Starts a ServiceMix instance and installs the project (and all
 *              dependencies) to it
 */
public class JbiProjectDeployerMojo extends AbstractDeployableMojo {

	private List deploymentTypes;

	/**
	 * @parameter default-value="${project}"
	 */
	private MavenProject project;

	/**
	 * @component
	 */
	private MavenProjectBuilder pb;

	/**
	 * @component
	 */
	private ArtifactFactory af;

	/**
	 * @parameter default-value="${localRepository}"
	 */
	private ArtifactRepository localRepo;

	/**
	 * @parameter default-value="${project.remoteArtifactRepositories}"
	 */
	private List remoteRepos;

	public void execute() throws MojoExecutionException, MojoFailureException {
		deployProject();
	}

	protected void deployProject() throws MojoExecutionException {
		if (!getDeployablePackagingTypes().contains(project.getPackaging())) {
			throw new MojoExecutionException(
					"Project must be of packaging type ["
							+ getDeployablePackagingTypes() + "]");
		}

		try {
			Stack dependencies = new Stack();
			dependencies.add(resolveDeploymentPackage(project, project
					.getArtifact()));
			ArrayList artifactList = new ArrayList();
			artifactList.addAll(project.getArtifacts());
			Collections.sort(artifactList, new ArtifactDepthComparator());
			for (Iterator iter = artifactList.iterator(); iter.hasNext();) {
				Artifact artifact = (Artifact) iter.next();
				getLog().info(
						"For Project " + project.getArtifactId()
								+ " -> Resolve artifact " + artifact);
				resolveArtifact(artifact, dependencies);
			}

			getLog()
					.info(
							"------------------ Deployment Analysis --------------------");
			getLog().info(
					project.getName() + " has " + (dependencies.size() - 1)
							+ " child dependencies");

			for (Iterator iterator = dependencies.iterator(); iterator
					.hasNext();) {
				getLog().info(" - " + iterator.next());
			}

			getLog()
					.info(
							"-----------------------------------------------------------");
			while (!dependencies.empty()) {
				deployDependency((JbiDeployableArtifact) dependencies.pop());
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to deploy project, "
					+ e.getMessage(), e);
		}

	}

	private void deployDependency(JbiDeployableArtifact jbiDeployable) {

		getLog().info(
				"Deploying " + jbiDeployable.getType() + " from "
						+ jbiDeployable.getFile());
		if ("jbi-shared-library".equals(jbiDeployable.getType())) {
			InstallSharedLibraryTask componentTask = new InstallSharedLibraryTask();
			initializeJbiTask(componentTask);
			componentTask.setFile(jbiDeployable.getFile());
			componentTask.execute();
		} else if ("jbi-service-assembly".equals(jbiDeployable.getType())) {
			DeployServiceAssemblyTask componentTask = new DeployServiceAssemblyTask();
			initializeJbiTask(componentTask);
			componentTask.setFile(jbiDeployable.getFile());
			componentTask.execute();
		}
		if ("jbi-component".equals(jbiDeployable.getType())) {
			InstallComponentTask componentTask = new InstallComponentTask();
			initializeJbiTask(componentTask);
			componentTask.setFile(jbiDeployable.getFile());
			componentTask.execute();

			StartComponentTask startTask = new StartComponentTask();
			initializeJbiTask(startTask);
			startTask.setName(jbiDeployable.getName());
			startTask.execute();
		}

	}

	private List getDeployablePackagingTypes() {
		if (deploymentTypes == null) {
			deploymentTypes = new ArrayList();
			deploymentTypes.add("jbi-shared-library");
			deploymentTypes.add("jbi-service-assembly");
			deploymentTypes.add("jbi-component");
		}
		return deploymentTypes;
	}

	private Collection resolveArtifact(Artifact artifact, Stack dependencies)
			throws ArtifactResolutionException, ArtifactNotFoundException {
		MavenProject project = null;
		try {
			project = pb.buildFromRepository(artifact, remoteRepos, localRepo,
					true);
		} catch (ProjectBuildingException e) {
			getLog().warn(
					"Unable to determine packaging for dependency : "
							+ artifact.getArtifactId() + " assuming jar");
		}

		if (project != null) {
			getLog().debug(
					"Resolved artifact to project " + project.getArtifactId());
			if (getDeployablePackagingTypes().contains(project.getPackaging())) {
				getLog().debug(
						"Checking for dependency from project "
								+ project.getArtifactId());
				JbiDeployableArtifact deployableArtifact = resolveDeploymentPackage(
						project, artifact);
				if (!dependencies.contains(deployableArtifact)) {
					getLog().debug(
							"Adding dependency from project "
									+ project.getArtifactId());
					dependencies.push(deployableArtifact);
				}
			}
		}
		return dependencies;
	}

	private JbiDeployableArtifact resolveDeploymentPackage(
			MavenProject project, Artifact artifact)
			throws ArtifactResolutionException, ArtifactNotFoundException {
		Artifact jbiArtifact = af.createArtifactWithClassifier(artifact
				.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
				"zip", getExtension(project));
		resolver.resolve(jbiArtifact, remoteRepos, localRepo);
		return new JbiDeployableArtifact(project.getArtifactId(), project
				.getPackaging(), jbiArtifact.getFile().getAbsolutePath());
	}

	private String getExtension(MavenProject project2) {
		if (project2.getPackaging().equals("jbi-service-assembly"))
			return "";
		else
			return "installer";
	}

	private class ArtifactDepthComparator implements Comparator {

		public int compare(Object arg0, Object arg1) {
			int size1 = ((Artifact) arg0).getDependencyTrail().size();
			int size2 = ((Artifact) arg1).getDependencyTrail().size();
			if (size1 == size2)
				return 0;
			if (size1 > size2)
				return 1;
			else
				return -1;
		}

	}

	private class JbiDeployableArtifact {
		private String file;

		private String type;

		private String name;

		public String getName() {
			return name;
		}

		public JbiDeployableArtifact(String name, String type, String file) {
			this.name = name;
			this.file = file;
			this.type = type;
		}

		public String getFile() {
			return file;
		}

		public String getType() {
			return type;
		}

		public String toString() {
			return type + " : " + file;
		}

		public boolean equals(Object obj) {
			if (obj instanceof JbiDeployableArtifact)
				return ((JbiDeployableArtifact) obj).toString().equals(
						this.toString());
			else
				return false;
		}
	}
}
