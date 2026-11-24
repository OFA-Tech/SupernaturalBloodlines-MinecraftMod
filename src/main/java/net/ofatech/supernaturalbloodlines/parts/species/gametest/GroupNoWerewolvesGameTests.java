package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandReplyPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommands;
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

    /**
     * A coven whose leader becomes a hybrid is a pack. Without Werewolves nobody is an Alpha, so a pack's leader must be fit by being a hybrid-type
     * kind (spec Q3): the pack can still invite, kick and be left. (With Werewolves the gate is the Alpha rule, so this test lifts it with open founding
     * and only the no-Werewolves run proves the fix.)
     */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGroupSwitchedToAPackCanStillInviteKickAndLeave(GameTestHelper helper) {
        TalkingDuelist leader = witch(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(leader, a, b);
        if (net.neoforged.fml.ModList.get().isLoaded("werewolves")) {
            GroupsConfig.override("open_founding", true);
        }
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, a), "the coven could not be built");
            BloodlineAccess.set(leader, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            GroupSwitcher.refresh(leader);
            helper.assertTrue(Groups.typeOf(a) == GroupType.PACK, "the group is " + Groups.typeOf(a));
            var refused = PackCommand.invite(leader, b);
            helper.assertTrue(refused == null, "a pack with a hybrid leader could not invite: " + GroupTestKit.key(refused));
            helper.assertTrue(PackCommand.accept(b, PackCommand.requests().tokenOf(leader.getUUID(), b.getUUID()).orElseThrow()) == null, "the invited could not join the pack");
            helper.assertTrue(index.betasOf(leader.getUUID()).size() == 2, "the pack holds " + index.betasOf(leader.getUUID()));
            helper.assertTrue(PackCommand.kickNamed(leader, a.getUUID().toString()) == null && index.alphaOf(a.getUUID()).isEmpty(), "the pack could not kick");
            helper.assertTrue(PackCommand.leave(b) == null && index.alphaOf(b.getUUID()).isEmpty(), "a member could not leave the pack");
        } finally {
            clean(helper, leader, a, b);
        }
        helper.succeed();
    }

    /** The hub's leave (P3) and kick (P4) buttons run as /group in a world with no /pack, and really do the work. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHubLeaveAndKickRowsRunThroughTheGroupTree(GameTestHelper helper) {
        TalkingDuelist leader = witch(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(leader, a, b);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, a) && join(leader, b), "the coven could not be built");
            List<HubCommandReplyPayload> out = new ArrayList<>();
            HubCommands.run(leader, 0, new HubCommandPayload("P4", List.of(b.getUUID().toString())), p -> p.createCommandSourceStack(), out::add);
            helper.assertTrue(!out.isEmpty() && out.get(out.size() - 1).success() && index.alphaOf(b.getUUID()).isEmpty(), "P4 did not kick: " + out);
            out.clear();
            HubCommands.run(a, 0, new HubCommandPayload("P3", List.of()), p -> p.createCommandSourceStack(), out::add);
            helper.assertTrue(!out.isEmpty() && out.get(out.size() - 1).success() && index.alphaOf(a.getUUID()).isEmpty(), "P3 did not leave: " + out);
        } finally {
            clean(helper, leader, a, b);
        }
        helper.succeed();
    }}
