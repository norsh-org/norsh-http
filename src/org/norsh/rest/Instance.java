package org.norsh.rest;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;

import org.norsh.rest.annotations.ThrowableHandler;

/**
 * Handles an individual HTTP request instance, processing requests, 
 * invoking handlers, and managing error handling.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
public class Instance {
	/**
	 * Processes an HTTP request using the provided socket and server instance.
	 *
	 * @param socket The client socket connection.
	 * @param server The HTTP server instance handling requests.
	 */
	public Instance(Socket socket, HttpServer server) {
		RestRequest request = null;
		RestResponse response = null;

		try {
			request = RestRequest.build(socket, server);
			OutputStream out = socket.getOutputStream();
			response = new RestResponse(request, out);

			if (request.getHttpStatus() != HttpStatus.OK) {
				response.setStatus(request.getHttpStatus().getCode());
				return;
			}

			response.setStatus(200);

			Method method = request.getMethod();
			Object[] args = buildArguments(method, request, response);

			Long start = System.currentTimeMillis();
			method.invoke(request.getHandler(), args);

			Long end = System.currentTimeMillis();
			response.setDuration(end - start);

			response.writeResponse();
		} catch (InvocationTargetException e) {
			throwableHandler(server, request, response, e.getCause());
		} catch (Throwable t) {
			throwableHandler(server, request, response, t);
		}
	}

	/**
	 * Constructs the method arguments dynamically based on parameter types.
	 *
	 * @param method The target method to invoke.
	 * @param request The HTTP request object.
	 * @param response The HTTP response object.
	 * @return An array of arguments matching the method signature.
	 */
	private Object[] buildArguments(Method method, RestRequest request, RestResponse response) {
		Object[] args = new Object[method.getParameterCount()];
		for (int i = 0; i < args.length; i++) {
			Parameter parameter = method.getParameters()[i];
			if (parameter.getType().equals(RestRequest.class)) {
				args[i] = request;
			} else if (parameter.getType().equals(RestResponse.class)) {
				args[i] = response;
			}
		}
		
		return args;
	}

	/**
	 * Retrieves the appropriate throwable handler method based on the exception type.
	 *
	 * @param httpServer The HTTP server instance.
	 * @param t The exception thrown during request processing.
	 * @param identical If true, only exact type matches will be considered.
	 * @param genericCatch If true, allows catching generic `Exception` or `Throwable`.
	 * @return The matching method, or null if no suitable handler is found.
	 */
	private Method throwableHandlerMethod(HttpServer httpServer, Throwable t, boolean identical, boolean genericCatch) {
		for (Method method : httpServer.getThrowableHandler().getClass().getMethods()) {
			ThrowableHandler throwableHandler = method.getAnnotation(ThrowableHandler.class);

			if (throwableHandler == null) continue;

			if (identical && throwableHandler.value().equals(t.getClass())) {
				return method;
			}

			if (throwableHandler.value().equals(Exception.class) || throwableHandler.value().equals(Throwable.class)) {
				if (genericCatch) return method;
			} else {
				if (!identical && throwableHandler.value().isInstance(t)) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Handles thrown exceptions using the appropriate handler method.
	 *
	 * @param httpServer The HTTP server instance.
	 * @param request The HTTP request object.
	 * @param response The HTTP response object.
	 * @param t The thrown exception to handle.
	 */
	private void throwableHandler(HttpServer httpServer, RestRequest request, RestResponse response, Throwable t) {
		Method method = throwableHandlerMethod(httpServer, t, true, false);
		if (method == null) {
			method = throwableHandlerMethod(httpServer, t, false, false);
			if (method == null) {
				method = throwableHandlerMethod(httpServer, t, false, true);
			}
		}

		if (method == null) return;

		Object[] args = new Object[method.getParameterCount()];
		for (int i = 0; i < args.length; i++) {
			Parameter parameter = method.getParameters()[i];
			if (parameter.getType().equals(RestRequest.class)) {
				args[i] = request;
			} else if (parameter.getType().equals(RestResponse.class)) {
				args[i] = response;
			} else if (parameter.getType().equals(HttpServer.class)) {
				args[i] = httpServer;
			} else if (parameter.getType().isInstance(t)) {
				args[i] = t;
			}
		}

		try {
			method.invoke(httpServer.getThrowableHandler(), args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}