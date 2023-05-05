package com.proit.app.service.enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация используется над полем enum, чтобы показать,
 * что поле используется в качестве описания для константы enum.
 * <p>
 * Пример:
 * <pre>{@code
 * @ApiEnum
 * @RequiredArgsConstructor
 * public enum ConsumptionType
 * {
 *      EXAMPLE("example")
 *
 *      @ApiEnumDescription
 *      private final String description;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
public @interface ApiEnumDescription
{
}
