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

package com.proit.bpm.service.workflow;

import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.ExportedWorkflow;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.model.WorkflowScript;
import com.proit.bpm.utils.DOMUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractWorkflowProvider implements WorkflowProvider
{
	public List<ExportedWorkflow> exportWorkflows()
	{
		return getWorkflows().stream().map(this::exportWorkflow).collect(Collectors.toList());
	}

	protected ExportedWorkflow exportWorkflow(Workflow workflow)
	{
		try
		{
			var document = DOMUtils.parseDocument(workflow.getDefinition());
			var rootNamespace = DOMUtils.getRootNamespace(workflow);

			Element processNode = DOMUtils.getChildrenElementByTagName(document.getDocumentElement(), rootNamespace + ":process").orElseThrow(() -> new RuntimeException("no process element in workflow definition"));
			Element processExtensions = DOMUtils.getChildrenElementByTagName(processNode, rootNamespace + ":extensionElements").orElse(null);

			if (processExtensions == null)
			{
				processExtensions = document.createElement(rootNamespace + ":extensionElements");
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

			return new ExportedWorkflow(workflow.getId(), workflow.getVersion().toString(), DOMUtils.writeDocument(document));
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
			var document = DOMUtils.parseDocument(workflow.getDefinition());
			var rootNamespace = DOMUtils.getRootNamespace(workflow);

			Element processNode = (Element) document.getDocumentElement().getElementsByTagName(rootNamespace + ":process").item(0);
			NodeList processScripts = processNode.getElementsByTagName("processScript");

			for (int i = 0; i < processScripts.getLength(); i++)
			{
				Element scriptNode = (Element) processScripts.item(i);

				WorkflowScript script = new WorkflowScript(workflow);

				script.setId(scriptNode.getAttribute("name"));
				script.setType(WorkflowScript.Type.valueOf(scriptNode.getAttribute("type")));
				script.setDefinition(((CDATASection) scriptNode.getFirstChild()).getData());

				workflow.getScripts().put(script.getId(), script);
			}
		}
		catch (Exception e)
		{
			throw new BpmException("failed to import workflow", e);
		}
	}
}
