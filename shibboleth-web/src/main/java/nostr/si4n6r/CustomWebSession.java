package nostr.si4n6r;

import jakarta.servlet.http.HttpSession;
import nostr.si4n6r.rest.client.SessionManager;
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
        var sessionManager = SessionManager.getInstance();
        sessionManager.deactivateSession(npub.toString());
    }

    public void setSessionTimeout(int timeoutInMinutes) {
        HttpSession httpSession = (HttpSession) getAttribute("session");
        if (httpSession != null) {
            httpSession.setMaxInactiveInterval(timeoutInMinutes * 60);
        }
    }
}