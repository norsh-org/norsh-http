package org.norsh.rest;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.*;
import org.norsh.exceptions.InternalException;
import org.norsh.rest.annotations.Mapping;

/**
 * A lightweight HTTP server implementation that processes incoming requests, 
 * dynamically registers endpoints, and manages execution threads.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class HttpServer {

    /** Indicates if the server is currently running. */
    private volatile boolean isRunning = false;

    /** Stores registered HTTP endpoints mapped to their paths. */
    private final Map<String, Endpoint> endpoints = new HashMap<>();

    /** The handler for processing exceptions in request execution. */
    private Object throwableHandler;

    /** The main server socket listening for incoming connections. */
    private ServerSocket serverSocket;

    /** Thread pool for handling client requests concurrently. */
    private ExecutorService threadPool;

    /** Number of worker threads used for request processing. */
    private int threads = Runtime.getRuntime().availableProcessors();

    /** Maximum number of pending connections in the server socket queue. */
    private int backlog = 50;

    /**
     * Registers a new endpoint by scanning its methods for `@Mapping` annotations.
     *
     * @param routeType The class containing HTTP request handlers.
     */
    public void addEndpoint(Class<?> routeType) {
        Mapping type = routeType.getAnnotation(Mapping.class);
        if (type == null) {
            type = new Mapping() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Mapping.class;
                }

                @Override
                public String[] value() {
                    return new String[]{""};
                }

                @Override
                public RestMethod[] method() {
                    return new RestMethod[]{RestMethod.GET, RestMethod.POST, RestMethod.PUT, RestMethod.DELETE};
                }
            };
        }

        try {
            Object handler = routeType.getConstructor().newInstance();

            for (Method method : routeType.getMethods()) {
                for (String root : type.value()) {
                    for (Mapping mapping : method.getAnnotationsByType(Mapping.class)) {
                        for (RestMethod httpMethod : mapping.method()) {
                            for (String url : mapping.value()) {
                                Endpoint endpoint = new Endpoint(httpMethod, root.concat(url), handler, method);
                                endpoints.put(endpoint.getRegex(), endpoint);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new InternalException(e.getMessage(), e);
        }
    }

    /**
     * Starts the HTTP server on the specified port.
     *
     * @param port The port number to listen on.
     */
    public void start(int port, boolean useSSL) {
        try {
            isRunning = true;
            if (useSSL) {
                serverSocket = createSSLServerSocket(port);
            } else {
                serverSocket = new ServerSocket(port);
            }
    
            serverSocket.setPerformancePreferences(2, 1, 0);
            threadPool = Executors.newFixedThreadPool(threads);
    
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> new Instance(clientSocket, this));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }


    /**
     * Stops the HTTP server, closing all connections and shutting down the thread pool.
     *
     * @throws IOException If an error occurs while closing the server socket.
     */
    public void stop() throws IOException {
        isRunning = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        if (threadPool != null) {
            threadPool.shutdown(); // Ensures all running tasks complete before shutdown
        }
    }

    /**
     * Retrieves the map of registered endpoints.
     *
     * @return A map of endpoints where the key is the path and the value is the `Endpoint` instance.
     */
    public Map<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    /**
     * Sets the backlog size for pending connections in the server socket.
     *
     * @param backlog The maximum number of pending connections.
     */
    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    /**
     * Gets the current backlog size.
     *
     * @return The maximum number of pending connections.
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Sets the number of worker threads in the thread pool.
     *
     * @param threads The number of threads.
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Gets the current number of worker threads in the thread pool.
     *
     * @return The number of threads.
     */
    public int getThreads() {
        return threads;
    }

    /**
     * Retrieves the exception handler object.
     *
     * @return The exception handler instance.
     */
    public Object getThrowableHandler() {
        return throwableHandler;
    }

    /**
     * Sets the global exception handler for request processing errors.
     *
     * @param exceptionHandler The object that handles exceptions.
     */
    public void setExceptionHandler(Object exceptionHandler) {
        this.throwableHandler = exceptionHandler;
    }

    /**
     * Checks if the server is currently running.
     *
     * @return True if the server is running, false otherwise.
     */
    public boolean isRunning() {
        return isRunning;
    }

    private SSLServerSocket createSSLServerSocket(int port) throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (FileInputStream keyStoreFile = new FileInputStream("keystore.jks")) {
        keyStore.load(keyStoreFile, "password".toCharArray()); // Altere para a senha do seu keystore
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(keyStore, "password".toCharArray());

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, null);

    SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
    return (SSLServerSocket) factory.createServerSocket(port);
}
}
