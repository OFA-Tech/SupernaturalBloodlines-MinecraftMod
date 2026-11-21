package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import org.junit.jupiter.api.Test;

/** Plan 17b panel: the words the players read (admin labels, the Guide chapter, the hints) say what the review asked them to say. */
class BossTextTest {
    private static JsonObject lang() throws Exception {
        try (var in = new InputStreamReader(BossTextTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    private static String label(String key) {
        return "supernaturalbloodlines.hub.admin.key." + BossesConfig.FILE + ":" + key;
    }

    @Test
    void everyBossSwitchIsLabelledBossColonName() throws Exception {
        JsonObject lang = lang();
        for (BossId b : BossId.values()) {
            String key = label(b.id() + "_enabled");
            assertTrue(lang.has(key), "no admin label for " + key);
            String name = lang.get("supernaturalbloodlines.boss.name." + b.id()).getAsString();
            assertEquals("Boss: " + name, lang.get(key).getAsString(), key);
        }
    }

    @Test
    void theStakeSwitchSaysWhatItNeeds() throws Exception {
        assertEquals("NPC Original stake needs a desiccated target", lang().get(label("npc_original_stake_needs_open")).getAsString());
    }

    @Test
    void everyKolFinnAndLinkSettingHasALabelAndEveryNewThingAName() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"kol_min_mark", "kol_cloak_minutes", "kol_grimoire_sp", "finn_chance_per_eclipse", "finn_ritual_seconds", "link_seconds", "link_damage_share"}) {
            assertTrue(lang.has(label(k)), "no admin label for " + k);
        }
        for (String k : new String[] {"entity.supernaturalbloodlines.kol", "entity.supernaturalbloodlines.finn", "item.supernaturalbloodlines.kol_grimoire",
                "item.supernaturalbloodlines.esther_grimoire", "rite.supernaturalbloodlines.rite_of_cloaking", "rite.supernaturalbloodlines.rite_of_linking",
                "supernaturalbloodlines.refusal.cloak_not_witch", "supernaturalbloodlines.refusal.cloak_unread", "supernaturalbloodlines.rite.cloak_done",
                "supernaturalbloodlines.refusal.link_not_witch", "supernaturalbloodlines.refusal.link_wont_hold", "supernaturalbloodlines.rite.link_done",
                "supernaturalbloodlines.rite.link_linked", "supernaturalbloodlines.rite.link_ended", "supernaturalbloodlines.rite.cloak_faded", "supernaturalbloodlines.boss.notice.finn.hint",
                "item.supernaturalbloodlines.kol_grimoire.tooltip_who", "item.supernaturalbloodlines.esther_grimoire.tooltip_who", "supernaturalbloodlines.hub.scroll_more",
                "supernaturalbloodlines.boss.notice.kol.unknown", "supernaturalbloodlines.boss.notice.kol.known", "supernaturalbloodlines.boss.notice.kol.hint",
                "supernaturalbloodlines.boss.kol.gone", "supernaturalbloodlines.boss.notice.finn.unknown", "supernaturalbloodlines.boss.notice.finn.known",
                "supernaturalbloodlines.boss.finn.done", "supernaturalbloodlines.boss.finn.broken", "item.supernaturalbloodlines.kol_grimoire.read",
                "item.supernaturalbloodlines.kol_grimoire.already", "item.supernaturalbloodlines.kol_grimoire.tooltip", "item.supernaturalbloodlines.esther_grimoire.tooltip"}) {
            assertTrue(lang.has(k), "no text for " + k);
        }
    }

