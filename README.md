# Norsh HTTP Server

A high-performance HTTP server built specifically for Norsh, optimized for handling structured data exchanges within the ecosystem. Designed to eliminate non-essential processes, ensuring maximum efficiency and minimal overhead for Norsh operations.

## Features
- Native Java HTTP Server without external dependencies.
- Annotation-based routing (`@Mapping`, `@Mappings`).
- Automatic request parsing (query params, path variables, request body).
- Custom Exception Handling via `@ThrowableHandler`.
- Thread Pool Management for high-performance request handling.
- JSON serialization/deserialization for request and response bodies.
- SSL/TLS support for secure HTTPS connections.

---

## Installation

### Clone the repository
```sh
git clone https://github.com/norsh/http-server.git
cd http-server
```

### Install the project
```sh
mvn clean install
```

---

## Configuration

You can customize:
- Port Number
- Thread Pool Size
- Backlog Size
- Exception Handlers
- SSL/TLS Configuration

Example:
```java
HttpServer server = new HttpServer();
server.setThreads(8);
server.setBacklog(100);
server.start(8080, false); // HTTP mode
```

---

## Using SSL/TLS (HTTPS)

### 1️⃣ Generate a Keystore (For Testing)
```sh
keytool -genkeypair -alias norsh-server -keyalg RSA -keysize 2048 -keystore keystore.jks -validity 365
```

### 2️⃣ Configure the Server to Use SSL/TLS
```java
HttpServer server = new HttpServer();
server.setKeystore("keystore.jks", "password");
server.start(8443, true); // HTTPS mode
```

### 3️⃣ Test the HTTPS Connection
Using `curl`:
```sh
curl -k https://localhost:8443/hello
```

---

## Usage

### Define a REST Endpoint
Use `@Mapping` to define HTTP routes:

```java
import org.norsh.rest.annotations.Mapping;
import org.norsh.rest.RestRequest;
import org.norsh.rest.RestResponse;

public class ExampleHandler {

    @Mapping(value = "/hello", method = RestMethod.GET)
    public void sayHello(RestRequest request, RestResponse response) {
        response.setBody(200, Map.of("message", "Hello, World!"));
        response.writeResponse();
    }
}
```

### Register the Endpoint
```java
HttpServer server = new HttpServer();
server.addEndpoint(ExampleHandler.class);
server.start(8080, false); // HTTP Mode
```

### Handle Exceptions Globally
```java
import org.norsh.rest.annotations.ThrowableHandler;
import java.io.IOException;
import java.util.Map;

public class GlobalExceptionHandler {

    @ThrowableHandler(Exception.class)
    public void handleException(RestRequest request, RestResponse response, Throwable ex) throws IOException {
        response.setBody(500, Map.of("error", true, "message", ex.getMessage()));
        response.writeResponse();
    }
}
```
Register the handler:
```java
server.setExceptionHandler(new GlobalExceptionHandler());
```

---

## Annotations

### `@Mapping` - Define an HTTP route
```java
@Mapping(value = "/users/{id}", method = RestMethod.GET)
```

### `@Mappings` - Multiple mappings for a method
```java
@Mappings({
    @Mapping(value = "/user", method = RestMethod.GET),
    @Mapping(value = "/user/create", method = RestMethod.POST)
})
```

### `@ThrowableHandler` - Exception handling
```java
@ThrowableHandler(IllegalArgumentException.class)
```

---

## Project Structure
```
norsh-http
│── org/norsh/rest/
│   │── HttpServer.java
│   │── RestRequest.java
│   │── RestResponse.java
│   │── HttpStatus.java
│   │── Endpoint.java
│   └── annotations/
│       │── Mapping.java
│       │── Mappings.java
│       └── ThrowableHandler.java
└── org/norsh/api/handlers/
```

---

## License
This project is licensed under the Norsh Commons License (NCL).

---

## Contact
For any questions or contributions, feel free to reach out via:
- Website: [https://norsh.org](https://norsh.org)
- Documentation: [https://docs.norsh.org](https://docs.norsh.org)