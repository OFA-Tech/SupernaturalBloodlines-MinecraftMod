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
import dev.architectury.event.EventResult;
import io.github.manasmods.tensura.config.entity.PlayerConfig;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import io.github.manasmods.tensura.menu.NamingMenu;
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

    /** These fake players are not in the player list, so the sire is found through the lookup; reset gives it back. */
    private static void online(ServerPlayer... ps) {
        SireCall.overridePlayerLookup(id -> {
            for (ServerPlayer p : ps) {
                if (p.getUUID().equals(id)) {
                    return p;
                }
            }
            return null;
        });
    }

    private static void reset(ServerPlayer... ps) {
        SireCall.overridePlayerLookup(null);
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

    private static final String CANNOT_NAME = "tensura.naming.cannot_name";
    private static double lastCost;

    static {
        // sees the magicule cost Tensura worked out for the last naming (SireNaming does not change it)
        TensuraEntityEvents.NAMING_EVENT.register((target, namer, epGain, cost, type, name) -> {
            lastCost = cost.get();
            return EventResult.pass();
        });
    }

    /** canName refuses, and the reason Tensura gives is its own player-naming refusal (isNotNameable), not another check. */
    private static void assertRuleRefusal(GameTestHelper helper, ServerPlayer namer, LivingEntity target, String what) {
        helper.assertTrue(namer instanceof Namer, "the namer cannot record Tensura's refusal");
        ((Namer) namer).keys.clear();
        helper.assertTrue(!RequestNamingKeyPacket.canName(namer, target), what + " was allowed");
        helper.assertTrue(said(namer).contains(CANNOT_NAME), what + " was refused for another reason: " + said(namer));
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

    @GameTest(template = "empty", templateNamespace = NS)
    public static void switchOffUnlinksOnlyWhatWeLinked(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer other = TestPlayers.create(helper);
        ServerPlayer child = hybrid(helper);
        ServerPlayer named = hybrid(helper);
        try {
            SireRecorder.record(sire, child);
            helper.assertTrue(child.getPersistentData().hasUUID(SubordinateLink.KEY), "the linked sire was not stored");
            // an owner Tensura set itself (a naming) is the sire too, but was never ours
            existence(named).setPermanentOwner(sire.getUUID());
            SireRecorder.record(sire, named);
            helper.assertTrue(!named.getPersistentData().hasUUID(SubordinateLink.KEY), "stored a link that Tensura made");

            SpeciesTensuraConfig.overrideSireLink(false);
            SubordinateLink.sync(child);
            SubordinateLink.sync(named);
            helper.assertTrue(existence(child).getPermanentOwner() == null, "the switch is off and the link stayed");
            helper.assertTrue(!child.getPersistentData().hasUUID(SubordinateLink.KEY), "the stored sire stayed");
            helper.assertTrue(sire.getUUID().equals(existence(named).getPermanentOwner()), "the switch took an owner Tensura had set");

            SpeciesTensuraConfig.overrideSireLink(null);
            SubordinateLink.sync(child);
            helper.assertTrue(sire.getUUID().equals(existence(child).getPermanentOwner()), "the link did not come back with the switch");

            // Tensura changed the owner meanwhile: a cure must not take it
            existence(child).setPermanentOwner(other.getUUID());
            BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().sire(null).build());
            helper.assertTrue(other.getUUID().equals(existence(child).getPermanentOwner()), "the cure took an owner that is no longer our sire");
            helper.assertTrue(!child.getPersistentData().hasUUID(SubordinateLink.KEY), "the stored sire stayed after the cure");
        } finally {
            SpeciesTensuraConfig.overrideSireLink(null);
            reset(sire, other, child, named);
            child.getPersistentData().remove(SubordinateLink.KEY);
            named.getPersistentData().remove(SubordinateLink.KEY);
        }
        helper.succeed();
    }

    // ---- naming ----

    /** The key test: siring names the sired in Tensura with their own current name. Fails when the hook in SubordinateLink.sync is removed. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void siringNamesTheSiredWithTheirOwnName(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        online(sire);
        try {
            helper.assertTrue(existence(sired).getName() == null, "control: the sired is named before the siring");
            helper.assertTrue(SireRecorder.record(sire, sired), "the sire was not recorded");
            String own = sired.getGameProfile().getName();
            helper.assertTrue(own.equals(existence(sired).getName()), "the sired's Tensura name is " + existence(sired).getName() + ", not their own " + own + " " + said(sire));
            helper.assertTrue(sire.getUUID().equals(existence(sired).getPermanentOwner()), "the owner is " + existence(sired).getPermanentOwner());
        } finally {
            reset(sire, sired);
        }
        helper.succeed();
    }

    /** No consent and no server rule: playerNaming=false and a sired who never turned naming on do not block it, and both are given back. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void autoNamingIgnoresPlayerNamingAndConsent(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer stranger = namer(helper);
        ServerPlayer hurt = TestPlayers.create(helper);
        existence(hurt).setNameable(true);
        hurt.setHealth(1);
        boolean before = playerNaming(helper, false);
        online(sire);
        try {
            existence(sired).setNameable(false);
            helper.assertTrue(!RequestNamingKeyPacket.canName(sire, sired), "control: the sired is nameable by hand with the rule off and no consent");
            SireRecorder.record(sire, sired);
            helper.assertTrue(sired.getGameProfile().getName().equals(existence(sired).getName()), "not named: " + existence(sired).getName() + " " + said(sire));
            helper.assertTrue(!helper.getLevel().getGameRules().getBoolean(TensuraGameRules.PLAYER_NAME), "the game rule was left on");
            helper.assertTrue(!existence(sired).isNameable(), "the sired's nameable toggle was left on");
            // the rule still stops everyone else
            assertRuleRefusal(helper, stranger, hurt, "a stranger naming a player with the game rule off");
        } finally {
            restore(helper, before);
            reset(sire, sired, stranger, hurt);
        }
        helper.succeed();
    }

    /** The sire pays Tensura's cost and the sired gets Tensura's EP and stat reward, and no evolution (LOW); a real HIGH naming does evolve (control). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void autoNamingCostsTheSireAndRewardsTheSiredWithoutEvolving(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer stranger = namer(helper);
        ServerPlayer control = TestPlayers.create(helper);
        boolean before = playerNaming(helper, true);
        online(sire);
        try {
            existence(control).setNameable(true);
            setRace(sired, "tensura:human");
            setRace(control, "tensura:human");
            double base = sired.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
            double baseAura = sired.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue();
            double magicule = existence(sire).getMagicule();
            double aura = existence(sire).getAura();
            lastCost = 0;

            SireRecorder.record(sire, sired);
            helper.assertTrue(existence(sired).getName() != null, "the sired was not named " + said(sire));
            helper.assertTrue(lastCost > 0, "Tensura saw no cost");
            double paid = (magicule - existence(sire).getMagicule()) + (aura - existence(sire).getAura());
            helper.assertTrue(Math.abs(paid - lastCost) < 1e-6 * lastCost, "the sire paid " + paid + ", Tensura's cost was " + lastCost);
            helper.assertTrue(sired.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue() > base
                    || sired.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue() > baseAura, "no EP and stat reward for the sired");
            helper.assertTrue("tensura:human".equals(race(sired)), "the naming changed the sired's race to " + race(sired));

            // control: Tensura's own HIGH naming of a player nobody sired evolves them, so the test can tell a LOW naming from one that cannot evolve
            control.setHealth(1);
            RequestNamingMenuPacket.name(control, stranger, NamingType.HIGH, "Free");
            helper.assertTrue("Free".equals(existence(control).getName()), "the control was not named " + said(stranger));
            helper.assertTrue(!"tensura:human".equals(race(control)), "the control kept its race too, so this test cannot tell LOW from HIGH");
        } finally {
            restore(helper, before);
            reset(sire, sired, stranger, control);
        }
        helper.succeed();
    }

    /** Tensura's own LOW roll decides the sire's loss of max magicule: at 100 percent the base falls by the cost, at 0 it does not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void autoNamingFollowsTensurasLowMaxLossRoll(GameTestHelper helper) {
        PlayerConfig.Naming config = NamingMenu.CONFIG;
        double subdue = config.subdueLostChance;
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer lucky = namer(helper);
        ServerPlayer luckySired = hybrid(helper);
        online(sire, lucky);
        try {
            config.subdueLostChance = 100;
            double base = sire.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
            SireRecorder.record(sire, sired);
            helper.assertTrue(existence(sired).getName() != null, "the sired was not named: " + said(sire));
            double lost = base - sire.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
            helper.assertTrue(lastCost > 0 && Math.abs(lost - lastCost) < 1e-6 * lastCost, "the sire lost " + lost + " max magicule, the cost was " + lastCost);

            config.subdueLostChance = 0;
            double base2 = lucky.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
            SireRecorder.record(lucky, luckySired);
            helper.assertTrue(existence(luckySired).getName() != null, "the second sired was not named: " + said(lucky));
            helper.assertTrue(lucky.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue() == base2, "the sire lost max magicule on a roll that cannot hit");
        } finally {
            config.subdueLostChance = subdue;
            reset(sire, sired, lucky, luckySired);
        }
        helper.succeed();
    }

    /** An offline sire or one short of magicule leaves the sired unnamed; the next login of either names them. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void absentOrBrokeSireNamesAtTheNextLogin(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer broke = namer(helper);
        ServerPlayer brokeSired = hybrid(helper);
        try {
            // the sire is not online
            SireRecorder.record(sire, sired);
            helper.assertTrue(sire.getUUID().equals(existence(sired).getPermanentOwner()), "control: no link");
            helper.assertTrue(existence(sired).getName() == null, "named with the sire offline");
            online(sire);
            SireNaming.retryFor(sire, List.of(sire, sired));
            helper.assertTrue(existence(sired).getName() != null, "the sire's login did not name the sired " + said(sire));

            // the sire is online but has nothing to pay with
            existence(broke).setMagicule(0);
            existence(broke).setAura(0);
            online(sire, broke);
            SireRecorder.record(broke, brokeSired);
            helper.assertTrue(existence(brokeSired).getName() == null, "named by a sire with no magicule");
            existence(broke).setMagicule(10_000_000);
            existence(broke).setAura(10_000_000);
            SireNaming.retryFor(brokeSired, List.of(brokeSired));
            helper.assertTrue(existence(brokeSired).getName() != null, "the sired's login did not name them once the sire could pay " + said(broke));
        } finally {
            reset(sire, sired, broke, brokeSired);
        }
        helper.succeed();
    }

    /** The switch off: siring links and names nobody; an already named sired and one with a foreign Tensura owner are left alone and cost the sire nothing. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void autoNamingSwitchAndSkips(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer off = hybrid(helper);
        ServerPlayer named = hybrid(helper);
        ServerPlayer owned = hybrid(helper);
        ServerPlayer other = TestPlayers.create(helper);
        online(sire);
        try {
            SpeciesTensuraConfig.overrideSireNaming(false);
            SireRecorder.record(sire, off);
            helper.assertTrue(sire.getUUID().equals(existence(off).getPermanentOwner()), "the link depends on the naming switch");
            helper.assertTrue(existence(off).getName() == null, "named with sire_naming_enabled off");
            helper.assertTrue(!SireNaming.tryName(off), "tryName named with the switch off");
            SpeciesTensuraConfig.overrideSireNaming(null);

            double magicule = existence(sire).getMagicule();
            existence(named).setName("Taken");
            SireRecorder.record(sire, named);
            helper.assertTrue("Taken".equals(existence(named).getName()), "a named sired was renamed to " + existence(named).getName());
            existence(owned).setPermanentOwner(other.getUUID());
            SireRecorder.record(sire, owned);
            helper.assertTrue(existence(owned).getName() == null, "a sired with a foreign Tensura owner was named");
            helper.assertTrue(existence(sire).getMagicule() == magicule, "the sire paid for a naming that did not happen");
        } finally {
            SpeciesTensuraConfig.overrideSireNaming(null);
            reset(sire, off, named, owned, other);
        }
        helper.succeed();
    }

    /** R100: the sire naming their sired by hand at HIGH is lowered to LOW, so the sired keeps their race. Fails when the NAMING_EVENT handler is removed. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void sireHandNamingNeverEvolvesTheSired(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer stranger = namer(helper);
        ServerPlayer control = hybrid(helper);
        boolean before = playerNaming(helper, true);
        try {
            setRace(sired, "tensura:human");
            // the sire is not online for the lookup, so only the hand naming below can name
            SireRecorder.record(sire, sired);
            helper.assertTrue(existence(sired).getName() == null, "control: named already");
            helper.assertTrue(RequestNamingKeyPacket.canName(sire, sired), "control: the sire cannot name by hand: " + said(sire));
            RequestNamingMenuPacket.name(sired, sire, NamingType.HIGH, "Manual");
            helper.assertTrue("Manual".equals(existence(sired).getName()), "the sired was not named by hand: " + said(sire));
            helper.assertTrue("tensura:human".equals(race(sired)), "the sire's HIGH naming evolved the sired to " + race(sired));

            // control: the same naming by someone who is not the sire evolves a player of the same kind, so the test can tell
            setRace(control, "tensura:human");
            control.setHealth(1);
            RequestNamingMenuPacket.name(control, stranger, NamingType.HIGH, "Free");
            helper.assertTrue("Free".equals(existence(control).getName()), "the control was not named " + said(stranger));
            helper.assertTrue(!"tensura:human".equals(race(control)), "the control kept its race too, so this test cannot tell the handler's work");
        } finally {
            restore(helper, before);
            reset(sire, sired, stranger, control);
        }
        helper.succeed();
    }

    /** A link made at login is a single naming try (SubordinateLink.sync does not try on login, retryFor does): the broke sire is told once. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginMakesOneNamingTryPerPair(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        try {
            existence(sire).setMagicule(0);
            existence(sire).setAura(0);
            SireRecorder.record(sire, sired);
            online(sire);
            existence(sired).setPermanentOwner(null);
            ((Namer) sire).keys.clear();
            SubordinateLink.onLogin(sired, List.of(sired, sire));
            helper.assertTrue(sire.getUUID().equals(existence(sired).getPermanentOwner()), "control: the login did not heal the link");
            helper.assertTrue(existence(sired).getName() == null, "control: a sire with no magicule named");
            helper.assertTrue(((Namer) sire).keys.size() == 1, "the sire was told " + said(sire) + ", not once");
        } finally {
            reset(sire, sired);
        }
        helper.succeed();
    }

    /** A failing naming leaves neither the game rule nor the sired's toggle changed. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void ruleAndToggleAreRestoredAfterAnException(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        boolean before = playerNaming(helper, false);
        TensuraEntityEvents.NamingEvent boom = (target, namer, cost, epGain, type, name) -> {
            throw new IllegalStateException("boom");
        };
        try {
            existence(sired).setNameable(false);
            SireRecorder.record(sire, sired);
            online(sire);
            TensuraEntityEvents.NAMING_EVENT.register(boom);
            boolean thrown = false;
            try {
                SireNaming.tryName(sired);
            } catch (IllegalStateException expected) {
                thrown = true;
            }
            helper.assertTrue(thrown, "control: the naming did not reach the failing listener");
            helper.assertTrue(!helper.getLevel().getGameRules().getBoolean(TensuraGameRules.PLAYER_NAME), "the game rule was left on");
            helper.assertTrue(!existence(sired).isNameable(), "the sired's toggle was left on");
        } finally {
            TensuraEntityEvents.NAMING_EVENT.unregister(boom);
            restore(helper, before);
            reset(sire, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nonSireCannotNameWithGameRuleOff(GameTestHelper helper) {
        ServerPlayer namer = namer(helper);
        ServerPlayer target = TestPlayers.create(helper);
        existence(target).setNameable(true);
        // low on health, so the game rule is the only thing that can refuse
        target.setHealth(1);
        boolean before = playerNaming(helper, false);
        try {
            assertRuleRefusal(helper, namer, target, "a non-sire naming a hurt player with the game rule off");
            playerNaming(helper, true);
            ((Namer) namer).keys.clear();
            helper.assertTrue(RequestNamingKeyPacket.canName(namer, target), "the control does not pass with the game rule on: " + said(namer));
        } finally {
            restore(helper, before);
            reset(namer, target);
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

    /** R101b: a severed bond frees the sired from the Tensura owner link and the Tensura name goes with it; the sire stays recorded. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceFreesTheOwnerAndClearsTheName(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer newSire = TestPlayers.create(helper);
        ServerPlayer sired = hybrid(helper);
        online(sire);
        try {
            SireRecorder.record(sire, sired);
            helper.assertTrue(sire.getUUID().equals(existence(sired).getPermanentOwner()), "control: no link");
            String own = sired.getGameProfile().getName();
            helper.assertTrue(own.equals(existence(sired).getName()), "control: not named " + said(sire));

            BloodlineAccess.set(sired, BloodlineAccess.get(sired).toBuilder().severedSire(sire.getUUID()).build());
            helper.assertTrue(existence(sired).getPermanentOwner() == null, "the owner stayed: " + existence(sired).getPermanentOwner());
            helper.assertTrue(!SubordinateHelper.isSubordinate(sire, sired), "still a subordinate after the severance");
            helper.assertTrue(existence(sired).getName() == null, "the name survived the severance: " + existence(sired).getName());
            helper.assertTrue(sire.getUUID().equals(BloodlineAccess.get(sired).sire()), "the sire was forgotten");

            // a new sire is a new bond: linked again, to the new sire
            helper.assertTrue(SireRecorder.record(newSire, sired), "could not be sired again");
            helper.assertTrue(newSire.getUUID().equals(existence(sired).getPermanentOwner()), "the new sire is not the owner: " + existence(sired).getPermanentOwner());
        } finally {
            reset(sire, newSire, sired);
        }
        helper.succeed();
    }

    /** The sired's base max aura plus max magicule: what Tensura's naming reward raises. */
    private static double maxEp(LivingEntity e) {
        return e.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue() + e.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
    }

    private static void sever(ServerPlayer sired, ServerPlayer sire) {
        BloodlineAccess.set(sired, BloodlineAccess.get(sired).toBuilder().severedSire(sire.getUUID()).build());
    }

    /** An owner Tensura's own naming set (no link of ours stored) is freed by a severance too, and the name goes with it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceClearsTheOwnerANamingSetAndTheName(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        boolean before = playerNaming(helper, true);
        // no link of ours: the only owner is the one the naming sets
        SpeciesTensuraConfig.overrideSireLink(false);
        try {
            SireRecorder.record(sire, sired);
            RequestNamingMenuPacket.name(sired, sire, NamingType.LOW, "Ward");
            helper.assertTrue("Ward".equals(existence(sired).getName()), "the sired was not named: " + existence(sired).getName() + " " + said(sire));
            helper.assertTrue(sire.getUUID().equals(existence(sired).getPermanentOwner()), "the naming did not make the sire the owner");
            helper.assertTrue(!sired.getPersistentData().hasUUID("supernaturalbloodlines:linked_sire"), "control: a link of ours is stored");

            sever(sired, sire);
            helper.assertTrue(existence(sired).getPermanentOwner() == null, "the naming owner survived the severance: " + existence(sired).getPermanentOwner());
            helper.assertTrue(existence(sired).getName() == null, "the name survived the severance: " + existence(sired).getName());
            helper.assertTrue(sire.getUUID().equals(BloodlineAccess.get(sired).sire()), "the sire was forgotten");
        } finally {
            SpeciesTensuraConfig.overrideSireLink(null);
            restore(helper, before);
            reset(sire, sired);
        }
        helper.succeed();
    }

    /** An owner that is not the severed sire is nobody's to clear, and the name it gave stays. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceKeepsAnUnrelatedOwnerAndItsName(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer other = TestPlayers.create(helper);
        ServerPlayer ownedSired = hybrid(helper);
        try {
            existence(ownedSired).setPermanentOwner(other.getUUID());
            existence(ownedSired).setName("Taken");
            SireRecorder.record(sire, ownedSired);
            sever(ownedSired, sire);
            helper.assertTrue(other.getUUID().equals(existence(ownedSired).getPermanentOwner()), "an unrelated owner was cleared: " + existence(ownedSired).getPermanentOwner());
            helper.assertTrue("Taken".equals(existence(ownedSired).getName()), "an unrelated name was cleared: " + existence(ownedSired).getName());
        } finally {
            reset(sire, other, ownedSired);
        }
        helper.succeed();
    }

    /** severance_clears_name off: today's behaviour, the owner goes and the name stays. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceNameClearSwitchOffKeepsTheName(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        online(sire);
        SpeciesTensuraConfig.overrideSeveranceClearsName(false);
        try {
            SireRecorder.record(sire, sired);
            String own = sired.getGameProfile().getName();
            helper.assertTrue(own.equals(existence(sired).getName()), "control: not named " + said(sire));
            sever(sired, sire);
            helper.assertTrue(existence(sired).getPermanentOwner() == null, "the owner stayed: " + existence(sired).getPermanentOwner());
            helper.assertTrue(own.equals(existence(sired).getName()), "the name was cleared with the switch off: " + existence(sired).getName());
            helper.assertTrue(sired.getCustomName() != null, "the visible name was cleared with the switch off");
        } finally {
            SpeciesTensuraConfig.overrideSeveranceClearsName(null);
            reset(sire, sired);
        }
        helper.succeed();
    }

    /** The old sire does not re-name a severed player at login, a later namer's name survives an unrelated bloodline change (no second severance). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severedPlayerKeepsALaterNameFromAnotherNamer(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer other = namer(helper);
        ServerPlayer sired = hybrid(helper);
        boolean before = playerNaming(helper, true);
        online(sire);
        try {
            SireRecorder.record(sire, sired);
            sever(sired, sire);
            helper.assertTrue(existence(sired).getName() == null, "the name survived the severance: " + existence(sired).getName());

            SubordinateLink.onLogin(sired, List.of(sire, sired));
            helper.assertTrue(existence(sired).getName() == null, "the old sire named the severed player again: " + existence(sired).getName());

            RequestNamingMenuPacket.name(sired, other, NamingType.LOW, "Second");
            helper.assertTrue("Second".equals(existence(sired).getName()), "another namer could not name them: " + said(other));
            helper.assertTrue(other.getUUID().equals(existence(sired).getPermanentOwner()), "the new namer is not the owner");

            BloodlineAccess.set(sired, BloodlineAccess.get(sired).toBuilder().thirst(10).build());
            helper.assertTrue("Second".equals(existence(sired).getName()), "an unrelated change wiped the name: " + existence(sired).getName());
            helper.assertTrue(other.getUUID().equals(existence(sired).getPermanentOwner()), "an unrelated change cleared the owner: " + existence(sired).getPermanentOwner());
        } finally {
            restore(helper, before);
            reset(sire, other, sired);
        }
        helper.succeed();
    }

    /** The naming's EP and stats stay when the name goes. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceKeepsEpAndStats(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        online(sire);
        try {
            double base = maxEp(sired);
            SireRecorder.record(sire, sired);
            helper.assertTrue(existence(sired).getName() != null, "control: not named " + said(sire));
            double named = maxEp(sired);
            helper.assertTrue(named > base, "control: the naming gave no EP and stats");

            sever(sired, sire);
            helper.assertTrue(existence(sired).getName() == null, "the name survived the severance");
            helper.assertTrue(maxEp(sired) == named, "the severance changed the stats: " + named + " to " + maxEp(sired));
        } finally {
            reset(sire, sired);
        }
        helper.succeed();
    }

    /** Default: a sire naming the player again after a severance pays Tensura's reward again. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void resiringAfterSeverancePaysTheNamingRewardAgain(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer newSire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        online(sire, newSire);
        try {
            SireRecorder.record(sire, sired);
            sever(sired, sire);
            double afterFirst = maxEp(sired);
            helper.assertTrue(SireRecorder.record(newSire, sired), "could not be sired again");
            helper.assertTrue(existence(sired).getName() != null, "the re-siring did not name them " + said(newSire));
            helper.assertTrue(maxEp(sired) > afterFirst, "no reward for the re-naming");
        } finally {
            reset(sire, newSire, sired);
        }
        helper.succeed();
    }

    /** resire_naming_reward off: the re-naming happens and costs the new sire as usual, but gives the sired nothing; a first naming is unaffected. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void resireNamingRewardOffNamesWithoutReward(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer newSire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        online(sire, newSire);
        SpeciesTensuraConfig.overrideResireNamingReward(false);
        try {
            double base = maxEp(sired);
            SireRecorder.record(sire, sired);
            helper.assertTrue(maxEp(sired) > base, "the switch took away a first naming's reward");
            sever(sired, sire);
            double afterFirst = maxEp(sired);
            double magicule = existence(newSire).getMagicule() + existence(newSire).getAura();

            helper.assertTrue(SireRecorder.record(newSire, sired), "could not be sired again");
            helper.assertTrue(existence(sired).getName() != null, "the re-siring did not name them " + said(newSire));
            helper.assertTrue(maxEp(sired) == afterFirst, "the re-naming still paid a reward: " + afterFirst + " to " + maxEp(sired));
            helper.assertTrue(existence(newSire).getMagicule() + existence(newSire).getAura() < magicule, "the new sire paid nothing");
            helper.assertTrue(!sired.getPersistentData().getBoolean(SubordinateLink.SEVERED_KEY), "the re-naming left the severed marker behind");
        } finally {
            SpeciesTensuraConfig.overrideResireNamingReward(null);
            reset(sire, newSire, sired);
        }
        helper.succeed();
    }

    /** Tensura's naming also sets the visible custom name; the severance takes it away with the name. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceClearsTheVisibleName(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        online(sire);
        try {
            SireRecorder.record(sire, sired);
            helper.assertTrue(sired.getCustomName() != null, "control: the naming set no visible name " + said(sire));
            sever(sired, sire);
            helper.assertTrue(sired.getCustomName() == null, "the visible name survived the severance: " + sired.getCustomName());
        } finally {
            reset(sire, sired);
            sired.setCustomName(null);
        }
        helper.succeed();
    }

    /** The recorded ex-sire of a severed player is no sire, but their hand naming at HIGH is still lowered to LOW: the severed player keeps their race. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void exSireHandNamingNeverEvolvesTheSevered(GameTestHelper helper) {
        ServerPlayer sire = namer(helper);
        ServerPlayer sired = hybrid(helper);
        ServerPlayer stranger = namer(helper);
        ServerPlayer control = hybrid(helper);
        boolean before = playerNaming(helper, true);
        try {
            setRace(sired, "tensura:human");
            // the sire is not online, so nothing names the sired before the severance
            SireRecorder.record(sire, sired);
            sever(sired, sire);
            helper.assertTrue(existence(sired).getName() == null, "control: named already");
            helper.assertTrue(RequestNamingKeyPacket.canName(sire, sired), "control: the ex-sire cannot name by hand: " + said(sire));
            RequestNamingMenuPacket.name(sired, sire, NamingType.HIGH, "Manual");
            helper.assertTrue("Manual".equals(existence(sired).getName()), "the severed player was not named by hand: " + said(sire));
            helper.assertTrue("tensura:human".equals(race(sired)), "the ex-sire's HIGH naming evolved the severed player to " + race(sired));

            // control: the same naming by a stranger evolves a player of the same kind
            setRace(control, "tensura:human");
            control.setHealth(1);
            RequestNamingMenuPacket.name(control, stranger, NamingType.HIGH, "Free");
            helper.assertTrue("Free".equals(existence(control).getName()), "the control was not named " + said(stranger));
            helper.assertTrue(!"tensura:human".equals(race(control)), "the control kept its race too, so this test cannot tell the handler's work");
        } finally {
            restore(helper, before);
            reset(sire, sired, stranger, control);
            sired.setCustomName(null);
            control.setCustomName(null);
        }
        helper.succeed();
    }
}