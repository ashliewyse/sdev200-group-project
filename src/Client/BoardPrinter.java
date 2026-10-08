public class BoardPrinter {

    public static void print(String board) {
        System.out.println();
        for (int i = 0; i < 9; i++) {
            char c = board.charAt(i) == '-' ? ' ' : board.charAt(i);
            System.out.print(" " + c + " ");
            if (i % 3 != 2) System.out.print("|");
            else if (i != 8) System.out.println("\n-----------");
        }
        System.out.println();
    }
}