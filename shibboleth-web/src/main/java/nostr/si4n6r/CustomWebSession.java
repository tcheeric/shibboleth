package nostr.si4n6r;

import nostr.base.PublicKey;
import nostr.si4n6r.core.impl.SessionManager;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;


public class CustomWebSession extends WebSession {
    public CustomWebSession(Request request) {
        super(request);
    }

    @Override
    public void onInvalidate() {
        super.onInvalidate();

        var npub = getAttribute("npub");
        if (npub == null) {
            return;
        }
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.invalidate(new PublicKey(npub.toString()));
    }
}