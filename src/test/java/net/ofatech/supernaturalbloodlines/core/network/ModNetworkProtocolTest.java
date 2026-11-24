package net.ofatech.supernaturalbloodlines.core.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * The version of the required payloads is what keeps a client of another build out: NeoForge refuses it at login with the version mismatch screen. The game's
 * entity data ids are positional, so a client that is given an id it does not have reads past its table and is kicked with an error nobody can read. Whoever
 * adds or removes a synced field of an entity bumps the number, in {@link ModNetwork#PROTOCOL} and here.
 */
class ModNetworkProtocolTest {
    @Test
    void theVersionWasBumpedWhenTheBeastGotTwoMoreSyncedFields() {
        assertEquals("2", ModNetwork.PROTOCOL);
        assertNotEquals("1", ModNetwork.PROTOCOL, "an old client would be let in and kicked by the Beast's entity data");
    }
}