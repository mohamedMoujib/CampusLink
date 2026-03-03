package org.example.campusLink.services.users;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;
import org.example.campusLink.entities.GoogleUser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GoogleAuthServices {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList("openid email profile");

    // ✅ Hardcoded — NOT Spring, @Value does nothing here
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String CLIENT_ID;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String CLIENT_SECRET;
    public GoogleUser authenticate() throws Exception {
        System.out.println("🔐 Démarrage de l'authentification Google OAuth...");

        try {
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

            // ✅ Port -1 = random available port (avoids "Address already in use")
            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(-1)
                    .build();

            System.out.println("🌐 Serveur de callback démarré sur port: " + receiver.getPort());

            AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(flow, receiver);
            Credential credential = app.authorize("user");

            System.out.println("✅ Autorisation obtenue");

            if (credential == null || credential.getAccessToken() == null) {
                throw new IllegalStateException("Credential ou Access Token est null");
            }

            System.out.println("✅ Access Token reçu: " + credential.getAccessToken().substring(0, 20) + "...");

            GoogleUser googleUser = getUserInfo(credential);
            System.out.println("✅ Informations utilisateur récupérées: " + googleUser.getEmail());

            return googleUser;

        } catch (IOException e) {
            System.err.println("❌ Erreur IO: " + e.getMessage());
            throw new Exception("Erreur lors de l'authentification Google: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            throw new Exception("Échec de l'authentification Google: " + e.getMessage(), e);
        }
    }

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

    private GoogleUser getUserInfo(Credential credential) throws IOException {
        try {
            String url = "https://www.googleapis.com/oauth2/v3/userinfo";

            com.google.api.client.http.HttpRequest request = new NetHttpTransport()
                    .createRequestFactory(credential)
                    .buildGetRequest(new com.google.api.client.http.GenericUrl(url));

            String response = request.execute().parseAsString();
            System.out.println("📧 Réponse API: " + response);

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
}
