package nostr.si4n6r.shibboleth;

import com.auth0.jwt.JWT;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.api.NIP01;
import nostr.api.NIP46;
import nostr.api.Nostr;
import nostr.base.IEvent;
import nostr.base.PublicKey;
import nostr.event.list.KindList;
import nostr.event.list.PublicKeyList;
import nostr.si4n6r.storage.common.ApplicationProxy;
import nostr.si4n6r.model.dto.MethodDto;
import nostr.si4n6r.model.dto.ParameterDto;
import nostr.si4n6r.model.dto.RequestDto;
import nostr.si4n6r.model.dto.ResponseDto;
import nostr.si4n6r.model.dto.SessionDto;
import nostr.si4n6r.rest.client.MethodRestClient;
import nostr.si4n6r.rest.client.ParameterRestClient;
import nostr.si4n6r.rest.client.SessionManager;
import nostr.si4n6r.signer.SignerService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static nostr.api.NIP46.createRequestEvent;

@Data
@AllArgsConstructor
@Log
public class AppService {

    private final Application application;
    private final PublicKey signer;
    private String jwtToken;

    private static AppService instance;

    private AppService(@NonNull Application app, @NonNull PublicKey signer) {
        this(app, signer, null);
        filter();
    }

    private AppService(@NonNull PublicKey signer) {
        this(Application.getInstance(), signer, null);
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

    public void handle(@NonNull ResponseDto response) {

        log.log(Level.INFO, "Handling response {0}", response);

        switch (response.getMethod().getName()) {
            case "sign_event" -> {
            }
            case "connect" -> {
                var result = getResult(response.getResult());
                if (ResponseDto.RESULT_ACK.equals(result.getValue())) {
                    log.log(Level.INFO, "Connected");
                } else {
                    log.log(Level.SEVERE, "Connection failed.");
                    // TODO - Throw an exception?
                }
            }
            case "disconnect" -> {
                var result = getResult(response.getResult());
                if (ResponseDto.RESULT_ACK.equals(result.getValue())) {
                    log.log(Level.INFO, "Disconnected");
                }
            }
            case "delegate" -> {
            }
            case "describe" -> {
                var result = getResult(response.getResult());
                if (ResponseDto.RESULT_ACK.equals(result.getValue())) {
                    log.log(Level.INFO, "Describe: {0}", result.toJson());
                } else {
                    // TODO - Throw an exception?
                }
            }
            case "get_public_key" -> {
            }
            case "get_relays" -> {
            }
            case "nip04_encrypt" -> {
            }
            case "nip04_decrypt" -> {
            }
            default -> {
            }
        }
    }

    public void describe() {

        log.log(Level.INFO, "Describe...");

        if (!sessionIdValid()) {
            return;
        }

        var request = new RequestDto();
        request.setMethod(getMethod(MethodDto.MethodType.DESCRIBE.getName()));
        request.setToken(jwtToken);
        request.setInitiator(this.application.getPublicKey().toString());
        request.setSession(getSession());
        submit(request);
    }

    public void connect() {

        log.log(Level.INFO, "Connecting App...");

        if (!sessionIdValid()) {
            return;
        }

        var request = new RequestDto();
        request.setMethod(getMethod(MethodDto.MethodType.CONNECT.getName()));
        request.setToken(jwtToken);
        request.setInitiator(this.application.getPublicKey().toString());
        request.setSession(getSession());
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

        var request = new RequestDto();
        request.setMethod(getMethod(MethodDto.MethodType.DISCONNECT.getName()));
        request.setToken(jwtToken);
        request.setInitiator(this.application.getPublicKey().toString());
        request.setSession(getSession());
        submit(request);
    }

    public void getPublicKey() {

        log.log(Level.INFO, "Getting public key...");

        if (!sessionIdValid()) {
            return;
        }

        var request = new RequestDto();
        request.setMethod(getMethod(MethodDto.MethodType.GET_PUBLIC_KEY.getName()));
        request.setToken(jwtToken);
        request.setInitiator(this.application.getPublicKey().toString());
        request.setSession(getSession());
        submit(request);
    }


    public void signEvent(@NonNull IEvent event) {
        log.log(Level.INFO, "Signing event...");

        if (!sessionIdValid()) {
            return;
        }

        var request = new RequestDto();
        request.setMethod(getMethod(MethodDto.MethodType.SIGN_EVENT.getName()));
        request.setToken(jwtToken);
        request.setInitiator(this.application.getPublicKey().toString());
        request.setSession(getSession());
        submit(request);
    }


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

    private void submit(@NonNull RequestDto request) {

        log.log(Level.INFO, "Submitting request {0}", request);

        var nip46Request = new NIP46.Request();
        nip46Request.setMethod(toNIP46Method(request.getMethod()));
        nip46Request.setSession(toNIP46Session(request.getSession()));
        nip46Request.setInitiator(request.getInitiator());
        nip46Request.setRequestUuid(request.getRequestUuid());
        nip46Request.setToken(request.getToken());
        nip46Request.setParameters(getParams(request));
        nip46Request.setCreatedAt(request.getCreatedAt());

        // Create a request for the signer
        var event = createRequestEvent(nip46Request, application.getAppIdentity(), signer);
        log.log(Level.FINE, "Event request for the signer: {0}", event);

        Nostr.sign(event);
        Nostr.send(event);
    }

    private NIP46.Method toNIP46Method(MethodDto method) {
        NIP46.Method nip46Method = new NIP46.Method();
        nip46Method.setId(method.getId());
        nip46Method.setDescription(method.getDescription());
        nip46Method.setName(method.getName());
        return nip46Method;
    }

    private NIP46.Response toNIP46Response(ResponseDto response) {
        NIP46.Response nip46Response = new NIP46.Response();
        nip46Response.setMethod(toNIP46Method(response.getMethod()));
        nip46Response.setSession(toNIP46Session(response.getSession()));
        nip46Response.setResponseUuid(response.getResponseUuid());
        nip46Response.setResult(response.getResult());
        return nip46Response;
    }

    private NIP46.Request toNIP46Request(RequestDto request) {
        NIP46.Request nip46Request = new NIP46.Request();
        nip46Request.setMethod(toNIP46Method(request.getMethod()));
        nip46Request.setSession(toNIP46Session(request.getSession()));
        nip46Request.setRequestUuid(request.getRequestUuid());
        return nip46Request;
    }

    private NIP46.Session toNIP46Session(SessionDto session) {
        NIP46.Session nip46Session = new NIP46.Session();
        nip46Session.setId(session.getId());
        nip46Session.setApp(session.getApp());
        nip46Session.setAccount(session.getAccount());
        nip46Session.setToken(session.getToken());
        return nip46Session;
    }

    private NIP46.Parameter toNIP46Parameter(ParameterDto p) {
        NIP46.Parameter param = new NIP46.Parameter();
        param.setId(p.getId());
        param.setName(p.getName());
        param.setValue(p.getValue());
        return param;
    }

    private Set<NIP46.Parameter> getParams(@NonNull RequestDto request) {
        Set<NIP46.Parameter> result = new LinkedHashSet<>();

        var restClient = new ParameterRestClient();
        var params = restClient.getParametersByRequest(request);

        params.forEach(p -> result.add(toNIP46Parameter(p)));

        return result;
    }

    public static ApplicationProxy toApplicationProxy(@NonNull Application application) {
        var proxy = new ApplicationProxy(application.getPublicKey());
        final Map<String, Object> metadata = application.getMetadata();
        proxy.setName(metadata.get("name").toString());
        proxy.setId(String.valueOf(System.currentTimeMillis()));

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

    private boolean sessionIdValid() {

        var sessionId = getSessionId();
        if (sessionId == null) {
            log.log(Level.WARNING, "No session id.");
            return false;
        }

        var jwt = JWT.decode(jwtToken);
        var app = jwt.getAudience().get(0);
        var sessionManager = SessionManager.getInstance();
        var session = sessionManager.getSession(app);

        if (!app.equalsIgnoreCase(application.getPublicKey().toString())) {
            log.log(Level.WARNING, "App {0} does not match the app in the JWT token {1}", new Object[]{application.getPublicKey(), app});
            return false;
        }

        if (session == null) {
            log.log(Level.WARNING, "No session found.");
            return false;
        }

        if (sessionManager.sessionIsInactive(app)) {
            log.log(Level.WARNING, "Session has expired.");
            return false;
        }

        return true;
    }

    private SignerService.Result getResult(String result) {
        return SignerService.Result.fromJson(result);
    }

    private MethodDto getMethod(@NonNull String name) {
        var restClient = new MethodRestClient();
        return restClient.getMethodByName(name);
    }

    private SessionDto getSession() {
        var sessionManager = SessionManager.getInstance();
        return sessionManager.getSession(application.getPublicKey().toString());
    }
}
