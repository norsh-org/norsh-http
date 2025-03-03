package org.norsh.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is responsible for handling a specific type of exception.
 * This allows for centralized exception handling in the application.
 *
 * The method annotated with `@ThrowableHandler` must accept a parameter of the specified exception type,
 * along with an instance of {@link org.norsh.rest.RestRequest} and {@link org.norsh.rest.RestResponse}.
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * {@code
 * public class ApiThrowableHandler {
 * 
 *     @ThrowableHandler(Exception.class)
 *     public void handleGenericException(RestRequest request, RestResponse response, Throwable ex) throws IOException {
 *         response.setBody(500, Map.of("error", true, "message", ex.getMessage()));
 *         response.writeResponse();
 *     }
 *
 *     @ThrowableHandler(NorshException.class)
 *     public void handleNorshException(RestRequest request, RestResponse response, NorshException ex) throws IOException {
 *         response.setBody(500, Map.of("error", true, "message", ex.getMessage(), "details", ex.getDetails()));
 *         response.writeResponse();
 *     }
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ThrowableHandler {

    /**
     * Specifies the type of exception that this handler is responsible for.
     *
     * @return The exception class that this handler will catch.
     */
    Class<? extends Throwable> value();
}
