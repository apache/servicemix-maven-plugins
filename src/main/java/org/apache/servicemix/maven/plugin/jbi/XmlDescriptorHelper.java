package org.apache.servicemix.maven.plugin.jbi;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XmlDescriptorHelper {

	public static QName getServiceName(Element childElement) {
		if (childElement.hasAttribute("service-name")) {
			String prefixAndLocalPart = childElement
					.getAttribute("service-name");
			String prefix = prefixAndLocalPart.substring(0, prefixAndLocalPart
					.indexOf(':'));
			String localPart = prefixAndLocalPart.substring(prefixAndLocalPart
					.indexOf(':') + 1);
			return new QName(childElement.lookupNamespaceURI(prefix), localPart);
		}
		return null;
	}

	public static QName getInterfaceName(Element childElement) {
		if (childElement.hasAttribute("interface-name")) {
			String prefixAndLocalPart = childElement
					.getAttribute("interface-name");
			String prefix = prefixAndLocalPart.substring(0, prefixAndLocalPart
					.indexOf(':'));
			String localPart = prefixAndLocalPart.substring(prefixAndLocalPart
					.indexOf(':') + 1);
			return new QName(childElement.lookupNamespaceURI(prefix), localPart);
		}
		return null;
	}

	public static boolean isElement(Node node, String namespaceUrl,
			String localPart) {
		if (node instanceof Element) {
			Element element = (Element) node;
			System.out.println("Got Element nodeName:" + element.getNodeName()
					+ " namespaceUri:" + element.getNamespaceURI()
					+ " localName:" + element.getLocalName());
			if (localPart.equals(element.getNodeName()))
				return true;
			else {
				// Compare the namespace URI and localname
				System.out.println(namespaceUrl + "="
						+ element.getNamespaceURI() + " is "
						+ namespaceUrl.equals(element.getNamespaceURI()));
				System.out.println(localPart + "="
						+ element.getLocalName() + " is "
						+ localPart.equals(element.getLocalName()));
				if ((namespaceUrl.equals(element.getNamespaceURI()))
						&& (localPart.equals(element.getLocalName()))) {
					return true;
				}
			}
		}
		return false;
	}

	public static String getEndpointName(Element childElement) {
		if (childElement.hasAttribute("endpoint-name")) {
			return childElement.getAttribute("endpoint-name");
		}
		return null;
	}

}
