package nostr.nip46.app;

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
import nostr.si4n6r.core.IMethod;
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
import static nostr.si4n6r.core.IMethod.Constants.METHOD_CONNECT;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_DELEGATE;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_DESCRIBE;
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
        if (!responses.contains(response)) {
            responses.add(response);
        }

        switch (response.getMethod()) {
            case METHOD_SIGN_EVENT -> {}
            case METHOD_CONNECT -> {}
            case METHOD_DELEGATE -> {}
            case METHOD_DESCRIBE -> {}
            case METHOD_GET_PUBLIC_KEY -> {}
            case METHOD_GET_RELAYS -> {}
            case METHOD_NIP04_DECRYPT -> {}
            case METHOD_NIP04_ENCRYPT -> {}
            default -> {}
        }
    }

    public void describe() {
        var request = new Request(new Describe(), application.getPublicKey());
        submit(request);
    }

    public void connect() {
        var connect = new Connect(this.application.getPublicKey());
        var request = new Request(connect, application.getPublicKey());
        submit(request);
    }

    public void disconnect() {
        var request = new Request(new Disconnect(), application.getPublicKey());
        submit(request);
    }

    private void filter() {

        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            log.log(Level.INFO, "Running the filter from within {0}", Thread.currentThread().getName());

            var kinds = new KindList();
            kinds.add(24133);

            var authors = new PublicKeyList();
            authors.add(signer);

            var filters = NIP01.createFilters(null, authors, kinds, null, null, null, null, null, null);
            Nostr.send(filters, "shibboleth_" + signer.toString() + "_" + Thread.currentThread().getName());
        });

        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        } finally {
            executor.shutdownNow();
        }
    }

    private void submit(@NonNull Request request) {
        var params = getParams(request);
        var method = request.getMethod().getName();
        var req = new NIP46Request(request.getId(), method, params);

        // Create a request for the signer
        var event = createRequestEvent(application.getAppIdentity(), req, signer);

        log.log(Level.INFO, "Event request for the signer: {0}", event);

        Nostr.sign(event);
        Nostr.send(event);
    }

    private static List<String> getParams(@NonNull Request request) {
        List<String> params = new ArrayList<>();

        request.getMethod().getParams().forEach(p -> params.add(((IParameter) p).get().toString()));
        return params;
    }

}
