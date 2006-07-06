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
			setDeployed(isResultContaining(result, "service-assembly", name));
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
