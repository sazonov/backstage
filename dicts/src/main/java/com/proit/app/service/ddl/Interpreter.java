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

package com.proit.app.service.ddl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.domain.*;
import com.proit.app.exception.dictionary.enums.EnumNotFoundException;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.service.DictDataService;
import com.proit.app.service.DictService;
import com.proit.app.service.ddl.ast.*;
import com.proit.app.service.ddl.ast.expression.*;
import com.proit.app.service.ddl.ast.expression.table.AlterTable;
import com.proit.app.service.ddl.ast.expression.table.CreateIndexExpression;
import com.proit.app.service.ddl.ast.expression.table.CreateTable;
import com.proit.app.service.ddl.ast.expression.table.DeleteIndexExpression;
import com.proit.app.service.ddl.ast.expression.table.operation.*;
import com.proit.app.service.ddl.ast.value.*;
import com.proit.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.ID;
import static java.util.function.Predicate.not;

@Component
@RequiredArgsConstructor
public class Interpreter
{
	private final ObjectMapper objectMapper;

	private final DictService dictService;
	private final DictDataService dictDataService;

	public void execute(List<Expression> expressions)
	{
		for (var expr : expressions)
		{
			if (expr instanceof CreateTable createTable)
			{
				execute(createTable);
			}
			else if (expr instanceof AlterTable alterTable)
			{
				execute(alterTable);
			}
			else if (expr instanceof Insert insert)
			{
				execute(insert);
			}
			else if (expr instanceof Delete delete)
			{
				execute(delete);
			}
			else if (expr instanceof Update update)
			{
				execute(update);
			}
			else if (expr instanceof Drop drop)
			{
				execute(drop);
			}
			else if (expr instanceof CreateIndexExpression createIndex)
			{
				execute(createIndex);
			}
			else if (expr instanceof DeleteIndexExpression deleteIndex)
			{
				execute(deleteIndex);
			}
			else if (expr instanceof CreateEnum createEnum)
			{
				execute(createEnum);
			}
		}
	}

	private void execute(Insert insert)
	{
		var dictId = insert.getTable().getName();

		final var fieldIds = insert.getFields()
				.stream()
				.map(Id::getName)
				.collect(Collectors.toList());

		if (fieldIds.isEmpty())
		{
			fieldIds.addAll(
					dictService.getDataFieldsByDictId(dictId)
							.stream()
							.map(DictField::getId)
							.toList());
		}

		insert.getValues()
				.stream()
				.filter(values -> fieldIds.size() != values.size())
				.findAny()
				.ifPresent(values -> {
					throw new RuntimeException("Количество значений не совпадает с количеством полей справочника.");
				});

		var items = insert.getValues()
				.stream()
				.map(it -> buildDataItemMap(it, fieldIds))
				.map(it -> buildDictDataItem(dictId, it))
				.toList();

		//TODO: после перевода всех вызовов createMany на параметр DictDataItem вместо мапы
		// убрать SecurityUtils.getCurrentUserId()
		dictDataService.createMany(dictId, items, SecurityUtils.getCurrentUserId());
	}