    @Test
    void theGuideExplainsPeterTheHouseTheOnePeterTheFeralWolfTheRewardAndTheClawsConsent() throws Exception {
        String guide = lang().get("supernaturalbloodlines.hub.guide.bosses.body").getAsString();
        assertTrue(guide.contains("Peter Hale waits in a burned Hale house") && guide.contains("only one Peter"), "the Guide does not say there are many houses and one Peter");
        assertTrue(guide.contains("/locate structure supernaturalbloodlines:hale_house") && guide.contains("book about the fire"), "the Guide does not say how to find a house");
        assertTrue(guide.contains("feral") && guide.contains("not a boss") && guide.contains("3 to 4 blocks") && guide.contains("only at what he can see"), "the Guide does not say what feral means or how far he pounces");
        assertTrue(guide.contains("the most to bring him down") && guide.contains("True Alpha takes none") && guide.contains("a player who is no werewolf is told so"), "the Guide leaves out who is paid");
        assertTrue(guide.contains("Memory Claws") && guide.contains("they face you, sneak with a lead in their main hand") && guide.contains("PvP must be on") && guide.contains("allowed to hurt them (no teams)"),
                "the Guide leaves out how the claws are consented to");
        assertTrue(guide.contains("as they know them then") && guide.contains("never updated") && guide.contains("12 uses, 30 seconds apart"), "the Guide leaves out that the claws take a snapshot, and their limits");
        assertTrue(guide.contains("A server can switch the houses off") && guide.contains("A server can let him be found again"), "the Guide leaves out the settings");
        String peter = guide.substring(guide.indexOf("Peter Hale waits"), guide.indexOf("Nothing but white oak kills"));
        for (String paragraph : peter.split("\n\n")) {
            assertTrue(paragraph.length() < 520, "a paragraph of the Guide is not short: " + paragraph.length());
        }
    }

    @Test
    void thePeterNoticeNeverNamesHimAndTheClawsTextsSayWhatToDoWithoutSayingAnythingAboutTheTarget() throws Exception {
        JsonObject lang = lang();
        for (String key : new String[] {"supernaturalbloodlines.boss.notice.peter", "supernaturalbloodlines.boss.notice.peter.hint"}) {
            String text = lang.get(key).getAsString().toLowerCase(java.util.Locale.ROOT);
            assertTrue(!text.contains("peter") && !text.contains("hale"), "the notice names him: " + text);
        }
        String claws = "item.supernaturalbloodlines.memory_claws.";
        String refused = lang.get(claws + "refused").getAsString();
        assertTrue(refused.contains("face you") && refused.contains("sneak") && refused.contains("lead in their main hand") && refused.contains("PvP"), "the refusal does not say what the other player must do: " + refused);
        assertTrue(lang.get(claws + "pvp_off").getAsString().contains("PvP is off"), "the claws do not say PvP is off");
        assertTrue(lang.get(claws + "wait").getAsString().contains("%s"), "the wait does not give the seconds left");
        assertTrue(lang.get(claws + "learned").getAsString().contains("Profile"), "the reader is not told where to look");
        assertTrue(lang.get(claws + "tooltip_consent").getAsString().contains("face you") && lang.get(claws + "tooltip_consent").getAsString().contains("no teams"), "the tooltip leaves out the conditions");
        assertTrue(lang.has(claws + "usage") && lang.has(claws + "used_on"), "a text of the claws is missing");
        assertEquals("Peter: burned Hale houses generate (new chunks only)", lang.get("supernaturalbloodlines.hub.admin.key.supernaturalbloodlines-bosses.toml:peter_house_enabled").getAsString());
    }

