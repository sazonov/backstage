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

import com.google.common.reflect.ClassPath
import groovy.xml.MarkupBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import java.lang.annotation.Annotation

class JpaPersistenceXmlPlugin implements Plugin<Project>
{
	static final def DEFAULT_PERSISTENCE_XML_LOCATION = "resources/main/db/META-INF/persistence.xml"

	void apply(Project project)
	{
		def extension = project.extensions.create('jpaPersistenceXml', JpaPersistenceXmlPluginExtension)

		project.tasks.classes {
			doLast {
				println ':persistence-xml'

				List<URL> urls = new ArrayList<>()

				project.sourceSets.main.output.classesDirs.each { urls.add(it.toURI().toURL()) }
				project.configurations.compileClasspath.each { urls.add(it.toURI().toURL()) }

				URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]) as URL[])

				def annotation = classLoader.loadClass(extension.entityAnnotationClassName.get()) as Class<? extends Annotation>
				def entityClasses = []

				extension.entityPackages.get().each { packageName ->
					ClassPath.from(classLoader).getTopLevelClassesRecursive(packageName).each {info ->
						try
						{
							def clazz = info.load()

							if (clazz.isAnnotationPresent(annotation))
							{
								entityClasses.add(clazz.canonicalName)
							}
						}
						catch (Throwable ignore)
						{
						}
					}
				}

				def persistenceXmlFile = extension.persistenceXmlPath.get().asFile
				persistenceXmlFile.parentFile.mkdirs()

				persistenceXmlFile.withWriter { writer ->
					def xml = new MarkupBuilder(new IndentPrinter(writer, "\t", true))

					xml.doubleQuotes = true
					xml.mkp.xmlDeclaration(version: '1.0', encoding: 'utf-8')

					xml.persistence('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance', 'version': '2.0', 'xmlns': 'http://java.sun.com/xml/ns/persistence', 'xsi:schemaLocation': 'http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd') {
						'persistence-unit'('name': extension.persistenceUnitName.get()) {
							extension.attributeConverters.get().each { attributeConverterClass ->
								'class'(attributeConverterClass)
							}

							mkp.yield('\n')

							entityClasses.sort().each { entityClass ->
								'class'(entityClass)
							}
						}
					}

					xml.mkp.yield('\n')
					xml.mkp.comment("DO NOT MODIFY! Auto generated at ${new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS 'GMT'X")}.")
				}
			}
		}
	}
}

class JpaPersistenceXmlPluginExtension
{
	RegularFileProperty persistenceXmlPath

	Property<String> entityAnnotationClassName

	Property<String> persistenceUnitName

	ListProperty<String> entityPackages

	ListProperty<String> attributeConverters

	JpaPersistenceXmlPluginExtension(Project project)
	{
		persistenceXmlPath = project.objects.fileProperty().convention(project.layout.buildDirectory.file(JpaPersistenceXmlPlugin.DEFAULT_PERSISTENCE_XML_LOCATION))
		entityAnnotationClassName = project.objects.property(String.class).convention("javax.persistence.Entity")
		persistenceUnitName = project.objects.property(String.class).convention("app")
		entityPackages = project.objects.listProperty(String.class).convention(["com.proit"])
		attributeConverters = project.objects.listProperty(String.class).convention([])
	}
}
