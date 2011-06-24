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

import org.apache.servicemix.jbi.management.task.JbiTask;
import org.apache.tools.ant.Project;

public abstract class AbstractDeployableMojo extends AbstractJbiMojo {

    /**
     * @parameter default-value="rmi"
     */
    protected String serverProtocol;

    /**
     * @parameter default-value="localhost" expression="${host}"
     */
    protected String host;

    /**
     * @parameter default-value="ServiceMix" expression="${containerName}
     */
    protected String containerName;

    /**
     * @parameter default-value="org.apache.servicemix"
     */
    protected String jmxDomainName;

    /**
     * @parameter default-value="1099" expression="${port}"
     */
    protected String port;

    /**
     * @parameter default-value="/jmxrmi"
     */
    protected String jndiPath;

    /**
     * @parameter default-value="smx" expression="${username}"
     */
    protected String username;

    /**
     * @parameter default-value="smx" expression="${password}"
     */
    protected String password;

    /**
     * @parameter expression="${serviceUrl}
     */
    protected String serviceUrl;

    /**
     * @parameter expression=${environment}
     */
    protected String environment;

    protected JbiTask initializeJbiTask(JbiTask task) {

        Project antProject = new Project();
        antProject.init();
        task.setProject(antProject);

        task.setContainerName(containerName);
        task.setHost(host);
        task.setServerProtocol(serverProtocol);
        task.setJmxDomainName(jmxDomainName);
        task.setPort(Integer.parseInt(port));
        task.setJndiPath(jndiPath);
        task.setUsername(username);
        task.setPassword(password);
        task.setServiceUrl(serviceUrl);
        task.setEnvironment(environment);

        task.setTaskName("JBITask");
        task.setTaskType("JBITask");
        return task;
    }

}
