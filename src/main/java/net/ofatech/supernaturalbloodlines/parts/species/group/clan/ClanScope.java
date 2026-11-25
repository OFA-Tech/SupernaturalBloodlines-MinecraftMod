package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;

/**
 * The one place that decides whether clan rules reach a group and a member. Everything that is a clan perk asks here, never {@code type == CLAN}
 * on its own: a group that can hold several types (a later plan) changes {@link #isClan} and {@link #fits} and nothing else.
 * <ul>
 * <li>{@link #of}: the clan a player leads or belongs to (membership rules: ranks, the Charter, the contest, exile).</li>
 * <li>{@link #appliesTo}: a perk that needs the member to be clan-like themselves (shared feeding, the sire-line bonus).</li>
 * </ul>
 */
public final class ClanScope {
    /** A clan by its leader and its stable group id. */
    public record Clan(UUID leader, UUID group) {
    }

    private ClanScope() {
    }

    /** Whether the group led by {@code leader} has the clan type. */
    public static boolean isClan(ServerLevel level, UUID leader) {
        return Groups.typeOfGroup(level, leader) == GroupType.CLAN;
    }

    /** Whether this player personally fits the clan: a vampire, an Original, an Augustine, a vampire-siphoner. */
    public static boolean fits(ServerPlayer p) {
        return VampirismSupernatural.isVampire(p);
    }

    /** The clan {@code player} leads or belongs to, whatever the switches say; null when they are in no group or in another type's. */
    @Nullable
    public static Clan of(ServerLevel level, UUID player) {
        PackIndex packs = PackIndex.get(level);
        if (!packs.inPack(player)) {
            return null;
        }
        UUID leader = packs.leaderOf(player);
        if (!isClan(level, leader)) {
            return null;
        }
        return GroupIndex.get(level).existingGroupOf(leader).map(g -> new Clan(leader, g)).orElse(null);
    }

    /** Clan perks are on, the group is a clan and this member fits it. */
    public static boolean appliesTo(ServerLevel level, UUID leader, ServerPlayer member) {
        return GroupsConfig.clanPerksOn() && isClan(level, leader) && fits(member);
    }
}
