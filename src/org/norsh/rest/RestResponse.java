package org.norsh.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.norsh.util.Converter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Handles the HTTP response, managing status codes, headers, and the response body.
 * Automatically converts the response body to JSON format if applicable.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Getter
@Setter
public class RestResponse {

    /** The HTTP status code for the response. */
    private Integer status;

    /** Stores HTTP headers in a linked order. */
    private final Map<String, String> headers = new LinkedHashMap<>();

    /** The response body object (if any). */
    @Setter(value = AccessLevel.NONE)
    private Object body;

    /** The output stream to write the response to. */
    private final OutputStream outputStream;

    /** The associated request instance. */
    private final RestRequest request;

    /**
     * Constructs a new HTTP response.
     *
     * @param request The original HTTP request.
     * @param outputStream The output stream for sending the response.
     */
    public RestResponse(RestRequest request, OutputStream outputStream) {
        this.request = request;
        this.outputStream = outputStream;
    }

    /**
     * Adds a header to the HTTP response.
     *
     * @param key The header name.
     * @param value The header value.
     */
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * Sets the response body along with a custom HTTP status code.
     *
     * @param status The HTTP status code.
     * @param body The response body object.
     */
    public void setBody(Integer status, Object body) {
        this.status = status;
        this.body = body;
    }

    /**
     * Sets the response body with a default HTTP status of 200 (OK).
     *
     * @param body The response body object.
     */
    public void setBody(Object body) {
        setBody(200, body);
    }

    /**
     * Writes the HTTP response to the output stream, including status, headers, and body.
     *
     * @throws IOException If an I/O error occurs while writing the response.
     */
    public void writeResponse() throws IOException {
        if (status == null) {
            status = request.getHttpStatus().getCode();
        }

        try (PrintWriter writer = new PrintWriter(outputStream, true)) {
            // Send HTTP status line
            writer.println("HTTP/1.1 " + status + " " + getHttpStatusMessage(status));

            // Serialize body if applicable
            Integer contentLength = 0;
            Object responseBody = body;
            if (responseBody != null) {
                responseBody = Converter.toJson(responseBody);
                contentLength = responseBody.toString().length();
            }

            // Set headers for response body
            if (contentLength > 0) {
                addHeader("Content-Type", "application/json; charset=UTF-8");
                addHeader("Content-Length", String.valueOf(contentLength));
            }

            // Connection management
            if (request.getCloseConnection()) {
                addHeader("Connection", "close");
            } else {
                addHeader("Connection", "keep-alive");
            }

            // Write headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                writer.println(header.getKey() + ": " + header.getValue());
            }

            writer.println(); // End of headers

            // Write body if applicable
            if (responseBody != null) {
                writer.print(responseBody);
            }

            writer.flush();
        }
    }

    /**
     * Retrieves the HTTP status message corresponding to the given status code.
     *
     * @param statusCode The HTTP status code.
     * @return The associated status message.
     */
    private String getHttpStatusMessage(int statusCode) {
        HttpStatus status = HttpStatus.fromCode(statusCode);
        return (status != null) ? status.getMessage() : "Unknown Status";
    }
}
