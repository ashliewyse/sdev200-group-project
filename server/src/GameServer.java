import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTP adapter for the five endpoints agreed with the client author. */
public final class GameServer implements AutoCloseable {
    private static final int MAX_BODY_BYTES = 1024;
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).serializeNulls().create();
    private static final Map<String, String> METHODS = Map.of(
        "/game/create", "POST", "/game/join", "POST", "/game/state", "GET",
        "/game/move", "POST", "/game/result", "GET");
    private final GameRegistry games = new GameRegistry();
    private final HttpServer server;
    private final ExecutorService workers;

    public GameServer(String host, int port) throws IOException {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Host must not be blank");
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Port must be between 0 and 65535");
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        workers = Executors.newFixedThreadPool(8);
        server.setExecutor(workers);
        server.createContext("/", this::handle);
    }

    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); workers.shutdownNow(); }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            String method = METHODS.get(path);
            if (method == null) throw new ApiProblem(404, "Unknown endpoint");
            if (!method.equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", method);
                throw new ApiProblem(405, "Use " + method + " for this endpoint");
            }
            JsonObject input = method.equals("POST") ? readObject(exchange) : null;
            JsonObject response;
            switch (path) {
                case "/game/create" -> response = participant(games.create().snapshot(), "X");
                case "/game/join" -> {
                    var game = find(stringField(input, "gameId"));
                    try { response = participant(game.join(), "O"); }
                    catch (IllegalStateException ex) { throw new ApiProblem(409, ex.getMessage()); }
                }
                case "/game/state" -> response = state(find(queryGameId(exchange)).snapshot());
                case "/game/result" -> response = result(find(queryGameId(exchange)).snapshot());
                case "/game/move" -> {
                    String id = stringField(input, "gameId");
                    String player = stringField(input, "player");
                    if (!player.equals("X") && !player.equals("O")) throw new ApiProblem(400, "Player must be X or O");
                    int position = positionField(input);
                    var move = find(id).move(player, position);
                    response = new JsonObject();
                    response.addProperty("valid", move.valid());
                    if (move.valid()) {
                        response.addProperty("board", move.state().board());
                        response.addProperty("status", move.state().status());
                    } else response.addProperty("reason", move.reason());
                }
                default -> throw new ApiProblem(404, "Unknown endpoint");
            }
            send(exchange, 200, response);
        } catch (ApiProblem ex) {
            send(exchange, ex.status, error(path, ex.getMessage()));
        } catch (RuntimeException ex) {
            System.err.println("Request failed: " + ex);
            send(exchange, 500, error(path, "Internal server error"));
        } finally { exchange.close(); }
    }

    private TicTacToeGame find(String id) {
        var game = games.find(id);
        if (game == null) throw new ApiProblem(404, "Game not found");
        return game;
    }

    private static JsonObject readObject(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT).equals("application/json"))
            throw new ApiProblem(415, "Content-Type must be application/json");
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) throw new ApiProblem(413, "Request body exceeds 1024 bytes");
        try {
            JsonElement parsed = JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) throw new ApiProblem(400, "Body must be one JSON object");
            return parsed.getAsJsonObject();
        } catch (JsonParseException ex) { throw new ApiProblem(400, "Malformed JSON body"); }
    }

    private static String stringField(JsonObject input, String name) {
        JsonElement value = input.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().isBlank())
            throw new ApiProblem(400, name + " must be a nonempty string");
        return value.getAsString();
    }

    private static int positionField(JsonObject input) {
        JsonElement value = input.get("position");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                || !value.getAsString().matches("-?(0|[1-9][0-9]*)"))
            throw new ApiProblem(400, "Position must be an integer from 0 to 8");
        try {
            int position = Integer.parseInt(value.getAsString());
            if (position < 0 || position > 8) throw new NumberFormatException();
            return position;
        } catch (NumberFormatException ex) { throw new ApiProblem(400, "Position must be an integer from 0 to 8"); }
    }

    private static String queryGameId(HttpExchange exchange) {
        String raw = exchange.getRequestURI().getRawQuery();
        String id = null;
        if (raw != null) {
            try {
                for (String item : raw.split("&", -1)) {
                    String[] pair = item.split("=", 2);
                    if (URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals("gameId")) {
                        if (id != null) throw new ApiProblem(400, "Supply gameId exactly once");
                        id = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                    }
                }
            } catch (IllegalArgumentException ex) { throw new ApiProblem(400, "Invalid query encoding"); }
        }
        if (id == null || id.isBlank()) throw new ApiProblem(400, "Supply a nonempty gameId query parameter");
        return id;
    }

    private static JsonObject participant(TicTacToeGame.Snapshot value, String player) {
        var body = new JsonObject();
        body.addProperty("gameId", value.gameId()); body.addProperty("player", player);
        body.addProperty("board", value.board()); body.addProperty("status", value.status());
        return body;
    }

    private static JsonObject state(TicTacToeGame.Snapshot value) {
        var body = new JsonObject();
        body.addProperty("gameId", value.gameId()); body.addProperty("board", value.board());
        if (value.currentPlayer() == null) body.add("currentPlayer", JsonNull.INSTANCE);
        else body.addProperty("currentPlayer", value.currentPlayer());
        body.addProperty("status", value.status());
        return body;
    }

    private static JsonObject result(TicTacToeGame.Snapshot value) {
        var body = new JsonObject();
        body.addProperty("status", value.status());
        if (value.status().equals("WIN")) body.addProperty("winner", value.winner());
        return body;
    }

    private static JsonObject error(String path, String reason) {
        var body = new JsonObject();
        if (path.equals("/game/move")) { body.addProperty("valid", false); body.addProperty("reason", reason); }
        else body.addProperty("error", reason);
        return body;
    }

    private static void send(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] bytes = JSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        if (exchange.getRequestMethod().equals("HEAD")) exchange.sendResponseHeaders(status, -1);
        else {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private static final class ApiProblem extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int status;
        ApiProblem(int status, String reason) { super(reason); this.status = status; }
    }

    public static void main(String[] args) throws IOException {
        String host = "127.0.0.1";
        int port = 8080;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help" -> { System.out.println("Usage: GameServer [--host 127.0.0.1] [--port 8080]"); return; }
                case "--host" -> {
                    if (++i >= args.length) throw new IllegalArgumentException("--host requires a value");
                    host = args[i];
                }
                case "--port" -> {
                    if (++i >= args.length) throw new IllegalArgumentException("--port requires a value");
                    port = Integer.parseInt(args[i]);
                }
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        var gameServer = new GameServer(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(gameServer::close));
        gameServer.start();
        System.out.println("Tic-tac-toe server listening on http://" + host + ":" + gameServer.port());
        System.out.println("X creates a game; O joins its gameId. Press Ctrl+C to stop.");
    }
}
