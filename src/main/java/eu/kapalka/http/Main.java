package eu.kapalka.http;

import eu.kapalka.http.handler.StaticContentRequestHandler;
import eu.kapalka.http.repository.StaticFileRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.LogManager;

import static java.lang.Integer.parseInt;

/**
 * Example main class that sets up the HTTP server.
 */
public class Main {

    private final Path baseDir;
    private final String bindAddress;
    private final int portNumber;

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
        new Main(args).startServer();
    }

    private Main(String[] args) {
        if (args.length != 3) {
            printUsageAndExit();
        }
        this.baseDir = Path.of(args[0]);
        this.bindAddress = args[1];
        this.portNumber = parsePortNumber(args[2]);
   }

    private void startServer() {
        var fileRepository = new StaticFileRepository(baseDir);
        var staticFileHandler = new StaticContentRequestHandler(fileRepository);
        var server = new HttpServer(bindAddress, portNumber);
        server.registerRequestHandler("/", staticFileHandler);
        server.start();
    }

    private int parsePortNumber(String portNumberStr) {
        try {
            return parseInt(portNumberStr);
        } catch (NumberFormatException ex) {
            System.err.println("Invalid port number: " + portNumberStr);
            printUsageAndExit();
            return 0; // Not reachable but required to make compiler happy
        }
    }

    private void printUsageAndExit() {
        System.err.println("""
                Usage: run-server BASEDIR BINDADDR PORT
                
                Starts HTTP server using bind address BINDADDR and port number PORT.
                Files under directory BASEDIR are exposed as HTTP resources, e.g.,
                URL "/some/file.txt" corresponds to file "$BASEDIR/some/file.txt".
                """);
        System.exit(1);
    }
}
