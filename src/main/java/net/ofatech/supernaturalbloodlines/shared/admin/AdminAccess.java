package net.ofatech.supernaturalbloodlines.shared.admin;

/** Who may use the admin path: permission level 2 or more. Pure; the server asks it again for every payload. */
public final class AdminAccess {
    public static final int OP_LEVEL = 2;

    private AdminAccess() {}

    public static boolean allowed(int permissionLevel) {
        return permissionLevel >= OP_LEVEL;
    }
}
