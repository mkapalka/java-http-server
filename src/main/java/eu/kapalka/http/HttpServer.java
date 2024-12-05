package eu.kapalka.http;

import eu.kapalka.http.handler.ResourceRequestHandler;
import eu.kapalka.http.handler.TopLevelRequestHandler;
import eu.kapalka.http.request.RequestParser;
import eu.kapalka.http.response.HttpStatus;
import eu.kapalka.http.response.Response;
import eu.kapalka.http.response.ResponseWriter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.*;

/**
 * Entry point class of the HTTP server implementation. See {@link Main} for an example of how to configure
 * and start the server, and the README.md file for a detailed description of the design and implementation choices.
 */
public class HttpServer {

    private static final Logger logger = Logger.getLogger(HttpServer.class.getName());

    // Max length of connection queue
    private static final int BACKLOG_SIZE = 100;

    // Size of the static thread pool
    private static final int THREADPOOL_SIZE = 10;

    // Timeout for each individual read operation on the data stream sent from the HTTP client.
    // It also limits the idle time between subsequent requests sent over the same connection (using keep-alive).
    private static final int SOCKET_READ_TIMEOUT_MS = 5_000;

    // Max time for processing each HTTP request, including the idle time since the end of the latest request
    // over the same connection.
    private static final long REQUEST_TIMEOUT_MS = 10_000;

    // Max number of HTTP requests that can be served over the same connection. Together with REQUEST_TIMEOUT_MS
    // it limits the time an individual thread in the thread pool can be blocked by a single client connection.
    private static final int MAX_NUM_REQUESTS_PER_CONNECTION = 10;

    private final String bindAddress;
    private final int port;
    private final TopLevelRequestHandler topLevelRequestHandler;

    private ServerSocket socket;
    private ExecutorService threadPool;
    private ScheduledExecutorService timerThreadPool;

    public HttpServer(String bindAddress, int port) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.topLevelRequestHandler = new TopLevelRequestHandler();
    }

    /**
     * Register <code>requestHandler</code> that will handle all requests with URI path starting with the given
     * prefix <code>pathPrefix</code>.
     */
    public void registerRequestHandler(String pathPrefix, ResourceRequestHandler requestHandler) {
        topLevelRequestHandler.registerRequestHandler(pathPrefix, requestHandler);
    }

    /**
     * Start the HTTP server. This method blocks until the server is stopped with method {@link #stop()}
     * or shut down because of an error.
     */
    public void start() {
        try {
            threadPool = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            timerThreadPool = Executors.newSingleThreadScheduledExecutor();
            socket = new ServerSocket(port, BACKLOG_SIZE, InetAddress.getByName(bindAddress));
            logger.log(INFO, "HTTP server started on {0} port {1}", new Object[]{bindAddress, socket.getLocalPort()});
            while (true) {
                var connection = socket.accept();
                logger.log(FINE, "New connection from {0}", connection.getRemoteSocketAddress());

                threadPool.execute(() -> handleConnection(connection));
            }
        } catch (IOException ex) {
            logger.log(SEVERE, "HTTP server shut down because of network error", ex);
        }
    }

    /**
     * Stop the running HTTP server.
     */
    public void stop() throws IOException {
        logger.log(INFO, "Shutting down the HTTP server");
        if (socket != null) {
            threadPool.shutdown();
            timerThreadPool.shutdown();
            socket.close();
        }
    }

    /**
     * Returns the port on which the server is listening.
     */
    public int getPort() {
        return socket.getLocalPort();
    }

    private void handleConnection(Socket connection) {
        try (connection) {
            connection.setSoTimeout(SOCKET_READ_TIMEOUT_MS);
            handleRequestStream(connection);
        } catch (IOException ex) {
            logger.log(FINE, "Communication with HTTP client interrupted because of I/O error or timeout", ex);
        }
    }

    private void handleRequestStream(Socket connection) throws IOException {
        var responseWriter = new ResponseWriter(connection.getOutputStream());
        try {
            var requestParser = new RequestParser(connection.getInputStream());
            // Handle multiple requests on the same connection if keep-alive is requested by the client
            int numRequests = 0;
            boolean keepAlive = false;
            do {
                // Set timeout for serving the request (cannot be set on the socket itself)
                var timeoutTask = timerThreadPool.schedule(() -> closeConnection(connection),
                        REQUEST_TIMEOUT_MS, MILLISECONDS);

                var request = requestParser.parse();
                keepAlive = topLevelRequestHandler.handleRequest(request, responseWriter);

                timeoutTask.cancel(false);
                numRequests++;
            } while (keepAlive && numRequests < MAX_NUM_REQUESTS_PER_CONNECTION);

            logger.log(FINE, "Connection is to be closed after {0} requests", numRequests);
        } catch (RuntimeException ex) {
            logger.log(SEVERE, "Internal server error", ex);
            handleServerError(ex, responseWriter);
        }
    }

    private void closeConnection(Socket connection) {
        if (connection.isClosed()) {
            return;
        }

        logger.log(FINE, "Closing connection because of request timeout");
        try {
            connection.close();
        } catch (IOException ex) {
            // Ignore
        }
    }

    private void handleServerError(Exception exception, ResponseWriter responseWriter) throws IOException {
        logger.log(WARNING, "Unexpected exception when handling request, sending HTTP 500", exception);
        if (responseWriter == null) {
            return;
        }

        var response = Response.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Server encountered an internal error while processing the request")
                .build();
        responseWriter.writeFull(response, false);
    }
}
