package nostr.si4n6r.shibboleth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import nostr.base.PublicKey;
import nostr.base.Relay;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class NostrConnectURI {

    private final static String NOSTR_CONNECT_PROTOCOL = "nostrconnect";

    @NonNull
    private final PublicKey publicKey;
    private final Relay relay;
    @NonNull
    private final Map<String, String> metadata;

    @Override
    public String toString() {
        var sb = new StringBuffer(NOSTR_CONNECT_PROTOCOL).append("://");
        sb.append(publicKey);

        if (relay != null) {
            sb.append("?relay=").append(relay);
        }

        if (relay == null) {
            sb.append("?");
        } else {
            sb.append("&");
        }

        var jsonMetadata = getJsonMetadata();
        sb.append(jsonMetadata);

        return URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
    }

    public static NostrConnectURI fromString(@NonNull String uri) {

        if (!uri.startsWith(NOSTR_CONNECT_PROTOCOL + "://")) {
            throw new IllegalArgumentException("Invalid NostrConnectURI format");
        }

        try {
            // Remove the protocol part and decode the URI
            String decodedUri = URLDecoder.decode(uri.substring(NOSTR_CONNECT_PROTOCOL.length() + 3), StandardCharsets.UTF_8);

            // Parse the URI components
            URI uriComponents = new URI(decodedUri);
            String publicKeyStr = uriComponents.getHost();
            Map<String, String> queryParams = parseQueryParameters(uriComponents.getQuery());

            // Extract privateKey, relay, and metadata
            var publicKey = new PublicKey(publicKeyStr);
            String relayUri = queryParams.get("relay");
            var relay = getRelay(relayUri);
            Map<String, String> metadata = new ObjectMapper().readValue(queryParams.get("metadata"), new TypeReference<>() {
            });

            return new NostrConnectURI(publicKey, relay, metadata);
        } catch (URISyntaxException | JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid NostrConnectURI format", e);
        }
    }

    private static Relay getRelay(@NonNull String relayUri) {
        Relay relay;
        if (relayUri.isEmpty()) {
            throw new RuntimeException("Invalid relay protocol");
        } else if (relayUri.startsWith(Relay.PROTOCOL_WSS)) {
            relay = new Relay(relayUri.substring(Relay.PROTOCOL_WSS.length() + 3));
        } else if (relayUri.startsWith(Relay.PROTOCOL_WS)) {
            relay = new Relay(Relay.PROTOCOL_WS, relayUri.substring(Relay.PROTOCOL_WS.length() + 3));
        } else {
            throw new RuntimeException("Invalid relay protocol");
        }
        return relay;
    }

    private static Map<String, String> parseQueryParameters(String query) {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] parts = pair.split("=");
                if (parts.length == 2) {
                    queryParams.put(parts[0], parts[1]);
                }
            }
        }
        return queryParams;
    }

    private String getJsonMetadata() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting metadata to JSON", e);
        }
    }
}