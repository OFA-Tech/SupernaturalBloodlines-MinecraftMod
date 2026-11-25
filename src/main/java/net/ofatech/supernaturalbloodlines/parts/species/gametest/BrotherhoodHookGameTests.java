package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Witch;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.StalePartModifiers;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.network.GlowPayload;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.FiveRoster;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterCurse;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.SparkHandler;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * Plan 20d Task 0: the hooks the brotherhood rests on, proved before anything is built on them. Each proof says what it saw in the log
 * ({@code [brotherhood task 0]}). Nothing here loops on {@code tickCount}: a death is posted by hand, time is not stepped.
 */
@PrefixGameTestTemplate(false)
public final class BrotherhoodHookGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final double EPS = 1e-9;

    private BrotherhoodHookGameTests() {
    }

    private static void say(String line) {
        SupernaturalBloodlines.LOGGER.info("[brotherhood task 0] {}", line);
    }

    private static net.minecraft.resources.ResourceLocation factionId(ServerPlayer p) {
        var f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return f == null ? null : f.getID();
    }

    /** Every fact the brotherhood's "fits" question will read. */
    private record Facts(Species species, Object faction, boolean vampire, boolean werewolf, Lineage lineage, boolean humanLike) {
    }

    private static Facts facts(ServerPlayer p) {
        var s = BloodlineAccess.get(p);
        return new Facts(s.species(), factionId(p), VampirismSupernatural.isVampire(p), VampirismSupernatural.isWerewolf(p), s.wolf().lineage(), SpeciesEligibility.isHumanLike(p));
    }

    private static boolean namesAMarkOrAKill(String method) {
        for (String word : method.split("(?<=[a-z0-9])(?=[A-Z])")) {
            String w = word.toLowerCase(java.util.Locale.ROOT);
            if (w.equals("mark") || w.equals("marks") || w.equals("kill") || w.equals("kills")) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampirismHasNoMarkApi(GameTestHelper helper) {
        List<String> found = new ArrayList<>();
        for (Class<?> type : List.of(de.teamlapen.vampirism.api.entity.player.hunter.IHunterPlayer.class, de.teamlapen.vampirism.api.entity.player.IFactionPlayer.class)) {
            for (java.lang.reflect.Method m : type.getMethods()) {
                if (namesAMarkOrAKill(m.getName())) {
                    found.add(type.getSimpleName() + "." + m.getName());
                }
            }
        }
        helper.assertTrue(found.isEmpty(), "Vampirism now has a mark or kill API, the brotherhood's own mark may need to read it: " + found);
        helper.assertTrue(namesAMarkOrAKill("getKills") && namesAMarkOrAKill("hunterMark") && !namesAMarkOrAKill("getSkillHandler"), "the audit's word split is wrong");
        say("IHunterPlayer and IFactionPlayer have no method with a mark or kill word in its name: the mark is ours");
        helper.succeed();
    }

    // ---- the faction ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHunterIsReadFromTheFactionHandler(GameTestHelper helper) {
        TalkingDuelist p = GroupTestKit.hunter(helper, 1.5);
        try {
            var handler = VampirismAPI.factionPlayerHandler(p);
            helper.assertTrue(handler.getCurrentFaction() == VReference.HUNTER_FACTION, "faction " + handler.getCurrentFaction());
            helper.assertTrue(handler.getCurrentLevel() == 1, "level " + handler.getCurrentLevel());
            helper.assertTrue(Groups.isHunter(p), "Groups.isHunter is false for a Vampirism hunter");
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.markKills() == 0 && s.species() == Species.NONE, "a Vampirism hunter has mark " + s.markKills() + " and species " + s.species());
            say("a Vampirism hunter (level 1) reads as a hunter through Groups.isHunter, with species NONE and markKills 0: no mark comes with the faction");
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlainHumanIsNoneHumanLikeAndInNoFaction(GameTestHelper helper) {
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer vampire = HunterMarkGameTests.vampire(helper);
        ServerPlayer hunter = HunterMarkGameTests.huntNone(helper);
        ServerPlayer witch = TestPlayers.createMortal(helper);
        ServerPlayer potential = TestPlayers.createMortal(helper);
        ServerPlayer tribrid = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            BloodlineAccess.set(potential, BloodlineState.NONE.toBuilder().species(Species.POTENTIAL).build());
            BloodlineAccess.set(tribrid, BloodlineState.NONE.toBuilder().species(Species.TRIBRID).build());
            Facts plain = facts(human);
            helper.assertTrue(plain.species() == Species.NONE && plain.faction() == null && plain.humanLike() && !plain.vampire() && !plain.werewolf() && plain.lineage() == Lineage.NONE, "a plain human reads as " + plain);
            for (ServerPlayer other : List.of(vampire, hunter, witch, potential, tribrid)) {
                helper.assertTrue(!facts(other).equals(plain), "a kind reads exactly like a plain human: " + facts(other));
            }
            say("a plain human reads " + plain + "; a vampire, a hunter, a witch, a Potential and a tribrid each differ in at least one fact");
        } finally {
            HunterMarkGameTests.reset(vampire, hunter, witch, potential, tribrid, human);
        }
        helper.succeed();
    }

    // ---- the attack ----

    private static final net.minecraft.resources.ResourceLocation PACK = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "pack_bonus");
    private static final net.minecraft.resources.ResourceLocation ARMS = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "brothers_in_arms_probe");

    private static void dropProbes(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        a.removeModifier(PACK);
        a.removeModifier(ARMS);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTransientModifierMultipliesWithTheMarkAndThePackBonus(GameTestHelper helper) {
        ServerPlayer p = HunterMarkGameTests.marked(helper, 0);
        try {
            AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
            double none = a.getValue();
            BloodlineAccess.set(p, HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 4));
            double marked = a.getValue();
            helper.assertTrue(Math.abs(marked - none * 1.04) < EPS, "the mark gave " + marked + " from " + none);
            a.addPermanentModifier(new AttributeModifier(PACK, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            a.addOrUpdateTransientModifier(new AttributeModifier(ARMS, 0.06, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            double all = a.getValue();
            helper.assertTrue(Math.abs(all - none * 1.04 * 1.10 * 1.06) < EPS, "value " + all + " is not " + none * 1.04 * 1.10 * 1.06);
            say("ATTACK_DAMAGE " + none + " -> mark(4) " + marked + " -> with a pack_bonus 0.10 and a transient 0.06: " + all + " (the product, within 1e-9)");
        } finally {
            dropProbes(p);
            HunterMarkGameTests.reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTransientModifierIsNotInTheSavedAttribute(GameTestHelper helper) {
        ServerPlayer p = HunterMarkGameTests.marked(helper, 4);
        try {
            AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
            a.addOrUpdateTransientModifier(new AttributeModifier(ARMS, 0.06, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            String saved = String.valueOf(a.save());
            helper.assertTrue(saved.contains("hunter_mark"), "the permanent mark is not saved: " + saved);
            helper.assertTrue(!saved.contains(ARMS.getPath()), "the transient modifier is saved: " + saved);
            StalePartModifiers.strip(p, Set.<Part>of());
            helper.assertTrue(a.getModifier(MarkTracker.MODIFIER_ID) == null, "strip left hunter_mark although species is off");
            helper.assertTrue(a.getModifier(ARMS) != null, "strip took a modifier that no part owns");
            say("attribute.save() lists hunter_mark and not the transient id; StalePartModifiers.strip with no parts removes hunter_mark and leaves the transient one");
        } finally {
            dropProbes(p);
            HunterMarkGameTests.reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markSyncLeavesAForeignModifierAlone(GameTestHelper helper) {
        ServerPlayer p = HunterMarkGameTests.marked(helper, 4);
        try {
            AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
            a.addOrUpdateTransientModifier(new AttributeModifier(ARMS, 0.06, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            MarkTracker.sync(p);
            MarkTracker.sync(p);
            BloodlineAccess.set(p, HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 9));
            helper.assertTrue(a.getModifier(ARMS) != null && a.getModifier(ARMS).amount() == 0.06, "sync touched the foreign modifier");
            helper.assertTrue(a.getModifier(MarkTracker.MODIFIER_ID).amount() > 0.08, "the mark did not follow the kills");
            say("MarkTracker.sync (twice, and through a markKills change) leaves a foreign transient modifier alone");
        } finally {
            dropProbes(p);
            HunterMarkGameTests.reset(p);
        }
        helper.succeed();
    }

    private static float hit(GameTestHelper helper, ServerPlayer p, LivingEntity target) {
        target.setHealth(target.getMaxHealth());
        target.invulnerableTime = 0;
        p.resetAttackStrengthTicker();
        float before = target.getHealth();
        p.attack(target);
        return before - target.getHealth();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aScaledHitDoesMoreDamage(GameTestHelper helper) {
        ServerPlayer p = HunterMarkGameTests.huntNone(helper);
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 1, 2));
        try {
            p.setPos(cow.position().add(0.5, 0, 0));
            float plain = hit(helper, p, cow);
            AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
            a.addOrUpdateTransientModifier(new AttributeModifier(ARMS, 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            float scaled = hit(helper, p, cow);
            if (plain <= 0) {
                helper.assertTrue(Math.abs(a.getValue() - 1.12) < EPS || a.getValue() > a.getBaseValue(), "the attribute did not rise");
                say("a fake player cannot hurt a cow here (damage " + plain + "): the attribute value and the javap reading of Player.attack are the proof");
            } else {
                helper.assertTrue(scaled > plain && Math.abs(scaled / plain - 1.12) < 0.01, "plain " + plain + " scaled " + scaled);
                say("a real Player.attack did " + plain + " plain and " + scaled + " with a transient +12% (ratio " + scaled / plain + ")");
            }
        } finally {
            dropProbes(p);
            p.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ARMS);
            cow.discard();
            HunterMarkGameTests.reset(p);
        }
        helper.succeed();
    }

    // ---- the glow ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGlowSendToAFakePlayerIsSeenBySpyAndNotDelivered(GameTestHelper helper) {
        ServerPlayer fake = TestPlayers.create(helper);
        List<String> seen = new ArrayList<>();
        GlowPayload.overrideSpy((who, ids) -> seen.add(who.getUUID() + ":" + ids));
        try {
            boolean sent = GlowPayload.send(fake, List.of(4, 5));
            helper.assertTrue(!sent, "a fake player was sent a packet");
            helper.assertTrue(seen.equals(List.of(fake.getUUID() + ":[4, 5]")), "the spy saw " + seen);
            say("GlowPayload.send to a fake player answered false and the spy saw " + seen);
        } finally {
            GlowPayload.overrideSpy(null);
        }
        helper.succeed();
    }

    // ---- the kill ----

    private static boolean isCancelledDeathSeen(GameTestHelper helper, LivingEntity victim, DamageSource source) {
        boolean[] seen = {false};
        Consumer<LivingDeathEvent> cancel = e -> e.setCanceled(true);
        Consumer<LivingDeathEvent> probe = e -> seen[0] = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, cancel);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingDeathEvent.class, probe);
        try {
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, source));
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
            NeoForge.EVENT_BUS.unregister(probe);
        }
        return seen[0];
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theKillFeedAttributesANonPreyVictimToo(GameTestHelper helper) {
        ServerPlayer killer = TestPlayers.createMortal(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        ServerPlayer human = TestPlayers.createMortal(helper);
        try {
            for (LivingEntity victim : List.<LivingEntity>of(villager, human)) {
                String what = victim.getType().toShortString();
                Entity direct = MarkTracker.killerOf(victim, victim.damageSources().source(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL, killer));
                helper.assertTrue(direct == killer, what + ": a source with the killer gave " + direct);
                victim.setLastHurtByMob(killer);
                Entity credit = MarkTracker.killerOf(victim, victim.damageSources().genericKill());
                helper.assertTrue(credit == killer, what + ": the kill credit alone gave " + credit);
                victim.setLastHurtByMob(null);
                helper.assertTrue(isCancelledDeathSeen(helper, victim, victim.damageSources().source(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL, killer)) == false,
                        what + ": a cancelled death reached a LOW, not-cancelled listener");
            }
            say("MarkTracker.killerOf gives the killer for a villager and a human victim by source and by kill credit alone; a LOW, not-cancelled listener never sees a cancelled death");
        } finally {
            villager.discard();
            HunterMarkGameTests.reset(killer, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theInnocenceTable(GameTestHelper helper) {
        ServerPlayer killer = TestPlayers.createMortal(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer fought100 = TestPlayers.createMortal(helper);
        ServerPlayer fought300 = TestPlayers.createMortal(helper);
        ServerPlayer witch = TestPlayers.createMortal(helper);
        ServerPlayer siphoner = TestPlayers.createMortal(helper);
        ServerPlayer vampire = HunterMarkGameTests.vampire(helper);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 2));
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            BloodlineAccess.set(siphoner, BloodlineState.NONE.toBuilder().species(Species.SIPHONER).build());
            fought100.setLastHurtMob(killer);
            fought100.tickCount += 100;
            fought300.setLastHurtMob(killer);
            fought300.tickCount += 300;
            record Row(String name, LivingEntity victim, boolean innocent, boolean prey) {
            }
            List<Row> table = new ArrayList<>(List.of(
                    new Row("villager", villager, true, false),
                    new Row("human", human, true, false),
                    new Row("human who hit the killer 100 ticks ago", fought100, false, false),
                    new Row("human who hit the killer 300 ticks ago", fought300, true, false),
                    new Row("witch who did not fight", witch, true, false),
                    new Row("siphoner who did not fight", siphoner, true, false),
                    new Row("vampire", vampire, true, true),
                    new Row("zombie", zombie, false, false),
                    new Row("cow", cow, false, false)));
            StringBuilder out = new StringBuilder();
            for (Row r : table) {
                boolean innocent = SparkHandler.isInnocent(killer, r.victim());
                boolean prey = MarkTracker.isPrey(r.victim());
                out.append(r.name()).append(": innocent=").append(innocent).append(" prey=").append(prey).append("; ");
                helper.assertTrue(innocent == r.innocent() && prey == r.prey(), r.name() + " read innocent=" + innocent + " prey=" + prey);
            }
            say("the table: " + out);
        } finally {
            villager.discard();
            zombie.discard();
            cow.discard();
            HunterMarkGameTests.reset(killer, human, fought100, fought300, witch, siphoner, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void samePackIsGroupWideWithoutWerewolves(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist member = GroupTestKit.human(helper, 2.5);
        online(leader, member);
        try {
            helper.assertTrue(join(leader, member), "the member could not join");
            boolean group = PackIndex.samePack(leader, member);
            boolean wolfGuard;
            try {
                var m = MarkTracker.class.getDeclaredMethod("samePack", Entity.class, Entity.class);
                m.setAccessible(true);
                wolfGuard = (boolean) m.invoke(null, leader, member);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
            boolean wolves = ModList.get().isLoaded("werewolves");
            helper.assertTrue(group, "PackIndex.samePack is false for two members of one group");
            helper.assertTrue(wolfGuard == wolves, "MarkTracker.samePack is " + wolfGuard + " with werewolves loaded=" + wolves);
            say("two members of a brotherhood: PackIndex.samePack=" + group + ", MarkTracker.samePack=" + wolfGuard + " (werewolves loaded=" + wolves + ")");
        } finally {
            clean(helper, leader, member);
        }
        helper.succeed();
    }

    // ---- the curse ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondCurseKeepsTheFirstTimer(GameTestHelper helper) {
        ServerPlayer killer = HunterMarkGameTests.vampire(helper);
        try {
            HunterCurse.curse(killer);
            long first = HunterCurse.nextDue(killer).orElse(-1);
            HunterCurse.curse(killer);
            long second = HunterCurse.nextDue(killer).orElse(-2);
            helper.assertTrue(first >= 0 && first == second, "the timer moved from " + first + " to " + second);
            helper.assertTrue(HunterMarkGameTests.roster(killer).cursed().stream().filter(killer.getUUID()::equals).count() == 1, "the roster holds the killer more than once");
            say("two curses: the next hallucination stayed at tick " + first + ", one roster entry");
        } finally {
            HunterMarkGameTests.reset(killer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCurseOfAnOfflineKillerIsLiftedAtLogin(GameTestHelper helper) {
        ServerPlayer killer = HunterMarkGameTests.vampire(helper);
        ServerPlayer other = TestPlayers.createMortal(helper);
        FiveRoster roster = HunterMarkGameTests.roster(killer);
        try {
            HunterCurse.curse(killer);
            helper.assertTrue(BloodlineAccess.get(killer).hunterCurse(), "the killer is not cursed");
            HunterMarkGameTests.online(other);
            HunterCurse.liftAll(helper.getLevel().getServer());
            helper.assertTrue(roster.liftedCurses().contains(killer.getUUID()) && !roster.cursed().contains(killer.getUUID()), "the offline killer is not marked to be freed: lifted=" + roster.liftedCurses().contains(killer.getUUID()));
            HunterCurse.onLogin(killer);
            helper.assertTrue(!BloodlineAccess.get(killer).hunterCurse() && !roster.liftedCurses().contains(killer.getUUID()), "the login did not free the killer");
            say("liftAll marked an offline killer in liftedCurses; onLogin cleared the curse and the flag");
        } finally {
            HunterMarkGameTests.clearHooks();
            HunterMarkGameTests.reset(killer, other);
        }
        helper.succeed();
    }

    // ---- the type of a group when the leader's faction changes ----

    private static GroupType stored(GameTestHelper helper, UUID leader) {
        GroupIndex g = GroupIndex.get(helper.getLevel());
        return g.typeOf(g.existingGroupOf(leader).orElseThrow());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderWhoJoinsTheHuntersSwitchesTheirGroup(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.witch(helper, 1.5);
        TalkingDuelist member = GroupTestKit.human(helper, 2.5);
        online(leader, member);
        try {
            helper.assertTrue(join(leader, member) && stored(helper, leader.getUUID()) == GroupType.COVEN, "no coven to begin with: " + Groups.typeOf(leader));
            VampirismAPI.factionPlayerHandler(leader).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            GroupType after = stored(helper, leader.getUUID());
            helper.assertTrue(after == GroupType.BROTHERHOOD, "the faction event did not run the switch: the group is " + after);
            say("a witch who leads a coven and joins the hunters: the faction event switched the group to " + after);
        } finally {
            clean(helper, leader, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchIsInnocentAndNotPreyForTheCode(GameTestHelper helper) {
        Witch mob = helper.spawnWithNoFreeWill(EntityType.WITCH, new BlockPos(2, 1, 2));
        ServerPlayer killer = TestPlayers.createMortal(helper);
        try {
            helper.assertTrue(!SparkHandler.isInnocent(killer, mob) && !MarkTracker.isPrey(mob), "a Witch mob is neither Villager nor Player: not innocent, not prey");
            say("the Witch mob is not innocent (not a Villager or a Player) and not prey; only a player witch is the Code's case");
        } finally {
            mob.discard();
            HunterMarkGameTests.reset(killer);
        }
        helper.succeed();
    }
}
