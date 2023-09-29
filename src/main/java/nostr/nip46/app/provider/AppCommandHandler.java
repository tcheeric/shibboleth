package nostr.nip46.app.provider;

import lombok.extern.java.Log;
import nostr.api.NIP04;
import nostr.api.NIP46;
import nostr.base.Relay;
import nostr.event.impl.GenericEvent;
import nostr.event.json.codec.GenericEventDecoder;
import nostr.nip46.app.AppService;
import nostr.nip46.app.Application;
import nostr.nip46.app.NostrUser;
import nostr.si4n6r.util.Util;
import nostr.util.NostrException;
import nostr.ws.handler.command.spi.ICommandHandler;

import java.util.logging.Level;

import static nostr.si4n6r.util.Util.toResponse;


@Log
public class AppCommandHandler implements ICommandHandler {
    @Override
    public void onEose(String subId, Relay relay) {
        log.log(Level.INFO, "onEose({0}, {1})", new Object[]{subId, relay});
        // TODO
    }

    @Override
    public void onOk(String eventId, String reasonMessage, Reason reason, boolean result, Relay relay) {
        log.log(Level.INFO, "onOk({0}, {1}, {2}, {3}, {4})", new Object[]{eventId, reasonMessage, reason, result, relay});
        // TODO
    }

    @Override
    public void onNotice(String message) {
        log.log(Level.INFO, "onNotice({0})", message);
        // TODO
    }

    @Override
    public void onEvent(String jsonEvent, String subId, Relay relay) {
        log.log(Level.INFO, "App: onEvent({0}, {1}, {2})", new Object[]{jsonEvent, subId, relay});
        var event = decodeEvent(jsonEvent);
        log.log(Level.INFO, "App: Decoded event: {0}", event);
        var recipient = Util.getEventRecipient(event);
        var app = Application.getInstance(NostrUser.DUMMY_USER);

        // TODO - Also make sure the public key is the signer's pubkey, and ignore all other pubkeys
        if (event.getKind() == 24133 && recipient.equals(app.getPublicKey())) {
            log.log(Level.INFO, ">>>> Gotcha! App: {0}", event);

            String content = null;
            try {
                content = NIP04.decrypt(event);
            } catch (NostrException e) {
                throw new RuntimeException(e);
            }
            log.log(Level.INFO, "Content: {0}", content);

            if (content != null) {
                var nip46Response = NIP46.NIP46Response.fromString(content);
                var response = toResponse(nip46Response);
                var service = AppService.getInstance(app, event.getPubKey());
                service.handle(response);
            }
        } else {
            log.log(Level.FINE, "Skipping event {0} with nip {1}. All fine!", new Object[]{event, event.getNip()});
        }
    }

    @Override
    public void onAuth(String challenge, Relay relay) {
        log.log(Level.INFO, "onAuth({0}, {1})", new Object[]{challenge, relay});
        // TODO
    }

    private static GenericEvent decodeEvent(String jsonEvent) {
        var decoder = new GenericEventDecoder(jsonEvent);
        return decoder.decode();
    }

}
