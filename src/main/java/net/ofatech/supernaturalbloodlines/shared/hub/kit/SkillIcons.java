package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.Map;

/** The sprite each of our skills wears in the radial tree: one per skill id (any case: the files say S-VAM-1), by what the skill is about, SPARK for one this table does not know. Pure. */
public final class SkillIcons {
    private static final Map<String, Icons> BY_ID = Map.ofEntries(
            Map.entry("s-hum-1", Icons.HEART), Map.entry("s-hum-2", Icons.SHIELD), Map.entry("s-hum-3", Icons.BOOT),
            Map.entry("s-hun-1", Icons.BOLT), Map.entry("s-hun-2", Icons.EYE), Map.entry("s-hun-3", Icons.SHIELD), Map.entry("s-hun-4", Icons.EYE),
            Map.entry("s-hun-5", Icons.CROWN),
            Map.entry("s-hyb-1", Icons.DROP), Map.entry("s-hyb-2", Icons.FANG),
            Map.entry("s-ori-1", Icons.DROP), Map.entry("s-ori-2", Icons.VIAL), Map.entry("s-ori-3", Icons.VOICE), Map.entry("s-ori-4", Icons.SPARK),
            Map.entry("s-sip-1", Icons.DROP), Map.entry("s-sip-2", Icons.BOLT), Map.entry("s-sip-3", Icons.HEART), Map.entry("s-sip-4", Icons.KEY),
            Map.entry("s-sip-5", Icons.SPARK),
            Map.entry("s-vam-1", Icons.FANG), Map.entry("s-vam-2", Icons.BOOT), Map.entry("s-vam-3", Icons.VOICE), Map.entry("s-vam-4", Icons.SHIELD),
            Map.entry("s-vam-5", Icons.DROP), Map.entry("s-vam-6", Icons.CLAW), Map.entry("s-vam-7", Icons.SUN),
            Map.entry("s-wit-1", Icons.SPARK), Map.entry("s-wit-2", Icons.SPARK), Map.entry("s-wit-3", Icons.SHIELD), Map.entry("s-wit-4", Icons.VOICE),
            Map.entry("s-wit-5", Icons.EYE), Map.entry("s-wit-6", Icons.VOICE), Map.entry("s-wit-7", Icons.SHIELD),
            Map.entry("s-wol-1", Icons.SHIELD), Map.entry("s-wol-2", Icons.MOON_FULL), Map.entry("s-wol-3", Icons.EYE), Map.entry("s-wol-4", Icons.VIAL),
            Map.entry("s-wol-5", Icons.HEART), Map.entry("s-wol-6", Icons.VOICE), Map.entry("s-wol-7", Icons.MOON_FULL), Map.entry("s-wol-8", Icons.CLAW));

    private SkillIcons() {
    }

    public static Icons of(String skillId) {
        return BY_ID.getOrDefault(skillId.toLowerCase(java.util.Locale.ROOT), Icons.SPARK);
    }

    public static boolean known(String skillId) {
        return BY_ID.containsKey(skillId.toLowerCase(java.util.Locale.ROOT));
    }
}
