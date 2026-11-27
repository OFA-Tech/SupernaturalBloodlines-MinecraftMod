package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Kol's shadow (R296): when Kol is placed for a brother, the other brothers who are online hear that the target is being hunted: the target's name and nothing
 * else (never Kol, never his nature, never a lineage). Species is the lower part, so Kol's code calls this one static method; it can never stop him spawning.
 */
public final class KolShadow {
    private static Consumer<ServerPlayer> probe;
    private static boolean logged;

    private KolShadow() {
    }

    /** For tests: runs inside {@link #warn} before anything else, so a test can make it fail; null removes it. */
    public static void overrideProbe(Consumer<ServerPlayer> value) {
        probe = value;
    }

    public static void warn(ServerPlayer target) {
        try {
            if (probe != null) {
                probe.accept(target);
            }
            if (!GroupsConfig.kolWarningEnabled() || !GroupsConfig.brotherhoodPerksOn()) {
                return;
            }
            ServerLevel level = target.serverLevel();
            BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, target.getUUID());
            if (b == null || !BrotherhoodScope.appliesTo(level, b.leader(), target)) {
                return;
            }
            Component line = BrotherhoodText.of("kol_warning", target.getGameProfile().getName());
            for (UUID id : PackIndex.get(level).roster(b.leader())) {
                ServerPlayer p = id.equals(target.getUUID()) ? null : SireCall.player(target.server, id);
                if (p != null && BrotherhoodScope.appliesTo(level, b.leader(), p)) {
                    p.sendSystemMessage(line);
                    p.displayClientMessage(line, true);
                }
            }
        } catch (RuntimeException e) {
            if (!logged) {
                logged = true;
                SupernaturalBloodlines.LOGGER.error("Kol's shadow failed; Kol is not held back by it", e);
            }
        }
    }
}