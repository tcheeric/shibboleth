package nostr.nip46.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import nostr.base.PublicKey;

@Data
@AllArgsConstructor
public class NostrUser {

    public static final NostrUser DUMMY_USER = new NostrUser(new PublicKey("9cb64796ed2c5f18846082cae60c3a18d7a506702cdff0276f86a2ea68a94123"));

    private final PublicKey publicKey;
}
