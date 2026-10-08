import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Independent game fixtures: wrong rules, mutations, or races break these checks. */
public final class GameRulesTest {
    private static int passed;

    public static void main(String[] args) throws Exception {
        check("create waits for second player", () -> {
            var game = new TicTacToeGame("example");
            equal("---------", game.snapshot().board());
            equal("WAITING_FOR_PLAYER", game.snapshot().status());
            equal(null, game.snapshot().currentPlayer());
            var before = game.snapshot();
            require(!game.move("X", 4).valid());
            equal(before, game.snapshot());
        });
        check("join starts X and cannot reset play", () -> {
            var game = ready();
            equal("X", game.snapshot().currentPlayer());
            equal("IN_PROGRESS", game.snapshot().status());
            require(game.move("X", 4).valid());
            var before = game.snapshot();
            rejects(IllegalStateException.class, game::join);
            equal(before, game.snapshot());
        });
        check("center move changes turn", () -> {
            var game = ready();
            require(game.move("X", 4).valid());
            equal("----X----", game.snapshot().board());
            equal("O", game.snapshot().currentPlayer());
        });
        check("wrong turn preserves state", () -> {
            var game = ready();
            var before = game.snapshot();
            require(!game.move("O", 0).valid());
            equal(before, game.snapshot());
        });
        check("occupied move preserves turn and board", () -> {
            var game = ready();
            game.move("X", 4);
            var before = game.snapshot();
            equal("Position already taken", game.move("O", 4).reason());
            equal(before, game.snapshot());
        });
        check("invalid player and positions cannot mutate game", () -> {
            var game = ready();
            var before = game.snapshot();
            rejects(IllegalArgumentException.class, () -> game.move("x", 0));
            rejects(IllegalArgumentException.class, () -> game.move(null, 0));
            rejects(IllegalArgumentException.class, () -> game.move("X", -1));
            rejects(IllegalArgumentException.class, () -> game.move("X", 9));
            equal(before, game.snapshot());
        });
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int i = 0; i < lines.length; i++) {
            final int[] line = lines[i];
            check("winning line " + (i + 1), () -> {
                var game = ready();
                var other = new ArrayList<Integer>();
                for (int cell = 0; cell < 9; cell++) {
                    if (cell != line[0] && cell != line[1] && cell != line[2]) other.add(cell);
                }
                play(game, new int[]{line[0], other.get(0), line[1], other.get(1), line[2]});
                equal("WIN", game.snapshot().status());
                equal("X", game.snapshot().winner());
                equal(null, game.snapshot().currentPlayer());
                var before = game.snapshot();
                require(!game.move("O", other.get(2)).valid());
                equal(before, game.snapshot());
            });
        }
        check("O can win", () -> {
            var game = ready();
            play(game, new int[]{3,0,4,1,8,2});
            equal("WIN", game.snapshot().status());
            equal("O", game.snapshot().winner());
        });
        check("full board is draw and rejects further moves", () -> {
            var game = ready();
            play(game, new int[]{0,1,2,4,3,5,7,6,8});
            equal("XOXXOOOXX", game.snapshot().board());
            equal("DRAW", game.snapshot().status());
            equal(null, game.snapshot().winner());
            equal(null, game.snapshot().currentPlayer());
            var before = game.snapshot();
            require(!game.move("O", 0).valid());
            equal(before, game.snapshot());
        });
        check("registry creates distinct independent games", () -> {
            var registry = new GameRegistry();
            var first = registry.create();
            var second = registry.create();
            require(!first.snapshot().gameId().equals(second.snapshot().gameId()));
            equal(first, registry.find(first.snapshot().gameId()));
            equal(null, registry.find("missing"));
            first.join();
            first.move("X", 0);
            equal("---------", second.snapshot().board());
            equal("WAITING_FOR_PLAYER", second.snapshot().status());
        });
        check("concurrent joins admit one O", () -> {
            var game = new TicTacToeGame("race");
            var results = race(() -> {
                try { game.join(); return true; }
                catch (IllegalStateException expected) { return false; }
            });
            equal(1L, results.stream().filter(Boolean::booleanValue).count());
            equal("X", game.snapshot().currentPlayer());
        });
        check("concurrent moves consume only one turn", () -> {
            var game = ready();
            var results = race(() -> game.move("X", 4).valid());
            equal(1L, results.stream().filter(Boolean::booleanValue).count());
            equal("----X----", game.snapshot().board());
            equal("O", game.snapshot().currentPlayer());
        });
        System.out.println("GameRulesTest: " + passed + " passed, 0 failed");
    }

    private static TicTacToeGame ready() {
        var game = new TicTacToeGame("fixture");
        game.join();
        return game;
    }
    private static void play(TicTacToeGame game, int[] moves) {
        for (int i = 0; i < moves.length; i++) require(game.move(i % 2 == 0 ? "X" : "O", moves[i]).valid());
    }
    private static List<Boolean> race(Callable<Boolean> action) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var barrier = new CyclicBarrier(2);
        try {
            Callable<Boolean> task = () -> { barrier.await(5, TimeUnit.SECONDS); return action.call(); };
            var a = pool.submit(task);
            var b = pool.submit(task);
            return List.of(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
    private static void check(String name, Checked action) throws Exception {
        action.run();
        passed++;
        System.out.println("PASS " + name);
    }
    private static void require(boolean condition) { if (!condition) throw new AssertionError("Condition failed"); }
    private static void equal(Object want, Object got) {
        if (!java.util.Objects.equals(want, got)) throw new AssertionError("Expected " + want + ", got " + got);
    }
    private static void rejects(Class<? extends Throwable> type, Checked action) throws Exception {
        try { action.run(); }
        catch (Throwable ex) { if (type.isInstance(ex)) return; throw ex; }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
}
