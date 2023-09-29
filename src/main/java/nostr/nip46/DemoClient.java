package nostr.nip46;

import lombok.extern.java.Log;
import nostr.base.PublicKey;
import nostr.nip46.app.AppService;
import nostr.nip46.app.Application;
import nostr.nip46.app.NostrUser;

@Log
public class DemoClient {

    public static void main(String[] args) {
        var app = Application.getInstance(NostrUser.DUMMY_USER);

        // TODO - Put in config file
        var signer = new PublicKey("245cabdd7e7e78f417a0d9e7f157cfda8bcda2d13ad9336603dcea45a3c07e58");

        var service = AppService.getInstance(app, signer);

        log.info("Connecting to the signer...");
        service.connect();
    }
}
