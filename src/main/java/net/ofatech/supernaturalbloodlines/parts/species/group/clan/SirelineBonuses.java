package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.clan.SirelineBonus;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * The sire-line bonus (R275): each online clanmate of the same sireline within 32 blocks adds a little attack damage, up to a cap. A live transient modifier,
 * never saved, refreshed every 20 ticks and removed when alone, when the group is not a clan or when clans are off. Only counts are ever shown.
 */
public final class SirelineBonuses {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "sireline_bonus");
    public static final double RANGE = 32;
    private static final int DEPTH_CAP = 64;
    private static Supplier<List<ServerPlayer>> players;
    private static int walks;

    private SirelineBonuses() {
    }

    /** For tests: the players the refresh reads instead of the server's list; null restores it. */
    public static void overridePlayers(Supplier<List<ServerPlayer>> supplier) {
        players = supplier;
    }

    /** For tests: how many root walks the last refresh made. */
    public static int lastWalks() {
        return walks;
    }

    /** The bonus a player has now, as a fraction, or 0. Used by the hub's count too. */
    public static int matesNear(ServerPlayer p, Map<UUID, UUID> roots) {
        ServerLevel level = p.serverLevel();
        ClanScope.Clan clan = ClanScope.of(level, p.getUUID());
        if (clan == null || !ClanScope.appliesTo(level, clan.leader(), p)) {
            return 0;
        }
        UUID mine = rootOf(level, p.getUUID(), roots);
        List<UUID> everyone = new ArrayList<>(PackIndex.get(level).betasOf(clan.leader()));
        everyone.add(clan.leader());
        int count = 0;
        for (UUID id : everyone) {
            if (id.equals(p.getUUID())) {
                continue;
            }
            ServerPlayer mate = SireCall.player(p.server, id);
            if (mate != null && mate.isAlive() && mate.level() == p.level() && mate.distanceToSqr(p) <= RANGE * RANGE
                    && ClanScope.appliesTo(level, clan.leader(), mate) && mine.equals(rootOf(level, id, roots))) {
                count++;
            }
        }
        return count;
    }

    private static UUID rootOf(ServerLevel level, UUID id, Map<UUID, UUID> roots) {
        return roots.computeIfAbsent(id, k -> {
            walks++;
            SirelineIndex index = SirelineIndex.get(level);
            return SirelineBonus.rootOf(k, index::sireOf, DEPTH_CAP);
        });
    }

    public static void refresh(MinecraftServer server) {
        walks = 0;
        Map<UUID, UUID> roots = new HashMap<>();
        boolean on = GroupsConfig.clanPerksOn() && GroupsConfig.sirelineBonusPercent() > 0;
        for (ServerPlayer p : players != null ? players.get() : server.getPlayerList().getPlayers()) {
            double bonus = on ? SirelineBonus.of(matesNear(p, roots), GroupsConfig.sirelineBonusPercent(), GroupsConfig.sirelineBonusMaxPercent()) : 0;
            set(p, bonus);
        }
    }

    static void set(ServerPlayer p, double amount) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (a == null) {
            return;
        }
        AttributeModifier current = a.getModifier(MODIFIER_ID);
        if (amount <= 0) {
            if (current != null) {
                a.removeModifier(MODIFIER_ID);
            }
        } else if (current == null || current.amount() != amount) {
            a.removeModifier(MODIFIER_ID);
            a.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount, Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
