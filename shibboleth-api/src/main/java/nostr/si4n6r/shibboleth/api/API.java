package nostr.si4n6r.shibboleth.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;
import nostr.base.PublicKey;
import nostr.si4n6r.model.dto.MethodDto;
import nostr.si4n6r.model.dto.RequestDto;
import nostr.si4n6r.rest.client.MethodRestClient;
import nostr.si4n6r.rest.client.SessionManager;
import nostr.si4n6r.signer.SignerService;

@Log
@AllArgsConstructor
@Data
public class API {

    private final PublicKey signer;
    private final PublicKey app;
    private final String jwt;
    private final MethodRestClient methodRestClient;

    public API(PublicKey signer, PublicKey app, String jwt) {
        this.signer = signer;
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

    private String processRequest(@NonNull String methodName) {
        var request = new RequestDto();
        request.setInitiator(app.toString());
        request.setToken(jwt);
        request.setSession(SessionManager.getInstance().getSession(app.toString()));
        request.setMethod(getMethod(methodName));
        var signerService = SignerService.getInstance();

        var response = signerService.handle(request);
        return response.getResult();
    }

    private MethodDto getMethod(@NonNull String name) {
        return methodRestClient.getMethodByName(name);
    }
}