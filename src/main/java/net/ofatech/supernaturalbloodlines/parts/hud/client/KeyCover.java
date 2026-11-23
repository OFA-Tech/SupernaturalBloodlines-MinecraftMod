package net.ofatech.supernaturalbloodlines.parts.hud.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.hud.net.SetLoadoutPayload;
import net.ofatech.supernaturalbloodlines.core.hud.CoverPolicy;
import net.ofatech.supernaturalbloodlines.core.hud.CoverRun;
import net.ofatech.supernaturalbloodlines.core.hud.RecordFile;

/**
 * Unbinds the keys of Tensura, Tensura Slots, Vampirism and Werewolves that the HUD covers, and gives them back. The rules are
 * {@code CoveredKeys} and {@code CoverRun} (core); this class reads the game's binds, applies the plan, saves the options and keeps the record in
 * {@code config/supernaturalbloodlines-hud-keys.json}. Keys are found by name, so a mod that is not installed has no key to cover and nothing
 * is touched. It runs on the first client tick (every mod has registered its keys and the options are loaded by then) and again whenever
 * the cover policy changes ({@code hud_enabled}, {@code hide_covered_keys}, joining or leaving a world whose server has the HUD channel) or the Controls screen closes (its "Reset All" puts covered keys back on their defaults).
 * <p>
 * With the part off nothing of it would run, so the main class calls {@link #registerRestoreOnly()} then: the recorded binds come back.
 * Never touched: Tensura's {@code next_mode} and {@code main_gui}, and Vampirism's skill screen. Once a build has a wheel screen ({@code WheelBuild}), Vampirism's action wheel and vision key, Tensura's race-ability key, Great Sage's menu, assess, scan, magic sense and thought acceleration keys and Infinity's wheel and soul control keys are covered too ({@code CoveredKeyIds.WHEEL_IDS}).
 */
public final class KeyCover {
    public static final String FILE = SupernaturalBloodlines.MODID + "-hud-keys.json";

    private static final RecordFile RECORD = new RecordFile(FMLPaths.CONFIGDIR.get().resolve(FILE), System::currentTimeMillis,
            m -> SupernaturalBloodlines.LOGGER.warn("Covered keys: {}", m));
    private static Boolean applied;
    private static boolean covering;

    private KeyCover() {
    }

    private static List<String> covered() {
        return net.ofatech.supernaturalbloodlines.core.hud.CoveredKeyIds.all(HudClientConfig.sprintLeap(), net.ofatech.supernaturalbloodlines.core.wheel.WheelBuild.screenPresent(),
                net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity.InfinityCompat.state() == net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.State.ACTIVE,
                net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity.InfinityCompat.soulControlCovered());
    }

    /** The list the keys were last applied with: when {@code sprint_leap} changes, the plan runs again so the leap key is restored or covered at once. */
    private static List<String> appliedList;

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick(KeyCover::wantCover));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing e) -> {
            if (e.getScreen() instanceof KeyBindsScreen) {
                applied = null;
            }
        });
    }

    /** The part is off: put back what a run with it on took away, once. */
    public static void registerRestoreOnly() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick(() -> false));
    }

    /** The keys are taken only while the HUD can take over (see {@link CoverPolicy}): in a world whose server has the HUD channel. */
    private static boolean wantCover() {
        Minecraft mc = Minecraft.getInstance();
        return CoverPolicy.cover(HudClientConfig.hudEnabled(), HudClientConfig.hideCoveredKeys(), mc.player != null && mc.level != null,
                HudSend.canSend(SetLoadoutPayload.TYPE));
    }

    /** Whether {@code name} is covered now: in the list the keys were last applied with (the leap key drops out while {@code sprint_leap} is off). */
    public static boolean isCovered(String name) {
        return (appliedList != null ? appliedList : covered()).contains(name);
    }

    /** True while covering is in effect (the option is on and the record could be kept): only then are the keys hidden from Controls. */
    public static boolean covering() {
        return covering;
    }

    private static void tick(BooleanSupplier hide) {
        Minecraft mc = Minecraft.getInstance();
        boolean want = hide.getAsBoolean();
        List<String> list = covered();
        if (mc.options == null || applied != null && applied == want && list.equals(appliedList)) {
            return;
        }
        applied = want;
        appliedList = list;
        try {
            apply(mc, want);
        } catch (RuntimeException e) {
            covering = false;
            SupernaturalBloodlines.LOGGER.warn("Covered keys: could not apply the plan, leaving the keys as they are", e);
        }
    }

    private static void apply(Minecraft mc, boolean hide) {
        Map<String, KeyMapping> byName = new HashMap<>();
        Map<String, String> current = new HashMap<>();
        Map<String, String> defaults = new HashMap<>();
        for (KeyMapping km : mc.options.keyMappings) {
            byName.put(km.getName(), km);
            current.put(km.getName(), bind(km.getKey(), km.getKeyModifier()));
            defaults.put(km.getName(), bind(km.getDefaultKey(), km.getDefaultKeyModifier()));
        }
        CoverRun.Keys keys = new CoverRun.Keys() {
            @Override
            public Map<String, String> current() {
                return current;
            }

            @Override
            public Map<String, String> defaults() {
                return defaults;
            }

            @Override
            public boolean set(String id, String bind) {
                return KeyCover.set(byName.get(id), bind);
            }

            @Override
            public void save() {
                KeyMapping.resetMapping();
                mc.options.save();
            }
        };
        CoverRun.Result result = CoverRun.run(keys, RECORD, appliedList, hide);
        covering = hide && result.outcome() == CoverRun.Outcome.DONE;
        switch (result.outcome()) {
            case BLOCKED -> SupernaturalBloodlines.LOGGER.warn("Covered keys: not covering, an unreadable record is waiting ({}.bad-*); see the wiki, Key binds", FILE);
            case RECORD_NOT_SAVED -> SupernaturalBloodlines.LOGGER.warn("Covered keys: the record of the original binds could not be saved; {}",
                    hide ? "no key was touched" : "the keys were restored but a stale record stays");
            default -> {
            }
        }
        if (!result.changed().isEmpty()) {
            SupernaturalBloodlines.LOGGER.info("Covered keys: {} {}", hide ? "unbound" : "restored", result.changed());
        }
    }

    private static String bind(InputConstants.Key key, KeyModifier modifier) {
        return modifier == KeyModifier.NONE ? key.getName() : key.getName() + ":" + modifier.name();
    }

    private static boolean set(KeyMapping km, String bind) {
        String[] parts = bind.split(":");
        try {
            km.setKeyModifierAndCode(parts.length > 1 ? KeyModifier.valueFromString(parts[1]) : KeyModifier.NONE, InputConstants.getKey(parts[0]));
            return true;
        } catch (IllegalArgumentException e) {
            SupernaturalBloodlines.LOGGER.warn("Covered keys: {} is not a bind I understand for {}, leaving it (its record stays)", bind, km.getName());
            return false;
        }
    }
}