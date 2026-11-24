package net.ofatech.supernaturalbloodlines.parts.species.group;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The groups config (plan 20a). Every key has an Admin row ({@code AdminEntries}). Before the config loads the defaults answer. */
public final class GroupsConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-groups.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue GROUPS_ENABLED;
    private static final ModConfigSpec.BooleanValue CLANS_ENABLED;
    private static final ModConfigSpec.BooleanValue COVENS_ENABLED;
    private static final ModConfigSpec.BooleanValue OPEN_FOUNDING;
    private static final ModConfigSpec.IntValue MAX_MEMBERS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        GROUPS_ENABLED = b.comment("Groups on. Off: nobody can found a new clan, coven or brotherhood (packs and existing groups are kept).")
                .define("groups_enabled", true);
        CLANS_ENABLED = b.comment("Vampire leaders can found clans. Off: no new clan; existing clans keep their members.")
                .define("clans_enabled", true);
        COVENS_ENABLED = b.comment("Magic users can found covens. Off: no new coven; existing covens keep their members.")
                .define("covens_enabled", true);
        OPEN_FOUNDING = b.comment("Anyone can found the group their own kind leads, with no need to be an Alpha, a vampire, a magic user or a hunter.")
                .define("open_founding", false);
        MAX_MEMBERS = b.comment("The most members a group can have, the leader not counted. 0 = no limit.")
                .defineInRange("max_members", 0, 0, 100000);
        SPEC = b.build();
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
}
