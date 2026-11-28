package net.ofatech.supernaturalbloodlines.parts.enchantedcurses;

import net.favouriteless.enchanted.api.curses.Curse;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.curse.InsanityRules;
import net.ofatech.supernaturalbloodlines.core.network.HallucinationPayload;

/**
 * The Curse of Insanity (R320, R321): every so often the cursed player sees one hallucination, the same ones the Hunter's Curse and werewolf venom
 * give (a phantom, a whisper, a darkening). It only draws on the cursed player's own screen: nothing on the server changes, so it says nothing about
 * what any real creature is (R325). The timer counts this curse's own calls (Enchanted's {@code age} is not reliable), is not saved and restarts on login.
 */
public final class InsanityCurse implements Curse {
    private long wait = -1;

    @Override
    public void tick(ServerPlayer target, int strength, long age) {
        if (target.isRemoved() || !EnchantedCursesConfig.insanityEnabled()) {
            return;
        }
        if (wait < 0) {
            wait = InsanityRules.nextDelay(EnchantedCursesConfig.insanityIntervalSeconds(), strength, () -> target.getRandom().nextDouble());
        }
        if (--wait > 0) {
            return;
        }
        HallucinationPayload.send(target, HallucinationPayload.Kind.random(target.getRandom()));
        wait = InsanityRules.nextDelay(EnchantedCursesConfig.insanityIntervalSeconds(), strength, () -> target.getRandom().nextDouble());
    }

    @Override
    public Type<?> type() {
        return PlagueCurses.INSANITY;
    }
}