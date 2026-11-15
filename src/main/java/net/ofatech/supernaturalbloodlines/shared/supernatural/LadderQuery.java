package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.Optional;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlice;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/**
 * What the other parts may ask the ladders and the skill trees. The supernatural part installs the answers; without it the layer is
 * the whole layer (today's behaviour), every rung is 0 and no skill is owned.
 */
public final class LadderQuery {
    public interface Provider {
        Optional<LayerSlice> layerSlice(Player player);

        int rung(Player player, SupernaturalRace race);

        /** The player has reached the apex rung of that race's ladder (see {@link LadderQuery#apexRungReached}). */
        boolean apexRungReached(Player player, SupernaturalRace race);

        /** The skill is bought and the tree of the player's creature or magic race lists it. */
        boolean owns(Player player, String skillId);

        /** The Tensura skills (resource locations) of the owned, active TENSURA skills. */
        Set<String> tensuraSkills(Player player);
    }

    private static final Provider NONE = new Provider() {
        @Override
        public Optional<LayerSlice> layerSlice(Player player) {
            return Optional.empty();
        }

        @Override
        public int rung(Player player, SupernaturalRace race) {
            return 0;
        }

        @Override
        public boolean apexRungReached(Player player, SupernaturalRace race) {
            return true;
        }

        @Override
        public boolean owns(Player player, String skillId) {
            return false;
        }

        @Override
        public Set<String> tensuraSkills(Player player) {
            return Set.of();
        }
    };

    private static volatile Provider provider = NONE;

    private LadderQuery() {
    }

    public static void set(Provider p) {
        provider = p;
    }

    /** The part of the species layer granted so far; empty means the whole layer. */
    public static Optional<LayerSlice> layerSlice(Player player) {
        return provider.layerSlice(player);
    }

    /** The highest rung reached on that race's ladder (0 for none). */
    public static int rung(Player player, SupernaturalRace race) {
        return provider.rung(player, race);
    }

    /**
     * The apex offer needs the apex rung of the race's ladder (the top rung, L-VAM-6 and its kin). True without the supernatural part (today's
     * behaviour), for a player who has not been migrated yet (nothing is taken back), and when the ladder has no apex rung (no Tensura, or
     * the supernatural_tensura part is off: a rung that cannot be climbed cannot be asked for).
     */
    public static boolean apexRungReached(Player player, SupernaturalRace race) {
        return provider.apexRungReached(player, race);
    }

    /** True only when the skill is owned <b>and</b> active: a dormant skill (the player's race tree does not list it) is not owned. */
    public static boolean owns(Player player, String skillId) {
        return provider.owns(player, skillId);
    }

    /**
     * The number a RULE or BLOOD skill gives for {@code key} (its {@code params} in the skill JSON), or {@code base} when the player does
     * not own the skill or the skill names no such number. A rule asks for its number here and keeps its own behaviour as the base.
     */
    public static double value(Player player, String skillId, String key, double base) {
        if (!provider.owns(player, skillId)) {
            return base;
        }
        return SkillTable.current().skill(skillId).map(s -> s.param(key, base)).orElse(base);
    }

    public static Set<String> tensuraSkills(Player player) {
        return provider.tensuraSkills(player);
    }
}
