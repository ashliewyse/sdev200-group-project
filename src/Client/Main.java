public class Main {
    public static void main(String[] args) {
        try {
            GameClient client = new GameClient();
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}