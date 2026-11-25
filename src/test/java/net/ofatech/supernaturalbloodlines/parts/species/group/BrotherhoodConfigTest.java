package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodSwitches;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminAccess;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSettings;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingEntry;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BrotherhoodConfigTest {
    private record Whole(String key, IntSupplier get, int def, int lo, int hi) {
    }

    private record Flag(String key, BooleanSupplier get) {
    }

    private static final List<Whole> WHOLES = List.of(
            new Whole("arms_percent", GroupsConfig::armsPercent, 3, 0, 50),
            new Whole("arms_max_percent", GroupsConfig::armsMaxPercent, 12, 0, 100),
            new Whole("arms_radius", GroupsConfig::armsRadius, 24, 2, 64),
            new Whole("sight_radius", GroupsConfig::sightRadius, 48, 8, 128),
            new Whole("code_mark_penalty_percent", GroupsConfig::codeMarkPenaltyPercent, 25, 0, 100),
            new Whole("code_breaker_hours", GroupsConfig::codeBreakerHours, 24, 1, 720),
            new Whole("code_strikes", GroupsConfig::codeStrikes, 3, 2, 20),
            new Whole("code_strikes_days", GroupsConfig::codeStrikesDays, 7, 1, 365),
            new Whole("code_exile_days", GroupsConfig::codeExileDays, 7, 1, 365),
            new Whole("brotherhood_curse_hours", GroupsConfig::brotherhoodCurseHours, 48, 1, 720));

    private static final List<Flag> FLAGS = List.of(
            new Flag("brotherhood_enabled", GroupsConfig::brotherhoodEnabled),
            new Flag("lead_by_mark", GroupsConfig::leadByMark),
            new Flag("sight_share_enabled", GroupsConfig::sightShareEnabled),
            new Flag("code_enabled", GroupsConfig::codeEnabled),
            new Flag("curse_extends_enabled", GroupsConfig::curseExtendsEnabled),
            new Flag("kol_warning_enabled", GroupsConfig::kolWarningEnabled));

    /** The 16 keys plan 20d adds. */
    static final List<String> NEW_KEYS = List.of("brotherhood_enabled", "lead_by_mark", "arms_percent", "arms_max_percent", "arms_radius", "sight_share_enabled", "sight_radius",
            "code_enabled", "code_mark_penalty_percent", "code_breaker_hours", "code_strikes", "code_strikes_days", "code_exile_days", "curse_extends_enabled",
            "brotherhood_curse_hours", "kol_warning_enabled");

    @AfterEach
    void restore() {
        WHOLES.forEach(w -> GroupsConfig.override(w.key(), null));
        FLAGS.forEach(f -> GroupsConfig.override(f.key(), null));
        GroupsConfig.override("groups_enabled", null);
    }

    @Test
    void thereAreSixteenNewKeys() {
        assertEquals(16, NEW_KEYS.size());
        assertEquals(16, NEW_KEYS.stream().distinct().count());
        assertEquals(NEW_KEYS.stream().sorted().toList(), java.util.stream.Stream.concat(WHOLES.stream().map(Whole::key), FLAGS.stream().map(Flag::key)).sorted().toList());
    }

    @Test
    void defaultsAnswerBeforeTheFileLoads() {
        for (Whole w : WHOLES) {
            assertEquals(w.def(), w.get().getAsInt(), w.key());
        }
        for (Flag f : FLAGS) {
            assertTrue(f.get().getAsBoolean(), f.key());
        }
    }

    @Test
    void overrideRestores() {
        GroupsConfig.override("arms_percent", 5);
        GroupsConfig.override("code_enabled", false);
        assertEquals(5, GroupsConfig.armsPercent());
        assertFalse(GroupsConfig.codeEnabled());
        restore();
        assertEquals(3, GroupsConfig.armsPercent());
        assertTrue(GroupsConfig.codeEnabled());
    }

    @Test
    void everyRangeIsClamped() {
        for (Whole w : WHOLES) {
            GroupsConfig.override(w.key(), w.lo() - 1000);
            assertEquals(w.lo(), w.get().getAsInt(), w.key() + " below");
            GroupsConfig.override(w.key(), w.hi() + 1000);
            assertEquals(w.hi(), w.get().getAsInt(), w.key() + " above");
            GroupsConfig.override(w.key(), null);
        }
    }

    @Test
    void brotherhoodOffSwitchesEveryPerkOff() {
        assertTrue(BrotherhoodSwitches.perksOn(true, true));
        assertFalse(BrotherhoodSwitches.perksOn(true, false));
        assertFalse(BrotherhoodSwitches.perksOn(false, true));
        assertFalse(BrotherhoodSwitches.perksOn(false, false));
        assertTrue(GroupsConfig.brotherhoodPerksOn());
        GroupsConfig.override("brotherhood_enabled", false);
        assertFalse(GroupsConfig.brotherhoodPerksOn());
        GroupsConfig.override("brotherhood_enabled", null);
        GroupsConfig.override("groups_enabled", false);
        assertFalse(GroupsConfig.brotherhoodPerksOn());
    }

    @Test
    void armsPercentZeroIsOffAndPenaltyZeroIsNoLoss() {
        GroupsConfig.override("arms_percent", 0);
        GroupsConfig.override("code_mark_penalty_percent", 0);
        assertEquals(0, GroupsConfig.armsPercent());
        assertEquals(0, GroupsConfig.codeMarkPenaltyPercent());
    }

    @Test
    void theSwitchesAreIndependent() {
        GroupsConfig.override("code_enabled", false);
        assertEquals(3, GroupsConfig.armsPercent());
        assertTrue(GroupsConfig.sightShareEnabled() && GroupsConfig.curseExtendsEnabled() && GroupsConfig.kolWarningEnabled() && GroupsConfig.leadByMark());
        GroupsConfig.override("code_enabled", null);
        GroupsConfig.override("sight_share_enabled", false);
        assertTrue(GroupsConfig.codeEnabled() && GroupsConfig.armsPercent() == 3);
    }

    @Test
    void theBrotherhoodKeysAreOperatorOnly() {
        AdminSettings.Writer never = new AdminSettings.Writer() {
            @Override
            public void set(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?> value, Object boxed) {
                throw new AssertionError("written");
            }

            @Override
            public void save(net.neoforged.neoforge.common.ModConfigSpec spec) {
                throw new AssertionError("saved");
            }

            @Override
            public void setRule(SettingEntry rule, Object boxed) {
                throw new AssertionError("rule");
            }
        };
        for (String key : NEW_KEYS) {
            String id = GroupsConfig.FILE + ":" + key;
            assertTrue(AdminEntries.find(id).isPresent(), "no Admin row for " + key);
            for (int level : new int[] {0, 1}) {
                assertEquals(AdminSettings.Status.NOT_OP, AdminSettings.apply(level, id, new SettingValue.Int(5), never).status(), key + " at " + level);
            }
            assertTrue(AdminAccess.allowed(2), key);
            assertTrue(AdminSettings.apply(2, id, new SettingValue.Int(5), never).status() != AdminSettings.Status.NOT_OP, key + " at 2");
        }
    }
}
