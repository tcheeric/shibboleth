package nostr.si4n6r.shibboleth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NostrConnectURITest {

    @Test
    @DisplayName("Test a nostr connect URI")
    public void fromString() {
        var uri = NostrConnectURI.fromString("nostrconnect://9cb64796ed2c5f18846082cae60c3a18d7a506702cdff0276f86a2ea68a94123?relay=wss%3A%2F%2Frelay.damus.io&metadata=%7B%22name%22%3A%22Example%22%7D");

        var relay = uri.getRelay();
        var metadata = uri.getMetadata();
        var publicKey = uri.getPublicKey();

        assertEquals("relay.damus.io", relay.getHostname());
        assertEquals("wss", relay.getScheme());
        assertEquals("Example", metadata.get("name"));
        assertEquals("9cb64796ed2c5f18846082cae60c3a18d7a506702cdff0276f86a2ea68a94123", publicKey.toString());
    }
}