    @Test
    void theChestOfTheHouseHoldsABookWhoseTextHintsAtASurvivorWithoutNamingHim() throws Exception {
        try (var in = BossTextTest.class.getResourceAsStream("/data/supernaturalbloodlines/loot_table/chests/hale_house.json")) {
            JsonObject table = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            String json = table.toString();
            assertTrue(json.contains("minecraft:written_book") && json.contains("The Hale Fire"), "the chest has no book about the fire");
            JsonObject lang = lang();
            for (String page : new String[] {"supernaturalbloodlines.lore.hale_fire.page1", "supernaturalbloodlines.lore.hale_fire.page2"}) {
                assertTrue(json.contains(page), "the book does not use " + page);
                String text = lang.get(page).getAsString().toLowerCase(java.util.Locale.ROOT);
                assertTrue(!text.contains("peter") && !text.contains("werewolf"), "the book spoils who he is: " + text);
            }
            assertTrue(lang.get("supernaturalbloodlines.lore.hale_fire.page2").getAsString().contains("comes back"), "the book does not hint that somebody still walks");
        }
    }
    @Test
    void theWordsOfKolAndFinnNeverNameThemBeforeTheyAreDiscoveredAndTheLinkRefusalNamesNoOriginal() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"supernaturalbloodlines.boss.notice.kol.unknown", "supernaturalbloodlines.boss.notice.finn.unknown"}) {
            String text = lang.get(k).getAsString();
            assertTrue(!text.contains("Kol") && !text.contains("Finn") && !text.contains("Mikaelson"), k + ": " + text);
        }
        for (String k : new String[] {"supernaturalbloodlines.refusal.link_wont_hold"}) {
            assertTrue(!lang.get(k).getAsString().toLowerCase().contains("original"), k + " says Original");
        }
        String guide = lang.get("supernaturalbloodlines.hub.guide.bosses.body").getAsString();
        assertTrue(guide.contains("Kol comes by night") && guide.contains("Finn comes on a lunar eclipse") && guide.contains("pack Emissary"), "the Guide does not explain Kol and Finn");
        assertTrue(guide.contains("Left alone, the ritual hurts no one"), "the Guide does not say the ritual is harmless");
        assertTrue(guide.contains("more than a fifth of his health"), "the Guide does not give the threshold that breaks the ritual");
        assertTrue(guide.contains("It will not take an NPC Original, a boss") && guide.contains("That is deliberate"), "the Guide does not explain the Linking Spell's refusals");
        assertTrue(guide.contains("sneak in the circle holding a lead in their main hand") && guide.contains("witch-type rite") && guide.contains("where PvP is off") && guide.contains("a player Original may"), "the Guide leaves out who can be linked and how they agree");
        assertTrue(guide.contains("11 blocks wide") && guide.contains("in your name"), "the Guide gives neither the circle's size nor whose damage it is");
        assertTrue(guide.contains("30 minutes (a server setting)") && guide.contains("Dying ends the cloak"), "the Guide does not say how long the cloak lasts");
        String kolAndFinn = guide.substring(guide.indexOf("Kol comes by night"), guide.indexOf("Nothing but white oak kills"));
        for (String paragraph : kolAndFinn.split("\n\n")) {
            assertTrue(paragraph.length() < 520, "a paragraph of the Guide is not short: " + paragraph.length());
        }
        JsonObject l = lang();
        String hint = l.get("supernaturalbloodlines.boss.notice.finn.hint").getAsString();
        assertEquals("Left alone, the ritual hurts no one. Hurt him badly and he will fight.", hint);
        assertTrue(l.get("supernaturalbloodlines.boss.notice.deucalion").getAsString().contains("%s sparks"), "the warning does not give the threshold from the config");
        assertTrue(l.get("supernaturalbloodlines.boss.notice.deucalion.hint").getAsString().contains("does the most"), "the hint does not say who is paid");
        assertTrue(l.get("supernaturalbloodlines.hub.skill_desc.s-wol-11").getAsString().contains("player Alphas"), "the Roar's description is missing or does not say player Alphas");
        assertTrue(l.get("supernaturalbloodlines.boss.deucalion.roar_given").getAsString().contains("other player Alphas"));
        assertTrue(l.has("supernaturalbloodlines.boss.deucalion.reward_no_werewolf") && l.has("supernaturalbloodlines.boss.deucalion.reward_true_alpha"), "a branch of the reward is silent");
        String deu = guide.substring(guide.indexOf("Deucalion comes on"), guide.indexOf("Nothing but white oak kills"));
        assertTrue(deu.contains("the player who did the most to bring him down") && deu.contains("paid once") || deu.contains("The reward is paid once, to the player who did the most to bring him down"), "the Guide does not say who is paid");
        assertTrue(deu.contains("A True Alpha counts as holding the full stack") && deu.contains("later full moon") && deu.contains("A server can let him come again"), "the Guide leaves out the True Alpha, leaving, or the respawn");
        assertTrue(deu.contains("player Alphas") && deu.contains("PvP"), "the Guide does not say the Roar is for player Alphas and follows PvP");
        assertTrue(l.get("supernaturalbloodlines.boss.notice.kol.unknown").getAsString().contains("It knows how many you have hunted."), "Kol's notice does not say why");
        assertTrue(l.get("supernaturalbloodlines.boss.notice.kol.known").getAsString().contains("how many you have hunted"));
        assertEquals("The link will not hold.", l.get("supernaturalbloodlines.refusal.link_wont_hold").getAsString());
        assertTrue(l.get("supernaturalbloodlines.rite.link_linked").getAsString().contains("%s%% of the damage either of you takes is shared"));
        assertEquals("Your cloak fades.", l.get("supernaturalbloodlines.rite.cloak_faded").getAsString());
        for (String rite : new String[] {"rite_of_cloaking", "rite_of_linking"}) {
            for (int i = 1; i <= 2; i++) {
                assertTrue(l.has("supernaturalbloodlines.hub.rites.desc." + rite + "." + i), rite + " has no description line " + i);
            }
        }
        assertTrue(l.get("supernaturalbloodlines.hub.rites.desc.rite_of_cloaking.1").getAsString().contains("30 minutes"), "the cloak's duration is not in its description");
        assertTrue(l.get("supernaturalbloodlines.hub.rites.desc.rite_of_linking.2").getAsString().contains("sneaking in the circle with a lead"), "how a player agrees is not in the description");
        assertTrue(l.get("item.supernaturalbloodlines.kol_grimoire.tooltip_who").getAsString().contains("witch, siphoner, heretic or tribrid"));
    }

    @Test
    void theOriginalsSwitchIsLabelled() throws Exception {
        assertTrue(lang().has(label("mikael_stakes_originals")));
    }

    @Test
    void theGuideChapterSaysPlainlyThatYourOriginalsDeathIsYours() throws Exception {
        String body = lang().get("supernaturalbloodlines.hub.guide.bosses.body").getAsString();
        assertTrue(body.contains("If your Original dies to white oak, you die too"), body);
        assertTrue(body.contains("rung 3"), "the rung is not given");
        assertTrue(body.contains("\n\n"), "the chapter is not split into short paragraphs");
        assertTrue(lang().has("supernaturalbloodlines.hub.guide.bosses.link"), "no link to the Bloodline page");
    }

    @Test
    void klausIsNamedWarnedAboutAndExplained() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"unknown", "known", "unknown_now", "known_now", "hint"}) {
            assertTrue(lang.has("supernaturalbloodlines.boss.notice.klaus." + k), k);
        }
        assertTrue(lang.get("supernaturalbloodlines.boss.notice.klaus.unknown").getAsString().contains("%s seconds"), "the warning does not say how long");
        String hint = lang.get("supernaturalbloodlines.boss.notice.klaus.hint").getAsString();
        assertTrue(hint.contains("hybrid wolf") && hint.contains("venom"), "the hint does not name the threat: " + hint);
        assertTrue(hint.contains("laid by a pack Emissary"), "the hint claims more of mountain ash than is true: " + hint);
        assertEquals("Klaus", lang.get("entity.supernaturalbloodlines.klaus").getAsString());
        assertEquals("Klaus's Blood", lang.get("item.supernaturalbloodlines.klaus_blood").getAsString());
        String body = lang.get("supernaturalbloodlines.hub.guide.bosses.body").getAsString();
        assertTrue(body.contains("Klaus comes on a full moon") && body.contains("Werewolves must be installed") && body.contains("His Blood cures it"), body);
        assertTrue(!body.contains("90"), "the Guide names a number a server can change: " + body);
        assertTrue(body.contains("only works on vampires"), "the Guide does not say who the venom is for");
        assertTrue(body.contains("switch the player hunt off"), "the Guide does not mention the player hunt switch");
        assertTrue(body.contains("find out they carry the gene"), "the Guide does not say that being hunted reveals the gene");
        assertTrue(body.contains("laid by a pack Emissary"), "the Guide claims more of mountain ash than is true");
    }

    @Test
    void theHuntMessageSaysWhyAndTheOtherLinesAreThere() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"unknown", "known", "unknown_now", "known_now"}) {
            String text = lang.get("supernaturalbloodlines.boss.notice.klaus." + k).getAsString();
            assertTrue(text.contains("you carry the doppelgänger gene"), k + ": " + text);
        }
        assertEquals("The wolf lost your scent.", lang.get("supernaturalbloodlines.boss.klaus.lost_scent").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.boss.klaus.countdown").getAsString().contains("%s seconds"));
        assertTrue(lang.has("supernaturalbloodlines.boss.klaus.dawn"));
        assertEquals("Cures werewolf venom. Counts as an Original hybrid's blood for the hybrid transition.", lang.get("item.supernaturalbloodlines.klaus_blood.tooltip").getAsString());
        assertTrue(lang.has("item.supernaturalbloodlines.klaus_blood.dead_line"));
    }

    @Test
    void theKlausSettingsAreLabelled() throws Exception {
        for (String k : new String[] {"klaus_chance_per_full_moon", "klaus_hunts_players", "klaus_warning_seconds"}) {
            assertTrue(lang().has(label(k)), k);
        }
    }

    @Test
    void theHomeLinesAreThereForAStandingAndAFallenBossNamedOrNot() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"boss_near", "boss_near_named", "boss_fallen", "boss_fallen_named"}) {
            assertTrue(lang.has("supernaturalbloodlines.hub.home." + k), k);
        }
        assertEquals("An Original lies fallen nearby.", lang.get("supernaturalbloodlines.hub.home.boss_fallen").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.hub.home.boss_near").getAsString().contains("128"), "the Home line does not say the real distance");
    }

    @Test
    void theHintsAndTheRisingAreWritten() throws Exception {
        JsonObject lang = lang();
        assertEquals("White oak only works on an Original who is down.", lang.get("supernaturalbloodlines.boss.white_oak_closed").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.boss.notice.mikael.hint").getAsString().contains("laid by a pack Emissary will hold him"));
        assertTrue(lang.get("supernaturalbloodlines.line.risen_named").getAsString().contains("Your line is safe again."));
        assertTrue(lang.get("supernaturalbloodlines.line.risen_hidden").getAsString().contains("Your line is safe again."));
        assertTrue(lang.get("supernaturalbloodlines.boss.bar.desiccated").getAsString().contains("desiccated"));
        assertTrue(lang.has("supernaturalbloodlines.boss.notice.mikael.stake"));
    }

    @Test
    void theNewSettingsAndItemsOfPhase17dAreLabelledAndNamed() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"elijah_chance_per_day", "elijah_oath_days", "elixir_heal_fraction", "rebekah_delay_seconds"}) {
            assertTrue(lang.has(label(k)), k);
        }
        for (String k : new String[] {"entity.supernaturalbloodlines.elijah", "entity.supernaturalbloodlines.rebekah", "item.supernaturalbloodlines.the_elixir",
                "item.supernaturalbloodlines.the_true_cure", "item.supernaturalbloodlines.the_elixir.tooltip", "item.supernaturalbloodlines.the_true_cure.tooltip"}) {
            assertTrue(lang.has(k), k);
        }
    }

    @Test
    void theDealAndTheNoticesAreWrittenAndNeverNameWhoIsNotKnown() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"title", "offer.pick", "offer.oath", "offer.penalty", "no_deal", "keep", "done", "declined", "sworn", "broken", "gone", "refused"}) {
            assertTrue(lang.has("supernaturalbloodlines.boss.elijah." + k), k);
        }
        String pick = lang.get("supernaturalbloodlines.boss.elijah.offer.pick").getAsString();
        String oath = lang.get("supernaturalbloodlines.boss.elijah.offer.oath").getAsString();
        String penalty = lang.get("supernaturalbloodlines.boss.elijah.offer.penalty").getAsString();
        assertEquals("Choose whose blood you receive.", pick);
        assertTrue(oath.contains("%s") && oath.contains("harm or desiccate no NPC Original but Mikael, and do not strike me"), "the oath terms: " + oath);
        assertEquals("Break your word and I turn on you for good. No second deal.", penalty);
        assertTrue(oath.contains("and anything you tame"), "the oath leaves out what you tame: " + oath);
        assertTrue(lang.get("supernaturalbloodlines.boss.elijah.sworn").getAsString().contains("and anything you tame"));
        String tooltip = lang.get("item.supernaturalbloodlines.the_elixir.tooltip").getAsString();
        assertTrue(tooltip.contains("turning into something") && tooltip.contains("void") && tooltip.contains("/kill") && tooltip.contains("line death"), "the Elixir tooltip: " + tooltip);
        assertEquals("%s in-game day", lang.get("supernaturalbloodlines.boss.days.one").getAsString());
        assertEquals("%s in-game days", lang.get("supernaturalbloodlines.boss.days.other").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.boss.elijah.sworn").getAsString().contains("harm or desiccate no NPC Original but Mikael"), "the chat line leaves the terms out");
        assertEquals("The rite is done. Something old has noticed.", lang.get("supernaturalbloodlines.boss.rebekah.rite").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.boss.notice.rebekah.known").getAsString().startsWith("Rebekah is here"), "the arrival notice");
        String unknownElijah = lang.get("supernaturalbloodlines.boss.notice.elijah.unknown").getAsString();
        String unknownRebekah = lang.get("supernaturalbloodlines.boss.notice.rebekah.unknown").getAsString();
        assertTrue(!unknownElijah.contains("Elijah") && !unknownRebekah.contains("Rebekah"), "an undiscovered Original is named: " + unknownElijah + " / " + unknownRebekah);
        assertTrue(lang.get("supernaturalbloodlines.boss.notice.elijah.known").getAsString().contains("Elijah"));
        assertTrue(lang.get("supernaturalbloodlines.boss.notice.rebekah.known").getAsString().contains("Rebekah"));
        assertTrue(lang.get("supernaturalbloodlines.boss.notice.rebekah.hint").getAsString().contains("laid by a pack Emissary"), "a claim about mountain ash that is not true");
    }

    @Test
    void theGuideSaysWhatTheyDoAndOnlyWhatIsTrue() throws Exception {
        String body = lang().get("supernaturalbloodlines.hub.guide.bosses.body").getAsString();
        assertTrue(body.contains("Elijah is calm") && body.contains("the Elixir") && body.contains("any but himself"), "the Elijah paragraph");
        assertTrue(body.contains("NPC Original") && body.contains("in-game days"), "the Guide says NPC Original and in-game days");
        assertTrue(body.contains("anything you tame") && body.contains("turning into something, or the void"), "the Guide leaves out the tame and the Elixir's exceptions");
        assertTrue(body.contains("hostile for good") && body.contains("Mikael"), "the oath and its one exception");
        assertTrue(body.contains("Rebekah comes for whoever makes The Cure at the Rite of the Cure"), "the Rebekah paragraph");
        assertTrue(body.contains("A Cure that was given or dropped does not call her"), "she is not called by a given Cure");
        assertTrue(body.contains("laid by a pack Emissary holds her, and Elijah too"), "the mountain ash claim for Elijah and Rebekah (the GameTest shows it is true)");
        assertTrue(!body.contains("7 days") && !body.contains("60 seconds"), "the Guide names a number a server can change: " + body);
    }
}
