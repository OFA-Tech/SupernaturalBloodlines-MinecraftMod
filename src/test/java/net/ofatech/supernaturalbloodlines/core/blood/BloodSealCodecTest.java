package net.ofatech.supernaturalbloodlines.core.blood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BloodSealCodecTest {
    private static final byte[] KEY = BloodSealCodec.newKey();

    @Test
    void aSealOpensToTheSameSource() {
        for (BloodKind k : BloodKind.values()) {
            BloodSource s = new BloodSource(UUID.randomUUID(), "Elena Gilbert", k.ordinal() % 2 == 0, k);
            assertEquals(Optional.of(s), BloodSealCodec.open(KEY, BloodSealCodec.seal(KEY, s)), k.name());
        }
    }

    @Test
    void aSealHoldsNoReadableSecret() {
        UUID id = UUID.randomUUID();
        String blob = BloodSealCodec.seal(KEY, new BloodSource(id, "Elena Gilbert", false, BloodKind.ORIGINAL));
        byte[] raw = Base64.getDecoder().decode(blob);
        assertFalse(new String(raw, StandardCharsets.ISO_8859_1).contains("Elena"));
        assertFalse(blob.contains(id.toString()) || blob.contains("ORIGINAL"));
    }

    @Test
    void everySealIsTheSameLengthWhateverItHolds() {
        int len = BloodSealCodec.seal(KEY, new BloodSource(UUID.randomUUID(), "", true, BloodKind.COMMON_VAMPIRE)).length();
        assertEquals(len, BloodSealCodec.seal(KEY, new BloodSource(UUID.randomUUID(), "Bonnie", false, BloodKind.ORIGINAL)).length());
        assertEquals(len, BloodSealCodec.seal(KEY, new BloodSource(UUID.randomUUID(), "x".repeat(500), false, BloodKind.TRIBRID)).length());
    }

    @Test
    void twoSealsOfTheSameSourceDiffer() {
        BloodSource s = new BloodSource(UUID.randomUUID(), "Bonnie", false, BloodKind.HYBRID);
        assertNotEquals(BloodSealCodec.seal(KEY, s), BloodSealCodec.seal(KEY, s));
    }

    @Test
    void aTamperedSealOpensToNothing() {
        byte[] raw = Base64.getDecoder().decode(BloodSealCodec.seal(KEY, new BloodSource(UUID.randomUUID(), "Bonnie", false, BloodKind.ORIGINAL)));
        for (int i : new int[] {0, 12, raw.length / 2, raw.length - 1}) {
            byte[] bad = raw.clone();
            bad[i] ^= 1;
            assertTrue(BloodSealCodec.open(KEY, Base64.getEncoder().encodeToString(bad)).isEmpty(), "byte " + i);
        }
        assertTrue(BloodSealCodec.open(KEY, "not a seal").isEmpty());
        assertTrue(BloodSealCodec.open(KEY, "").isEmpty());
    }

    @Test
    void aSealOfAnotherWorldOpensToNothing() {
        String blob = BloodSealCodec.seal(KEY, new BloodSource(UUID.randomUUID(), "Bonnie", false, BloodKind.ORIGINAL));
        assertTrue(BloodSealCodec.open(BloodSealCodec.newKey(), blob).isEmpty());
    }

    @Test
    void aLongNameIsCutWithoutSplittingACharacter() {
        BloodSource s = new BloodSource(UUID.randomUUID(), "\u00e9".repeat(40), false, BloodKind.COMMON_VAMPIRE);
        String name = BloodSealCodec.open(KEY, BloodSealCodec.seal(KEY, s)).orElseThrow().name();
        assertEquals("\u00e9".repeat(24), name);
    }
}
