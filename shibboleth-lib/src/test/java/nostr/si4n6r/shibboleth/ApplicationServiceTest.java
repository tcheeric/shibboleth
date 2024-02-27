package nostr.si4n6r.shibboleth;

import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.id.Identity;
import nostr.si4n6r.model.dto.MethodDto;
import nostr.si4n6r.model.dto.ParameterDto;
import nostr.si4n6r.model.dto.RequestDto;
import nostr.si4n6r.model.dto.SessionDto;
import nostr.si4n6r.rest.client.MethodRestClient;
import nostr.si4n6r.rest.client.ParameterRestClient;
import nostr.si4n6r.rest.client.SessionManager;
import nostr.si4n6r.rest.client.SessionRestClient;
import nostr.si4n6r.signer.SignerService;
import nostr.si4n6r.storage.Vault;
import nostr.si4n6r.storage.common.AccountProxy;
import nostr.si4n6r.storage.common.ApplicationProxy;
import nostr.si4n6r.storage.fs.NostrAccountFSVault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.logging.Level;

import static nostr.si4n6r.core.impl.BaseActorProxy.VAULT_ACTOR_ACCOUNT;
import static nostr.si4n6r.core.impl.BaseActorProxy.VAULT_ACTOR_APPLICATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Log
public class ApplicationServiceTest {

    private SignerService signerService;
    private AppService appService;
    private Vault<AccountProxy> accountVault;
    private Vault<ApplicationProxy> appVault;
    private AccountProxy accountProxy;
    private ApplicationProxy applicationProxy;

    private Application application;

    private static final String PASSWORD = "password";

    private SessionDto session;

    @BeforeEach
    public void init() {
        System.out.println("init");

        this.signerService = SignerService.getInstance();
        this.application = Application.getInstance(Identity.generateRandomIdentity());

        var appPublicKey = application.getPublicKey();
        var signer = Identity.generateRandomIdentity().getPublicKey();

        this.appService = AppService.getInstance(application, signer);
        this.session = SessionManager.getInstance().createSession(
                Identity.generateRandomIdentity().getPublicKey().toString(),
                appPublicKey.toString(),
                20 * 60,
                PASSWORD);

        this.appService.setJwtToken(session.getToken());

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

        var app = this.application.getPublicKey();
        var request = new RequestDto();
        request.setSession(session);
        request.setToken(session.getToken());
        request.setInitiator(app.toString());
        request.setMethod(getMethod(MethodDto.MethodType.CONNECT.getName()));

        var response = this.signerService.handle(request);
        assertEquals(request.getRequestUuid(), response.getResponseUuid());

        assertEquals("ACK", getResult(response.getResult()).getValue());

        this.appService.handle(response);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Test a disconnect response")
    public void handleDisconnect() {
        System.out.println("handleDisconnect");

        var app = this.application.getPublicKey();

        var request = new RequestDto();
        request.setSession(session);
        request.setToken(session.getToken());
        request.setInitiator(app.toString());
        request.setMethod(getMethod(MethodDto.MethodType.CONNECT.getName()));

        var response = this.signerService.handle(request);
        assertEquals(request.getRequestUuid(), response.getResponseUuid());

        assertEquals("ACK", getResult(response.getResult()).getValue());

        var request1 = new RequestDto();
        request1.setSession(session);
        request1.setToken(session.getToken());
        request1.setInitiator(app.toString());
        request1.setMethod(getMethod(MethodDto.MethodType.DISCONNECT.getName()));

        var response1 = this.signerService.handle(request1);
        assertEquals(request1.getRequestUuid(), response1.getResponseUuid());

        assertEquals("ACK", getResult(response1.getResult()).getValue());
        assertNotNull(response1);

        this.appService.handle(response1);
    }

    @Test
    @DisplayName("Test a describe response")
    public void describe() {
        System.out.println("describe");
        var app = this.application.getPublicKey();

        var request = new RequestDto();
        request.setSession(session);
        request.setToken(session.getToken());
        request.setInitiator(app.toString());
        request.setMethod(getMethod(MethodDto.MethodType.CONNECT.getName()));

        var response = this.signerService.handle(request);

        assertEquals("ACK", getResult(response.getResult()).getValue());
        assertEquals(request.getRequestUuid(), response.getResponseUuid());

        request = new RequestDto();
        request.setSession(session);
        request.setToken(session.getToken());
        request.setInitiator(app.toString());
        request.setMethod(getMethod(MethodDto.MethodType.DESCRIBE.getName()));

        response = this.signerService.handle(request);

        assertNotNull(response);
        assertEquals(request.getRequestUuid(), response.getResponseUuid());

        assertEquals(3, getResult(response.getResult()).getValue().split(",").length);

        this.appService.handle(response);
    }

    @Test
    @DisplayName("Test invalid Principal Password")
    public void invalidPrincipalPassword() {
        System.out.println("invalidPrincipalPassword");
    }

    private void createAppProxy() {
        var app = this.application.getPublicKey();
        this.applicationProxy = new ApplicationProxy(app.toString());
        this.applicationProxy.setId(String.valueOf(System.currentTimeMillis()));
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
        accountProxy.setId(String.valueOf(System.currentTimeMillis()));
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

    private MethodDto getMethod(@NonNull String name) {
        MethodRestClient methodRestClient = new MethodRestClient();
        return methodRestClient.getMethodByName(name);
    }

    private SignerService.Result getResult(@NonNull String resultJson) {
        return SignerService.Result.fromJson(resultJson);
    }

    @AfterEach
    public void cleanUp() {
        System.out.println("cleanUp");
        this.application = null;
    }

}
