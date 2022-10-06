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

package com.proit.bpm.service.jbpm;

import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.model.event.WorkflowsUpdatedEvent;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drools.compiler.compiler.ProcessLoadError;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService implements ApplicationListener<WorkflowsUpdatedEvent>
{
	private final WorkflowService workflowService;

	@Value(value = "classpath:/globals.drl")
	private Resource globalRulesSource;

	@Getter
	private KieBase kieBase;

	public void onApplicationEvent(WorkflowsUpdatedEvent event)
	{
		initialize();
	}

	@PostConstruct
	public synchronized void initialize()
	{
		try
		{
			kieBase = readKnowledgeBase();
		}
		catch (Exception e)
		{
			log.error("Failed to load workflow definitions.", e);
		}
	}

	private KieBase readKnowledgeBase()
	{
		log.info("Initializing jbpm knowledge base.");

		KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();

		try
		{
			builder.add(ResourceFactory.newInputStreamResource(globalRulesSource.getInputStream()), ResourceType.DRL);
		}
		catch (Exception e)
		{
			throw new BpmException("failed to init global definitions", e);
		}

		workflowService.getWorkflows().forEach(workflow -> {
			var resource = ResourceFactory.newInputStreamResource(new ByteArrayInputStream(processWorkflow(workflow).getBytes(StandardCharsets.UTF_8)), "UTF-8");
			resource.setSourcePath(workflow.getId());

			builder.add(resource, ResourceType.BPMN2);
		});

		if (builder.hasErrors())
		{
			var processErrors = builder.getErrors().stream().filter(it -> it instanceof ProcessLoadError).collect(Collectors.toList());
			var otherErrors = builder.getErrors().stream().filter(it -> !processErrors.contains(it)).collect(Collectors.toList());

			processErrors.forEach(error -> {
				log.error("Error while loading workflow '{}': {}", error.getResource().getSourcePath(), error);
			});

			if (!otherErrors.isEmpty())
			{
				log.error("There are errors while loading workflow definitions: {}.", otherErrors);
			}

			throw new BpmException("failed to init knowledge base");
		}

		return builder.newKieBase();
	}

	private String processWorkflow(Workflow workflow)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			org.w3c.dom.Document document = builder.parse(new ByteArrayInputStream(workflow.getDefinition().getBytes(StandardCharsets.UTF_8)));

			Element processNode = (Element) document.getDocumentElement().getElementsByTagName("bpmn2:process").item(0);

			if (processNode == null)
			{
				processNode = (Element) document.getDocumentElement().getElementsByTagName("process").item(0);

				if (processNode == null)
				{
					throw new BpmException(String.format("cannot find process node in workflow '%s'", workflow.getId()));
				}
			}

			processNode.setAttribute("id", workflow.getFullId());

			ByteArrayOutputStream resultStream = new ByteArrayOutputStream();

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult result = new StreamResult(resultStream);
			DOMSource source = new DOMSource(document);

			transformer.transform(source, result);

			return resultStream.toString(StandardCharsets.UTF_8);
		}
		catch (Exception e)
		{
			throw new BpmException("failed to patch workflow definition");
		}
	}
}
