package nostr.si4n6r.shibboleth.web.components;

import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.id.Identity;
import nostr.si4n6r.ApplicationConfiguration;
import nostr.si4n6r.ApplicationTemplateConfiguration;
import nostr.si4n6r.bottin.model.dto.NostrIdentityDto;
import nostr.si4n6r.bottin.model.dto.RelayDto;
import nostr.si4n6r.core.impl.AccountProxy;
import nostr.si4n6r.core.impl.ApplicationProxy;
import nostr.si4n6r.rest.client.BottinRestClient;
import nostr.si4n6r.shibboleth.web.LoginPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.validator.StringValidator;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Log
public class SignUpForm extends Form {

    private static final String BOTTIN_SERVER_BASE_URL = ApplicationConfiguration.getInstance().getBottinServerBaseUrl();

    private TextField username;
    private PasswordTextField passwordField;
    private Label signUpStatus;

    public SignUpForm(String id) {
        super(id);

        username = new TextField("username", Model.of(""));
        passwordField = new PasswordTextField("password", Model.of(""));
        signUpStatus = new Label("signUpStatus", Model.of(""));

        add(username);
        add(passwordField);
        add(signUpStatus);

        // Add validation
        username.setRequired(true);
        passwordField.setRequired(true);
        passwordField.add(StringValidator.minimumLength(8));
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public final void onSubmit() {
        log.log(Level.INFO, "Signing up...");

        try {
            var app = getApp();
            var template = ApplicationTemplateConfiguration.getInstance().getTemplate();
            var applicationProxy = new ApplicationProxy(app, template);
            var appConfig = ApplicationConfiguration.getInstance();
            applicationProxy.setPublicKey(appConfig.getAppNpub());
            applicationProxy.setName("Cyclops");

            var username = URLEncoder.encode((String) this.username.getModelObject(), StandardCharsets.UTF_8);
            var password = URLEncoder.encode(passwordField.getModelObject(), StandardCharsets.UTF_8);

            log.log(Level.INFO, "Creating the nip05 identity...");
            var identity = Identity.generateRandomIdentity();
            var npub = identity.getPublicKey().toString();
            var nsec = identity.getPrivateKey().toString();

            log.log(Level.INFO, "Calling the identity service...");
            var identityClient = new BottinRestClient<>("identities", NostrIdentityDto.class);

            List<RelayDto>relays = new ArrayList<>();
            var relayClient = new BottinRestClient<>("relays", RelayDto.class);
            var relay = relayClient.get(1L);
            relay.setId(1L);
            log.log(Level.INFO, "Relay: {0}", relay);
            relays.add(relay);

            var nostrIdentity = identityClient.create(new NostrIdentityDto(username, "badgr.space", npub));
            log.log(Level.INFO, "- Nostr identity: {0}", nostrIdentity.get_links());
            nostrIdentity.setRelays(relays);
            nostrIdentity.setId(nostrIdentity.getIdFromLinks());
            identityClient.update(nostrIdentity.getId(), nostrIdentity);
            log.log(Level.INFO, "+ Nostr identity: {0}", nostrIdentity);

            var accountProxy = new AccountProxy(nsec, applicationProxy);
            accountProxy.setPublicKey(npub);

            log.log(Level.INFO, "Registering account: {0}", accountProxy);
            var result = registerIdentity(npub, nsec, password, applicationProxy.getName(), applicationProxy.getPublicKey());
            log.log(Level.INFO, "Result: {0}", result);
            signUpStatus.setDefaultModelObject("Sign-up successful!");

            setResponsePage(LoginPage.class);
        } catch (RuntimeException e) {
            signUpStatus.setDefaultModelObject("Sign-up failed:" + e);
        }
    }

    private String registerIdentity(@NonNull String npub, @NonNull String nsec, @NonNull String password, @NonNull String appName, @NonNull String appPubKey) {
        try {
            String urlWithParams = UriComponentsBuilder.fromHttpUrl(BOTTIN_SERVER_BASE_URL + "/identity")
                    .queryParam("npub", npub)
                    .queryParam("nsec", nsec)
                    .queryParam("password", password)
                    .queryParam("appName", appName)
                    .queryParam("appPubKey", appPubKey)
                    .toUriString();

            return sendPostRequest(urlWithParams);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error registering identity: " + npub, e);
            throw new RuntimeException(e);
        }
    }

    private String sendPostRequest(String urlString) throws Exception {
        URL url = new URI(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        log.log(Level.INFO, "Sending 'POST' request to URL : {0}", conn.getURL());

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            var in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return content.toString();
        } else {
            // Registration failed
            throw new RuntimeException("Registration failed. Response code: " + responseCode);
        }
    }

    private static String getApp() {
        return ApplicationConfiguration.getInstance().getAppNpub();
    }

}
