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
import java.util.List;

import junit.framework.TestCase;

import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.packaging.Provides;

/**
 * Test case to exercise the default service file analyzer which uses a
 * service.xml file.
 */
public class JbiServiceFileAnalyzerTest extends TestCase
{
    private static final File SERVICES_FILE = new File("./src/test/resources/jbi-services.xml");

    /**
     * Simple test to retrieve and parse a jbi-services.xml file
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testParseServicesXml() throws Exception {
        JbiServiceFileAnalyzer analyzer = new JbiServiceFileAnalyzer();
        assertNotNull("analyzer should not be null", analyzer);

        analyzer.setJbiServicesFile(SERVICES_FILE);
        List<Consumes> consumes = analyzer.getConsumes();
        List<Provides> provides = analyzer.getProvides();
        assertNotNull("Consumes List should not be null", consumes);
        assertNotNull("Provides List should not be null", provides);
        assertTrue("Should be Empty consumes list", consumes.isEmpty());
        assertFalse("Provides List should not be empty", provides.isEmpty());
    }
}
