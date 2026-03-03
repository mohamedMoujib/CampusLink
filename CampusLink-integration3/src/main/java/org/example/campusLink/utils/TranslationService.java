package org.example.campusLink.utils;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationService {

    // ✅ MyMemory — free, no API key, no registration
    private static final String API_URL =
            "https://api.mymemory.translated.net/get";

    private static final ConcurrentHashMap<String, String> cache =
            new ConcurrentHashMap<>();

    public static String frToEn(String text) {
        if (text == null || text.isBlank()) return text;
        if (cache.containsKey(text)) return cache.get(text);

        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = API_URL + "?q=" + encoded + "&langpair=fr|en";

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("User-Agent", "CampusLink/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("⚠️ [Translation] HTTP " + status);
                return text;
            }

            String response = new String(
                    conn.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);

            JSONObject json = new JSONObject(response);
            int responseStatus = json.getInt("responseStatus");

            if (responseStatus != 200) {
                System.err.println("⚠️ [Translation] API status "
                        + responseStatus);
                return text;
            }

            String translated = json
                    .getJSONObject("responseData")
                    .getString("translatedText");

            cache.put(text, translated);
            System.out.println("🌐 [Translation] \""
                    + text + "\" → \"" + translated + "\"");
            return translated;

        } catch (Exception e) {
            System.err.println("❌ [Translation] " + e.getMessage());
            return text; // fallback — never crash
        }
    }

    public static String enToFr(String text) {
        if (text == null || text.isBlank()) return text;
        if (cache.containsKey("en_" + text))
            return cache.get("en_" + text);

        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = API_URL + "?q=" + encoded + "&langpair=en|fr";

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("User-Agent", "CampusLink/1.0");

            if (conn.getResponseCode() != 200) return text;

            String response = new String(
                    conn.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);

            String translated = new JSONObject(response)
                    .getJSONObject("responseData")
                    .getString("translatedText");

            cache.put("en_" + text, translated);
            return translated;

        } catch (Exception e) {
            System.err.println("❌ [Translation] " + e.getMessage());
            return text;
        }
    }

    public static void clearCache() {
        cache.clear();
    }
}