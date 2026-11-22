package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.Set;

/** Which HUD textures of this mod have a picture in the kit's 16 px icons ({@code icons/16/<name>}): the slot draws that one. Another mod's texture is never ours. Pure. */
public final class HudIcons {
    private static final String PREFIX = "textures/gui/hud/";
    private static final String SUFFIX = ".png";
    /** The kit has icons of these. */
    public static final Set<String> OURS = Set.of("bite", "alpha_roar", "unknown");

    /** The other pictures in the HUD's texture folder: not slot icons, so they have no outlined twin. A new picture there must be named in one of the two sets. */
    public static final Set<String> NOT_ICONS = Set.of("blood_empty", "blood_full", "slot_active");

    private HudIcons() {
    }

    /** The kit icon of the texture {@code namespace:path}, or null when it is not one of ours with a kit icon. */
    public static String kitIcon(String namespace, String path, String ownNamespace) {
        if (!ownNamespace.equals(namespace) || !path.startsWith(PREFIX) || !path.endsWith(SUFFIX)) {
            return null;
        }
        String name = path.substring(PREFIX.length(), path.length() - SUFFIX.length());
        return OURS.contains(name) ? name : null;
    }
}