package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.core.ModActions;
import de.teamlapen.werewolves.util.FormHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMoonGift;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl.Control;

/** R96, the pack gift: a werewolf in a pack whose current leader is a hybrid, an Original hybrid or a tribrid has moon control. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class PackMoonGiftGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private PackMoonGiftGameTests() {
    }

    private static LineTestPlayer wolf(GameTestHelper helper, Rank rank, Species species, boolean bound, UUID packAlpha, int level, double x) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(species == Species.NONE ? Lineage.BITTEN : Lineage.BORN).rank(rank).geneRolled(true).protectPath(false)
                .packAlpha(packAlpha).betaSince(rank == Rank.BETA ? 1 : 0).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).hybridCurseBound(bound).wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        return p;
    }

    /** A leader who is a hybrid (unbound), or a plain Alpha. */
    private static LineTestPlayer leader(GameTestHelper helper, Species species, double x) {
        return wolf(helper, Rank.ALPHA, species, false, null, 8, x);
    }

    private static LineTestPlayer beta(GameTestHelper helper, ServerPlayer leader, int level, double x) {
        return wolf(helper, Rank.BETA, Species.NONE, false, leader.getUUID(), level, x);
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
    }

    private static void clean(ServerPlayer... players) {
        SireCall.overridePlayerLookup(null);
        SpeciesConfig.overrideHybridPackMoon(null);
        PackMoonGift.reset();
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean formCancelled(ServerPlayer p, IAction<?> action, boolean insideOwnTick) {
        if (insideOwnTick) {
            ForcedShift.enterTick(p);
        }
        try {
            var event = new ActionEvent.ActionActivatedEvent(VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(), (IAction) action, 0, 0);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled();
        } finally {
            ForcedShift.leaveTick(p);
        }
    }

    private static boolean beastCancelled(ServerPlayer p, boolean forced) {
        return formCancelled(p, ModActions.BEAST_FORM.get(), forced);
    }

    private static boolean halfCancelled(ServerPlayer p) {
        return formCancelled(p, ModActions.SURVIVAL_FORM.get(), false) || formCancelled(p, ModActions.HUMAN_FORM.get(), false);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void memberOfAHybridLedPackShiftsAtWill(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.HYBRID, 1.5);
        LineTestPlayer low = beta(helper, lead, 1, 3.5);
        LineTestPlayer high = beta(helper, lead, 8, 4.5);
        online(lead, low, high);
        try {
            helper.assertTrue(PackMoonGift.has(low) && PackMoonGift.has(high), "a Beta of a hybrid-led pack has no gift");
            helper.assertTrue(MoonControl.of(low) == Control.HALF, "a level 1 member has " + MoonControl.of(low));
            helper.assertFalse(halfCancelled(low), "the member could not take a half form at will");
            helper.assertTrue(beastCancelled(low, false), "a half-control member shifted into the beast at will");
            helper.assertTrue(MoonControl.of(high) == Control.FULL, "a level 8 member has " + MoonControl.of(high));
            helper.assertFalse(beastCancelled(high, false), "a level 8 member could not take the beast form at will");
        } finally {
            clean(lead, low, high);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void memberOfANormalAlphasPackCannotShiftAtWill(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.NONE, 1.5);
        LineTestPlayer member = beta(helper, lead, 8, 3.5);
        online(lead, member);
        try {
            helper.assertFalse(PackMoonGift.has(member), "a plain Alpha's pack gave the gift");
            helper.assertTrue(MoonControl.of(member) == Control.NONE, "a member of a plain pack has " + MoonControl.of(member));
            helper.assertTrue(beastCancelled(member, false), "the member shifted into the beast at will");
            helper.assertTrue(halfCancelled(member), "the member took a half form at will");
        } finally {
            clean(lead, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theForcedFullMoonShiftIsSkippedForTheGift(GameTestHelper helper) {
        LineTestPlayer hybrid = leader(helper, Species.ORIGINAL_HYBRID, 1.5);
        LineTestPlayer gifted = beta(helper, hybrid, 1, 3.5);
        LineTestPlayer plainLead = leader(helper, Species.NONE, 5.5);
        LineTestPlayer plain = beta(helper, plainLead, 1, 7.5);
        online(hybrid, gifted, plainLead, plain);
        try {
            helper.assertFalse(beastCancelled(plain, true), "control: the full moon's shift was cancelled for a member of a plain pack");
            helper.assertTrue(beastCancelled(gifted, true), "the full moon forced the shift on a member of a hybrid-led pack");
        } finally {
            clean(hybrid, gifted, plainLead, plain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingThePackEndsTheGift(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.TRIBRID, 1.5);
        LineTestPlayer member = beta(helper, lead, 8, 3.5);
        online(lead, member);
        try {
            helper.assertTrue(PackMoonGift.has(member) && !beastCancelledAtWill(member), "setup: no gift before leaving");
            helper.assertTrue(PackMoonGift.check(member) == null, "the first check said something");
            helper.assertTrue(PackCommand.leave(member) == null, "/pack leave was refused");
            helper.assertFalse(PackMoonGift.has(member), "the gift outlived /pack leave");
            helper.assertTrue(MoonControl.of(member) == Control.NONE, "the member kept control after leaving: " + MoonControl.of(member));
            helper.assertTrue(beastCancelledAtWill(member), "the member shifted at will after leaving");
            helper.assertTrue(PackMoonGift.LOST.equals(PackMoonGift.check(member)) && member.count(PackMoonGift.LOST) == 1, "no single 'lost' line: " + member.heard());
            helper.assertTrue(PackMoonGift.check(member) == null && member.count(PackMoonGift.LOST) == 1, "the 'lost' line came twice");
        } finally {
            clean(lead, member);
        }
        helper.succeed();
    }

    private static boolean beastCancelledAtWill(ServerPlayer p) {
        return beastCancelled(p, false);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void beingKickedEndsTheGift(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.HYBRID, 1.5);
        LineTestPlayer member = beta(helper, lead, 8, 3.5);
        LineTestPlayer other = beta(helper, lead, 8, 4.5);
        online(lead, member, other);
        try {
            helper.assertTrue(PackMoonGift.has(member), "setup: no gift before the kick");
            helper.assertTrue(PackCommand.kick(lead, member.getUUID(), "member") == null, "the kick was refused");
            helper.assertFalse(PackMoonGift.has(member), "the gift outlived the kick");
            helper.assertTrue(beastCancelledAtWill(member), "a kicked member shifted at will");
            helper.assertTrue(PackMoonGift.has(other), "the kick took the gift from somebody else");
        } finally {
            clean(lead, member, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLeadPassingToAPlainAlphaEndsTheGift(GameTestHelper helper) {
        LineTestPlayer hybrid = leader(helper, Species.HYBRID, 1.5);
        LineTestPlayer heir = wolf(helper, Rank.ALPHA, Species.NONE, false, hybrid.getUUID(), 8, 3.5);
        LineTestPlayer member = beta(helper, hybrid, 8, 5.5);
        online(hybrid, heir, member);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            index.join(hybrid.getUUID(), heir.getUUID());
            helper.assertTrue(PackMoonGift.has(member), "setup: no gift under the hybrid");
            helper.assertTrue(PackMoonGift.check(member) == null, "the first check said something");
            index.promote(hybrid.getUUID(), heir.getUUID());
            BloodlineAccess.set(member, BloodlineAccess.get(member).toBuilder().wolf(PackAccess.wolf(member).toBuilder().packAlpha(heir.getUUID()).build()).build());
            helper.assertTrue(index.leaderOf(member.getUUID()).equals(heir.getUUID()), "setup: the lead did not pass");
            helper.assertFalse(PackMoonGift.has(member), "the gift outlived the change of leader");
            helper.assertTrue(beastCancelledAtWill(member), "the member shifted at will under a plain Alpha");
            helper.assertTrue(PackMoonGift.LOST.equals(PackMoonGift.check(member)), "no 'lost' line: " + member.heard());
            // and the lead coming back brings it back, with its own line
            index.promote(heir.getUUID(), hybrid.getUUID());
            BloodlineAccess.set(member, BloodlineAccess.get(member).toBuilder().wolf(PackAccess.wolf(member).toBuilder().packAlpha(hybrid.getUUID()).build()).build());
            helper.assertTrue(PackMoonGift.has(member), "the gift did not come back with the hybrid's lead");
            helper.assertTrue(PackMoonGift.GAINED.equals(PackMoonGift.check(member)), "no 'gained' line: " + member.heard());
        } finally {
            index.leave(heir.getUUID());
            index.leave(member.getUUID());
            index.leave(hybrid.getUUID());
            clean(hybrid, heir, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHybridCoAlphaWhoDoesNotLeadGivesNothing(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.NONE, 1.5);
        LineTestPlayer coAlpha = wolf(helper, Rank.ALPHA, Species.HYBRID, false, lead.getUUID(), 8, 3.5);
        LineTestPlayer member = beta(helper, lead, 8, 5.5);
        online(lead, coAlpha, member);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            index.join(lead.getUUID(), coAlpha.getUUID());
            helper.assertTrue(index.isHybridLike(coAlpha.getUUID()), "setup: the co-Alpha is not seen as hybrid-like");
            helper.assertFalse(PackMoonGift.has(member), "a hybrid co-Alpha who is not the leader gave the gift");
            helper.assertTrue(beastCancelledAtWill(member), "the member shifted at will under a plain leader");
        } finally {
            index.leave(coAlpha.getUUID());
            clean(lead, coAlpha, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theConfigOffTakesTheGiftAway(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.HYBRID, 1.5);
        LineTestPlayer member = beta(helper, lead, 8, 3.5);
        online(lead, member);
        try {
            helper.assertTrue(PackMoonGift.has(member), "control: no gift with the config on");
            SpeciesConfig.overrideHybridPackMoon(false);
            helper.assertFalse(PackMoonGift.has(member), "hybrid_pack_moon_control = false still gave the gift");
            helper.assertTrue(MoonControl.of(member) == Control.NONE, "the member kept control with the config off: " + MoonControl.of(member));
            helper.assertTrue(beastCancelledAtWill(member), "the member shifted at will with the config off");
            SpeciesConfig.overrideHybridPackMoon(null);
            helper.assertTrue(PackMoonGift.has(member), "the gift did not come back with the config on");
        } finally {
            clean(lead, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBoundHybridLeaderAndAHybridsOwnPackmatesGiveNothing(GameTestHelper helper) {
        LineTestPlayer bound = wolf(helper, Rank.ALPHA, Species.HYBRID, true, null, 8, 1.5);
        LineTestPlayer member = beta(helper, bound, 8, 3.5);
        online(bound, member);
        try {
            helper.assertFalse(PackMoonGift.has(member), "a bound hybrid (no forms of their own) gave the gift");
            BloodlineAccess.set(bound, BloodlineAccess.get(bound).toBuilder().hybridCurseBound(false).build());
            helper.assertTrue(PackMoonGift.has(member), "the gift did not come when the hybrid was unbound");
        } finally {
            clean(bound, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberKeepsTheFormTheyAreInWhenTheyLoseTheGift(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.HYBRID, 1.5);
        LineTestPlayer member = beta(helper, lead, 14, 3.5);
        online(lead, member);
        try {
            IWerewolfPlayer w = (IWerewolfPlayer) VampirismAPI.factionPlayerHandler(member).getCurrentFactionPlayer().orElseThrow();
            toggleBeast(member);
            helper.assertTrue(FormHelper.isFormActionActive(w), "setup: the member could not take the beast form with the gift");
            helper.assertTrue(PackCommand.kick(lead, member.getUUID(), "member") == null, "the kick was refused");
            PackMoonGift.check(member);
            helper.assertFalse(PackMoonGift.has(member), "setup: the gift is still there");
            helper.assertTrue(FormHelper.isFormActionActive(w), "losing the gift threw the member out of their form");
            // the next forced shift is the usual rule again, and nothing was forced meanwhile
            helper.assertFalse(beastCancelled(member, true), "the member is still spared the forced shift");
        } finally {
            clean(lead, member);
        }
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void toggleBeast(ServerPlayer p) {
        var fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        IActionHandler handler = fp.getActionHandler();
        handler.unlockActions(List.of(ModActions.BEAST_FORM.get(), ModActions.SURVIVAL_FORM.get(), ModActions.HUMAN_FORM.get()));
        IAction.ActivationContext none = new IAction.ActivationContext() {
            @Override
            public Optional<BlockPos> targetBlock() {
                return Optional.empty();
            }

            @Override
            public Optional<Entity> targetEntity() {
                return Optional.empty();
            }
        };
        for (IAction<?> form : List.of(ModActions.BEAST_FORM.get(), ModActions.SURVIVAL_FORM.get(), ModActions.HUMAN_FORM.get())) {
            if (handler.toggleAction((IAction) form, none) == IAction.PERM.ALLOWED) {
                return;
            }
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackmateAndAnOmegaGetNothingAndTheHubSaysSo(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.HYBRID, 1.5);
        LineTestPlayer member = beta(helper, lead, 8, 3.5);
        LineTestPlayer omega = wolf(helper, Rank.OMEGA, Species.NONE, false, null, 8, 5.5);
        LineTestPlayer mate = LineTestPlayer.create(helper);
        online(lead, member, omega, mate);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            index.join(lead.getUUID(), mate.getUUID());
            helper.assertFalse(PackMoonGift.has(mate), "a Packmate who is no werewolf got the gift");
            helper.assertFalse(PackMoonGift.has(omega), "an Omega with no pack got the gift");
            helper.assertTrue(HubSocial.moonGiftOf(member), "the hub hook does not say the member has it");
            helper.assertFalse(HubSocial.moonGiftOf(omega) || HubSocial.moonGiftOf(mate), "the hub hook says somebody without it has it");
            helper.assertTrue(HubSnapshotPayload.of(member).moonGift(), "the snapshot of the member does not carry the gift");
            helper.assertFalse(HubSnapshotPayload.of(omega).moonGift(), "the snapshot of an Omega carries the gift");
        } finally {
            index.leave(mate.getUUID());
            index.leave(member.getUUID());
            clean(lead, member, omega);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void joiningAHybridLedPackSaysOnceThatTheMoonLetsGo(GameTestHelper helper) {
        LineTestPlayer lead = leader(helper, Species.HYBRID, 1.5);
        LineTestPlayer omega = wolf(helper, Rank.OMEGA, Species.NONE, false, null, 8, 3.5);
        online(lead, omega);
        try {
            helper.assertTrue(PackMoonGift.check(omega) == null && !PackMoonGift.has(omega), "setup: an Omega with no pack");
            BloodlineAccess.set(omega, BloodlineAccess.get(omega).toBuilder().wolf(PackAccess.wolf(omega).toBuilder().rank(Rank.BETA).packAlpha(lead.getUUID()).build()).build());
            helper.assertTrue(PackMoonGift.has(omega), "setup: the Beta has no gift");
            helper.assertTrue(PackMoonGift.GAINED.equals(PackMoonGift.check(omega)), "no 'gained' line: " + omega.heard());
            helper.assertTrue(PackMoonGift.check(omega) == null && omega.count(PackMoonGift.GAINED) == 1, "the 'gained' line came twice");
        } finally {
            clean(lead, omega);
        }
        helper.succeed();
    }
}
