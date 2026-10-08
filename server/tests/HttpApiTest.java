import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Real HTTP fixtures exercise our routes and wire contract, without mocking the server. */
public final class HttpApiTest {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static String base;
    private static int passed;
    private record Reply(int code, JsonObject body, HttpResponse<String> raw) {}

    public static void main(String[] args) throws Exception {
        try (var server = new GameServer("127.0.0.1", 0)) {
            server.start();
            base = "http://127.0.0.1:" + server.port();
            check("create assigns X with Alex's exact fields", () -> {
                var reply = post("/game/create", "{}");
                equal(200, reply.code());
                keys(reply.body(), "gameId", "player", "board", "status");
                field(reply, "player", "X"); field(reply, "board", "---------");
                field(reply, "status", "WAITING_FOR_PLAYER");
                require(!reply.body().get("gameId").getAsString().isBlank());
                require(reply.raw().headers().firstValue("Content-Type").orElse("").contains("application/json"));
            });
            check("waiting state and result", () -> {
                var id = create();
                var state = get("/game/state?gameId=" + id);
                keys(state.body(), "gameId", "board", "currentPlayer", "status");
                field(state, "status", "WAITING_FOR_PLAYER");
                require(state.body().get("currentPlayer").isJsonNull());
                keys(get("/game/result?gameId=" + id).body(), "status");
                var rejected = move(id, "X", 4);
                equal(200, rejected.code()); require(!rejected.body().get("valid").getAsBoolean());
                field(get("/game/state?gameId=" + id), "board", "---------");
            });
            check("join assigns O and X gets first turn", () -> {
                var id = create();
                var reply = join(id);
                equal(200, reply.code());
                keys(reply.body(), "gameId", "player", "board", "status");
                field(reply, "gameId", id); field(reply, "player", "O");
                field(reply, "status", "IN_PROGRESS");
                field(get("/game/state?gameId=" + id), "currentPlayer", "X");
            });
            check("Alex's state and center move examples", () -> {
                var id = ready(); move(id, "X", 0); move(id, "O", 3);
                var state = get("/game/state?gameId=" + id);
                field(state, "board", "X--O-----"); field(state, "currentPlayer", "X");
                var response = move(id, "X", 4);
                keys(response.body(), "valid", "board", "status");
                require(response.body().get("valid").getAsBoolean());
                field(response, "board", "X--OX----"); field(response, "status", "IN_PROGRESS");
            });
            check("occupied and wrong-turn responses preserve state", () -> {
                var id = ready(); move(id, "X", 4);
                var before = get("/game/state?gameId=" + id).body();
                var occupied = move(id, "O", 4);
                equal(200, occupied.code()); keys(occupied.body(), "valid", "reason");
                require(!occupied.body().get("valid").getAsBoolean());
                field(occupied, "reason", "Position already taken");
                require(!move(id, "X", 0).body().get("valid").getAsBoolean());
                equal(before, get("/game/state?gameId=" + id).body());
            });
            check("X win and completed game rejection", () -> {
                var id = ready(); play(id, new int[]{0,3,1,4,2});
                var state = get("/game/state?gameId=" + id);
                field(state, "board", "XXXOO----"); field(state, "status", "WIN");
                require(state.body().get("currentPlayer").isJsonNull());
                var result = get("/game/result?gameId=" + id);
                keys(result.body(), "status", "winner"); field(result, "winner", "X");
                require(!move(id, "O", 5).body().get("valid").getAsBoolean());
                equal(state.body(), get("/game/state?gameId=" + id).body());
            });
            check("O win result", () -> {
                var id = ready(); play(id, new int[]{3,0,4,1,8,2});
                field(get("/game/result?gameId=" + id), "winner", "O");
            });
            check("draw result omits winner", () -> {
                var id = ready(); play(id, new int[]{0,1,2,4,3,5,7,6,8});
                var result = get("/game/result?gameId=" + id);
                keys(result.body(), "status"); field(result, "status", "DRAW");
            });
            check("new games are independent", () -> {
                var id = ready(); move(id, "X", 0);
                var second = create(); require(!id.equals(second));
                field(get("/game/state?gameId=" + second), "board", "---------");
                field(get("/game/state?gameId=" + id), "board", "X--------");
            });
            check("duplicate join cannot reset a game", () -> {
                var id = ready(); move(id, "X", 4);
                equal(409, join(id).code());
                field(get("/game/state?gameId=" + id), "board", "----X----");
            });
            check("unknown game is 404 across endpoints", () -> {
                equal(404, join("missing").code());
                equal(404, get("/game/state?gameId=missing").code());
                equal(404, get("/game/result?gameId=missing").code());
                var missing = move("missing", "X", 0);
                equal(404, missing.code()); keys(missing.body(), "valid", "reason");
            });
            check("exact routes reject suffixes", () -> equal(404, post("/game/create/extra", "{}").code()));
            check("wrong methods advertise Allow", () -> {
                for (String route : List.of("/game/create", "/game/join", "/game/move")) {
                    var reply = get(route); equal(405, reply.code());
                    equal("POST", reply.raw().headers().firstValue("Allow").orElse(""));
                }
                for (String route : List.of("/game/state", "/game/result")) {
                    var reply = post(route, "{}"); equal(405, reply.code());
                    equal("GET", reply.raw().headers().firstValue("Allow").orElse(""));
                }
            });
            String id = ready();
            for (String value : List.of("-1", "9", "4.0", "4e0", "\"4\"", "null", "true", "[]", "{}", "999999999999999999999999999")) {
                check("reject invalid position " + value, () -> {
                    var before = get("/game/state?gameId=" + id).body();
                    var reply = post("/game/move", "{\"gameId\":\"" + id + "\",\"player\":\"X\",\"position\":" + value + "}");
                    equal(400, reply.code()); require(!reply.body().get("valid").getAsBoolean());
                    equal(before, get("/game/state?gameId=" + id).body());
                });
            }
            check("missing and wrong-type fields rejected", () -> {
                for (String body : List.of("{}", "{\"gameId\":null}", "{\"gameId\":1}", "{\"gameId\":\" \"}"))
                    equal(400, post("/game/join", body).code());
                for (String player : List.of("\"x\"", "\"Z\"", "null", "1", "true"))
                    equal(400, post("/game/move", "{\"gameId\":\"" + id + "\",\"player\":" + player + ",\"position\":0}").code());
                equal(400, post("/game/move", "{\"gameId\":\"" + id + "\",\"player\":\"X\"}").code());
            });
            for (String body : List.of("", "{", "[]", "null", "{} {}", "{'x':1}", "{\"x\":1,}", "/*comment*/{}")) {
                check("strict JSON rejects " + body, () -> equal(400, post("/game/create", body).code()));
            }
            check("query requires one valid gameId", () -> {
                for (String query : List.of("", "?gameId=", "?gameId=%20", "?gameId=" + id + "&gameId=" + id))
                    equal(400, get("/game/state" + query).code());
                field(get("/game/state?game%49d=" + id), "gameId", id);
            });
            check("JSON media type checked and charset accepted", () -> {
                equal(415, request("POST", "/game/create", "{}", "text/plain").code());
                equal(415, request("POST", "/game/create", "{}", null).code());
                equal(200, request("POST", "/game/create", "{}", "application/json; charset=utf-8").code());
            });
            check("body size is bounded", () -> {
                equal(200, post("/game/create", "{}" + " ".repeat(1022)).code());
                equal(413, post("/game/create", "{}" + " ".repeat(1023)).code());
            });
            check("concurrent HTTP joins admit one player", () -> {
                var gameId = create();
                var responses = race(() -> join(gameId));
                equal(1L, responses.stream().filter(r -> r.code() == 200).count());
                equal(1L, responses.stream().filter(r -> r.code() == 409).count());
            });
            check("concurrent HTTP moves consume one turn", () -> {
                var gameId = ready();
                var responses = race(() -> move(gameId, "X", 4));
                equal(1L, responses.stream().filter(r -> r.body().get("valid").getAsBoolean()).count());
                field(get("/game/state?gameId=" + gameId), "board", "----X----");
                field(get("/game/state?gameId=" + gameId), "currentPlayer", "O");
            });
        }
        System.out.println("HttpApiTest: " + passed + " passed, 0 failed");
    }

