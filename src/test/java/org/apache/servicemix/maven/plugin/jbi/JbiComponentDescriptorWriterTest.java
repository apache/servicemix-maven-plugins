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
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import junit.framework.TestCase;
import org.xml.sax.InputSource;

/**
 * Test cases for {@link org.apache.servicemix.maven.plugin.jbi.JbiComponentDescriptorWriter}
 */
public class JbiComponentDescriptorWriterTest extends TestCase {

    private static final String VERSION = "1.0";

    private static final NamespaceContext NAMESPACES = new NamespaceContext() {

        public String getNamespaceURI(String prefix) {
            return "http://java.sun.com/xml/ns/jbi";
        }

        public String getPrefix(String namespaceURI) {
            return "jbi";
        }

        public Iterator getPrefixes(String namespaceURI) {
            return Arrays.asList("jbi").iterator();
        }
    };

    private JbiComponentDescriptorWriter writer;
    private File file;
    private XPathFactory factory = XPathFactory.newInstance();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        writer = new JbiComponentDescriptorWriter(GenerateComponentDescriptorMojo.UTF_8);
        file = File.createTempFile(getClass().getName(), ".xml");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    public void testWrite() throws Exception {
        List<DependencyInformation> deps = new ArrayList<DependencyInformation>();
        deps.add(createJarDependency("simple-jar", "lib/simple-jar.jar"));
        deps.add(createSharedLibraryDependency("simple-sl"));

        writer.write(file,
                     "org.apache.servicemix.test.Component",
                     "org.apache.servicemix.test.Bootstrap",
                     "binding-component",
                     "servicemix-test",
                     "ServiceMix :: A Test Component",
                     "parent-first",
                     "parent-first",
                     deps);


        assertEquals("Should have a reference to the SL in the shared-library element",
                     "simple-sl", xpath("/jbi:jbi/jbi:component/jbi:shared-library"));
        assertEquals("1.0", xpath("/jbi:jbi/jbi:component/jbi:shared-library/@version"));

        assertEquals("Only the JAR should be on the component-class-path", "1",
                     xpath("count(/jbi:jbi/jbi:component/jbi:component-class-path/jbi:path-element)"));
        assertEquals("lib/simple-jar.jar",
                     xpath("/jbi:jbi/jbi:component/jbi:component-class-path/jbi:path-element"));
    }

    private String xpath(String expression) throws Exception {
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(NAMESPACES);

        InputSource source = new InputSource(new FileReader(file));

        return xpath.evaluate(expression, source);
    }

    private DependencyInformation createJarDependency(String name, String filename) {
        DependencyInformation dep = createDependency(name, "jar");
        dep.setFilename(filename);
        return dep;
    }    

    private DependencyInformation createSharedLibraryDependency(String name) {
        return createDependency(name, DependencyInformation.SHARED_LIBRARY_TYPE);
    }

    private DependencyInformation createDependency(String name, String type) {
        DependencyInformation dep = new DependencyInformation();
        dep.setName(name);
        dep.setType(type);
        dep.setVersion(VERSION);
        return dep;
    }
}
