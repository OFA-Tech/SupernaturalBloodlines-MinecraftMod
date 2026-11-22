package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSealCodec;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * Writes and reads whose blood a bottle holds (R210). The source is never on the stack in the clear: it is one sealed string ({@link BloodSealCodec}, the
 * world's key) that only the server can open, so the saved item, the wire, a decorated pot and a chat hover carry ciphertext and a public label (a common
 * vampire's name, {@code ?} for every other kind). A seal that does not open (another world, a lost key, a tampered or forged string, a client) reads as
 * unstamped: common vampire blood.
 *
 * <p>Bottles made before sealing keep the old plain {@code blood_source} and {@code blood_source_species}. They are read as they were (an Original's Blood
 * bottle with no species as an Original hybrid's, so it is accepted under either {@code hybrid_transition_blood} setting) and moved into a seal by
 * {@link #migrate}, on a player's login and whenever the bottle is used.
 */
public final class BloodStamp {
    /** The label of every secret kind. */
    public static final String SECRET_LABEL = "?";

    private BloodStamp() {
    }

    /** Seals the source on the stack with the public label. A client cannot (no key): nothing is written. */
    public static void write(ItemStack stack, BloodSource s) {
        if (writeSeal(stack, s)) {
            stack.set(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get(), s.kind().secret() ? SECRET_LABEL : s.name());
        }
    }

    /** Seals the source and nothing else (Sire's Blood: no blood label, its own public name). @return whether it was written */
    public static boolean writeSeal(ItemStack stack, BloodSource s) {
        byte[] key = BloodKeys.key();
        if (key == null) {
            return false;
        }
        stack.set(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get(), BloodSealCodec.seal(key, s));
        stack.remove(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get());
        stack.remove(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get());
        return true;
    }

    /** The stack holds a source in either form (the client sees only the seal). */
    public static boolean has(ItemStack stack) {
        return stack.has(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get()) || stack.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get());
    }

    /** The stack carries a seal (a source in the old plain form is not one). */
    public static boolean sealed(ItemStack stack) {
        return stack.has(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get());
    }

    /** Empty for an unstamped bottle, one whose seal does not open, or one whose old source is NIL. */
    public static Optional<BloodSource> read(ItemStack stack) {
        String seal = stack.get(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get());
        if (seal != null) {
            byte[] key = BloodKeys.key();
            return key == null ? Optional.empty() : BloodSealCodec.open(key, seal);
        }
        return readPlain(stack);
    }

    private static Optional<BloodSource> readPlain(ItemStack stack) {
        UUID id = stack.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get());
        if (id == null || Util.NIL_UUID.equals(id)) {
            return Optional.empty();
        }
        String species = stack.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get());
        BloodKind kind = species == null && stack.is(SpeciesRegistries.ORIGINAL_BLOOD.get()) ? BloodKind.ORIGINAL_HYBRID : BloodKind.parse(species);
        return Optional.of(new BloodSource(id, "", false, kind));
    }

    /** Moves the old plain source into a seal. @return whether the stack changed (server only: a client has no key) */
    public static boolean migrate(ItemStack stack) {
        if (stack.has(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get()) || !stack.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get())) {
            return false;
        }
        Optional<BloodSource> plain = readPlain(stack);
        if (plain.isEmpty()) {
            stack.remove(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get());
            stack.remove(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get());
            return true;
        }
        if (stack.is(SpeciesRegistries.SIRES_BLOOD.get())) {
            return writeSeal(stack, plain.get());
        }
        write(stack, plain.get());
        return sealed(stack);
    }

    /** Unstamped counts as common vampire blood. */
    public static BloodKind kindOf(ItemStack stack) {
        return read(stack).map(BloodSource::kind).orElse(BloodKind.COMMON_VAMPIRE);
    }
}
