package nostr.si4n6r.shibboleth;

import lombok.Data;
import lombok.NonNull;
import nostr.base.*;
import nostr.client.Client;
import nostr.crypto.schnorr.Schnorr;
import nostr.id.IIdentity;
import nostr.id.Identity;
import nostr.id.IdentityHelper;
import nostr.util.NostrException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class Application implements IIdentity {

    private PublicKey user;
    private final Identity appIdentity;
    private final Relay relay;
    private final Map<String, Object> metadata;

    private ConnectionContext connectionContext;

    private static Application instance;

    private Application() {

        // TODO - introduce a configuration.
        this.appIdentity = Identity.getInstance(PrivateKey.generateRandomPrivKey());
        this.relay = Client.getInstance().getDefaultRelay();
        this.metadata = new HashMap<>();
        this.connectionContext = null;
    }

    public static Application getInstance() {
        if (instance == null) {
            instance = new Application();
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

    @Override
    public Signature sign(@NonNull ISignable signable) {
        try {
            return new IdentityHelper(this).sign(signable);
        } catch (NostrException e) {
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
