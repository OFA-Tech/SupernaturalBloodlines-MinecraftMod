package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.ScopeRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.jetbrains.annotations.Nullable;

/**
 * The one place that decides whether brotherhood rules reach a group and a member. Every brotherhood perk asks {@link #appliesTo}, never
 * {@code type == BROTHERHOOD} on its own: a group that can hold several types (plan 20e) changes {@link #typeOf} (what types a group is) and
 * {@link #fits} (which of them this member belongs to) and nothing else.
 * <ul>
 * <li>{@link #of}: the brotherhood a player leads or belongs to (membership rules: marks, strikes, the lead).</li>
 * <li>{@link #appliesTo}: a perk that needs the member to fit the brotherhood themselves (the bonus, the sight, the Code, the curse).</li>
 * </ul>
 */
public final class BrotherhoodScope {
    /** A brotherhood by its leader and its stable group id. */
    public record Brotherhood(UUID leader, UUID group) {
    }

    private BrotherhoodScope() {
    }

    /** The type the group led by {@code leader} has now. */
    private static GroupType typeOf(ServerLevel level, UUID leader) {
        return Groups.typeOfGroup(level, leader);
    }

    /** Whether the group led by {@code leader} is a brotherhood. */
    public static boolean isBrotherhood(ServerLevel level, UUID leader) {
        return typeOf(level, leader) == GroupType.BROTHERHOOD;
    }

    /** Whether this player personally fits the brotherhood: a hunter, or a plain human. */
    public static boolean fits(ServerPlayer p) {
        return net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodFit.fits(Groups.brotherFacts(p, Groups.isHunter(p)));
    }

    /** The brotherhood {@code player} leads or belongs to, whatever the switches say; null when they are in no group or in another type's. */
    @Nullable
    public static Brotherhood of(ServerLevel level, UUID player) {
        PackIndex packs = PackIndex.get(level);
        if (!packs.inPack(player)) {
            return null;
        }
        UUID leader = packs.leaderOf(player);
        if (!isBrotherhood(level, leader)) {
            return null;
        }
        return GroupIndex.get(level).existingGroupOf(leader).map(g -> new Brotherhood(leader, g)).orElse(null);
    }

    /** Brotherhood perks are on, the group is a brotherhood and this member fits it. */
    public static boolean appliesTo(ServerLevel level, UUID leader, ServerPlayer member) {
        return PackIndex.get(level).leaderOf(member.getUUID()).equals(leader)
                && ScopeRules.appliesTo(typeOf(level, leader), GroupsConfig.brotherhoodPerksOn(), fits(member));
    }
}
