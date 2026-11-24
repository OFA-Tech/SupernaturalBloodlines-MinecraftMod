package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.State;
import org.junit.jupiter.api.Test;

class InfinityProbeTest {
    private static InfinityProbe probe(boolean loaded, boolean setting, InfinityProbe.Factory factory, List<String> warnings, AtomicInteger created) {
        return new InfinityProbe(() -> loaded, () -> setting, () -> {
            created.incrementAndGet();
            return factory.create();
        }, warnings::add);
    }

    @Test
    void missingModIsOffMissingAndSilent() {
        List<String> warnings = new ArrayList<>();
        AtomicInteger created = new AtomicInteger();
        InfinityProbe p = probe(false, true, FakeStoneApi::new, warnings, created);
        assertEquals(State.OFF_MISSING, p.state());
        assertNull(p.api());
        assertFalse(p.soulControlCovered());
        assertTrue(warnings.isEmpty(), "no warning for a mod the player does not have");
        assertEquals(0, created.get(), "his classes are never touched");
    }

    @Test
    void settingOffBehavesAsMissingAndNeverReadsHisClasses() {
        List<String> warnings = new ArrayList<>();
        AtomicInteger created = new AtomicInteger();
        InfinityProbe p = probe(true, false, FakeStoneApi::new, warnings, created);
        assertEquals(State.OFF_SETTING, p.state());
        assertNull(p.api());
        assertFalse(p.soulControlCovered());
        assertEquals(0, created.get());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void brokenSignatureLogsOnceAndTurnsOff() {
        List<String> warnings = new ArrayList<>();
        AtomicInteger created = new AtomicInteger();
        InfinityProbe p = probe(true, true, () -> {
            throw new ReflectiveStoneApi.Missing("method AbilityMeta.needsAim");
        }, warnings, created);
        for (int i = 0; i < 5; i++) {
            assertEquals(State.OFF_BROKEN, p.state());
            assertNull(p.api());
            assertFalse(p.soulControlCovered());
        }
        assertEquals(List.of("Infinity Stones compat off: method AbilityMeta.needsAim"), warnings, "one warning, naming what");
        assertEquals(1, created.get(), "resolved once, not on every frame");
    }

    @Test
    void anUnexpectedErrorInHisClassesIsAlsoOffAndNeverEscapes() {
        List<String> warnings = new ArrayList<>();
        InfinityProbe p = probe(true, true, () -> {
            throw new NoClassDefFoundError("net/minecraft/Gone");
        }, warnings, new AtomicInteger());
        assertEquals(State.OFF_BROKEN, p.state());
        assertEquals(1, warnings.size());
    }

    @Test
    void anActiveCompatCoversTheSoulKeyOnlyWithHisSoulClasses() {
        FakeStoneApi api = new FakeStoneApi();
        InfinityProbe p = probe(true, true, () -> api, new ArrayList<>(), new AtomicInteger());
        assertEquals(State.ACTIVE, p.state());
        assertTrue(p.soulControlCovered());
        api.soulSupported = false;
        assertEquals(State.ACTIVE, p.state());
        assertFalse(p.soulControlCovered(), "no SoulNet.Action: a covered key would leave a possessed player no way out");
    }

    @Test
    void theSettingIsReadEveryTimeSoTurningItOffTakesEffectAtOnce() {
        boolean[] on = {true};
        InfinityProbe p = new InfinityProbe(() -> true, () -> on[0], FakeStoneApi::new, m -> { });
        assertEquals(State.ACTIVE, p.state());
        on[0] = false;
        assertEquals(State.OFF_SETTING, p.state());
        on[0] = true;
        assertEquals(State.ACTIVE, p.state());
    }

    @Test
    void whenAClassIsGoneTheMissingErrorNamesIt() {
        try {
            ReflectiveStoneApi.create(name -> {
                throw new ClassNotFoundException(name);
            });
        } catch (ReflectiveStoneApi.Missing missing) {
            assertTrue(missing.getMessage().startsWith("class "), missing.getMessage());
            return;
        }
        throw new AssertionError("a finder that finds nothing must not give an api");
    }

    /** Against the real jar of the version this was written for, when it is there (a developer's machine); CI has none and skips. */
    @Test
    void theReflectedShapesAreHisRealOnes() throws Exception {
        File jar = InfinityJar.get();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toURI().toURL()}, getClass().getClassLoader())) {
            ReflectiveStoneApi api = ReflectiveStoneApi.create(name -> Class.forName(name, false, loader));
            assertNotNull(api);
            assertTrue(api.soulSupported(), "SoulNet, SoulNet$State, SoulNet$Action and Possession are all there");
        }
    }
}
