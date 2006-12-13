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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.servicemix.maven.plugin.jbi.GenerateServiceAssemblyDescriptorMojo.Connection;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Helper that can be used to write the jbi.xml for a service assembly
 * 
 */
public class JbiServiceAssemblyDescriptorWriter extends
		AbstractDescriptorWriter {

	private final String encoding;

	public JbiServiceAssemblyDescriptorWriter(String encoding) {
		this.encoding = encoding;
	}

	public void write(File descriptor, String name, String description,
			List uris, List connections) throws JbiPluginException {
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

		writer.startElement("service-assembly");

		writer.startElement("identification");
		writer.startElement("name");
		writer.writeText(name);
		writer.endElement();
		writer.startElement("description");
		writer.writeText(description);
		writer.endElement();
		writer.endElement();

		for (Iterator it = uris.iterator(); it.hasNext();) {
			DependencyInformation serviceUnitInfo = (DependencyInformation) it
					.next();
			writeServiceUnit(writer, serviceUnitInfo);

		}

		if (!connections.isEmpty()) {
			writer.startElement("connections");

			Map namespaceMap = buildNamespaceMap(connections);
			for (Iterator it = connections.iterator(); it.hasNext();) {
				GenerateServiceAssemblyDescriptorMojo.Connection connection = (GenerateServiceAssemblyDescriptorMojo.Connection) it
						.next();
				writeConnection(namespaceMap, writer, connection);

			}
			writer.endElement();
		}

		writer.endElement();
		writer.endElement();

		close(w);
	}

	private Map buildNamespaceMap(List connections) {
		List consumes = new ArrayList();
		List provides = new ArrayList();
		for (Iterator it = connections.iterator(); it.hasNext();) {
			GenerateServiceAssemblyDescriptorMojo.Connection connection = (GenerateServiceAssemblyDescriptorMojo.Connection) it
					.next();
			consumes.add(connection.getConsumes());
			provides.add(connection.getProvides());
		}

		return getNamespaceMap(provides, consumes);

	}

	private void writeConnection(Map namespaceMap, XMLWriter writer,
			Connection connection) {
		writer.startElement("connection");
		if (connection.getConsumes() != null) {
			writer.startElement("consumer");
			addQNameAttribute(writer, "interface-name", connection
					.getConsumes().getInterfaceName(), namespaceMap);
			addQNameAttribute(writer, "service-name", connection.getConsumes()
					.getServiceName(), namespaceMap);
			addStringAttribute(writer, "endpoint-name", connection
					.getConsumes().getEndpointName());
			writer.endElement();
		}
		if (connection.getProvides() != null) {
			writer.startElement("provider");
			addQNameAttribute(writer, "interface-name", connection
					.getProvides().getInterfaceName(), namespaceMap);
			addQNameAttribute(writer, "service-name", connection.getProvides()
					.getServiceName(), namespaceMap);
			addStringAttribute(writer, "endpoint-name", connection
					.getProvides().getEndpointName());
			writer.endElement();
		}

		writer.endElement();

	}

	private void writeServiceUnit(XMLWriter writer,
			DependencyInformation serviceUnitInfo) throws JbiPluginException {
		writer.startElement("service-unit");
		writer.startElement("identification");
		writer.startElement("name");
		writer.writeText(serviceUnitInfo.getName());
		writer.endElement();
		writer.startElement("description");
        if (serviceUnitInfo.getDescription() != null) {
            writer.writeText(serviceUnitInfo.getDescription());
        } else {
            writer.writeText(serviceUnitInfo.getName());
        }
		writer.endElement();
		writer.endElement();

		writer.startElement("target");
		writer.startElement("artifacts-zip");
		writer.writeText(serviceUnitInfo.getFilename());
		writer.endElement();

		writer.startElement("component-name");
		writer.writeText(serviceUnitInfo.getComponent());
		writer.endElement();

		writer.endElement();

		writer.endElement();
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
