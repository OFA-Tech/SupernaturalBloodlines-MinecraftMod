package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import org.jetbrains.annotations.VisibleForTesting;

/** The artifacts part's own config file. */
public final class ArtifactsConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-artifacts.toml";
    public static final ModConfigSpec SPEC;
    public static final int DEFAULT_COMPASS_DOPPELGANGER_RANGE = 512;
    public static final int DEFAULT_WARD_RADIUS = 10;
    public static final int DEFAULT_WARD_MAX_PER_OWNER = 3;
    public static final int DEFAULT_WARD_MAX_INVITES = 16;
    public static final int DEFAULT_WARD_EXPEL_SECONDS = 5;

    private static final ModConfigSpec.IntValue COMPASS_DOPPELGANGER_RANGE;
    private static final ModConfigSpec.BooleanValue WARD_ENABLED;
    private static final ModConfigSpec.IntValue WARD_RADIUS;
    private static final ModConfigSpec.IntValue WARD_MAX_PER_OWNER;
    private static final ModConfigSpec.IntValue WARD_MAX_INVITES;
    private static final ModConfigSpec.BooleanValue WARD_FALLS_ON_OWNER_DEATH;
    private static final ModConfigSpec.BooleanValue WARD_AFFECTS_MOBS;
    private static final ModConfigSpec.BooleanValue WARD_BLOCKS_WEREWOLVES;
    private static final ModConfigSpec.BooleanValue WARD_EXPEL_ON_REVOKE;
    private static final ModConfigSpec.IntValue WARD_EXPEL_SECONDS;
    private static final ModConfigSpec.BooleanValue WARD_SPOKEN_INVITES;
    private static final ModConfigSpec.BooleanValue WARD_DOOR_ENABLED;
    private static final ModConfigSpec.BooleanValue WARD_ALERT_OWNER;
    private static final ModConfigSpec.BooleanValue WARD_USE_CLAIMS;
    private static volatile Integer compassRangeOverride;
    private static final Map<String, Object> OVERRIDES = new ConcurrentHashMap<>();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("artifacts");
        COMPASS_DOPPELGANGER_RANGE = b.comment("How far, in blocks, the Vampire Compass in Doppelgänger mode finds a doppelgänger villager (only villagers in loaded chunks are found). Players with the gene are found anywhere in the dimension.")
                .defineInRange("compass_doppelganger_range", DEFAULT_COMPASS_DOPPELGANGER_RANGE, 1, 30_000_000);
        WARD_ENABLED = b.comment("R300: the Rites of the Ward (witch types). Off: every ward stands down at once (none is deleted) and the rites are refused.")
                .define("ward_enabled", true);
        WARD_RADIUS = b.comment("R300: an area ward's half-size in blocks, from the altar it was raised on (a cube of 2 x this + 1).")
                .defineInRange("ward_radius", DEFAULT_WARD_RADIUS, 1, WardRules.MAX_RADIUS);
        WARD_MAX_PER_OWNER = b.comment("R301: how many standing wards (areas and doors together) one owner may have.")
                .defineInRange("ward_max_per_owner", DEFAULT_WARD_MAX_PER_OWNER, 1, net.ofatech.supernaturalbloodlines.core.ward.WardRules.MAX_PER_OWNER_LIMIT);
        WARD_MAX_INVITES = b.comment("R306: how many names a ward's invite list holds (the owner counts).")
                .defineInRange("ward_max_invites", DEFAULT_WARD_MAX_INVITES, 1, 64);
        WARD_FALLS_ON_OWNER_DEATH = b.comment("R307: a ward falls when its owner dies, and when its owner becomes vampiric (turning means dying first). Off: it keeps standing.")
                .define("ward_falls_on_owner_death", true);
        WARD_AFFECTS_MOBS = b.comment("R302: vampire mobs are turned away by a ward too (a mob is never invited).")
                .define("ward_affects_mobs", true);
        WARD_BLOCKS_WEREWOLVES = b.comment("R302: werewolves also need an invitation. Off (the show's rule): only vampiric types are turned away.")
                .define("ward_blocks_werewolves", false);
        WARD_EXPEL_ON_REVOKE = b.comment("R308: withdrawing an invitation throws out a vampire who is inside. Off: it only stops them coming back in.")
                .define("ward_expel_on_revoke", true);
        WARD_EXPEL_SECONDS = b.comment("R303: how long a turned-away entity may stay inside before it is moved out (the push cannot always clear a closed room).")
                .defineInRange("ward_expel_seconds", DEFAULT_WARD_EXPEL_SECONDS, 1, 300);
        WARD_SPOKEN_INVITES = b.comment("R305: the owner saying 'come in' or 'get out' in chat invites or withdraws the player they look at or stand nearest to.")
                .define("ward_spoken_invites", true);
        WARD_DOOR_ENABLED = b.comment("R300: the Rite of the Door Ward (a single door cell), the small form of the ward.")
                .define("ward_door_enabled", true);
        WARD_ALERT_OWNER = b.comment("R325: tell the owner 'your ward was tested' when somebody is turned away. It never says who or what.")
                .define("ward_alert_owner", false);
        WARD_USE_CLAIMS = b.comment("R301: with Open Parties and Claims, a ward may only be raised on ground that is unclaimed, the owner's or their party's. Never defines the home.")
                .define("ward_use_claims", true);
        b.pop();
        SPEC = b.build();
    }

    private ArtifactsConfig() {}

    /** For tests: pins {@link #compassDoppelgangerRange()}; null clears it. */
    @VisibleForTesting
    public static void overrideCompassDoppelgangerRange(Integer value) {
        compassRangeOverride = value;
    }

    /** For tests: pins one ward setting by its key ({@code ward_radius}, ...); null clears it. */
    @VisibleForTesting
    public static void override(String key, Object value) {
        if (value == null) {
            OVERRIDES.remove(key);
        } else {
            OVERRIDES.put(key, value);
        }
    }

    public static int compassDoppelgangerRange() {
        Integer pinned = compassRangeOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return COMPASS_DOPPELGANGER_RANGE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_COMPASS_DOPPELGANGER_RANGE;
        }
    }

    private static boolean bool(String key, ModConfigSpec.BooleanValue value, boolean fallback) {
        Object pinned = OVERRIDES.get(key);
        if (pinned instanceof Boolean b) {
            return b;
        }
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;
        }
    }

    private static int number(String key, ModConfigSpec.IntValue value, int fallback) {
        Object pinned = OVERRIDES.get(key);
        if (pinned instanceof Integer i) {
            return i;
        }
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;
        }
    }

    public static boolean wardEnabled() {
        return bool("ward_enabled", WARD_ENABLED, true);
    }

    public static int wardRadius() {
        return WardRules.clampRadius(number("ward_radius", WARD_RADIUS, DEFAULT_WARD_RADIUS));
    }

    public static int wardMaxPerOwner() {
        return number("ward_max_per_owner", WARD_MAX_PER_OWNER, DEFAULT_WARD_MAX_PER_OWNER);
    }

    public static int wardMaxInvites() {
        return number("ward_max_invites", WARD_MAX_INVITES, DEFAULT_WARD_MAX_INVITES);
    }

    public static boolean wardFallsOnOwnerDeath() {
        return bool("ward_falls_on_owner_death", WARD_FALLS_ON_OWNER_DEATH, true);
    }

    public static boolean wardAffectsMobs() {
        return bool("ward_affects_mobs", WARD_AFFECTS_MOBS, true);
    }

    public static boolean wardBlocksWerewolves() {
        return bool("ward_blocks_werewolves", WARD_BLOCKS_WEREWOLVES, false);
    }

    public static boolean wardExpelOnRevoke() {
        return bool("ward_expel_on_revoke", WARD_EXPEL_ON_REVOKE, true);
    }

    public static int wardExpelSeconds() {
        return number("ward_expel_seconds", WARD_EXPEL_SECONDS, DEFAULT_WARD_EXPEL_SECONDS);
    }

    public static boolean wardSpokenInvites() {
        return bool("ward_spoken_invites", WARD_SPOKEN_INVITES, true);
    }

    public static boolean wardDoorEnabled() {
        return bool("ward_door_enabled", WARD_DOOR_ENABLED, true);
    }

    public static boolean wardAlertOwner() {
        return bool("ward_alert_owner", WARD_ALERT_OWNER, false);
    }

    public static boolean wardUseClaims() {
        return bool("ward_use_claims", WARD_USE_CLAIMS, true);
    }
}
