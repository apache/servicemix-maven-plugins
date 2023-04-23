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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Generates the dependencies properties file
 */
@Mojo( name = "generate-depends-file", defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
       requiresDependencyResolution = ResolutionScope.TEST )
public class GenerateDependsFileMojo extends AbstractMojo {

    protected static final String SEPARATOR = "/";

    /**
     * The maven project.
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    protected MavenProject project;

    @Parameter( required = true, defaultValue = "true" )
    protected boolean recursive;

    @Parameter( defaultValue = "true" )
    protected boolean includeVersion;

    @Parameter( defaultValue = "true" )
    protected boolean includeClassifier;

    @Parameter( defaultValue = "true" )
    protected boolean includeScope;

    @Parameter( defaultValue = "true" )
    protected boolean includeType;

    /**
     * The file to generate
     */
    @Parameter( defaultValue = "${project.build.directory}/classes/META-INF/maven/dependencies.properties" )
    private File outputFile;

    @Parameter( defaultValue = "${filterGroupIds}" )
    protected String[] filterGroupIds;

    @Component
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
