import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {

    private final String BASE_URL = "http://localhost:8080";
    private static final int REQUEST_TIMEOUT_MILLIS = 5000;

    public String sendPost(String endpoint, String json) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
        conn.setReadTimeout(REQUEST_TIMEOUT_MILLIS);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        return response.toString();
    }

    public String sendGet(String endpoint) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
        conn.setReadTimeout(REQUEST_TIMEOUT_MILLIS);

        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        return response.toString();
    }
}