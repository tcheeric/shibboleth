package nostr.nip46.app;

import lombok.Data;
import lombok.NonNull;
import nostr.base.PrivateKey;
import nostr.base.PublicKey;
import nostr.base.Relay;
import nostr.client.Client;
import nostr.crypto.schnorr.Schnorr;
import nostr.id.IIdentity;
import nostr.id.Identity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class Application implements IIdentity {

    private final NostrUser user;
    private final Identity appIdentity;
    private final Relay relay;
    private final Map<String, Object> metadata;

    private ConnectionContext connectionContext;

    private static Application instance;

    private Application(@NonNull NostrUser user) {

        // TODO - introduce a configuration.
        this.appIdentity = Identity.getInstance(PrivateKey.generateRandomPrivKey());

        this.user = user;
        this.relay = Client.getInstance().getDefaultRelay();
        this.metadata = new HashMap<>();
        this.connectionContext = null;
    }

    public static Application getInstance(@NonNull NostrUser user) {
        if (instance == null) {
            instance = new Application(user);
        }
        return instance;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return this.appIdentity.getPrivateKey();
    }

    public PublicKey getPublicKey() {
        try {
            return new PublicKey(Schnorr.genPubKey(getPrivateKey().getRawData()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void connect(ConnectionContext context) {
        this.setConnectionContext(context);
    }

    public void disconnect() {
        this.connect(null);
    }

    public boolean isConnected() {
        return this.connectionContext != null;
    }

    public boolean isDisconnected() {
        return !isConnected();
    }

    @Data
    public static class ConnectionContext {
        private Date date;
        private PublicKey signer;

        public ConnectionContext(@NonNull PublicKey publicKey) {
            this.date = new Date();
            this.signer = publicKey;
        }
    }
}
