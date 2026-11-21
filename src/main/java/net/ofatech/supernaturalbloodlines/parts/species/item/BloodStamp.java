package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * Writes and reads whose blood a bottle holds (R210). The source, its species, its mob flag and its name are saved on the stack but reach a client only as
 * units, so a client cannot read or resolve them; the one synced thing is the label a tooltip shows: {@code "?"} for every secret kind, the name of a common
 * vampire. A creative player's client sends stacks back through the network codec, so a bottle they move comes back with a NIL source: that reads as
 * unstamped (common vampire blood), never as an error.
 */
public final class BloodStamp {
    /** The label of every secret kind. */
    public static final String SECRET_LABEL = "?";

    private BloodStamp() {
    }

    public static void write(ItemStack stack, BloodSource s) {
        stack.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), s.id());
        stack.set(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get(), s.kind().speciesName());
        stack.set(SpeciesRegistries.BLOOD_SOURCE_NPC_COMPONENT.get(), s.npc());
        stack.set(SpeciesRegistries.BLOOD_SOURCE_PRIVATE_NAME_COMPONENT.get(), s.name());
        stack.set(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get(), s.kind().secret() ? SECRET_LABEL : s.name());
    }

    /** Empty for an unstamped bottle or one whose source is NIL. */
    public static Optional<BloodSource> read(ItemStack stack) {
        UUID id = stack.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get());
        if (id == null || Util.NIL_UUID.equals(id)) {
            return Optional.empty();
        }
        String species = stack.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get());
        BloodKind kind = species == null && stack.is(SpeciesRegistries.ORIGINAL_BLOOD.get())
                ? BloodKind.ORIGINAL_HYBRID // made before the bottles recorded a species: accepted by either hybrid_transition_blood setting
                : BloodKind.parse(species);
        return Optional.of(new BloodSource(id, stack.getOrDefault(SpeciesRegistries.BLOOD_SOURCE_PRIVATE_NAME_COMPONENT.get(), ""),
                stack.getOrDefault(SpeciesRegistries.BLOOD_SOURCE_NPC_COMPONENT.get(), false), kind));
    }

    /** Unstamped counts as common vampire blood. */
    public static BloodKind kindOf(ItemStack stack) {
        return read(stack).map(BloodSource::kind).orElse(BloodKind.COMMON_VAMPIRE);
    }
}
