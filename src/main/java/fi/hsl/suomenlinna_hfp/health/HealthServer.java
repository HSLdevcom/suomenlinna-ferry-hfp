package fi.hsl.suomenlinna_hfp.health;

import com.sun.net.httpserver.*;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public class HealthServer {
    private static final Logger log = LoggerFactory.getLogger(HealthServer.class);

    public final int port;
    public final HttpServer httpServer;
    private List<BooleanSupplier> checks = new ArrayList<>();

    public HealthServer(final int port) throws IOException {
        this.port = port;
        log.info("Creating HealthServer, listening port {}", port);
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", createDefaultHandler());
        httpServer.createContext("/health", createHandler());
        httpServer.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        }));
        httpServer.start();
        log.info("HealthServer started");
    }

    private void writeResponse(final HttpExchange httpExchange, final int responseCode, final String responseBody) throws IOException {
        final byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        httpExchange.sendResponseHeaders(responseCode, response.length);
        try (OutputStream out = httpExchange.getResponseBody()) {
            out.write(response);
        }
    }

    private HttpHandler createDefaultHandler() {
        return httpExchange -> {
            final int responseCode = 404;
            final String responseBody = "Not Found";
            writeResponse(httpExchange, responseCode, responseBody);
        };
    }

    private HttpHandler createHandler() {
        return httpExchange -> {
            String method = httpExchange.getRequestMethod();
            int responseCode;
            String responseBody;
            if (!method.equals("GET")) {
                responseCode = 405;
                responseBody = "Method Not Allowed";
            } else {
                final boolean isHealthy = checkHealth();
                responseCode = isHealthy ? 200 : 503;
                responseBody = isHealthy ? "OK" : "FAIL";
            }
            writeResponse(httpExchange, responseCode, responseBody);
        };
    }

    public void addCheck(final BooleanSupplier check) {
        if (check != null) {
            checks.add(check);
        }
    }

    public void removeCheck(final BooleanSupplier check) {
        checks.remove(check);
    }

    public void clearChecks() {
        checks.clear();
    }

    public boolean checkHealth() {
        for (final BooleanSupplier check : checks) {
            if (!check.getAsBoolean()) {
                return false;
            }
        }
        return true;
    }

    public void close() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }
}
