package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** The attachment on a real player: read, write, listeners, and what a death keeps. */
@PrefixGameTestTemplate(false)
public final class SupernaturalStateGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SupernaturalStateGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void newPlayerHasNoProgress(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(SupernaturalAccess.get(p).equals(SupernaturalState.NONE), "a new player did not start at NONE");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void setStoresAndNotifiesOnlyOnChange(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<SupernaturalAccess.SupernaturalChange> seen = new ArrayList<>();
        SupernaturalAccess.addListener((who, change) -> {
            if (who == p) {
                seen.add(change);
            }
        });
        SupernaturalState one = SupernaturalState.NONE.toBuilder().sp(3).rung(SupernaturalRace.VAMPIRE, 1).build();
        SupernaturalAccess.set(p, one);
        SupernaturalAccess.set(p, one);
        helper.assertTrue(SupernaturalAccess.get(p).equals(one), "the state was not stored");
        helper.assertTrue(seen.size() == 1, "expected one notification, got " + seen.size());
        helper.assertTrue(seen.get(0).before().equals(SupernaturalState.NONE) && seen.get(0).after().equals(one),
                "the change did not carry before and after");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aThrowingListenerDoesNotLoseTheChange(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        SupernaturalAccess.addListener((who, change) -> {
            if (who == p) {
                // this test expects the access to log this and carry on
                throw new IllegalStateException("EXPECTED: deliberate failure of a test listener");
            }
        });
        SupernaturalState s = SupernaturalState.NONE.toBuilder().sp(1).build();
        SupernaturalAccess.set(p, s);
        helper.assertTrue(SupernaturalAccess.get(p).equals(s), "a throwing listener undid the change");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void stateSurvivesSerialisationAndIsCopiedOnDeath(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        SupernaturalState s = SupernaturalState.NONE.toBuilder().sp(2).rung(SupernaturalRace.WEREWOLF, 4)
                .skills(java.util.Set.of("S-WOL-1")).build();
        SupernaturalAccess.set(p, s);
        ServerPlayer clone = TestPlayers.create(helper);
        clone.restoreFrom(p, false); // what a respawn does
        helper.assertTrue(SupernaturalAccess.get(clone).equals(s), "a death lost the supernatural state");
        helper.succeed();
    }
}
