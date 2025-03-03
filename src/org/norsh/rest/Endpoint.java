package org.norsh.rest;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * Represents an HTTP endpoint, mapping a URL path to a handler method.
 * Supports dynamic path parameters using regex conversion.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class Endpoint {
    
    /** The registered path for this endpoint. */
    private final String path;

    /** The handler instance responsible for processing the request. */
    private final Object handler;

    /** The method that should be invoked when this endpoint is matched. */
    private final Method method;

    /** The generated regex pattern for path matching. */
    private final String regex;

    /** The compiled pattern used to match incoming requests. */
    private final Pattern pattern;

    /** The HTTP method (GET, POST, etc.) required for this endpoint. */
    private final RestMethod httpMethod;

    /**
     * Converts a path with placeholders (e.g., `/user/{id}`) into a regex pattern.
     *
     * @param path The raw path containing placeholders.
     * @return The regex pattern matching dynamic segments.
     */
    private static String pathToTemplate(String path) {
        return path.replaceAll("\\{([a-zA-Z0-9_]+)\\}", "(?<$1>.*?)");
    }

    /**
     * Constructs a new endpoint with the given HTTP method, path, handler, and method reference.
     *
     * @param httpMethod The HTTP method type (GET, POST, PUT, DELETE).
     * @param path The raw URL path associated with this endpoint.
     * @param handler The instance handling the request.
     * @param method The method to invoke when the endpoint is triggered.
     */
    public Endpoint(RestMethod httpMethod, String path, Object handler, Method method) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.handler = handler;
        this.method = method;
        
        this.regex = "^" + httpMethod + "[:]" + pathToTemplate(path) + "$";
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Retrieves the HTTP method associated with this endpoint.
     *
     * @return The HTTP method type.
     */
    public RestMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Retrieves the handler instance responsible for processing this request.
     *
     * @return The handler object.
     */
    public Object getHandler() {
        return handler;
    }

    /**
     * Retrieves the method to be invoked when this endpoint is matched.
     *
     * @return The handler method.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Retrieves the raw path for this endpoint.
     *
     * @return The registered path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Retrieves the regex pattern used for matching requests.
     *
     * @return The compiled regex pattern.
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Retrieves the regex template representation of this path.
     *
     * @return The regex string template.
     */
    public String getRegex() {
        return regex;
    }
}
