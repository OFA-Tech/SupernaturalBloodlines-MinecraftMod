package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class PayloadCapsTest {
    private static JsonElement body(int chars) {
        return JsonParser.parseString("{\"x\":\"" + "a".repeat(chars) + "\"}");
    }

    @Test
    void aFileWithinTheCapsIsAccepted() {
        assertNull(new PayloadCaps().check("s-vam-1", body(100), 64, 1 << 16));
    }

    @Test
    void anOversizedBodyOrNameIsRefusedWithAReason() {
        PayloadCaps caps = new PayloadCaps();
        String big = caps.check("s", body(70_000), 64, 1 << 16);
        assertNotNull(big);
        assertEquals(true, big.contains("limit is 65536"), big);
        assertNotNull(caps.check("n".repeat(65), body(1), 64, 1 << 16));
        assertNotNull(caps.check("é".repeat(33), body(1), 64, 1 << 16), "the name is counted in UTF-8 bytes, as the codec does");
        assertNull(caps.check("ok", body(1), 64, 1 << 16), "a refused file used up the budget");
    }

    @Test
    void theTotalIsKeptUnderThePacketLimit() {
        PayloadCaps caps = new PayloadCaps();
        int accepted = 0;
        for (int i = 0; i < 20; i++) {
            accepted += caps.check("f" + i, body(60_000), 64, 1 << 16) == null ? 1 : 0;
        }
        assertEquals(14, accepted, "900,000 bytes hold fourteen 60,000 byte files");
    }
}