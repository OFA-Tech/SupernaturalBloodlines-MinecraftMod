package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Kind;
import org.junit.jupiter.api.Test;

/**
 * Runs {@link ReflectiveStoneApi} against his real classes: his client wheel state is set to a few real powers and what the wheel reads out of it is checked. The jar is the
 * version this was written for (8.0.0-alpha.6); a machine without it (CI) skips the test, as {@link InfinityProbeTest} does.
 */
class InfinityRealJarTest {
    private static final String ROOT = "com.infinitystones.tensura.";

    private static File jar() {
        return InfinityJar.get();
    }

    /** Stone ordinals of his enum: MIND, POWER, SPACE, REALITY, SOUL, TIME. */
    private static Object entry(ClassLoader l, int stone, String node) throws Exception {
        Class<?> entry = Class.forName(ROOT + "wheel.WheelPayloads$Entry", true, l);
        return entry.getConstructor(int.class, String.class, boolean.class, boolean.class, int.class, int.class, int.class).newInstance(stone, node, true, false, 0, 0, 0);
    }

    private static void setState(ClassLoader l, String holding, Object... entries) throws Exception {
        Class<?> state = Class.forName(ROOT + "wheel.WheelPayloads$State", true, l);
        Object value = state.getConstructor(List.class, String.class).newInstance(List.of(entries), holding);
        Field f = Class.forName(ROOT + "wheel.WheelClientState", true, l).getDeclaredField("state");
        f.setAccessible(true);
        f.set(null, value);
    }

    private static NodeInfo find(List<NodeInfo> nodes, String modeId) {
        return nodes.stream().filter(n -> n.modeId().equals(modeId)).findFirst().orElseThrow(() -> new AssertionError(modeId + " not read: " + nodes));
    }

    private static void inject(Language language) {
        Language.inject(language);
    }

    /** A language that puts a tag before every text, so a name read in another "language" differs. */
    private static Language tagged(String tag) {
        return new Language() {
            @Override
            public String getOrDefault(String key, String defaultValue) {
                return tag + defaultValue;
            }

            @Override
            public boolean has(String key) {
                return true;
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return FormattedCharSequence.EMPTY;
            }
        };
    }

