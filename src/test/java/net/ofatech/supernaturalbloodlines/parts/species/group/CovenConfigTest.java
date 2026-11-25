package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CovenConfigTest {
    private static final List<String> KEYS = List.of("link_enabled", "link_member_share", "channel_cap", "channel_other_factor", "channel_regen_minutes", "ep_keep_percent",
            "potency_max_bonus", "potency_falloff", "tier1_members", "tier2_members", "tier3_members", "link_window_seconds", "link_death_damage_percent", "link_death_lethal",
            "link_shaken_minutes", "elder_min_days", "members_per_elder", "vote_hours", "election_cooldown_days", "shun_days", "moonlight_ring_enabled", "sun_ring_forge_enabled",
            "sun_ring_day_walker_only", "mastery_ultimate_circle", "grand_rites_enabled", "grand_rite_cooldown_hours", "grand_rite_radius", "fete_minutes", "harvest_xp_levels",
            "harvest_max_blocks");

    @AfterEach
    void restore() {
        KEYS.forEach(k -> GroupsConfig.override(k, null));
    }

    private record Whole(String key, IntSupplier get, int def, int lo, int hi) {
    }

    private record Real(String key, DoubleSupplier get, double def, double lo, double hi) {
    }

    private static final List<Whole> WHOLES = List.of(
            new Whole("channel_cap", GroupsConfig::channelCap, 3000, 1, 100000),
            new Whole("channel_regen_minutes", GroupsConfig::channelRegenMinutes, 10, 1, 1440),
            new Whole("ep_keep_percent", GroupsConfig::epKeepPercent, 10, 0, 90),
            new Whole("tier1_members", GroupsConfig::tier1Members, 3, 2, 13),
            new Whole("link_window_seconds", GroupsConfig::linkWindowSeconds, 30, 5, 3600),
            new Whole("link_death_damage_percent", GroupsConfig::linkDeathDamagePercent, 60, 0, 100),
            new Whole("link_shaken_minutes", GroupsConfig::linkShakenMinutes, 30, 0, 1440),
            new Whole("elder_min_days", GroupsConfig::elderMinDays, 3, 0, 365),
            new Whole("members_per_elder", GroupsConfig::membersPerElder, 4, 1, 100),
            new Whole("vote_hours", GroupsConfig::voteHours, 24, 1, 168),
            new Whole("election_cooldown_days", GroupsConfig::electionCooldownDays, 7, 0, 365),
            new Whole("shun_days", GroupsConfig::shunDays, 7, 1, 365),
            new Whole("mastery_ultimate_circle", GroupsConfig::masteryUltimateCircle, 7, 0, 13),
            new Whole("grand_rite_cooldown_hours", GroupsConfig::grandRiteCooldownHours, 12, 0, 720),
            new Whole("grand_rite_radius", GroupsConfig::grandRiteRadius, 24, 4, 64),
            new Whole("fete_minutes", GroupsConfig::feteMinutes, 10, 1, 120),
            new Whole("harvest_xp_levels", GroupsConfig::harvestXpLevels, 30, 0, 1000),
            new Whole("harvest_max_blocks", GroupsConfig::harvestMaxBlocks, 2048, 64, 65536));

    private static final List<Real> REALS = List.of(
            new Real("link_member_share", GroupsConfig::linkMemberShare, 0.70, 0.0, 0.9),
            new Real("channel_other_factor", GroupsConfig::channelOtherFactor, 0.5, 0.0, 1.0),
            new Real("potency_max_bonus", GroupsConfig::potencyMaxBonus, 1.0, 0.0, 10.0),
            new Real("potency_falloff", GroupsConfig::potencyFalloff, 0.8, 0.0, 0.99));

    @Test
    void defaultsAnswerBeforeTheFileLoads() {
        for (Whole w : WHOLES) {
            assertEquals(w.def(), w.get().getAsInt(), w.key());
        }
        for (Real r : REALS) {
            assertEquals(r.def(), r.get().getAsDouble(), 1e-9, r.key());
        }
        assertTrue(GroupsConfig.linkEnabled() && GroupsConfig.moonlightRingEnabled() && GroupsConfig.sunRingForgeEnabled() && GroupsConfig.grandRitesEnabled());
        assertFalse(GroupsConfig.linkDeathLethal());
        assertTrue(GroupsConfig.sunRingDayWalkerOnly(), "plan 20c: the default is Day Walkers only");
        assertEquals(7, GroupsConfig.tier2Members());
        assertEquals(13, GroupsConfig.tier3Members());
    }

    @Test
    void overrideRestores() {
        GroupsConfig.override("channel_cap", 99);
        GroupsConfig.override("link_death_lethal", true);
        GroupsConfig.override("link_member_share", 0.3);
        assertEquals(99, GroupsConfig.channelCap());
        assertTrue(GroupsConfig.linkDeathLethal());
        assertEquals(0.3, GroupsConfig.linkMemberShare(), 1e-9);
        KEYS.forEach(k -> GroupsConfig.override(k, null));
        assertEquals(3000, GroupsConfig.channelCap());
        assertFalse(GroupsConfig.linkDeathLethal());
        assertEquals(0.70, GroupsConfig.linkMemberShare(), 1e-9);
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
        for (Real r : REALS) {
            GroupsConfig.override(r.key(), r.lo() - 5.0);
            assertEquals(r.lo(), r.get().getAsDouble(), 1e-9, r.key() + " below");
            GroupsConfig.override(r.key(), r.hi() + 5.0);
            assertEquals(r.hi(), r.get().getAsDouble(), 1e-9, r.key() + " above");
            GroupsConfig.override(r.key(), null);
        }
    }

    @Test
    void tiersAreReadAsANonDecreasingTriple() {
        GroupsConfig.override("tier1_members", 7);
        GroupsConfig.override("tier2_members", 3);
        GroupsConfig.override("tier3_members", 13);
        assertEquals(7, GroupsConfig.tier1Members());
        assertEquals(7, GroupsConfig.tier2Members());
        assertEquals(13, GroupsConfig.tier3Members());
        GroupsConfig.override("tier3_members", 5);
        assertEquals(7, GroupsConfig.tier3Members());
    }

    @Test
    void theMemberShareNeverReachesOne() {
        GroupsConfig.override("link_member_share", 0.95);
        assertEquals(0.9, GroupsConfig.linkMemberShare(), 1e-9);
    }
}