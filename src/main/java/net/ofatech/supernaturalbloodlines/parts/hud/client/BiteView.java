package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.hud.BiteAction;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import org.jetbrains.annotations.Nullable;

/**
 * What the Bite slot shows: the decision {@code BiteRules} would make for the local player's form, from the same rules the server uses (the server stays
 * the authority and refuses with a reason when the client's guess was wrong; the client cannot run the turning rules, so it guesses "the turning bite" for
 * anyone in the turning who is not a werewolf). The cooldown is the server's 20 ticks, counted from the press.
 */
public final class BiteView {
    private static long lastPress = Long.MIN_VALUE;

    private BiteView() {
    }

    /** The slot key of a Bite slot went down. */
    public static void pressed() {
        var level = Minecraft.getInstance().level;
        lastPress = level == null ? Long.MIN_VALUE : level.getGameTime();
    }

    static BiteAction action() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? BiteAction.NONE : BiteService.actionFor(p);
    }

    /** What Bite does for the player's form now, for the hub's Loadout tab and the Alt tooltip. */
    @Nullable
    static Component note() {
        return Component.translatable(BiteService.KEY + "tooltip." + action().name().toLowerCase(Locale.ROOT));
    }

    /** The lang key explaining why Bite does nothing for this form, null when it does something. */
    @Nullable
    static String greyed() {
        return action() == BiteAction.NONE ? BiteService.KEY + (BiteService.usable() ? "none" : "needs_vampirism") : null;
    }

    public static SlotView slot(String key) {
        var level = Minecraft.getInstance().level;
        long since = level == null || lastPress == Long.MIN_VALUE ? BiteService.COOLDOWN_TICKS : level.getGameTime() - lastPress;
        int left = (int) Math.max(0, BiteService.COOLDOWN_TICKS - since);
        return new SlotView(ClientLoadout.BITE_ICON, key, left / (float) BiteService.COOLDOWN_TICKS, (left + 19) / 20, false, greyed(),
                Component.translatable(BiteService.KEY + "name").getString(), note().getString());
    }
}
