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

import junit.framework.TestCase;

import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.packaging.Provides;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JbiServiceUnitDescriptorWriterTest extends TestCase {

	private static final String ENCODING_UTF8 = "UTF-8";
	private static final String ENCODING_ISO88591 = "ISO-8859-1";
	private static final String JBI_NAMESPACE = "http://java.sun.com/xml/ns/jbi";

	//TODO: how to fetch the build directory ('./target') from Maven property?
	private String generatedDescriptorLocation = "./target/test-outputs";

	private File outputDir;

	protected void setUp() throws Exception {
		super.setUp();

		this.outputDir = new File(generatedDescriptorLocation);
		if (!this.outputDir.exists()) {
			this.outputDir.mkdirs();
		}
	}

	public void testUTF8EncodingWrite()
			throws Exception {
		File descriptor = new File(outputDir, "jbi-su-UTF8.xml");

		String xmlEncoding = ENCODING_UTF8;
		writeDescriptor(descriptor, xmlEncoding);
		verifyDescriptor(descriptor, xmlEncoding);
	}

	public void testISO88591EncodingWrite()
			throws Exception {
		File descriptor = new File(outputDir, "jbi-su-ISO88591.xml");

		String xmlEncoding = ENCODING_ISO88591;
		writeDescriptor(descriptor, xmlEncoding);
		verifyDescriptor(descriptor, xmlEncoding);
	}

	private void writeDescriptor(File descriptor, String encoding)
			throws JbiPluginException {
		List consumes = new ArrayList();
		List provides = new ArrayList();

		QName serviceName = new QName("http://test.com/encoding", "abcåäö");

		Consumes newConsumes = new Consumes();
		newConsumes.setServiceName(serviceName);
		newConsumes.setEndpointName("consumeråäö");
		consumes.add(newConsumes);

		Provides newProvides = new Provides();
		newProvides.setServiceName(serviceName);
		newProvides.setEndpointName("provideråäö");
		provides.add(newProvides);

		JbiServiceUnitDescriptorWriter writer = new JbiServiceUnitDescriptorWriter(
				encoding);
		writer.write(descriptor, false, "name", "description", new ArrayList(), consumes,
				provides);
	}

	private void verifyDescriptor(File descriptor, String expectedXmlEncoding) throws Exception {
		Document doc = getDocument(descriptor);
		assertEquals(doc.getXmlEncoding(), expectedXmlEncoding);

		Element serviceElement = getServicesElement(doc);

		List consumes = getConsumes(serviceElement);
		Consumes con = (Consumes) consumes.get(0);
		assertEquals(con.getServiceName().getLocalPart(), "abcåäö");
		assertEquals(con.getEndpointName(), "consumeråäö");

		List provides = getProvides(serviceElement);
		Provides prov = (Provides) provides.get(0);
		assertEquals(prov.getServiceName().getLocalPart(), "abcåäö");
		assertEquals(prov.getEndpointName(), "provideråäö");
	}

	private Document getDocument(File jbiServicesFile) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(jbiServicesFile);
		
		return doc;
	}
	
	private Element getServicesElement(Document doc) throws Exception {
		Node jbiNode = doc.getFirstChild();
		assertTrue(XmlDescriptorHelper.isElement(jbiNode, JBI_NAMESPACE, "jbi"));
		Node tmpNode = jbiNode.getFirstChild();
		while (true) {
			assertNotNull(tmpNode);
			if (XmlDescriptorHelper.isElement(tmpNode, JBI_NAMESPACE,
					"services")) {
				return (Element) tmpNode;
			} else {
				tmpNode = tmpNode.getNextSibling();
			}
		}
	}

	private List getConsumes(Element servicesElement) throws Exception {
		List consumes = new ArrayList();
		NodeList children = servicesElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				Element childElement = (Element) children.item(i);
				if (XmlDescriptorHelper.isElement(childElement, JBI_NAMESPACE,
						"consumes")) {
					Consumes newConsumes = new Consumes();
					newConsumes.setEndpointName(XmlDescriptorHelper
							.getEndpointName(childElement));
					newConsumes.setInterfaceName(XmlDescriptorHelper
							.getInterfaceName(childElement));
					newConsumes.setServiceName(XmlDescriptorHelper
							.getServiceName(childElement));
					consumes.add(newConsumes);
				}
			}
		}
		
		return consumes;
	}

	private List getProvides(Element servicesElement) throws Exception {
		List provides = new ArrayList();
		NodeList children = servicesElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				Element childElement = (Element) children.item(i);
				if (XmlDescriptorHelper.isElement(childElement, JBI_NAMESPACE,
						"provides")) {
					Provides newProvides = new Provides();
					newProvides.setEndpointName(XmlDescriptorHelper
							.getEndpointName(childElement));
					newProvides.setInterfaceName(XmlDescriptorHelper
							.getInterfaceName(childElement));
					newProvides.setServiceName(XmlDescriptorHelper
							.getServiceName(childElement));
					provides.add(newProvides);
				}
			}
		}
		
		return provides;
	}
}
