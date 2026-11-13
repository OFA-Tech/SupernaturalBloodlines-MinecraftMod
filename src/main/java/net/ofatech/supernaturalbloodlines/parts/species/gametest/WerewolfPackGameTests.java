package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import java.util.List;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Plan 9, task 1: the hidden gene, its awakening, the bound hybrid's lock and the Rite of Lineage. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class WerewolfPackGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final WolfState GENE = WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build();

    private WerewolfPackGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    private static ServerPlayer withGene(ServerPlayer p, Species s) {
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).wolf(GENE).build());
        return p;
    }

    private static boolean werewolf(ServerPlayer p) {
        return handler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION;
    }

    private static void kill(ServerPlayer killer, Entity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent((LivingEntity) victim, killer.damageSources().playerAttack(killer)));
    }

    private static Villager villager(GameTestHelper helper) {
        return helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
    }

    private static void assertBornOmega(GameTestHelper helper, ServerPlayer p) {
        WolfState w = BloodlineAccess.get(p).wolf();
        helper.assertTrue(w.lineage() == WolfState.Lineage.BORN && w.rank() == WolfState.Rank.OMEGA, "wolf layer is " + w);
        helper.assertTrue(werewolf(p), "faction is " + handler(p).getCurrentFaction());
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineageRiteDataLoads(GameTestHelper helper) {
        RiteType reveal = type(helper, "rite_of_lineage_reveal");
        RiteType awaken = type(helper, "rite_of_lineage_awaken");
        helper.assertTrue(reveal != null && reveal.getPower() == 3000, "rite_of_lineage_reveal missing or wrong power");
        helper.assertTrue(awaken != null && awaken.getPower() == 9000, "rite_of_lineage_awaken missing or wrong power");
        helper.succeed();
    }

    // ---- the gene roll ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void geneRolledOnceOnFirstLogin(GameTestHelper helper) {
        ServerPlayer lucky = TestPlayers.createMortal(helper);
        ServerPlayer unlucky = TestPlayers.createMortal(helper);
        ServerPlayer edge = TestPlayers.createMortal(helper);
        SpeciesConfig.overrideLatentGeneChance(0.15);
        try {
            GeneHandler.useRoll(() -> 0.149);
            GeneHandler.onLogin(lucky);
            helper.assertTrue(BloodlineAccess.get(lucky).wolf().geneRolled() && BloodlineAccess.get(lucky).wolf().latentGene(), "a roll under the chance gave no gene");
            // the same player logs in again with a roll that would not give the gene: nothing is rerolled
            GeneHandler.useRoll(() -> 0.99);
            GeneHandler.onLogin(lucky);
            helper.assertTrue(BloodlineAccess.get(lucky).wolf().latentGene(), "the second login rerolled the gene away");

            GeneHandler.onLogin(unlucky);
            helper.assertTrue(BloodlineAccess.get(unlucky).wolf().geneRolled() && !BloodlineAccess.get(unlucky).wolf().latentGene(), "a high roll gave a gene or no roll was recorded");
            GeneHandler.useRoll(() -> 0.0);
            GeneHandler.onLogin(unlucky);
            helper.assertTrue(!BloodlineAccess.get(unlucky).wolf().latentGene(), "the second login rerolled the gene in");

            GeneHandler.useRoll(() -> 0.15); // the chance itself is not enough: the roll has to be below it
            GeneHandler.onLogin(edge);
            helper.assertTrue(!BloodlineAccess.get(edge).wolf().latentGene(), "a roll equal to the chance gave the gene");
        } finally {
            GeneHandler.resetRoll();
            SpeciesConfig.overrideLatentGeneChance(null);
            reset(lucky);
            reset(unlucky);
            reset(edge);
        }
        helper.succeed();
    }

    // ---- the human awakening ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void geneAwakensOnVillagerKill(GameTestHelper helper) {
        ServerPlayer killer = withGene(TestPlayers.createMortal(helper), Species.NONE);
        ServerPlayer playerKiller = withGene(TestPlayers.createMortal(helper), Species.NONE);
        try {
            Villager v = villager(helper);
            v.hurt(killer.damageSources().playerAttack(killer), 1000f); // a real death, not a posted event
            helper.assertTrue(v.isDeadOrDying(), "the villager survived");
            assertBornOmega(helper, killer);
            helper.assertTrue(BloodlineAccess.get(killer).species() == Species.NONE, "species is " + BloodlineAccess.get(killer).species());
            helper.assertTrue(handler(killer).getCurrentLevel() == 1, "level is " + handler(killer).getCurrentLevel());

            kill(playerKiller, TestPlayers.createMortal(helper));
            assertBornOmega(helper, playerKiller);
        } finally {
            reset(killer);
            reset(playerKiller);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void geneStaysLatentForOtherKills(GameTestHelper helper) {
        ServerPlayer noGene = TestPlayers.createMortal(helper);
        ServerPlayer carrier = withGene(TestPlayers.createMortal(helper), Species.NONE);
        try {
            kill(noGene, villager(helper));
            helper.assertTrue(!werewolf(noGene) && BloodlineAccess.get(noGene).wolf().lineage() == WolfState.Lineage.NONE, "a killer without the gene awakened");
            kill(carrier, helper.spawn(EntityType.WANDERING_TRADER, new BlockPos(1, 1, 1)));
            kill(carrier, helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1)));
            helper.assertTrue(!werewolf(carrier) && BloodlineAccess.get(carrier).wolf().latentGene(), "a trader or a zombie awakened the gene");
            kill(carrier, carrier); // killing yourself is no kill
            helper.assertTrue(!werewolf(carrier), "a self kill awakened the gene");
        } finally {
            reset(noGene);
            reset(carrier);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void geneDoesNotAwakenAsHunter(GameTestHelper helper) {
        ServerPlayer hunter = withGene(TestPlayers.createMortal(helper), Species.NONE);
        try {
            handler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            kill(hunter, villager(helper));
            helper.assertTrue(handler(hunter).getCurrentFaction() == VReference.HUNTER_FACTION, "faction is " + handler(hunter).getCurrentFaction());
            WolfState w = BloodlineAccess.get(hunter).wolf();
            helper.assertTrue(w.lineage() == WolfState.Lineage.NONE && w.latentGene(), "the hunter's gene did not stay latent: " + w);
        } finally {
            reset(hunter);
            handler(hunter).leaveFaction(false);
        }
        helper.succeed();
    }

    // ---- the vampire awakening (GeneAwakening, reused) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireWithGeneBecomesBoundHybrid(GameTestHelper helper) {
        ServerPlayer killer = TestPlayers.createMortal(helper);
        try {
            handler(killer).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            withGene(killer, Species.NONE);
            kill(killer, villager(helper));
            BloodlineState s = BloodlineAccess.get(killer);
            helper.assertTrue(s.species() == Species.HYBRID && s.hybridCurseBound(), "species " + s.species() + ", bound " + s.hybridCurseBound());
            assertBornOmega(helper, killer);
        } finally {
            reset(killer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalWithGeneBecomesBoundOriginalHybrid(GameTestHelper helper) {
        ServerPlayer killer = TestPlayers.createMortal(helper);
        try {
            handler(killer).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            withGene(killer, Species.ORIGINAL);
            kill(killer, TestPlayers.createMortal(helper));
            BloodlineState s = BloodlineAccess.get(killer);
            helper.assertTrue(s.species() == Species.ORIGINAL_HYBRID && s.hybridCurseBound(), "species " + s.species() + ", bound " + s.hybridCurseBound());
            assertBornOmega(helper, killer);
        } finally {
            reset(killer);
        }
        helper.succeed();
    }

    // ---- the bound block ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean formCancelled(ServerPlayer p, IAction<?> action, boolean insideOwnTick) {
        if (insideOwnTick) {
            ForcedShift.enterTick(p);
        }
        try {
            var event = new ActionEvent.ActionActivatedEvent(handler(p).getCurrentFactionPlayer().orElseThrow(), (IAction) action, 0, 0);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled();
        } finally {
            ForcedShift.leaveTick(p);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void boundHybridCannotShift(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            handler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).hybridCurseBound(true).wolf(GENE).build());
            for (var form : List.of(ModActions.BEAST_FORM.get(), ModActions.SURVIVAL_FORM.get(), ModActions.HUMAN_FORM.get())) {
                helper.assertTrue(formCancelled(p, form, false), "a bound hybrid shifted into " + form);
            }
            helper.assertTrue(p.receivedOnActionBar("supernaturalbloodlines.hybrid_curse.bound"), "the bound message was not shown");
            helper.assertTrue(formCancelled(p, ModActions.BEAST_FORM.get(), true), "the forced shift was not cancelled");
            helper.assertTrue(!formCancelled(p, ModActions.RAGE.get(), false), "only the forms are bound, not the other werewolf actions");

            // the Rite of the Moon clears the curse, and then the form is theirs again
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().hybridCurseBound(false).build());
            helper.assertTrue(!formCancelled(p, ModActions.BEAST_FORM.get(), false), "an unbound hybrid was stopped");
        } finally {
            reset(p);
            handler(p).leaveFaction(false);
        }
        helper.succeed();
    }

    // ---- the Rite of Lineage ----

    private static List<ItemStack> revealItems() {
        return List.of(new ItemStack(Items.BONE), new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.PAPER));
    }

    private static List<ItemStack> awakenItems() {
        return List.of(new ItemStack(SpeciesRegistries.MOONSTONE.get()), new ItemStack(Items.BONE, 3));
    }

    /** Someone who is not on the circle: the target is the only one who may be told. */
    private static RecordingPlayer bystander(GameTestHelper helper) {
        RecordingPlayer b = RecordingPlayer.create(helper);
        b.setPos(Vec3.atBottomCenterOf(centre(helper)).add(6, 1, 0));
        helper.getLevel().addNewPlayer(b);
        return b;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteRevealsGene(GameTestHelper helper) {
        RecordingPlayer target = RecordingPlayer.create(helper);
        RecordingPlayer without = RecordingPlayer.create(helper);
        RecordingPlayer watcher = bystander(helper);
        place(helper, target);
        try {
            BloodlineAccess.set(target, BloodlineState.NONE.toBuilder().wolf(GENE).build());
            start(helper, "rite_of_lineage_reveal", target, revealItems());
            helper.assertTrue(target.received("supernaturalbloodlines.lineage.gene_yes"), "the target was not told about the gene");
            helper.assertTrue(!target.received("supernaturalbloodlines.lineage.gene_no") && !watcher.received("supernaturalbloodlines.lineage.gene_yes"),
                    "the result went to the wrong player");
            helper.assertTrue(!werewolf(target) && BloodlineAccess.get(target).wolf().equals(GENE), "revealing changed the target: " + BloodlineAccess.get(target).wolf());

            remove(helper, target);
            place(helper, without);
            BloodlineAccess.set(without, BloodlineState.NONE);
            start(helper, "rite_of_lineage_reveal", without, revealItems());
            helper.assertTrue(without.received("supernaturalbloodlines.lineage.gene_no") && !without.received("supernaturalbloodlines.lineage.gene_yes"),
                    "a player without the gene was not told so");
            helper.assertTrue(!watcher.received("supernaturalbloodlines.lineage.gene_no"), "the bystander heard the result");
        } finally {
            remove(helper, target);
            remove(helper, without);
            remove(helper, watcher);
            reset(target);
            reset(without);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteAwakensWithoutGene(GameTestHelper helper) {
        RecordingPlayer target = RecordingPlayer.create(helper);
        place(helper, target);
        try {
            BloodlineAccess.set(target, BloodlineState.NONE);
            start(helper, "rite_of_lineage_awaken", target, awakenItems());
            assertBornOmega(helper, target);
            WolfState w = BloodlineAccess.get(target).wolf();
            helper.assertTrue(w.latentGene() && w.geneRolled(), "the rite did not give the gene: " + w);
            helper.assertTrue(BloodlineAccess.get(target).species() == Species.NONE && handler(target).getCurrentLevel() == 1, "species/level are wrong");
        } finally {
            remove(helper, target);
            reset(target);
            handler(target).leaveFaction(false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteAwakensVampiresAsBoundHybrids(GameTestHelper helper) {
        for (Species s : List.of(Species.NONE, Species.ORIGINAL)) {
            RecordingPlayer target = RecordingPlayer.create(helper);
            place(helper, target);
            try {
                handler(target).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
                BloodlineAccess.set(target, BloodlineState.NONE.toBuilder().species(s).build());
                start(helper, "rite_of_lineage_awaken", target, awakenItems());
                BloodlineState after = BloodlineAccess.get(target);
                Species hybrid = s == Species.ORIGINAL ? Species.ORIGINAL_HYBRID : Species.HYBRID;
                helper.assertTrue(after.species() == hybrid && after.hybridCurseBound(), s + " became " + after.species() + ", bound " + after.hybridCurseBound());
                helper.assertTrue(after.wolf().latentGene(), s + " got no gene");
                assertBornOmega(helper, target);
            } finally {
                remove(helper, target);
                reset(target);
                handler(target).leaveFaction(false);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteRefusesHuntersAndRefunds(GameTestHelper helper) {
        RecordingPlayer target = RecordingPlayer.create(helper);
        place(helper, target);
        try {
            handler(target).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            BloodlineAccess.set(target, BloodlineState.NONE);
            start(helper, "rite_of_lineage_awaken", target, awakenItems());
            helper.assertTrue(target.receivedOnActionBar("supernaturalbloodlines.refusal.hunter_lineage"), "the caster was not told hunters are refused");
            helper.assertTrue(handler(target).getCurrentFaction() == VReference.HUNTER_FACTION && BloodlineAccess.get(target).equals(BloodlineState.NONE),
                    "the refused rite changed the hunter");
            helper.assertTrue(dropped(helper, "minecraft:bone") == 3, "the ingredients were not given back, bones: " + dropped(helper, "minecraft:bone"));
        } finally {
            remove(helper, target);
            reset(target);
            handler(target).leaveFaction(false);
        }
        helper.succeed();
    }
}
