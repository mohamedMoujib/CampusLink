package org.example.campusLink.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;
import org.example.campusLink.entities.GoogleUser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

/**
 * Service d'authentification Google OAuth 2.0 pour application desktop
 */
public class GoogleAuthServices {

    // Configuration
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList("openid email profile");

    // Credentials hardcodés (depuis le JSON fourni)
    @Value("${google.client.id}")
    private String CLIENT_ID;

    @Value("${google.client.secret}")
    private String CLIENT_SECRET;
    /**
     * Authentifier un utilisateur via Google OAuth
     */
    public GoogleUser authenticate() throws Exception {
        System.out.println("🔐 Démarrage de l'authentification Google OAuth...");

        try {
            // 1. Créer le flow OAuth
            GoogleClientSecrets clientSecrets = createClientSecrets();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    clientSecrets,
                    SCOPES
            )
                    .setAccessType("online")
                    .build();

            System.out.println("✅ Flow OAuth créé");

            // 2. Lancer le serveur local pour le callback (port aléatoire)
            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(-1)
                    .build();

            System.out.println("🌐 Serveur de callback démarré sur port: " + receiver.getPort());

            // 3. Obtenir l'autorisation (ouvre le navigateur)
            System.out.println("🌍 Ouverture du navigateur pour l'authentification...");

            AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(flow, receiver);
            Credential credential = app.authorize("user");

            System.out.println("✅ Autorisation obtenue");

            // 4. Vérifier que nous avons un access token
            if (credential == null || credential.getAccessToken() == null) {
                throw new IllegalStateException("Credential ou Access Token est null");
            }

            System.out.println("✅ Access Token reçu: " + credential.getAccessToken().substring(0, 20) + "...");

            // 5. Récupérer les informations utilisateur depuis le token
            GoogleUser googleUser = getUserInfo(credential);

            System.out.println("✅ Informations utilisateur récupérées: " + googleUser.getEmail());

            return googleUser;

        } catch (IOException e) {
            System.err.println("❌ Erreur IO: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Erreur lors de l'authentification Google: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Échec de l'authentification Google: " + e.getMessage(), e);
        }
    }

    /**
     * Créer les secrets client depuis les credentials hardcodés
     */
    private GoogleClientSecrets createClientSecrets() {
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(CLIENT_ID);
        details.setClientSecret(CLIENT_SECRET);
        details.setAuthUri("https://accounts.google.com/o/oauth2/auth");
        details.setTokenUri("https://oauth2.googleapis.com/token");
        details.setRedirectUris(Collections.singletonList("http://localhost"));

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);

        return clientSecrets;
    }

    /**
     * Récupérer les informations utilisateur depuis le credential
     */
    private GoogleUser getUserInfo(Credential credential) throws IOException {
        try {
            // Utiliser l'API Google pour récupérer les infos utilisateur
            String url = "https://www.googleapis.com/oauth2/v3/userinfo";

            com.google.api.client.http.HttpRequest request = new NetHttpTransport()
                    .createRequestFactory(credential)
                    .buildGetRequest(new com.google.api.client.http.GenericUrl(url));

            String response = request.execute().parseAsString();

            System.out.println("📧 Réponse API: " + response);

            // Parser la réponse JSON
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response).getAsJsonObject();

            GoogleUser user = new GoogleUser();
            user.setGoogleId(json.get("sub").getAsString());
            user.setEmail(json.get("email").getAsString());
            user.setName(json.get("name").getAsString());
            user.setEmailVerified(json.get("email_verified").getAsBoolean());

            if (json.has("picture")) {
                user.setPictureUrl(json.get("picture").getAsString());
            }

            return user;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des infos utilisateur: " + e.getMessage());
            throw new IOException("Impossible de récupérer les informations utilisateur", e);
        }
    }

    /**
     * Méthode de test
     */
    public static void main(String[] args) {
        System.out.println("🧪 TEST GOOGLE AUTH SERVICE\n");

        try {
            GoogleAuthServices service = new GoogleAuthServices();
            GoogleUser user = service.authenticate();

            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("✅ AUTHENTIFICATION RÉUSSIE!");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("Google ID: " + user.getGoogleId());
            System.out.println("Nom:       " + user.getName());
            System.out.println("Email:     " + user.getEmail());
            System.out.println("Vérifié:   " + user.isEmailVerified());
            System.out.println("Photo:     " + user.getPictureUrl());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            System.err.println("\n❌ ÉCHEC DU TEST");
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}