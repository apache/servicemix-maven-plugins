/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

/**
 * The dependency information is a simple value object to help with passing
 * around information on dependencies used to help in the creation of the jbi
 * descriptors
 * 
 */
public class DependencyInformation {

	private String artifactZip;

	private String component;

	private String description;

	private String name;

	private String version;

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

	public String getVersion() {
		return version;
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

	public void setVersion(String version) {
		this.version = version;
	}
}
