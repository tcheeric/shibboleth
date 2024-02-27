package nostr.si4n6r.shibboleth.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.base.PrivateKey;
import nostr.base.PublicKey;
import nostr.event.json.codec.BaseEventEncoder;
import nostr.event.json.codec.GenericEventDecoder;
import nostr.id.Identity;
import nostr.si4n6r.model.dto.MethodDto;
import nostr.si4n6r.model.dto.ParameterDto;
import nostr.si4n6r.model.dto.RequestDto;
import nostr.si4n6r.rest.client.MethodRestClient;
import nostr.si4n6r.rest.client.ParameterRestClient;
import nostr.si4n6r.rest.client.RequestRestClient;
import nostr.si4n6r.rest.client.SessionManager;
import nostr.si4n6r.signer.SignerService;
import nostr.si4n6r.storage.Vault;
import nostr.si4n6r.storage.common.AccountProxy;
import nostr.si4n6r.storage.fs.NostrAccountFSVault;
import nostr.si4n6r.util.JWTUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;

@Log
@AllArgsConstructor
@Data
public class API {

    private final PublicKey app;
    private final String jwt;
    private final MethodRestClient methodRestClient;

    // TODO - Signer is not used in this method
    public API(PublicKey signer, PublicKey app, String jwt) {
        this.app = app;
        this.jwt = jwt;
        this.methodRestClient = new MethodRestClient();
    }

    public String connect() {
        return processRequest(MethodDto.MethodType.CONNECT.getName());
    }

    public String disconnect() {
        return processRequest(MethodDto.MethodType.DISCONNECT.getName());
    }

    public String describe() {
        return processRequest(MethodDto.MethodType.DESCRIBE.getName());
    }

    public String sign(@NonNull byte[] data) {
        return processRequest(MethodDto.MethodType.SIGN_EVENT.getName(), data);
    }

    public String get_public_key() {
        return processRequest(MethodDto.MethodType.GET_PUBLIC_KEY.getName());
    }

    private String processRequest(@NonNull String methodName) {
        return processRequest(methodName, null);
    }

    private String processRequest(@NonNull String methodName, byte[] data) {
        log.log(Level.INFO, "Processing request for method {0}", methodName);
        var request = new RequestDto();
        request.setInitiator(app.toString());
        request.setToken(jwt);
        request.setSession(SessionManager.getInstance().getSession(app.toString())); // TODO - Check if the session has expired and exit if it has
        request.setMethod(getMethod(methodName));

        var requestRestClient = new RequestRestClient();
        var req = requestRestClient.create(request);

        if (methodName.equals(MethodDto.MethodType.SIGN_EVENT.getName())) {
            var param = new ParameterDto();
            param.setName(ParameterDto.PARAM_EVENT);
            param.setValue(getSignedEvent(data));
            param.setRequest(req);

            var paramRestClient = new ParameterRestClient();
            paramRestClient.create(param);
        }

        var signerService = SignerService.getInstance();

        var response = signerService.handle(req);
        return response.getResult();
    }

    private MethodDto getMethod(@NonNull String name) {
        return methodRestClient.getMethodByName(name);
    }

    private String getSignedEvent(@NonNull byte[] data) {

        var strEvent = new String(data, StandardCharsets.UTF_8);
        var event = new GenericEventDecoder(strEvent).decode();
        var jwtUtil = new JWTUtil(this.jwt);
        var account = jwtUtil.getSubject();
        var password = jwtUtil.getPassword();

        log.log(Level.INFO, "Account {0} - Password: {1}", new Object[]{account, password});

        event.setPubKey(new PublicKey(account));
        log.log(Level.INFO, "Event: {0}", event);
        var bottin = new nostr.si4n6r.API();
        var nip05 = bottin.getNip05(account);

        Vault<AccountProxy> vault = new NostrAccountFSVault();
        var accountProxy = new AccountProxy();
        accountProxy.setId(nip05);
        accountProxy.setPublicKey(account);

        var privateKey = new PrivateKey(vault.retrieve(accountProxy, password));
        var identity = Identity.getInstance(privateKey);
        identity.sign(event);

        BaseEventEncoder encoder = new BaseEventEncoder(event);
        return Base64.getEncoder().encodeToString(encoder.encode().getBytes(StandardCharsets.UTF_8));
    }

}