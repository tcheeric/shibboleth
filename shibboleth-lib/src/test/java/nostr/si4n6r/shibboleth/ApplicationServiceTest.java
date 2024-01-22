package nostr.si4n6r.shibboleth;

import lombok.NonNull;
import nostr.base.PublicKey;
import nostr.id.Identity;
import nostr.si4n6r.core.impl.*;
import nostr.si4n6r.signer.SignerService;
import nostr.si4n6r.signer.methods.Connect;
import nostr.si4n6r.signer.methods.Describe;
import nostr.si4n6r.signer.methods.Disconnect;
import nostr.si4n6r.storage.Vault;
import nostr.si4n6r.storage.fs.NostrAccountFSVault;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.ServiceLoader;

import static nostr.si4n6r.core.IMethod.Constants.*;
import static nostr.si4n6r.core.impl.BaseActorProxy.VAULT_ACTOR_ACCOUNT;
import static nostr.si4n6r.core.impl.BaseActorProxy.VAULT_ACTOR_APPLICATION;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplicationServiceTest {

    private SignerService signerService;
    private AppService appService;
    private Vault<AccountProxy> accountVault;
    private Vault<ApplicationProxy> appVault;
    private AccountProxy accountProxy;
    private ApplicationProxy applicationProxy;

    private static final String PASSWORD = "password";

    private Session session;

    @BeforeEach
    public void init() {
        System.out.println("init");

        this.signerService = SignerService.getInstance();
        var application = Application.getInstance();
        var signer = Identity.generateRandomIdentity().getPublicKey();
        this.appService = AppService.getInstance(application, signer);
        final var appPublicKey = appService.getApplication().getPublicKey();
        this.session = SessionManager.getInstance().createSession(Identity.generateRandomIdentity().getPublicKey(), appPublicKey, 20 * 60, PASSWORD, "secret");
        this.appService.setJwtToken(session.getJwtToken());

        createAppProxy();
        createAccountProxy(this.applicationProxy);

        this.accountVault = getAccountVault(PASSWORD);
        this.appVault = getApplicationVault();
        this.appVault.store(applicationProxy);
        this.accountVault.store(accountProxy);
    }

    @Test
    @DisplayName("Test a connect response")
    public void handleConnect() {
        System.out.println("handleConnect");

        var app = this.appService.getApplication().getPublicKey();
        var request = new Request<>(new Connect(app), session.getJwtToken());
        request.setInitiator(applicationProxy);
        this.signerService.handle(request);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());

        this.appService.handle(response);
        responses = this.signerService.getSessionManager().getSession(app).getResponses();
        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());
        assertEquals(request.getId(), response.getId());
    }

    @Test
    @DisplayName("Test a disconnect response")
    public void handleDisconnect() {
        System.out.println("handleDisconnect");

        var app = this.appService.getApplication().getPublicKey();
        var request = new Request<>(new Connect(app), session.getJwtToken());
        request.setInitiator(applicationProxy);
        this.signerService.handle(request);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);

        request = new Request<>(new Disconnect(new PublicKey(applicationProxy.getPublicKey())), session.getJwtToken());
        request.setInitiator(applicationProxy);
        this.signerService.handle(request);
        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_DISCONNECT)).findFirst().orElse(null);

        assertNotNull(response);

        this.appService.handle(response);
        assertEquals(request.getId(), response.getId());
        assertEquals("ACK", response.getResult());
    }

    @Test
    @DisplayName("Test a describe response")
    public void describe() {
        System.out.println("describe");
        var app = this.appService.getApplication().getPublicKey();

        var connectReq = new Request<>(new Connect(app), session.getJwtToken());
        connectReq.setInitiator(applicationProxy);
        this.signerService.handle(connectReq);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);

        var describeReq = new Request<>(new Describe(), session.getJwtToken());
        describeReq.setInitiator(applicationProxy);
        describeReq.setJwt(session.getJwtToken());
        this.signerService.handle(describeReq);

        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_DESCRIBE)).findFirst().orElse(null);
        assertNotNull(response);
        assertTrue(response.getResult() instanceof List);
        assertEquals(describeReq.getId(), response.getId());
        assertEquals(3, ((List) response.getResult()).size());
        assertTrue(((List) response.getResult()).contains(METHOD_CONNECT));
        assertTrue(((List) response.getResult()).contains(METHOD_DISCONNECT));
        assertTrue(((List) response.getResult()).contains(METHOD_DESCRIBE));

        this.appService.handle(response);
        assertNotNull(this.signerService.getSessionManager().getSession(app));
    }

    @Test
    @DisplayName("Test invalid Principal Password")
    public void invalidPrincipalPassword() {
        System.out.println("invalidPrincipalPassword");
    }

    private void createAppProxy() {
        var app = this.appService.getApplication().getPublicKey();
        this.applicationProxy = new ApplicationProxy(app.toString());
        this.applicationProxy.setId(System.currentTimeMillis());
        this.applicationProxy.setName("shibboleth");

        var template = applicationProxy.getTemplate();
        template.setUrl("https://nostr.com");
        template.setDescription("A nip-46 compliant nostr application");
        template.setIcons(List.of("https://nostr.com/favicon.ico", "https://nostr.com/favicon.png"));
    }

    private void createAccountProxy(@NonNull ApplicationProxy application) {
        var identity = Identity.generateRandomIdentity();
        accountProxy = new AccountProxy();
        accountProxy.setPublicKey(identity.getPublicKey().toString());
        accountProxy.setPrivateKey(identity.getPrivateKey().toString());
        accountProxy.setId(System.currentTimeMillis());
        accountProxy.setApplication(application);
    }

    private static Vault<AccountProxy> getAccountVault(@NonNull String password) {
        var vault = getVault(VAULT_ACTOR_ACCOUNT);
        ((NostrAccountFSVault) vault).setPassword(password);
        return vault;
    }

    private static Vault<ApplicationProxy> getApplicationVault() {
        return getVault(VAULT_ACTOR_APPLICATION);
    }

    private static Vault getVault(@NonNull String entity) {
        return ServiceLoader
                .load(Vault.class)
                .stream()
                .map(p -> p.get())
                .filter(v -> entity.equals(v.getEntityName()))
                .findFirst()
                .get();
    }

    @AfterEach
    public void cleanUp() {
        System.out.println("cleanUp");
        this.signerService.getSessionManager().getSessions().stream().forEach(s -> s.getRequests().clear());
        this.signerService.getSessionManager().getSessions().stream().forEach(s -> s.getResponses().clear());
        this.signerService.getSessionManager().getSessions().clear();
        this.appService.getRequests().clear();
        this.appService.getResponses().clear();
    }

}
