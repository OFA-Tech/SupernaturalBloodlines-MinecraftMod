package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.bloodline.FriendRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.jetbrains.annotations.Nullable;

/**
 * Who a Wolf Spirit's owner counts as a friend (R144): themselves, their pack, allied packs, and the other spirits of those. The rule is
 * {@link FriendRules}; this finds the two leaders and whether the packs are allied.
 */
public final class Friends {
    private Friends() {
    }

    /** Whether {@code other} is a friend of the pack {@code owner} leads or is in. */
    public static boolean ofOwner(Level level, @Nullable UUID owner, LivingEntity other) {
        if (owner == null || !(level instanceof ServerLevel server)) {
            return false;
        }
        UUID otherPlayer = playerOf(other);
        if (otherPlayer == null) {
            return false;
        }
        PackIndex index = PackIndex.get(server);
        return FriendRules.friends(index.leaderOf(owner), index.leaderOf(otherPlayer), index.alliedPlayers(owner, otherPlayer));
    }

    /** The player an entity stands for: itself, or the owner of a Wolf Spirit. */
    @Nullable
    public static UUID playerOf(LivingEntity e) {
        if (e instanceof Player p) {
            return p.getUUID();
        }
        if (e instanceof Mob m && m.hasData(SpeciesRegistries.NPC_MARK)) {
            NpcMark mark = m.getData(SpeciesRegistries.NPC_MARK);
            return mark.kind() == NpcMark.Kind.SPIRIT ? mark.owner() : null;
        }
        return null;
    }
}
