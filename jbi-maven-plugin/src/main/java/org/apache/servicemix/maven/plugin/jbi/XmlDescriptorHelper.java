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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class XmlDescriptorHelper {

    private static final Log LOG = LogFactory.getLog(XmlDescriptorHelper.class);

    private XmlDescriptorHelper() {
    }
    
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
            LOG.debug("Got Element nodeName:" + element.getNodeName()
                    + " namespaceUri:" + element.getNamespaceURI()
                    + " localName:" + element.getLocalName());
            if (localPart.equals(element.getNodeName())) {
                return true;
            } else {
                // Compare the namespace URI and localname
                LOG.debug(namespaceUrl + "=" + element.getNamespaceURI()
                        + " is "
                        + namespaceUrl.equals(element.getNamespaceURI()));
                LOG.debug(localPart + "=" + element.getLocalName() + " is "
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
