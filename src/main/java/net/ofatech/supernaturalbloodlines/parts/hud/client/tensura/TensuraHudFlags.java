package net.ofatech.supernaturalbloodlines.parts.hud.client.tensura;

import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.tensura.client.screen.SettingsScreen;
import io.github.manasmods.tensura.config.client.HudConfig;
import java.util.function.BooleanSupplier;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.hud.HudFlagGuard;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;

/**
 * Hides Tensura's own skill HUD while ours is on, by lowering its in-memory {@code abilities.render} flag (no mixin; its renderer returns early when it
 * is false). The player's value is remembered and given back when ours goes off or the player leaves the world; while Tensura's settings screen is open
 * it has the player's value, because that screen is what makes ManasCore save the config. See {@link HudFlagGuard}. Linked to Tensura's client classes:
 * only reached from {@code HudClient} after {@code ModList.isLoaded("tensura")}.
 */
public final class TensuraHudFlags {
    private static final HudFlagGuard SKILL_HUD = new HudFlagGuard();
    private static BooleanSupplier ourHudShown = () -> false;

    private TensuraHudFlags() {
    }

    /** @param ourHudShown true while our HUD is drawn in any mode (the layer of 13b Task 4 supplies it; until then nothing is hidden) */
    public static void register(BooleanSupplier ourHudShown) {
        TensuraHudFlags.ourHudShown = ourHudShown;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            HudConfig cfg = config();
            if (cfg != null) {
                SKILL_HUD.tick(ourHudShown.getAsBoolean() && HudClientConfig.hideTensuraSkillHud(), cfg.abilities.render).ifPresent(v -> cfg.abilities.render = v);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            HudConfig cfg = config();
            if (cfg != null && e.getNewScreen() instanceof SettingsScreen) {
                SKILL_HUD.screenOpening().ifPresent(v -> cfg.abilities.render = v);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing e) -> {
            HudConfig cfg = config();
            if (cfg != null && e.getScreen() instanceof SettingsScreen) {
                SKILL_HUD.screenClosed(cfg.abilities.render);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            HudConfig cfg = config();
            if (cfg != null) {
                SKILL_HUD.restoreNow(cfg.abilities.render).ifPresent(v -> cfg.abilities.render = v);
            }
        });
    }

    private static HudConfig config() {
        return ConfigRegistry.getConfig(HudConfig.class);
    }
}
