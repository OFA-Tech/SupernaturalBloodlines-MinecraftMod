package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import org.junit.jupiter.api.Test;

/**
 * "Narration for every target": every control of the new pages is read out through a lang key (the tooltip is what the narrator says), so a
 * target with no words fails here by name. Covers the nav, the groups, each request kind and answer, each profile fact, action, reason and
 * confirmation, and the admin sections.
 */
class HubLangCoverageTest {
    private static final String H = "supernaturalbloodlines.hub.";

    private static JsonObject lang() throws Exception {
        try (var in = new InputStreamReader(HubLangCoverageTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    @Test
    void noPlayerFacingTextTellsThePlayerToTypeACommandOrDumpsAState() throws Exception {
        JsonObject lang = lang();
        List<String> bad = new ArrayList<>();
        for (var e : lang.entrySet()) {
            if (!e.getKey().startsWith("supernaturalbloodlines.hub.") || !e.getValue().isJsonPrimitive()) {
                continue;
            }
            String v = e.getValue().getAsString();
            if (v.matches("(?s).*(type|run|use|enter) /.*") || v.matches("(?s).*\\s/(pack|bloodline|supernaturalbloodlines) .*") || v.contains("State[")) {
                bad.add(e.getKey() + " = " + v);
            }
        }
        assertTrue(bad.isEmpty(), "text that sends the player to the command line, or prints a raw state: " + bad);
    }

    @Test
    void everyControlOfTheNewPagesHasWords() throws Exception {
        JsonObject lang = lang();
        List<String> keys = new ArrayList<>();
        for (HubPage p : HubPage.values()) {
            keys.add(p.langKey());
            keys.add(p.group().langKey());
        }
        for (HubRequests.Kind k : HubRequests.Kind.values()) {
            keys.add(H + "request.kind." + lower(k));
            for (HomePage.Answer a : HomePage.answers(k)) {
                keys.add(H + "home.answer." + a.label());
            }
        }
        keys.add(H + "home.answer.dismiss");
        for (Fact f : Fact.values()) {
            keys.add(H + "profile.fact." + lower(f));
            keys.add(H + "profile.howto." + lower(f));
        }
        for (String g : ProfilePage.GROUPS.keySet()) {
            keys.add(H + "profile.group." + g);
        }
        for (ProfileActions.Role r : ProfileActions.Role.values()) {
            if (r != ProfileActions.Role.NONE) {
                keys.add(H + "pack.member." + lower(r).replace("challenger", "coalpha_challenger").replace("leader", "alpha"));
            }
        }
        List<String> actionIds = List.of("leave", "anchor_clear", "challenge", "accept_duel", "decline_duel", "invite", "emissary", "emissary_clear", "heal", "kick",
                "ally_remove", "anchor", "severance", "open_admin", "protect_on", "protect_off");
        for (String id : actionIds) {
            keys.add(H + "profile.action." + id);
        }
        for (String id : List.of("leave", "challenge", "heal", "kick", "ally_remove", "protect_off")) {
            keys.add(H + "profile.confirm." + id);
        }
        keys.add(H + "profile.disabled.leader");
        keys.add(H + "profile.disabled.true_alpha");
        keys.add(H + "profile.disabled.in_duel");
        for (var ph : net.ofatech.supernaturalbloodlines.shared.hub.profile.DuelView.Phase.values()) {
            keys.add(H + "profile.duel." + lower(ph));
        }
        keys.add(H + "group.operator");
        keys.add(H + "profile.link");
        for (String id : actionIds) {
            keys.add(H + "profile.action." + id + ".what");
        }
        for (String k : List.of("bonus_title", "actions_title", "no_actions", "no_actions_npc", "rules_title", "rule.loser", "rule.dimension", "rule.range", "rule.cooldown", "rule.free",
                "rule.state.ok", "rule.state.fail", "rule.state.unknown")) {
            keys.add(H + "profile." + k);
        }
        keys.add(H + "profile.npc");
        for (String k : List.of("next_title", "caught_up", "ready", "claim_all")) {
            keys.add(H + "home." + k);
        }
        for (String s : AdminEntries.SECTIONS) {
            keys.add(H + "admin.section." + s);
        }
        for (String s : List.of("debug", "parts")) {
            keys.add(H + "admin.section." + s);
        }
        for (String a : List.of("get", "show", "backfill", "reset", "tree", "set", "event", "sp", "rung", "gene_on", "gene_off", "eclipse_start", "eclipse_stop")) {
            keys.add(H + "admin.act." + a);
        }
        for (String s : List.of("look", "hud", "keys")) {
            keys.add(H + "settings.section." + s);
        }
        keys.add(H + "true_alpha.keep");
        keys.add(H + "settings.protect");
        keys.add(H + "settings.protect.about");
        List<String> missing = keys.stream().filter(k -> !lang.has(k)).toList();
        assertTrue(missing.isEmpty(), "controls with no words in en_us.json: " + missing);
    }

    @Test
    void everyProfileActionRowIsAKnownCommandRow() {
        for (String row : List.of("P1", "P3", "P4", "P7", "P8", "P9", "P10", "P11", "P13", "P15", "P16", "P17", "B3")) {
            assertTrue(AdminCommands.byId(row).isPresent(), row);
        }
    }
}

