module shibboleth {
    requires nostr.base;
    requires si4n6r.core;
    requires lombok;
    requires nostr.api;
    requires java.logging;
    requires nostr.id;
    requires nostr.client;
    requires nostr.crypto;
    requires nostr.ws.handler;
    requires si4n6r.util;
    requires nostr.event;
    requires nostr.util;

    provides nostr.ws.handler.command.spi.ICommandHandler with nostr.nip46.app.provider.AppCommandHandler;
}