package org.norsh.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.norsh.util.Converter;

/**
 * Represents an HTTP request, handling parsing of method, headers, URL parameters, query strings, and body content.
 * Supports automatic JSON conversion for request bodies.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class RestRequest {
    
    private RestMethod restMethod;
    private HttpStatus httpStatus = HttpStatus.OK;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, List<String>> query = new HashMap<>();
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private String path = null;
    private String raw = null;
    private Object body = null;
    private String queryString = null;
    private Endpoint endpoint = null;
    private Object handler;
    private Method method;
    private Boolean closeConnection = true;

    /**
     * Parses an incoming HTTP request from a socket connection.
     *
     * @param socket The client socket connection.
     * @param server The HTTP server instance managing requests.
     * @return A parsed `RestRequest` instance.
     */
    public static RestRequest build(Socket socket, HttpServer server) {
        RestRequest request = new RestRequest();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 2048)) {
            String line = in.readLine();
            if (line == null || line.isEmpty()) {
                request.httpStatus = HttpStatus.BAD_REQUEST;
                return request;
            }

            String[] parts = line.split(" ");
            if (parts.length < 3) {
                request.httpStatus = HttpStatus.BAD_REQUEST;
                return request;
            }

            request.restMethod = RestMethod.parse(parts[0]);
            if (request.restMethod == null) {
                request.httpStatus = HttpStatus.METHOD_NOT_ALLOWED;
                return request;
            }

            String url = parts[1];
            if (url.contains("?")) {
                request.queryString = url.substring(url.indexOf("?") + 1);
                url = url.substring(0, url.indexOf("?"));
            }

            Map<String, Endpoint> endpoints = server.getEndpoints();
            String methodUrl = request.restMethod + ":" + url;
            for (String template : endpoints.keySet()) {
                if (methodUrl.matches(template)) {
                    request.endpoint = endpoints.get(template);
                    request.path = url;
                    request.handler = request.endpoint.getHandler();
                    request.method = request.endpoint.getMethod();
                    break;
                }
            }

            if (request.endpoint == null) {
                request.httpStatus = HttpStatus.NOT_FOUND;
                return request;
            }

            while ((line = in.readLine()) != null && !line.trim().isEmpty()) {
                String[] keyValue = line.split(":", 2);
                if (keyValue.length == 2) {
                    request.headers.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }

            int contentLength = Integer.parseInt(request.headers.getOrDefault("Content-Length", "-1").trim());
            if (contentLength < 0 && (request.restMethod == RestMethod.POST || request.restMethod == RestMethod.PUT)) {
                request.httpStatus = HttpStatus.LENGTH_REQUIRED;
                return request;
            }
            if (contentLength > 1024) {
                request.httpStatus = HttpStatus.PAYLOAD_TOO_LARGE;
                return request;
            }

            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int totalRead = in.read(bodyChars, 0, contentLength);
                request.raw = new String(bodyChars, 0, totalRead);

                try {
                    request.body = Converter.fromJson(request.raw, Object.class);
                } catch (Exception ex) {
                    request.httpStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
                    return request;
                }
            }

            if ("keep-alive".equalsIgnoreCase(request.headers.getOrDefault("Connection", "").trim())) {
                request.closeConnection = false;
            }

            extractPathParams(request.endpoint, request);
            extractQueryString(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return request;
    }

    /**
     * Extracts path parameters from the request URL using regex pattern matching.
     *
     * @param endpoint The matched endpoint.
     * @param request The request instance.
     */
    private static void extractPathParams(Endpoint endpoint, RestRequest request) {
        Matcher matcher = endpoint.getPattern().matcher(request.getPath());
        if (!matcher.matches()) return;

        for (int i = 1; i <= matcher.groupCount(); i++) {
            request.parameters.put(endpoint.getMethod().getParameters()[i - 1].getName(), matcher.group(i));
        }
    }

    /**
     * Extracts query string parameters from the request URL.
     *
     * @param request The request instance containing the query string.
     */
    public static void extractQueryString(RestRequest request) {
        if (request.queryString == null) {
            return;
        }

        String[] pairs = request.queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");

                    request.query.computeIfAbsent(key, _ -> new ArrayList<>()).add(value);
                } else {
                    request.query.computeIfAbsent(pair, _ -> new ArrayList<>()).add("");
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Error decoding query string", e);
            }
        }
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Object getHandler() {
        return handler;
    }

    public Method getMethod() {
        return method;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public RestMethod getRestMethod() {
        return restMethod;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getBody() {
        return body;
    }

    /**
     * Retrieves the parsed JSON body as an object of the specified type.
     *
     * @param <T> The expected type.
     * @param type The class type to convert the body to.
     * @return The parsed object.
     */
    public <T> T getBody(Class<T> type) {
        return Converter.fromJson(raw, type);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, List<String>> getQuery() {
        return query;
    }

    public void setCloseConnection(Boolean closeConnection) {
        this.closeConnection = closeConnection;
    }

    public Boolean getCloseConnection() {
        return closeConnection;
    }
}
