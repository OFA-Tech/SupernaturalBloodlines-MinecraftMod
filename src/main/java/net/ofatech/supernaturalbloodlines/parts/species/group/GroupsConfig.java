package net.ofatech.supernaturalbloodlines.parts.species.group;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The groups config (plans 20a and 20b). Every key has an Admin row ({@code AdminEntries}). Before the config loads the defaults answer. */
public final class GroupsConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-groups.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue GROUPS_ENABLED;
    private static final ModConfigSpec.BooleanValue CLANS_ENABLED;
    private static final ModConfigSpec.BooleanValue COVENS_ENABLED;
    private static final ModConfigSpec.BooleanValue OPEN_FOUNDING;
    private static final ModConfigSpec.IntValue MAX_MEMBERS;

    static {
        GROUPS_ENABLED = B.comment("Groups on. Off: nobody can found a new clan, coven or brotherhood (packs and existing groups are kept).")
                .define("groups_enabled", true);
        CLANS_ENABLED = B.comment("Vampire leaders can found clans. Off: no new clan; existing clans keep their members.")
                .define("clans_enabled", true);
        COVENS_ENABLED = B.comment("Magic users can found covens. Off: no new coven; existing covens keep their members.")
                .define("covens_enabled", true);
        OPEN_FOUNDING = B.comment("Anyone can found the group their own kind leads, with no need to be an Alpha, a vampire, a magic user or a hunter.")
                .define("open_founding", false);
        MAX_MEMBERS = B.comment("The most members a group can have, the leader not counted. 0 = no limit.")
                .defineInRange("max_members", 0, 0, 100000);
    }

    private record Flag(String key, ModConfigSpec.BooleanValue value) {
        boolean get() {
            return pick(key, value);
        }
    }

    private record Whole(String key, int lo, int hi, ModConfigSpec.IntValue value) {
        int get() {
            return Math.max(lo, Math.min(hi, pick(key, value)));
        }
    }

    private record Real(String key, double lo, double hi, ModConfigSpec.DoubleValue value) {
        double get() {
            return Math.max(lo, Math.min(hi, pick(key, value)));
        }
    }

    private static Flag flag(String key, boolean def, String comment) {
        return new Flag(key, B.comment(comment).define(key, def));
    }

    private static Whole whole(String key, int def, int lo, int hi, String comment) {
        return new Whole(key, lo, hi, B.comment(comment).defineInRange(key, def, lo, hi));
    }

    private static Real real(String key, double def, double lo, double hi, String comment) {
        return new Real(key, lo, hi, B.comment(comment).defineInRange(key, def, lo, hi));
    }

    private static final Flag LINK_ENABLED = flag("link_enabled", true,
            "The circle link: linked witches split a rite's cost. Off: no new circle, every rite is a solo cast and Mastery's Ultimate needs no circle. Nothing already paid is refunded.");
    private static final Real LINK_MEMBER_SHARE = real("link_member_share", 0.70, 0.0, 0.9,
            "The part of a linked rite's altar bill the circle's members pay from their channel meters; the altar pays the rest. Never 1: the altar always pays something.");
    private static final Whole CHANNEL_CAP = whole("channel_cap", 3000, 1, 100000, "A witch's channel meter: the power a member can give a rite before it hurts. Other kinds have channel_other_factor of it.");
    private static final Real CHANNEL_OTHER_FACTOR = real("channel_other_factor", 0.5, 0.0, 1.0, "The share of channel_cap a member who is not a witch type has.");
    private static final Whole CHANNEL_REGEN_MINUTES = whole("channel_regen_minutes", 10, 1, 1440, "Minutes a channel meter takes to refill completely.");
    private static final Whole EP_KEEP_PERCENT = whole("ep_keep_percent", 10, 0, 90, "A member whose share of a rite's EP would leave them under this percent of their base Max EP is left out of the circle.");
    private static final Real POTENCY_MAX_BONUS = real("potency_max_bonus", 1.0, 0.0, 10.0, "The most a large circle adds to a rite's potency (1.0 = up to double).");
    private static final Real POTENCY_FALLOFF = real("potency_falloff", 0.8, 0.0, 0.99, "How fast each extra member adds less potency (closer to 1 = a slower climb).");
    private static final Whole TIER1_MEMBERS = whole("tier1_members", 3, 2, 13, "Linked members for the first circle tier (the Fete des Benedictions and the Moon and Sun rings).");
    private static final Whole TIER2_MEMBERS = whole("tier2_members", 7, 2, 13, "Linked members for the second circle tier (Night Watch).");
    private static final Whole TIER3_MEMBERS = whole("tier3_members", 13, 2, 13, "Linked members for the third circle tier (Great Harvest). 13 is the circle's size limit.");
    private static final Whole LINK_WINDOW_SECONDS = whole("link_window_seconds", 30, 5, 3600, "Seconds after a linked rite starts (or while it runs) in which the anchor's death hurts the linked witches.");
    private static final Whole LINK_DEATH_DAMAGE_PERCENT = whole("link_death_damage_percent", 60, 0, 100, "Percent of their max health the linked members lose when the anchor dies in the window (never below 1 HP).");
    private static final Flag LINK_DEATH_LETHAL = flag("link_death_lethal", false, "The anchor's death kills the linked members instead of hurting them. Off by default: a server owner decides.");
    private static final Whole LINK_SHAKEN_MINUTES = whole("link_shaken_minutes", 30, 0, 1440, "Minutes a member hurt by the anchor's death cannot join any circle.");
    private static final Whole ELDER_MIN_DAYS = whole("elder_min_days", 3, 0, 365, "Days in the coven before a member can be made an Elder.");
    private static final Whole MEMBERS_PER_ELDER = whole("members_per_elder", 4, 1, 100, "One Elder seat per this many coven members (at least one once there are three).");
    private static final Whole VOTE_HOURS = whole("vote_hours", 24, 1, 168, "Hours a coven vote stays open.");
    private static final Whole ELECTION_COOLDOWN_DAYS = whole("election_cooldown_days", 7, 0, 365, "Days between two calls for a new Regent.");
    private static final Whole SHUN_DAYS = whole("shun_days", 7, 1, 365, "Days a shunned witch cannot join any coven.");
    private static final Flag MOONLIGHT_RING_ENABLED = flag("moonlight_ring_enabled", true, "A circle of three can forge a Moon Ring for a werewolf in a pack, for half the silver.");
    private static final Flag SUN_RING_FORGE_ENABLED = flag("sun_ring_forge_enabled", true, "A circle of three can forge a Sun Ring that binds at half power.");
    private static final Flag SUN_RING_DAY_WALKER_ONLY = flag("sun_ring_day_walker_only", false, "The coven Sun Ring only for a Day Walker of a clan. Off: any vampire.");
    private static final Whole MASTERY_ULTIMATE_CIRCLE = whole("mastery_ultimate_circle", 7, 0, 13, "Linked members standing in the circle for the Rite of Mastery's Ultimate tier. 0 = no requirement.");
    private static final Flag GRAND_RITES_ENABLED = flag("grand_rites_enabled", true, "The three grand rites (Fete des Benedictions, Night Watch, Great Harvest) can be cast.");
    private static final Whole GRAND_RITE_COOLDOWN_HOURS = whole("grand_rite_cooldown_hours", 12, 0, 720, "Hours before a coven can cast the same grand rite again.");
    private static final Whole GRAND_RITE_RADIUS = whole("grand_rite_radius", 24, 4, 64, "Blocks around the chalk a grand rite reaches, before potency.");
    private static final Whole FETE_MINUTES = whole("fete_minutes", 10, 1, 120, "Minutes the Fete des Benedictions lasts, before potency.");
    private static final Whole HARVEST_XP_LEVELS = whole("harvest_xp_levels", 30, 0, 1000, "Experience levels the circle's members split to cast the Great Harvest.");
    private static final Whole HARVEST_MAX_BLOCKS = whole("harvest_max_blocks", 2048, 64, 65536, "The most blocks the Great Harvest touches in one cast.");

    static {
        SPEC = B.build();
    }

    private GroupsConfig() {
    }

    private static final java.util.Map<String, Object> OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

    /** For tests, which cannot write the config: pins a key ({@code "open_founding"}); null clears it. */
    public static void override(String key, Object value) {
        if (value == null) {
            OVERRIDES.remove(key);
        } else {
            OVERRIDES.put(key, value);
        }
    }

    private static <T> T pick(String key, ModConfigSpec.ConfigValue<T> v) {
        Object pinned = OVERRIDES.get(key);
        if (pinned != null) {
            @SuppressWarnings("unchecked")
            T t = (T) pinned;
            return t;
        }
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    public static boolean groupsEnabled() {
        return pick("groups_enabled", GROUPS_ENABLED);
    }

    public static boolean clansEnabled() {
        return pick("clans_enabled", CLANS_ENABLED);
    }

    public static boolean covensEnabled() {
        return pick("covens_enabled", COVENS_ENABLED);
    }

    public static boolean openFounding() {
        return pick("open_founding", OPEN_FOUNDING);
    }

    public static int maxMembers() {
        return Math.max(0, pick("max_members", MAX_MEMBERS));
    }

    public static boolean linkEnabled() {
        return LINK_ENABLED.get();
    }

    public static double linkMemberShare() {
        return LINK_MEMBER_SHARE.get();
    }

    public static int channelCap() {
        return CHANNEL_CAP.get();
    }

    public static double channelOtherFactor() {
        return CHANNEL_OTHER_FACTOR.get();
    }

    public static int channelRegenMinutes() {
        return CHANNEL_REGEN_MINUTES.get();
    }

    public static int epKeepPercent() {
        return EP_KEEP_PERCENT.get();
    }

    public static double potencyMaxBonus() {
        return POTENCY_MAX_BONUS.get();
    }

    public static double potencyFalloff() {
        return POTENCY_FALLOFF.get();
    }

    public static int tier1Members() {
        return TIER1_MEMBERS.get();
    }

    /** The three tier sizes are read as a non-decreasing triple: a smaller second or third number is raised to the one before it. */
    public static int tier2Members() {
        return Math.max(tier1Members(), TIER2_MEMBERS.get());
    }

    public static int tier3Members() {
        return Math.max(tier2Members(), TIER3_MEMBERS.get());
    }

    public static int linkWindowSeconds() {
        return LINK_WINDOW_SECONDS.get();
    }

    public static int linkDeathDamagePercent() {
        return LINK_DEATH_DAMAGE_PERCENT.get();
    }

    public static boolean linkDeathLethal() {
        return LINK_DEATH_LETHAL.get();
    }

    public static int linkShakenMinutes() {
        return LINK_SHAKEN_MINUTES.get();
    }

    public static int elderMinDays() {
        return ELDER_MIN_DAYS.get();
    }

    public static int membersPerElder() {
        return MEMBERS_PER_ELDER.get();
    }

    public static int voteHours() {
        return VOTE_HOURS.get();
    }

    public static int electionCooldownDays() {
        return ELECTION_COOLDOWN_DAYS.get();
    }

    public static int shunDays() {
        return SHUN_DAYS.get();
    }

    public static boolean moonlightRingEnabled() {
        return MOONLIGHT_RING_ENABLED.get();
    }

    public static boolean sunRingForgeEnabled() {
        return SUN_RING_FORGE_ENABLED.get();
    }

    public static boolean sunRingDayWalkerOnly() {
        return SUN_RING_DAY_WALKER_ONLY.get();
    }

    public static int masteryUltimateCircle() {
        return MASTERY_ULTIMATE_CIRCLE.get();
    }

    public static boolean grandRitesEnabled() {
        return GRAND_RITES_ENABLED.get();
    }

    public static int grandRiteCooldownHours() {
        return GRAND_RITE_COOLDOWN_HOURS.get();
    }

    public static int grandRiteRadius() {
        return GRAND_RITE_RADIUS.get();
    }

    public static int feteMinutes() {
        return FETE_MINUTES.get();
    }

    public static int harvestXpLevels() {
        return HARVEST_XP_LEVELS.get();
    }

    public static int harvestMaxBlocks() {
        return HARVEST_MAX_BLOCKS.get();
    }
}
