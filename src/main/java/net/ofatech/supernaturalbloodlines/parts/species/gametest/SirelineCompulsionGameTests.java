package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.Capture;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.named;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.place;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.run;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.CompulsionImmunity;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallFollowers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.Compulsion;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * Compulsion (plan 8, Task 3): sneak + empty hand on a villager or a lesser vampire, with eye contact. The click is
 * handled by calling the listener with a hand-made event, so no other mod sees a fake player interact.
 */
@PrefixGameTestTemplate(false)
public final class SirelineCompulsionGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final Set<UUID> IMMUNE = new HashSet<>();

    static {
        CompulsionImmunity.register(e -> IMMUNE.contains(e.getUUID()));
    }

    private SirelineCompulsionGameTests() {
    }

    private static ServerPlayer caster(GameTestHelper helper) {
        ServerPlayer p = named(helper, helper.getLevel(), "caster");
        species(p, Species.ORIGINAL);
        p.setShiftKeyDown(true);
        place(helper, p, 4);
        return p;
    }

    private static Villager villager(GameTestHelper helper, int x) {
        Villager v = helper.spawn(EntityType.VILLAGER, new Vec3(x + 0.5, 2, 4.5));
        v.setNoAi(true);
        return v;
    }

    private static Mob mob(GameTestHelper helper, String id, int x) {
        Mob m = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)).create(helper.getLevel());
        m.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2, 4.5));
        m.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(m);
        return m;
    }

    private static void look(Entity e, Vec3 at) {
        e.lookAt(EntityAnchorArgument.Anchor.EYES, at);
        e.setYHeadRot(e.getYRot());
    }

    private static void gaze(Entity a, Entity b) {
        look(a, b.getEyePosition());
        look(b, a.getEyePosition());
    }

    private static PlayerInteractEvent.EntityInteract click(ServerPlayer p, Entity target) {
        PlayerInteractEvent.EntityInteract e = new PlayerInteractEvent.EntityInteract(p, InteractionHand.MAIN_HAND, target);
        Compulsion.onInteract(e);
        return e;
    }

    private static int reputation(Villager v, UUID player) {
        return v.getGossips().getReputation(player, t -> t == GossipType.MAJOR_POSITIVE);
    }

    private static String token(GameTestHelper helper, ServerPlayer caster, Villager v) {
        return Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).orElseThrow(() -> new AssertionError("no choice was offered"));
    }

    // ---- villagers ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalCompelsVillagerDiscount(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        Villager v = villager(helper, 6);
        try {
            gaze(caster, v);
            PlayerInteractEvent.EntityInteract e = click(caster, v);
            helper.assertTrue(e.isCanceled(), "the click still opened the trade screen");
            String token = token(helper, caster, v);
            helper.assertTrue(reputation(v, caster.getUUID()) == 0, "the discount came before the choice");

            Capture out = new Capture();
            helper.assertTrue(run(caster, 0, out, "bloodline compel " + token + " discount") == 1, "the discount failed: " + out.text());
            helper.assertTrue(reputation(v, caster.getUUID()) == 100, "reputation is " + reputation(v, caster.getUUID()));
            helper.assertTrue(v.hasData(SpeciesRegistries.VILLAGER_DISCOUNT), "the discount is not recorded");
            helper.assertTrue(run(caster, 0, new Capture(), "bloodline compel " + token + " discount") == 0, "a token worked twice");
            helper.assertTrue(reputation(v, caster.getUUID()) == 100, "the second use changed the reputation");

            long now = helper.getLevel().getGameTime();
            Compulsion.expireDiscount(v, now + Compulsion.DISCOUNT_TICKS - 1);
            helper.assertTrue(reputation(v, caster.getUUID()) == 100, "the discount ended early");
            Compulsion.expireDiscount(v, now + Compulsion.DISCOUNT_TICKS);
            helper.assertTrue(reputation(v, caster.getUUID()) == 0, "the discount did not end: " + reputation(v, caster.getUUID()));
            helper.assertTrue(!v.hasData(SpeciesRegistries.VILLAGER_DISCOUNT), "the record outlived the discount");
        } finally {
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void discountGivesBackOnlyWhatItAdded(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        Villager v = villager(helper, 6);
        try {
            // an earlier everlasting gossip (a cured villager has one): the discount tops it up, then only the top-up goes
            v.getGossips().add(caster.getUUID(), GossipType.MAJOR_POSITIVE, 8);
            gaze(caster, v);
            click(caster, v);
            helper.assertTrue(run(caster, 0, new Capture(), "bloodline compel " + token(helper, caster, v) + " discount") == 1, "the discount failed");
            helper.assertTrue(reputation(v, caster.getUUID()) == 100, "reputation is " + reputation(v, caster.getUUID()));

            // compelling again before it ends starts over, it does not stack
            Compulsion.clearGuard(caster.getUUID());
            click(caster, v);
            helper.assertTrue(run(caster, 0, new Capture(), "bloodline compel " + token(helper, caster, v) + " discount") == 1, "the second discount failed");
            helper.assertTrue(reputation(v, caster.getUUID()) == 100, "reputation after a second discount is " + reputation(v, caster.getUUID()));

            Compulsion.expireDiscount(v, helper.getLevel().getGameTime() + Compulsion.DISCOUNT_TICKS);
            helper.assertTrue(reputation(v, caster.getUUID()) == 40, "the earlier 8 was lost or kept wrongly: " + reputation(v, caster.getUUID()));
        } finally {
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compelledVillagerFollowsForFiveMinutes(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        Villager v = villager(helper, 6);
        try {
            online(caster);
            gaze(caster, v);
            click(caster, v);
            Capture out = new Capture();
            helper.assertTrue(run(caster, 0, out, "bloodline compel " + token(helper, caster, v) + " follow") == 1, "follow failed: " + out.text());
            helper.assertTrue(CallFollowers.isFollowing(v.getUUID()), "the villager is not following");
            helper.assertTrue(reputation(v, caster.getUUID()) == 0, "following also gave a discount");
            long now = helper.getLevel().getGameTime();
            CallFollowers.tick(helper.getLevel().getServer(), now + 299 * 20L);
            helper.assertTrue(CallFollowers.isFollowing(v.getUUID()), "the villager stopped before 5 minutes");
            CallFollowers.tick(helper.getLevel().getServer(), now + 301 * 20L);
            helper.assertTrue(!CallFollowers.isFollowing(v.getUUID()), "the villager follows forever");
        } finally {
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void choiceWindowClosesAfterFiveSeconds(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        Villager v = villager(helper, 6);
        Villager other = villager(helper, 8);
        try {
            gaze(caster, v);
            click(caster, v);
            String token = token(helper, caster, v);
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(Compulsion.choose(caster, token, "discount", now + Compulsion.CHOICE_TICKS + 1) != null, "a late answer worked");
            helper.assertTrue(reputation(v, caster.getUUID()) == 0, "a late answer gave the discount");

            // someone else cannot use the token, and neither can the caster on another villager
            Compulsion.clearGuard(caster.getUUID());
            click(caster, v);
            String again = token(helper, caster, v);
            ServerPlayer thief = named(helper, helper.getLevel(), "thief");
            helper.assertTrue(Compulsion.choose(thief, again, "discount", now) != null, "a stranger used the caster's token");
            helper.assertTrue(Compulsion.choose(caster, again, "discount", now + Compulsion.CHOICE_TICKS) == null, "an answer on the last tick was refused");
            helper.assertTrue(reputation(other, caster.getUUID()) == 0, "the wrong villager got it");
        } finally {
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
            v.discard();
            other.discard();
        }
        helper.succeed();
    }

    // ---- lesser vampires ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compelLesserVampireMakesSired(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        ServerPlayer child = named(helper, helper.getLevel(), "child");
        ServerPlayer stranger = named(helper, helper.getLevel(), "stranger");
        Mob vamp = mob(helper, "vampirism:vampire", 6);
        try {
            SireRecorder.record(caster, child);
            vamp.setTarget(caster);
            helper.assertTrue(vamp.getTarget() == caster, "the vampire was not hostile to start with");
            gaze(caster, vamp);
            PlayerInteractEvent.EntityInteract e = click(caster, vamp);
            helper.assertTrue(e.isCanceled(), "the click went through");
            SirelineIndex index = SirelineIndex.get(helper.getLevel());
            helper.assertTrue(caster.getUUID().equals(index.sireOf(vamp.getUUID()).orElse(null)), "the index sire is " + index.sireOf(vamp.getUUID()));
            helper.assertTrue(caster.getUUID().equals(vamp.getData(SpeciesRegistries.SIRE)), "the sire attachment is wrong");
            helper.assertTrue(caster.getUUID().equals(vamp.getData(SpeciesRegistries.COMPELLED_BY)), "the compelled-by attachment is wrong");
            helper.assertTrue(index.childrenOf(caster.getUUID()).stream().anyMatch(n -> n.id().equals(vamp.getUUID()) && n.entityType().equals("vampirism:vampire")),
                    "the sire's children are " + index.childrenOf(caster.getUUID()));
            helper.assertTrue(vamp.getTarget() == null, "the vampire kept its target: " + vamp.getTarget());

            vamp.setTarget(caster);
            helper.assertTrue(vamp.getTarget() == null, "the vampire went for its sire");
            vamp.setTarget(child);
            helper.assertTrue(vamp.getTarget() == null, "the vampire went for its sire's sired");
            vamp.setTarget(stranger);
            helper.assertTrue(vamp.getTarget() == stranger, "the vampire stopped hunting everyone");
        } finally {
            cleanUp(caster, child, stranger);
            Compulsion.clearGuard(caster.getUUID());
            SirelineIndex.get(helper.getLevel()).remove(vamp.getUUID());
            vamp.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyLesserVampiresAreCompelledAndOnlyOnce(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        ServerPlayer rival = named(helper, helper.getLevel(), "rival");
        Mob advanced = mob(helper, "vampirism:advanced_vampire", 6);
        Mob baron = mob(helper, "vampirism:vampire_baron", 7);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(3.5, 2, 4.5));
        Mob vamp = mob(helper, "vampirism:vampire", 5);
        try {
            for (Mob m : new Mob[] {advanced, baron}) {
                Compulsion.clearGuard(caster.getUUID());
                place(helper, caster, 4);
                gaze(caster, m);
                click(caster, m);
                helper.assertTrue(!m.hasData(SpeciesRegistries.SIRE), m.getType() + " was compelled");
            }
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, cow);
            PlayerInteractEvent.EntityInteract e = click(caster, cow);
            helper.assertTrue(!e.isCanceled() && !cow.hasData(SpeciesRegistries.SIRE), "a cow was compelled");

            // the first sire wins: a vampire another Original holds does not switch sides
            SireRecorder.record(rival, vamp);
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, vamp);
            click(caster, vamp);
            helper.assertTrue(rival.getUUID().equals(vamp.getData(SpeciesRegistries.SIRE)), "the vampire changed sire");
            helper.assertTrue(!vamp.hasData(SpeciesRegistries.COMPELLED_BY), "the vampire answers to two");
        } finally {
            cleanUp(caster, rival);
            Compulsion.clearGuard(caster.getUUID());
            for (Mob m : new Mob[] {advanced, baron, cow, vamp}) {
                SirelineIndex.get(helper.getLevel()).remove(m.getUUID());
                m.discard();
            }
        }
        helper.succeed();
    }

    // ---- who may, and on whom ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compulsionNeedsEyeContact(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        Villager v = villager(helper, 6);
        try {
            // the villager looks away
            look(caster, v.getEyePosition());
            look(v, v.getEyePosition().add(v.getEyePosition().subtract(caster.getEyePosition())));
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "compelled a villager looking away");

            // the caster looks away
            Compulsion.clearGuard(caster.getUUID());
            look(v, caster.getEyePosition());
            look(caster, caster.getEyePosition().add(caster.getEyePosition().subtract(v.getEyePosition())));
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "compelled while looking away");

            // 20 degrees off is fine for the villager (30) but not for the caster (15)
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, v);
            caster.setYRot(caster.getYRot() + 20);
            caster.setYHeadRot(caster.getYRot());
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "the caster's 15 degrees were not enforced");
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, v);
            v.setYRot(v.getYRot() + 20);
            v.setYHeadRot(v.getYRot());
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isPresent(), "the villager's 30 degrees were not allowed");

            // too far
            Compulsion.clearGuard(caster.getUUID());
            Compulsion.forgetChoices(caster.getUUID());
            place(helper, v, 9);
            gaze(caster, v);
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "compelled from 5 blocks");
            place(helper, v, 8);
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, v);
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isPresent(), "refused from 4 blocks");
        } finally {
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compulsionNeedsPower(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        Villager v = villager(helper, 6);
        try {
            gaze(caster, v);
            for (Species s : new Species[] {Species.NONE, Species.WITCH, Species.SIPHONER, Species.HERETIC, Species.POTENTIAL, Species.HUNTER_OF_THE_FIVE}) {
                species(caster, s);
                Compulsion.clearGuard(caster.getUUID());
                PlayerInteractEvent.EntityInteract e = click(caster, v);
                helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty() && !e.isCanceled(), s + " could compel");
            }
            for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID}) {
                species(caster, s);
                Compulsion.clearGuard(caster.getUUID());
                click(caster, v);
                helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isPresent(), s + " could not compel");
                Compulsion.forgetChoices(caster.getUUID());
            }
            // a vampire from level 10
            species(caster, Species.NONE);
            helper.assertTrue(VampirismAPI.factionPlayerHandler(caster).setFactionAndLevel(VReference.VAMPIRE_FACTION, 9), "could not make a vampire");
            Compulsion.clearGuard(caster.getUUID());
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "a level 9 vampire could compel");
            helper.assertTrue(VampirismAPI.factionPlayerHandler(caster).setFactionAndLevel(VReference.VAMPIRE_FACTION, 10), "could not raise the level");
            Compulsion.clearGuard(caster.getUUID());
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isPresent(), "a level 10 vampire could not compel");
            // not sneaking, or something in the hand: an ordinary click
            Compulsion.forgetChoices(caster.getUUID());
            Compulsion.clearGuard(caster.getUUID());
            caster.setShiftKeyDown(false);
            PlayerInteractEvent.EntityInteract plain = click(caster, v);
            helper.assertTrue(!plain.isCanceled() && Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "compelled without sneaking");
            caster.setShiftKeyDown(true);
            caster.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
            PlayerInteractEvent.EntityInteract held = click(caster, v);
            helper.assertTrue(!held.isCanceled() && Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "compelled with a full hand");
        } finally {
            caster.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compulsionRefusedOnImmune(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        ServerPlayer player = named(helper, helper.getLevel(), "target");
        Villager v = villager(helper, 6);
        Mob vamp = mob(helper, "vampirism:vampire", 5);
        try {
            IMMUNE.add(v.getUUID());
            IMMUNE.add(vamp.getUUID());
            helper.assertTrue(CompulsionImmunity.isImmune(v), "the test predicate is not registered");
            gaze(caster, v);
            click(caster, v);
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).isEmpty(), "an immune villager was compelled");
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, vamp);
            click(caster, vamp);
            helper.assertTrue(!vamp.hasData(SpeciesRegistries.SIRE) && !vamp.hasData(SpeciesRegistries.COMPELLED_BY), "an immune vampire was compelled");

            // the choice is checked again when it is made
            IMMUNE.remove(v.getUUID());
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, v);
            click(caster, v);
            String token = token(helper, caster, v);
            IMMUNE.add(v.getUUID());
            helper.assertTrue(run(caster, 0, new Capture(), "bloodline compel " + token + " discount") == 0, "the choice ignored a new immunity");
            helper.assertTrue(reputation(v, caster.getUUID()) == 0, "an immune villager gave a discount");

            // players never, whatever the caster
            Compulsion.clearGuard(caster.getUUID());
            place(helper, player, 6);
            gaze(caster, player);
            PlayerInteractEvent.EntityInteract e = click(caster, player);
            helper.assertTrue(CompulsionImmunity.isImmune(player) && !e.isCanceled(), "a player was compelled");
        } finally {
            IMMUNE.remove(v.getUUID());
            IMMUNE.remove(vamp.getUUID());
            cleanUp(caster, player);
            Compulsion.clearGuard(caster.getUUID());
            SirelineIndex.get(helper.getLevel()).remove(vamp.getUUID());
            v.discard();
            vamp.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compulsionHasASpamGuard(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        Villager v = villager(helper, 6);
        try {
            gaze(caster, v);
            click(caster, v);
            String first = token(helper, caster, v);
            click(caster, v);
            helper.assertTrue(first.equals(token(helper, caster, v)), "a second click in the same tick offered a new choice");
            Compulsion.clearGuard(caster.getUUID());
            click(caster, v);
            helper.assertTrue(!first.equals(token(helper, caster, v)), "a click after the guard did nothing");
        } finally {
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unknownChoiceTokenIsRefused(GameTestHelper helper) {
        ServerPlayer caster = caster(helper);
        try {
            Capture out = new Capture();
            helper.assertTrue(run(caster, 0, out, "bloodline compel nonsense discount") == 0, "a made-up token worked");
            helper.assertTrue(out.text().contains("expired"), "no refusal message: " + out.text());
        } finally {
            cleanUp(caster);
        }
        helper.succeed();
    }
}
