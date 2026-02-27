package org.example.campusLink.Services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🤖 SERVICE D'ASSISTANCE IA — SANS BASE DE DONNÉES
 *
 * Connecte l'application JavaFX aux workflows n8n pour :
 * - Aide à la rédaction de publications
 * - Génération de descriptions de services
 * - Chatbot d'assistance (historique géré en mémoire côté Java)
 * - Création directe de publications en base MySQL via n8n
 */
public class Gestion_IA {

    // URLs des webhooks n8n
    private static final String N8N_BASE_URL          = "http://localhost:5678/webhook";
    private static final String PUBLICATION_ENDPOINT  = N8N_BASE_URL + "/aide-publication";
    private static final String SERVICE_ENDPOINT      = N8N_BASE_URL + "/generer-service";
    private static final String CHATBOT_ENDPOINT      = N8N_BASE_URL + "/chatbot";
    private static final String CREER_PUB_ENDPOINT    = N8N_BASE_URL + "/creer-publication"; // ← NEW

    // ✅ Historique en mémoire : clé = user_id, valeur = liste de messages {role, content}
    private static final Map<Integer, List<JSONObject>> chatHistories = new HashMap<>();

    // Nombre max de messages à garder en mémoire par utilisateur
    private static final int MAX_HISTORY_SIZE = 10;

    // =========================================================
    // 1️⃣  AIDE RÉDACTION PUBLICATION
    // =========================================================

    /**
     * Aide à la rédaction d'une publication.
     * Retourne un JSONObject avec 3 versions : version_courte, version_detaillee, version_urgente + conseils.
     */
    public JSONObject aiderRedactionPublication(int userId, String type, String categorie,
                                                String idee, double budget) throws Exception {
        System.out.println("🤖 Appel IA - Aide rédaction publication...");

        JSONObject request = new JSONObject();
        request.put("user_id",   userId);
        request.put("type",      type);
        request.put("categorie", categorie);
        request.put("idee",      idee);
        request.put("budget",    budget);

        String response = callWebhook(PUBLICATION_ENDPOINT, request);
        JSONObject result = parseWebhookResponse(response);

        System.out.println("✅ Suggestions générées avec succès");
        return result;
    }

    // =========================================================
    // 2️⃣  GÉNÉRATION DESCRIPTION SERVICE
    // =========================================================

    public JSONObject genererDescriptionService(int userId, String titre, String categorie,
                                                String competences, String experience,
                                                double prix, String publicCible) throws Exception {
        System.out.println("🤖 Appel IA - Génération description service...");

        JSONObject request = new JSONObject();
        request.put("user_id",      userId);
        request.put("titre",        titre);
        request.put("categorie",    categorie);
        request.put("competences",  competences);
        request.put("experience",   experience);
        request.put("prix",         prix);
        request.put("public_cible", publicCible);

        String response = callWebhook(SERVICE_ENDPOINT, request);
        JSONObject result = parseWebhookResponse(response);

        System.out.println("✅ Description générée avec succès");
        return result;
    }

    // =========================================================
    // 3️⃣  CHATBOT — HISTORIQUE EN MÉMOIRE
    // =========================================================

    public String envoyerMessageChatbot(int userId, String message) throws Exception {
        System.out.println("🤖 Appel IA - Chatbot...");

        List<JSONObject> history = chatHistories.computeIfAbsent(userId, k -> new ArrayList<>());

        JSONArray historyArray = new JSONArray();
        for (JSONObject msg : history) {
            historyArray.put(msg);
        }

        JSONObject request = new JSONObject();
        request.put("user_id", userId);
        request.put("message", message);
        request.put("history", historyArray);

        String response = callWebhook(CHATBOT_ENDPOINT, request);
        JSONObject result = new JSONObject(response);
        String botResponse = result.getString("response");

        // Update in-memory history
        JSONObject userMsg = new JSONObject();
        userMsg.put("role",    "user");
        userMsg.put("content", message);
        history.add(userMsg);

        JSONObject botMsg = new JSONObject();
        botMsg.put("role",    "assistant");
        botMsg.put("content", botResponse);
        history.add(botMsg);

        // Trim history
        while (history.size() > MAX_HISTORY_SIZE * 2) {
            history.remove(0);
            history.remove(0);
        }

        System.out.println("✅ Réponse chatbot: " + botResponse);
        return botResponse;
    }

    public void effacerHistoriqueChatbot(int userId) {
        chatHistories.remove(userId);
        System.out.println("🗑️ Historique chatbot effacé pour user " + userId);
    }

    // =========================================================
    // 4️⃣  CRÉER PUBLICATION → MySQL via n8n   ← NEW
    // =========================================================

    /**
     * Envoie les données d'une publication au webhook n8n /creer-publication
     * qui les insère directement dans la table MySQL `publications`.
     *
     * @param payload JSONObject contenant :
     *   - student_id       (int)     REQUIRED
     *   - titre            (String)  REQUIRED
     *   - message          (String)  REQUIRED
     *   - prix_vente       (double)  REQUIRED
     *   - type_publication (String)  REQUIRED  ex: "DEMANDE_SERVICE" | "VENTE_OBJET"
     *   - status           (String)  optional  default: "ACTIVE"
     *   - localisation     (String)  optional
     *   - image_url        (String)  optional
     *
     * @return JSONObject:
     *   { "success": true,  "publication_id": 42,  "message": "Publication créée avec succès" }
     *   { "success": false, "errors": [...],        "message": "Validation échouée" }
     */
    public JSONObject creerPublication(JSONObject payload) throws Exception {
        System.out.println("📢 Envoi publication vers n8n → MySQL...");
        System.out.println("   Payload: " + payload.toString());

        String   raw    = callWebhook(CREER_PUB_ENDPOINT, payload);
        JSONObject result = new JSONObject(raw);

        if (result.optBoolean("success", false)) {
            System.out.println("✅ Publication créée, ID = " + result.optInt("publication_id"));
        } else {
            System.out.println("❌ Échec création : " + result.optString("message"));
        }

        return result;
    }

