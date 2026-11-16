package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/** The hub's pages, in ribbon order. Which ones show depends on the installed mods and the player's races. Pure, no client classes. */
public enum HubPage {
    PATH, SKILLS, QUESTS, RITES, BLOODLINE, PACK, GUIDE, LOADOUT;

    /** The races and installed mods a page's visibility depends on. */
    public record Context(SupernaturalRace creature, Optional<SupernaturalRace> magic, boolean werewolves, boolean enchanted, boolean loadout) {
        public Context(SupernaturalRace creature, Optional<SupernaturalRace> magic, boolean werewolves, boolean enchanted) {
            this(creature, magic, werewolves, enchanted, false);
        }
    }

    public String langKey() {
        return "supernaturalbloodlines.hub.page." + name().toLowerCase(Locale.ROOT);
    }

    public boolean visible(Context c) {
        return switch (this) {
            case PATH, SKILLS, QUESTS, BLOODLINE, GUIDE -> true;
            // the ability HUD part brings the slots to arrange; without it there is nothing to assign (the Skills page keeps its own tab)
            case LOADOUT -> c.loadout();
            case RITES -> c.enchanted();
            // a current member of the werewolf faction or a hybrid type; a former werewolf (cured, or turned a vampire) keeps no pack page
            case PACK -> c.werewolves() && wolfRace(c.creature());
        };
    }

    /** A Ghoul sees Path, Bloodline and the Guide; every other page asks them to complete the turning first. */
    public boolean locked(Context c) {
        return c.creature() == SupernaturalRace.GHOUL && this != PATH && this != BLOODLINE && this != GUIDE;
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
