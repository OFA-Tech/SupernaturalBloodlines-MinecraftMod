package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.CompulsionImmunity;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.InfectionSire;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * Sireline recording (plan 8, Task 1): a player infected by a player-vampire, a converted creature, the first sire
 * staying, and the index following the state. The infection goes through Vampirism's own Infect action.
 */
@PrefixGameTestTemplate(false)
public final class SirelineGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation INFECT = ResourceLocation.fromNamespaceAndPath("vampirism", "infect");
    private static final ResourceLocation SANGUINARE = ResourceLocation.fromNamespaceAndPath("vampirism", "sanguinare");

    private record Heard(UUID sire, UUID child) {
    }

    private static final List<Heard> HEARD = new ArrayList<>();

    static {
        SireRecorder.addListener((sire, child) -> HEARD.add(new Heard(sire, child.getUUID())));
    }

    private SirelineGameTests() {
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static SirelineIndex index(GameTestHelper helper) {
        return SirelineIndex.get(helper.getLevel());
    }

    private static Holder<MobEffect> sanguinare() {
        return BuiltInRegistries.MOB_EFFECT.getHolder(SANGUINARE).orElseThrow();
    }

    private static void place(GameTestHelper helper, Entity e, int x) {
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2, 4.5));
        e.moveTo(at.x, at.y, at.z);
    }

    /** The vampire uses Vampirism's Infect action on the target, standing two blocks away and looking at it. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IAction.PERM infect(GameTestHelper helper, ServerPlayer sire, LivingEntity target) {
        place(helper, sire, 4);
        place(helper, target, 6);
        sire.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        // Infect refuses a player or villager who can see the vampire: the victim looks the other way
        target.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition().add(target.getEyePosition().subtract(sire.getEyePosition())));
        IAction action = VampirismRegistries.ACTION.get().get(INFECT);
        IFactionPlayer<?> fp = VampirismAPI.factionPlayerHandler(sire).getCurrentFactionPlayer().orElseThrow();
        IActionHandler handler = fp.getActionHandler();
        handler.unlockActions(List.of(action));
        return handler.toggleAction(action, new IAction.ActivationContext() {
            @Override
            public Optional<net.minecraft.core.BlockPos> targetBlock() {
                return Optional.empty();
            }

            @Override
            public Optional<Entity> targetEntity() {
                return Optional.of(target);
            }
        });
    }

    private static String why(ServerPlayer sire, ServerPlayer victim) {
        try {
            Class<?> vp = Class.forName("de.teamlapen.vampirism.entity.player.vampire.VampirePlayer");
            Object sireVp = vp.getMethod("get", net.minecraft.world.entity.player.Player.class).invoke(null, sire);
            Object victimVp = vp.getMethod("get", net.minecraft.world.entity.player.Player.class).invoke(null, victim);
            Class<?> helper = Class.forName("de.teamlapen.vampirism.util.Helper");
            return "canBeInfected=" + vp.getMethod("canBeInfected", de.teamlapen.vampirism.api.entity.vampire.IVampire.class).invoke(victimVp, sireVp)
                    + " canTurnPlayer=" + helper.getMethod("canTurnPlayer", de.teamlapen.vampirism.api.entity.vampire.IVampire.class, net.minecraft.world.entity.player.Player.class).invoke(null, sireVp, victim)
                    + " canBecomeVampire=" + helper.getMethod("canBecomeVampire", net.minecraft.world.entity.player.Player.class).invoke(null, victim);
        } catch (ReflectiveOperationException e) {
            return e.toString();
        }
    }

    private static void turn(GameTestHelper helper, ServerPlayer p) {
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not join the vampires");
    }

    private static void reset(ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    // ---- players ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void playerBittenAndTurnedRecordsSire(GameTestHelper helper) {
        ServerPlayer sire = vampire(helper);
        ServerPlayer victim = TestPlayers.create(helper);
        try {
            IAction.PERM perm = infect(helper, sire, victim);
            helper.assertTrue(perm == IAction.PERM.ALLOWED, "the infect action was refused: " + perm);
            helper.assertTrue(victim.hasEffect(sanguinare()), "the victim has no sanguinare: " + why(sire, victim));
            helper.assertTrue(victim.getData(SpeciesRegistries.INFECTION_SIRE).sire().equals(sire.getUUID()),
                    "pending infector is " + victim.getData(SpeciesRegistries.INFECTION_SIRE));
            helper.assertTrue(BloodlineAccess.get(victim).sire() == null, "sired before the sanguinare finished");
            turn(helper, victim);
            helper.assertTrue(sire.getUUID().equals(BloodlineAccess.get(victim).sire()), "sire is " + BloodlineAccess.get(victim).sire());
            SirelineIndex index = index(helper);
            helper.assertTrue(sire.getUUID().equals(index.sireOf(victim.getUUID()).orElse(null)), "index sire is " + index.sireOf(victim.getUUID()));
            helper.assertTrue(index.childrenOf(sire.getUUID()).stream()
                            .anyMatch(n -> n.id().equals(victim.getUUID()) && n.entityType().equals("minecraft:player")
                                    && n.name().equals(victim.getGameProfile().getName())),
                    "the sire's children are " + index.childrenOf(sire.getUUID()));
            helper.assertTrue(index.nameOf(sire.getUUID()).filter(sire.getGameProfile().getName()::equals).isPresent(), "the sire's name was not kept");
            helper.assertTrue(HEARD.contains(new Heard(sire.getUUID(), victim.getUUID())), "no listener heard it");
            helper.assertTrue(!victim.hasData(SpeciesRegistries.INFECTION_SIRE), "the pending infection was kept");
        } finally {
            reset(sire, victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sanguinareFromNowhereHasNoSire(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.create(helper);
        try {
            victim.addEffect(new MobEffectInstance(sanguinare(), 600));
            helper.assertTrue(!victim.hasData(SpeciesRegistries.INFECTION_SIRE), "an infector was invented");
            turn(helper, victim);
            helper.assertTrue(BloodlineAccess.get(victim).sire() == null, "sired by nobody: " + BloodlineAccess.get(victim).sire());
            helper.assertTrue(index(helper).sireOf(victim.getUUID()).isEmpty(), "indexed with no sire");
        } finally {
            reset(victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void staleInfectionSiresNobody(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.create(helper);
        try {
            long now = helper.getLevel().getGameTime();
            victim.setData(SpeciesRegistries.INFECTION_SIRE, new InfectionSire(UUID.randomUUID(), "gone", now - 1));
            turn(helper, victim);
            helper.assertTrue(BloodlineAccess.get(victim).sire() == null, "an expired infection sired: " + BloodlineAccess.get(victim).sire());
            helper.assertTrue(!victim.hasData(SpeciesRegistries.INFECTION_SIRE), "the expired infection was kept");
        } finally {
            reset(victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curedInfectionIsForgotten(GameTestHelper helper) {
        ServerPlayer sire = vampire(helper);
        ServerPlayer victim = TestPlayers.create(helper);
        try {
            helper.assertTrue(infect(helper, sire, victim) == IAction.PERM.ALLOWED && victim.hasData(SpeciesRegistries.INFECTION_SIRE), "not infected");
            victim.removeEffect(sanguinare());
            helper.assertTrue(!victim.hasData(SpeciesRegistries.INFECTION_SIRE), "the cure kept the infection");
            turn(helper, victim);
            helper.assertTrue(BloodlineAccess.get(victim).sire() == null, "sired after a cure: " + BloodlineAccess.get(victim).sire());
        } finally {
            reset(sire, victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void firstInfectorWins(GameTestHelper helper) {
        ServerPlayer first = vampire(helper);
        ServerPlayer second = vampire(helper);
        ServerPlayer victim = TestPlayers.create(helper);
        try {
            helper.assertTrue(infect(helper, first, victim) == IAction.PERM.ALLOWED, "first infection refused");
            infect(helper, second, victim);
            helper.assertTrue(victim.getData(SpeciesRegistries.INFECTION_SIRE).sire().equals(first.getUUID()), "the second infector took over");
        } finally {
            reset(first, second, victim);
        }
        helper.succeed();
    }

    // ---- creatures ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void convertedCreatureRecordsSire(GameTestHelper helper) {
        ServerPlayer sire = vampire(helper);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        try {
            IAction.PERM perm = infect(helper, sire, cow);
            helper.assertTrue(perm == IAction.PERM.ALLOWED && cow.hasEffect(sanguinare()), "the cow was not infected: " + perm);
            helper.assertTrue(cow.getData(SpeciesRegistries.INFECTION_SIRE).sire().equals(sire.getUUID()), "pending infector is wrong");
            int heard = HEARD.size();
            convert(cow);
            helper.assertTrue(HEARD.size() == heard + 1, "the listener heard " + (HEARD.size() - heard));
            Heard h = HEARD.get(heard);
            helper.assertTrue(h.sire().equals(sire.getUUID()), "heard sire " + h.sire());
            Entity converted = ((ServerLevel) helper.getLevel()).getEntity(h.child());
            helper.assertTrue(converted instanceof IConvertedCreature<?>, "the child is " + converted);
            helper.assertTrue(converted.getData(SpeciesRegistries.SIRE).equals(sire.getUUID()), "the converted cow's sire attachment is wrong");
            helper.assertTrue(!converted.hasData(SpeciesRegistries.INFECTION_SIRE), "the pending infection followed the cow");
            SirelineIndex index = index(helper);
            helper.assertTrue(sire.getUUID().equals(index.sireOf(h.child()).orElse(null)), "index sire is " + index.sireOf(h.child()));
            helper.assertTrue(index.childrenOf(sire.getUUID()).stream().anyMatch(n -> n.id().equals(h.child()) && n.entityType().equals("minecraft:cow")),
                    "the sire's children are " + index.childrenOf(sire.getUUID()));
            // cured: the creature is a plain one again and leaves the index
            Cow back = helper.spawn(EntityType.COW, new Vec3(8.5, 2, 4.5));
            NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post((LivingEntity) converted, back));
            helper.assertTrue(index.sireOf(h.child()).isEmpty(), "a cured creature stayed in the index");
            helper.assertTrue(!back.hasData(SpeciesRegistries.SIRE), "the cured creature kept the sire");
        } finally {
            reset(sire);
            helper.getLevel().getEntities((Entity) null, new AABB(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(16), e -> !(e instanceof ServerPlayer))
                    .forEach(Entity::discard);
        }
        helper.succeed();
    }

    /** What Vampirism does when sanguinare runs out on a creature: {@code ExtendedCreature.makeVampire()}. */
    private static void convert(LivingEntity e) {
        try {
            Class<?> c = Class.forName("de.teamlapen.vampirism.entity.ExtendedCreature");
            Optional<?> ext = (Optional<?>) c.getMethod("getSafe", Entity.class).invoke(null, e);
            c.getMethod("makeVampire").invoke(ext.orElseThrow());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ordinaryConversionsAreNotSireRecorded(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(8.5, 2, 4.5));
        int heard = HEARD.size();
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(cow, zombie));
        helper.assertTrue(HEARD.size() == heard && !zombie.hasData(SpeciesRegistries.SIRE), "a vanilla conversion was recorded");
        cow.discard();
        zombie.discard();
        helper.succeed();
    }

    // ---- the first sire wins ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sireNotOverwritten(GameTestHelper helper) {
        ServerPlayer a = TestPlayers.create(helper);
        ServerPlayer b = TestPlayers.create(helper);
        ServerPlayer child = TestPlayers.create(helper);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        try {
            helper.assertTrue(SireRecorder.record(a, child), "the first sire was refused");
            helper.assertTrue(!SireRecorder.record(b, child), "a second sire was taken");
            helper.assertTrue(a.getUUID().equals(BloodlineAccess.get(child).sire()), "sire is " + BloodlineAccess.get(child).sire());
            SirelineIndex index = index(helper);
            helper.assertTrue(a.getUUID().equals(index.sireOf(child.getUUID()).orElse(null)), "index sire is " + index.sireOf(child.getUUID()));
            helper.assertTrue(index.childrenOf(b.getUUID()).isEmpty(), "the second sire has children");

            helper.assertTrue(SireRecorder.record(a, cow) && !SireRecorder.record(b, cow), "creature sires: first wins");
            helper.assertTrue(a.getUUID().equals(cow.getData(SpeciesRegistries.SIRE)), "the cow's sire is " + cow.getData(SpeciesRegistries.SIRE));
            helper.assertTrue(index.childrenOf(b.getUUID()).isEmpty(), "the second sire has children (cow)");
            helper.assertTrue(!SireRecorder.record(child, child), "someone sired themselves");
        } finally {
            reset(a, b, child);
            cow.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void clearingTheSireTakesThePlayerOutOfTheIndex(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = TestPlayers.create(helper);
        try {
            SireRecorder.record(sire, child);
            helper.assertTrue(index(helper).sireOf(child.getUUID()).isPresent(), "not indexed");
            // what a cure does (the rules clear the sire)
            BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().sire(null).build());
            helper.assertTrue(index(helper).sireOf(child.getUUID()).isEmpty(), "still indexed after the sire was cleared");
            helper.assertTrue(index(helper).childrenOf(sire.getUUID()).isEmpty(), "the sire still lists the child");
            SireRecorder.record(sire, child);
            helper.assertTrue(index(helper).sireOf(child.getUUID()).isPresent(), "a cleared player cannot be sired again");
        } finally {
            reset(sire, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void indexIsHealedFromTheState(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = TestPlayers.create(helper);
        try {
            SireRecorder.record(sire, child);
            index(helper).remove(child.getUUID()); // the file was lost
            SireRecorder.sync(child);
            helper.assertTrue(sire.getUUID().equals(index(helper).sireOf(child.getUUID()).orElse(null)), "not healed");
            BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().sire(null).build());
            index(helper).add(sire.getUUID(), child.getUUID(), "minecraft:player", "stale");
            SireRecorder.sync(child);
            helper.assertTrue(index(helper).sireOf(child.getUUID()).isEmpty(), "a stale entry was kept");
        } finally {
            reset(sire, child);
        }
        helper.succeed();
    }

    // ---- compulsion ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void playersAreNeverCompelled(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        helper.assertTrue(CompulsionImmunity.isImmune(p), "a player can be compelled");
        helper.assertTrue(!CompulsionImmunity.isImmune(cow), "a cow is immune");
        cow.discard();
        helper.succeed();
    }
}
