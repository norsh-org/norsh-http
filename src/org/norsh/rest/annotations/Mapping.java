package org.norsh.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.norsh.rest.RestMethod;

/**
 * Defines an HTTP mapping for a method or class, allowing it to handle specific routes and HTTP methods.
 * Supports multiple routes and HTTP methods per annotation.
 *
 * This annotation is {@link Repeatable}, allowing multiple mappings on the same method or class
 * by using {@link Mappings} as a container.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see Mappings
 * @see RestMethod
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(Mappings.class)
public @interface Mapping {

    /**
     * Specifies one or more URL patterns that this mapping should handle.
     *
     * @return An array of route patterns.
     */
    String[] value() default "";

    /**
     * Defines the HTTP methods that this mapping supports.
     * Defaults to supporting GET, POST, PUT, and DELETE.
     *
     * @return An array of allowed HTTP methods.
     */
    RestMethod[] method() default {RestMethod.GET, RestMethod.POST, RestMethod.PUT, RestMethod.DELETE};
}
