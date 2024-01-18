package nostr.si4n6r.shibboleth;

import com.auth0.jwt.JWT;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.api.NIP01;
import nostr.api.NIP46.NIP46Request;
import nostr.api.Nostr;
import nostr.base.PublicKey;
import nostr.event.list.KindList;
import nostr.event.list.PublicKeyList;
import nostr.si4n6r.core.IParameter;
import nostr.si4n6r.core.impl.ApplicationProxy;
import nostr.si4n6r.core.impl.Request;
import nostr.si4n6r.core.impl.Response;
import nostr.si4n6r.core.impl.SessionManager;
import nostr.si4n6r.signer.methods.Connect;
import nostr.si4n6r.signer.methods.Describe;
import nostr.si4n6r.signer.methods.Disconnect;
import nostr.si4n6r.signer.methods.GetPublicKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static nostr.api.NIP46.createRequestEvent;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_CONNECT;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_DELEGATE;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_DESCRIBE;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_DISCONNECT;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_GET_PUBLIC_KEY;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_GET_RELAYS;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_NIP04_DECRYPT;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_NIP04_ENCRYPT;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_SIGN_EVENT;

@Data
@AllArgsConstructor
@Log
public class AppService {

    private final Application application;
    private final PublicKey signer;
    private final List<Request> requests;
    private final List<Response> responses;
    private String jwtToken;

    private static AppService instance;

    private AppService(@NonNull Application app, @NonNull PublicKey signer) {
        this(app, signer, new ArrayList<>(), new ArrayList<>(), null);
        filter();
    }

    private AppService(@NonNull PublicKey signer) {
        this(Application.getInstance(), signer, new ArrayList<>(), new ArrayList<>(), null);
        filter();
    }

    public static AppService getInstance(@NonNull PublicKey signer) {
        if (instance == null) {
            instance = new AppService(Application.getInstance(), signer);
        }
        return instance;
    }

    public static AppService getInstance(@NonNull Application app, @NonNull PublicKey signer) {
        if (instance == null) {
            instance = new AppService(app, signer);
        }
        return instance;
    }

    public void handle(@NonNull Response response) {

        log.log(Level.INFO, "Handling response {0}", response);

        if (!responses.contains(response)) {
            responses.add(response);
        } else {
            log.log(Level.WARNING, "Response {0} already handled. Ignoring...", response);
            return;
        }

        switch (response.getMethod()) {
            case METHOD_SIGN_EVENT -> {
            }
            case METHOD_CONNECT -> {
                if (response.getResult().equals("ACK")) {
                    log.log(Level.INFO, "Connected");
                } else if (null != response.getError()) {
                    log.log(Level.SEVERE, "Connection failed: {0}", response.getError());
                    // TODO - Throw an exception?
                } else {

                }
            }
            case METHOD_DISCONNECT -> {
                if (response.getResult().equals("ACK")) {
                    log.log(Level.INFO, "Disconnected");
                } else if (null != response.getError()) {
                    log.log(Level.SEVERE, "Disconnection failed: {0}", response.getError());
                    // TODO - Throw an exception?
                } else {

                }
            }
            case METHOD_DELEGATE -> {
            }
            case METHOD_DESCRIBE -> {
                if (response.getResult() instanceof List) {
                    log.log(Level.INFO, "Describe: {0}", printList(response.getResult()));
                } else if (null != response.getError()) {
                    log.log(Level.SEVERE, "Describe failed: {0}", response.getError());
                    // TODO - Throw an exception?
                } else {
                    // TODO - Throw an exception?
                }
            }
            case METHOD_GET_PUBLIC_KEY -> {
            }
            case METHOD_GET_RELAYS -> {
            }
            case METHOD_NIP04_DECRYPT -> {
            }
            case METHOD_NIP04_ENCRYPT -> {
            }
            default -> {
            }
        }
    }

    public void describe() {

        log.log(Level.INFO, "Describe...");

        if(!sessionIdValid()) {
            return;
        }

        var sessionId = getSessionId();
        var request = new Request<>(new Describe(), toApplicationProxy(application));
        request.setSessionId(sessionId);
        submit(request);
    }

    public void connect() {

        log.log(Level.INFO, "Connecting App...");

        if (!sessionIdValid()) {
            return;
        }

        var sessionId = getSessionId();
        var connect = new Connect(this.application.getPublicKey());
        var request = new Request<>(connect, toApplicationProxy(application));
        request.setSessionId(sessionId);
        submit(request);
    }

    public static void connect(@NonNull String connectURI, @NonNull PublicKey signer) {

        log.log(Level.INFO, "Connecting App with URI {0}", connectURI);

        var nostrConnectURI = NostrConnectURI.fromString(connectURI);
        var application = Application.getInstance();
        application.setUser(nostrConnectURI.getPublicKey());
        var appService = AppService.getInstance(application, signer);

        appService.connect();
    }

