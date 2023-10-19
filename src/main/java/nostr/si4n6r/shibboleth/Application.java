package nostr.si4n6r.shibboleth;

import lombok.Data;
import lombok.NonNull;
import nostr.base.*;
import nostr.client.Client;
import nostr.crypto.schnorr.Schnorr;
import nostr.id.IIdentity;
import nostr.id.Identity;
import nostr.id.IdentityHelper;
import nostr.si4n6r.core.ConnectionManager;
import nostr.util.NostrException;

import java.util.HashMap;
import java.util.Map;

@Data
public class Application {

    private PublicKey user;
    private final Identity appIdentity;
    private final Relay relay;
    private final Map<String, Object> metadata;

    private ConnectionManager connectionManager;

    private static Application instance;

    private Application() {

        // TODO - introduce a configuration.
        this.appIdentity = Identity.getInstance(PrivateKey.generateRandomPrivKey());
        this.relay = Client.getInstance().getDefaultRelay();
        this.metadata = new HashMap<>();
        this.connectionManager = ConnectionManager.getInstance();
    }

    public static Application getInstance() {
        if (instance == null) {
            instance = new Application();
        }
        return instance;
    }

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
}
