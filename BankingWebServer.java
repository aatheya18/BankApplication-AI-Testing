package com.nirma.banking;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankingWebServer {
    private static final String INDEX_FILE = "index.html";
    private final Bank bank = new Bank();
    private final HttpServer server;

    public BankingWebServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        seedDemoAccounts();
        registerRoutes();
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("bank.port", "8080"));
        BankingWebServer app = new BankingWebServer(port);
        app.start();
    }

    private void start() {
        server.start();
        System.out.println("Banking web app is running at http://localhost:8080");
        System.out.println("Use Ctrl+C to stop the server.");
    }

    private void registerRoutes() {
        server.createContext("/", this::serveRootPage);
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/accounts", this::handleAccounts);
        server.createContext("/api/accounts/", this::handleAccountRoute);
        server.createContext("/api/transfer", this::handleTransfer);
    }

    private void seedDemoAccounts() {
        try {
            bank.createAccount("ACC1001", "Alice Johnson", new BigDecimal("1500.00"));
            bank.createAccount("ACC1002", "Bob Smith", new BigDecimal("750.50"));
            bank.createAccount("ACC1003", "Charlie Davis", new BigDecimal("2400.00"));
        } catch (IllegalArgumentException ignored) {
            // Ignore if already seeded in a restart cycle
        }
    }

    private void serveRootPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        Path filePath = Paths.get(INDEX_FILE).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            sendJson(exchange, 404, "{\"error\":\"Frontend page not found\"}");
            return;
        }

        byte[] content = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String response = "{\"status\":\"UP\",\"accountCount\":" + bank.getAccountCount() + "}";
        sendJson(exchange, 200, response);
    }

    private void handleAccounts(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (BankAccount account : bank.getAllAccounts()) {
                result.add(accountToMap(account));
            }
            sendJson(exchange, 200, toJsonArray(result));
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            Map<String, String> payload = readPayload(exchange);
            String accountNumber = payload.get("accountNumber");
            String accountHolder = payload.get("accountHolder");
            String openingBalance = payload.get("openingBalance");

            try {
                BankAccount account = bank.createAccount(accountNumber, accountHolder, new BigDecimal(openingBalance));
                sendJson(exchange, 201, toJson(accountToMap(account)));
            } catch (Exception ex) {
                sendJson(exchange, 400, "{\"error\":\"" + safeJson(ex.getMessage()) + "\"}");
            }
            return;
        }

        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private void handleAccountRoute(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] sections = path.split("/");
        if (sections.length < 4) {
            sendJson(exchange, 404, "{\"error\":\"Account route not found\"}");
            return;
        }

        String accountNumber = sections[3];
        String action = sections.length > 4 ? sections[4] : null;

        try {
            BankAccount account = bank.findAccount(accountNumber);

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && action == null) {
                sendJson(exchange, 200, toJson(accountToMap(account)));
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && "history".equals(action)) {
                sendJson(exchange, 200, toJson(Map.of("accountNumber", account.getAccountNumber(), "transactionHistory", account.getTransactionHistory())));
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && "deposit".equals(action)) {
                Map<String, String> payload = readPayload(exchange);
                bank.deposit(accountNumber, new BigDecimal(payload.getOrDefault("amount", "0")));
                sendJson(exchange, 200, toJson(accountToMap(bank.findAccount(accountNumber))));
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && "withdraw".equals(action)) {
                Map<String, String> payload = readPayload(exchange);
                bank.withdraw(accountNumber, new BigDecimal(payload.getOrDefault("amount", "0")));
                sendJson(exchange, 200, toJson(accountToMap(bank.findAccount(accountNumber))));
                return;
            }

            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        } catch (Exception ex) {
            sendJson(exchange, 400, "{\"error\":\"" + safeJson(ex.getMessage()) + "\"}");
        }
    }

    private void handleTransfer(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            Map<String, String> payload = readPayload(exchange);
            String fromAccountNumber = payload.get("fromAccountNumber");
            String toAccountNumber = payload.get("toAccountNumber");
            String amount = payload.get("amount");

            bank.transfer(fromAccountNumber, toAccountNumber, new BigDecimal(amount));
            sendJson(exchange, 200, "{\"status\":\"SUCCESS\",\"fromAccount\":\"" + safeJson(fromAccountNumber)
                    + "\",\"toAccount\":\"" + safeJson(toAccountNumber) + "\",\"amount\":\"" + amount + "\"}");
        } catch (Exception ex) {
            sendJson(exchange, 400, "{\"error\":\"" + safeJson(ex.getMessage()) + "\"}");
        }
    }

    private Map<String, String> readPayload(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        String rawBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> payload = new LinkedHashMap<>();

        if (rawBody == null || rawBody.isBlank()) {
            return payload;
        }

        if (rawBody.startsWith("{")) {
            extractJsonPairs(rawBody, payload);
            return payload;
        }

        for (String pair : rawBody.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                payload.put(parts[0], java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }

        return payload;
    }

    private void extractJsonPairs(String rawBody, Map<String, String> payload) {
        Pattern pattern = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"");
        Matcher matcher = pattern.matcher(rawBody);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
            payload.put(key, value);
        }
    }

    private Map<String, Object> accountToMap(BankAccount account) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("accountNumber", account.getAccountNumber());
        map.put("accountHolder", account.getAccountHolder());
        map.put("balance", account.getBalance().toPlainString());
        map.put("active", account.isActive());
        map.put("transactionHistory", account.getTransactionHistory());
        return map;
    }

    private String toJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("\"").append(safeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(safeJson((String) value)).append("\"");
            } else if (value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof List) {
                json.append(toJsonArray((List<?>) value));
            } else {
                json.append("\"").append(safeJson(String.valueOf(value))).append("\"");
            }
        }
        json.append("}");
        return json.toString();
    }

    private String toJsonArray(List<?> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            Object value = values.get(i);
            if (value instanceof String) {
                json.append("\"").append(safeJson((String) value)).append("\"");
            } else {
                json.append(String.valueOf(value));
            }
        }
        json.append("]");
        return json.toString();
    }

    private void sendJson(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] response = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
