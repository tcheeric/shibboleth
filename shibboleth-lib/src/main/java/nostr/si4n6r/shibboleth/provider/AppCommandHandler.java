package nostr.si4n6r.shibboleth.provider;

import com.auth0.jwt.JWT;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.api.NIP04;
import nostr.api.NIP46;
import nostr.base.Relay;
import nostr.event.impl.GenericEvent;
import nostr.si4n6r.model.dto.MethodDto;
import nostr.si4n6r.model.dto.ResponseDto;
import nostr.si4n6r.model.dto.SessionDto;
import nostr.si4n6r.shibboleth.AppService;
import nostr.si4n6r.shibboleth.Application;
import nostr.si4n6r.signer.Signer;
import nostr.si4n6r.util.Util;
import nostr.util.NostrException;
import nostr.ws.handler.command.spi.ICommandHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;

import static nostr.api.Nostr.Json.decodeEvent;


@Log
public class AppCommandHandler implements ICommandHandler {
    @Override
    public void onEose(String subId, Relay relay) {
        log.log(Level.FINER, "onEose({0}, {1})", new Object[]{subId, relay});
        // TODO
    }

    @Override
    public void onOk(String eventId, String reasonMessage, Reason reason, boolean result, Relay relay) {
        log.log(Level.FINER, "onOk({0}, {1}, {2}, {3}, {4})", new Object[]{eventId, reasonMessage, reason, result, relay});
        // TODO
    }

    @Override
    public void onNotice(String message) {
        log.log(Level.FINER, "onNotice({0})", message);
        // TODO
    }

    @Override
    public void onEvent(String jsonEvent, String subId, Relay relay) {

        log.log(Level.INFO, "Received event {0} with subscription id {1} from relay {2}", new Object[]{jsonEvent, subId, relay});

        var event = decodeEvent(jsonEvent);
        log.log(Level.INFO, "App: Decoded event: {0}", event);
        var recipient = Util.getEventRecipient(event);
        var sender = event.getPubKey();
        var app = Application.getInstance();

        // TODO - Also make sure the public key is the signer's pubkey, and ignore all other pubkeys
        if (event.getKind() == 24133 && recipient.equals(app.getPublicKey())) {
            handleKind24133(event);
        } else if (event.getKind() == 4 && sender.equals(Signer.getInstance().getIdentity().getPublicKey())) {
            handleKind4(event, Signer.getInstance());
        } else if (event.getKind() == 4 && !sender.equals(Signer.getInstance().getIdentity().getPublicKey())) {
            log.log(Level.WARNING, "Ignoring event {0} with nip {1}. INVALID SIGNER {2}", new Object[]{event, event.getNip(), sender});
        } else {
            log.log(Level.FINE, "Skipping event {0} with nip {1}. All fine!", new Object[]{event, event.getNip()});
        }
    }

    @Override
    public void onAuth(String challenge, Relay relay) {
        log.log(Level.FINER, "onAuth({0}, {1})", new Object[]{challenge, relay});
        // TODO
    }

    private static ResponseDto toResponse(@NonNull NIP46.Response nip46Response) {
        ResponseDto response = new ResponseDto();
        response.setId(nip46Response.getId());
        response.setMethod(toMethod(nip46Response.getMethod()));
        response.setSession(toSession(nip46Response.getSession()));
        response.setResponseUuid(nip46Response.getResponseUuid());
        response.setResult(nip46Response.getResult());
        response.setCreatedAt(nip46Response.getCreatedAt());
        return response;
    }

    private static SessionDto toSession(NIP46.Session session) {
        SessionDto sessionDto = new SessionDto();
        sessionDto.setId(session.getId());
        sessionDto.setApp(session.getApp());
        sessionDto.setAccount(session.getAccount());
        sessionDto.setToken(session.getToken());
        sessionDto.setCreatedAt(session.getCreatedAt());
        return sessionDto;
    }

    private static MethodDto toMethod(NIP46.Method method) {
        MethodDto methodDto = new MethodDto();
        methodDto.setId(method.getId());
        methodDto.setName(method.getName());
        methodDto.setDescription(method.getDescription());
        return methodDto;
    }

    private static void handleKind24133(GenericEvent event) {
        log.log(Level.FINE, "Processing {0}", event);

        var app = Application.getInstance();
        String content;
        try {
            content = NIP04.decrypt(app.getAppIdentity(), event);
        } catch (NostrException e) {
            throw new RuntimeException(e);
        }
        log.log(Level.FINER, "Content: {0}", content);

        if (content != null) {
            var nip46Response = NIP46.Response.fromString(content);
            var response = toResponse(nip46Response);
            var service = AppService.getInstance(app, event.getPubKey());
            service.handle(response);
        }
    }

    private void handleKind4(GenericEvent event, Signer signer) {
        // TODO - Do not allow concurrent sessions from the same app. If a session already exists for the app, ignore the new session.
        log.log(Level.FINE, "Processing {0}", event);

        String jwtToken;
        var application = Application.getInstance();
        try {
            jwtToken = NIP04.decrypt(application.getAppIdentity(), event);
        } catch (NostrException e) {
            throw new RuntimeException(e);
        }
        log.log(Level.FINER, "JWT token: {0}", jwtToken);

        if (jwtToken != null) {
            var jwt = JWT.decode(jwtToken);
            var app = jwt.getAudience().get(0);
            log.log(Level.INFO, "Audience: {0}", app);
            if (Application.getInstance().getPublicKey().toString().equalsIgnoreCase(app)) {
                log.log(Level.INFO, "A session already exists for {0}. Ignoring the login request...", app);
                return;
            }

            AppService.getInstance(signer.getIdentity().getPublicKey()).setJwtToken(jwtToken);
        }
    }

    public void sendJwtToken(String jwtToken, String server, int port) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String url = "http://" + server + ":" + port + "/shibboleth/token?jwt=" + jwtToken;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected response code: " + response.statusCode());
        }
    }

}
