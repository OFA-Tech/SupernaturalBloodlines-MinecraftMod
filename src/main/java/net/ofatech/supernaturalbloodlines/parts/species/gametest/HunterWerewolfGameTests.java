package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.*;

import de.teamlapen.werewolves.api.WReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterCurse;

/** The werewolf half of the hunters' prey (R81, R83). Registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class HunterWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private HunterWerewolfGameTests() {}

    private static ServerPlayer werewolf(GameTestHelper helper) {
        ServerPlayer p = human(helper);
        handler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void factionWerewolfIsPrey(GameTestHelper helper) {
        ServerPlayer wolf = werewolf(helper);
        try {
            helper.assertTrue(Supernatural.isPrey(wolf) && Supernatural.is(wolf) && !Supernatural.isVampiric(wolf), "a faction werewolf is not prey");
        } finally {
            reset(wolf);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markGrowsOnWerewolfKill(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        ServerPlayer victim = werewolf(helper);
        try {
            kill(hunter, victim);
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 1, "mark is " + BloodlineAccess.get(hunter).markKills());
        } finally {
            reset(hunter, victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfKillerIsCursed(GameTestHelper helper) {
        ServerPlayer killer = werewolf(helper);
        ServerPlayer hunter = marked(helper, 6);
        try {
            online();
            kill(killer, hunter);
            helper.assertTrue(BloodlineAccess.get(hunter).species() == Species.NONE, "the hunter is " + BloodlineAccess.get(hunter).species());
            helper.assertTrue(BloodlineAccess.get(killer).hunterCurse(), "the werewolf was not cursed");
            helper.assertTrue(HunterCurse.nextDue(killer).isPresent(), "no hallucination scheduled");
        } finally {
            clearHooks();
            reset(killer, hunter);
        }
        helper.succeed();
    }
}
