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
package org.apache.servicemix.tooling.depends;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DefaultArtifactCollector;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Generates the dependencies properties file
 *
 * @version $Id: $
 * @goal generate-depends-file
 * @phase generate-resources
 * @requiresDependencyResolution test
 * @description Generates the dependencies properties file
 */
public class GenerateDependsFileMojo extends AbstractMojo {

    protected static final String SEPARATOR = "/";

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     *
     * @parameter expression='true'
     * @required
     */
    protected boolean recursive;
    
    /**
     *
     * @parameter expression='true'
     */
    protected boolean includeVersion;

    /**
     *
     * @parameter expression='true'
     */
    protected boolean includeClassifier;

    /**
     *
     * @parameter expression='true'
     */
    protected boolean includeScope;

    /**
     *
     * @parameter expression='true'
     */
    protected boolean includeType;

    /**
     * The file to generate
     *
     * @parameter default-value="${project.build.directory}/classes/META-INF/maven/dependencies.properties"
     */
    
    private File outputFile;
    
    /**
     * @parameter default-value="${localRepository}"
     */
    protected ArtifactRepository localRepo;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    protected List remoteRepos;

    /**
     * @parameter expression="${filterGroupIds}"
     */
    protected String[] filterGroupIds;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    protected ArtifactResolver resolver;

    protected ArtifactCollector collector = new DefaultArtifactCollector();

    /**
     * @component
     */
    protected ArtifactFactory factory;

    /**
     * @component
     */
    private BuildContext buildContext;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (buildContext.hasDelta("pom.xml")) {
            List<Dependency> dependencies = getDependencies();
            writeDependencies(dependencies);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Dependency> getDependencies() {
        List<Dependency> dependencies;
        if (!recursive) {
            dependencies = project.getDependencies();

            if ( filterGroupIds.length != 0 ) {
                Iterator<Dependency> dependencyIterator = dependencies.iterator();
                while (dependencyIterator.hasNext()) {
                    Dependency dependency = dependencyIterator.next();
                    if ( !doFilterGroupId( dependency.getGroupId() ) ) {
                        dependencyIterator.remove();
                    }
                }
            }

        } else {
            Set<Artifact> artifacts = project.getArtifacts();
            dependencies = new ArrayList<Dependency>();
            for (Artifact a : artifacts) {
                if( filterGroupIds.length == 0 || doFilterGroupId( a.getGroupId() ) ) {
                    dependencies.add(generateDependency( a ));
                }
            }
        }
        Collections.sort(dependencies, new DependencyComparator());
        return dependencies;
    }
    

    private void writeDependencies(List<Dependency> dependencies) throws MojoExecutionException {
        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            out = buildContext.newFileOutputStream(outputFile);
            PrintStream printer = new PrintStream(out);
            populateProperties(printer, dependencies);
            getLog().info("Created: " + outputFile);
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Unable to create dependencies file: " + e, e);
        } finally {
            safeClose(out);
        }
    }

    protected boolean doFilterGroupId( String groupId )
    {
        return Arrays.asList( filterGroupIds ).indexOf( groupId ) > -1;
    }

    protected Dependency generateDependency(Artifact a)
    {
        Dependency dep = new Dependency();
                   dep.setGroupId(a.getGroupId());
                   dep.setArtifactId(a.getArtifactId());
                   dep.setVersion(a.getBaseVersion());
                   dep.setClassifier(a.getClassifier());
                   dep.setType(a.getType());
                   dep.setScope(a.getScope());
        return dep;
    }

    protected void populateProperties(PrintStream out, List<Dependency> dependencies) {
        out.println("# Project dependencies generated by the Apache ServiceMix Maven Plugin");
        out.println("# Generated at: " + new Date());
        out.println();

        out.println("groupId = " + project.getGroupId());
        out.println("artifactId = " + project.getArtifactId());
        out.println("version = " + project.getVersion());
        out.println(project.getGroupId() + SEPARATOR + project.getArtifactId() + SEPARATOR + "version = " + project.getVersion());
        out.println();
        out.println("# dependencies");
        out.println();

        for (Dependency dependency : dependencies) {
            String prefix = dependency.getGroupId() + SEPARATOR + dependency.getArtifactId() + SEPARATOR;
            
            if( includeVersion )
                out.println(prefix + "version = " + dependency.getVersion());

            String classifier = dependency.getClassifier();
            if (classifier != null && includeClassifier) {
                out.println(prefix + "classifier = " + classifier);
            }

            if( includeType )
                out.println(prefix + "type = " + dependency.getType());
            if( includeScope )
                out.println(prefix + "scope = " + dependency.getScope());

            out.println();

            getLog().debug("Dependency: " + dependency + " classifier: " + classifier + " type: " + dependency.getType());
        }
    }
    
    private void safeClose(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                getLog().info("Failed to close: " + outputFile + ". Reason: " + e, e);
            }
        }
    }

}