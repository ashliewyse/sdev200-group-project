<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.io.OutputStream" %>
<%@ page import="java.net.HttpURLConnection" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%!
    private static final String BASE_URL = "http://localhost:8080";
    private static final int REQUEST_TIMEOUT_MILLIS = 5000;

    private String sendPost(String endpoint, String json) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
        conn.setReadTimeout(REQUEST_TIMEOUT_MILLIS);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        return readResponse(conn);
    }

    private String sendGet(String endpoint) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
        conn.setReadTimeout(REQUEST_TIMEOUT_MILLIS);

        conn.setRequestMethod("GET");

        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        return response.toString();
    }

    private String extract(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";

        int colon = json.indexOf(":", idx);
        if (colon == -1) return "";

        int startQuote = json.indexOf("\"", colon + 1);
        if (startQuote == -1) return "";

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return "";

        return json.substring(startQuote + 1, endQuote);
    }

    private String cellText(String board, int position) {
        if (board == null || board.length() <= position) {
            return "";
        }

        char mark = board.charAt(position);
        return mark == '-' ? "" : String.valueOf(mark);
    }

    private boolean isOpenCell(String board, int position) {
        return board != null && board.length() > position && board.charAt(position) == '-';
    }

    private String html(String value) {
        if (value == null) return "";

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String jsonEscape(String value) {
        if (value == null) return "";

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
%>

<%
    String error = null;
    String message = null;

    String action = request.getParameter("action");

    String gameId = (String) session.getAttribute("gameId");
    String player = (String) session.getAttribute("player");

    try {
        if ("create".equals(action)) {
            String responseJson = sendPost("/game/create", "{}");

            gameId = extract(responseJson, "gameId");
            player = extract(responseJson, "player");

            session.setAttribute("gameId", gameId);
            session.setAttribute("player", player);

            message = "Game created successfully.";
        } else if ("join".equals(action)) {
            String requestedGameId = request.getParameter("gameId");

            String json = "{\"gameId\":\"" + jsonEscape(requestedGameId) + "\"}";
            String responseJson = sendPost("/game/join", json);

            gameId = extract(responseJson, "gameId");
            player = extract(responseJson, "player");

            session.setAttribute("gameId", gameId);
            session.setAttribute("player", player);

            message = "Game joined successfully.";
        } else if ("move".equals(action)) {
            String position = request.getParameter("position");

            String moveJson = String.format(
                    "{\"gameId\":\"%s\",\"player\":\"%s\",\"position\":%s}",
                    jsonEscape(gameId),
                    jsonEscape(player),
                    position
            );

            String moveResponse = sendPost("/game/move", moveJson);
            message = "Move response: " + moveResponse;
        } else if ("reset".equals(action)) {
            session.removeAttribute("gameId");
            session.removeAttribute("player");

            response.sendRedirect("index.jsp");
            return;
        }
    } catch (Exception e) {
        error = e.getMessage();
    }

    if (gameId == null || gameId.isBlank() || player == null || player.isBlank()) {
        response.sendRedirect("index.jsp");
        return;
    }

    String board = "---------";
        String currentPlayer = "-";
        String status = "";
        String winner = "";
        String stateJson = "";
        String resultJson = "";

        try {
            String encodedGameId = URLEncoder.encode(gameId, StandardCharsets.UTF_8);
            stateJson = sendGet("/game/state?gameId=" + encodedGameId);

            board = extract(stateJson, "board");
            status = extract(stateJson, "status");
            currentPlayer = "IN_PROGRESS".equals(status) ? extract(stateJson, "currentPlayer") : "-";

            if ("WIN".equals(status) || "DRAW".equals(status)) {
                resultJson = sendGet("/game/result?gameId=" + encodedGameId);
                winner = extract(resultJson, "winner");
            }
        } catch (Exception e) {
            error = e.getMessage();
        }

        boolean gameFinished = "WIN".equals(status) || "DRAW".equals(status);
        boolean waitingForPlayer = "WAITING_FOR_PLAYER".equals(status);
        boolean yourTurn = "IN_PROGRESS".equals(status) && player.equals(currentPlayer);
        boolean playerWon = "WIN".equals(status) && player.equals(winner);
        boolean playerLost = "WIN".equals(status) && !winner.isBlank() && !player.equals(winner);

%>

<!DOCTYPE html>
<html>
<head>
    <title>API Tic Tac Toe - Game</title>
    <link rel="stylesheet" href="style.css">

    <% if (!gameFinished) { %>
    <meta http-equiv="refresh" content="3">
    <% } %>
</head>
<body>
<div class="page">
    <h1>API Tic Tac Toe</h1>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert error">
        <strong>Error:</strong> <%= html(error) %>
    </div>
    <% } %>

    <% if (message != null && !message.isBlank()) { %>
    <div class="alert success">
        <%= html(message) %>
    </div>
    <% } %>

    <div class="card">
        <p><strong>Game ID:</strong> <%= html(gameId) %></p>
        <p><strong>You are player:</strong> <%= html(player) %></p>
        <p><strong>Status:</strong> <%= html(status) %></p>

        <% if (currentPlayer != null && !currentPlayer.isBlank()) { %>
        <p><strong>Current turn:</strong> <%= html(currentPlayer) %></p>
        <% } %>

        <% if (waitingForPlayer) { %>
        <p class="notice">Waiting for the other player to join...</p>
        <% } else if ("WIN".equals(status) && playerWon) { %>
        <p class="notice winner">Game finished. You won! Winner: <%= html(winner) %></p>
        <% } else if ("WIN".equals(status) && playerLost) { %>
        <p class="notice loser">Game finished. You lost. Winner: <%= html(winner) %></p>
        <% } else if ("WIN".equals(status)) { %>
        <p class="notice">Game finished. Winner: <%= html(winner.isBlank() ? "Unknown" : winner) %></p>
        <% } else if ("DRAW".equals(status)) { %>
        <p class="notice draw">Game finished. The game is a draw.</p>
        <% } %>
       </div>

    <div class="board">
        <% for (int i = 0; i < 9; i++) { %>
            <% if (yourTurn && isOpenCell(board, i)) { %>
            <form action="game.jsp" method="post" class="cell-form">
                <input type="hidden" name="action" value="move">
                <input type="hidden" name="position" value="<%= i %>">
                <button type="submit" class="cell open">
                    <span class="position"><%= i %></span>
                </button>
            </form>
            <% } else { %>
            <div class="cell">
                <span class="mark"><%= html(cellText(board, i)) %></span>
                <% if (isOpenCell(board, i)) { %>
                <span class="position"><%= i %></span>
                <% } %>
            </div>
            <% } %>
        <% } %>
    </div>

    <div class="card actions">
        <form action="game.jsp" method="get">
            <button type="submit">Refresh</button>
        </form>

        <form action="game.jsp" method="post">
            <input type="hidden" name="action" value="reset">
            <button type="submit" class="secondary">Leave Game</button>
        </form>
    </div>

    <details class="debug">
        <summary>Raw state JSON</summary>
        <pre><%= html(stateJson) %></pre>
    </details>
</div>
</body>
</html>