    @Test
    void nodesReadsTheRealPowersAndMenusOfHisState() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar().toURI().toURL()}, getClass().getClassLoader())) {
            ReflectiveStoneApi api = ReflectiveStoneApi.create(name -> Class.forName(name, false, loader));
            setState(loader, "mind.psionic_overload_aim",
                    entry(loader, 2, "teleport_gui"), entry(loader, 2, "teleport"), entry(loader, 2, "spatial_banishment"),
                    entry(loader, 3, "will_construct"), entry(loader, 0, "suggestion"), entry(loader, 0, "phantasm"));
            List<NodeInfo> nodes = api.nodes();
            assertEquals(6, nodes.size(), nodes.toString());
            assertEquals("mind.psionic_overload_aim", api.holding(), "the channel his state says is running");

            NodeInfo gui = find(nodes, "space.teleport_gui");
            assertEquals(2, gui.stone(), "his stone ordinal, which his Use takes");
            assertEquals("space", gui.stoneKey());
            assertEquals(Kind.DESTINATION, gui.menu());
            assertTrue(gui.menuOnLeft(), "his Teleport GUI: the plain click is the menu");
            assertTrue(gui.unlocked());
            assertFalse(gui.isSwitch());
            assertFalse(gui.name().isEmpty(), "the name comes from his tree");

            NodeInfo teleport = find(nodes, "space.teleport");
            assertEquals(Kind.DESTINATION, teleport.menu());
            assertFalse(teleport.menuOnLeft());

            NodeInfo banish = find(nodes, "space.spatial_banishment");
            assertEquals(Kind.DESTINATION_TARGET, banish.menu(), "it moves what you aim at");
            assertTrue(banish.isDual(), "left: the End, right: above the Nether roof");

            NodeInfo will = find(nodes, "reality.will_construct");
            assertEquals(Kind.WILL, will.menu());
            assertTrue(will.isDual());

            NodeInfo suggestion = find(nodes, "mind.suggestion");
            assertEquals(Kind.CHOICE, suggestion.menu());
            assertTrue(suggestion.isDual());

            NodeInfo phantasm = find(nodes, "mind.phantasm");
            assertEquals(Kind.NONE, phantasm.menu(), "a dual power with no menu");
            assertTrue(phantasm.isDual());
            assertNotNull(phantasm.description());
        }
    }

    @Test
    void theListIsReadOncePerStateAndTheNamesAreReadAgainAfterAReload() throws Exception {
        Language before = Language.getInstance();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar().toURI().toURL()}, getClass().getClassLoader())) {
            ReflectiveStoneApi api = ReflectiveStoneApi.create(name -> Class.forName(name, false, loader));
            inject(tagged("A:"));
            setState(loader, "", entry(loader, 2, "teleport_gui"));
            List<NodeInfo> first = api.nodes();
            assertTrue(first.get(0).name().startsWith("A:"), first.get(0).name());
            assertSame(first, api.nodes(), "the same state is not read again");

            inject(tagged("B:"));
            assertTrue(api.nodes().get(0).name().startsWith("A:"), "nothing re-reads a name by itself: a reload says so");
            api.reload();
            List<NodeInfo> after = api.nodes();
            assertNotSame(first, after);
            assertTrue(after.get(0).name().startsWith("B:"), "the name is read again in the new language: " + after.get(0).name());

            setState(loader, "", entry(loader, 2, "teleport_gui"), entry(loader, 0, "phantasm"));
            assertEquals(2, api.nodes().size(), "a new state is read");
        } finally {
            inject(before);
        }
    }

    @Test
    void hisPrayerClassIsRequiredSoWithoutItTheCompatIsOffAndHisKeyComesBack() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar().toURI().toURL()}, getClass().getClassLoader())) {
            try {
                ReflectiveStoneApi.create(name -> {
                    if (name.endsWith("GatherNet$Pray")) {
                        throw new ClassNotFoundException(name);
                    }
                    return Class.forName(name, false, loader);
                });
            } catch (ReflectiveStoneApi.Missing missing) {
                assertTrue(missing.getMessage().contains("GatherNet$Pray"), missing.getMessage());
                return;
            }
            throw new AssertionError("without GatherNet$Pray the stones' key would leave a player with no stone no way to pray");
        }
    }

    @Test
    void withNoPlayerThereIsNoPrayerShownAndNothingFailsAtRunTime() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar().toURI().toURL()}, getClass().getClassLoader())) {
            List<String> broke = new java.util.ArrayList<>();
            ReflectiveStoneApi api = ReflectiveStoneApi.create(name -> Class.forName(name, false, loader), new StoneFailures(m -> { }, broke::add));
            assertFalse(api.prayShown(), "no player in a unit test: nothing to pray for");
            assertTrue(broke.isEmpty());
        }
    }

    @Test
    void aStateTheProbeDidNotExpectTurnsTheCompatOffInsteadOfLeavingItEmptyAndHisKeyCovered() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar().toURI().toURL()}, getClass().getClassLoader())) {
            List<String> broke = new java.util.ArrayList<>();
            ReflectiveStoneApi api = ReflectiveStoneApi.create(name -> Class.forName(name, false, loader), new StoneFailures(m -> { }, broke::add));
            setState(loader, "", entry(loader, 99, "teleport_gui")); // a stone ordinal his enum does not have
            assertTrue(api.nodes().isEmpty());
            assertEquals(1, broke.size(), "the failure is reported to the probe: " + broke);
            setState(loader, "", entry(loader, 2, "teleport_gui"));
            assertEquals(1, api.nodes().size(), "a state that reads is read as before (the probe, not the api, decides to stay off)");
            assertEquals(1, broke.size());
        }
    }}
