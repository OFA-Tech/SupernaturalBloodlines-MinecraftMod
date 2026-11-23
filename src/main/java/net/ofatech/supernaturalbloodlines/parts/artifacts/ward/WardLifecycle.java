package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;

/**
 * The two falls of a ward (R307): its owner dies, and its owner becomes vampiric. The second is its own trigger, not a death: a turning's lethal blow is
 * cancelled (the player lies down), so no death reaches {@code LivingDeathEvent} for it. It is read from the index by owner, so a ward whose chunk is not
 * loaded falls too. An owner who turns while offline keeps the ward until their next login, which checks. One switch, {@code ward_falls_on_owner_death}.
 */
public final class WardLifecycle {
    private static final ResourceLocation VAMPIRE_FACTION = ResourceLocation.parse("vampirism:vampire");

    private WardLifecycle() {
    }

    public static void register() {
        // LOWEST and not for a cancelled blow: a totem, or a rule of ours that stops the death, leaves the ward standing
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                onDeath(player);
            }
        });
        BloodlineAccess.addListener((player, change) -> onBecoming(player));
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getPlayer().getPlayer() instanceof ServerPlayer player && e.getCurrentFaction() != null && VAMPIRE_FACTION.equals(e.getCurrentFaction().getID())) {
                fall(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                onBecoming(player);
            }
        });
    }

    /** The owner died: every ward of theirs falls. */
    public static void onDeath(ServerPlayer owner) {
        fall(owner);
    }

    /** The owner may have become vampiric (a species change, a faction, a login): every ward of theirs falls when they are. */
    public static void onBecoming(ServerPlayer owner) {
        if (Supernatural.isVampiric(owner)) {
            fall(owner);
        }
    }

    private static void fall(ServerPlayer owner) {
        if (ArtifactsConfig.wardFallsOnOwnerDeath()) {
            WardIndex.get(owner.serverLevel()).fallAllOf(owner.getUUID());
        }
    }
}
