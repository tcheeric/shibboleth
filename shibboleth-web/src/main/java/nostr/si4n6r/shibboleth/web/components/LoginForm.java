package nostr.si4n6r.shibboleth.web.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.api.NIP04;
import nostr.api.Nostr;
import nostr.base.PublicKey;
import nostr.id.CustomIdentity;
import nostr.si4n6r.ApplicationConfiguration;
import nostr.si4n6r.CustomWebSession;
import nostr.si4n6r.core.impl.SessionManager;
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

        usernameField = new TextField("username", Model.of(""));
        appField = new TextField("app", Model.of(""));
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

        log.log(Level.FINE, "Logging in with {0} and {1}...", new Object[]{username, password});
        try {
            EncryptionUtil.decryptNsec(publicKey, secret);
            loginStatus.setDefaultModelObject("Logged in!");
            createSession(publicKey, password);
        } catch (Exception e) {
            loginStatus.setDefaultModelObject("Wrong username/password combination!");
        }
    }

    private void createSession(@NonNull PublicKey npub, @NonNull String password) {
        var sessionManager = SessionManager.getInstance();
        log.log(Level.FINE, "Creating session...");

        // Create session
        log.log(Level.FINE, "Creating a session for npub: {0}", npub);
        sessionManager.createSession(npub, new PublicKey(appField.getDefaultModelObject().toString()), TOKEN_EXPIRATION * 60, password);

        log.log(Level.FINER, "Adding npub to web application session...");
        var session = (CustomWebSession) Session.get();
        var jwtToken = sessionManager.getSession(npub).getJwtToken();
        session.setAttribute("si4n6r-token", jwtToken);
        session.setSessionTimeout(TOKEN_EXPIRATION);

        // Send token to the app through relay
        var signer = new CustomIdentity("signer");
        var nip04Event = NIP04.createDirectMessageEvent(signer, new PublicKey(appField.getDefaultModelObject().toString()), jwtToken);
        Nostr.sign(nip04Event);
        Nostr.send(nip04Event);
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