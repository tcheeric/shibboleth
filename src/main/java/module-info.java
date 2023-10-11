import nostr.si4n6r.shibboleth.provider.AppCommandHandler;

module shibboleth {

    requires com.fasterxml.jackson.databind;

    requires lombok;
    requires java.logging;

    requires nostr.base;
    requires nostr.api;
    requires nostr.id;
    requires nostr.client;
    requires nostr.crypto;
    requires nostr.ws.handler;
    requires nostr.event;
    requires nostr.util;

    requires si4n6r.core;
    requires si4n6r.util;

    exports nostr.si4n6r.shibboleth;
    exports nostr.si4n6r.shibboleth.provider;

    provides nostr.ws.handler.command.spi.ICommandHandler with AppCommandHandler;
}