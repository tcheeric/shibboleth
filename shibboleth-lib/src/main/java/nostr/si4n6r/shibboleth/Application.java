package nostr.si4n6r.shibboleth;

import lombok.Data;
import lombok.NonNull;
import nostr.base.PrivateKey;
import nostr.base.PublicKey;
import nostr.id.IIdentity;
import nostr.id.Identity;

import java.util.HashMap;
import java.util.Map;

@Data
public class Application {

    private PublicKey user;
    private final IIdentity appIdentity;
    //private final Relay relay;
    private final Map<String, Object> metadata;

    private static Application instance;

    private Application() {
        this.appIdentity = Identity.getInstance(PrivateKey.generateRandomPrivKey());
        //this.relay = Client.getInstance().getDefaultRelay();
        this.metadata = new HashMap<>();
    }

    private Application(@NonNull IIdentity identity) {
        this.appIdentity = identity;
        //this.relay = Client.getInstance().getDefaultRelay();
        this.metadata = new HashMap<>();
    }

    public static Application getInstance() {
        if (instance == null) {
            instance = new Application();
        }
        return instance;
    }

    public static Application getInstance(@NonNull IIdentity identity) {
        if (instance == null || !instance.getAppIdentity().getPublicKey().equals(identity.getPublicKey())) {
            instance = new Application(identity);
        }
        return instance;
    }

    public PrivateKey getPrivateKey() {
        return this.appIdentity.getPrivateKey();
    }

    public PublicKey getPublicKey() {
        try {
            return this.appIdentity.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}
