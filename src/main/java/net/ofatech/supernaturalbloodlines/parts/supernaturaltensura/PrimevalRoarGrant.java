package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.shared.tensura.PrimevalAlpha;

/**
 * S-WOL-8 Primeval Roar is granted, never bought: it is in the player's skills while they are a True Alpha on {@code alpha_primeval}
 * ({@link PrimevalAlpha#active}) and out of them while that does not hold (a dormant True Alpha, or a different race), back on the rejoin.
 * It costs nothing, so nothing is refunded or lost. {@link #sync} is idempotent; it runs on a rank or faction change, login, respawn and every
 * 200 ticks (a Tensura evolution is noticed then).
 */
public final class PrimevalRoarGrant {
    public static final String SKILL = "S-WOL-8";
    private static final int RECHECK_TICKS = 200;

    private PrimevalRoarGrant() {
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> sync(p));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> syncIfServer(e.getPlayer().getPlayer()));
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % RECHECK_TICKS == 0) {
                sync(p);
            }
        });
    }

    private static void syncIfServer(Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    public static void sync(ServerPlayer p) {
        if (p.level().isClientSide || (p instanceof FakePlayer && !SupernaturalAccess.tracksFakePlayers())) {
            return;
        }
        SupernaturalState s = SupernaturalAccess.get(p);
        boolean want = PrimevalAlpha.active(p);
        if (want == s.skills().contains(SKILL)) {
            return;
        }
        Set<String> skills = new HashSet<>(s.skills());
        if (want) {
            skills.add(SKILL);
        } else {
            skills.remove(SKILL);
        }
        SupernaturalAccess.set(p, s.toBuilder().skills(skills).build());
    }
}
