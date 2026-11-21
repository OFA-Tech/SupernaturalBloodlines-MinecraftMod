package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * Line death (R122): when an Original is killed with white oak, everyone in their line dies with them, unless the config leaves it
 * off. The deaths are announced by hand-made events, as {@code OriginalHybridGameTests} does, and a fake player's login is the
 * handler called directly.
 */
@PrefixGameTestTemplate(false)
public final class LineDeathGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final List<UUID> LINE_DEATHS = new ArrayList<>();

    static {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, e -> {
            if (e.getSource().is(LineDeath.DAMAGE_TYPE)) {
                LINE_DEATHS.add(e.getEntity().getUUID());
            }
        });
    }

    private LineDeathGameTests() {
    }

    private static ServerPlayer mortal(GameTestHelper helper, Species s) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).build());
        return p;
    }

    private static LineTestPlayer talker(GameTestHelper helper, Species s) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        species(p, s);
        return p;
    }

    private static LineIndex lines(GameTestHelper helper) {
        return LineIndex.get(helper.getLevel().getServer());
    }

    private static DamageSource whiteOakBlow(GameTestHelper helper) {
        ServerPlayer killer = TestPlayers.create(helper);
        killer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WhiteOak.STAKE.get()));
        return killer.damageSources().playerAttack(killer);
    }

    private static void die(ServerPlayer victim, DamageSource source) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, source));
    }

    private static boolean dead(ServerPlayer p) {
        return p.isDeadOrDying();
    }

    private static long deathsOf(UUID id) {
        return LINE_DEATHS.stream().filter(id::equals).count();
    }

    private static void forget(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            lines(helper).remove(p.getUUID());
            lines(helper).takeOwed(p.getUUID());
        }
        cleanUp(ps);
    }

    private static Mob spawnVampire(GameTestHelper helper) {
        Mob mob = (Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")), new Vec3(6.5, 2, 4.5));
        mob.setNoAi(true);
        return mob;
    }

    // ---- on by default, and the old key is gone ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineDeathIsOnByDefault(GameTestHelper helper) {
        helper.assertTrue(SpeciesConfig.originalLineDeath(), "line death is off by default");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void oldSirelineDeathKeyIsGone(GameTestHelper helper) {
        helper.assertTrue(!SpeciesConfig.SPEC.getValues().contains("species.sireline_death"), "the old key is still in the config spec");
        helper.assertTrue(SpeciesConfig.SPEC.getValues().contains("species.original_line_death"), "the new key is missing from the config spec");
        helper.succeed();
    }

    // ---- what counts ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakKillOfAnOriginalKillsTheWholeLine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        LineTestPlayer a = talker(helper, Species.HYBRID);
        ServerPlayer b = mortal(helper, Species.HYBRID);
        ServerPlayer outsider = mortal(helper, Species.HYBRID);
        Mob mob = spawnVampire(helper);
        UUID c = UUID.randomUUID();
        UUID far = UUID.randomUUID();
        try {
            SireRecorder.record(o, a);
            SireRecorder.record(a, b);
            SireRecorder.record(o, mob);
            lines(helper).setRoot(c, o.getUUID(), true);
            lines(helper).setRoot(far, o.getUUID(), false);
            online(o, a, b, outsider);

            die(o, whiteOakBlow(helper));

            helper.assertTrue(dead(a) && dead(b), "the line did not die: a=" + dead(a) + " b=" + dead(b));
            helper.assertTrue(deathsOf(a.getUUID()) == 1 && deathsOf(b.getUUID()) == 1, "the deaths were not announced once each");
            helper.assertTrue(a.received("supernaturalbloodlines.line.death"), "the member was not told why");
            helper.assertTrue(mob.isRemoved(), "the loaded creature stayed");
            helper.assertTrue(!dead(o) && !dead(outsider), "the Original or someone outside the line died");
            LineIndex idx = lines(helper);
            helper.assertTrue(idx.rootOf(a.getUUID()).isEmpty() && idx.rootOf(b.getUUID()).isEmpty() && idx.rootOf(mob.getUUID()).isEmpty()
                    && idx.rootOf(far).isEmpty(), "the dead stayed in the line index");
            helper.assertTrue(idx.rootOf(o.getUUID()).filter(o.getUUID()::equals).isPresent(), "the Original left their own line");
            helper.assertTrue(idx.rootOf(c).filter(o.getUUID()::equals).isPresent() && idx.takeOwed(c).filter(o.getUUID()::equals).isPresent(),
                    "the offline member does not owe the death");
            helper.assertTrue(SirelineIndex.get(level).isPendingMob(far), "the unloaded creature is not waiting");
            helper.assertTrue(BloodlineAccess.get(a).sire() == null && BloodlineAccess.get(b).sire() == null, "the dead kept their sire");

            // the same death again finds nothing new
            int before = LINE_DEATHS.size();
            die(o, whiteOakBlow(helper));
            helper.assertTrue(LINE_DEATHS.size() == before, "the line died twice");
        } finally {
            forget(helper, o, a, b, outsider);
            lines(helper).remove(c);
            SirelineIndex.get(level).takePendingMob(far);
            mob.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridsDieWithTheLine(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer hybrid = mortal(helper, Species.HYBRID);
        ServerPlayer tribrid = mortal(helper, Species.TRIBRID);
        try {
            SireRecorder.record(o, hybrid);
            SireRecorder.record(o, tribrid);
            online(o, hybrid, tribrid);
            die(o, whiteOakBlow(helper));
            helper.assertTrue(dead(hybrid) && dead(tribrid), "a hybrid survived the line: hybrid=" + dead(hybrid) + " tribrid=" + dead(tribrid));
        } finally {
            forget(helper, o, hybrid, tribrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void freedMemberSurvives(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.HYBRID);
        ServerPlayer b = mortal(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, a);
            SireRecorder.record(a, b);
            lines(helper).free(a.getUUID(), o.getUUID(), LineIndex.Kind.BROKEN_LINE);
            online(o, a, b);
            die(o, whiteOakBlow(helper));
            helper.assertTrue(!dead(a), "a freed member died with their old Original");
            helper.assertTrue(dead(b), "the one still in the line survived");
        } finally {
            forget(helper, o, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalInTheLineKeepsTheirOwnBranch(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer other = mortal(helper, Species.ORIGINAL_HYBRID);
        ServerPlayer otherChild = mortal(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, other);
            SireRecorder.record(other, otherChild);
            helper.assertTrue(lines(helper).rootOf(other.getUUID()).filter(other.getUUID()::equals).isPresent(), "control: the sired Original is not their own root");
            online(o, other, otherChild);
            die(o, whiteOakBlow(helper));
            helper.assertTrue(!dead(other) && !dead(otherChild), "an Original and their line died with the first");
        } finally {
            forget(helper, o, other, otherChild);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nonWhiteOakDeathDoesNothing(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer hybrid = mortal(helper, Species.ORIGINAL_HYBRID);
        ServerPlayer plain = mortal(helper, Species.HYBRID);
        ServerPlayer child = mortal(helper, Species.HYBRID);
        ServerPlayer plainChild = mortal(helper, Species.HYBRID);
        ServerPlayer hybridChild = mortal(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, child);
            SireRecorder.record(plain, plainChild);
            SireRecorder.record(hybrid, hybridChild);
            online(o, hybrid, plain, child, plainChild, hybridChild);

            die(o, o.damageSources().generic());
            helper.assertTrue(!dead(child), "a death by something else took the line");
            ServerPlayer bare = TestPlayers.create(helper);
            die(o, bare.damageSources().playerAttack(bare));
            helper.assertTrue(!dead(child), "a bare hand took the line");
            die(o, o.damageSources().genericKill());
            helper.assertTrue(!dead(child), "/kill took the line");
            LivingDeathEvent saved = new LivingDeathEvent(o, whiteOakBlow(helper));
            saved.setCanceled(true);
            NeoForge.EVENT_BUS.post(saved);
            helper.assertTrue(!dead(child), "a cancelled death took the line");
            die(plain, whiteOakBlow(helper));
            helper.assertTrue(!dead(plainChild), "a plain hybrid's death took the line");

            // an Original hybrid counts, and so does the kill Original's dying state ends with (generic_kill naming the blow's wielder)
            ServerPlayer wielder = TestPlayers.create(helper);
            wielder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WhiteOak.STAKE.get()));
            die(hybrid, wielder.damageSources().source(DamageTypes.GENERIC_KILL, wielder));
            helper.assertTrue(dead(hybridChild), "the line of an Original hybrid did not die");

            die(o, whiteOakBlow(helper));
            helper.assertTrue(dead(child), "the line of an Original did not die");
        } finally {
            forget(helper, o, hybrid, plain, child, plainChild, hybridChild);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineDeathOffKillsNobody(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = mortal(helper, Species.HYBRID);
        UUID away = UUID.randomUUID();
        SpeciesConfig.overrideOriginalLineDeath(false);
        try {
            SireRecorder.record(o, child);
            lines(helper).setRoot(away, o.getUUID(), true);
            online(o, child);
            die(o, whiteOakBlow(helper));
            helper.assertTrue(!dead(child) && deathsOf(child.getUUID()) == 0, "a member died with line death off");
            helper.assertTrue(lines(helper).rootOf(child.getUUID()).isPresent(), "the line index changed");
            helper.assertTrue(lines(helper).takeOwed(away).isEmpty(), "an offline member was marked");
        } finally {
            SpeciesConfig.overrideOriginalLineDeath(null);
            forget(helper, o, child);
            lines(helper).remove(away);
        }
        helper.succeed();
    }

    // ---- a sired Vampirism vampire ----

    private static ServerPlayer siredVampire(GameTestHelper helper, ServerPlayer sire) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        SireRecorder.record(sire, p);
        return p;
    }

    private static void checkDead(GameTestHelper helper, ServerPlayer p, String when) {
        helper.assertTrue(dead(p), when + ": the sired vampire survived, health " + p.getHealth());
        helper.assertTrue(!VampirismAPI.vampirePlayer(p).isDBNO(), when + ": the dying state was left behind");
        helper.assertTrue(deathsOf(p.getUUID()) == 1, when + ": the line death was announced " + deathsOf(p.getUUID()) + " times");
        helper.assertTrue(BloodlineAccess.get(p).sire() == null, when + ": the dead kept their sire");
        helper.assertTrue(SirelineIndex.get(p.serverLevel()).sireOf(p.getUUID()).isEmpty(), when + ": the dead stayed in the sireline index");
        helper.assertTrue(lines(helper).rootOf(p.getUUID()).isEmpty(), when + ": the dead stayed in the line index");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineDeathKillsASiredVampire(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = siredVampire(helper, o);
        try {
            online(o, child);
            die(o, whiteOakBlow(helper));
            checkDead(helper, child, "healthy vampire");
        } finally {
            VampirismAPI.factionPlayerHandler(child).leaveFaction(false);
            forget(helper, o, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineDeathKillsASiredVampireInTheDyingState(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = siredVampire(helper, o);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        try {
            online(o, child);
            child.hurt(child.damageSources().mobAttack(zombie), 1000f);
            helper.assertTrue(VampirismAPI.vampirePlayer(child).isDBNO() && !dead(child), "the vampire did not enter the dying state");
            // the dying state blocks every hit, so a plain hurt would leave them there and the line unbroken
            die(o, whiteOakBlow(helper));
            checkDead(helper, child, "dying vampire");
        } finally {
            zombie.discard();
            VampirismAPI.factionPlayerHandler(child).leaveFaction(false);
            forget(helper, o, child);
        }
        helper.succeed();
    }

    /** A plain fake player cannot be hurt at all: the stand-in for anything that keeps a sired player alive. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoSurvivesKeepsTheirSireAndOwesTheDeath(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = TestPlayers.create(helper);
        // a member who is supernatural: line death spares someone who only drank the blood and is no vampire yet
        BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.HYBRID).build());
        try {
            SireRecorder.record(o, child);
            online(o, child);
            die(o, whiteOakBlow(helper));
            helper.assertTrue(!dead(child), "the test player died");
            helper.assertTrue(o.getUUID().equals(BloodlineAccess.get(child).sire()), "the sire was cleared before the death: " + BloodlineAccess.get(child).sire());
            helper.assertTrue(lines(helper).rootOf(child.getUUID()).filter(o.getUUID()::equals).isPresent(), "the survivor left the line");
            helper.assertTrue(lines(helper).takeOwed(child.getUUID()).isPresent(), "the survivor does not owe the death");
        } finally {
            forget(helper, o, child);
        }
        helper.succeed();
    }

    /** The hunter whose blow last hurt a sired vampire is not credited with the line's death. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineDeathGivesNoMarkKill(GameTestHelper helper) {
        ServerPlayer hunter = HunterMarkGameTests.marked(helper, 0);
        ServerPlayer victim = HunterMarkGameTests.vampire(helper);
        try {
            victim.setLastHurtByMob(hunter);
            die(victim, helper.getLevel().damageSources().source(LineDeath.DAMAGE_TYPE));
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 0, "a line death grew the mark: " + BloodlineAccess.get(hunter).markKills());
            // the same credit with an ordinary death does count, so the check above can fail
            victim.setLastHurtByMob(hunter);
            die(victim, helper.getLevel().damageSources().generic());
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 1, "the control kill gave " + BloodlineAccess.get(hunter).markKills());
        } finally {
            HunterMarkGameTests.reset(hunter, victim);
        }
        helper.succeed();
    }

    // ---- offline and unloaded ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void offlinePlayerPaysAtLogin(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        LineTestPlayer c = talker(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, c);
            lines(helper).addOwed(c.getUUID(), o.getUUID());
            LineDeath.onLogin(c);
            helper.assertTrue(dead(c) && deathsOf(c.getUUID()) == 1, "the offline player did not die on login");
            helper.assertTrue(c.received("supernaturalbloodlines.line.death_login"), "they were not told why");
            helper.assertTrue(BloodlineAccess.get(c).sire() == null, "the dead kept their sire");
            helper.assertTrue(lines(helper).rootOf(c.getUUID()).isEmpty(), "the dead stayed in the line");

            // once: the next login finds nothing
            c.setHealth(c.getMaxHealth());
            LineDeath.onLogin(c);
            helper.assertTrue(!dead(c), "they died twice");
        } finally {
            forget(helper, o, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginMessageSaysUnknownUntilDiscovered(GameTestHelper helper) {
        UUID klaus = NpcOriginal.KLAUS.uuid();
        LineTestPlayer hidden = talker(helper, Species.HYBRID);
        LineTestPlayer known = talker(helper, Species.HYBRID);
        try {
            for (LineTestPlayer p : List.of(hidden, known)) {
                lines(helper).setRoot(p.getUUID(), klaus, true);
                lines(helper).addOwed(p.getUUID(), klaus);
            }
            BloodlineAccess.set(known, BloodlineAccess.get(known).toBuilder().knownLine(klaus).build());
            LineDeath.onLogin(hidden);
            LineDeath.onLogin(known);
            helper.assertTrue(hidden.heard().contains("[supernaturalbloodlines.line.unknown_original]"), "the hidden Original was not called unknown: " + hidden.heard());
            helper.assertTrue(!hidden.heard().contains("klaus"), "the login message named an undiscovered Original: " + hidden.heard());
            helper.assertTrue(known.heard().contains("[supernaturalbloodlines.original.klaus]"), "the discovered Original was not named: " + known.heard());
        } finally {
            forget(helper, hidden, known);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void owedDeathIsDroppedWhenNoLongerAMember(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer freed = mortal(helper, Species.HYBRID);
        ServerPlayer cured = mortal(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, freed);
            SireRecorder.record(o, cured);
            lines(helper).addOwed(freed.getUUID(), o.getUUID());
            lines(helper).addOwed(cured.getUUID(), o.getUUID());
            lines(helper).free(freed.getUUID(), o.getUUID(), LineIndex.Kind.BROKEN_LINE);
            species(cured, Species.NONE);
            LineDeath.onLogin(freed);
            LineDeath.onLogin(cured);
            helper.assertTrue(!dead(freed), "a freed player was killed by their old Original");
            helper.assertTrue(!dead(cured), "a cured player was killed by their old Original");
            helper.assertTrue(lines(helper).takeOwed(freed.getUUID()).isEmpty() && lines(helper).takeOwed(cured.getUUID()).isEmpty(), "the debt was kept");
        } finally {
            forget(helper, o, freed, cured);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void npcRootDeathKillsItsLineAndMarksItDead(GameTestHelper helper) {
        UUID klaus = NpcOriginal.KLAUS.uuid();
        ServerPlayer a = mortal(helper, Species.HYBRID);
        Mob mob = spawnVampire(helper);
        Mob other = spawnVampire(helper);
        try {
            lines(helper).setRoot(a.getUUID(), klaus, true);
            // un-sired NPC vampires hold their line on the creature alone: the index does not list them, line death still reaches them
            mob.setData(SpeciesRegistries.LINE, klaus);
            other.setData(SpeciesRegistries.LINE, NpcOriginal.ELIJAH.uuid());
            helper.assertTrue(lines(helper).rootOf(mob.getUUID()).isEmpty(), "control: the creature is in the index");
            online(a);
            LineDeath.run(helper.getLevel().getServer(), klaus);
            helper.assertTrue(dead(a), "a member of Klaus's line survived");
            helper.assertTrue(mob.isRemoved(), "a loaded creature of Klaus's line stayed");
            helper.assertTrue(!other.isRemoved(), "a creature of another line was removed");
            helper.assertTrue(lines(helper).isDead(klaus), "Klaus's line is not marked dead");
        } finally {
            lines(helper).unmarkDead(klaus);
            forget(helper, a);
            other.discard();
            mob.discard();
        }
        helper.succeed();
    }

    /** The mob was not loaded when its Original died: nothing was recorded for it, and it is turned away when it loads (no pending list for these). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void creatureOfADeadLineIsTurnedAwayWhenItLoads(GameTestHelper helper) {
        UUID klaus = NpcOriginal.KLAUS.uuid();
        Mob late = spawnVampire(helper);
        Mob living = spawnVampire(helper);
        try {
            late.setData(SpeciesRegistries.LINE, klaus);
            living.setData(SpeciesRegistries.LINE, NpcOriginal.ELIJAH.uuid());
            lines(helper).markDead(klaus);
            EntityJoinLevelEvent lateJoin = new EntityJoinLevelEvent(late, helper.getLevel());
            LineDeath.onJoin(lateJoin);
            helper.assertTrue(lateJoin.isCanceled(), "a creature of a dead line joined the world");
            EntityJoinLevelEvent livingJoin = new EntityJoinLevelEvent(living, helper.getLevel());
            LineDeath.onJoin(livingJoin);
            helper.assertTrue(!livingJoin.isCanceled(), "a creature of a living line was turned away");
            helper.assertTrue(!SirelineIndex.get(helper.getLevel()).isPendingMob(late.getUUID()), "a pending entry was kept for it");
        } finally {
            lines(helper).unmarkDead(klaus);
            late.discard();
            living.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void legacyOwedPlayerIsMigratedAtLogin(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        LineTestPlayer c = talker(helper, Species.HYBRID);
        LineTestPlayer lost = talker(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, c);
            // a world saved by the old sireline death: owed in the sireline index, and no entry in the line index
            lines(helper).remove(c.getUUID());
            // the old rule removed the doomed player from the sire index, so only their own recorded sire is left to climb from
            SirelineIndex.get(helper.getLevel()).remove(c.getUUID());
            SirelineIndex.get(helper.getLevel()).addLegacyPendingPlayer(c.getUUID());
            SirelineIndex.get(helper.getLevel()).addLegacyPendingPlayer(lost.getUUID());
            online(o, c, lost);
            LineDeath.onLogin(c);
            LineDeath.onLogin(lost);
            helper.assertTrue(dead(c), "an old debt of a member of a line was not paid");
            helper.assertTrue(!dead(lost), "an old debt with no Original above was paid");
            helper.assertTrue(!SirelineIndex.get(helper.getLevel()).takeLegacyPendingPlayer(c.getUUID())
                    && !SirelineIndex.get(helper.getLevel()).takeLegacyPendingPlayer(lost.getUUID()), "the old debts were kept");
        } finally {
            forget(helper, o, c, lost);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlineMessageNeverNamesAnUndiscoveredOriginal(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        LineTestPlayer hidden = talker(helper, Species.HYBRID);
        LineTestPlayer known = talker(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, hidden);
            SireRecorder.record(o, known);
            BloodlineAccess.set(known, BloodlineAccess.get(known).toBuilder().knownLine(o.getUUID()).build());
            online(o, hidden, known);
            die(o, whiteOakBlow(helper));
            helper.assertTrue(hidden.heard().contains("[supernaturalbloodlines.line.unknown_original]") && !hidden.heard().contains(o.getGameProfile().getName()),
                    "the message named an Original the player has not discovered: " + hidden.heard());
            helper.assertTrue(known.heard().contains(o.getGameProfile().getName()), "a discovered Original was not named: " + known.heard());
        } finally {
            forget(helper, o, hidden, known);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unloadedSiredMobsAreDiscardedWhenTheyLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        Mob later = spawnVampire(helper);
        Cow bystander = helper.spawn(EntityType.COW, new Vec3(8.5, 2, 4.5));
        UUID far = UUID.randomUUID();
        try {
            SirelineIndex index = SirelineIndex.get(level);
            index.add(o.getUUID(), far, "minecraft:cow", "Far Cow");
            lines(helper).setRoot(far, o.getUUID(), false);
            online(o);
            int loaded = level.getChunkSource().getLoadedChunksCount();
            die(o, whiteOakBlow(helper));
            helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded, "the death loaded chunks");
            helper.assertTrue(level.getEntity(far) == null && index.isPendingMob(far), "the unloaded creature was touched or forgotten");

            // a creature that loads later and is waiting is turned away; another is not
            EntityJoinLevelEvent late = new EntityJoinLevelEvent(later, level);
            index.addPendingMob(later.getUUID(), level.getServer().overworld().getGameTime());
            LineDeath.onJoin(late);
            helper.assertTrue(late.isCanceled(), "a waiting creature joined the world");
            helper.assertTrue(!index.isPendingMob(later.getUUID()), "the waiting creature stayed on the list");
            EntityJoinLevelEvent other = new EntityJoinLevelEvent(bystander, level);
            LineDeath.onJoin(other);
            helper.assertTrue(!other.isCanceled(), "an ordinary creature was turned away");
            // a player is never looked at
            Entity player = o;
            LineDeath.onJoin(new EntityJoinLevelEvent(player, level));
        } finally {
            forget(helper, o);
            SirelineIndex.get(level).takePendingMob(far);
            SirelineIndex.get(level).takePendingMob(later.getUUID());
            later.discard();
            bystander.discard();
        }
        helper.succeed();
    }

    /** R101b: line death is blood, not bond. A child whose bond was severed still dies with the Original. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severedChildStillDiesWithTheOriginal(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = mortal(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, child);
            SeveranceGameTests.sever(child);
            helper.assertTrue(BloodlineAccess.get(child).bondBroken(), "control: the bond is not severed");
            online(o, child);
            die(o, whiteOakBlow(helper));
            helper.assertTrue(dead(child), "a severed child did not die with the Original");
        } finally {
            forget(helper, o, child);
        }
        helper.succeed();
    }

    /** The Original's own white oak is not a line death; someone else's is. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalsOwnWhiteOakDoesNotStartLineDeath(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = mortal(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, child);
            online(o, child);
            o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WhiteOak.STAKE.get()));
            die(o, o.damageSources().playerAttack(o));
            helper.assertTrue(!dead(child), "a self-inflicted white oak blow took the line");
            die(o, o.damageSources().source(DamageTypes.GENERIC_KILL, o));
            helper.assertTrue(!dead(child), "a generic_kill naming the Original themselves took the line");
            die(o, o.damageSources().source(DamageTypes.GENERIC_KILL));
            helper.assertTrue(!dead(child), "a kill with no killer took the line");
            // the same weapon in someone else's hand does
            die(o, whiteOakBlow(helper));
            helper.assertTrue(dead(child), "control: another entity's white oak did not take the line");
        } finally {
            forget(helper, o, child);
        }
        helper.succeed();
    }

    /** Line death takes vampires and players: a converted animal is in no line and is never discarded, loaded or not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void convertedAnimalsAreNeverTakenByLineDeath(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        Entity cow = helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:converted_cow")), new Vec3(8.5, 2, 4.5));
        Entity lateCow = helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:converted_cow")), new Vec3(10.5, 2, 4.5));
        Mob vampire = spawnVampire(helper);
        try {
            helper.assertTrue(cow instanceof IConvertedCreature<?> && LineDeath.dies(vampire) && !LineDeath.dies(cow), "control: the converted cow is not a converted creature (or the vampire is not killable)");
            SireRecorder.record(o, (LivingEntity) cow);
            helper.assertTrue(lines(helper).rootOf(cow.getUUID()).isEmpty(), "a converted animal was given a line when it was sired");
            // an old entry (or a creature that stopped being one) still does not die
            lines(helper).setRoot(cow.getUUID(), o.getUUID(), false);
            SireRecorder.record(o, vampire);
            SirelineIndex.get(level).addPendingMob(lateCow.getUUID(), level.getServer().overworld().getGameTime());
            online(o);
            LineDeath.run(level.getServer(), o.getUUID());
            helper.assertTrue(vampire.isRemoved(), "control: the vampire stayed");
            helper.assertTrue(!cow.isRemoved(), "a converted cow was discarded");
            EntityJoinLevelEvent join = new EntityJoinLevelEvent(lateCow, level);
            LineDeath.onJoin(join);
            helper.assertTrue(!join.isCanceled(), "a converted cow owed a death was turned away");
            helper.assertTrue(!SirelineIndex.get(level).isPendingMob(lateCow.getUUID()), "its debt was kept");
        } finally {
            forget(helper, o);
            lines(helper).remove(cow.getUUID());
            SirelineIndex.get(level).takePendingMob(lateCow.getUUID());
            cow.discard();
            lateCow.discard();
            vampire.discard();
        }
        helper.succeed();
    }

    /** With line death off, a death the old rule still owes is dropped (and logged), not paid. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void legacyOwedDeathIsDroppedWhenLineDeathIsOff(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        LineTestPlayer paid = talker(helper, Species.HYBRID);
        LineTestPlayer dropped = talker(helper, Species.HYBRID);
        try {
            for (LineTestPlayer c : List.of(paid, dropped)) {
                SireRecorder.record(o, c);
                lines(helper).remove(c.getUUID());
                SirelineIndex.get(helper.getLevel()).remove(c.getUUID());
                SirelineIndex.get(helper.getLevel()).addLegacyPendingPlayer(c.getUUID());
            }
            online(o, paid, dropped);
            // the control: the same debt with line death on is paid
            LineDeath.onLogin(paid);
            helper.assertTrue(dead(paid), "control: an old debt was not paid with line death on");
            SpeciesConfig.overrideOriginalLineDeath(false);
            LineDeath.onLogin(dropped);
            helper.assertTrue(!dead(dropped), "an old debt was paid with line death off");
            helper.assertTrue(!dropped.received("supernaturalbloodlines.line.death_login") && lines(helper).rootOf(dropped.getUUID()).isEmpty(),
                    "the old debt was turned into a new one: " + dropped.heard() + " / " + lines(helper).rootOf(dropped.getUUID()));
            helper.assertTrue(!SirelineIndex.get(helper.getLevel()).takeLegacyPendingPlayer(dropped.getUUID()), "the old debt was kept");
            helper.assertTrue(lines(helper).takeOwed(dropped.getUUID()).isEmpty(), "the old debt became a new one");
            // and switched on again, a login owes nothing: the debt is gone, not postponed
            SpeciesConfig.overrideOriginalLineDeath(null);
            LineDeath.onLogin(dropped);
            helper.assertTrue(!dead(dropped), "the dropped debt came back");
        } finally {
            SpeciesConfig.overrideOriginalLineDeath(null);
            forget(helper, o, paid, dropped);
        }
        helper.succeed();
    }
}
