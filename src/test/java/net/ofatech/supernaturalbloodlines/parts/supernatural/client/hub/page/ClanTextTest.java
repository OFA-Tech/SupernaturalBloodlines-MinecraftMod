package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupTextTest;
import org.junit.jupiter.api.Test;

/** The clan's words: every key the cards, the commands and the items use exists, the clan never says pack or coven, and no word names a species. */
class ClanTextTest {
    private static final String H = "supernaturalbloodlines.hub.clan.";
    private static final String C = "supernaturalbloodlines.clan.";

    private static List<String> keys() {
        List<String> k = new ArrayList<>();
        for (String s : List.of("title", "rank.head", "rank.day_walker", "rank.night_walker", "you", "to_day_walker", "head_decides", "charter.title", "charter.none", "charter.held_you",
                "charter.held", "charter.held_mate", "charter.stored", "charter.dropped", "charter.lost", "charter.inert", "charter.reforge_wait", "charter_forge", "charter_forge.what",
                "charter_forge.few", "charter_reforge", "charter_reforge.what", "charter_reforge.wait", "contest.pending", "contest.pending_anon", "contest.pending_left", "contest.running",
                "contest.cooldown", "challenge", "challenge.what", "accept", "accept.what", "feed", "line", "line_none", "member_title", "promote", "promote.what", "demote", "demote.what",
                "exile", "exile.what", "exile.body", "you.one", "to_day_walker.one", "contest.cooldown.one")) {
            k.add(H + s);
        }
        for (ContestRules.Refusal r : ContestRules.Refusal.values()) {
            k.add(H + "why." + r.id());
            k.add(C + "err.challenge_" + r.id().replace("not_day_walker", "not_day_walker"));
        }
        for (String s : List.of("promoted", "promoted_ok", "demoted", "demoted_ok", "auto_promoted", "exiled", "exiled_ok", "shared_feed", "charter_forged", "charter_reforged", "charter_taken",
                "charter_dust", "charter_lost", "challenged", "contest_started", "contest_void", "contest_cooldown", "contest_cooldown.one", "contest_nochange.not_held", "contest_nochange.left", "contest_nochange.leader_holds", "contest_nochange.offline", "contest_nochange.dead", "contest_nochange.not_day_walker", "contest_nochange.forfeit_challenger_away", "contest_new_head_forfeit", "contest_new_head", "err.leader_only",
                "err.leader_target", "err.not_member", "err.already_day_walker", "err.not_day_walker", "err.exiled_still", "err.charter_off", "err.charter_too_few", "err.charter_already",
                "err.charter_use_reforge", "err.charter_not_lost", "err.charter_reforge_wait", "err.charter_none", "err.charter_cost", "err.challenge_leader", "err.challenge_cooldown",
                "err.challenge_none", "err.no_decline", "err.contest_running", "err.cannot_promote", "exiled_until", "err.exiled_brings")) {
            k.add(C + s);
        }
        k.add("item.supernaturalbloodlines.clan_charter");
        k.add("item.supernaturalbloodlines.clan_charter.tooltip");
        k.add("item.supernaturalbloodlines.clan_charter.tooltip_night");
        k.add("supernaturalbloodlines.hub.request.kind.charter_contest");
        for (String s : List.of("promote", "demote", "exile")) {
            k.add("supernaturalbloodlines.hub.profile.action." + s);
            k.add("supernaturalbloodlines.hub.profile.action." + s + ".what");
        }
        k.add("supernaturalbloodlines.hub.profile.confirm.exile");
        return k;
    }

    @Test
    void everyClanKeyThisPlanNamesExists() throws Exception {
        JsonObject lang = GroupTextTest.lang();
        List<String> missing = new ArrayList<>();
        for (String k : keys()) {
            if (!lang.has(k)) {
                missing.add(k);
            }
        }
        assertTrue(missing.isEmpty(), "clan keys missing from the lang file: " + missing);
    }

    @Test
    void noClanWordIsACopyOfThePackOrCovenText() throws Exception {
        JsonObject lang = GroupTextTest.lang();
        List<String> bad = new ArrayList<>();
        for (String k : lang.keySet()) {
            if (k.startsWith(H) || k.startsWith(C) || k.startsWith("item.supernaturalbloodlines.clan_charter")) {
                String text = lang.get(k).getAsString().toLowerCase();
                if (text.contains("pack") || text.contains("coven") || text.contains("werewolf") || text.contains("witch")) {
                    bad.add(k);
                }
            }
        }
        assertTrue(bad.isEmpty(), "a clan text names a pack or a coven: " + bad);
    }

    @Test
    void noClanWordNamesASpeciesOrASireline() throws Exception {
        JsonObject lang = GroupTextTest.lang();
        List<String> bad = new ArrayList<>();
        for (String k : lang.keySet()) {
            if (k.startsWith(H) || k.startsWith(C) || k.startsWith("item.supernaturalbloodlines.clan_charter")) {
                String text = lang.get(k).getAsString().toLowerCase();
                for (String word : List.of("original", "augustine", "species", "ghoul", "hybrid", "siphoner", "genes", "gene ")) {
                    if (text.contains(word)) {
                        bad.add(k + " (" + word + ")");
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "a clan text names a species or a hidden fact: " + bad);
    }
}
