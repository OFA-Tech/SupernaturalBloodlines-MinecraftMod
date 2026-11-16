package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/**
 * The chapters of the Guide page: the ones about what the player is first (their creature, then their magic race), then how the hub and the
 * game around it work. A chapter id has a title key {@code supernaturalbloodlines.hub.guide.<id>} and a text key with {@code .body}. Pure.
 */
public final class GuideChapters {
    /** Chapters every player gets, in reading order; each is skipped when it does not apply (see {@link #forRaces}). */
    static final List<String> GENERAL = List.of("hub", "path", "skills", "quests", "rites", "bloodline", "pack", "loadout", "moon");

    private GuideChapters() {
    }

    public static String titleKey(String id) {
        return "supernaturalbloodlines.hub.guide." + id;
    }

    public static String bodyKey(String id) {
        return titleKey(id) + ".body";
    }

    /**
     * @param werewolves Werewolves is installed (the pack chapter)
     * @param enchanted  Enchanted is installed (the rites chapter)
     * @param loadout    the ability HUD part is on (the loadout chapter)
     */
    public static List<String> forRaces(SupernaturalRace creature, Optional<SupernaturalRace> magic, boolean werewolves, boolean enchanted, boolean loadout) {
        List<String> out = new ArrayList<>();
        out.add("race_" + creature.id());
        magic.ifPresent(m -> out.add("race_" + m.id()));
        for (String id : GENERAL) {
            boolean show = switch (id) {
                case "rites" -> enchanted;
                case "pack" -> werewolves && isWolf(creature);
                case "loadout" -> loadout;
                default -> true;
            };
            if (show && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    private static boolean isWolf(SupernaturalRace r) {
        return r == SupernaturalRace.WEREWOLF || r == SupernaturalRace.HYBRID || r == SupernaturalRace.ORIGINAL_HYBRID || r == SupernaturalRace.TRIBRID;
    }
}