	private void execute(AlterTable alterTable)
	{
		var dict = dictService.getById(alterTable.getTable().getName());
		var fields = dictService.getDataFieldsByDict(dict);
		var dictId = dict.getId();

		if (alterTable.getOperation() instanceof AddTableColumnOperation addTableColumnOperation)
		{
			fields.add(mapDictField(addTableColumnOperation.getField()));
		}
		else if (alterTable.getOperation() instanceof DropTableColumnOperation dropTableColumnOperation)
		{
			fields = fields.stream()
					.filter(it -> !it.getId().equalsIgnoreCase(dropTableColumnOperation.getField().getName()))
					.collect(Collectors.toList());
		}
		else if (alterTable.getOperation() instanceof SetTableParameterOperation setTableParameterOperation)
		{
			executeSetTableParameter(dict, setTableParameterOperation);
		}
		else if (alterTable.getOperation() instanceof RenameTableOperation renameTableOperation)
		{
			dict.setId(renameTableOperation.getNewValue().getId().getName());

			var name = renameTableOperation.getNewValue().getName().getValue();
			dict.setName(name == null ? dict.getName() : name);
		}
		else if (alterTable.getOperation() instanceof RenameColumnOperation renameColumnOperation)
		{
			var newColumnId = renameColumnOperation.getNewValue().getId().getName();
			var newColumnName = renameColumnOperation.getNewValue().getName().getValue();

			dictService.renameField(dictId, renameColumnOperation.getOldColumnId().getName(), newColumnId, newColumnName);

			return;
		}
		else if (alterTable.getOperation() instanceof AddConstraintColumnOperation addConstraintColumnOperation)
		{
			var constraint = new DictConstraint(
					addConstraintColumnOperation.getName().getName(),
					addConstraintColumnOperation.getColumns().stream().map(Id::getName).toList());

			dictService.createConstraint(alterTable.getTable().getName(), constraint);

			return;
		}
		else if (alterTable.getOperation() instanceof DropConstraintColumnOperation dropConstraintColumnOperation)
		{
			dictService.deleteConstraint(alterTable.getTable().getName(), dropConstraintColumnOperation.getName().getName());

			return;
		}
		else if (alterTable.getOperation() instanceof AddEnumValueOperation addEnumValueOperation)
		{
			var savedEnum = dictService.getById(alterTable.getTable().getName())
					.getEnums()
					.stream()
					.filter(it -> it.getId().equals(addEnumValueOperation.getId().getName()))
					.findAny()
					.orElseThrow(() -> new EnumNotFoundException(addEnumValueOperation.getId().getName()));

			if (savedEnum.getValues().contains(addEnumValueOperation.getNewValue().getValue()))
			{
				throw new RuntimeException("Значение %s в enum %s уже существует.".formatted(
						addEnumValueOperation.getNewValue().getValue(), addEnumValueOperation.getId().getName()));
			}

			savedEnum.getValues().add(addEnumValueOperation.getNewValue().getValue());

			dictService.updateEnum(alterTable.getTable().getName(), savedEnum);

			return;
		}
		else if (alterTable.getOperation() instanceof DropEnumOperation dropEnumOperation)
		{
			dictService.deleteEnum(alterTable.getTable().getName(), dropEnumOperation.getId().getName());

			return;
		}

		dict.setFields(fields);

		dictService.update(dictId, dict);
	}

	private void execute(CreateTable createTable)
	{
		var fields = createTable.getFields().stream()
				.map(this::mapDictField)
				.collect(Collectors.toList());

		var dict = Dict.builder()
				.id(createTable.getId().getName())
				.name(createTable.getName().getValue())
				.fields(fields)
				.build();

		dictService.create(dict);
	}

	private void execute(Delete delete)
	{
		if (delete.getColumn() == null)
		{
			dictDataService.deleteAll(delete.getTable().getName(), true);

			return;
		}

		var filtersQuery = buildFilterQuery(delete.getColumn());

		dictDataService.getByFilter(delete.getTable().getName(), List.of("*"), filtersQuery, Pageable.unpaged())
				.forEach(item -> dictDataService.delete(delete.getTable().getName(), item.getId(), true));
	}

	private void execute(Drop drop)
	{
		dictService.delete(drop.getTable().getName(), true);
	}

