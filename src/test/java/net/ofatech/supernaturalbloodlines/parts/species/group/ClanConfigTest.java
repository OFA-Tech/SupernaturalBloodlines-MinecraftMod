package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterCost;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanSwitches;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminAccess;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSettings;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingEntry;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClanConfigTest {
    private record Whole(String key, IntSupplier get, int def, int lo, int hi) {
    }

    private static final List<Whole> WHOLES = List.of(
            new Whole("daywalker_days", GroupsConfig::daywalkerDays, 14, 0, 365),
            new Whole("exile_days", GroupsConfig::exileDays, 7, 1, 365),
            new Whole("charter_min_members", GroupsConfig::charterMinMembers, 3, 2, 50),
            new Whole("charter_lost_days", GroupsConfig::charterLostDays, 7, 1, 365),
            new Whole("challenger_min_days", GroupsConfig::challengerMinDays, 7, 0, 365),
            new Whole("contest_cooldown_days", GroupsConfig::contestCooldownDays, 7, 0, 365),
            new Whole("contest_accept_minutes", GroupsConfig::contestAcceptMinutes, 10, 1, 120),
            new Whole("contest_min_minutes", GroupsConfig::contestMinMinutes, 2, 0, 20),
            new Whole("contest_max_hours", GroupsConfig::contestMaxHours, 1, 1, 24),
            new Whole("feed_share_percent", GroupsConfig::feedSharePercent, 10, 1, 100),
            new Whole("feed_share_max", GroupsConfig::feedShareMax, 2, 1, 20),
            new Whole("feed_share_radius", GroupsConfig::feedShareRadius, 16, 2, 64),
            new Whole("feed_share_cap_per_minute", GroupsConfig::feedShareCapPerMinute, 4, 1, 100),
            new Whole("sireline_bonus_percent", GroupsConfig::sirelineBonusPercent, 3, 0, 50),
            new Whole("sireline_bonus_max_percent", GroupsConfig::sirelineBonusMaxPercent, 15, 0, 100));

    private static final List<String> FLAGS = List.of("charter_enabled", "feed_share_enabled", "sun_ring_day_walker_only");

    /** The 18 keys plan 20c adds (the ring key is 20b's). */
    static final List<String> NEW_KEYS = List.of("daywalker_days", "exile_days", "charter_enabled", "charter_min_members", "charter_lost_days", "charter_forge_cost",
            "challenger_min_days", "contest_cooldown_days", "contest_accept_minutes", "contest_min_minutes", "contest_max_hours", "feed_share_enabled", "feed_share_percent",
            "feed_share_max", "feed_share_radius", "feed_share_cap_per_minute", "sireline_bonus_percent", "sireline_bonus_max_percent");

    @AfterEach
    void restore() {
        WHOLES.forEach(w -> GroupsConfig.override(w.key(), null));
        FLAGS.forEach(k -> GroupsConfig.override(k, null));
        GroupsConfig.override("charter_forge_cost", null);
        GroupsConfig.override("clans_enabled", null);
    }

    @Test
    void thereAreEighteenNewKeys() {
        assertEquals(18, NEW_KEYS.size());
        assertEquals(18, NEW_KEYS.stream().distinct().count());
    }

    @Test
    void defaultsAnswerBeforeTheFileLoads() {
        for (Whole w : WHOLES) {
            assertEquals(w.def(), w.get().getAsInt(), w.key());
        }
        assertTrue(GroupsConfig.charterEnabled() && GroupsConfig.feedShareEnabled());
        assertEquals(List.of("minecraft:writable_book", "minecraft:gold_block"), GroupsConfig.charterForgeCostRaw());
    }

    @Test
    void overrideRestores() {
        GroupsConfig.override("daywalker_days", 3);
        GroupsConfig.override("charter_enabled", false);
        GroupsConfig.override("charter_forge_cost", List.of("minecraft:diamond"));
        assertEquals(3, GroupsConfig.daywalkerDays());
        assertFalse(GroupsConfig.charterEnabled());
        assertEquals(List.of("minecraft:diamond"), GroupsConfig.charterForgeCostRaw());
        restore();
        assertEquals(14, GroupsConfig.daywalkerDays());
        assertTrue(GroupsConfig.charterEnabled());
        assertEquals(2, GroupsConfig.charterForgeCostRaw().size());
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
    void theSunRingDefaultIsNowDayWalkerOnly() {
        assertTrue(GroupsConfig.sunRingDayWalkerOnly());
    }

    @Test
    void anOldFilesFalseIsKept() {
        CommentedConfig old = new TomlParser().parse("sun_ring_day_walker_only = false\n");
        GroupsConfig.SPEC.correct(old);
        assertEquals(Boolean.FALSE, old.get("sun_ring_day_walker_only"), "a stored false was replaced by the new default");
        CommentedConfig fresh = new TomlParser().parse("");
        GroupsConfig.SPEC.correct(fresh);
        assertEquals(Boolean.TRUE, fresh.get("sun_ring_day_walker_only"), "a new file does not get the new default");
    }

    @Test
    void forgeCostParses() {
        List<String> warned = new ArrayList<>();
        List<CharterCost.Entry> two = CharterCost.parse(CharterCost.DEFAULT, id -> true, warned::add);
        assertEquals(List.of(new CharterCost.Entry("minecraft:writable_book", 1), new CharterCost.Entry("minecraft:gold_block", 1)), two);
        assertTrue(warned.isEmpty());
        List<CharterCost.Entry> counted = CharterCost.parse(List.of("minecraft:gold_ingot=5", "minecraft:nope", "not an id", "minecraft:iron_ingot=0"), id -> !id.equals("minecraft:nope"), warned::add);
        assertEquals(List.of(new CharterCost.Entry("minecraft:gold_ingot", 5)), counted);
        assertEquals(3, warned.size(), "one warning for each skipped entry: " + warned);
        warned.clear();
        assertEquals(two, CharterCost.parse(List.of("minecraft:nope", "junk"), id -> false, warned::add), "a list that parses to nothing falls back to the default");
        assertEquals(List.of(), CharterCost.parse(List.of(), id -> true, warned::add), "an empty list is free");
        GroupsConfig.override("charter_forge_cost", List.of("minecraft:diamond=2"));
        assertEquals(List.of(new CharterCost.Entry("minecraft:diamond", 2)), GroupsConfig.charterForgeCost(id -> true));
    }

    @Test
    void clansOffSwitchesEveryPerkOff() {
        assertTrue(ClanSwitches.perksOn(true, true));
        assertFalse(ClanSwitches.perksOn(true, false));
        assertFalse(ClanSwitches.perksOn(false, true));
        assertFalse(ClanSwitches.perksOn(false, false));
        assertTrue(GroupsConfig.clanPerksOn());
        GroupsConfig.override("clans_enabled", false);
        assertFalse(GroupsConfig.clanPerksOn());
    }

    @Test
    void theClanKeysAreOperatorOnly() {
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
