package nostr.si4n6r.shibboleth.web;

import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.base.PublicKey;
import nostr.si4n6r.shibboleth.api.API;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

@Log
public class SendTokenPage extends WebPage {

    private static final long serialVersionUID = 1L;

    public SendTokenPage(final PageParameters parameters) {
        super(parameters);

        // Get the jwt parameter
        var jwt = parameters.get("jwt").toString();
        var app = parameters.get("app").toString();

        // Call the token endpoint with the jwt parameter
        callTokenEndpoint(jwt, app);

        // TODO - java.lang.LinkageError: loader constraint violation: when resolving method 'void org.eclipse.jetty.websocket.client.WebSocketClient.<init>(org.eclipse.jetty.client.HttpClient)'
        //  the class loader org.eclipse.jetty.webapp.WebAppClassLoader @1b4872bc of the current class, nostr/ws/Connection,
        //  and the class loader org.codehaus.plexus.classworlds.realm.ClassRealm @54067fdc for the method's defining class, org/eclipse/jetty/websocket/client/WebSocketClient,
        //  have different Class objects for the type org/eclipse/jetty/client/HttpClient used in the signature (nostr.ws.Connection is in unnamed module of loader org.eclipse.jetty.webapp.WebAppClassLoader @1b4872bc,
        //  parent loader org.codehaus.plexus.classworlds.realm.ClassRealm @54067fdc; org.eclipse.jetty.websocket.client.WebSocketClient is in unnamed module of loader org.codehaus.plexus.classworlds.realm.ClassRealm @54067fdc, parent loader 'bootstrap')
        //	at java.base/java.util.concurrent.FutureTask.report(FutureTask.java:122)
        //	at java.base/java.util.concurrent.FutureTask.get(FutureTask.java:191)
        //API.sendToken(jwt, new PublicKey(app));
    }

    private void callTokenEndpoint(@NonNull String jwt, @NonNull String app) {
        try {
            // Create a URL string that includes the jwt parameter
            String urlString = "http://localhost:8080/shibboleth/token?jwt=" + jwt + "&app=" + app;

            // Create a URL object from the URL string
            var url = new URI(urlString).toURL();

            // Open a connection to the URL
            var conn = (HttpURLConnection) url.openConnection();

            // Set the request method to GET
            conn.setRequestMethod("GET");

            // Get the response code
            int responseCode = conn.getResponseCode();

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
}