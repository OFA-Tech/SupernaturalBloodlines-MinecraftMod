package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/** Keeps the {@link FiveRoster} in step with the species, and frees the slot of a Hunter of the Five who leaves the hunters. */
public final class HunterEvents {
    private HunterEvents() {}

    public static void register() {
        // every way the species changes (rite, debug command, rules) goes through BloodlineAccess.set
        BloodlineAccess.addListener((p, change) -> setMember(p, change.after().species()));
        NeoForge.EVENT_BUS.addListener(HunterEvents::onFactionLevelChanged);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sync(p);
            }
        });
    }

    /** Idempotent: heals the roster if it ever missed a change (lost file, state set while the part was off). */
    public static void sync(ServerPlayer p) {
        setMember(p, BloodlineAccess.get(p).species());
    }

    private static void setMember(ServerPlayer p, Species species) {
        FiveRoster roster = FiveRoster.get(p.serverLevel());
        if (species == Species.HUNTER_OF_THE_FIVE) {
            roster.add(p.getUUID());
        } else {
            roster.remove(p.getUUID());
        }
    }

    /** Leaving the hunter faction ends the mark: the rules (LEFT_BASE_FACTION) turn HUNTER_OF_THE_FIVE into NONE. */
    private static void onFactionLevelChanged(PlayerFactionEvent.FactionLevelChanged event) {
        if (event.getOldFaction() != VReference.HUNTER_FACTION || event.getCurrentFaction() == VReference.HUNTER_FACTION
                || !(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide) {
            return;
        }
        BloodlineAccess.applyEvent(p, BloodlineEvent.LEFT_BASE_FACTION,
                SpeciesContext.of(p, BaseFaction.HUNTER, p.level().getGameTime(), null));
    }
}
