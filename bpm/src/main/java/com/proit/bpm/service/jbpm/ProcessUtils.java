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

import com.proit.bpm.domain.Process;
import com.proit.bpm.exception.BpmException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.jbpm.workflow.instance.node.SubProcessNodeInstance;
import org.kie.api.definition.process.NodeContainer;
import org.kie.api.definition.process.WorkflowProcess;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.NodeInstanceContainer;
import org.kie.api.runtime.process.ProcessInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@UtilityClass
public class ProcessUtils
{
	private static final String PROCESS_VARIABLE_PROCESS_ID = "__processId";

	private static final String NODE_METADATA_UNIQUE_ID = "UniqueId";

	public void setProcessId(ProcessInstance processInstance, String id)
	{
		setProcessVariable(processInstance, PROCESS_VARIABLE_PROCESS_ID, id);
	}

	public Optional<String> getProcessId(ProcessInstance processInstance)
	{
		return Optional.ofNullable((String) getProcessVariable(processInstance, PROCESS_VARIABLE_PROCESS_ID));
	}

	public Object getProcessVariable(ProcessInstance processInstance, String name)
	{
		return ((RuleFlowProcessInstance) processInstance).getVariable(name);
	}

	public void setProcessVariable(ProcessInstance processInstance, String name, Object value)
	{
		((RuleFlowProcessInstance) processInstance).setVariable(name, value);
	}

	public void migrateProcess(Process process, ProcessInstance processInstance, String workflowId)
	{
		var processId = getProcessId(processInstance).orElseThrow(() ->
				new BpmException("cannot get process id for jbpm process instance %d".formatted(processInstance.getId()))
		);

		log.info("Migrating process instance {}...", processId);

		var currentWorkflowId = processInstance.getProcess().getId();

		if (workflowId.equals(process.getWorkflowId()) || workflowId.equals(currentWorkflowId))
		{
			log.info("Process instance already uses workflow '{}'.", workflowId);

			return;
		}

		log.info("Changing workflow from '{}' to '{}'.", currentWorkflowId, workflowId);

		WorkflowProcessInstanceImpl workflowProcessInstance = (WorkflowProcessInstanceImpl) processInstance;

		var knowledgeRuntime = workflowProcessInstance.getKnowledgeRuntime();
		var newProcess = (WorkflowProcess) knowledgeRuntime.getKieBase().getProcess(workflowId);

		if (newProcess == null)
		{
			throw new BpmException(String.format("failed to find target workflow '%s'", workflowId));
		}

		var oldProcess = workflowProcessInstance.getProcess();
		workflowProcessInstance.disconnect();
		workflowProcessInstance.setProcess(oldProcess);

		updateNodeInstances(workflowProcessInstance, newProcess);

		workflowProcessInstance.setKnowledgeRuntime(knowledgeRuntime);
		workflowProcessInstance.setProcess(newProcess);
		workflowProcessInstance.reconnect();

		process.setWorkflowId(workflowId);
	}

	private static void updateNodeInstances(NodeInstanceContainer fromInstanceContainer, NodeContainer toNodeContainer)
	{
		Map<String, Long> nodeMapping = createNodeMapping(fromInstanceContainer, toNodeContainer);

		for (NodeInstance nodeInstance: fromInstanceContainer.getNodeInstances())
		{
			String oldNodeId = ((NodeImpl) nodeInstance.getNode()).getUniqueId();
			Long newNodeId = nodeMapping.get(oldNodeId);

			if (nodeInstance instanceof SubProcessNodeInstance)
			{
				log.warn("Migration of inner sub processes is not supported!");
			}
			else if (nodeInstance instanceof NodeInstanceContainer)
			{
				NodeContainer nodeContainer = ((NodeContainer) toNodeContainer.getNode(newNodeId));

				updateNodeInstances((NodeInstanceContainer) nodeInstance, nodeContainer);
			}

			if (newNodeId == null)
			{
				newNodeId = nodeInstance.getNodeId();
			}

			((NodeInstanceImpl) nodeInstance).setNodeId(newNodeId);
		}
	}

	private static Map<String, Long> createNodeMapping(NodeInstanceContainer fromContainer, NodeContainer toContainer)
	{
		HashMap<String, Long> mapping = new HashMap<>();

		for (NodeInstance fromItem : fromContainer.getNodeInstances())
		{
			String fromInstanceId = ((NodeImpl) fromItem.getNode()).getUniqueId();

			Object uniqueId = fromItem.getNode().getMetaData().get(NODE_METADATA_UNIQUE_ID);

			if (uniqueId == null)
			{
				throw new BpmException(String.format("failed to get unique id for node %d", fromItem.getNodeId()));
			}

			for (var toItem : toContainer.getNodes())
			{
				Object toUniqueId = toItem.getMetaData().get(NODE_METADATA_UNIQUE_ID);

				if (toUniqueId != null)
				{
					if (StringUtils.equals(uniqueId.toString(), toUniqueId.toString()))
					{
						mapping.put(fromInstanceId, toItem.getId());

						break;
					}
				}
			}

			if (!mapping.containsKey(fromInstanceId))
			{
				throw new BpmException(String.format("cannot create node mapping for node %d", fromItem.getNodeId()));
			}
		}

		return mapping;
	}
}
