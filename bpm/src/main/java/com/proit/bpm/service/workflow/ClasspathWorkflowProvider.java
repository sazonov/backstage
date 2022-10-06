/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.bpm.service.workflow;

import com.proit.bpm.model.Workflow;
import com.proit.bpm.model.WorkflowScript;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * Загружает процессы, находящиеся на classpath в папке /workflows/{workflow_id}/{workflow_files}.
 * Обходит все доступные ресурсы, включая jar файлы.
 */
@Slf4j
@Component
public class ClasspathWorkflowProvider extends AbstractWorkflowProvider
{
	private static final String DEFAULT_WORKFLOW_FILE = "workflow.bpmn";
	private static final String BASE_PATH = "workflows";
	private static final String BASE_JAR_PATH = BASE_PATH + "/";

	@Setter
	private ClassLoader classLoader;

	public List<Workflow> getWorkflows()
	{
		List<Workflow> workflows = new LinkedList<>();
		PathMatchingResourcePatternResolver resolver = classLoader != null ? new PathMatchingResourcePatternResolver(classLoader) : new PathMatchingResourcePatternResolver();

		try
		{
			for (Resource resource : resolver.getResources("classpath*:/" + BASE_PATH))
			{
				var resourceConnection = resource.getURL().openConnection();

				if (resourceConnection instanceof JarURLConnection)
				{
					var file = ((JarURLConnection) resourceConnection).getJarFile();
					var workflowIds = new HashSet<String>();

					file.entries().asIterator().forEachRemaining(jarEntry -> {
						if (jarEntry.getName().startsWith(BASE_JAR_PATH) && !BASE_JAR_PATH.equals(jarEntry.getName()))
						{
							workflowIds.add(jarEntry.getName().split("/")[1]);
						}
					});

					for (String workflowId : workflowIds)
					{
						Workflow workflow = new Workflow();
						workflow.setId(workflowId);
						workflow.setDefinition(IOUtils.toString(file.getInputStream(file.getEntry(BASE_JAR_PATH + workflowId + "/" + DEFAULT_WORKFLOW_FILE)), CharEncoding.UTF_8));

						file.entries().asIterator().forEachRemaining(jarEntry -> {
							if (isWorkflowScript(workflowId, jarEntry))
							{
								try
								{
									addScript(workflow, jarEntry.getName(), file.getInputStream(jarEntry));
								}
								catch (IOException ex)
								{
									throw new RuntimeException("bad workflow script", ex);
								}
							}
						});

						workflows.add(workflow);
					}
				}
				else
				{
					for (File workflowDir : FileUtils.listFilesAndDirs(resource.getFile(), FalseFileFilter.INSTANCE, DirectoryFileFilter.INSTANCE))
					{
						if (workflowDir.equals(resource.getFile()))
						{
							continue;
						}

						Workflow workflow = new Workflow();
						workflow.setId(FilenameUtils.getBaseName(workflowDir.getName()));
						workflow.setDefinition(IOUtils.toString(new File(workflowDir, DEFAULT_WORKFLOW_FILE).toURI(), StandardCharsets.UTF_8));

						for (File scriptFile : FileUtils.listFiles(workflowDir, WorkflowScript.Type.allExtensions().toArray(String[]::new), false))
						{
							addScript(workflow, scriptFile.getName(), new FileInputStream(scriptFile));
						}

						workflows.add(workflow);
					}
				}
			}

			workflows.forEach(this::processWorkflow);
		}
		catch (Exception e)
		{
			log.warn("Failed to load workflows from classpath.", e);
		}

		return workflows;
	}

	private boolean isWorkflowScript(String workflowId, JarEntry jarEntry)
	{
		var scriptExtensions = WorkflowScript.Type.allExtensions();

		return jarEntry.getName().startsWith(BASE_JAR_PATH + workflowId) && scriptExtensions.stream().anyMatch(ext -> jarEntry.getName().endsWith(ext));
	}

	private void addScript(Workflow workflow, String fileName, InputStream fileStream) throws IOException
	{
		WorkflowScript script = new WorkflowScript(workflow);
		script.setId(FilenameUtils.getBaseName(fileName));
		script.setType(WorkflowScript.Type.fromExtension(FilenameUtils.getExtension(fileName)));
		script.setDefinition(IOUtils.toString(fileStream, StandardCharsets.UTF_8));

		workflow.getScripts().put(script.getId(), script);
	}
}
