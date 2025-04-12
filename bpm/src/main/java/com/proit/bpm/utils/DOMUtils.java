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

package com.proit.bpm.utils;

import com.proit.bpm.model.Workflow;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class DOMUtils
{
	@SneakyThrows
	public Document parseDocument(String definition)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(new ByteArrayInputStream(definition.getBytes(StandardCharsets.UTF_8)));
	}

	@SneakyThrows
	public String writeDocument(Document document)
	{
		ByteArrayOutputStream resultStream = new ByteArrayOutputStream();

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StreamResult result = new StreamResult(resultStream);
		DOMSource source = new DOMSource(document);

		transformer.transform(source, result);

		return resultStream.toString(StandardCharsets.UTF_8);
	}

	public Optional<Element> getChildrenElementByTagName(Element parent, String tagName)
	{
		var elements = getChildrenElementsByTagName(parent, tagName);

		return Optional.ofNullable(elements.isEmpty() ? null : elements.get(0));
	}

	public List<Element> getChildrenElementsByTagName(Element parent, String tagName)
	{
		var nodes = parent.getElementsByTagName(tagName);
		var result = new ArrayList<Element>();

		for (int i = 0; i < nodes.getLength(); i++)
		{
			var node = nodes.item(i);

			if (node.getParentNode() == parent)
			{
				result.add((Element) node);
			}
		}

		return result;
	}

	public String getRootNamespace(Workflow workflow)
	{
		return workflow.getDefinition().contains("bpmn2:process") ? "bpmn2" : "bpmn";
	}
}
