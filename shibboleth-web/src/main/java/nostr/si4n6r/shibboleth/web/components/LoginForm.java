package nostr.si4n6r.shibboleth.web.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.base.PublicKey;
import nostr.event.impl.DirectMessageEvent;
import nostr.event.json.codec.BaseEventEncoder;
import nostr.si4n6r.ApplicationConfiguration;
import nostr.si4n6r.CustomWebSession;
import nostr.si4n6r.rest.client.SessionManager;
import nostr.si4n6r.util.EncryptionUtil;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.validator.StringValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Log
public class LoginForm extends Form {

    private static final String NIP05_URL = ApplicationConfiguration.getInstance().getNip05Url();
    private static final String TOKEN_ALGO_SECRET = ApplicationConfiguration.getInstance().getTokenAlgoSecret();
    private static final Integer TOKEN_EXPIRATION = ApplicationConfiguration.getInstance().getTokenExpiration();

    private TextField usernameField;
    private TextField appField;
    private PasswordTextField passwordField;
    private Label loginStatus;

    public LoginForm(String id) {
        super(id);

        // Get the app parameter from the URL
        var appParam = getRequest().getQueryParameters().getParameterValue("app").toString();

        usernameField = new TextField("username", Model.of(""));
        appField = new TextField("app", Model.of(appParam)); // Set the appField with the app parameter
        passwordField = new PasswordTextField("password", Model.of(""));
        loginStatus = new Label("loginStatus", Model.of(""));

        add(usernameField);
        add(appField);
        add(passwordField);
        add(loginStatus);

        // Add input validation
        usernameField.setRequired(true);
        passwordField.setRequired(true);
        appField.setRequired(true);
        appField.setVisible(false);

        usernameField.add(StringValidator.minimumLength(1));
        passwordField.add(StringValidator.minimumLength(8));
    }

    public final void onSubmit() {
        String username = (String) usernameField.getDefaultModelObject();
        String password = (String) passwordField.getDefaultModelObject();
        String secret = getTokenPassword();

        var publicKey = getPublicKey(username);

        if (publicKey == null) {
            loginStatus.setDefaultModelObject("Invalid username!");
            return;
        }

        log.log(Level.INFO, "Logging in with {0} and {1}...", new Object[]{username, password});
        try {
            EncryptionUtil.decryptNsec(username, publicKey, password);
            loginStatus.setDefaultModelObject("Logged in!");
            var jwtToken = createSession(publicKey, password, secret);
            log.log(Level.INFO, "Created session with JWT token: {0}", jwtToken);
        } catch (Exception e) {
            loginStatus.setDefaultModelObject("Wrong username/password combination!");
            log.log(Level.SEVERE, "Error logging in", e);
        }
    }

    private String createSession(@NonNull PublicKey user, @NonNull String password, @NonNull String secret) {
        var sessionManager = SessionManager.getInstance();
        log.log(Level.FINE, "Creating session...");

        // Create session
        //var app = new PublicKey((String) appField.getDefaultModelObject());
        var app = getRequest().getQueryParameters().getParameterValue("app").toString();

        log.log(Level.INFO, "Creating a session for user: {0} on app: {1}", new Object[]{user, app});
        sessionManager.createSession(user.toString(), app.toString(), TOKEN_EXPIRATION * 60, password, secret);

        log.log(Level.INFO, "Adding user to web application session...");
        var session = (CustomWebSession) Session.get();
        log.log(Level.INFO, "Wicket Session: {0}", session);
        log.log(Level.INFO, "Shibboleth Session: {0}", sessionManager.getSession(app.toString()));
        var jwtToken = sessionManager.getSession(app.toString()).getToken();
        log.log(Level.INFO, "JWT token: {0}", jwtToken);
        return jwtToken;
    }

    private List<String> getRelays() {
        return List.of("wss://localhost:5555/");
/*
        var client = Client.getInstance();
        return client.getRelays();
*/
    }

    private String getEncodedData(DirectMessageEvent nip04Event) {
        var relays = getRelays();
        if (relays.isEmpty()) {
            log.log(Level.WARNING, "No relays found!");
            throw new RuntimeException("No relays found!");
        }

        var encoder = new BaseEventEncoder(nip04Event);
        var jsonEvent = encoder.encode();

        // Create a map and put the event and the arrays into the map
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("event", jsonEvent);
        dataMap.put("relays", relays);

        try {
            // Convert the map to a JSON string
            var mapper = new ObjectMapper();
            var jsonString = mapper.writeValueAsString(dataMap);

            // Encode the JSON string to Base64
            var encodedData = Base64.getEncoder().encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));

            log.log(Level.INFO, "Encoded data: {0}", encodedData);
            return encodedData;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error encoding data", e);
            throw new RuntimeException(e);
        }
    }

    private PublicKey getPublicKey(@NonNull String username) {
        try {
            String[] parts = username.split("@");
            if (parts.length != 2) {
                return null;
            }

            String localpart = URLEncoder.encode(parts[0], StandardCharsets.UTF_8);
            String domain = URLEncoder.encode(parts[1], StandardCharsets.UTF_8);

            String urlString = NIP05_URL + "?localpart=" + localpart + "&domain=" + domain;
            String response = sendGetRequest(urlString);

            return parsePublicKeyFromResponse(response, localpart);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error getting public key for username: " + username, e);
            return null;
        }
    }

    private String sendGetRequest(String urlString) throws Exception {
        URL url = new URI(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }

    private PublicKey parsePublicKeyFromResponse(String response, String localpart) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonResponse = mapper.readValue(response, Map.class);
        Map<String, String> namesObject = (Map<String, String>) jsonResponse.get("names");
        String publicKeyString = namesObject.get(localpart);

        return new PublicKey(publicKeyString);
    }

    private String getTokenPassword() {
        var filePath = System.getProperty("user.home") + "/.si4n6r/" + TOKEN_ALGO_SECRET;
        try {
            return readFileContent(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFileContent(@NonNull String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}