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

import org.eclipse.persistence.tools.weaving.jpa.StaticWeaveProcessor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty

import java.nio.file.Path
import java.nio.file.Paths

class JpaWeavingPlugin implements Plugin<Project>
{
	void apply(Project project)
	{
		project.configurations.create 'weaving'

		def extension = project.extensions.create('jpaWeaving', JpaWeavingPluginExtension)

		project.tasks.classes {
			doLast {
				println ':weave'

				// Добавляем совместимость с 4+ версией gradle.
				def sourcePath = project.layout.buildDirectory.file("classes/java/main").get().asFile

				if (!sourcePath.exists())
				{
					sourcePath = project.layout.buildDirectory.file("classes/main").get().asFile
				}

				def targetPath = sourcePath
				def persistenceXmlPath = extension.persistenceXmlPath.get().asFile

				if (!persistenceXmlPath.exists())
				{
					return
				}

				Path pathBase = Paths.get(sourcePath.toURI())
				Path pathAbsolute = Paths.get(persistenceXmlPath.toURI())

				def mergedConfiguration = project.configurations.weaving + project.configurations.compileClasspath

				List<URL> urls = new ArrayList<>()

				mergedConfiguration.each { urls.add(it.toURI().toURL()) }

				URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]) as URL[])

				StaticWeaveProcessor weave = new StaticWeaveProcessor(sourcePath.absolutePath, targetPath.absolutePath)
				weave.setClassLoader(classLoader)
				weave.setPersistenceXMLLocation(pathBase.relativize(pathAbsolute).toString())
				weave.performWeaving()
			}
		}
	}
}

class JpaWeavingPluginExtension
{
	RegularFileProperty persistenceXmlPath

	JpaWeavingPluginExtension(Project project)
	{
		persistenceXmlPath = project.objects.fileProperty().convention(project.layout.buildDirectory.file(JpaPersistenceXmlPlugin.DEFAULT_PERSISTENCE_XML_LOCATION))
	}
}
