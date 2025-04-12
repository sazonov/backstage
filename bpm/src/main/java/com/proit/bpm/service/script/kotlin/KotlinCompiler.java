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

package com.proit.bpm.service.script.kotlin;

import com.proit.bpm.model.Workflow;
import com.proit.bpm.model.WorkflowScript;
import com.proit.bpm.service.script.Script;
import kotlin.script.experimental.jvm.util.JvmClasspathUtilKt;
import org.apache.commons.io.FileUtils;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.config.JvmTarget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KotlinCompiler
{
	private final Map<Workflow, ClassLoader> classLoaders = new ConcurrentHashMap<>();

	public Script compile(KotlinBackend kotlinBackend, WorkflowScript workflowScript)
	{
		var workflow = workflowScript.getWorkflow();

		try
		{
			if (!classLoaders.containsKey(workflow))
			{
				classLoaders.put(workflow, compile(workflow));
			}

			return new Script(workflowScript.getId(), classLoaders.get(workflow).loadClass("workflows.%s.%s".formatted(workflow.getId(), workflowScript.getId())), kotlinBackend);
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to compile script", e);
		}
	}

	private ClassLoader compile(Workflow workflow) throws IOException
	{
		var parentClassLoader = getClass().getClassLoader();
		var sourcePath = Files.createTempDirectory("bpmKotlin").toFile();
		var outputPath = Files.createTempDirectory("bpmKotlin").toFile();

		extractScriptSources(workflow, sourcePath);

		var configuration = new CompilerConfiguration();
		configuration.put(CommonConfigurationKeys.MODULE_NAME, workflow.getId());

		var outputStream = new ByteArrayOutputStream();
		var printStream = new PrintStream(outputStream);

		configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, new PrintingMessageCollector(printStream, MessageRenderer.PLAIN_FULL_PATHS, true));
		configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, outputPath);
		configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_17);

		JvmContentRootsKt.configureJdkClasspathRoots(configuration);
		JvmContentRootsKt.addJvmClasspathRoots(configuration, JvmClasspathUtilKt.classpathFromClassloader(parentClassLoader, false));

		var env = KotlinCoreEnvironment.createForProduction(new StubDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
		env.addKotlinSourceRoots(List.of(sourcePath));

		var result = KotlinToJVMBytecodeCompiler.INSTANCE.analyzeAndGenerate(env);

		printStream.flush();

		if (result != null)
		{
			try
			{
				URL[] urls = { outputPath.toURI().toURL() };

				return URLClassLoader.newInstance(urls, parentClassLoader);
			}
			catch (Exception e)
			{
				throw new RuntimeException("failed to process kotlin compilation result", e);
			}
		}
		else
		{
			throw new RuntimeException("failed to compile kotlin sources: %s".formatted(outputStream));
		}
	}

	private void extractScriptSources(Workflow workflow, File sourcePath) throws IOException
	{
		for (var workflowScript : workflow.getScripts().values())
		{
			FileUtils.writeStringToFile(new File(sourcePath, workflowScript.getFilename()), workflowScript.getDefinition(), StandardCharsets.UTF_8);
		}
	}

	private static class StubDisposable implements Disposable
	{
		@Override
		public void dispose()
		{
		}
	}
}
