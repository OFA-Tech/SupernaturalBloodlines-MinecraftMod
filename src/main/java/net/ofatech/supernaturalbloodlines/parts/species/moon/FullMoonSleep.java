package net.ofatech.supernaturalbloodlines.parts.species.moon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.ofatech.supernaturalbloodlines.core.moon.SleepRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * No sleeping on a full moon (R141, {@code full_moon_sleep}). A bed refuses the player from dusk to the morning of a phase-0 day, in the Overworld,
 * and says so on the action bar. The event only fires when a player lies down, so someone already asleep is not woken; creative players are left
 * alone. It needs no Werewolves: with the mode {@code werewolves} it asks Vampirism's faction.
 */
public final class FullMoonSleep {
    public static final String MESSAGE = "supernaturalbloodlines.moon.no_sleep";

    private FullMoonSleep() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FullMoonSleep::onSleep);
    }

    private static void onSleep(CanPlayerSleepEvent e) {
        ServerPlayer p = e.getEntity();
        if (e.getProblem() == null && blocked(p)) {
            e.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            p.displayClientMessage(Component.translatable(MESSAGE), true);
        }
    }

    /** Whether {@code p} may not use a bed now. */
    public static boolean blocked(ServerPlayer p) {
        return !p.isCreative() && SleepRules.blocked(SpeciesConfig.fullMoonSleep(), VampirismSupernatural.isWerewolf(p),
                p.level().dimensionType().hasFixedTime(), p.level().getMoonPhase(), p.level().getDayTime());
    }
}
