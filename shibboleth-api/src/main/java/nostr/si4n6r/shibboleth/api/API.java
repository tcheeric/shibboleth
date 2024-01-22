package nostr.si4n6r.shibboleth.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.api.NIP04;
import nostr.api.Nostr;
import nostr.base.PublicKey;
import nostr.base.Relay;
import nostr.id.CustomIdentity;
import nostr.id.IIdentity;
import nostr.si4n6r.core.impl.Request;
import nostr.si4n6r.shibboleth.AppService;
import nostr.si4n6r.signer.SignerService;
import nostr.si4n6r.signer.methods.Connect;
import nostr.si4n6r.signer.methods.Describe;
import nostr.si4n6r.signer.methods.Disconnect;

@Log
public class API {

    @AllArgsConstructor
    @Data
    public static class Method {
        private final PublicKey signer;
        private final PublicKey app;
        private final String jwt;

        /**
         * Connect the app to the Signer Service
         */
        public void connect() {

            var request = new Request<>(new Connect(app), getJwt());
            var signerService = SignerService.getInstance(getRelay());

            signerService.handle(request);
        }

        /**
         * Disconnect the app from the Signer Service
         */
        public void disconnect() {

            var request = new Request<>(new Disconnect(app), getJwt());
            var signerService = SignerService.getInstance(getRelay());

            signerService.handle(request);
        }

        /**
         * Describe the Signer Service's supported methods
         */
        public void describe() {

            var request = new Request<>(new Describe(), getJwt());
            var signerService = SignerService.getInstance(getRelay());

            signerService.handle(request);
        }

        private static Relay getRelay() {
            return null;
        }
    }

    public static void sendToken(@NonNull String jwt, @NonNull PublicKey app) {
        IIdentity signer = new CustomIdentity("signer");
        var dm = NIP04.createDirectMessageEvent(signer, app, jwt);
        Nostr.sign(dm);
        Nostr.send(dm);
    }
}
