import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** A new game never replaces an existing game's board. Games live until shutdown. */
public final class GameRegistry {
    private final ConcurrentMap<String, TicTacToeGame> games = new ConcurrentHashMap<>();

    public TicTacToeGame create() {
        for (;;) {
            String id = UUID.randomUUID().toString();
            var game = new TicTacToeGame(id);
            if (games.putIfAbsent(id, game) == null) return game;
        }
    }

    public TicTacToeGame find(String gameId) { return games.get(gameId); }
}
