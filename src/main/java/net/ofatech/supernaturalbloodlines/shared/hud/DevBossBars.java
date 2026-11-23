package net.ofatech.supernaturalbloodlines.shared.hud;

/**
 * Development only (the boss bar screenshots): the demo the boss part's client puts on the screen, by the name the shot gave it (for example {@code klaus_shifting} or
 * {@code stack3}), or null for none. The hub's screenshot driver sets it, the bosses part reads it, so neither imports the other.
 */
public final class DevBossBars {
    public static volatile String demo;

    private DevBossBars() {
    }
}
