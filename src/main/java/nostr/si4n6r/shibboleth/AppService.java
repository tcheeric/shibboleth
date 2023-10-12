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
import nostr.si4n6r.core.Request;
import nostr.si4n6r.core.Response;
import nostr.si4n6r.core.impl.Connect;
import nostr.si4n6r.core.impl.Describe;
import nostr.si4n6r.core.impl.Disconnect;

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

    private static AppService instance;

    private AppService(@NonNull Application app, @NonNull PublicKey signer) {
        this(app, signer, new ArrayList<>());
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
        }

        switch (response.getMethod()) {
            case METHOD_SIGN_EVENT -> {
            }
            case METHOD_CONNECT -> {
            }
            case METHOD_DELEGATE -> {
            }
            case METHOD_DESCRIBE -> {
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
        var request = new Request(new Describe(), application.getPublicKey());
        submit(request);
    }

    public void connect() {

        log.log(Level.INFO, "Connecting App...");

        var connect = new Connect(this.application.getPublicKey());
        var request = new Request(connect, application.getPublicKey());
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

        log.log(Level.INFO, "Disonnecting App...");

        var request = new Request(new Disconnect(), application.getPublicKey());
        submit(request);
    }

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
        var req = new NIP46Request(request.getId(), method, params);

        // Create a request for the signer
        var event = createRequestEvent(req, application.getAppIdentity(), signer);

        // Add the user pubkey
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
