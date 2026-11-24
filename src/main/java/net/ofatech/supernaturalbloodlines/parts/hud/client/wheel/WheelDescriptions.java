package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.util.HashMap;
import net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity.InfinityCompat;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.parts.hud.client.BiteView;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/**
 * The line of text under a power's name in the side panel: what the mod that owns the power says about it. The server's list carries none, so it is read here: Tensura's and
 * Infinity Stones' skills from their skill's own description, Vampirism's and Werewolves' actions from their {@code action.<mod>.<name>.desc} text, our own actions from their
 * note, Great Sage's from our lang file. Empty when there is none. Client only; looked up once per power and language.
 */
final class WheelDescriptions {
    private static final Map<String, String> CACHE = new HashMap<>();

    private WheelDescriptions() {
    }

    static void reload() {
        CACHE.clear();
    }

    /** Development only: the screenshots' demo powers have no real mod text. */
    static volatile Map<String, String> demo;

    static String of(WheelEntry e) {
        Map<String, String> d = demo;
        if (d != null && d.containsKey(e.ref().id().getPath())) {
            return d.get(e.ref().id().getPath());
        }
        // a stone says what its current power does, so the power it is on is part of the key
        String key = InfinityCompat.owns(e) ? e.ref().key() + '#' + e.state().mode() + '#' + e.modeNames().size() : e.ref().key();
        return CACHE.computeIfAbsent(key, k -> lookup(e));
    }

    private static String lookup(WheelEntry e) {
        PowerRef ref = e.ref();
        try {
            switch (ref.source()) {
                case INFINITY:
                    if (InfinityCompat.owns(e)) {
                        return InfinityCompat.description(e);
                    }
                    return ModList.get().isLoaded("tensura") ? TensuraWheelLink.description(ref.id()) : "";
                case TENSURA:
                    return ModList.get().isLoaded("tensura") ? TensuraWheelLink.description(ref.id()) : "";
                case VAMPIRISM, WEREWOLVES:
                    String key = "action." + ref.id().getNamespace() + "." + ref.id().getPath() + ".desc";
                    return I18n.exists(key) ? I18n.get(key) : "";
                case GREAT_SAGE:
                    String own = "supernaturalbloodlines.wheel.greatsage." + ref.id().getPath() + ".desc";
                    return I18n.exists(own) ? I18n.get(own) : "";
                default:
                    if (Loadout.BITE.equals(ref.id())) {
                        return BiteView.note().getString();
                    }
                    return ExtraActions.get(ref.id()).map(a -> a.note() == null ? "" : a.note().getString()).orElse("");
            }
        } catch (RuntimeException | LinkageError unreadable) {
            return "";
        }
    }
}
