package com.proit.app.service.enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация используется над enum.
 * <p>
 * Аннотированный enum доступен для EnumApiService.
 * <p>
 * Пример:
 * <pre>{@code
 * @ApiEnum
 * @RequiredArgsConstructor public enum ConsumptionType
 * {
 *      EXAMPLE("example")
 *
 *      private final String description;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
public @interface ApiEnum
{
}
