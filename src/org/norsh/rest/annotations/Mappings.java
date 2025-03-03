package org.norsh.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A container annotation for defining multiple {@link Mapping} annotations on a single method.
 * This allows a method to handle multiple HTTP routes or methods.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see Mapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Mappings {

    /**
     * Defines an array of {@link Mapping} annotations.
     *
     * @return An array of mappings associated with the annotated method.
     */
    Mapping[] value();
}
