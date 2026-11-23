package net.ofatech.supernaturalbloodlines.parts.supernatural;

import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The part's own config file. The ladders and skills are datapack JSON, not config. */
public final class SupernaturalConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-supernatural.toml";
    public static final ModConfigSpec SPEC;

    /** R213: the four Vampirism tweaks, all off by default. */
    public enum Tweak {
        SUNLIGHT_IGNITES_VAMPIRES("tweak_sunlight_ignites_vampires"),
        CLEANSING_ALTAR_DISABLED("tweak_cleansing_altar_disabled"),
        HOSTILE_MOBS_ATTACK_VAMPIRES("tweak_hostile_mobs_attack_vampires"),
        SKELETONS_ATTACK_WEREWOLVES("tweak_skeletons_attack_werewolves");

        public final String key;

        Tweak(String key) {
            this.key = key;
        }
    }

    private static final ModConfigSpec.BooleanValue RITE_COMPLETE_SIGNAL;
    private static final ModConfigSpec.IntValue VAMPIRE_FEED_BLOOD;
    private static final Map<Tweak, ModConfigSpec.BooleanValue> TWEAKS = new EnumMap<>(Tweak.class);
    public static final int DEFAULT_VAMPIRE_FEED_BLOOD = 3;
    private static volatile Boolean riteCompleteOverride;
    private static final Map<Tweak, Boolean> TWEAK_OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();
    /** Client side: what the server said about the altar, until the client leaves the world. */
    private static volatile Boolean pushedAltarDisabled;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RITE_COMPLETE_SIGNAL = b.comment("A rite that worked counts for ladder tasks (the RiteCompleteMixin hook on Enchanted's Rite.start). Off: no rite counts for any task.")
                .define("rite_complete_signal", true);
        VAMPIRE_FEED_BLOOD = b.comment("Predator's Thirst (S-VAM-6): the Vampirism blood a bite on a vampire gives when Vampirism gives none for that victim (it is also what a vampire player loses).")
                .defineInRange("vampire_feed_blood", DEFAULT_VAMPIRE_FEED_BLOOD, 1, 20);
        TWEAKS.put(Tweak.SUNLIGHT_IGNITES_VAMPIRES, b.comment("R213: every time Vampirism's own sun damage hurts a player, they are also set on fire for 4 seconds (Vampirism turns fire into its own heavy vampire fire damage). Vampirism's rules decide who is hurt: its level gate, grace period, attributes, Sun Ring, poppet, umbrella and creative.")
                .define(Tweak.SUNLIGHT_IGNITES_VAMPIRES.key, false));
        TWEAKS.put(Tweak.CLEANSING_ALTAR_DISABLED, b.comment("R213: the cleansing altar cannot be used to leave the vampire faction (the click is refused on the server and on every client that has this mod, and the server refuses the revert packet itself from any client). A player who sends that packet without the switch in mind still dies (Vampirism kills on leaving outside hardcore) but stays a vampire. Vampirism's med chair is not touched.")
                .define(Tweak.CLEANSING_ALTAR_DISABLED.key, false));
        TWEAKS.put(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, b.comment("R213: zombies, husks, drowned, zombie villagers, skeletons and strays also attack vampire players, whatever Vampirism's own settings say; it takes effect at once, also on mobs already loaded. Other mobs (creepers, zombified piglins, wither skeletons, bogged) are not changed.")
                .define(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES.key, false));
        TWEAKS.put(Tweak.SKELETONS_ATTACK_WEREWOLVES, b.comment("R213: skeletons and strays also attack werewolf players, whatever Werewolves' own setting says; it takes effect at once, also on mobs already loaded. Does nothing without Werewolves.")
                .define(Tweak.SKELETONS_ATTACK_WEREWOLVES.key, false));
        SPEC = b.build();
    }

    private SupernaturalConfig() {}

    public static boolean riteCompleteSignal() {
        Boolean pinned = riteCompleteOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return RITE_COMPLETE_SIGNAL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static int vampireFeedBlood() {
        try {
            return VAMPIRE_FEED_BLOOD.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_VAMPIRE_FEED_BLOOD;
        }
    }

    /** For tests: pins {@link #riteCompleteSignal()}; null clears it. */
    public static void overrideRiteCompleteSignal(Boolean value) {
        riteCompleteOverride = value;
    }

    /** This side's own file only (a server's, or a client's own), ignoring anything pushed. */
    public static boolean localTweak(Tweak t) {
        Boolean pinned = TWEAK_OVERRIDES.get(t);
        if (pinned != null) {
            return pinned;
        }
        try {
            return TWEAKS.get(t).get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    /** What applies here: the altar tweak is the server's value on a client that was told it; the others are this side's own file (they run on the server only). */
    public static boolean tweak(Tweak t) {
        Boolean pushed = t == Tweak.CLEANSING_ALTAR_DISABLED ? pushedAltarDisabled : null;
        return pushed != null ? pushed : localTweak(t);
    }

    /** For tests: pins one tweak; null clears it. */
    public static void overrideTweak(Tweak t, Boolean value) {
        if (value == null) {
            TWEAK_OVERRIDES.remove(t);
        } else {
            TWEAK_OVERRIDES.put(t, value);
        }
    }

    /** Client side: the server's value of {@code tweak_cleansing_altar_disabled} wins over this client's file until {@link #clearPushed}. */
    public static void applyPushedAltar(boolean disabled) {
        pushedAltarDisabled = disabled;
    }

    public static void clearPushed() {
        pushedAltarDisabled = null;
    }
}
