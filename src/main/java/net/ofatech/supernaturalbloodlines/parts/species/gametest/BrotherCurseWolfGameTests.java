package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import de.teamlapen.werewolves.api.WReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.FiveRoster;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterCurse;

/** Plan 20d Task 9: a werewolf is prey (R81), so one who kills a brother is cursed too. Registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class BrotherCurseWolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BrotherCurseWolfGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfTypeKillerIsCursedToo(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        ServerPlayer wolf = HunterMarkGameTests.human(helper);
        online(leader, brother, wolf);
        HunterMarkGameTests.online(leader, brother, wolf);
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            HunterMarkGameTests.handler(wolf).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
            HunterMarkGameTests.kill(wolf, brother);
            helper.assertTrue(BloodlineAccess.get(wolf).hunterCurse() && GroupIndex.get(helper.getLevel()).curseUntil(wolf.getUUID()).isPresent(), "a werewolf killer was not cursed with an end");
        } finally {
            HunterMarkGameTests.clearHooks();
            GroupIndex.get(helper.getLevel()).clearCurse(wolf.getUUID());
            FiveRoster.get(helper.getLevel()).setCursed(wolf.getUUID(), false);
            HunterCurse.onLogout(wolf);
            HunterMarkGameTests.reset(wolf);
            clean(helper, leader, brother);
        }
        helper.succeed();
    }
}