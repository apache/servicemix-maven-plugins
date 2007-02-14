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
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Helper that is used to write the jbi.xml for a shared library
 * 
 */
public class JbiSharedLibraryDescriptorWriter {

	private final String encoding;

	public JbiSharedLibraryDescriptorWriter(String encoding) {
		this.encoding = encoding;
	}

	public void write(File descriptor, String name, String description,
			String version, String classLoaderDelegation, List uris)
			throws JbiPluginException {
        PrintWriter w;
		try {
			w = new PrintWriter(descriptor, encoding);
		} catch (IOException ex) {
			throw new JbiPluginException("Exception while opening file["
					+ descriptor.getAbsolutePath() + "]", ex);
		}

		XMLWriter writer = new PrettyPrintXMLWriter(w, encoding, null);
		writer.startElement("jbi");
		writer.addAttribute("xmlns", "http://java.sun.com/xml/ns/jbi");
		writer.addAttribute("version", "1.0");

		writer.startElement("shared-library");
		writer.addAttribute("class-loader-delegation", classLoaderDelegation);
		writer.addAttribute("version", version);

		writer.startElement("identification");
		writer.startElement("name");
		writer.writeText(name);
		writer.endElement();
		writer.startElement("description");
		writer.writeText(description);
		writer.endElement();
		writer.endElement();

		writer.startElement("shared-library-class-path");
		for (Iterator it = uris.iterator(); it.hasNext();) {
			DependencyInformation dependency = (DependencyInformation) it.next();
			writer.startElement("path-element");
			writer.writeText(dependency.getFilename());
			writer.endElement();			
		}
		writer.endElement();

		writer.endElement();
		writer.endElement();

		close(w);
	}

	private void close(Writer closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception e) {
				// TODO: warn
			}
		}
	}

}
