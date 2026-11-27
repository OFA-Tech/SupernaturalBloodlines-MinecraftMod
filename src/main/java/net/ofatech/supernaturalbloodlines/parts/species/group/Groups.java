package net.ofatech.supernaturalbloodlines.parts.species.group;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodFit;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/** What type a player's group is, or would be: the live facts for {@link GroupTypes}, and the stored type of a group. */
public final class Groups {
    private static final ResourceLocation HUNTER = ResourceLocation.parse("vampirism:hunter");

    private Groups() {
    }

    public static boolean isHunter(ServerPlayer p) {
        Species s = BloodlineAccess.get(p).species();
        if (s == Species.HUNTER_OF_THE_FIVE || s == Species.POTENTIAL) {
            return true;
        }
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return f != null && HUNTER.equals(f.getID());
    }

    public static boolean isMagicUser(Species s) {
        return s == Species.WITCH || s == Species.SIPHONER || s == Species.HERETIC;
    }

    /** What the brotherhood's fit reads about a player: the hunter fact given, the rest live. */
    public static BrotherhoodFit.Facts brotherFacts(ServerPlayer p, boolean hunter) {
        var state = BloodlineAccess.get(p);
        boolean werewolf = VampirismSupernatural.isWerewolf(p) || state.wolf().lineage() != Lineage.NONE;
        return new BrotherhoodFit.Facts(hunter, state.species(), VampirismSupernatural.isVampire(p), werewolf, SpeciesEligibility.isHumanLike(p));
    }

    /** A plain human: no species, human-like, no vampire and no werewolf fact, and not a hunter. */
    public static boolean isPlainHuman(ServerPlayer p) {
        return BrotherhoodFit.fits(brotherFacts(p, false));
    }

    /** The type this player's own kind leads, from the facts that can be read now. */
    public static GroupType derive(ServerPlayer p) {
        var state = BloodlineAccess.get(p);
        boolean werewolf = VampirismSupernatural.isWerewolf(p) || state.wolf().lineage() != Lineage.NONE;
        return GroupTypes.of(new GroupTypes.Leader(state.species(), VampirismSupernatural.isVampire(p), werewolf, isHunter(p), SpeciesEligibility.isHumanLike(p)));
    }

    /** The type of the group the player is in, else the type their own kind would lead. */
    public static GroupType typeOf(ServerPlayer p) {
        PackIndex packs = PackIndex.get(p.serverLevel());
        return packs.inPack(p.getUUID()) ? typeOfGroup(p.serverLevel(), packs.leaderOf(p.getUUID())) : derive(p);
    }

    /** The stored type of the group {@code leader} leads; a group that has no row yet is a pack. */
    public static GroupType typeOfGroup(ServerLevel level, UUID leader) {
        GroupIndex groups = GroupIndex.get(level);
        return groups.existingGroupOf(leader).map(groups::typeOf).orElse(GroupType.PACK);
    }
}