	private void execute(Update update)
	{
		var dictId = update.getTable().getName();

		var fieldIds = dictService.getById(dictId)
				.getFields()
				.stream()
				.map(DictField::getId)
				.toList();

		update.getColumns()
				.stream()
				.map(ColumnWithValue::getValue)
				.filter(it -> it instanceof ColumnValue)
				.map(it -> (ColumnValue) it)
				.map(ColumnValue::getValue)
				.map(Id::getName)
				.filter(not(fieldIds::contains))
				.findFirst()
				.ifPresent((it) -> {
					throw new RuntimeException("Колонка %s отсутствует.".formatted(it));
				});

		var filtersQuery = buildFilterQuery(update.getRow());

		dictDataService.getByFilter(dictId, List.of("*"), filtersQuery, Pageable.unpaged()).stream()
				.peek(dictItem -> {
					var updatedMap = new HashMap<>(dictItem.getData());

					update.getColumns()
							.forEach(column -> {
								if (column.getValue() instanceof ColumnValue columnValue)
								{
									var columnName = column.getId().getName();
									var value = updatedMap.get(columnValue.getValue().getName());

									updatedMap.put(columnName, value);
								}
								else
								{
									var dataItemMap = buildDataItemMap(column.getValue(), column.getId().getName());

									updatedMap.putAll(dataItemMap);
								}
							});

					dictItem.setData(updatedMap);
				})
				.forEach(dictItem -> dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, dictItem.getData()), dictItem.getVersion()));
	}

	private void execute(CreateIndexExpression createIndex)
	{
		var fields = createIndex.getFields().stream()
				.map(Id::getName)
				.collect(Collectors.toList());

		dictService.createIndex(createIndex.getTable().getName(), DictIndex.builder()
				.id(createIndex.getId().getName())
				.direction(createIndex.isDescDirection() ? Sort.Direction.DESC : Sort.Direction.ASC)
				.fields(fields)
				.build());
	}

	private void execute(DeleteIndexExpression deleteIndex)
	{
		dictService.deleteIndex(deleteIndex.getTable().getName(), deleteIndex.getId().getName());
	}

	private void execute(CreateEnum createEnum)
	{
		var values = createEnum.getValues()
				.stream()
				.map(StringValue::getValue)
				.collect(Collectors.toSet());

		if (values.size() != createEnum.getValues().size())
		{
			throw new RuntimeException("Повторяющиеся значения в enum недопустимы.");
		}

		dictService.createEnum(createEnum.getTableId().getName(), new DictEnum(createEnum.getId().getName(), createEnum.getName().getValue(), values));
	}

	private DictField mapDictField(ColumnDefinition columnDefinition)
	{
		var type = ColumnType.fromString(columnDefinition.getType().getName());

		var field = DictField.builder()
				.id(columnDefinition.getId().getName())
				.name(columnDefinition.getName().getValue())
				.multivalued(columnDefinition.isMultivalued())
				.required(columnDefinition.isRequired())
				.type(type.toDictFieldType());

		if (type == ColumnType.ENUM)
		{
			field.enumId(columnDefinition.getType().getName());
		}
		if (columnDefinition.getReference() != null)
		{
			field
					.type(DictFieldType.DICT)
					.dictRef(new DictFieldName(columnDefinition.getReference().getName(), ID));
		}

		return field.build();
	}

	private void executeSetTableParameter(Dict dict, SetTableParameterOperation operation)
	{
		switch (operation.getParameter())
		{
			case READ_PERMISSION -> dict.setViewPermission(operation.getParameterValue().getValue());
			case WRITE_PERMISSION -> dict.setEditPermission(operation.getParameterValue().getValue());
		}
	}

	private Map<String, Object> buildDataItemMap(Value<?> value, String fieldId)
	{
		return buildDataItemMap(List.of(value), List.of(fieldId));
	}

	private Map<String, Object> buildDataItemMap(List<Value<?>> values, List<String> fieldIds)
	{
		var item = new HashMap<String, Object>();

		for (int i = 0; i < fieldIds.size(); i++)
		{
			var value = values.get(i);

			if (value instanceof NullValue)
			{
				continue;
			}

			if (value instanceof ArrayValue arrayValue && !arrayValue.getValue().isEmpty())
			{
				var clazz = arrayValue.getValue()
						.get(0)
						.getValue()
						.getClass();

				var v = arrayValue.getValue()
						.stream()
						.peek(it -> {
							if (!clazz.isInstance(it.getValue()))
							{
								throw new RuntimeException("Элементы массива должны иметь одинаковый тип.");
							}
						})
						.map(Value::getValue)
						.collect(Collectors.toList());

				item.put(fieldIds.get(i), v);
			}
			else if (value instanceof JsonValue jsonValue)
			{
				try
				{
					var map = objectMapper.readValue(jsonValue.getValue(), Map.class);

					item.put(fieldIds.get(i), map);
				}
				catch (JsonProcessingException e)
				{
					throw new RuntimeException("Некорректный формат json поля.");
				}
			}
			else
			{
				item.put(fieldIds.get(i), value.getValue());
			}
		}

		return item;
	}

	private String buildFilterQuery(ComparingValueColumn column)
	{
		if (column == null || column.getPredicateType() == null)
		{
			return null;
		}

		if (column.getValue() instanceof StringValue)
		{
			return "%s %s '%s'".formatted(column.getId().getName(), column.getPredicateType().getValue(), column.getValue().getValue());
		}
		else if (column.getValue() instanceof BooleanValue
				|| column.getValue() instanceof DecimalValue
				|| column.getValue() instanceof NullValue)
		{
			return "%s %s %s".formatted(column.getId().getName(), column.getPredicateType().getValue(), column.getValue().getValue());
		}
		else
		{
			throw new RuntimeException("Недопустимое выражение в where. Ожидалось StringValue, BooleanValue, DecimalValue или NullValue.");
		}
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataItemMap)
	{
		return DictDataItem.of(dictId, dataItemMap);
	}
}
