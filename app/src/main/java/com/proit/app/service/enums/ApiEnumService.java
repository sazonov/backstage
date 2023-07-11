package com.proit.app.service.enums;

import com.proit.app.exception.ObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ApiEnumService
{
	private static final String DEFAULT_PACKAGE = "com.proit";

	private final Map<String, Class<?>> applicationEnums = new TreeMap<>();

	/**
	 * Запускаем сканирование ClassPath на наличие enum'ов помеченных аннотацией @ApiEnum.
	 * Если такой находим, то кладем Class в мэпу applicationEnums.
	 *
	 * @see ApiEnum
	 * @see ApiEnumDescription
	 */
	@PostConstruct
	public void init()
	{
		var scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ApiEnum.class));

		log.info("Scanning @ApiEnum annotations.");

		scanner.findCandidateComponents(DEFAULT_PACKAGE)
				.stream()
				.map(BeanDefinition::getBeanClassName)
				.filter(Objects::nonNull)
				.map(className -> ClassUtils.resolveClassName(className, getClass().getClassLoader()))
				.filter(Class::isEnum)
				.forEach(clazz -> applicationEnums.put(clazz.getSimpleName(), clazz));

		log.info("@ApiEnum annotations found: {}.", applicationEnums.size());
	}

	/**
	 * Метод возвращает список имен всех enum'ов помеченных аннотацией @ApiEnum.
	 *
	 * @see ApiEnum
	 */
	public Set<String> getEnumNames()
	{
		return Collections.unmodifiableSet(applicationEnums.keySet());
	}

	public Map<String, String> getEnumDescription(String enumName)
	{
		return Optional.ofNullable(applicationEnums.get(enumName))
				.map(this::getTypeApplicationEnums)
				.orElseThrow(() -> new ObjectNotFoundException(String.class, enumName));
	}

	/**
	 * Возвращает мэпу вида:
	 * <pre>{@code
	 * {
	 *     "ИМЯ_КОНСТАНТЫ": "Описание"
	 * }
	 * }</pre>
	 * Если описание константы null, используем ее же имя в качестве описания.
	 */
	private Map<String, String> getTypeApplicationEnums(Class<?> clazz)
	{
		return Stream.of(clazz.getEnumConstants())
				.map(Enum.class::cast)
				.collect(Collectors.toMap(Enum::name, this::getDescriptionValue));
	}

	/**
	 * Метод возвращает описание enum.
	 * Описание должно быть помечено аннотацией @ApiEnumDescription.
	 *
	 * @param enumConstant внутрення константа enum
	 * @return описание константы
	 * @see ApiEnumDescription
	 */
	private String getDescriptionValue(Enum<?> enumConstant)
	{
		var declaredFields = enumConstant.getClass().getDeclaredFields();

		return Stream.of(declaredFields)
				.filter(declaredField -> declaredField.isAnnotationPresent(ApiEnumDescription.class))
				.map(Field::getName)
				.map(fieldName -> getProtectedFieldValue(fieldName, enumConstant))
				.map(String.class::cast)
				.findFirst()
				.orElse(enumConstant.name());
	}

	public Object getProtectedFieldValue(String protectedField, Object object)
	{
		try
		{
			Field field = object.getClass().getDeclaredField(protectedField);
			field.setAccessible(true);

			return field.get(object);
		}
		catch (NoSuchFieldException e)
		{
			throw new IllegalStateException(
					"Could not locate field '%s' on class %s".formatted(protectedField, object.getClass()));
		}
		catch (Exception ex)
		{
			ReflectionUtils.handleReflectionException(ex);

			return null;
		}
	}
}
