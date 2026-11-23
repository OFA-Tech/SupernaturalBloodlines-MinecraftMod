package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ReduceMotion;
import org.jetbrains.annotations.Nullable;

/**
 * What the client knows of one of our bars: the boss, its chips, its health and the fill's easing, the entity (for the distance that picks the focus), the last hit,
 * and everything that is worked out from them (the card's geometry, the chips' words and widths, the health text, the cut name, the token's form mark), kept until a
 * fact, the card's width or the language changes, so a frame measures and allocates nothing of it. Client only.
 */
final class BossBarInfo {
    private static final int[] NO_CHIPS = new int[0];

    final UUID bar;
    @Nullable
    BossId boss;
    Look look = Look.NONE;
    boolean fallen;
    int hp = 1;
    int max = 1;
    int entity = BossBarInfoPayload.NO_ENTITY;
    /** The tick of the UI clock this player last hit the boss, or {@link BossFocus#NEVER}. */
    long lastHit = BossFocus.NEVER;
    final BossBarMotion.Tween health;
    /** The vanilla bar of this boss (its name), set each time the overlay is read. */
    @Nullable
    LerpingBossEvent event;
    /** The chip lit before the last change (-1 for none) and when the change was seen: the lit one fades in and the old one out. */
    int prevActive = -1;
    double activeSince = Double.NEGATIVE_INFINITY;
    @Nullable
    BossPortraits.Portrait portrait;

    private boolean dirty = true;
    private String lang;
    private int cardW = -1;
    int[] chipTextW = NO_CHIPS;
    final String[] labels = {"", "", ""};
    BossBarLayout.Geometry geo;
    String hpShown = "";
    /** The first letter of the lit chip's word: the token's form mark, so the form is not told by colour alone. Empty for a boss whose chips are not drawn. */
    String mark = "";
    int markW;
    @Nullable
    private Component nameRef;
    String nameShown = "";

    BossBarInfo(BossBarInfoPayload p, double now) {
        this.bar = p.bar();
        this.health = new BossBarMotion.Tween(p.hp() / (float) p.max());
        apply(p, now, true);
    }

    /** A new fact from the server. {@code first}: the card has just appeared, so nothing animates into place. */
    void apply(BossBarInfoPayload p, double now, boolean first) {
        Look next = p.look();
        if (!first && (next.set() != look.set() || next.active() != look.active())) {
            prevActive = next.set() == look.set() ? look.active() : -1;
            activeSince = now;
        }
        look = next;
        boss = p.boss().orElse(null);
        fallen = p.fallen();
        hp = p.hp();
        max = p.max();
        entity = p.entity();
        if (!first) {
            health.target(hp / (float) max, now, ReduceMotion.on());
        }
        portrait = boss == null ? null : BossPortraits.of(boss, next);
        dirty = true;
    }

    /** Forgets what was worked out (the language or the resource packs changed). */
    void dropCaches() {
        dirty = true;
        lang = null;
        nameRef = null;
    }

    /** The share of health, 0 to 1, as the server last said it (the fill eases towards it). */
    float share() {
        return hp / (float) max;
    }

    /** Works out what a frame needs, when something it depends on has changed. */
    void prepare(Font font, int width, Component name) {
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        if (lang == null || (lang != selected && !lang.equals(selected))) {
            lang = selected;
            dirty = true;
        }
        if (dirty || width != cardW || geo == null) {
            int n = look.set().shownChips();
            if (chipTextW.length != n) {
                chipTextW = n == 0 ? NO_CHIPS : new int[n];
            }
            for (int i = 0; i < n; i++) {
                labels[i] = I18n.get(look.set().chipKey(i));
                chipTextW[i] = font.width(labels[i]);
            }
            mark = n == 0 ? "" : FormMark.of(labels[look.active()]);
            markW = font.width(mark);
            cardW = width;
            BossBarLayout.Health health = BossBarLayout.health(hp, max, BossBarLayout.localeOf(lang));
            geo = BossBarLayout.geometry(width, chipTextW, look.active(), font.width(health.full()));
            hpShown = BossBarLayout.healthText(health, geo.hpMaxW(), font::width);
            nameRef = null;
            dirty = false;
        }
        if (nameRef != name) {
            nameRef = name;
            nameShown = LabelFit.cut(name.getString(), geo.nameW() - 16, font::width);
        }
    }
}
