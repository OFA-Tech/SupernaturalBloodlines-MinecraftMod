package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;
import org.junit.jupiter.api.Test;

class SettingRulesTest {
    private static final ModConfigSpec.IntValue N;
    private static final ModConfigSpec.DoubleValue D;
    private static final ModConfigSpec.ConfigValue<String> CHOICE;
    private static final ModConfigSpec.BooleanValue FLAG;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> LIST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        N = b.defineInRange("n", 5, 1, 10);
        D = b.defineInRange("d", 0.5, 0.0, 1.0);
        CHOICE = b.defineInList("choice", "a", new ArrayList<>(List.of("a", "b")));
        FLAG = b.define("flag", true);
        LIST = b.defineListAllowEmpty("list", List.of("x=1"), () -> "", o -> o instanceof String);
        b.build();
    }

    private static final Predicate<String> SHAPE = line -> line.matches("[a-z]+=[0-9]+");

    private static String refusal(ValueSpec spec, SettingKind kind, SettingValue v, Predicate<String> lineCheck) {
        return SettingRules.check(spec, kind, v, lineCheck).map(SettingRules.Refusal::key).orElse("");
    }

    private static String refusal(ValueSpec spec, SettingKind kind, SettingValue v) {
        return refusal(spec, kind, v, null);
    }

    @Test
    void theEdgesOfARangeAreAcceptedAndOneBeyondIsRefused() {
        assertEquals("", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Int(1)));
        assertEquals("", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Int(10)));
        assertEquals(SettingRules.KEY + "range", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Int(0)));
        assertEquals(SettingRules.KEY + "range", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Int(11)));
        assertEquals("", refusal(D.getSpec(), SettingKind.DOUBLE, new SettingValue.Dbl(0.0)));
        assertEquals("", refusal(D.getSpec(), SettingKind.DOUBLE, new SettingValue.Dbl(1.0)));
        assertEquals(SettingRules.KEY + "range", refusal(D.getSpec(), SettingKind.DOUBLE, new SettingValue.Dbl(1.0001)));
    }

    @Test
    void notANumberAndInfinityAreRefused() {
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertEquals(SettingRules.KEY + "range", refusal(D.getSpec(), SettingKind.DOUBLE, new SettingValue.Dbl(bad)), "value " + bad);
        }
    }

    @Test
    void aLongPastTheIntRangeIsRefusedNotWrapped() {
        // 2^32 + 5 wraps to 5 in an int cast, which the range would accept
        assertEquals(SettingRules.KEY + "range", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Int((1L << 32) + 5)));
        assertEquals(SettingRules.KEY + "range", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Int(Long.MIN_VALUE)));
    }

    @Test
    void aValueOfTheWrongTypeIsRefused() {
        assertEquals(SettingRules.KEY + "type", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Text("5")));
        assertEquals(SettingRules.KEY + "type", refusal(N.getSpec(), SettingKind.INT, new SettingValue.Dbl(5)));
        assertEquals(SettingRules.KEY + "type", refusal(FLAG.getSpec(), SettingKind.BOOL, new SettingValue.Int(1)));
        assertEquals(SettingRules.KEY + "type", refusal(LIST.getSpec(), SettingKind.LIST, new SettingValue.Text("x=1")));
        assertEquals("", refusal(FLAG.getSpec(), SettingKind.BOOL, new SettingValue.Bool(false)));
    }

    @Test
    void aChoiceMustBeOneOfTheListed() {
        assertEquals("", refusal(CHOICE.getSpec(), SettingKind.CHOICE, new SettingValue.Text("b")));
        assertEquals(SettingRules.KEY + "choice", refusal(CHOICE.getSpec(), SettingKind.CHOICE, new SettingValue.Text("c")));
    }

    @Test
    void aListOverTheCapsIsRefused() {
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i <= SettingValue.MAX_LINES; i++) {
            tooMany.add("x=1");
        }
        assertEquals(SettingRules.KEY + "too_long", refusal(LIST.getSpec(), SettingKind.LIST, new SettingValue.Lines(tooMany)));
        String longLine = "x=" + "1".repeat(SettingValue.MAX_LINE);
        assertEquals(SettingRules.KEY + "too_long", refusal(LIST.getSpec(), SettingKind.LIST, new SettingValue.Lines(List.of(longLine))));
        List<String> heavy = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            heavy.add("x=" + "1".repeat(SettingValue.MAX_LINE - 2));
        }
        assertEquals(SettingRules.KEY + "too_long", refusal(LIST.getSpec(), SettingKind.LIST, new SettingValue.Lines(heavy)), "20 lines of 256 are over the total");
        assertEquals("", refusal(LIST.getSpec(), SettingKind.LIST, new SettingValue.Lines(List.of("x=1", "y=2"))));
        assertEquals("", refusal(LIST.getSpec(), SettingKind.LIST, new SettingValue.Lines(List.of())), "an empty list is allowed by the spec");
    }

    @Test
    void aLineTheKeysParserRejectsIsRefusedWithThatLine() {
        Optional<SettingRules.Refusal> r = SettingRules.check(LIST.getSpec(), SettingKind.LIST, new SettingValue.Lines(List.of("x=1", "oops")), SHAPE);
        assertTrue(r.isPresent());
        assertEquals(SettingRules.KEY + "line", r.get().key());
        assertEquals("oops", r.get().detail());
        assertTrue(SettingRules.check(LIST.getSpec(), SettingKind.LIST, new SettingValue.Lines(List.of("x=1", "y=22")), SHAPE).isEmpty());
    }

    @Test
    void aGameRuleChecksTypeAndIntRangeOnly() {
        assertTrue(SettingRules.checkRule(SettingKind.GAMERULE_BOOL, new SettingValue.Bool(true)).isEmpty());
        assertTrue(SettingRules.checkRule(SettingKind.GAMERULE_BOOL, new SettingValue.Int(1)).isPresent());
        assertTrue(SettingRules.checkRule(SettingKind.GAMERULE_INT, new SettingValue.Int(7)).isEmpty());
        assertTrue(SettingRules.checkRule(SettingKind.GAMERULE_INT, new SettingValue.Int(1L << 40)).isPresent());
    }
}
