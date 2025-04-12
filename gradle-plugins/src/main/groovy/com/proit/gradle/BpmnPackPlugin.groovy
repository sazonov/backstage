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

import groovy.json.JsonOutput
import org.gradle.api.Plugin
import org.gradle.api.Project

class BpmnPackPlugin implements Plugin<Project>
{
	void apply(Project project)
	{
		def extension = project.extensions.create('bpmn', BpmnPackPluginExtension)

		project.tasks.register('pack') {
			group = 'bpmn'

			doLast {
				if (!extension.dest) {
					extension.dest = new File(project.getBuildDir(), "bpmn")
				}

				println "Assembling BPMN from resources to ${extension.dest}"

				List<URL> urls = new ArrayList<>()

				project.configurations.compileClasspath.each { urls.add(it.toURI().toURL()) }

				ClassLoader classloader = new URLClassLoader(urls.toArray(new URL[urls.size()]) as URL[])

				println("BPMN pack classpath: ${classloader.URLs}")

				def provider = classloader.loadClass("com.proit.bpm.service.workflow.ClasspathWorkflowProvider").getConstructor().newInstance()
				provider.setClassLoader(new URLClassLoader([new File(project.projectDir, "src/main/resources").toURI().toURL()] as URL[]))

				extension.dest.mkdirs()

				provider.exportWorkflows().each { workflow ->
					println("Processing workflow '${workflow.id}'...")

					File file = new File(extension.dest, workflow.id + "_" + workflow.version + "_" + new Date().format("yyyyMMddHHmmss") + ".json")
					file.withOutputStream { stream ->
						stream.write(JsonOutput.toJson(workflow).getBytes())
					}
				}

				println "Done"
			}
		}
	}
}

class BpmnPackPluginExtension
{
	File dest
}