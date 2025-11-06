package net.nextfur.fws.api;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class NextFurAPI {
    private String apiUrl;
    private String apiKey;

    public NextFurAPI(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public APIResponse post(String endpoint, HashMap<String, Object> data) {
        data.put("timestamp", System.currentTimeMillis());
        return APIClient.post(apiUrl + endpoint, data, apiKey);
    }

    public APIResponse get(String endpoint) {
        return APIClient.get(apiUrl + endpoint, apiKey);
    }

    public APIResponse get(String endpoint, HashMap<String, Object> data) {
        data.put("timestamp", System.currentTimeMillis());
        StringBuilder urlBuilder = new StringBuilder(apiUrl + endpoint + "?");
        for (String key : data.keySet()) urlBuilder.append(key).append("=").append(data.get(key).toString()).append("&");
        urlBuilder.setLength(urlBuilder.length() - 1);

        return APIClient.get(urlBuilder.toString(), apiKey);
    }

    public CompletableFuture<APIResponse> postAsync(String endpoint, HashMap<String, Object> data) {
        return CompletableFuture.supplyAsync(() -> post(endpoint, data));
    }
}
