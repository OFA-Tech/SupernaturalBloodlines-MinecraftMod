package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/** The hub's pages, in ribbon order. Which ones show depends on the installed mods and the player's races. Pure, no client classes. */
public enum HubPage {
    PATH, SKILLS, QUESTS, RITES, BLOODLINE, PACK;

    /**
     * @param hasPack the player is in a pack (or leads one), whatever the race
     */
    public record Context(SupernaturalRace creature, Optional<SupernaturalRace> magic, boolean werewolves, boolean enchanted, boolean hasPack) {
    }

    public String langKey() {
        return "supernaturalbloodlines.hub.page." + name().toLowerCase(Locale.ROOT);
    }

    public boolean visible(Context c) {
        return switch (this) {
            case PATH, SKILLS, QUESTS, BLOODLINE -> true;
            case RITES -> c.enchanted();
            case PACK -> c.werewolves() && (wolfRace(c.creature()) || c.hasPack());
        };
    }

    /** A Ghoul sees Path and Bloodline; every other page asks them to complete the turning first. */
    public boolean locked(Context c) {
        return c.creature() == SupernaturalRace.GHOUL && this != PATH && this != BLOODLINE;
    }

    public static List<HubPage> visiblePages(Context c) {
        return Arrays.stream(values()).filter(p -> p.visible(c)).toList();
    }

    /** {@code wanted} when it shows, else the first page (a page can go away while the screen is open, for example on leaving a pack). */
    public static HubPage orFirst(HubPage wanted, Context c) {
        return wanted.visible(c) ? wanted : PATH;
    }

    private static boolean wolfRace(SupernaturalRace r) {
        return r == SupernaturalRace.WEREWOLF || r == SupernaturalRace.HYBRID || r == SupernaturalRace.ORIGINAL_HYBRID || r == SupernaturalRace.TRIBRID;
    }
}
