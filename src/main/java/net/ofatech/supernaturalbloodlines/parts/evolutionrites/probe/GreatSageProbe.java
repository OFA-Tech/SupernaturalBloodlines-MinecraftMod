package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;

/**
 * Great Sage's road to Raphael (probe id {@code greatsage.raphael}). The addon decides whether its own road is open ({@code CapstoneGate.OBTAINABLE}, false in
 * 0.0.7.1) and whether a player passes its whole gate ({@code CapstoneGate.available}); this reads both and never reimplements them. With FoxAblaze installed the
 * grant is FoxAblaze's Raphael (the merge, spec D14) and either Raphael counts as held.
 */
public final class GreatSageProbe implements AddonProbe {
    private static final String GATE = "dev.dedek.greatsage.server.CapstoneGate";
    private static final String RULES = "dev.dedek.greatsage.GreatSageGameRules";

    @Override
    public Optional<String> missingHook() {
        Optional<String> field = Hooks.staticField(GATE, "OBTAINABLE").missing();
        if (field.isPresent()) {
            return field;
        }
        Optional<String> gate = Hooks.method(GATE, "available", ServerPlayer.class).missing();
        if (gate.isPresent()) {
            return gate;
        }
        return Hooks.staticField(RULES, "ADDON_RAPHAELS").missing();
    }

    @Override
    public boolean roadOpen() {
        return Hooks.staticField(GATE, "OBTAINABLE").value().map(Boolean.TRUE::equals).orElse(false);
    }

    @Override
    public boolean satisfied(ServerPlayer target, EvolutionRecipe recipe) {
        Hooks.Hit hit = Hooks.method(GATE, "available", ServerPlayer.class);
        if (hit.handle().isEmpty()) {
            return false;
        }
        try {
            return (boolean) hit.handle().get().invoke(target);
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean ownedByOther(ServerPlayer target, EvolutionRecipe recipe) {
        return FoxAblazeProbe.loaded() && FoxAblazeProbe.ownedByOther(target);
    }

    @Override
    public Optional<String> grantOverride() {
        return FoxAblazeProbe.loaded() ? Optional.of(FoxAblazeProbe.RAPHAEL.toString()) : Optional.empty();
    }

    @Override
    public List<String> alsoHolds() {
        return List.of("greatsage:raphael", FoxAblazeProbe.RAPHAEL.toString());
    }
}
