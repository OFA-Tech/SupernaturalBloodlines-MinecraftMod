package net.ofatech.supernaturalbloodlines.core.blood;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Seals a {@link BloodSource} into one opaque string (AES-256-GCM, a random nonce per seal) so the readable source is never on the item stack: not on the wire,
 * not in the saved item, not in a decorated pot's update tag or a chat hover. The plaintext is a fixed size (kind, mob flag, UUID, a padded name), so the
 * length of a seal tells nothing about the kind or the name. {@link #open} gives nothing for a seal that was altered, made under another key, or is not one.
 */
public final class BloodSealCodec {
    public static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int NAME_BYTES = 48;
    private static final int PLAIN_BYTES = 1 + 1 + 16 + NAME_BYTES;
    private static final byte[] CONTEXT = "supernaturalbloodlines:blood_seal:1".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RANDOM = new SecureRandom();

    private BloodSealCodec() {
    }

    public static byte[] newKey() {
        byte[] key = new byte[KEY_BYTES];
        RANDOM.nextBytes(key);
        return key;
    }

    public static String seal(byte[] key, BloodSource s) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            ByteBuffer plain = ByteBuffer.allocate(PLAIN_BYTES);
            plain.put((byte) s.kind().ordinal()).put((byte) (s.npc() ? 1 : 0));
            plain.putLong(s.id().getMostSignificantBits()).putLong(s.id().getLeastSignificantBits());
            plain.put(paddedName(s.name()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            c.updateAAD(CONTEXT);
            byte[] cipher = c.doFinal(plain.array());
            byte[] out = new byte[NONCE_BYTES + cipher.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_BYTES);
            System.arraycopy(cipher, 0, out, NONCE_BYTES, cipher.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("cannot seal blood", e);
        }
    }

    /** Empty for anything that is not a seal made under this key. */
    public static Optional<BloodSource> open(byte[] key, String blob) {
        try {
            byte[] in = Base64.getDecoder().decode(blob);
            if (in.length != NONCE_BYTES + PLAIN_BYTES + TAG_BITS / 8) {
                return Optional.empty();
            }
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(in, 0, NONCE_BYTES)));
            c.updateAAD(CONTEXT);
            ByteBuffer plain = ByteBuffer.wrap(c.doFinal(in, NONCE_BYTES, in.length - NONCE_BYTES));
            int kind = plain.get();
            boolean npc = plain.get() == 1;
            UUID id = new UUID(plain.getLong(), plain.getLong());
            byte[] name = new byte[NAME_BYTES];
            plain.get(name);
            if (kind < 0 || kind >= BloodKind.values().length) {
                return Optional.empty();
            }
            return Optional.of(new BloodSource(id, unpad(name), npc, BloodKind.values()[kind]));
        } catch (GeneralSecurityException | IllegalArgumentException | java.nio.BufferUnderflowException e) {
            return Optional.empty();
        }
    }

    /** The name as UTF-8, cut to the field without splitting a character, then zero padded. */
    private static byte[] paddedName(String name) {
        byte[] raw = name.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(raw.length, NAME_BYTES);
        while (len > 0 && len < raw.length && (raw[len] & 0xC0) == 0x80) {
            len--;
        }
        return Arrays.copyOf(Arrays.copyOf(raw, len), NAME_BYTES);
    }

    private static String unpad(byte[] name) {
        int len = name.length;
        while (len > 0 && name[len - 1] == 0) {
            len--;
        }
        return new String(name, 0, len, StandardCharsets.UTF_8);
    }
}
