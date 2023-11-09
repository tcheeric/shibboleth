package nostr.si4n6r.shibboleth;

import nostr.id.Identity;
import nostr.si4n6r.core.impl.Request;
import nostr.si4n6r.core.impl.SessionManager;
import nostr.si4n6r.core.impl.SecurityManager;
import nostr.si4n6r.core.impl.SecurityManager.SecurityManagerException;
import nostr.si4n6r.signer.SignerService;
import nostr.si4n6r.signer.methods.Connect;
import nostr.si4n6r.signer.methods.Describe;
import nostr.si4n6r.signer.methods.Disconnect;
import nostr.si4n6r.storage.Vault;
import nostr.si4n6r.core.impl.AccountProxy;
import nostr.si4n6r.core.impl.ApplicationProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.ServiceLoader;
import lombok.NonNull;
import nostr.base.PublicKey;

import static nostr.si4n6r.core.IMethod.Constants.METHOD_CONNECT;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_DESCRIBE;
import static nostr.si4n6r.core.IMethod.Constants.METHOD_DISCONNECT;
import nostr.si4n6r.core.impl.Principal;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import static nostr.si4n6r.core.impl.BaseActorProxy.VAULT_ACTOR_ACCOUNT;
import static nostr.si4n6r.core.impl.BaseActorProxy.VAULT_ACTOR_APPLICATION;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplicationServiceTest {

    private SignerService signerService;
    private AppService appService;
    private Vault<AccountProxy> accountVault;
    private Vault<ApplicationProxy> appVault;
    private AccountProxy accountProxy;
    private ApplicationProxy applicationProxy;

    @BeforeAll
    public void init() {
        System.out.println("init");
        this.signerService = SignerService.getInstance();
        var app = Application.getInstance();
        var signer = Identity.generateRandomIdentity().getPublicKey();
        this.appService = AppService.getInstance(app, signer);

        createApplication();
        createAccount(this.applicationProxy);

        this.accountVault = getAccountVault();
        this.appVault = getApplicationVault();

        this.appVault.store(applicationProxy);

        final var appPublicKey = new PublicKey(applicationProxy.getPublicKey());
        final var principal = Principal.getInstance(appPublicKey, "password");
        SecurityManager.getInstance().addPrincipal(principal);

        this.accountVault.store(accountProxy);
    }

    @BeforeEach
    public void setUp() {
        System.out.println("setUp");
    }

    @Test
    @DisplayName("Test a connect response")
    public void handleConnect() throws SecurityManagerException {
        System.out.println("handleConnect");
        var app = this.appService.getApplication().getPublicKey();

        var request = new Request<>(new Connect(app), applicationProxy);

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
    public void handleDisconnect() throws SecurityManagerException {
        System.out.println("handleDisconnect");
        var app = this.appService.getApplication().getPublicKey();

        SecurityManager.getInstance().addPrincipal(Principal.getInstance(app, "password"));

        var request = new Request<>(new Connect(app), applicationProxy);
        this.signerService.handle(request);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);

        request = new Request<>(new Disconnect(), applicationProxy);
        request.setSessionId(response.getSessionId());
        this.signerService.handle(request);

        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_DISCONNECT)).findFirst().orElse(null);
        assertNotNull(response);
        assertEquals("ACK", response.getResult());

        this.appService.handle(response);
        assertEquals(appService.getSessionId(), response.getSessionId());
        assertEquals(request.getSessionId(), response.getSessionId());
    }

    @Test
    @DisplayName("Test a describe response")
    public void describe() throws SecurityManagerException {
        System.out.println("describe");
        var app = this.appService.getApplication().getPublicKey();

        SecurityManager.getInstance().addPrincipal(Principal.getInstance(app, "password"));

        var connectReq = new Request<>(new Connect(app), applicationProxy);
        this.signerService.handle(connectReq);

        var responses = SessionManager.getInstance().getSession(app).getResponses();
        var response = responses.stream().filter(r -> r.getMethod().equals(METHOD_CONNECT)).findFirst().orElse(null);

        var describeReq = new Request<List<String>, ApplicationProxy>(new Describe(), applicationProxy);
        describeReq.setSessionId(response.getSessionId());
        this.signerService.handle(describeReq);

        response = responses.stream().filter(r -> r.getMethod().equals(METHOD_DESCRIBE)).findFirst().orElse(null);
        assertNotNull(response);
        assertTrue(response.getResult() instanceof List);
        assertEquals(3, ((List) response.getResult()).size());
        assertTrue(((List) response.getResult()).contains(METHOD_CONNECT));
        assertTrue(((List) response.getResult()).contains(METHOD_DISCONNECT));
        assertTrue(((List) response.getResult()).contains(METHOD_DESCRIBE));

        this.appService.handle(response);
        assertEquals(appService.getSessionId(), response.getSessionId());
        assertEquals(connectReq.getSessionId(), response.getSessionId());
        assertNotNull(this.signerService.getSessionManager().getSession(app));
    }

    @Test
    @DisplayName("Test invalid Principal Password")
    public void invalidPrincipalPassword() {
        System.out.println("invalidPrincipalPassword");
        var app = this.appService.getApplication().getPublicKey();

        var addFlag = SecurityManager.getInstance().hasPrincipal(app, "password");
        assertTrue(addFlag);

        // TODO - Validate the password separately, by decrypting the nsec.
//        SecurityManager.getInstance().removePrincipal(app);
//        addFlag = SecurityManager.getInstance().addPrincipal(Principal.getInstance(app, "password1"));
//        assertFalse(addFlag);
    }

    private void createApplication() {
        PublicKey app = this.appService.getApplication().getPublicKey();
        applicationProxy = new ApplicationProxy();
        applicationProxy.setPublicKey(app.toString());
        applicationProxy.setId(System.currentTimeMillis());
        applicationProxy.setName("shibboleth");
        applicationProxy.setUrl("https://nostr.com");
        applicationProxy.setDescription("A nip-46 compliant nostr application");
        applicationProxy.setIcons(List.of("https://nostr.com/favicon.ico", "https://nostr.com/favicon.png"));
    }

    private void createAccount(@NonNull ApplicationProxy application) {
        var identity = Identity.generateRandomIdentity();
        accountProxy = new AccountProxy();
        accountProxy.setPublicKey(identity.getPublicKey().toString());
        accountProxy.setPrivateKey(identity.getPrivateKey().toString());
        accountProxy.setId(System.currentTimeMillis());
        accountProxy.setApplication(application);
    }

    private static Vault<AccountProxy> getAccountVault() {
        return getVault(VAULT_ACTOR_ACCOUNT);
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
