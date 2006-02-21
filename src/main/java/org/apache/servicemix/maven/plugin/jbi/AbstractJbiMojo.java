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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

public abstract class AbstractJbiMojo extends AbstractMojo {

    public static final String META_INF = "META-INF";

    public static final String JBI_DESCRIPTOR = "jbi.xml";
    
    public static final String LIB_DIRECTORY = "lib";
    
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
    
    protected MavenProject getProject()
    {
        return project;
    }

    protected File getWorkDirectory()
    {
        return workDirectory;
    }

	public MavenProjectHelper getProjectHelper() {
		return projectHelper;
	}

}
