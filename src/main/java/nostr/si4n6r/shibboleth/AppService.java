package nostr.si4n6r.shibboleth;

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
import nostr.event.tag.PubKeyTag;
import nostr.si4n6r.core.IParameter;
import nostr.si4n6r.core.impl.Request;
import nostr.si4n6r.core.impl.Response;
import nostr.si4n6r.core.impl.methods.Connect;
import nostr.si4n6r.core.impl.methods.Describe;
import nostr.si4n6r.core.impl.methods.Disconnect;
import nostr.si4n6r.core.impl.methods.GetPublicKey;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static nostr.api.NIP46.createRequestEvent;
import static nostr.si4n6r.core.IMethod.Constants.*;

@Data
@AllArgsConstructor
@Log
public class AppService {

    private final Application application;
    private final PublicKey signer;
    private final List<Response> responses;
    private String sessionId;

    private static AppService instance;

    private AppService(@NonNull Application app, @NonNull PublicKey signer) {
        this(app, signer, new ArrayList<>(), null);
        filter();
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
            log.log(Level.WARNING, "Response {0} already handled", response);
            return;
        }

        // Update the session id
        this.sessionId = response.getSessionId();

        switch (response.getMethod()) {
            case METHOD_SIGN_EVENT -> {
            }
            case METHOD_CONNECT -> {
                if (response.getResult().equals("ACK")) {
                    log.log(Level.INFO, "Connected");
                } else {
                    log.log(Level.SEVERE, "Connection failed: {0}", response.getError());
                }
            }
            case METHOD_DISCONNECT -> {
                if (response.getResult().equals("ACK")) {
                    log.log(Level.INFO, "Disconnected");
                } else {
                    log.log(Level.SEVERE, "Disconnection failed: {0}", response.getError());
                }
            }
            case METHOD_DELEGATE -> {
            }
            case METHOD_DESCRIBE -> {
                if (response.getResult().equals("ACK")) {
                    log.log(Level.INFO, "Describe: {0}", printList(response.getResult()));
                } else {
                    log.log(Level.SEVERE, "Described failed: {0}", response.getError());
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

    private String printList(Object result) {
        var list = (List<String>) result;
        var sb = new StringBuilder();
        list.forEach(s -> sb.append(s).append(", "));
        return sb.toString();
    }

    public void describe() {
        var request = new Request(new Describe(), application.getPublicKey());
        request.setSessionId(sessionId);
        submit(request);
    }

    public void connect() {

        log.log(Level.INFO, "Connecting App...");

        var connect = new Connect(this.application.getPublicKey());
        var request = new Request(connect, application.getPublicKey());
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

        var request = new Request(new Disconnect(), application.getPublicKey());
        request.setSessionId(sessionId);
        submit(request);
    }

    public void getPublicKey() {
        log.log(Level.INFO, "Getting public key...");

        var request = new Request(new GetPublicKey(), application.getPublicKey());
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

        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            var kinds = new KindList();
            kinds.add(24133);

            var authors = new PublicKeyList();
            authors.add(signer);

            var filters = NIP01.createFilters(null, authors, kinds, null, null, null, null, null, null);
            Nostr.send(filters, "shibboleth_" + signer + "_" + Thread.currentThread().getName());
        });

        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdownNow();
        }
    }

    private void submit(@NonNull Request request) {

        log.log(Level.INFO, "Submitting request {0}", request);

        var params = getParams(request);
        var method = request.getMethod().getName();
        var req = new NIP46Request(request.getId(), method, params, request.getSessionId());

        // Create a request for the signer
        var event = createRequestEvent(req, application.getAppIdentity(), signer);

        // Add the user pubkey
        // TODO - Is this still necessary?
        event.addTag(PubKeyTag.builder().publicKey(this.application.getPublicKey()).build());

        log.log(Level.FINE, "Event request for the signer: {0}", event);

        Nostr.sign(event);
        Nostr.send(event);
    }

    private static List<String> getParams(@NonNull Request request) {
        List<String> params = new ArrayList<>();

        request.getMethod().getParams().forEach(p -> params.add(((IParameter) p).get().toString()));
        return params;
    }

}
