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

import org.apache.servicemix.jbi.framework.AdminCommandsServiceMBean;
import org.apache.servicemix.jbi.management.task.JbiTask;

public class IsDeployedTask extends JbiTask {

	private static final String JBI_SHARED_LIBRARY = "jbi-shared-library";

	private static final String JBI_COMPONENT = "jbi-component";

	private static final String JBI_SERVICE_ASSEMBLY = "jbi-service-assembly";

	private boolean deployed = false;

	private String type;

	private String name;

	public boolean isDeployed() {
		return deployed;
	}

	public void setDeployed(boolean deployed) {
		this.deployed = deployed;
	}

	protected void doExecute(AdminCommandsServiceMBean acs) throws Exception {
		if (JBI_SHARED_LIBRARY.equals(type)) {
			String result = acs.listSharedLibraries(null, name);
			setDeployed(isResultContaining(result, "shared-library", name));
		} else if (JBI_SERVICE_ASSEMBLY.equals(type)) {
			String result = acs.listServiceAssemblies(null, null, name);
			setDeployed(result.contains("<service-assembly-info name='" + name
					+ "'"));
		}
		if (JBI_COMPONENT.equals(type)) {
			String result = acs.listComponents(false, false, false, null, null,
					null);
			if (isResultContaining(result, "service-engine", name)
					|| isResultContaining(result, "binding-component", name)) {
				setDeployed(true);
			}
		}
	}

	private boolean isResultContaining(String result, String type, String name) {
		String componentLine = "<component-info type='" + type + "' name='"
				+ name + "'";
		return result.contains(componentLine);
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setName(String name) {
		this.name = name;
	}

}
