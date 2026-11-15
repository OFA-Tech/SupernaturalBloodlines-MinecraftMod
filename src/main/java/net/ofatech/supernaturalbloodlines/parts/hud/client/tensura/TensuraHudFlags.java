package net.ofatech.supernaturalbloodlines.parts.hud.client.tensura;

import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.tensura.config.client.HudConfig;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.hud.HudFlagGuard;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;

/**
 * Hides Tensura's own skill HUD while ours is on, by lowering its in-memory {@code abilities.render} flag (no mixin; its renderer returns early when it
 * is false). The player's value is remembered and given back when ours goes off, when the world is left and when the game shuts down.
 * <p>
 * <b>Where a lowered flag could be saved as false</b> (javap of Tensura and ManasCore): ManasCore saves a config only when someone calls
 * {@code ManasConfig.save()} or {@code ConfigRegistry.saveAllConfigs()}; in Tensura the only callers that reach {@code HudConfig.save()} are
 * {@code SettingsOptions.saveAll} (from {@code SettingsScreen} and its buttons), {@code OverlayHandler.savePositionById} and {@code saveScaleById}
 * (both from {@code SettingsScreen}). ManasCore itself calls {@code saveAllConfigs} from nowhere. So, as a rule, <b>while any screen of Tensura or
 * ManasCore ({@code io.github.manasmods.*}) is open the player's real value is handed back</b>, and the flag is lowered again the tick after the last
 * such screen closed; another mod that calls {@code saveAllConfigs} from one of its own screens or from a timer is not covered, which is why the flag
 * is also handed back on logout and on {@code GameShuttingDownEvent}. See {@link HudFlagGuard}. Linked to Tensura's client classes: only reached from
 * {@code HudClient} after {@code ModList.isLoaded("tensura")}.
 */
public final class TensuraHudFlags {
    private static final HudFlagGuard SKILL_HUD = new HudFlagGuard();

    private TensuraHudFlags() {
    }

    /** @param ourHudShown true while our HUD is drawn in any mode */
    public static void register(BooleanSupplier ourHudShown) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            HudConfig cfg = config();
            if (cfg == null) {
                return;
            }
            if (manasScreenOpen(Minecraft.getInstance().screen)) {
                SKILL_HUD.screenOpening().ifPresent(v -> cfg.abilities.render = v);
                return;
            }
            SKILL_HUD.screenClosed(cfg.abilities.render);
            SKILL_HUD.tick(ourHudShown.getAsBoolean() && HudClientConfig.hideTensuraSkillHud(), cfg.abilities.render).ifPresent(v -> cfg.abilities.render = v);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            HudConfig cfg = config();
            if (cfg != null && manasScreenOpen(e.getNewScreen())) {
                SKILL_HUD.screenOpening().ifPresent(v -> cfg.abilities.render = v);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> restore());
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent e) -> restore());
    }

    private static boolean manasScreenOpen(Screen s) {
        return s != null && s.getClass().getName().startsWith("io.github.manasmods.");
    }

    private static void restore() {
        HudConfig cfg = config();
        if (cfg != null) {
            SKILL_HUD.restoreNow(cfg.abilities.render).ifPresent(v -> cfg.abilities.render = v);
        }
    }
    private static HudConfig config() {
        return ConfigRegistry.getConfig(HudConfig.class);
    }
}
