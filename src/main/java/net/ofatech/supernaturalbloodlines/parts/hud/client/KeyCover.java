package net.ofatech.supernaturalbloodlines.parts.hud.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import net.ofatech.supernaturalbloodlines.core.hud.CoveredKeys;

/**
 * Unbinds the keys of Tensura, Tensura Slots, Vampirism and Werewolves that the HUD covers, and gives them back. The rules are
 * {@link CoveredKeys}; this class reads the game's binds, applies the plan, saves the options and keeps the record in
 * {@code config/supernaturalbloodlines-hud-keys.json}. Keys are found by name, so a mod that is not installed has no key to cover and nothing
 * is touched. It runs on the first client tick (every mod has registered its keys and the options are loaded by then) and again whenever
 * {@code hide_covered_keys} changes or the Controls screen closes (its "Reset All" puts covered keys back on their defaults).
 * <p>
 * With the part off nothing of it would run, so the main class calls {@link #registerRestoreOnly()} then: the recorded binds come back.
 * Never touched: Tensura's {@code next_mode}, {@code race_ability} and {@code main_gui}, and Vampirism's action wheel, vision and skill screen.
 */
public final class KeyCover {
    public static final String FILE = SupernaturalBloodlines.MODID + "-hud-keys.json";

    /** Tensura's three skill keys, Tensura Slots' three, Vampirism's suck and nine action toggles, and Werewolves' bite. */
    public static final List<String> COVERED = covered();

    private static final Type RECORD_TYPE = new TypeToken<LinkedHashMap<String, String>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Boolean applied;

    private KeyCover() {
    }

    private static List<String> covered() {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ids.add("tensura.keybinding.ability.slot_" + i);
        }
        for (int i = 1; i <= 3; i++) {
            ids.add("tensuraslots.keybinding.secondary_ability.slot_" + i);
        }
        ids.add("keys.vampirism.suck");
        for (int i = 1; i <= 9; i++) {
            ids.add("keys.vampirism.action" + i);
        }
        ids.add("keys.werewolves.bite");
        return List.copyOf(ids);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick(HudClientConfig::hideCoveredKeys));
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

    private static void tick(BooleanSupplier hide) {
        Minecraft mc = Minecraft.getInstance();
        boolean want = hide.getAsBoolean();
        if (mc.options == null || applied != null && applied == want) {
            return;
        }
        applied = want;
        try {
            apply(mc, want);
        } catch (RuntimeException e) {
            SupernaturalBloodlines.LOGGER.warn("Covered keys: could not apply the plan, leaving the keys as they are", e);
        }
    }

    private static void apply(Minecraft mc, boolean hide) {
        Map<String, String> record = readRecord();
        Map<String, KeyMapping> byName = new HashMap<>();
        Map<String, String> current = new HashMap<>();
        Map<String, String> defaults = new HashMap<>();
        for (KeyMapping km : mc.options.keyMappings) {
            byName.put(km.getName(), km);
            current.put(km.getName(), bind(km.getKey(), km.getKeyModifier()));
            defaults.put(km.getName(), bind(km.getDefaultKey(), km.getDefaultKeyModifier()));
        }
        CoveredKeys.Plan plan = CoveredKeys.plan(current, defaults, COVERED, record, hide);
        for (Map.Entry<String, String> change : plan.set().entrySet()) {
            set(byName.get(change.getKey()), change.getValue());
        }
        if (!plan.set().isEmpty()) {
            KeyMapping.resetMapping();
            mc.options.save();
            SupernaturalBloodlines.LOGGER.info("Covered keys: {} {}", hide ? "unbound" : "restored", plan.set().keySet());
        }
        if (!plan.record().equals(record)) {
            writeRecord(plan.record());
        }
    }

    private static String bind(InputConstants.Key key, KeyModifier modifier) {
        return modifier == KeyModifier.NONE ? key.getName() : key.getName() + ":" + modifier.name();
    }

    private static void set(KeyMapping km, String bind) {
        String[] parts = bind.split(":");
        try {
            km.setKeyModifierAndCode(parts.length > 1 ? KeyModifier.valueFromString(parts[1]) : KeyModifier.NONE, InputConstants.getKey(parts[0]));
        } catch (IllegalArgumentException e) {
            SupernaturalBloodlines.LOGGER.warn("Covered keys: {} is not a bind I understand for {}, leaving it", bind, km.getName());
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static Map<String, String> readRecord() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return new LinkedHashMap<>();
        }
        try (Reader in = Files.newBufferedReader(file)) {
            Map<String, String> read = GSON.fromJson(in, RECORD_TYPE);
            return read == null ? new LinkedHashMap<>() : read;
        } catch (IOException | RuntimeException e) {
            // never overwrite what cannot be read: it may hold the player's only copy of their binds
            Path bad = file.resolveSibling(FILE + ".bad");
            try {
                Files.move(file, bad, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailed) {
                SupernaturalBloodlines.LOGGER.warn("Covered keys: cannot move the unreadable record away either", moveFailed);
            }
            SupernaturalBloodlines.LOGGER.warn("Covered keys: {} is unreadable ({}), kept as {}", file, e.toString(), bad.getFileName());
            return new LinkedHashMap<>();
        }
    }

    private static void writeRecord(Map<String, String> record) {
        try {
            Files.writeString(file(), GSON.toJson(record, RECORD_TYPE));
        } catch (IOException e) {
            SupernaturalBloodlines.LOGGER.warn("Covered keys: cannot save the record of the original binds", e);
        }
    }
}
