package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * The optional sireline death (plan 8, Task 3): when an Original is killed with white oak, everyone they sired and everyone
 * those sired die with them, unless the config leaves it off. The deaths are announced by hand-made events, as
 * {@code OriginalHybridGameTests} does, and a fake player's login is the handler called directly.
 */
@PrefixGameTestTemplate(false)
public final class SirelineDeathGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final List<UUID> SIRELINE_DEATHS = new ArrayList<>();

    static {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, e -> {
            if (e.getSource().is(SirelineDeath.DAMAGE_TYPE)) {
                SIRELINE_DEATHS.add(e.getEntity().getUUID());
            }
        });
    }

    private SirelineDeathGameTests() {
    }

    private static ServerPlayer mortal(GameTestHelper helper, Species s) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).build());
        return p;
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
        return SIRELINE_DEATHS.stream().filter(id::equals).count();
    }

    private static void revive(ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            p.setHealth(p.getMaxHealth());
        }
    }

    // ---- off by default ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sirelineDeathOffByDefault(GameTestHelper helper) {
        helper.assertTrue(!SpeciesConfig.sirelineDeath(), "the sireline death is on by default");
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = mortal(helper, Species.NONE);
        UUID offline = UUID.randomUUID();
        try {
            SireRecorder.record(root, child);
            SirelineIndex.get(helper.getLevel()).add(root.getUUID(), offline, "minecraft:player", "Away");
            online(root, child);
            die(root, whiteOakBlow(helper));
            helper.assertTrue(!dead(child) && deathsOf(child.getUUID()) == 0, "a descendant died with the sireline death off");
            helper.assertTrue(SirelineIndex.get(helper.getLevel()).sireOf(child.getUUID()).isPresent(), "the index changed");
            helper.assertTrue(!SirelineIndex.get(helper.getLevel()).takePendingPlayer(offline), "an offline descendant was marked");
        } finally {
            cleanUp(root, child);
            SirelineIndex.get(helper.getLevel()).remove(offline);
        }
        helper.succeed();
    }

    // ---- what counts ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sirelineDeathNeedsAWhiteOakKillOfAnOriginal(GameTestHelper helper) {
        ServerPlayer original = mortal(helper, Species.ORIGINAL);
        ServerPlayer hybrid = mortal(helper, Species.ORIGINAL_HYBRID);
        ServerPlayer plain = mortal(helper, Species.HYBRID);
        ServerPlayer child = mortal(helper, Species.NONE);
        ServerPlayer plainChild = mortal(helper, Species.NONE);
        ServerPlayer hybridChild = mortal(helper, Species.NONE);
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            SireRecorder.record(original, child);
            SireRecorder.record(plain, plainChild);
            SireRecorder.record(hybrid, hybridChild);
            online(original, hybrid, plain, child, plainChild, hybridChild);

            // not white oak
            die(original, original.damageSources().generic());
            helper.assertTrue(!dead(child), "a death by something else took the line");
            // a bare hand, a sword
            ServerPlayer bare = TestPlayers.create(helper);
            die(original, bare.damageSources().playerAttack(bare));
            helper.assertTrue(!dead(child), "a bare hand took the line");
            // /kill names nobody
            die(original, original.damageSources().genericKill());
            helper.assertTrue(!dead(child), "/kill took the line");
            // a death something else cancelled is no death
            LivingDeathEvent saved = new LivingDeathEvent(original, whiteOakBlow(helper));
            saved.setCanceled(true);
            NeoForge.EVENT_BUS.post(saved);
            helper.assertTrue(!dead(child), "a cancelled death took the line");
            // not an Original
            die(plain, whiteOakBlow(helper));
            helper.assertTrue(!dead(plainChild), "a plain hybrid's death took the line");

            // an Original hybrid counts, and so does the kill Original's dying state ends with (generic_kill naming the blow's wielder)
            ServerPlayer wielder = TestPlayers.create(helper);
            wielder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WhiteOak.STAKE.get()));
            die(hybrid, wielder.damageSources().source(DamageTypes.GENERIC_KILL, wielder));
            helper.assertTrue(dead(hybridChild), "the line of an Original hybrid did not die");

            die(original, whiteOakBlow(helper));
            helper.assertTrue(dead(child), "the line of an Original did not die");
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            cleanUp(original, hybrid, plain, child, plainChild, hybridChild);
        }
        helper.succeed();
    }

    // ---- the walk ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sirelineDeathKillsDescendants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.NONE);
        ServerPlayer b = mortal(helper, Species.HYBRID);
        ServerPlayer other = mortal(helper, Species.ORIGINAL_HYBRID);
        ServerPlayer otherChild = mortal(helper, Species.NONE);
        ServerPlayer outsider = mortal(helper, Species.NONE);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        UUID gone = UUID.randomUUID();
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            SirelineIndex index = SirelineIndex.get(level);
            SireRecorder.record(root, a);
            SireRecorder.record(a, b);
            SireRecorder.record(root, cow);
            SireRecorder.record(root, other);
            SireRecorder.record(other, otherChild);
            index.add(a.getUUID(), gone, "minecraft:cow", "Far Cow");
            online(root, a, b, other, otherChild, outsider);

            die(root, whiteOakBlow(helper));

            helper.assertTrue(dead(a) && dead(b), "the line did not die: a=" + dead(a) + " b=" + dead(b));
            helper.assertTrue(deathsOf(a.getUUID()) == 1 && deathsOf(b.getUUID()) == 1, "the deaths were not announced once each");
            helper.assertTrue(cow.isRemoved(), "the loaded creature stayed");
            helper.assertTrue(!dead(other) && !dead(otherChild), "an Original and their line died with the first");
            helper.assertTrue(!dead(outsider) && !dead(root), "someone outside the line died, or the Original was killed twice over");
            helper.assertTrue(index.isPendingMob(gone), "the unloaded creature is not waiting");
            helper.assertTrue(index.sireOf(a.getUUID()).isEmpty() && index.sireOf(b.getUUID()).isEmpty() && index.sireOf(cow.getUUID()).isEmpty() && index.sireOf(gone).isEmpty(),
                    "the dead stayed in the index");
            helper.assertTrue(BloodlineAccess.get(a).sire() == null && BloodlineAccess.get(b).sire() == null, "the dead kept their sire");
            helper.assertTrue(index.sireOf(other.getUUID()).map(root.getUUID()::equals).orElse(false), "the surviving Original left the line");
            helper.assertTrue(index.sireOf(otherChild.getUUID()).map(other.getUUID()::equals).orElse(false), "the survivor's line was cut");

            // the same death again finds nothing new
            int before = SIRELINE_DEATHS.size();
            die(root, whiteOakBlow(helper));
            helper.assertTrue(SIRELINE_DEATHS.size() == before, "the line died twice");
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            cleanUp(root, a, b, other, otherChild, outsider);
            SirelineIndex.get(level).takePendingMob(gone);
            cow.discard();
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
        helper.assertTrue(deathsOf(p.getUUID()) == 1, when + ": the sireline death was announced " + deathsOf(p.getUUID()) + " times");
        helper.assertTrue(BloodlineAccess.get(p).sire() == null, when + ": the dead kept their sire");
        helper.assertTrue(SirelineIndex.get(p.serverLevel()).sireOf(p.getUUID()).isEmpty(), when + ": the dead stayed in the index");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sirelineDeathKillsASiredVampire(GameTestHelper helper) {
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = siredVampire(helper, root);
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            online(root, child);
            die(root, whiteOakBlow(helper));
            checkDead(helper, child, "healthy vampire");
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            VampirismAPI.factionPlayerHandler(child).leaveFaction(false);
            cleanUp(root, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sirelineDeathKillsASiredVampireInTheDyingState(GameTestHelper helper) {
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = siredVampire(helper, root);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            online(root, child);
            child.hurt(child.damageSources().mobAttack(zombie), 1000f);
            helper.assertTrue(VampirismAPI.vampirePlayer(child).isDBNO() && !dead(child), "the vampire did not enter the dying state");
            // the dying state blocks every hit, so a plain hurt would leave them there and the line unbroken
            die(root, whiteOakBlow(helper));
            checkDead(helper, child, "dying vampire");
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            zombie.discard();
            VampirismAPI.factionPlayerHandler(child).leaveFaction(false);
            cleanUp(root, child);
        }
        helper.succeed();
    }

    /** A plain fake player cannot be hurt at all: the stand-in for anything that keeps a sired player alive. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSiredPlayerWhoSurvivesKeepsTheirSireAndOwesTheDeath(GameTestHelper helper) {
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        ServerPlayer child = TestPlayers.create(helper);
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            SirelineIndex index = SirelineIndex.get(helper.getLevel());
            SireRecorder.record(root, child);
            online(root, child);
            die(root, whiteOakBlow(helper));
            helper.assertTrue(!dead(child), "the test player died");
            helper.assertTrue(root.getUUID().equals(BloodlineAccess.get(child).sire()), "the sire was cleared before the death: " + BloodlineAccess.get(child).sire());
            helper.assertTrue(index.sireOf(child.getUUID()).isPresent(), "the survivor left the index");
            helper.assertTrue(index.takePendingPlayer(child.getUUID()), "the survivor does not owe the death");
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            cleanUp(root, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sirelineDeathStopsOnACycle(GameTestHelper helper) {
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.NONE);
        ServerPlayer b = mortal(helper, Species.NONE);
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            SireRecorder.record(root, a);
            SireRecorder.record(a, b);
            // the Original was cured and turned again by their own sired
            SirelineIndex.get(helper.getLevel()).add(b.getUUID(), root.getUUID(), "minecraft:player", "root");
            online(root, a, b);
            die(root, whiteOakBlow(helper));
            helper.assertTrue(dead(a) && dead(b), "the line did not die");
            helper.assertTrue(!dead(root), "the Original died of the cycle");
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            cleanUp(root, a, b);
        }
        helper.succeed();
    }

    // ---- offline and unloaded ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void offlineSiredDieOnLogin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        ServerPlayer away = mortal(helper, Species.NONE);
        ServerPlayer awayChild = mortal(helper, Species.NONE);
        ServerPlayer awayOriginal = mortal(helper, Species.ORIGINAL);
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            SirelineIndex index = SirelineIndex.get(level);
            SireRecorder.record(root, away);
            SireRecorder.record(away, awayChild);
            SireRecorder.record(root, awayOriginal);
            // only the Original is in the player list: the rest are "offline"
            online(root);
            die(root, whiteOakBlow(helper));
            helper.assertTrue(!dead(away) && !dead(awayChild), "an offline player died before logging in");
            helper.assertTrue(index.sireOf(away.getUUID()).isEmpty() && index.sireOf(awayChild.getUUID()).isEmpty(), "the offline dead stayed in the index");
            helper.assertTrue(index.sireOf(awayOriginal.getUUID()).isPresent(), "an offline Original left the line");

            SirelineDeath.onLogin(awayOriginal);
            helper.assertTrue(!dead(awayOriginal), "an offline Original died on login");

            SirelineDeath.onLogin(away);
            helper.assertTrue(dead(away) && deathsOf(away.getUUID()) == 1, "the offline player did not die on login");
            helper.assertTrue(BloodlineAccess.get(away).sire() == null, "the dead kept their sire");
            SirelineDeath.onLogin(awayChild);
            helper.assertTrue(dead(awayChild), "the second generation did not die on login");

            // once: the next login finds nothing
            revive(away, awayChild);
            SirelineDeath.onLogin(away);
            SirelineDeath.onLogin(awayChild);
            helper.assertTrue(!dead(away) && !dead(awayChild), "they died twice");
            helper.assertTrue(index.sireOf(away.getUUID()).isEmpty(), "the dead are back in the index");
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            cleanUp(root, away, awayChild, awayOriginal);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unloadedSiredMobsAreDiscardedWhenTheyLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer root = mortal(helper, Species.ORIGINAL);
        Cow later = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        Cow bystander = helper.spawn(EntityType.COW, new Vec3(8.5, 2, 4.5));
        UUID far = UUID.randomUUID();
        SpeciesConfig.overrideSirelineDeath(true);
        try {
            SirelineIndex index = SirelineIndex.get(level);
            SirelineIndex.get(level).add(root.getUUID(), far, "minecraft:cow", "Far Cow");
            online(root);
            int loaded = level.getChunkSource().getLoadedChunksCount();
            die(root, whiteOakBlow(helper));
            helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded, "the death loaded chunks");
            helper.assertTrue(level.getEntity(far) == null && index.isPendingMob(far), "the unloaded creature was touched or forgotten");

            // a creature that loads later and is waiting is turned away; another is not
            EntityJoinLevelEvent late = new EntityJoinLevelEvent(later, level);
            index.addPendingMob(later.getUUID());
            SirelineDeath.onJoin(late);
            helper.assertTrue(late.isCanceled(), "a waiting creature joined the world");
            helper.assertTrue(!index.isPendingMob(later.getUUID()), "the waiting creature stayed on the list");
            EntityJoinLevelEvent other = new EntityJoinLevelEvent(bystander, level);
            SirelineDeath.onJoin(other);
            helper.assertTrue(!other.isCanceled(), "an ordinary creature was turned away");
            // a client or a player is never looked at
            Entity player = root;
            SirelineDeath.onJoin(new EntityJoinLevelEvent(player, level));
        } finally {
            SpeciesConfig.overrideSirelineDeath(null);
            cleanUp(root);
            SirelineIndex.get(level).takePendingMob(far);
            SirelineIndex.get(level).takePendingMob(later.getUUID());
            later.discard();
            bystander.discard();
        }
        helper.succeed();
    }
}
