package net.ofatech.supernaturalbloodlines.core.group;

/** Which sprite (`icons/16/<name>`, `icons/32/<name>`) stands for the viewer's group: its type's own, the paw of a pack for someone in no group. Pure. */
public final class GroupIcons {
    private GroupIcons() {
    }

    public static String name(GroupType type, boolean inGroup) {
        if (!inGroup) {
            return "pack";
        }
        return switch (type) {
            case PACK -> "pack";
            case COVEN -> "coven";
            case CLAN -> "clan";
            case BROTHERHOOD -> "brotherhood";
        };
    }
}