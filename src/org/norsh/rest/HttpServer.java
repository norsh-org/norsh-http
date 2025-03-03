package org.norsh.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.norsh.exceptions.ArgumentException;
import org.norsh.exceptions.InternalException;
import org.norsh.rest.annotations.Mapping;

/**
 * A lightweight HTTP server implementation that supports both HTTP and HTTPS,
 * dynamically registers endpoints, and manages execution threads.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class HttpServer {

    private volatile boolean isRunning = false;
    private final Map<String, Endpoint> endpoints = new HashMap<>();
    private Object throwableHandler;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private int threads = Runtime.getRuntime().availableProcessors();
    private int backlog = 50;
    private String keystore;
    private String password;

    /**
     * Registers a new endpoint by scanning its methods for `@Mapping` annotations.
     *
     * @param routeType The class containing HTTP request handlers.
     */
    public void addEndpoint(Class<?> routeType) {
        try {
            Object handler = routeType.getConstructor().newInstance();

            for (Method method : routeType.getMethods()) {
                for (Mapping mapping : method.getAnnotationsByType(Mapping.class)) {
                    for (RestMethod httpMethod : mapping.method()) {
                        for (String url : mapping.value()) {
                            Endpoint endpoint = new Endpoint(httpMethod, url, handler, method);
                            endpoints.put(endpoint.getRegex(), endpoint);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new InternalException("Failed to register endpoint: " + routeType.getName(), e);
        }
    }

    /**
     * Starts the HTTP or HTTPS server on the specified port.
     *
     * @param port The port number to listen on.
     * @param useSSL Whether to enable SSL/TLS.
     */
    public void start(int port, boolean useSSL) {
        try {
            isRunning = true;
            serverSocket = useSSL ? createSSLServerSocket(port) : new ServerSocket(port);

            serverSocket.setPerformancePreferences(2, 1, 0);
            threadPool = Executors.newFixedThreadPool(threads);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> new Instance(clientSocket, this));
            }
        } catch (Exception ex) {
            throw new InternalException("Error while starting the server", ex);
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
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
    }

    /**
     * Configures the SSL keystore for HTTPS.
     *
     * @param keystore The path to the JKS keystore file.
     * @param password The password for the keystore.
     */
    public void setKeystore(String keystore, String password) {
        if (keystore == null || password == null) {
            throw new ArgumentException("SSL requires a valid JKS keystore file and password.");
        }
        this.keystore = keystore;
        this.password = password;
    }

    /**
     * Creates an SSL server socket for secure HTTPS communication.
     *
     * @param port The port number for HTTPS.
     * @return An SSLServerSocket instance.
     * @throws Exception If an error occurs while loading the keystore or initializing SSL context.
     */
    private SSLServerSocket createSSLServerSocket(int port) throws Exception {
        if (keystore == null || password == null) {
            throw new ArgumentException("SSL requires a valid JKS keystore file and password.");
        }

        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreFile = new FileInputStream(keystore)) {
            keyStore.load(keyStoreFile, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, password.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        return (SSLServerSocket) factory.createServerSocket(port);
    }

    public Map<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getThreads() {
        return threads;
    }

    public Object getThrowableHandler() {
        return throwableHandler;
    }

    public void setExceptionHandler(Object exceptionHandler) {
        this.throwableHandler = exceptionHandler;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
