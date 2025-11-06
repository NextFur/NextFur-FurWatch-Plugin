package net.nextfur.fws.api;

import com.google.gson.Gson;
import net.nextfur.fws.api.utils.JsonUtils;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class APIClient {
    private static final Gson gson = new Gson();

    public static APIResponse post(String urlString, HashMap<String, Object> data, String apiKey) {
        return sendRequest("POST", urlString, gson.toJson(data), apiKey);
    }

    public static APIResponse get(String urlString, String apiKey) {
        return sendRequest("GET", urlString, null, apiKey);
    }

    private static APIResponse sendRequest(String method, String urlString, String body, String apiKey) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            if (body != null && !body.isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = JsonUtils.readStream(stream);

            return new APIResponse(code, response);
        } catch (Exception e) {
            return new APIResponse(-1, e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
