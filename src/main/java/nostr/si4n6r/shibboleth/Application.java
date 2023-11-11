package nostr.si4n6r.shibboleth;

import lombok.Data;
import nostr.base.PrivateKey;
import nostr.base.PublicKey;
import nostr.base.Relay;
import nostr.client.Client;
import nostr.crypto.schnorr.Schnorr;
import nostr.id.Identity;

import java.util.HashMap;
import java.util.Map;

@Data
public class Application {

    private PublicKey user;
    private final Identity appIdentity;
    private final Relay relay;
    private final Map<String, Object> metadata;

    private static Application instance;

    private Application() {

        // TODO - introduce a configuration.
        this.appIdentity = Identity.getInstance(PrivateKey.generateRandomPrivKey());
        this.relay = Client.getInstance().getDefaultRelay();
        this.metadata = new HashMap<>();
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
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}
