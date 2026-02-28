package org.example.campusLink.Services;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private final Map<String, String> cache;

    public TranslationService() {
        this.cache = new HashMap<>();
    }

    public String translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) return text;

        String cacheKey = text + "_" + targetLang;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        try {
            String sourceLang = detectLanguage(text);
            if (sourceLang.equals(targetLang)) return text;

            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlString = API_URL + "?q=" + encodedText + "&langpair=" + sourceLang + "%7C" + targetLang;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                // ✅ Lire en UTF-8 pour préserver les caractères arabes
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String translated = parseTranslation(sb.toString());
                if (translated != null && !translated.isEmpty()) {
                    cache.put(cacheKey, translated);
                    return translated;
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur traduction: " + e.getMessage());
        }

        return text;
    }

    private String detectLanguage(String text) {
        if (text.matches(".*[\\u0600-\\u06FF]+.*")) return "ar";
        return "fr";
    }

    private String parseTranslation(String json) {
        try {
            int start = json.indexOf("\"translatedText\":\"");
            if (start == -1) return "";
            start += 18;
            int end = json.indexOf("\"", start);
            if (end == -1) return "";

            String raw = json.substring(start, end);

            // ✅ FIX ARABE : décoder les séquences unicode \XXXX manuellement
            return decodeUnicode(raw);

        } catch (Exception e) {
            return "";
        }
    }

    // ✅ Décode \XXXX → vrais caractères unicode (arabe, etc.)
    private String decodeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (i + 5 < input.length()
                    && input.charAt(i) == '\\'
                    && input.charAt(i + 1) == 'u') {
                String hex = input.substring(i + 2, i + 6);
                try {
                    int codePoint = Integer.parseInt(hex, 16);
                    sb.append((char) codePoint);
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(input.charAt(i));
                    i++;
                }
            } else if (input.charAt(i) == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i += 2; }
                    case 'r' -> { sb.append('\r'); i += 2; }
                    case 't' -> { sb.append('\t'); i += 2; }
                    case '"' -> { sb.append('"'); i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    case '/' -> { sb.append('/'); i += 2; }
                    default  -> { sb.append(input.charAt(i)); i++; }
                }
            } else {
                sb.append(input.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    public void clearCache() { cache.clear(); }

    public static String getLanguageName(String code) {
        return switch (code) {
            case "fr" -> "Français";
            case "ar" -> "العربية";
            case "en" -> "English";
            default -> code;
        };
    }

    public static String getLanguageFlag(String code) {
        return switch (code) {
            case "fr" -> "🇫🇷";
            case "ar" -> "🇹🇳";
            case "en" -> "🇬🇧";
            default -> "🌐";
        };
    }
}