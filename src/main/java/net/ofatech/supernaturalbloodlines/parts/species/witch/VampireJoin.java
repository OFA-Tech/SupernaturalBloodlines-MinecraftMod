package net.ofatech.supernaturalbloodlines.parts.species.witch;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/** R61/R62: a witch who joins the vampires loses the magic, a siphoner becomes a heretic (the rules do the change). */
public final class VampireJoin {
    private VampireJoin() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VampireJoin::onFactionLevelChanged);
    }

    private static void onFactionLevelChanged(PlayerFactionEvent.FactionLevelChanged event) {
        if (event.getCurrentFaction() != VReference.VAMPIRE_FACTION || event.getOldFaction() == VReference.VAMPIRE_FACTION
                || !(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide) {
            return;
        }
        BloodlineAccess.applyEvent(p, BloodlineEvent.JOINED_VAMPIRE, SpeciesContext.of(p, null));
    }
}
