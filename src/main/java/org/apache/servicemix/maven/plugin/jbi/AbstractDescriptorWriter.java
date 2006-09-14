package org.apache.servicemix.maven.plugin.jbi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.packaging.Provides;
import org.codehaus.plexus.util.xml.XMLWriter;

public class AbstractDescriptorWriter {

	protected void addStringAttribute(XMLWriter writer, String attributeName, String attributeValue) {
		if (attributeValue != null)
			writer.addAttribute(attributeName, attributeValue);
	}

	protected void addQNameAttribute(XMLWriter writer, String attributeName, QName attributeValue, Map namespaceMap) {		
		if (attributeValue != null) {
			StringBuffer attributeStringValue = new StringBuffer();
			attributeStringValue.append(namespaceMap.get(attributeValue
					.getNamespaceURI()));
			attributeStringValue.append(":");
			attributeStringValue.append(attributeValue.getLocalPart());
			writer.addAttribute(attributeName, attributeStringValue.toString());
		}
	
	}

	protected Map getNamespaceMap(List provides, List consumes) {
		Map namespaceMap = new HashMap();
		int namespaceCounter = 1;
		for (Iterator iterator = provides.iterator(); iterator.hasNext();) {
			Provides providesEntry = (Provides) iterator.next();
			resolveMapEntry(namespaceMap, providesEntry.getInterfaceName(),
					namespaceCounter);
			resolveMapEntry(namespaceMap, providesEntry.getServiceName(),
					namespaceCounter);
		}
	
		for (Iterator iterator = consumes.iterator(); iterator.hasNext();) {
			Consumes consumesEntry = (Consumes) iterator.next();
			resolveMapEntry(namespaceMap, consumesEntry.getInterfaceName(),
					namespaceCounter);
			resolveMapEntry(namespaceMap, consumesEntry.getServiceName(),
					namespaceCounter);
		}
	
		return namespaceMap;
	}

	private void resolveMapEntry(Map namespaceMap, QName qname, int namespaceCounter) {
		if ((qname != null)
				&& (!namespaceMap.containsKey(qname.getNamespaceURI()))) {
			if (qname.getPrefix() == null || qname.getPrefix().equals("") ) {
				namespaceMap.put(qname.getNamespaceURI(), "ns"
						+ namespaceCounter++);
			} else
				namespaceMap.put(qname.getNamespaceURI(), qname.getPrefix());
		}
	}

}