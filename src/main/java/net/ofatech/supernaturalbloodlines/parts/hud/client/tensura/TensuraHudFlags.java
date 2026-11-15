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
    /** One in-memory flag of Tensura's HudConfig that we lower while something of ours replaces it. */
    private record Flag(HudFlagGuard guard, java.util.function.Function<HudConfig, Boolean> read, java.util.function.BiConsumer<HudConfig, Boolean> write,
                        BooleanSupplier want) {
        void whileManasScreen(HudConfig cfg) {
            guard.screenOpening().ifPresent(v -> write.accept(cfg, v));
        }

        void tick(HudConfig cfg) {
            guard.screenClosed(read.apply(cfg));
            guard.tick(want.getAsBoolean(), read.apply(cfg)).ifPresent(v -> write.accept(cfg, v));
        }

        void restore(HudConfig cfg) {
            guard.restoreNow(read.apply(cfg)).ifPresent(v -> write.accept(cfg, v));
        }
    }

    private static final java.util.List<Flag> FLAGS = new java.util.ArrayList<>();

    private TensuraHudFlags() {
    }

    /**
     * @param ourHudShown true while our HUD is drawn in any mode (hides Tensura's skill HUD, {@code abilities.render})
     * @param hideFood    true while our blood replaces Tensura's food number ({@code decorations.food.render}); {@code () -> false} without Vampirism
     */
    public static void register(BooleanSupplier ourHudShown, BooleanSupplier hideFood) {
        FLAGS.add(new Flag(new HudFlagGuard(), c -> c.abilities.render, (c, v) -> c.abilities.render = v,
                () -> ourHudShown.getAsBoolean() && HudClientConfig.hideTensuraSkillHud()));
        FLAGS.add(new Flag(new HudFlagGuard(), c -> c.decorations.food.render, (c, v) -> c.decorations.food.render = v, hideFood));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            HudConfig cfg = config();
            if (cfg == null) {
                return;
            }
            boolean manas = manasScreenOpen(Minecraft.getInstance().screen);
            for (Flag f : FLAGS) {
                if (manas) {
                    f.whileManasScreen(cfg);
                } else {
                    f.tick(cfg);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            HudConfig cfg = config();
            if (cfg != null && manasScreenOpen(e.getNewScreen())) {
                FLAGS.forEach(f -> f.whileManasScreen(cfg));
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
            FLAGS.forEach(f -> f.restore(cfg));
        }
    }

    private static HudConfig config() {
        return ConfigRegistry.getConfig(HudConfig.class);
    }
}