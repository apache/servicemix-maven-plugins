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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.packaging.Provides;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Helper that is used to write the jbi.xml for a service unit
 * 
 */
public class JbiServiceUnitDescriptorWriter extends AbstractDescriptorWriter {

	private final String encoding;

	public JbiServiceUnitDescriptorWriter(String encoding) {
		this.encoding = encoding;
	}

	public void write(File descriptor, String name, String description,
			List uris, List consumes, List provides) throws JbiPluginException {
		FileWriter w;
		try {
			w = new FileWriter(descriptor);
		} catch (IOException ex) {
			throw new JbiPluginException("Exception while opening file["
					+ descriptor.getAbsolutePath() + "]", ex);
		}

		XMLWriter writer = new PrettyPrintXMLWriter(w, encoding, null);
		writer.startElement("jbi");
		writer.addAttribute("xmlns", "http://java.sun.com/xml/ns/jbi");
		writer.addAttribute("version", "1.0");

		writer.startElement("services");

		// We need to get all the namespaces into a hashmap so we
		// can get the QName output correctly
		Map namespaceMap = getNamespaceMap(provides, consumes);

		// Set-up the namespaces
		for (Iterator iterator = namespaceMap.keySet().iterator(); iterator
				.hasNext();) {
			String key = (String) iterator.next();
			StringBuffer namespaceDecl = new StringBuffer();
			namespaceDecl.append("xmlns:");
			namespaceDecl.append(namespaceMap.get(key));
			writer.addAttribute(namespaceDecl.toString(), key);
		}

		// Put in the provides
		for (Iterator iterator = provides.iterator(); iterator.hasNext();) {
			Provides providesEntry = (Provides) iterator.next();
			writer.startElement("provides");
			addQNameAttribute(writer, "interface-name", providesEntry
					.getInterfaceName(), namespaceMap);
			addQNameAttribute(writer, "service-name", providesEntry
					.getServiceName(), namespaceMap);
			addStringAttribute(writer, "endpoint-name", providesEntry
					.getEndpointName());
			writer.endElement();
		}

		// Put in the consumes
		for (Iterator iterator = consumes.iterator(); iterator.hasNext();) {
			Consumes consumesEntry = (Consumes) iterator.next();
			writer.startElement("consumes");
			addQNameAttribute(writer, "interface-name", consumesEntry
					.getInterfaceName(), namespaceMap);
			addQNameAttribute(writer, "service-name", consumesEntry
					.getServiceName(), namespaceMap);
			addStringAttribute(writer, "endpoint-name", consumesEntry
					.getEndpointName());

			// TODO Handling of LinkType?

			writer.endElement();
		}

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
