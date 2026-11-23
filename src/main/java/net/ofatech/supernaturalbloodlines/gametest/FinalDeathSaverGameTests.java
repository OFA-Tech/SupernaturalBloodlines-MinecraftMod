package net.ofatech.supernaturalbloodlines.gametest;

import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.death.FinalDeath;

/**
 * Tensura addons (Re:Zero Return by Death, Infinity Stones' Immortality, Horizons' Phoenix, Ascension) cancel the death event of a player and heal them.
 * They do not know {@link FinalDeath}; a chosen death must still happen.
 */
@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class FinalDeathSaverGameTests {
    private FinalDeathSaverGameTests() {
    }

    private static void kill(ServerPlayer p) {
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
        p.hurt(p.damageSources().genericKill(), 1000f);
    }

    @GameTest(template = "empty")
    public static void aSaverFromAnotherModDoesNotTakeBackAChosenDeath(GameTestHelper helper) {
        ServerPlayer saved = TestPlayers.createMortal(helper);
        ServerPlayer chosen = TestPlayers.createMortal(helper);
        Consumer<LivingDeathEvent> saver = e -> {
            e.setCanceled(true);
            e.getEntity().setHealth(e.getEntity().getMaxHealth());
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, saver);
        try {
            kill(saved);
            helper.assertTrue(!saved.isDeadOrDying(), "control: the saver did not save an ordinary death");
            FinalDeath.run(chosen, () -> kill(chosen));
            helper.assertTrue(chosen.isDeadOrDying(), "a chosen death was taken back by another mod's saver: health " + chosen.getHealth());
        } finally {
            NeoForge.EVENT_BUS.unregister(saver);
        }
        helper.succeed();
    }
}
