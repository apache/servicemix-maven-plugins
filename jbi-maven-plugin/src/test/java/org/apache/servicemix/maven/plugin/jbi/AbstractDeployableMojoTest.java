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

import junit.framework.TestCase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.servicemix.jbi.framework.AdminCommandsServiceMBean;
import org.apache.servicemix.jbi.management.task.JbiTask;

/**
 * Test cases for {@link AbstractDeployableMojo}
 */
public class AbstractDeployableMojoTest extends TestCase {

    private static final String CONTAINER_NAME = "someContainerName";
    private static final String HOST = "someHostName";
    private static final String JNDI_PATH = "someJndiPath";
    private static final String JMX_DOMAIN_NAME = "someJmxDomainName";
    private static final String PASSWORD = "somePassword";
    private static final Integer PORT = 1001;
    private static final String SERVER_PROTOCOL = "someProtocolName";
    private static final String SERVICE_URL = "someServiceUrl";
    private static final String USERNAME = "someUserName";

    public void testInitializeAntTask() {
        JbiTask task = new JbiTask() {
            @Override
            protected void doExecute(AdminCommandsServiceMBean adminCommandsServiceMBean) throws Exception {
                //graciously do nothing;
            }
        };

        AbstractDeployableMojo mojo = new AbstractDeployableMojo() {
            public void execute() throws MojoExecutionException, MojoFailureException {
                //graciously do nothing
            }
        };

        mojo.containerName = CONTAINER_NAME;
        mojo.environment = "someEnvironmentKey=someEnvironmentValue";
        mojo.host = HOST;
        mojo.jndiPath = JNDI_PATH;
        mojo.jmxDomainName = JMX_DOMAIN_NAME;
        mojo.password = PASSWORD;
        mojo.port = PORT.toString();
        mojo.serverProtocol = SERVER_PROTOCOL;
        mojo.serviceUrl = SERVICE_URL;
        mojo.username = USERNAME;

        mojo.initializeJbiTask(task);
        assertNotNull("Initialization should have injected the Ant project", task.getProject());
        assertEquals(CONTAINER_NAME, task.getContainerName());
        assertEquals(HOST, task.getHost());
        assertEquals(JNDI_PATH, task.getJndiPath());
        assertEquals(JMX_DOMAIN_NAME, task.getJmxDomainName());
        assertEquals(PASSWORD, task.getPassword());
        assertEquals(PORT, (Integer) task.getPort());
        assertEquals(SERVER_PROTOCOL, task.getServerProtocol());
        assertEquals(SERVICE_URL, task.getServiceUrl());
        assertEquals(USERNAME, task.getUsername());
    }

}
