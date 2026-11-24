package net.ofatech.supernaturalbloodlines.parts.species.group;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/** Who may lead a group: the one gate every invite, kick and alliance goes through (spec 3.2). */
public final class GroupGate {
    private GroupGate() {
    }

    public static GroupTypes.Fit fit(ServerPlayer p) {
        return new GroupTypes.Fit(alphaFit(p), VampirismSupernatural.isVampire(p),
                Groups.isMagicUser(BloodlineAccess.get(p).species()), Groups.isHunter(p));
    }

    /** A pack's leader is an active Alpha where Werewolves has Alphas; without it nobody can be one, and a hybrid-type leader (the kind that derives PACK) is fit (spec Q3). */
    private static boolean alphaFit(ServerPlayer p) {
        return !net.neoforged.fml.ModList.get().isLoaded("werewolves") || PackCommand.isActiveAlpha(p);
    }

    /**
     * Empty when allowed, else the refusal base ({@code not_fit}, {@code group_off}). A leader of a group that already has members
     * keeps leading it whatever they are now, except a pack, whose leader must still be an active Alpha as before.
     */
    public static Optional<String> canLead(ServerPlayer p) {
        UUID id = p.getUUID();
        PackIndex packs = PackIndex.get(p.serverLevel());
        boolean hasMembers = !packs.betasOf(id).isEmpty();
        GroupType type = hasMembers ? Groups.typeOfGroup(p.serverLevel(), id) : Groups.derive(p);
        if (hasMembers && type != GroupType.PACK) {
            return Optional.empty();
        }
        if (!hasMembers && !enabled(type)) {
            return Optional.of("group_off");
        }
        return GroupTypes.canFound(type, fit(p), GroupsConfig.openFounding()) ? Optional.empty() : Optional.of("not_fit");
    }

    /** Whether a shunning still bars the player from every coven. */
    public static boolean shunned(net.minecraft.server.MinecraftServer server, UUID player) {
        return GroupIndex.get(server.overworld()).banUntil(net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenGovernance.SHUN_SCOPE, player) > PackIndex.now();
    }

    /** {@link #canLead} and room for one more member. */
    public static Optional<String> canInvite(ServerPlayer p) {
        Optional<String> lead = canLead(p);
        if (lead.isPresent()) {
            return lead;
        }
        int max = GroupsConfig.maxMembers();
        return max > 0 && PackIndex.get(p.serverLevel()).betasOf(p.getUUID()).size() >= max ? Optional.of("full") : Optional.empty();
    }

    private static boolean enabled(GroupType type) {
        return switch (type) {
            case PACK -> true;
            case CLAN -> GroupsConfig.groupsEnabled() && GroupsConfig.clansEnabled();
            case COVEN -> GroupsConfig.groupsEnabled() && GroupsConfig.covensEnabled();
            case BROTHERHOOD -> GroupsConfig.groupsEnabled();
        };
    }
}
