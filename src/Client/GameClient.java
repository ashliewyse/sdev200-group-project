import java.util.Scanner;

public class GameClient {

    private ApiClient api = new ApiClient();
    private String gameId;
    private String player;

    public void start() throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to API Tic Tac Toe!");
        System.out.println("1. Create game");
        System.out.println("2. Join game");
        System.out.print("Choose: ");
        int choice = scanner.nextInt();

        if (choice == 1) {
            createGame();
        } else {
            joinGame(scanner);
        }

        System.out.println("Game ID: " + gameId);
        System.out.println("You are player: " + player);

        gameLoop(scanner);
    }

    private void createGame() throws Exception {
        String response = api.sendPost("/game/create", "{}");
        gameId = extract(response, "gameId");
        player = extract(response, "player");
    }

    private void joinGame(Scanner scanner) throws Exception {
        System.out.print("Enter game ID: ");
        String id = scanner.next();

        String json = "{\"gameId\":\"" + id + "\"}";
        String response = api.sendPost("/game/join", json);

        gameId = extract(response, "gameId");
        player = extract(response, "player");
    }

    private void gameLoop(Scanner scanner) throws Exception {
        while (true) {
            String stateJson = api.sendGet("/game/state?gameId=" + gameId);

            String board = extract(stateJson, "board");
            String currentPlayer = extract(stateJson, "currentPlayer");
            String status = extract(stateJson, "status");

            BoardPrinter.print(board);

            if (status.equals("WIN") || status.equals("DRAW")) {
                System.out.println("Game finished: " + status);
                break;
            }

            if (status.equals("WAITING_FOR_PLAYER")) {
                System.out.println("Waiting for the other player to join...");
            } else if (status.equals("IN_PROGRESS") && currentPlayer.equals(player)) {
                System.out.print("Your move (0-8): ");
                int pos = scanner.nextInt();

                String moveJson = String.format(
                        "{\"gameId\":\"%s\",\"player\":\"%s\",\"position\":%d}",
                        gameId, player, pos
                );

                String moveResponse = api.sendPost("/game/move", moveJson);
                System.out.println("Move response: " + moveResponse);
            }

            Thread.sleep(status.equals("WAITING_FOR_PLAYER") ? 5000 : 2000);
        }

        String resultJson = api.sendGet("/game/result?gameId=" + gameId);
        System.out.println("Final result: " + resultJson);
    }

    private String extract(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return ""; // key not found

        int colon = json.indexOf(":", idx);
        if (colon == -1) return "";

        int startQuote = json.indexOf("\"", colon + 1);
        if (startQuote == -1) return "";

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return "";

        return json.substring(startQuote + 1, endQuote);
    }
}