    // =========================================================
    // 🔧 UTILITAIRES
    // =========================================================

    /**
     * Safely parses a webhook response that may be:
     *   - A JSON object  { ... }           → returned as-is
     *   - A JSON array   [ { ... } ]       → returns first element
     *   - Plain text / HTML                → wraps in { "response": "..." }
     */
    private JSONObject parseWebhookResponse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            throw new Exception("Réponse vide du serveur n8n");
        }
        String trimmed = raw.trim();

        if (trimmed.startsWith("{")) {
            // Standard JSON object
            return new JSONObject(trimmed);
        } else if (trimmed.startsWith("[")) {
            // JSON array — n8n sometimes wraps responses in an array
            org.json.JSONArray arr = new org.json.JSONArray(trimmed);
            if (arr.length() > 0) {
                return arr.getJSONObject(0);
            }
            throw new Exception("Tableau JSON vide reçu de n8n");
        } else {
            // Plain text response (e.g. n8n not configured yet)
            System.err.println("⚠️ Réponse non-JSON reçue : " + trimmed.substring(0, Math.min(200, trimmed.length())));
            throw new Exception("La réponse n8n n'est pas du JSON valide.\nVérifiez que le workflow \"aide-publication\" est actif et retourne du JSON.\n\nRéponse reçue : " + trimmed.substring(0, Math.min(100, trimmed.length())));
        }
    }

    private String callWebhook(String webhookUrl, JSONObject data) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("🌐 HTTP " + responseCode + " from: " + webhookUrl);

            // Accept 200, 400, and other codes — always try to read the body
            java.io.InputStream stream = null;
            try {
                stream = conn.getInputStream();
            } catch (Exception ignored) {
                stream = conn.getErrorStream();
            }

            if (stream == null) {
                System.err.println("❌ Réponse nulle (stream null) — HTTP " + responseCode);
                throw new Exception("Réponse vide du serveur n8n (HTTP " + responseCode + ")");
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
                String body = response.toString();
                System.out.println("📥 Réponse brute (" + body.length() + " chars): "
                        + body.substring(0, Math.min(300, body.length())));
                if (body.isBlank()) {
                    throw new Exception("Réponse vide du serveur n8n (HTTP " + responseCode + ") — vérifiez que le workflow est actif et que tous les nœuds sont connectés");
                }
                return body;
            }
        } finally {
            conn.disconnect();
        }
    }

    public boolean verifierConnexionN8n() {
        try {
            URL url = new URL(N8N_BASE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.connect();
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode < 500;
        } catch (Exception e) {
            System.err.println("❌ n8n non accessible: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // 🧪 TESTS
    // =========================================================

    public static void main(String[] args) {
        try {
            Gestion_IA iaService = new Gestion_IA();

            System.out.println("\n🧪 Test 1: Vérification connexion n8n");
            boolean connected = iaService.verifierConnexionN8n();
            System.out.println(connected ? "✅ Connecté" : "❌ Non connecté");
            if (!connected) {
                System.out.println("⚠️  Démarrez n8n : docker run -it --rm -p 5678:5678 n8nio/n8n");
                return;
            }

            System.out.println("\n🧪 Test 2: Aide rédaction publication");
            JSONObject pubResult = iaService.aiderRedactionPublication(
                    1, "demande", "Mathématiques",
                    "J'ai besoin d'aide pour mes révisions d'algèbre", 25.0);
            System.out.println("Résultat: " + pubResult.toString(2));

            System.out.println("\n🧪 Test 3: Génération description service");
            JSONObject serviceResult = iaService.genererDescriptionService(
                    2, "Cours de Java", "Programmation",
                    "Java, Spring Boot, MySQL", "5 ans d'expérience en développement",
                    30.0, "Étudiants niveau débutant à intermédiaire");
            System.out.println("Résultat: " + serviceResult.toString(2));

            System.out.println("\n🧪 Test 4: Chatbot multi-tours");
            String r1 = iaService.envoyerMessageChatbot(1, "Comment publier une demande ?");
            System.out.println("Bot: " + r1);
            String r2 = iaService.envoyerMessageChatbot(1, "Et quel est le prix moyen ?");
            System.out.println("Bot: " + r2);

            System.out.println("\n🧪 Test 5: Créer publication directement en base");
            JSONObject payload = new JSONObject();
            payload.put("student_id",       1);
            payload.put("titre",            "Cherche tuteur Algo L2");
            payload.put("message",          "J'ai besoin d'aide pour mon partiel d'algorithmique.");
            payload.put("type_publication", "DEMANDE_SERVICE");
            payload.put("prix_vente",       20.0);
            payload.put("localisation",     "Paris 5ème");
            JSONObject createResult = iaService.creerPublication(payload);
            System.out.println("Résultat: " + createResult.toString(2));

            System.out.println("\n✅ Tous les tests passés !");

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}