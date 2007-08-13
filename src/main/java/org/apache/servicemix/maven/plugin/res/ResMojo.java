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
package org.apache.servicemix.maven.plugin.res;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * Copy resources for the main source code to the main output directory.
 * 
 * @author <a href="mailto:gnodet [at] gmail.org">Guillaume Nodet</a>
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author Andreas Hoheneder
 * @version $Id$
 * @goal resources
 * @phase process-resources
 */
public class ResMojo extends AbstractMojo {

    /**
     * The character encoding scheme to be applied.
     * 
     * @parameter
     */
    private String encoding;

    /**
     * The list of resources we want to transfer.
     * 
     * @parameter
     * @required
     */
    private String inputDirectory;

    /**
     * The output directory into which to copy the resources.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project; // NOPMD
    
    /**
     * @parameter 
     */
    private String beginToken = "@{";

    /**
     * @parameter 
     */
    private String endToken = "}";

    /**
     * @parameter
     */
    private Map filters;

    public void execute() throws MojoExecutionException {
        if (encoding == null || encoding.length() < 1) {
            getLog().info("Using default encoding to copy filtered resources.");
        } else {
            getLog().info("Using '" + encoding + "' to copy filtered resources.");
        }
        if (!new File(inputDirectory).exists() || !new File(inputDirectory).isDirectory()) {
            getLog().warn("Input directory does not exists. Exiting plugin.");
            return;
        }
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(inputDirectory);
        scanner.setIncludes(new String[] {"**/**" });
        scanner.addDefaultExcludes();
        scanner.scan();
        List includedFiles = Arrays.asList(scanner.getIncludedFiles());

        getLog().info("Copying " + includedFiles.size() + " resource" + (includedFiles.size() > 1 ? "s" : ""));

        for (Iterator j = includedFiles.iterator(); j.hasNext();) {
            String name = (String) j.next();
            File source = new File(inputDirectory, name);
            File destinationFile = new File(outputDirectory, name);
            if (!destinationFile.getParentFile().exists()) {
                destinationFile.getParentFile().mkdirs();
            }
            try {
                copyFile(source, destinationFile);
            } catch (IOException e) {
                throw new MojoExecutionException("Error copying resource " + source, e);
            }
        }
    }

    private void copyFile(File from, final File to) throws IOException {
        FileUtils.FilterWrapper[] wrappers = new FileUtils.FilterWrapper[] {
            // support @token@
            new FileUtils.FilterWrapper() {
                public Reader getReader(Reader reader) {
                    return new InterpolationFilterReader(reader, filters, beginToken, endToken);
                }
            }
        };
        FileUtils.copyFile(from, to, encoding, wrappers);
    }
}
