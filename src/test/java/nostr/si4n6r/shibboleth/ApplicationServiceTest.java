package nostr.si4n6r.shibboleth;

import nostr.id.Identity;
import nostr.si4n6r.core.IMethod;
import nostr.si4n6r.core.impl.Request;
import nostr.si4n6r.core.impl.SecurityManager;
import nostr.si4n6r.core.impl.SessionManager;
import nostr.si4n6r.signer.SignerService;
import nostr.si4n6r.signer.methods.Connect;
import nostr.si4n6r.signer.methods.Describe;
import nostr.si4n6r.signer.methods.Disconnect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static nostr.si4n6r.core.IMethod.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

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
    public void handleConnect() throws SecurityManager.SecurityManagerException {
        var app = this.appService.getApplication().getPublicKey();
        var request = new Request<>(new Connect(app), app);
        this.signerService.handle(request);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());
        assertEquals(request.getSessionId(), response.getSessionId());

        this.appService.handle(response);
        assertEquals(appService.getSessionId(), response.getSessionId());
        responses = this.signerService.getSessionManager().getSession(app).getResponses();
        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());
        assertEquals(appService.getSessionId(), response.getSessionId());
        assertEquals(request.getSessionId(), response.getSessionId());
    }

    @Test
    @DisplayName("Test a disconnect response")
    public void handleDisconnect() throws SecurityManager.SecurityManagerException {
        var app = this.appService.getApplication().getPublicKey();

        var request = new Request<>(new Connect(app), app);
        this.signerService.handle(request);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);

        request = new Request<>(new Disconnect(), app);
        request.setSessionId(response.getSessionId());
        this.signerService.handle(request);

        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_DISCONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());

        this.appService.handle(response);
        assertEquals(appService.getSessionId(), response.getSessionId());
        assertEquals(request.getSessionId(), response.getSessionId());
        assertThrows(RuntimeException.class, () -> this.signerService.getSessionManager().getSession(app));
    }

    @Test
    @DisplayName("Test a describe response")
    public void describe() throws SecurityManager.SecurityManagerException {
        var app = this.appService.getApplication().getPublicKey();

        var connectReq = new Request<>(new Connect(app), app);
        this.signerService.handle(connectReq);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);

        var describeReq = new Request<List<String>>(new Describe(), app);
        describeReq.setSessionId(response.getSessionId());
        this.signerService.handle(describeReq);

        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_DESCRIBE)).findFirst().orElse(null);
        assertNotNull(response);
        assertTrue(response.getResult() instanceof List);
        assertEquals(3, ((List) response.getResult()).size());
        assertTrue(((List) response.getResult()).contains(METHOD_CONNECT));
        assertTrue(((List) response.getResult()).contains(METHOD_DISCONNECT));
        assertTrue(((List) response.getResult()).contains(IMethod.Constants.METHOD_DESCRIBE));

        this.appService.handle(response);
        assertEquals(appService.getSessionId(), response.getSessionId());
        assertEquals(connectReq.getSessionId(), response.getSessionId());
        assertNotNull(this.signerService.getSessionManager().getSession(app));
    }
}
