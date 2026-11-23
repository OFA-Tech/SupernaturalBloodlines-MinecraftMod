package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/**
 * The hub's pages, in nav order (the IA doc, 3.2). Which ones show depends on the installed mods, the player's races, their pack and their
 * permission. {@link #LOADOUT} is only an alias: it opens Skills on its Loadout tab and is never in the nav. Pure, no client classes.
 */
public enum HubPage {
    HOME(Group.NONE), PATH(Group.ME), SKILLS(Group.ME), QUESTS(Group.ME), BLOODLINE(Group.PEOPLE), PACK(Group.PEOPLE),
    GUIDE(Group.HELP), SETTINGS(Group.SYSTEM), ADMIN(Group.OPERATOR), LOADOUT(Group.ME);

    /** What the nav groups pages under: Me, Magic, People, Help, System (Home stands alone); Admin is apart, under Operator. */
    public enum Group {
        NONE, ME, PEOPLE, HELP, SYSTEM, OPERATOR;

        public String langKey() {
            return "supernaturalbloodlines.hub.group." + name().toLowerCase(Locale.ROOT);
        }
    }

    private final Group group;

    HubPage(Group group) {
        this.group = group;
    }

    public Group group() {
        return group;
    }

    /**
     * The races and installed mods a page's visibility depends on. {@code op} is the client's own {@code hasPermissions(2)} (it only decides what is
     * drawn; the server checks every payload again); {@code inPack} is whether the player has a pack, whatever their race.
     */
    public record Context(SupernaturalRace creature, Optional<SupernaturalRace> magic, boolean werewolves, boolean enchanted, boolean loadout,
                          boolean op, boolean inPack) {
        public Context(SupernaturalRace creature, Optional<SupernaturalRace> magic, boolean werewolves, boolean enchanted, boolean loadout) {
            this(creature, magic, werewolves, enchanted, loadout, false, false);
        }

        public Context(SupernaturalRace creature, Optional<SupernaturalRace> magic, boolean werewolves, boolean enchanted) {
            this(creature, magic, werewolves, enchanted, false, false, false);
        }

        public Context withOp(boolean op) {
            return new Context(creature, magic, werewolves, enchanted, loadout, op, inPack);
        }

        public Context withPack(boolean inPack) {
            return new Context(creature, magic, werewolves, enchanted, loadout, op, inPack);
        }
    }

    private static volatile java.util.function.Supplier<String> packKey = () -> "supernaturalbloodlines.hub.page.pack";

    /** The Pack page keeps its constant and its saved layouts; only its label follows the group the viewer is in (the client installs this). */
    public static void usePackKey(java.util.function.Supplier<String> key) {
        packKey = key == null ? () -> "supernaturalbloodlines.hub.page.pack" : key;
    }

    public String langKey() {
        return this == PACK ? packKey.get() : "supernaturalbloodlines.hub.page." + name().toLowerCase(Locale.ROOT);
    }

    public boolean visible(Context c) {
        return switch (this) {
            case HOME, PATH, SKILLS, QUESTS, BLOODLINE, GUIDE, SETTINGS -> true;
            // the alias of a Skills tab: never a page of its own
            case LOADOUT -> false;
            // in a pack (any race: a witch Emissary, a Packmate), or a werewolf-type race with Werewolves; a former werewolf with no pack keeps none
            case PACK -> c.inPack() || (c.werewolves() && wolfRace(c.creature()));
            case ADMIN -> c.op();
        };
    }

    /** A Ghoul keeps Home, Path, the Guide and Settings; every other page asks them to complete the turning first. */
    public boolean locked(Context c) {
        return c.creature() == SupernaturalRace.GHOUL && this != HOME && this != PATH && this != GUIDE && this != SETTINGS;
    }

    public static List<HubPage> visiblePages(Context c) {
        return Arrays.stream(values()).filter(p -> p.visible(c)).toList();
    }

    /** {@code wanted} when it shows, else Home (a page can go away while the screen is open: leaving a pack, losing operator rights). */
    public static HubPage orFirst(HubPage wanted, Context c) {
        HubPage w = wanted == LOADOUT ? SKILLS : wanted;
        return w.visible(c) ? w : HOME;
    }

    /** A werewolf or a hybrid type: the races that have a pack by nature. */
    public static boolean isWolfType(SupernaturalRace r) {
        return wolfRace(r);
    }

    private static boolean wolfRace(SupernaturalRace r) {
        return r == SupernaturalRace.WEREWOLF || r == SupernaturalRace.HYBRID || r == SupernaturalRace.ORIGINAL_HYBRID || r == SupernaturalRace.TRIBRID;
    }
}
