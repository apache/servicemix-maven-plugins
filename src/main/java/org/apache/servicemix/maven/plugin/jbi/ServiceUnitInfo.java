package org.apache.servicemix.maven.plugin.jbi;

public class ServiceUnitInfo {

	private String artifactZip;

	private String component;

	private String description;

	private String name;

	public String getArtifactZip() {
		return artifactZip;
	}

	public String getComponent() {
		return component;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public void setArtifactZip(String artifactZip) {
		this.artifactZip = artifactZip;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}
}
