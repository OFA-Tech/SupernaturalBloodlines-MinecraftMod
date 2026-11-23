package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import org.junit.jupiter.api.Test;

class BossBarInfoPayloadTest {
    private static final UUID BAR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Test
    void everyBossAndEveryFormSurvivesTheWire() {
        for (BossId boss : BossId.values()) {
            for (int active = 0; active < 3; active++) {
                Look look = BossBarForms.checked(boss, BossBarForms.setOf(boss).ordinal(), active);
                BossBarInfoPayload sent = BossBarInfoPayload.of(BAR, boss, look, active == 1, 1640, 2000, 77);
                ByteBuf buf = Unpooled.buffer();
                BossBarInfoPayload.STREAM_CODEC.encode(buf, sent);
                BossBarInfoPayload got = BossBarInfoPayload.STREAM_CODEC.decode(buf);
                assertEquals(Optional.of(boss), got.boss(), boss.toString());
                assertEquals(look, got.look(), boss + " " + active);
                assertEquals(active == 1, got.fallen());
                assertEquals(BAR, got.bar());
                assertEquals(1640, got.hp());
                assertEquals(2000, got.max());
                assertEquals(77, got.entity(), "the entity id the client measures the distance to");
            }
        }
    }

    @Test
    void theFallenFlagAndTheLitChipDoNotShareABit() {
        BossBarInfoPayload fallenHuman = BossBarInfoPayload.of(BAR, BossId.KLAUS, BossBarForms.shifter(BossForm.HUMAN), true, 5, 10, 3);
        assertTrue(fallenHuman.fallen());
        assertEquals(0, fallenHuman.look().active());
        BossBarInfoPayload standingBeast = BossBarInfoPayload.of(BAR, BossId.KLAUS, BossBarForms.shifter(BossForm.BEAST), false, 5, 10, 3);
        assertFalse(standingBeast.fallen());
        assertEquals(2, standingBeast.look().active());
    }

    @Test
    void anEntityIdBelowMinusOneIsHeldAtMinusOneAndAnIdSurvivesTheWire() {
        assertEquals(BossBarInfoPayload.NO_ENTITY, new BossBarInfoPayload(BAR, 1, 1, 1, -900).entity());
        ByteBuf buf = Unpooled.buffer();
        BossBarInfoPayload.STREAM_CODEC.encode(buf, new BossBarInfoPayload(BAR, 1, 1, 1, 123456));
        assertEquals(123456, BossBarInfoPayload.STREAM_CODEC.decode(buf).entity());
    }

    @Test
    void anUnknownBossIsNoBossAndNoChips() {
        BossBarInfoPayload junk = new BossBarInfoPayload(BAR, 0xFF | 1 << 8 | 1 << 12, 1, 1, 0);
        assertEquals(Optional.empty(), junk.boss());
        assertEquals(Look.NONE, junk.look());
        assertEquals(Optional.empty(), new BossBarInfoPayload(BAR, 0, 1, 1, 0).boss(), "0 is no boss");
    }

    @Test
    void healthOutOfRangeIsHeldSoAForgedPayloadCannotAskForAHugeNumber() {
        BossBarInfoPayload huge = new BossBarInfoPayload(BAR, 1, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        assertEquals(BossBarInfoPayload.LIMIT, huge.hp());
        assertEquals(BossBarInfoPayload.LIMIT, huge.max());
        BossBarInfoPayload negative = new BossBarInfoPayload(BAR, 1, -50, 0, 0);
        assertEquals(0, negative.hp());
        assertEquals(1, negative.max(), "a maximum of 0 would divide by zero");
    }
}
