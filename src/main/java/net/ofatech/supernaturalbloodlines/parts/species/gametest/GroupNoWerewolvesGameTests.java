package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Plan 20a, Review Focus 2: the group stack that is not wolf-only registers without Werewolves. These run in every world, but
 * they only prove something in a world with no Werewolves (the -PnoWerewolves run), where the pack stack's own registration is off.
 */
@PrefixGameTestTemplate(false)
public final class GroupNoWerewolvesGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private GroupNoWerewolvesGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCovenRecordsLastSeen(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist m = human(helper, 2.5);
        online(w, m);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(w, m), "the coven could not be built");
            PackIndex.overrideClock(() -> 42_000L);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(m));
            helper.assertTrue(index.lastSeen(m.getUUID()).orElse(-1L) == 42_000L, "a logout did not store a last-seen time: " + index.lastSeen(m.getUUID()));
        } finally {
            clean(helper, w, m);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCovenLeaderWhoStaysAwayIsSeenAsAbsent(GameTestHelper helper) {
        java.util.UUID gone = java.util.UUID.randomUUID();
        TalkingDuelist m = human(helper, 1.5);
        online(m);
        PackIndex index = PackIndex.get(helper.getLevel());
        long now = System.currentTimeMillis();
        try {
            index.join(gone, m.getUUID());
            index.setLastSeen(gone, now - 8 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 0 && index.isLeader(gone), "a coven with no heir changed hands or threw");
            helper.assertTrue(index.lastSeen(gone).orElse(0L) == now - 8 * PackAbsence.DAY_MILLIS, "the absence was forgotten");
        } finally {
            index.leave(gone);
            clean(helper, m);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHubSourceIsInstalled(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist m = human(helper, 2.5);
        online(w, m);
        try {
            helper.assertTrue(join(w, m), "the coven could not be built");
            var pack = HubSocial.of(m).pack();
            helper.assertTrue(pack.size() == 2 && pack.get(0).id().equals(java.util.Optional.of(w.getUUID())), "the hub's group rows are " + pack);
        } finally {
            clean(helper, w, m);
        }
        helper.succeed();
    }
}
