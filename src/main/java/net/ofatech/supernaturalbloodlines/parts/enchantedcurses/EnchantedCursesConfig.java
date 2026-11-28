package net.ofatech.supernaturalbloodlines.parts.enchantedcurses;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.VisibleForTesting;

/** The Enchanted Curses part's own config file: the two curses (R320 to R324). */
public final class EnchantedCursesConfig {
 public static final String FILE = SupernaturalBloodlines.MODID + "-enchanted_curses.toml";
 public static final ModConfigSpec SPEC;

 private static final ModConfigSpec.BooleanValue INSANITY_ENABLED;
 private static final ModConfigSpec.IntValue INSANITY_INTERVAL_SECONDS;
 private static final ModConfigSpec.BooleanValue SUSCEPTIBILITY_ENABLED;
 private static final ModConfigSpec.IntValue SUSCEPTIBILITY_EXTRA_PERCENT;
 private static final ModConfigSpec.IntValue SUSCEPTIBILITY_MAX_PERCENT;
 private static final Map<String, Object> OVERRIDES = new ConcurrentHashMap<>();

 static {
 ModConfigSpec.Builder b = new ModConfigSpec.Builder();
 b.push("enchanted_curses");
 INSANITY_ENABLED = b.comment("R320: the Curse of Insanity does something. Off: a cursed player sees nothing (the curse is kept, the rite still works, and it does nothing).")
 .define("insanity_enabled", true);
 INSANITY_INTERVAL_SECONDS = b.comment("R320: the base time in seconds between two hallucinations; it shortens with the curse's strength and varies by a third either way.")
 .defineInRange("insanity_interval_seconds", 30, 1, 3600);
 SUSCEPTIBILITY_ENABLED = b.comment("R322: the Curse of Susceptibility does something. Off: a cursed player takes damage as usual.")
 .define("susceptibility_enabled", true);
 SUSCEPTIBILITY_EXTRA_PERCENT = b.comment("R322: the extra damage taken, in percent, for each strength level plus one (25: +25% at strength 0, +50% at 1, +75% at 2).")
 .defineInRange("susceptibility_extra_percent", 25, 0, 100);
 SUSCEPTIBILITY_MAX_PERCENT = b.comment("R322: the most damage a cursed player takes, in percent of normal (200: never more than double). 100 or less: no extra damage.")
 .defineInRange("susceptibility_max_percent", 200, 100, 1000);
 b.pop();
 SPEC = b.build();
 }

 private EnchantedCursesConfig() {}

 /** For tests: pins one setting by its key; null clears it. */
 @VisibleForTesting
 public static void override(String key, Object value) {
 if (value == null) {
 OVERRIDES.remove(key);
 } else {
 OVERRIDES.put(key, value);
 }
 }

 private static boolean bool(String key, ModConfigSpec.BooleanValue value, boolean fallback) {
 if (OVERRIDES.get(key) instanceof Boolean pinned) {
 return pinned;
 }
 try {
 return value.get();
 } catch (IllegalStateException notLoaded) {
 return fallback;
 }
 }

 private static int number(String key, ModConfigSpec.IntValue value, int fallback) {
 if (OVERRIDES.get(key) instanceof Integer pinned) {
 return pinned;
 }
 try {
 return value.get();
 } catch (IllegalStateException notLoaded) {
 return fallback;
 }
 }

 public static boolean insanityEnabled() {
 return bool("insanity_enabled", INSANITY_ENABLED, true);
 }

 public static int insanityIntervalSeconds() {
 return number("insanity_interval_seconds", INSANITY_INTERVAL_SECONDS, 30);
 }

 public static boolean susceptibilityEnabled() {
 return bool("susceptibility_enabled", SUSCEPTIBILITY_ENABLED, true);
 }

 public static int susceptibilityExtraPercent() {
 return number("susceptibility_extra_percent", SUSCEPTIBILITY_EXTRA_PERCENT, 25);
 }

 public static int susceptibilityMaxPercent() {
 return number("susceptibility_max_percent", SUSCEPTIBILITY_MAX_PERCENT, 200);
 }
}