    private static Reply request(String method, String path, String body, String contentType) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5));
        if (contentType != null) builder.header("Content-Type", contentType);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), JsonParser.parseString(response.body()).getAsJsonObject(), response);
    }
    private static Reply post(String path, String body) throws Exception { return request("POST", path, body, "application/json"); }
    private static Reply get(String path) throws Exception { return request("GET", path, null, null); }
    private static String create() throws Exception { return post("/game/create", "{}").body().get("gameId").getAsString(); }
    private static Reply join(String id) throws Exception { return post("/game/join", "{\"gameId\":\"" + id + "\"}"); }
    private static String ready() throws Exception { String id = create(); equal(200, join(id).code()); return id; }
    private static Reply move(String id, String player, int position) throws Exception {
        return post("/game/move", "{\"gameId\":\"" + id + "\",\"player\":\"" + player + "\",\"position\":" + position + "}");
    }
    private static void play(String id, int[] positions) throws Exception {
        for (int i = 0; i < positions.length; i++) require(move(id, i % 2 == 0 ? "X" : "O", positions[i]).body().get("valid").getAsBoolean());
    }
    private static List<Reply> race(java.util.concurrent.Callable<Reply> action) throws Exception {
        var pool = Executors.newFixedThreadPool(2); var barrier = new CyclicBarrier(2);
        try {
            java.util.concurrent.Callable<Reply> task = () -> { barrier.await(5, TimeUnit.SECONDS); return action.call(); };
            var a = pool.submit(task); var b = pool.submit(task);
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
    private static void keys(JsonObject value, String... fields) { equal(Set.of(fields), value.keySet()); }
    private static void field(Reply reply, String name, String expected) { equal(expected, reply.body().get(name).getAsString()); }
    private static void require(boolean value) { if (!value) throw new AssertionError("Condition failed"); }
    private static void equal(Object want, Object got) {
        if (!java.util.Objects.equals(want, got)) throw new AssertionError("Expected " + want + ", got " + got);
    }
    private static void check(String name, Checked action) throws Exception {
        action.run(); passed++; System.out.println("PASS " + name);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
}
