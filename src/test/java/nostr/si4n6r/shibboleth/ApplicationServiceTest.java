package nostr.si4n6r.shibboleth;

import nostr.id.Identity;
import nostr.si4n6r.core.impl.Request;
import nostr.si4n6r.core.impl.SessionManager;
import nostr.si4n6r.core.impl.methods.Connect;
import nostr.si4n6r.signer.SignerService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static nostr.si4n6r.core.IMethod.Constants.METHOD_CONNECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)

public class ApplicationServiceTest {

    private SignerService signerService;
    private AppService appService;

    @BeforeAll
    public void setUp() {
        var application = Application.getInstance();
        var signer = Identity.generateRandomIdentity().getPublicKey();

        this.signerService = SignerService.getInstance();
        this.appService = AppService.getInstance(application, signer);
    }
    @Test
    @DisplayName("Test a connect response")
    public void handleConnect() {
        var app = Identity.generateRandomIdentity().getPublicKey();
        var request = new Request(new Connect(app), app);
        this.signerService.handle(request);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());
        assertEquals(appService.getSessionId(), response.getSessionId());
        assertEquals(request.getSessionId(), response.getSessionId());

        this.appService.handle(response);
        assertEquals(appService.getSessionId(), response.getSessionId());
        responses = this.appService.getResponses();
        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());
        assertEquals(appService.getSessionId(), response.getSessionId());
        assertEquals(request.getSessionId(), response.getSessionId());
    }
}
