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
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension

import java.nio.charset.StandardCharsets

class CheckstylePlugin implements Plugin<Project>
{
	def checkstyleTaskName = "checkstyleMain"
	def propertyName = "sourceFiles"

	def preCommitHook = """#!/bin/sh

		echo "Performing pre-commit code style check..."

		modifiedFiles=\$(git diff --cached --name-only --diff-filter=ACMR | tr '\\n' ',')
		./gradlew ${checkstyleTaskName} -P${propertyName}=\${modifiedFiles%?}"""

	void apply(Project project)
	{
		project.pluginManager.apply(org.gradle.api.plugins.quality.CheckstylePlugin)

		project.extensions.configure(CheckstyleExtension, { ext ->
			ext.ignoreFailures = false
			ext.maxWarnings = 0
			ext.toolVersion = '10.8.0'
		})

		project.configurations.checkstyle.dependencies.add(
				project.dependencies.create("com.proit:checkstyle:1.0.1"))

		project.tasks.register("checkstyleApplyConfig").configure {
			doFirst {
				def configDir = new File(project.getRootDir(), "config/checkstyle")
				configDir.mkdirs()

				def suppressionsConfigFile = new File(configDir, "suppressions.xml")
				suppressionsConfigFile.bytes = this.class.getResourceAsStream("/checkstyle/suppressions.xml").bytes

				def checkstyleConfigFile = new File(configDir, "checkstyle.xml")
				checkstyleConfigFile.bytes = this.class.getResourceAsStream("/checkstyle/checkstyle.xml").bytes
			}
		}

		project.tasks.withType(Checkstyle.class).configureEach {
			// Убираем зависимость checkstyle от сборки проекта, так значительно быстрее.
			setClasspath(project.files())

			if (project.hasProperty(propertyName))
			{
				var commaSeparatedSources = project.property(propertyName) as String

				if (commaSeparatedSources?.trim())
				{
					var sourceFileNames = commaSeparatedSources.split(",").flatten()
					var projectSourceFiles = sourceFileNames.collect({ project.file(it)} ).findAll({ it.exists() })

					if (projectSourceFiles.size() != sourceFileNames.size() && project.rootProject)
					{
						projectSourceFiles += sourceFileNames.collect({ project.rootProject.file(it)} ).findAll({ it.exists() && isProjectFile(project, it as File) })
					}

					var sourceFileCollection = project.objects.fileCollection().from(projectSourceFiles)

					setClasspath(sourceFileCollection)
					setSource(sourceFileCollection.asFileTree)
				}
			}
		}

		var hooksDir = new File(project.getRootDir(), ".git/hooks")
		hooksDir.mkdirs()

		var preCommitHookFile = new File(hooksDir, "pre-commit")

		// Всегда поддерживаем актуальную версию скрипта.
		if (preCommitHookFile.exists())
		{
			preCommitHookFile.delete()
		}

		if (!project.tasks.names.contains(checkstyleTaskName))
		{
			println("Checkstyle plugin is not active, disabling pre-commit hooks.")

			return
		}

		preCommitHookFile.createNewFile()
		preCommitHookFile.setReadable(true)
		preCommitHookFile.setWritable(true, true)
		preCommitHookFile.setExecutable(true)

		preCommitHookFile.withOutputStream { stream ->
			stream.write(preCommitHook.getBytes(StandardCharsets.UTF_8))
		}
	}

	private static boolean isProjectFile(Project project, File file)
	{
		while (file.parentFile)
		{
			file = file.parentFile

			if (project.projectDir == file)
			{
				return true
			}
		}

		return false
	}
}

