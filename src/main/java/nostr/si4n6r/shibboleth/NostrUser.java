package nostr.si4n6r.shibboleth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import nostr.api.Nostr;
import nostr.base.PublicKey;

public interface NostrUser {
    PublicKey getPublicKey();
}
