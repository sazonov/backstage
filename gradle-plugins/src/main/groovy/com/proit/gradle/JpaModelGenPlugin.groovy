/*
 *    Copyright 2019-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

class JpaModelGenPlugin implements Plugin<Project>
{
	void apply(Project project)
	{
		def extension = project.extensions.create('jpaModelGen', JpaModelGenPluginExtension)

		project.configurations.annotationProcessor.dependencies.add(
				project.dependencies.create("org.hibernate:hibernate-jpamodelgen:${extension.hibernateVersion.get()}"))

		project.tasks.find {it.name == 'compileJava' }.configure {
			doFirst {
				println ':modelgen'
			}

			options.compilerArgs +=
					['-ApersistenceXml=' + extension.persistenceXmlPath.get().asFile.toString()]
		}
	}
}

class JpaModelGenPluginExtension
{
	Property<String> hibernateVersion

	RegularFileProperty persistenceXmlPath

	JpaModelGenPluginExtension(Project project)
	{
		hibernateVersion = project.objects.property(String.class).convention("6.0.0.Alpha8")
		persistenceXmlPath = project.objects.fileProperty().convention(project.layout.buildDirectory.file(JpaPersistenceXmlPlugin.DEFAULT_PERSISTENCE_XML_LOCATION))
	}
}
