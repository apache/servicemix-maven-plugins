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
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.packaging.Provides;
import org.apache.servicemix.common.packaging.ServiceUnitAnalyzer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A dummy implementation of the ServiceUnitAnalyzer that allows you to generate
 * the consumes and provides from a simple XML file
 * 
 */
public class JbiServiceFileAnalyzer implements ServiceUnitAnalyzer {

	List consumes = new ArrayList();

	List provides = new ArrayList();

	public List getConsumes() {
		return consumes;
	}

	public List getProvides() {
		return provides;
	}

	public void init(File explodedServiceUnitRoot) {

	}

	public void setJbiServicesFile(File jbiServicesFile)
			throws MojoExecutionException {
		parseXml(jbiServicesFile);
	}

	private void parseXml(File jbiServicesFile) throws MojoExecutionException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(jbiServicesFile);

			Node servicesNode = doc.getFirstChild();
			if (servicesNode instanceof Element) {
				if (((Element) servicesNode).getNodeName().equals("services")) {
					// We will process the children
					Element servicesElement = (Element) servicesNode;
					NodeList children = servicesElement.getChildNodes();
					for (int i = 0; i < children.getLength(); i++) {
						if (children.item(i) instanceof Element) {
							Element childElement = (Element) children.item(i);
							if (childElement.getNodeName().equals("consumes")) {
								Consumes newConsumes = new Consumes();
								newConsumes
										.setEndpointName(getEndpointName(childElement));
								newConsumes
										.setInterfaceName(getInterfaceName(childElement));
								newConsumes
										.setServiceName(getServiceName(childElement));
								consumes.add(newConsumes);
							} else if (childElement.getNodeName().equals(
									"provides")) {
								Provides newProvides = new Provides();
								newProvides
										.setEndpointName(getEndpointName(childElement));
								newProvides
										.setInterfaceName(getInterfaceName(childElement));
								newProvides
										.setServiceName(getServiceName(childElement));
								provides.add(newProvides);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to parse "
					+ jbiServicesFile.getAbsolutePath());
		}

	}

	private QName getServiceName(Element childElement) {
		if (childElement.hasAttribute("service-name")) {
			String prefixAndLocalPart = childElement
					.getAttribute("service-name");
			String prefix = prefixAndLocalPart.substring(0, prefixAndLocalPart
					.indexOf(':'));
			String localPart = prefixAndLocalPart.substring(prefixAndLocalPart
					.indexOf(':'));
			return new QName(childElement.lookupNamespaceURI(prefix), localPart);
		}
		return null;
	}

	private QName getInterfaceName(Element childElement) {
		if (childElement.hasAttribute("interface-name")) {
			String prefixAndLocalPart = childElement
					.getAttribute("interface-name");
			String prefix = prefixAndLocalPart.substring(0, prefixAndLocalPart
					.indexOf(':'));
			String localPart = prefixAndLocalPart.substring(prefixAndLocalPart
					.indexOf(':'));
			return new QName(childElement.lookupNamespaceURI(prefix), localPart);
		}
		return null;
	}

	private String getEndpointName(Element childElement) {
		if (childElement.hasAttribute("endpoint-name")) {
			return childElement.getAttribute("endpoint-name");
		}
		return null;
	}
}
