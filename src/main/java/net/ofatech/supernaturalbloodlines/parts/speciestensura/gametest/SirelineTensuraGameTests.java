package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.entity.monster.DirewolfEntity;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.network.c2s.RequestNamingKeyPacket;
import io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket;
import io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.NamingType;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.entity.MonsterEntityTypes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.SubordinateHelper;
import io.github.manasmods.tensura.world.TensuraGameRules;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SireNaming;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SubordinateLink;

/** Sireline as Tensura subordinates and the sire's naming (R101). Registered by the part itself. */
@PrefixGameTestTemplate(false)
public final class SirelineTensuraGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SirelineTensuraGameTests() {}

    private static IExistence existence(LivingEntity e) {
        return TensuraStorages.getExistenceFrom(e);
    }

    private static void reset(ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            BloodlineAccess.set(p, BloodlineState.NONE);
            existence(p).setPermanentOwner(null);
            existence(p).setName(null);
            existence(p).setNameable(true);
        }
    }

    private static String race(LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    private static void setRace(LivingEntity e, String id) {
        RaceAPI.getRaceFrom(e).setRace(ResourceLocation.parse(id), false);
    }

    /** What the game rule was; the test turns it to {@code value} and gives it back with {@link #restore}. */
    private static boolean playerNaming(GameTestHelper helper, boolean value) {
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(TensuraGameRules.PLAYER_NAME);
        boolean before = rule.get();
        rule.set(value, helper.getLevel().getServer());
        return before;
    }

    private static void restore(GameTestHelper helper, boolean before) {
        helper.getLevel().getGameRules().getRule(TensuraGameRules.PLAYER_NAME).set(before, helper.getLevel().getServer());
    }

    /** A FakePlayer drops every message it is sent; this one keeps the translation keys, to show why Tensura refused. */
    private static final class Namer extends FakePlayer {
        private final List<String> keys = new ArrayList<>();

        private Namer(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "namer"));
        }

        @Override
        public void displayClientMessage(Component message, boolean actionBar) {
            if (message.getContents() instanceof TranslatableContents t) {
                keys.add(t.getKey());
            }
        }

        @Override
        public void sendSystemMessage(Component message) {
            displayClientMessage(message, false);
        }
    }

    private static String said(ServerPlayer p) {
        return p instanceof Namer n ? n.keys.toString() : "[]";
    }

    /** A namer who can pay for and pass Tensura's own naming checks, so a refusal in a test is the one under test. */
    private static ServerPlayer namer(GameTestHelper helper) {
        ServerPlayer p = new Namer(helper);
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(10_000_000);
        p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(10_000_000);
        existence(p).setMagicule(10_000_000);
        existence(p).setAura(10_000_000);
        return p;
    }

    /** A player who can be sired for the link (a species keeps them supernatural) and has switched Tensura's naming on for themselves, which is off by default. */
    private static ServerPlayer hybrid(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
        existence(p).setNameable(true);
        return p;
    }

    // ---- the link ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siredPlayerBecomesSubordinate(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = hybrid(helper);
        try {
            helper.assertTrue(SireRecorder.record(sire, child), "the sire was not recorded");
            helper.assertTrue(sire.getUUID().equals(existence(child).getPermanentOwner()), "owner is " + existence(child).getPermanentOwner());
            helper.assertTrue(SubordinateHelper.isSubordinate(sire, child), "Tensura does not see a subordinate");
            helper.assertTrue(!SubordinateHelper.isSubordinate(child, sire), "the sire became the child's subordinate");
            helper.assertTrue(sire.getUUID().equals(SubordinateHelper.getSubordinateOwnerUUID(child)), "owner uuid " + SubordinateHelper.getSubordinateOwnerUUID(child));

            // a login heals a link that was missing (the sire was set while the part was off)
            existence(child).setPermanentOwner(null);
            SubordinateLink.sync(child);
            helper.assertTrue(sire.getUUID().equals(existence(child).getPermanentOwner()), "sync did not restore the link");
        } finally {
            reset(sire, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siredMobKillCreditsSire(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        DirewolfEntity wolf = helper.spawn(MonsterEntityTypes.DIREWOLF.get(), new Vec3(8.5, 2, 4.5));
        SireCall.overridePlayerLookup(id -> sire.getUUID().equals(id) ? sire : null);
        try {
            helper.assertTrue(SireRecorder.record(sire, cow), "the cow was not sired");
            // Tensura credits a subordinate's kill to its owner through this owner lookup (soul points of a demon lord sire, harvest festival gifts)
            helper.assertTrue(sire.getUUID().equals(SubordinateHelper.getSubordinateOwnerUUID(cow)), "cow owner " + SubordinateHelper.getSubordinateOwnerUUID(cow));
            helper.assertTrue(SubordinateHelper.isSubordinate(sire, cow), "the cow is not a subordinate");

            helper.assertTrue(wolf instanceof ISubordinate, "a direwolf is not an ISubordinate");
            helper.assertTrue(SireRecorder.record(sire, wolf), "the direwolf was not sired");
            helper.assertTrue(sire.getUUID().equals(existence(wolf).getPermanentOwner()), "wolf permanent owner " + existence(wolf).getPermanentOwner());
            helper.assertTrue(wolf.isTame() && sire.getUUID().equals(wolf.getOwnerUUID()), "the direwolf was not tamed by its online sire: tame " + wolf.isTame() + ", owner " + wolf.getOwnerUUID());
            helper.assertTrue(SubordinateHelper.isSubordinate(sire, wolf), "the direwolf is not a subordinate");
        } finally {
            SireCall.overridePlayerLookup(null);
            reset(sire);
            cow.discard();
            wolf.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cureRemovesSubordinateLink(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = hybrid(helper);
        try {
            SireRecorder.record(sire, child);
            helper.assertTrue(existence(child).getPermanentOwner() != null, "no link to cure");
            // what a cure does: the rules clear the sire
            BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().sire(null).build());
            helper.assertTrue(existence(child).getPermanentOwner() == null, "the owner stayed: " + existence(child).getPermanentOwner());
            helper.assertTrue(!SubordinateHelper.isSubordinate(sire, child), "still a subordinate after the cure");
        } finally {
            reset(sire, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void speciesLossEndsLinkAndRegainRestoresIt(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = hybrid(helper);
        try {
            SireRecorder.record(sire, child);
            helper.assertTrue(sire.getUUID().equals(existence(child).getPermanentOwner()), "no link to lose");
            // the species goes but the recorded sire stays (a stale sire): the player is no longer bound
            BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.NONE).build());
            helper.assertTrue(existence(child).getPermanentOwner() == null, "a player with no species is still a subordinate");
            helper.assertTrue(sire.getUUID().equals(BloodlineAccess.get(child).sire()), "the test changed the sire");
            BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(sire.getUUID().equals(existence(child).getPermanentOwner()), "the link did not come back with the species");
        } finally {
            reset(sire, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void existingTensuraOwnerKept(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer other = TestPlayers.create(helper);
        ServerPlayer child = hybrid(helper);
        ServerPlayer freed = hybrid(helper);
        try {
            existence(child).setPermanentOwner(other.getUUID());
            helper.assertTrue(SireRecorder.record(sire, child), "the sire was not recorded");
            helper.assertTrue(sire.getUUID().equals(BloodlineAccess.get(child).sire()), "the sire is not in the state");
            helper.assertTrue(other.getUUID().equals(existence(child).getPermanentOwner()), "Tensura's owner was overwritten: " + existence(child).getPermanentOwner());
            // ending the bond leaves an owner that was never ours alone
            BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().sire(null).build());
            helper.assertTrue(other.getUUID().equals(existence(child).getPermanentOwner()), "the cure removed Tensura's own owner");

            // and the other way round: a sire who changes takes the old link with them only while it is theirs
            SireRecorder.record(sire, freed);
            BloodlineAccess.set(freed, BloodlineAccess.get(freed).toBuilder().sire(other.getUUID()).build());
            helper.assertTrue(other.getUUID().equals(existence(freed).getPermanentOwner()), "a changed sire did not move the link: " + existence(freed).getPermanentOwner());
        } finally {
            reset(sire, other, child, freed);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sireLinkSwitchOffMakesNoLink(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = hybrid(helper);
        SpeciesTensuraConfig.overrideSireLink(false);
        try {
            SireRecorder.record(sire, child);
            helper.assertTrue(existence(child).getPermanentOwner() == null, "linked with the switch off");
        } finally {
            SpeciesTensuraConfig.overrideSireLink(null);
            reset(sire, child);
        }
        helper.succeed();
    }

    // ---- naming ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sireCanNameOwnSiredOnly(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer stranger = namer(helper);
        ServerPlayer rival = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer rivalSired = hybrid(helper);
        boolean before = playerNaming(helper, false);
        existence(stranger).setNameable(true);
        try {
            helper.assertTrue(SireNaming.hookApplied(), "the naming hook is not in place");
            SireRecorder.record(sire, sired);
            SireRecorder.record(rival, rivalSired);

            helper.assertTrue(RequestNamingKeyPacket.canName(sire, sired), "the sire cannot name their own sired with the game rule off: " + said(sire));
            helper.assertTrue(!RequestNamingKeyPacket.canName(stranger, sired), "a stranger named someone's sired with the game rule off");
            helper.assertTrue(!RequestNamingKeyPacket.canName(rival, sired), "another sire named a sired that is not theirs");
            helper.assertTrue(!RequestNamingKeyPacket.canName(sire, rivalSired), "a sire named another sire's sired");
            helper.assertTrue(!RequestNamingKeyPacket.canName(sire, stranger), "a sire named a player who is not their sired");

            // the sired keeps the say over their own nameable toggle
            existence(sired).setNameable(false);
            helper.assertTrue(!RequestNamingKeyPacket.canName(sire, sired), "a sire named a sired who switched naming off");
            existence(sired).setNameable(true);

            // with the switch off Tensura's own rule applies
            SpeciesTensuraConfig.overrideSireNaming(false);
            helper.assertTrue(!RequestNamingKeyPacket.canName(sire, sired), "the sire named their sired with sire_naming_enabled off");
            SpeciesTensuraConfig.overrideSireNaming(null);

            // once the sired has a name the lift is over
            existence(sired).setName("Taken");
            helper.assertTrue(!RequestNamingKeyPacket.canName(sire, sired), "a named sired can be named again");
        } finally {
            SpeciesTensuraConfig.overrideSireNaming(null);
            restore(helper, before);
            reset(sire, stranger, rival, sired, rivalSired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nonSireCannotNameWithGameRuleOff(GameTestHelper helper) {
        ServerPlayer namer = namer(helper);
        ServerPlayer target = TestPlayers.create(helper);
        existence(target).setNameable(true);
        boolean before = playerNaming(helper, false);
        try {
            helper.assertTrue(!RequestNamingKeyPacket.canName(namer, target), "a non-sire named a player with the game rule off");
            playerNaming(helper, true);
            target.setHealth(1);
            helper.assertTrue(RequestNamingKeyPacket.canName(namer, target), "the control does not pass with the game rule on: " + said(namer));
        } finally {
            restore(helper, before);
            reset(namer, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void namingSiredPlayerDoesNotChangeRace(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer stranger = namer(helper);
        ServerPlayer control = TestPlayers.create(helper);
        boolean before = playerNaming(helper, true);
        try {
            existence(control).setNameable(true);
            setRace(sired, "tensura:human");
            setRace(control, "tensura:human");
            SireRecorder.record(sire, sired);
            double base = sired.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();

            helper.assertTrue(RequestNamingKeyPacket.canName(sire, sired), "the sire cannot name: " + said(sire));
            RequestNamingMenuPacket.name(sired, sire, NamingType.HIGH, "Ward");
            helper.assertTrue("Ward".equals(existence(sired).getName()), "the sired was not named: " + existence(sired).getName() + " " + said(sire));
            helper.assertTrue(sire.getUUID().equals(existence(sired).getPermanentOwner()), "the owner changed");
            helper.assertTrue("tensura:human".equals(race(sired)), "naming changed the sired's race to " + race(sired));
            helper.assertTrue(sired.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue() > base, "no stat reward for the sired");

            // control: the same naming of a player nobody sired is Tensura's own, harvest festival evolution included
            control.setHealth(1);
            RequestNamingMenuPacket.name(control, stranger, NamingType.HIGH, "Free");
            helper.assertTrue("Free".equals(existence(control).getName()), "the control was not named " + said(stranger));
            SupernaturalBloodlines.LOGGER.info("species_tensura test: control race after a HIGH naming: {}", race(control));
            helper.assertTrue(!"tensura:human".equals(race(control)),
                    "the control kept its race too, so this test cannot tell the downgrade from a race that does not evolve");
        } finally {
            restore(helper, before);
            reset(sire, sired, stranger, control);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siredMobCanBeNamedByItsSire(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        DirewolfEntity wolf = helper.spawn(MonsterEntityTypes.DIREWOLF.get(), new Vec3(8.5, 2, 4.5));
        SireCall.overridePlayerLookup(id -> sire.getUUID().equals(id) ? sire : null);
        try {
            SireRecorder.record(sire, wolf);
            UUID owner = existence(wolf).getPermanentOwner();
            helper.assertTrue(sire.getUUID().equals(owner), "wolf owner " + owner);
            helper.assertTrue(RequestNamingKeyPacket.canName(sire, wolf), "a sire cannot name their own sired creature");
        } finally {
            SireCall.overridePlayerLookup(null);
            reset(sire);
            wolf.discard();
        }
        helper.succeed();
    }
}
