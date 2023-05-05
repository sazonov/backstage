/*
 *    Copyright 2019-2023 the original author or authors.
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

import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.ExportedWorkflow;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.model.WorkflowScript;
import org.springframework.data.util.Version;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractWorkflowProvider implements WorkflowProvider
{
	private static final Version DEFAULT_VERSION = Version.parse("1.0");

	public List<ExportedWorkflow> exportWorkflows()
	{
		return getWorkflows().stream().map(this::exportWorkflow).collect(Collectors.toList());
	}

	protected ExportedWorkflow exportWorkflow(Workflow workflow)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			org.w3c.dom.Document document = builder.parse(new ByteArrayInputStream(workflow.getDefinition().getBytes(StandardCharsets.UTF_8)));

			Element processNode = (Element) document.getDocumentElement().getElementsByTagName("bpmn2:process").item(0);
			Element processExtensions = (Element) processNode.getElementsByTagName("bpmn2:extensionElements").item(0);

			if (processExtensions == null)
			{
				processExtensions = document.createElement("bpmn2:extensionElements");
				processNode.appendChild(processExtensions);
			}

			Element finalProcessExtensions = processExtensions;

			workflow.getScripts().forEach((scriptId, script) -> {
				Element scriptElement = document.createElement("processScript");

				scriptElement.setAttribute("name", script.getId());
				scriptElement.setAttribute("type", script.getType().name());
				scriptElement.appendChild(document.createCDATASection(script.getDefinition()));

				finalProcessExtensions.appendChild(scriptElement);
			});

			ByteArrayOutputStream resultStream = new ByteArrayOutputStream();

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult result = new StreamResult(resultStream);
			DOMSource source = new DOMSource(document);

			transformer.transform(source, result);

			return new ExportedWorkflow(workflow.getId(), workflow.getVersion().toString(), resultStream.toString(StandardCharsets.UTF_8));
		}
		catch (Exception e)
		{
			throw new BpmException("failed to export workflow", e);
		}
	}

	protected void processWorkflow(Workflow workflow)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			org.w3c.dom.Document document = builder.parse(new ByteArrayInputStream(workflow.getDefinition().getBytes(StandardCharsets.UTF_8)));

			Element processNode = (Element) document.getDocumentElement().getElementsByTagName("bpmn2:process").item(0);
			Element processExtensions = (Element) processNode.getElementsByTagName("bpmn2:extensionElements").item(0);

			NodeList processScripts = processExtensions.getElementsByTagName("processScript");

			for (int i = 0; i < processScripts.getLength(); i++)
			{
				Element scriptNode = (Element) processScripts.item(i);

				WorkflowScript script = new WorkflowScript(workflow);

				script.setId(scriptNode.getAttribute("name"));
				script.setType(WorkflowScript.Type.valueOf(scriptNode.getAttribute("type")));
				script.setDefinition(((CDATASection) scriptNode.getFirstChild()).getData());

				workflow.getScripts().put(script.getId(), script);
			}

			if (workflow.getVersion() == null)
			{
				var version = Version.parse(processNode.getAttribute("drools:version"));

				workflow.setVersion(version.isLessThan(DEFAULT_VERSION) ? DEFAULT_VERSION : version);
			}
		}
		catch (Exception e)
		{
			throw new BpmException("failed to import workflow", e);
		}
	}
}
