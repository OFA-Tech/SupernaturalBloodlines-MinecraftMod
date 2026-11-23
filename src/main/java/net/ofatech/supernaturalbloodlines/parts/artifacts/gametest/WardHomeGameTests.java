package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.clean;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.invite;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.person;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.species;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.vampire;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.ward;
import static net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WardTestSupport.wardsOf;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.ClaimGuard;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardChat;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardIndex;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardLifecycle;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardService;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardView;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;
import net.ofatech.supernaturalbloodlines.shared.hub.WardHub;

/**
 * Who raises a ward and how, what the owner does with it, and how it falls (R300 to R309): the Rites of the Ward through their real rite files, the
 * invite service and the spoken phrase, the two falls, and what the hub is told. Species-based tests need the species part and end early without it.
 */
@PrefixGameTestTemplate(false)
public final class WardHomeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String DUST = NS + ":mountain_ash_dust";

    private WardHomeGameTests() {
    }

    private static boolean speciesOn() {
        return EnabledParts.isEnabled("species");
    }

    private static List<ItemStack> wardItems() {
        return List.of(new ItemStack(RiteTestSupport.item(DUST), 4), new ItemStack(RiteTestSupport.item("minecraft:iron_ingot")), new ItemStack(RiteTestSupport.item("minecraft:amethyst_shard"), 2));
    }

    private static List<ItemStack> doorItems() {
        return List.of(new ItemStack(RiteTestSupport.item(DUST), 2), new ItemStack(RiteTestSupport.item("minecraft:iron_nugget"), 4));
    }

    private static TalkingDuelist create(GameTestHelper helper) {
        TalkingDuelist p = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.createTalkingDuelist(helper);
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    /** A person in the level, 3 blocks from the circle's centre (not on it). */
    private static TalkingDuelist beside(GameTestHelper helper) {
        TalkingDuelist p = create(helper);
        p.setPos(Vec3.atBottomCenterOf(RiteTestSupport.centre(helper)).add(3, 1, 0));
        return p;
    }

    private static TalkingDuelist onCentre(GameTestHelper helper, boolean sneaking) {
        TalkingDuelist p = create(helper);
        p.setPos(Vec3.atBottomCenterOf(RiteTestSupport.centre(helper)).add(0, 1, 0));
        p.setShiftKeyDown(sneaking);
        return p;
    }

    private static void done(GameTestHelper helper, TalkingDuelist... people) {
        clean(helper, people);
        RiteTestSupport.clearDropped(helper);
    }

    private static int worldWards(GameTestHelper helper) {
        return WardIndex.get(helper.getLevel()).table().all().size();
    }

    // ---- the rite ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aWitchWardsHerOwnHomeWithNobodyOnTheCentre(GameTestHelper helper) {
        TalkingDuelist witch = beside(helper);
        try {
            species(witch, Species.WITCH);
            RiteTestSupport.start(helper, "rite_of_the_ward", witch, wardItems());
            List<WardTable.Ward> wards = WardIndex.get(helper.getLevel()).table().ownedBy(witch.getUUID());
            helper.assertTrue(wards.size() == 1 && wards.get(0).kind() == WardRules.Kind.AREA, "no area ward was raised: " + wards.size());
            helper.assertTrue(wards.get(0).half() == ArtifactsConfig.wardRadius(), "the radius is not ward_radius: " + wards.get(0).half());
            helper.assertTrue(wards.get(0).cx() == RiteTestSupport.centre(helper).getX() && wards.get(0).cz() == RiteTestSupport.centre(helper).getZ(), "not on the altar");
            helper.assertTrue(witch.countOnActionBar(WardRiteFactory.RAISED) == 1, "she was not told");
        } finally {
            done(helper, witch);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aSneakingPlayerOnTheCentreOwnsTheWard(GameTestHelper helper) {
        TalkingDuelist witch = beside(helper);
        TalkingDuelist friend = onCentre(helper, true);
        try {
            species(witch, Species.WITCH);
            RiteTestSupport.start(helper, "rite_of_the_ward", witch, wardItems());
            helper.assertTrue(wardsOf(helper, friend) == 1 && wardsOf(helper, witch) == 0, "the owner is not the one on the centre");
            helper.assertTrue(WardIndex.get(helper.getLevel()).table().ownedBy(friend.getUUID()).get(0).invited().contains(witch.getUUID()), "the caster is not on the list");
        } finally {
            done(helper, witch, friend);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aStandingPlayerOnTheCentreCancelsTheWardAndEverythingComesBack(GameTestHelper helper) {
        TalkingDuelist witch = beside(helper);
        TalkingDuelist stranger = onCentre(helper, false);
        try {
            species(witch, Species.WITCH);
            RiteTestSupport.start(helper, "rite_of_the_ward", witch, wardItems());
            helper.assertTrue(worldWards(helper) == 0, "a ward was raised over somebody who did not agree");
            helper.assertTrue(witch.countOnActionBar(WardRiteFactory.CANCELLED) == 1, "the caster was not told");
            helper.assertTrue(stranger.countOnActionBar(WardRiteFactory.SNEAK_TO_OWN) == 1, "the one on the centre was not asked");
            helper.assertTrue(RiteTestSupport.dropped(helper, DUST) == 4, "the ingredients did not come back: " + RiteTestSupport.dropped(helper, DUST));
        } finally {
            done(helper, witch, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void everyWardRefusalIsOneMessageAtOneCost(GameTestHelper helper) {
        TalkingDuelist caster = beside(helper);
        TalkingDuelist guest = onCentre(helper, true);
        try {
            species(caster, Species.WITCH);
            // the refusals, one after the other: a vampiric owner, a full list of wards, claimed ground, the switch off, a caster who is not a witch type
            VampirismAPI.factionPlayerHandler(guest).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            refuse(helper, caster, "a vampiric owner");
            VampirismAPI.factionPlayerHandler(guest).leaveFaction(false);

            ArtifactsConfig.override("ward_max_per_owner", 1);
            ward(helper, guest, 1);
            refuse(helper, caster, "the owner already has the most wards");
            ArtifactsConfig.override("ward_max_per_owner", null);
            WardIndex.get(helper.getLevel()).drop(WardIndex.get(helper.getLevel()).table().ownedBy(guest.getUUID()).get(0).id(), guest.getUUID());

            ClaimGuard.install((level, pos, owner) -> false);
            refuse(helper, caster, "claimed ground");
            ClaimGuard.install(null);

            ArtifactsConfig.override("ward_enabled", false);
            refuse(helper, caster, "ward_enabled off");
            ArtifactsConfig.override("ward_enabled", null);

            species(caster, Species.NONE);
            refuse(helper, caster, "a caster who is not a witch type");
        } finally {
            done(helper, caster, guest);
        }
        helper.succeed();
    }

    /** One rite that must be refused: the same one line, nothing raised, the ingredients spent (none dropped). */
    private static void refuse(GameTestHelper helper, TalkingDuelist caster, String why) {
        int before = worldWards(helper);
        int told = caster.countOnActionBar(WardRiteFactory.WONT_TAKE);
        RiteTestSupport.start(helper, "rite_of_the_ward", caster, wardItems());
        helper.assertTrue(worldWards(helper) == before, why + ": a ward was raised");
        helper.assertTrue(caster.countOnActionBar(WardRiteFactory.WONT_TAKE) == told + 1, why + ": the one line did not come exactly once");
        helper.assertTrue(RiteTestSupport.dropped(helper, DUST) == 0, why + ": the ingredients came back, so this refusal costs differently");
        helper.assertTrue(caster.countOnActionBar(WardRiteFactory.CANCELLED) == 0, why + ": a different message came");
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aVampiricWitchCannotOwnButCanRaiseOneForAHuman(GameTestHelper helper) {
        if (!speciesOn()) {
            helper.succeed();
            return;
        }
        TalkingDuelist tribrid = beside(helper);
        TalkingDuelist human = onCentre(helper, true);
        try {
            species(tribrid, Species.TRIBRID);
            human.setShiftKeyDown(false);
            human.setPos(human.position().add(0, 0, 40)); // away from the centre: the tribrid is alone on it, so she would be the owner
            RiteTestSupport.start(helper, "rite_of_the_ward", tribrid, wardItems());
            helper.assertTrue(worldWards(helper) == 0, "a tribrid (a vampiric type) owned a ward");
            human.setPos(Vec3.atBottomCenterOf(RiteTestSupport.centre(helper)).add(0, 1, 0));
            human.setShiftKeyDown(true);
            RiteTestSupport.clearDropped(helper);
            RiteTestSupport.start(helper, "rite_of_the_ward", tribrid, wardItems());
            helper.assertTrue(wardsOf(helper, human) == 1, "the same tribrid could not raise one for a sneaking human");
        } finally {
            done(helper, tribrid, human);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void everyVampiricTypeIsRefusedAndTheOthersAreNot(GameTestHelper helper) {
        if (!speciesOn()) {
            helper.succeed();
            return;
        }
        TalkingDuelist caster = beside(helper);
        TalkingDuelist owner = onCentre(helper, true);
        try {
            species(caster, Species.WITCH);
            for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID, Species.HERETIC}) {
                species(owner, s);
                RiteTestSupport.start(helper, "rite_of_the_ward", caster, wardItems());
                helper.assertTrue(wardsOf(helper, owner) == 0, s + " owned a ward");
            }
            for (Species s : new Species[] {Species.NONE, Species.WITCH, Species.SIPHONER, Species.POTENTIAL}) {
                species(owner, s);
                RiteTestSupport.start(helper, "rite_of_the_ward", caster, wardItems());
                helper.assertTrue(wardsOf(helper, owner) >= 1, s + " could not own a ward");
                WardIndex.get(helper.getLevel()).table().ownedBy(owner.getUUID()).forEach(w -> WardIndex.get(helper.getLevel()).drop(w.id(), owner.getUUID()));
            }
        } finally {
            done(helper, caster, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aDoorWardMarksTheNearestDoorWithinFourAndNeedsOne(GameTestHelper helper) {
        TalkingDuelist witch = beside(helper);
        BlockPos near = RiteTestSupport.centre(helper).offset(2, 0, 0);
        BlockPos far = RiteTestSupport.centre(helper).offset(3, 0, 3);
        try {
            species(witch, Species.WITCH);
            RiteTestSupport.start(helper, "rite_of_the_door_ward", witch, doorItems());
            helper.assertTrue(worldWards(helper) == 0, "a door ward was raised with no door near");
            helper.getLevel().setBlock(far, Blocks.OAK_DOOR.defaultBlockState(), 3);
            helper.getLevel().setBlock(near, Blocks.OAK_DOOR.defaultBlockState(), 3);
            RiteTestSupport.start(helper, "rite_of_the_door_ward", witch, doorItems());
            List<WardTable.Ward> wards = WardIndex.get(helper.getLevel()).table().ownedBy(witch.getUUID());
            helper.assertTrue(wards.size() == 1 && wards.get(0).kind() == WardRules.Kind.DOOR && wards.get(0).half() == 1, "no door ward of half-size 1");
            helper.assertTrue(wards.get(0).cx() == near.getX() && wards.get(0).cz() == near.getZ(), "not the nearest door: " + wards.get(0).cx() + "," + wards.get(0).cz());
        } finally {
            helper.getLevel().setBlock(near, Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(far, Blocks.AIR.defaultBlockState(), 3);
            done(helper, witch);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void claimedGroundIsRefusedAndTheOwnersOwnIsNot(GameTestHelper helper) {
        TalkingDuelist witch = beside(helper);
        try {
            species(witch, Species.WITCH);
            ClaimGuard.install((level, pos, owner) -> owner.equals(witch.getUUID()));
            RiteTestSupport.start(helper, "rite_of_the_ward", witch, wardItems());
            helper.assertTrue(wardsOf(helper, witch) == 1, "the owner's own claim refused her");
            ArtifactsConfig.override("ward_use_claims", false);
            ClaimGuard.install((level, pos, owner) -> false);
            RiteTestSupport.start(helper, "rite_of_the_ward", witch, wardItems());
            helper.assertTrue(wardsOf(helper, witch) == 2, "ward_use_claims off still asked the claims");
        } finally {
            done(helper, witch);
        }
        helper.succeed();
    }

    // ---- the owner's service: invite, withdraw, drop, speak ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void invitingAndWithdrawingWorkAndTellBothSides(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        TalkingDuelist guest = person(helper, 1, 1);
        try {
            helper.assertTrue(WardService.invite(owner, guest) != null, "invited with no ward");
            ward(helper, owner, 3);
            helper.assertTrue(WardService.invite(owner, guest) == null, "the invitation was refused");
            helper.assertTrue(guest.received(WardService.INVITED_YOU) && owner.received(WardService.YOU_INVITED), "not both told");
            helper.assertTrue(WardService.invite(owner, guest) == null && guest.count(WardService.INVITED_YOU) == 1, "inviting twice told them twice");
            helper.assertTrue(WardService.uninvite(owner, guest.getUUID().toString()) == null, "the withdrawal was refused");
            helper.assertTrue(WardService.uninvite(owner, guest.getUUID().toString()) != null, "a withdrawal of nobody worked");
            helper.assertTrue(WardService.uninvite(owner, "NobodyAtAll") != null, "a name nobody has worked");
        } finally {
            clean(helper, owner, guest);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void theInviteListHasACapAndOnlyTheOwnerDropsAWard(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        TalkingDuelist one = person(helper, 1, 1);
        TalkingDuelist two = person(helper, 1, -1);
        try {
            ArtifactsConfig.override("ward_max_invites", 2);
            WardTable.Ward ward = ward(helper, owner, 3);
            helper.assertTrue(WardService.invite(owner, one) == null, "the first invitation was refused");
            helper.assertTrue(WardService.invite(owner, two) != null, "a list of two (the owner and one) took a third name");
            helper.assertTrue(WardService.drop(one, ward.id()) != null && WardIndex.get(helper.getLevel()).table().get(ward.id()).isPresent(), "another player dropped the ward");
            helper.assertTrue(WardService.drop(owner, ward.id()) == null && WardIndex.get(helper.getLevel()).table().get(ward.id()).isEmpty(), "the owner could not drop it");
        } finally {
            clean(helper, owner, one, two);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void sayingComeInInvitesTheNearestAndGetOutWithdrawsThem(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        TalkingDuelist near = person(helper, 2, 0);
        TalkingDuelist far = person(helper, 4.9, 0);
        TalkingDuelist stranger = person(helper, 20, 20);
        try {
            WardTable.Ward ward = ward(helper, owner, 3);
            WardChat.speak(stranger, true);
            helper.assertTrue(ward.invited().size() == 1, "somebody who owns no ward invited a person");
            WardChat.speak(owner, true);
            helper.assertTrue(ward.invited().contains(near.getUUID()) && !ward.invited().contains(far.getUUID()), "not the nearest one: " + ward.invited().size());
            WardChat.speak(owner, false);
            helper.assertFalse(ward.invited().contains(near.getUUID()), "'get out' did not withdraw them");
            ArtifactsConfig.override("ward_spoken_invites", false);
            WardChat.speak(owner, true);
            helper.assertFalse(ward.invited().contains(near.getUUID()), "ward_spoken_invites off still invited");
            ArtifactsConfig.override("ward_spoken_invites", null);
            owner.setPos(owner.position().add(40, 0, 0));
            WardChat.speak(owner, true);
            helper.assertFalse(ward.invited().contains(near.getUUID()), "an owner far from the ward invited somebody");
        } finally {
            clean(helper, owner, near, far, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void sayingItWithNobodyAroundDoesNothingAndSaysNothing(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        try {
            ward(helper, owner, 3);
            int told = owner.received();
            WardChat.speak(owner, true);
            helper.assertTrue(owner.received() == told, "a phrase that found nobody said something: " + owner.keys());
        } finally {
            clean(helper, owner);
        }
        helper.succeed();
    }

    // ---- the two falls ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void anOwnersDeathFallsEveryWardOfTheirsAndNobodyElses(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        TalkingDuelist other = person(helper, 1, 1);
        try {
            WardTable.Ward a = ward(helper, owner, 3);
            WardTable.Ward b = WardTestSupport.wardAt(helper, owner, WardRules.Kind.DOOR, centre(helper).above(), 1);
            WardTable.Ward c = ward(helper, other, 3);
            CommonHooks.onLivingDeath(owner, helper.getLevel().damageSources().generic());
            helper.assertTrue(a.fallen() && b.fallen(), "a ward of the dead owner still stands");
            helper.assertFalse(c.fallen(), "somebody else's ward fell");
            owner.setHealth(owner.getMaxHealth());
            helper.assertTrue(a.fallen(), "a fallen ward came back");
        } finally {
            clean(helper, owner, other);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aFallenWardInAnUnloadedChunkNeedsNoChunk(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        try {
            BlockPos faraway = new BlockPos(centre(helper).getX() + 4_000_000, 70, centre(helper).getZ() + 4_000_000);
            WardTable.Ward ward = WardTestSupport.wardAt(helper, owner, WardRules.Kind.AREA, faraway, 5);
            helper.assertFalse(helper.getLevel().hasChunkAt(faraway), "setup: the chunk is loaded");
            CommonHooks.onLivingDeath(owner, helper.getLevel().damageSources().generic());
            helper.assertTrue(ward.fallen(), "a ward whose chunk is not loaded did not fall with its owner");
            helper.assertFalse(helper.getLevel().hasChunkAt(faraway), "the fall loaded the chunk");
        } finally {
            clean(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void becomingAVampireFallsTheWardWithNoDeathEvent(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        TalkingDuelist other = person(helper, 1, 1);
        try {
            WardTable.Ward mine = ward(helper, owner, 3);
            WardTable.Ward theirs = ward(helper, other, 3);
            // a turning's lethal blow is cancelled: no death is ever posted, only the faction changes
            VampirismAPI.factionPlayerHandler(owner).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            helper.assertTrue(mine.fallen(), "the ward did not fall when its owner joined the vampires");
            helper.assertFalse(theirs.fallen(), "somebody else's ward fell");
        } finally {
            clean(helper, owner, other);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aSpeciesChangeAndALoginCheckFallTheWardToo(GameTestHelper helper) {
        if (!speciesOn()) {
            helper.succeed();
            return;
        }
        TalkingDuelist owner = person(helper, -1, -1);
        TalkingDuelist late = person(helper, 1, 1);
        try {
            WardTable.Ward mine = ward(helper, owner, 3);
            species(owner, Species.HYBRID);
            helper.assertTrue(mine.fallen(), "a species change to a vampiric type did not fall the ward");
            WardTable.Ward kept = ward(helper, late, 3);
            species(late, Species.WITCH);
            WardLifecycle.onBecoming(late);
            helper.assertFalse(kept.fallen(), "a witch's ward fell at login");
            // the owner who became vampiric while no listener saw it: the login check finds them
            net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(late, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            WardLifecycle.onBecoming(late);
            helper.assertTrue(kept.fallen(), "the login check did not fall a vampiric owner's ward");
        } finally {
            clean(helper, owner, late);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aWerewolfFactionOrAHunterOwnerKeepsTheWardAndOneSwitchCoversBothFalls(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        try {
            WardTable.Ward ward = ward(helper, owner, 3);
            VampirismAPI.factionPlayerHandler(owner).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            helper.assertFalse(ward.fallen(), "a hunter owner's ward fell");
            ArtifactsConfig.override("ward_falls_on_owner_death", false);
            CommonHooks.onLivingDeath(owner, helper.getLevel().damageSources().generic());
            VampirismAPI.factionPlayerHandler(owner).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            helper.assertFalse(ward.fallen(), "ward_falls_on_owner_death = false still fell the ward");
        } finally {
            clean(helper, owner);
        }
        helper.succeed();
    }

    // ---- what the hub is told ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aViewHoldsOnlyTheViewersOwnWardsAndWhoInvitedThem(GameTestHelper helper) {
        TalkingDuelist alice = person(helper, -1, -1);
        TalkingDuelist bob = person(helper, 1, 1);
        TalkingDuelist cara = person(helper, 2, 2);
        try {
            WardTable.Ward aliceWard = ward(helper, alice, 3);
            ward(helper, bob, 3);
            invite(helper, alice, cara);
            WardHub.View carasView = WardView.of(cara);
            helper.assertTrue(carasView.own().isEmpty(), "somebody's own list showed another owner's ward");
            helper.assertTrue(carasView.welcomedBy().size() == 1 && carasView.welcomedBy().get(0).equals(alice.getGameProfile().getName()), "the welcome is not Alice: " + carasView.welcomedBy());
            WardHub.View alicesView = WardView.of(alice);
            helper.assertTrue(alicesView.own().size() == 1 && alicesView.own().get(0).id().equals(aliceWard.id()), "Alice does not see her ward");
            helper.assertTrue(alicesView.own().get(0).invited().size() == 1 && alicesView.own().get(0).invited().get(0).id().equals(cara.getUUID()), "the list is not Cara alone (the owner is not listed)");
            helper.assertTrue(WardView.of(bob).welcomedBy().isEmpty(), "Bob was welcomed by somebody who did not invite him");
            WardIndex.get(helper.getLevel()).fallAllOf(alice.getUUID());
            helper.assertTrue(WardView.of(cara).welcomedBy().isEmpty(), "a fallen ward still welcomes");
        } finally {
            clean(helper, alice, bob, cara);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void theIndexSurvivesASaveAndLoadAndAFreshOneHasNothing(GameTestHelper helper) {
        TalkingDuelist owner = person(helper, -1, -1);
        TalkingDuelist guest = person(helper, 1, 1);
        try {
            WardTable.Ward w = ward(helper, owner, 3);
            invite(helper, owner, guest);
            WardTable.Ward fallen = WardTestSupport.wardAt(helper, guest, WardRules.Kind.DOOR, centre(helper).above(2), 1);
            fallen.setFallen(true);
            WardIndex copy = WardIndex.load(WardIndex.get(helper.getLevel()).save(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
            WardTable.Ward back = copy.table().get(w.id()).orElseThrow();
            helper.assertTrue(back.owner().equals(owner.getUUID()) && back.invited().contains(guest.getUUID()) && back.invited().contains(owner.getUUID()), "the owner or the list was lost");
            helper.assertTrue(back.kind() == WardRules.Kind.AREA && back.half() == 3 && back.cx() == w.cx() && back.dimension().equals(w.dimension()), "the shape was lost");
            helper.assertTrue(copy.table().get(fallen.id()).orElseThrow().fallen() && copy.table().get(fallen.id()).orElseThrow().kind() == WardRules.Kind.DOOR, "the fallen flag or the kind was lost");
            helper.assertTrue(WardIndex.load(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess()).table().isEmpty(), "a world with no wards has some");
        } finally {
            clean(helper, owner, guest);
        }
        helper.succeed();
    }
}
