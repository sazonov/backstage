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

package com.proit.app.endpoint;

import com.proit.app.configuration.properties.JmsProperties;
import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.api.OkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.management.ObjectName;
import java.util.Hashtable;

@Slf4j
@Tag(name = "jms-endpoint", description = "Методы для работы с JMS брокером сервиса.")
@RestController
@RequestMapping("/api/jms")
@ConditionalOnProperty(JmsProperties.ACTIVATION_PROPERTY)
@RequiredArgsConstructor
public class JMSEndpoint
{
	private final BrokerService brokerService;

	@SneakyThrows
	@PostMapping("/queue/{name}/purge")
	@Operation(summary = "Очистка указанной очереди.")
	public OkResponse purge(@PathVariable String name)
	{
		log.info("Purging JMS queue '{}'.", name);

		getQueue(name).purge();

		log.info("Done.");

		return ApiResponse.ok();
	}

	@SneakyThrows
	@PostMapping("/queue/{name}/moveMessages")
	@Operation(summary = "Перенос сообщений из одной очереди в другую.")
	public OkResponse moveMessages(@PathVariable String name, @RequestParam String destination, @RequestParam(defaultValue = "0") Integer limit)
	{
		log.info("Moving JMS messages from queue '{}' to '{}' with limit {}.", name, destination, limit);

		getQueue(name).moveMatchingMessagesTo(null, destination, limit);

		log.info("Done.");

		return ApiResponse.ok();
	}

	private QueueViewMBean getQueue(String name) throws Exception
	{
		Hashtable<String, String> params = new Hashtable<>();
		params.put("type", "Broker");
		params.put("brokerName", brokerService.getBrokerName());
		params.put("destinationType", "Queue");
		params.put("destinationName", name);

		ObjectName queueObjectName = ObjectName.getInstance("org.apache.activemq", params);

		return (QueueViewMBean) brokerService.getManagementContext().newProxyInstance(queueObjectName, QueueViewMBean.class, true);
	}
}
