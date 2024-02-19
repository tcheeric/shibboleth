package nostr.si4n6r.shibboleth.web;

import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.IOException;
import java.io.Serial;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

@Log
public class SendTokenPage extends WebPage {

    @Serial
    private static final long serialVersionUID = 1L;

    public SendTokenPage(final PageParameters parameters) {
        super(parameters);

        // Get the jwt parameter
        var jwt = parameters.get("jwt").toString();
        var app = parameters.get("app").toString();

        // Call the token endpoint with the jwt parameter
        callTokenEndpoint(jwt, app);
    }

    private void callTokenEndpoint(@NonNull String jwt, @NonNull String app) {
        try {
            // Create a URL string that includes the jwt parameter
            int responseCode = getResponseCode(jwt, app);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                log.log(Level.INFO, "Successfully called the token endpoint with jwt: " + jwt);
            } else {
                log.log(Level.SEVERE, "Failed to call the token endpoint with jwt: " + jwt + ". Response code: " + responseCode);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error calling the token endpoint with jwt: " + jwt, e);
        } catch (URISyntaxException e) {
            log.log(Level.SEVERE, "Error creating the URL object from the URL string: " + jwt, e);
            throw new RuntimeException(e);
        }
    }

    private static int getResponseCode(String jwt, String app) throws URISyntaxException, IOException {
        var urlString = "http://localhost:8080/shibboleth/token?jwt=" + jwt + "&app=" + app; // TODO - Fix me

        // Create a URL object from the URL string
        var url = new URI(urlString).toURL();

        // Open a connection to the URL
        var conn = (HttpURLConnection) url.openConnection();

        // Set the request method to GET
        conn.setRequestMethod("GET");

        // Get the response code
        int responseCode = conn.getResponseCode();
        return responseCode;
    }
}