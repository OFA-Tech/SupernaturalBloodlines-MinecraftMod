package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.Rung;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Which of the server's rites the Rites page puts first for the player: a rite is "for you" when its factory names your faction side
 * ({@code faction}: vampire, werewolf, vampire_or_werewolf, hunter, none) or when one of your ladders asks for it. Anyone can perform any
 * rite with the power, so the others are still listed, after. The server only sends rites whose part and mods are on. Pure.
 */
public final class HubRites {
    public record Listed(HubSocial.Rite rite, boolean forYou) {
    }

    private static final Set<SupernaturalRace> VAMPIRE_SIDE = Set.of(SupernaturalRace.VAMPIRE, SupernaturalRace.ORIGINAL, SupernaturalRace.HERETIC);
    private static final Set<SupernaturalRace> WEREWOLF_SIDE = Set.of(SupernaturalRace.WEREWOLF, SupernaturalRace.HYBRID,
            SupernaturalRace.ORIGINAL_HYBRID, SupernaturalRace.TRIBRID);

    private HubRites() {
    }

    /** The rites the player's ladders ask for, on any rung. */
    public static Set<String> fromLadders(LadderTable table, List<SupernaturalRace> races) {
        Set<String> out = new HashSet<>();
        for (SupernaturalRace race : races) {
            for (Rung r : table.get(race).rungs()) {
                for (TaskSpec t : r.tasks()) {
                    for (TaskSpec leaf : t.leaves()) {
                        if (leaf.detector() == TaskSpec.Detector.RITE && leaf.args().containsKey("rite")) {
                            out.add(leaf.args().get("rite"));
                        }
                    }
                }
            }
        }
        return out;
    }

    public static boolean forYou(HubSocial.Rite rite, List<SupernaturalRace> races, Set<String> ladderRites) {
        if (ladderRites.contains(rite.id())) {
            return true;
        }
        SupernaturalRace creature = races.get(0);
        return switch (rite.faction()) {
            case "vampire" -> VAMPIRE_SIDE.contains(creature);
            case "werewolf" -> WEREWOLF_SIDE.contains(creature);
            case "vampire_or_werewolf" -> VAMPIRE_SIDE.contains(creature) || WEREWOLF_SIDE.contains(creature);
            case "hunter" -> creature == SupernaturalRace.HUNTER;
            case "none" -> creature == SupernaturalRace.HUMAN;
            default -> false;
        };
    }

    /** Yours first, then the rest; by power within each (the cheapest first). */
    public static List<Listed> list(List<HubSocial.Rite> rites, List<SupernaturalRace> races, Set<String> ladderRites) {
        List<Listed> out = new ArrayList<>();
        rites.forEach(r -> out.add(new Listed(r, forYou(r, races, ladderRites))));
        out.sort(Comparator.comparing((Listed l) -> !l.forYou()).thenComparingInt(l -> l.rite().power()).thenComparing(l -> l.rite().id()));
        return out;
    }
}
