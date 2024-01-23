package com.proit.app.database.configuration.jpa.eclipselink.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Аннотация позволяет указать принудительное read only поведение над атрибутом entity класса.
 *
 * @see ReadOnlyFieldAnnotationExtension
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ReadOnlyColumn
{
}
