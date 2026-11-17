package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminSettingsTest {
    private static final String FILE = "admin-test.toml";
    private static final SettingEntry N = SettingEntry.of(FILE, "t", "grp.n", SettingKind.INT);
    private static final SettingEntry LIST = SettingEntry.of(FILE, "t", "list", SettingKind.LIST);
    private static final SettingEntry SWITCH = SettingEntry.of(FILE, "t", "grp.switch", SettingKind.BOOL).needsRestart();
    private static final SettingEntry SECRET = SettingEntry.of(FILE, "t", "secret", SettingKind.BOOL).hide("reserved");
    private static final SettingEntry RULE = SettingEntry.rule("testRule", SettingKind.GAMERULE_INT);
    private static final List<SettingEntry> KEYS = List.of(N, LIST, SWITCH, SECRET);
    private static final List<SettingEntry> RULES = List.of(RULE);

    private ModConfigSpec spec;
    private int saved;

    /** Writes into the spec's loaded config like the real one, and logs what it was asked to do. */
    private final class Recording implements AdminSettings.Writer {
        final List<String> calls = new ArrayList<>();
        boolean failSave;

        @Override
        @SuppressWarnings("unchecked")
        public void set(ModConfigSpec.ConfigValue<?> value, Object boxed) {
            calls.add("set " + String.join(".", value.getPath()));
            ((ModConfigSpec.ConfigValue<Object>) value).set(boxed);
        }

        @Override
        public void save(ModConfigSpec s) {
            calls.add("save");
            if (failSave) {
                throw new IllegalStateException("disk full");
            }
            saved++;
        }

        @Override
        public void setRule(SettingEntry rule, Object boxed) {
            calls.add("rule " + rule.id() + " " + boxed);
        }
    }

    @BeforeEach
    void load() {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("grp");
        b.defineInRange("n", 5, 1, 10);
        b.define("switch", true);
        b.pop();
        b.defineListAllowEmpty("list", List.of("a=1"), () -> "", o -> o instanceof String);
        b.define("secret", true);
        spec = b.build();
        CommentedConfig config = CommentedConfig.inMemory();
        spec.correct(config);
        try {
            // FML's LoadedConfig is sealed and package private; the test never calls its save
            var ctor = Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            spec.acceptConfig((IConfigSpec.ILoadedConfig) ctor.newInstance(config, null, null));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        AdminSpecs.register(FILE, spec);
        AdminSpecs.lineCheck(LIST.id(), line -> line.matches("[a-z]+=[0-9]+"));
        saved = 0;
    }

    @AfterEach
    void unload() {
        AdminSpecs.forget(FILE);
    }

    private int value() {
        return ((ModConfigSpec.ConfigValue<Integer>) AdminSpecs.value(N).orElseThrow()).get();
    }

    @Test
    void aLevelOneSenderNeverReachesTheWriter() {
        Recording w = new Recording();
        for (int level : new int[] {-1, 0, 1}) {
            assertEquals(AdminSettings.Status.NOT_OP, AdminSettings.apply(KEYS, RULES, level, N.id(), new SettingValue.Int(7), w).status());
            assertEquals(AdminSettings.Status.NOT_OP, AdminSettings.apply(KEYS, RULES, level, RULE.id(), new SettingValue.Int(7), w).status());
        }
        assertTrue(w.calls.isEmpty(), "writer calls " + w.calls);
        assertEquals(5, value());
    }

    @Test
    void anUnknownOrHiddenIdNeverReachesTheWriter() {
        Recording w = new Recording();
        assertEquals(AdminSettings.Status.UNKNOWN, AdminSettings.apply(KEYS, RULES, 2, FILE + ":nope", new SettingValue.Int(7), w).status());
        assertEquals(AdminSettings.Status.UNKNOWN, AdminSettings.apply(KEYS, RULES, 2, "hud-client.toml:mode", new SettingValue.Text("AUTO"), w).status());
        assertEquals(AdminSettings.Status.HIDDEN, AdminSettings.apply(KEYS, RULES, 2, SECRET.id(), new SettingValue.Bool(false), w).status());
        assertTrue(w.calls.isEmpty(), "writer calls " + w.calls);
    }

    @Test
    void anInvalidValueNeverReachesTheWriterAndTheOldValueStays() {
        Recording w = new Recording();
        AdminSettings.Result r = AdminSettings.apply(KEYS, RULES, 2, N.id(), new SettingValue.Int(11), w);
        assertEquals(AdminSettings.Status.INVALID, r.status());
        assertEquals(SettingRules.KEY + "range", r.refusalKey());
        assertEquals(AdminSettings.Status.INVALID, AdminSettings.apply(KEYS, RULES, 2, N.id(), new SettingValue.Text("7"), w).status());
        assertEquals(AdminSettings.Status.INVALID, AdminSettings.apply(KEYS, RULES, 2, LIST.id(), new SettingValue.Lines(List.of("a=1", "bad")), w).status());
        assertTrue(w.calls.isEmpty(), "writer calls " + w.calls);
        assertEquals(5, value());
    }

    @Test
    void aValidValueIsSetThenSavedOnceInThatOrder() {
        Recording w = new Recording();
        AdminSettings.Result r = AdminSettings.apply(KEYS, RULES, 2, N.id(), new SettingValue.Int(7), w);
        assertTrue(r.ok());
        assertFalse(r.restartRequired());
        assertEquals(List.of("set grp.n", "save"), w.calls);
        assertEquals(7, value());
        assertEquals(1, saved);
    }

    @Test
    void aListIsWrittenWhenEveryLinePasses() {
        Recording w = new Recording();
        assertTrue(AdminSettings.apply(KEYS, RULES, 3, LIST.id(), new SettingValue.Lines(List.of("a=1", "bc=22")), w).ok());
        assertEquals(List.of("set list", "save"), w.calls);
    }

    @Test
    void aPartSwitchSaysARestartIsNeeded() {
        Recording w = new Recording();
        AdminSettings.Result r = AdminSettings.apply(KEYS, RULES, 2, SWITCH.id(), new SettingValue.Bool(false), w);
        assertTrue(r.ok());
        assertTrue(r.restartRequired());
    }

    @Test
    void aFailedSaveIsReportedAndTheOldValueGoesBack() {
        Recording w = new Recording();
        w.failSave = true;
        AdminSettings.Result r = AdminSettings.apply(KEYS, RULES, 2, N.id(), new SettingValue.Int(7), w);
        assertEquals(AdminSettings.Status.SAVE_FAILED, r.status());
        assertEquals("disk full", r.detail());
        assertEquals(5, value(), "memory and file agree again");
    }

    @Test
    void aKeyOfAPartThatIsOffIsNotLoaded() {
        Recording w = new Recording();
        AdminSpecs.forget(FILE);
        assertEquals(AdminSettings.Status.NOT_LOADED, AdminSettings.apply(KEYS, RULES, 2, N.id(), new SettingValue.Int(7), w).status());
        assertTrue(w.calls.isEmpty());
    }

    @Test
    void aGameRuleGoesThroughTheRuleWriterAtLevelTwoOnly() {
        Recording w = new Recording();
        assertTrue(AdminSettings.apply(KEYS, RULES, 2, RULE.id(), new SettingValue.Int(3), w).ok());
        assertEquals(List.of("rule gamerule:testRule 3"), w.calls);
        assertEquals(AdminSettings.Status.INVALID, AdminSettings.apply(KEYS, RULES, 2, RULE.id(), new SettingValue.Bool(true), w).status());
    }
}
