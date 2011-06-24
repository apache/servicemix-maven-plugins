/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
package org.apache.servicemix.docs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.servicemix.docs.confluence.ConfluenceConverter;

/**
 * Convert pages in Confluence wiki markup to Docbook syntax
 *
 * @goal confluence-to-docbook
 * @phase generate-resources
 */
public class ConfluenceToDocbookMojo extends AbstractMojo {

    /**
     * Location of the snippet cache
     * @parameter default-value="${basedir}/src/confluence"
     */
    protected File input;

    /**
     * Output directory for the Docbook sources
     * @parameter default-value="${project.build.directory}/docbkx/sources"
     */
    protected File output;

    /**
     * Filter to determine which files to convert 
     */
    private final FileFilter filter = new DefaultFileFilterImpl();

    private final ConfluenceConverter converter = new ConfluenceConverter();

    public void execute() throws MojoExecutionException {
        doConvert(input);
    }

    private void doConvert(File directory) throws MojoExecutionException {
        if (directory.exists() && directory.isDirectory()) {
            for (File file : directory.listFiles(filter)) {
                if (file.isDirectory()) {
                    doConvert(file);
                } else {
                    getLog().info("Creating DocBook from " + file.getAbsolutePath());
                    Reader reader = null;
                    Writer writer = null;
                    try {
                        String relativePath = FilenameUtils.getPath(file.getAbsolutePath().replace(input.getAbsolutePath(), ""));

                        File result = new File(output + File.separator + relativePath, file.getName() + ".xml");
                        if (!result.getParentFile().exists()) {
                            result.getParentFile().mkdirs();
                        }

                        reader = new FileReader(file);
                        writer = new FileWriter(result);

                        converter.convert(reader, writer);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Unable to convert " + file, e);
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                // ignore this
                            }
                        }
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e) {
                                // ignore this
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Default FileFilter implementation to filter out hidden files (e.g. .svn)
     */
    private class DefaultFileFilterImpl implements FileFilter {
        public boolean accept(File file) {
            return !file.getName().startsWith(".");  
        }
    }
}