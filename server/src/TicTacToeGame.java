import java.util.Arrays;

/** Owns one game. Every operation is atomic for clients sharing the game ID. */
public final class TicTacToeGame {
    private static final int[][] WINNING_LINES = {
        {0,1,2}, {3,4,5}, {6,7,8}, {0,3,6}, {1,4,7}, {2,5,8}, {0,4,8}, {2,4,6}
    };
    private final String gameId;
    private final char[] board = new char[9];
    private String status = "WAITING_FOR_PLAYER";
    private String currentPlayer;
    private String winner;

    public record Snapshot(String gameId, String board, String currentPlayer, String status, String winner) {}
    public record MoveResult(boolean valid, String reason, Snapshot state) {}

    public TicTacToeGame(String gameId) {
        if (gameId == null || gameId.isBlank()) throw new IllegalArgumentException("Game ID is required");
        this.gameId = gameId;
        Arrays.fill(board, '-');
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(gameId, new String(board), currentPlayer, status, winner);
    }

    /** X is reserved at creation; the one successful join reserves O. */
    public synchronized Snapshot join() {
        if (!status.equals("WAITING_FOR_PLAYER")) throw new IllegalStateException("Game already has two players");
        status = "IN_PROGRESS";
        currentPlayer = "X";
        return snapshot();
    }

    public synchronized MoveResult move(String player, int position) {
        if (!"X".equals(player) && !"O".equals(player)) throw new IllegalArgumentException("Player must be X or O");
        if (position < 0 || position >= board.length) throw new IllegalArgumentException("Position must be an integer from 0 to 8");
        if (status.equals("WAITING_FOR_PLAYER")) return rejected("Waiting for another player");
        if (!status.equals("IN_PROGRESS")) return rejected("Game is already finished");
        if (!player.equals(currentPlayer)) return rejected("Not your turn");
        if (board[position] != '-') return rejected("Position already taken");

        board[position] = player.charAt(0);
        if (hasWinningLine(board[position])) {
            status = "WIN";
            winner = player;
            currentPlayer = null;
        } else if (new String(board).indexOf('-') < 0) {
            status = "DRAW";
            currentPlayer = null;
        } else {
            currentPlayer = player.equals("X") ? "O" : "X";
        }
        return new MoveResult(true, null, snapshot());
    }

    private MoveResult rejected(String reason) { return new MoveResult(false, reason, snapshot()); }

    private boolean hasWinningLine(char mark) {
        for (int[] line : WINNING_LINES) {
            if (board[line[0]] == mark && board[line[1]] == mark && board[line[2]] == mark) return true;
        }
        return false;
    }
}