    public void disconnect() {
        log.log(Level.INFO, "Disconnecting App...");

        if (!sessionIdValid()) {
            return;
        }

        var sessionId = getSessionId();
        var request = new Request<>(new Disconnect(), toApplicationProxy(application));
        request.setSessionId(sessionId);
        submit(request);
    }

    public void getPublicKey() {

        log.log(Level.INFO, "Getting public key...");

        if (!sessionIdValid()) {
            return;
        }

        var sessionId = getSessionId();
        var request = new Request<>(new GetPublicKey(), toApplicationProxy(application));
        request.setSessionId(sessionId);
        submit(request);
    }

/*
    public void signEvent(@NonNull IEvent event) {
        log.log(Level.INFO, "Signing event...");

        var request = new Request(new SignEvent(event), application.getPublicKey());
        request.setSessionId(sessionId);
        submit(request);
    }

    public void nip04£ncrypt(@NonNull String plaintext) {
        log.log(Level.INFO, "Encrypting {0}...", plaintext);

        var request = new Request(new NIP04Encrypt(plaintext), application.getPublicKey());
        request.setSessionId(sessionId);
        submit(request);
    }
*/

    private void filter() {
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                var kinds = new KindList();
                kinds.add(24133);

                var authors = new PublicKeyList();
                authors.add(signer);

                var filters = NIP01.createFilters(null, authors, kinds, null, null, null, null, null, null);
                Nostr.send(filters, "shibboleth_" + signer + "_" + Thread.currentThread().getName());
            });

            // Set a reasonable timeout
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                // Handle the case when the task doesn't finish in time
                log.log(Level.WARNING, "Task didn't finish in 10 seconds.");
            }
        } catch (InterruptedException e) {
            // Handle or log the exception gracefully
            log.log(Level.SEVERE, "Thread was interrupted: {0}", e.getMessage());
        }
    }

    private void submit(@NonNull Request request) {

        log.log(Level.INFO, "Submitting request {0}", request);

        if (this.requests.contains(request)) {
            log.log(Level.WARNING, "Request {0} already submitted", request);
            return;
        } else {
            this.requests.add(request);
        }

        var params = getParams(request);
        var method = request.getMethod().getName();
        var req = new NIP46Request(request.getId(), method, params, request.getSessionId());

        // Create a request for the signer
        var event = createRequestEvent(req, application.getAppIdentity(), signer);
        log.log(Level.FINE, "Event request for the signer: {0}", event);

        Nostr.sign(event);
        Nostr.send(event);
    }

    private static List<String> getParams(@NonNull Request request) {
        List<String> params = new ArrayList<>();

        request.getMethod().getParams().forEach(p -> params.add(((IParameter) p).get().toString()));
        return params;
    }

    public static ApplicationProxy toApplicationProxy(@NonNull Application application) {
        ApplicationProxy proxy = new ApplicationProxy(application.getPublicKey());
        final Map<String, Object> metadata = application.getMetadata();
        proxy.setName(metadata.get("name").toString());
        proxy.setId(System.currentTimeMillis());

        var template = proxy.getTemplate();
        template.setDescription(metadata.get("description").toString());
        template.setName(metadata.get("name").toString());
        template.setIcons((List<String>) metadata.get("icons"));
        template.setUrl(metadata.get("url").toString());

        return proxy;
    }

    public String getSessionId() {
        if (jwtToken == null) {
            return null;
        }

        var jwt = JWT.decode(jwtToken);
        return jwt.getId();
    }

    private static String printList(Object result) {
        if (result instanceof List) {
            var list = (List<String>) result;
            var sb = new StringBuilder();
            list.forEach(s -> sb.append(s).append(", "));
            return sb.toString();
        }
        return null;
    }

    private boolean sessionIdValid() {

        var sessionId = getSessionId();
        if (sessionId == null) {
            log.log(Level.WARNING, "No session id.");
            return false;
        }

        var jwt = JWT.decode(jwtToken);
        var app = jwt.getAudience().get(0);
        var sessionManager = SessionManager.getInstance();
        var session = sessionManager.getSession(new PublicKey(app));

        if(!app.equalsIgnoreCase(application.getPublicKey().toString())) {
            log.log(Level.WARNING, "App {0} does not match the app in the JWT token {1}", new Object[]{application.getPublicKey(), app});
            return false;
        }

        if(session == null) {
            log.log(Level.WARNING, "No session found.");
            return false;
        }

        if (session.hasExpired()) {
            log.log(Level.WARNING, "Session has expired.");
            return false;
        }

        return true;
    }
}
