package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.clean;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.invite;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.person;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.vampire;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.ward;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.wardAt;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardEnforcer;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardIndex;

/**
 * The ward's enforcement (R302, R303): who is turned away, the push, the timeout, the door, the settings. Vampires here are Vampirism's own faction, so
 * the tests do not need the species part. Wards are raised straight in the index around the cell's centre, 3 cells each way.
 */
@PrefixGameTestTemplate(false)
public final class WardGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final int HALF = 3;

    private WardGameTests() {
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void anUninvitedVampireIsPushedOutAndAHumanIsNot(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist vamp = vampire(helper, 2, 0);
        TalkingDuelist human = person(helper, -2, 1);
        try {
            ward(helper, owner, HALF);
            helper.assertTrue(WardEnforcer.checkNow(vamp), "the vampire was not turned away");
            helper.assertTrue(vamp.getDeltaMovement().x > 0.5 && vamp.getDeltaMovement().y > 0.1, "the push is not Ashwarden's push: " + vamp.getDeltaMovement());
            helper.assertTrue(vamp.countOnActionBar(WardEnforcer.CANNOT_ENTER) == 1, "the line came " + vamp.countOnActionBar(WardEnforcer.CANNOT_ENTER) + " times");
            WardEnforcer.checkNow(vamp);
            helper.assertTrue(vamp.countOnActionBar(WardEnforcer.CANNOT_ENTER) == 1, "the line is not said again within two seconds");
            helper.assertFalse(WardEnforcer.checkNow(human), "a human was turned away");
            helper.assertTrue(human.getDeltaMovement().lengthSqr() == 0, "a human was pushed");
            helper.assertFalse(WardEnforcer.checkNow(owner), "the owner was turned away");
        } finally {
            clean(helper, owner, vamp, human);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void anInvitedVampireAndAVampireOwnerAreLeftAlone(GameTestHelper helper) {
        TalkingDuelist owner = vampire(helper, -2, -2);
        TalkingDuelist guest = vampire(helper, 2, 0);
        TalkingDuelist stranger = vampire(helper, 0, 2);
        try {
            ward(helper, owner, HALF);
            invite(helper, owner, guest);
            helper.assertFalse(WardEnforcer.checkNow(guest), "an invited vampire was turned away");
            helper.assertFalse(WardEnforcer.checkNow(owner), "the owner (made a vampire later) was turned away");
            helper.assertTrue(WardEnforcer.checkNow(stranger), "a stranger was not turned away");
        } finally {
            clean(helper, owner, guest, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void invitationIsPerWard(GameTestHelper helper) {
        TalkingDuelist first = person(helper, -2, -2);
        TalkingDuelist second = person(helper, -2, 2);
        TalkingDuelist vamp = vampire(helper, 0, 0);
        try {
            BlockPos here = centre(helper);
            WardTestSupport.wardAt(helper, first, WardRules.Kind.AREA, here, HALF);
            WardTestSupport.wardAt(helper, second, WardRules.Kind.AREA, here, HALF);
            invite(helper, first, vamp);
            helper.assertTrue(WardEnforcer.checkNow(vamp), "an invitation into one ward let the vampire through another");
            invite(helper, second, vamp);
            helper.assertFalse(WardEnforcer.checkNow(vamp), "invited into both and still turned away");
        } finally {
            clean(helper, first, second, vamp);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aBoxedInVampireIsTeleportedOutAfterTheTimeout(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist vamp = vampire(helper, 1, 0);
        try {
            ArtifactsConfig.override("ward_expel_seconds", 1);
            WardTestSupport.ward(helper, owner, HALF);
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(WardEnforcer.checkNow(vamp), "pushed " + i);
                helper.assertTrue(centreBox(helper, HALF).contains(vamp.getX(), vamp.getY(), vamp.getZ()), "moved out before the time (the fake player does not move by itself)");
            }
            WardEnforcer.checkNow(vamp);
            helper.assertFalse(centreBox(helper, HALF).contains(vamp.getX(), vamp.getY(), vamp.getZ()), "still inside after 20 ticks: " + vamp.position());
        } finally {
            clean(helper, owner, vamp);
        }
        helper.succeed();
    }

    private static WardRules.Box centreBox(GameTestHelper helper, int half) {
        BlockPos c = centre(helper);
        return new WardRules.Box(c.getX(), c.getY(), c.getZ(), half);
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aDoorWardKeepsTheDoorShutForAnUninvitedVampire(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -4, -4);
        TalkingDuelist vamp = vampire(helper, 4, 0);
        TalkingDuelist human = person(helper, 4, 2);
        BlockPos door = helper.absolutePos(new BlockPos(12, 2, 8));
        try {
            helper.getLevel().setBlock(door, Blocks.OAK_DOOR.defaultBlockState(), 3);
            wardAt(helper, owner, WardRules.Kind.DOOR, door, 1);
            helper.assertTrue(click(vamp, door), "the door opened for an uninvited vampire");
            helper.assertFalse(click(human, door), "the door did not open for a human");
            invite(helper, owner, vamp);
            helper.assertFalse(click(vamp, door), "the door stayed shut for an invited vampire");
            ArtifactsConfig.override("ward_door_enabled", false);
            invite(helper, owner, vamp);
            WardEnforcer.reset();
            helper.assertFalse(click(vampire(helper, 4, 4), door), "the door ward stands with its switch off");
        } finally {
            helper.getLevel().setBlock(door, Blocks.AIR.defaultBlockState(), 3);
            clean(helper, owner, vamp, human);
        }
        helper.succeed();
    }

    /** Whether the ward stopped a right click on the block: the real event, as the game posts it. */
    private static boolean click(TalkingDuelist player, BlockPos at) {
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, at,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    @SuppressWarnings("unchecked")
    public static void aVampireMobIsTurnedAwayWhenTheSettingIsOn(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        try {
            ward(helper, owner, HALF);
            Mob mob = helper.spawnWithNoFreeWill((EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")), new BlockPos(8, 2, 8));
            helper.assertTrue(WardEnforcer.checkNow(mob), "a vampire mob was not turned away");
            ArtifactsConfig.override("ward_affects_mobs", false);
            mob.setDeltaMovement(Vec3.ZERO);
            helper.assertFalse(WardEnforcer.checkNow(mob), "a vampire mob was turned away with ward_affects_mobs off");
            helper.assertTrue(mob.getDeltaMovement().lengthSqr() == 0, "and pushed");
        } finally {
            clean(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aFallenWardAndASwitchedOffWardBlockNobody(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist vamp = vampire(helper, 1, 0);
        try {
            ward(helper, owner, HALF);
            helper.assertTrue(WardEnforcer.checkNow(vamp), "control: a standing ward turns the vampire away");
            ArtifactsConfig.override("ward_enabled", false);
            helper.assertFalse(WardEnforcer.checkNow(vamp), "ward_enabled = false still turned them away");
            ArtifactsConfig.override("ward_enabled", null);
            WardIndex.get(helper.getLevel()).fallAllOf(owner.getUUID());
            helper.assertFalse(WardEnforcer.checkNow(vamp), "a fallen ward turned them away");
        } finally {
            clean(helper, owner, vamp);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void creativeAndSpectatorAreNeverTurnedAway(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist creative = vampire(helper, 1, 0);
        TalkingDuelist spectator = vampire(helper, 0, 1);
        try {
            ward(helper, owner, HALF);
            creative.setGameMode(GameType.CREATIVE);
            spectator.setGameMode(GameType.SPECTATOR);
            helper.assertFalse(WardEnforcer.checkNow(creative), "a creative player was turned away");
            helper.assertFalse(WardEnforcer.checkNow(spectator), "a spectator was turned away");
            creative.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(WardEnforcer.checkNow(creative), "control: back in survival they are");
        } finally {
            clean(helper, owner, creative, spectator);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void theOwnerIsNeverToldWhoWasTurnedAwayAndOnlyWhenTheSettingIsOn(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist vamp = vampire(helper, 1, 0);
        try {
            ward(helper, owner, HALF);
            WardEnforcer.checkNow(vamp);
            helper.assertTrue(owner.received() == 0 && owner.countOnActionBar(WardEnforcer.TESTED) == 0, "the owner was told with ward_alert_owner off");
            ArtifactsConfig.override("ward_alert_owner", true);
            WardEnforcer.checkNow(vamp);
            helper.assertTrue(owner.countOnActionBar(WardEnforcer.TESTED) == 1, "the owner was not told their ward was tested");
            helper.assertTrue(owner.received() == 0, "the owner heard more than the one neutral line: " + owner.keys());
        } finally {
            clean(helper, owner, vamp);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void anOutsideVampireAndAWardInAnotherPlaceDoNothing(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist outside = vampire(helper, 6, 0);
        try {
            ward(helper, owner, HALF);
            helper.assertFalse(WardEnforcer.checkNow(outside), "a vampire outside the box was pushed");
            WardEnforcer.checkNow(vampire(helper, 0, 0));
            helper.assertTrue(outside.getDeltaMovement().lengthSqr() == 0, "and the one outside moved");
        } finally {
            clean(helper, owner, outside);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void withdrawingAnInvitationExpelsOrOnlyBarsReEntryBySetting(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist guest = vampire(helper, 1, 0);
        try {
            ward(helper, owner, HALF);
            invite(helper, owner, guest);
            helper.assertFalse(WardEnforcer.checkNow(guest), "setup: an invited guest is left alone");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardService.uninvite(owner, guest.getUUID().toString()) == null, "the withdrawal was refused");
            helper.assertTrue(guest.received(net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardService.NOT_WELCOME), "they were not told");
            helper.assertTrue(WardEnforcer.checkNow(guest), "ward_expel_on_revoke: the next check did not push them out");

            ArtifactsConfig.override("ward_expel_on_revoke", false);
            invite(helper, owner, guest);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardService.uninvite(owner, guest.getUUID().toString()) == null, "the second withdrawal was refused");
            helper.assertFalse(WardEnforcer.checkNow(guest), "expel off: somebody already inside was pushed");
            guest.setPos(guest.position().add(8, 0, 0));
            WardEnforcer.checkNow(guest);
            guest.setPos(guest.position().add(-8, 0, 0));
            helper.assertTrue(WardEnforcer.checkNow(guest), "expel off: coming back in was allowed");
        } finally {
            clean(helper, owner, guest);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void theVampireFactionLeavingAndJoiningIsReadAtEveryCheck(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -2, -2);
        TalkingDuelist p = person(helper, 1, 0);
        try {
            ward(helper, owner, HALF);
            helper.assertFalse(WardEnforcer.checkNow(p), "a human was turned away");
            VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            helper.assertTrue(WardEnforcer.checkNow(p), "the same player, now a vampire, was not turned away");
        } finally {
            clean(helper, owner, p);
        }
        helper.succeed();
    }
}
