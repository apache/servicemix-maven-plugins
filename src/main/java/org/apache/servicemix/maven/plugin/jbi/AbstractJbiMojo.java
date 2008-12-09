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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.servicemix.maven.plugin.jbi.JbiResolutionListener.Node;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

public abstract class AbstractJbiMojo extends AbstractMojo {

    public static final String META_INF = "META-INF";

    public static final String JBI_DESCRIPTOR = "jbi.xml";

    public static final String LIB_DIRECTORY = "lib";

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+(\\.\\d+(\\.\\d+)?)?)-");

    private static final String[] VERSION_COMPLETERS = new String[] {".0.0", ".0" };

    /**
     * Maven ProjectHelper
     * 
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Directory that resources are copied to during the build.
     * 
     * @parameter expression="${project.build.directory}/${project.artifactId}-${project.version}-installer"
     * @required
     */
    protected File workDirectory;

    /**
     * @component
     */
    protected MavenProjectBuilder projectBuilder;

    /**
     * @parameter default-value="${localRepository}"
     */
    protected ArtifactRepository localRepo;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    protected List remoteRepos;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    protected ArtifactResolver resolver;

    /**
     * @component
     * @required
     * @readonly
     */
    protected ArtifactCollector collector;

    /**
     * @component
     */
    protected ArtifactFactory factory;

    protected MavenProject getProject() {
        return project;
    }

    protected File getWorkDirectory() {
        return workDirectory;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    protected void removeBranch(JbiResolutionListener listener, Artifact artifact) {
        Node n = listener.getNode(artifact);
        if (n != null) {
            for (Node parent : n.getParents()) {
                parent.getChildren().remove(n);
            }
		}
    }

    protected void removeChildren(JbiResolutionListener listener, Artifact artifact) {
        Node n = listener.getNode(artifact);
        n.getChildren().clear();
    }

    protected Set<Artifact> getArtifacts(Node n, Set<Artifact> s) {
        if (s.add(n.getArtifact())) {
            for (Node c : n.getChildren()) {
                getArtifacts(c, s);
            }
        }
        return s;
    }

    protected void excludeBranch(Node n, Set<Artifact> excludes) {
        excludes.add(n.getArtifact());
        for (Node c : n.getChildren()) {
            excludeBranch(c, excludes);
        }
    }

    protected void print(Node rootNode) {
        for (Artifact a : getArtifacts(rootNode, new HashSet<Artifact>())) {
            getLog().info(" " + a);
        }
    }

    protected void print(Node rootNode, String pfx) {
        getLog().info(pfx + rootNode.getArtifact());
        for (Node child : rootNode.getChildren()) {
            print(child, " " + pfx);
        }
    }

    protected void pruneTree(Node node, Set<Artifact> excludes) {
        for (Iterator<Node> iter = node.getChildren().iterator(); iter.hasNext();) {
            Node child = iter.next();
            if (child.getArtifact().isOptional() ||
                    (!child.getScope().equals(Artifact.SCOPE_COMPILE) && !child.getScope().equals(Artifact.SCOPE_RUNTIME)) ||
                    excludes.contains(child.getArtifact())) {
                iter.remove();
            } else {
                pruneTree(child, excludes);
            }
        }
    }

    protected Set<Artifact> retainArtifacts(Set<Artifact> includes, JbiResolutionListener listener) {
        Set<Artifact> finalIncludes = new HashSet<Artifact>();
        Set<Artifact> filteredArtifacts = getArtifacts(listener.getRootNode(), new HashSet<Artifact>());
        for (Artifact artifact : includes) {
            for (Artifact filteredArtifact : filteredArtifacts) {
                if (filteredArtifact.getArtifactId().equals(
                        artifact.getArtifactId())
                        && filteredArtifact.getType()
                                .equals(artifact.getType())
                        && filteredArtifact.getGroupId().equals(
                                artifact.getGroupId())) {
                    if (!filteredArtifact.getVersion().equals(
                            artifact.getVersion())) {
                        getLog()
                                .warn(
                                        "Resolved artifact "
                                                + artifact
                                                + " has a different version from that in dependency management "
                                                + filteredArtifact
                                                + ", overriding dependency management");
                    }
                    finalIncludes.add(artifact);
                }
            }

        }
        return finalIncludes;
    }


    protected JbiResolutionListener resolveProject() {
        Map managedVersions = null;
        try {
            managedVersions = createManagedVersionMap(project.getId(), project.getDependencyManagement());
        } catch (ProjectBuildingException e) {
            getLog().error("An error occurred while resolving project dependencies.", e);
        }
        JbiResolutionListener listener = new JbiResolutionListener();
        listener.setLog(getLog());
        try {
            collector.collect(project.getDependencyArtifacts(), project
                    .getArtifact(), managedVersions, localRepo, remoteRepos,
                    artifactMetadataSource, null, Collections
                            .singletonList(listener));
        } catch (ArtifactResolutionException e) {
            getLog().error(
                    "An error occurred while resolving project dependencies.",
                    e);
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Dependency graph");
            getLog().debug("================");
            print(listener.getRootNode());
            getLog().debug("================");
        }
        return listener;
    }

    protected Map createManagedVersionMap(String projectId,
            DependencyManagement dependencyManagement) throws ProjectBuildingException {
        Map map;
        if (dependencyManagement != null
                && dependencyManagement.getDependencies() != null) {
            map = new HashMap();
            for (Iterator i = dependencyManagement.getDependencies().iterator(); i
                    .hasNext();) {
                Dependency d = (Dependency) i.next();

                try {
                    VersionRange versionRange = VersionRange
                            .createFromVersionSpec(d.getVersion());
                    Artifact artifact = factory.createDependencyArtifact(d
                            .getGroupId(), d.getArtifactId(), versionRange, d
                            .getType(), d.getClassifier(), d.getScope());
                    map.put(d.getManagementKey(), artifact);
                } catch (InvalidVersionSpecificationException e) {
                    throw new ProjectBuildingException(projectId,
                            "Unable to parse version '" + d.getVersion()
                                    + "' for dependency '"
                                    + d.getManagementKey() + "': "
                                    + e.getMessage(), e);
                }
            }
        } else {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    /**
     * Set up a classloader for the execution of the main class.
     * 
     * @return
     * @throws MojoExecutionException
     */
    protected URLClassLoader getClassLoader() throws MojoExecutionException {
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
                        "Adding artifact: " + classPathElement.getFile()
                                + " to classpath");
                urls.add(classPathElement.getFile().toURL());
            }
            URLClassLoader appClassloader = new URLClassLoader((URL[]) urls
                    .toArray(new URL[urls.size()]), this.getClass().getClassLoader());
            return appClassloader;
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(
                    "Error during setting up classpath", e);
        }
    }

    protected Manifest createManifest() throws ManifestException {
        Manifest manifest = new Manifest();
        //manifest.getMainSection().addConfiguredAttribute(new Manifest.Attribute("Bundle-Name", project.getName()));
        //manifest.getMainSection().addConfiguredAttribute(new Manifest.Attribute("Bundle-SymbolicName", project.getArtifactId()));
        //manifest.getMainSection().addConfiguredAttribute(new Manifest.Attribute(
            //"Bundle-Version", fixBundleVersion(project.getVersion())));
        return manifest;
    }

    public static String fixBundleVersion(String version) {
        // Maven uses a '-' to separate the version qualifier, while
        // OSGi uses a '.', so we need to convert the first '-' to a
        // '.' and fill in any missing minor or micro version
        // components if necessary.
        final Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.lookingAt()) {
            return version;
        }
        // Leave extra space for worst-case additional insertion:
        final StringBuffer sb = new StringBuffer(version.length() + 4);
        sb.append(matcher.group(1));
        if (null == matcher.group(3)) {
            final int count = null != matcher.group(2) ? 2 : 1;
            sb.append(VERSION_COMPLETERS[count - 1]);
        }
        sb.append('.');
        sb.append(version.substring(matcher.end(), version.length()));
        return sb.toString();
    }

}
