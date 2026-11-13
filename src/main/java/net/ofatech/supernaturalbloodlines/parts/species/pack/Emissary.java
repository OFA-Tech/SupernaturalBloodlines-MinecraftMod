package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.werewolves.EmissaryLines;
import org.jetbrains.annotations.Nullable;

/** The pack Emissary (R94): the player an Alpha named. Their rites cost less near the pack, and their mountain ash lines are stronger. */
public final class Emissary {
    public static final double RANGE = 16;

    private Emissary() {
    }

    /** Tells the artifacts part who an Emissary is (it asks when a mountain ash line is laid). */
    public static void register() {
        EmissaryLines.setEmissaryCheck(p -> p instanceof ServerPlayer sp && isEmissary(sp));
    }

    public static boolean isEmissary(ServerPlayer p) {
        return PackIndex.get(p.serverLevel()).emissaryAlpha(p.getUUID()).isPresent();
    }

    /** True when {@code caster} is an Emissary and the Alpha or a Beta of the pack that named them is within 16 blocks. */
    public static boolean nearPack(ServerLevel level, @Nullable UUID caster) {
        if (caster == null) {
            return false;
        }
        PackIndex index = PackIndex.get(level);
        UUID alpha = index.emissaryAlpha(caster).orElse(null);
        ServerPlayer me = alpha == null ? null : SireCall.player(level.getServer(), caster);
        if (me == null || me.level() != level) {
            return false;
        }
        List<UUID> pack = new ArrayList<>(index.betasOf(alpha));
        pack.add(alpha);
        for (UUID id : pack) {
            ServerPlayer member = id.equals(caster) ? null : SireCall.player(level.getServer(), id);
            if (member != null && member.isAlive() && member.level() == level && me.distanceToSqr(member) <= RANGE * RANGE) {
                return true;
            }
        }
        return false;
    }
}
