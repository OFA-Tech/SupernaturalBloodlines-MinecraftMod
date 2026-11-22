package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * What the kit's drawing needs to know about a look: which kit it is ({@code grimoire}, {@code bloodmoon}, {@code tensura}) and five colours, for the flat
 * stand-in of a sprite a resource pack removed and for text on the kit's pictures. A hub theme is one; so is each look of the HUD ({@code HudKit}), which has no theme object. Pure.
 */
public interface KitColors {
    /** The directory of the kit's sprites under {@code kit/}. */
    String kit();

    int paper();

    int ink();

    int muted();

    int accent();

    int good();

    int focusColor();
}