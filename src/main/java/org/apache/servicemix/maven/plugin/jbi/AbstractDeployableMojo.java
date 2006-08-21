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
	private String serverProtocol;

	/**
	 * @parameter default-value="localhost"
	 */
	private String host;

	/**
	 * @parameter default-value="ServiceMix"
	 */
	private String containerName;

	/**
	 * @parameter default-value="org.apache.servicemix"
	 */
	private String jmxDomainName;

	/**
	 * @parameter default-value="1099"
	 */
	protected String port;

	/**
	 * @parameter default-value="/jmxrmi"
	 */
	private String jndiPath;

	/**
	 * @parameter default-value=""
	 */
	private String username;

	/**
	 * @parameter default-value=""
	 */
	private String password;

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
		
		task.setTaskName("JBITask");		
		task.setTaskType("JBITask");
		return task;
	}

}
