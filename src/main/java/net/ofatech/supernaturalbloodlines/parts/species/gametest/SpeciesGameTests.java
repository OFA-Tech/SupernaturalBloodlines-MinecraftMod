package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesData;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesPart;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome.Action;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.HybridTraits;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteHooks;

/** Shared species plumbing: the context, eligibility and the outcome executor. */
@PrefixGameTestTemplate(false)
public final class SpeciesGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SpeciesGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static Outcome actions(Action... a) {
        return new Outcome(BloodlineState.NONE, List.of(a), null);
    }

    private static BloodlineState species(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).build();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void executorJoinsVampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        OutcomeExecutor.run(p, actions(Action.JOIN_VAMPIRE));
        helper.assertTrue(handler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION, "faction is " + handler(p).getCurrentFaction());
        helper.assertTrue(handler(p).getCurrentLevel() == 1, "level is " + handler(p).getCurrentLevel());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void executorKeepsTheVampireLevel(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
        OutcomeExecutor.run(p, actions(Action.JOIN_VAMPIRE));
        helper.assertTrue(handler(p).getCurrentLevel() == 3, "level is " + handler(p).getCurrentLevel());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void executorLeavesFaction(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 2), "could not make a vampire");
        OutcomeExecutor.run(p, actions(Action.LEAVE_FACTION));
        helper.assertTrue(handler(p).getCurrentFaction() == null, "still in " + handler(p).getCurrentFaction());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void executorRefusalSendsMessage(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        String key = Outcome.REFUSAL_PREFIX + "has_faction";
        OutcomeExecutor.run(p, new Outcome(BloodlineState.NONE, List.of(Action.JOIN_VAMPIRE), key));
        helper.assertTrue(p.received(key), "no chat message for " + key);
        helper.assertTrue(!p.receivedOnActionBar(key), "the refusal went to the action bar, not the chat");
        helper.assertTrue(handler(p).getCurrentFaction() == null, "a refused outcome still joined " + handler(p).getCurrentFaction());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void executorKillsOnKillPlayer(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        OutcomeExecutor.run(p, actions(Action.KILL_PLAYER));
        helper.assertTrue(p.isDeadOrDying(), "the player survived, health " + p.getHealth());
        helper.assertTrue(p.getLastDamageSource() != null && p.getLastDamageSource().is(OutcomeExecutor.FAILED_TRANSITION),
                "the death was not a failed transition: " + p.getLastDamageSource());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killIgnoresTotemOfUndying(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        OutcomeExecutor.run(p, actions(Action.KILL_PLAYER));
        helper.assertTrue(p.isDeadOrDying(), "the totem saved the player");
        helper.assertTrue(p.getMainHandItem().is(Items.TOTEM_OF_UNDYING), "the totem was used up");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killSparesCreativePlayers(GameTestHelper helper) {
        ServerPlayer p = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-creative")) {
            @Override
            public boolean isCreative() {
                return true;
            }
        };
        OutcomeExecutor.run(p, actions(Action.KILL_PLAYER));
        helper.assertTrue(p.isAlive(), "a creative player was killed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginResyncsTheOriginalsRegistry(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        SpeciesData data = SpeciesData.get(p.server);
        int before = data.count();
        try {
            BloodlineAccess.set(p, species(Species.ORIGINAL));
            data.setOriginal(p.getUUID(), false); // the registry lost them
            SpeciesPart.syncOriginal(p);
            helper.assertTrue(data.count() == before + 1, "login did not restore the Original");
            SpeciesPart.syncOriginal(p);
            helper.assertTrue(data.count() == before + 1, "a second sync counted them twice");
        } finally {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.assertTrue(data.count() == before, "the registry kept a non-Original");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void failedTransitionBypassesResistance(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .containsKey(OutcomeExecutor.FAILED_TRANSITION), "damage type failed_transition is missing");
        helper.assertTrue(helper.getLevel().damageSources().source(OutcomeExecutor.FAILED_TRANSITION).is(DamageTypeTags.BYPASSES_RESISTANCE),
                "failed_transition does not bypass Resistance");
        helper.assertTrue(helper.getLevel().damageSources().source(OutcomeExecutor.FAILED_TRANSITION).is(DamageTypeTags.BYPASSES_INVULNERABILITY),
                "failed_transition does not bypass invulnerability (a totem would save the player)");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nonHumanLikeRefusedWhenCheckSaysNo(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        SpeciesEligibility.HumanLikeCheck before = SpeciesEligibility.check();
        SpeciesEligibility.setCheck(x -> false);
        try {
            BloodlineContext ctx = SpeciesContext.of(p, null);
            helper.assertTrue(!ctx.humanLike(), "the context is human-like although the check says no");
            Outcome out = BloodlineAccess.applyEvent(p, BloodlineEvent.RITE_OF_IMMORTALITY, ctx);
            helper.assertTrue(out.refused() && out.refusalKey().equals(Outcome.REFUSAL_PREFIX + "not_human_like"), "outcome was " + out);
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE, "species is " + BloodlineAccess.get(p).species());
            OutcomeExecutor.run(p, out);
            helper.assertTrue(p.received(Outcome.REFUSAL_PREFIX + "not_human_like"), "the player was not told");
        } finally {
            SpeciesEligibility.setCheck(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanLikeByDefault(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineContext ctx = SpeciesContext.of(p, null);
        helper.assertTrue(ctx.humanLike(), "a player is not human-like by default");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void contextReadsFactionConfigAndWerewolves(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        java.util.UUID source = java.util.UUID.randomUUID();
        helper.assertTrue(handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineContext ctx = SpeciesContext.of(p, source);
        helper.assertTrue(ctx.faction() == BaseFaction.VAMPIRE, "faction is " + ctx.faction());
        helper.assertTrue(ctx.bloodSource().equals(source), "blood source is " + ctx.bloodSource());
        helper.assertTrue(ctx.maxOriginals() == 0, "max originals is " + ctx.maxOriginals());
        helper.assertTrue(ctx.transitionTicks() == 24000, "transition ticks are " + ctx.transitionTicks());
        helper.assertTrue(ctx.werewolvesLoaded() == ModList.get().isLoaded("werewolves"), "werewolves flag is " + ctx.werewolvesLoaded());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void doppelgangerRiteOnlyExistsWithWerewolves(GameTestHelper helper) {
        boolean present = net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type(helper, "doppelganger_blood") != null;
        helper.assertTrue(present == ModList.get().isLoaded("werewolves"), "doppelganger_blood rite present: " + present);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void contextCountsOriginals(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        int before = SpeciesContext.of(p, null).originalsInWorld();
        try {
            BloodlineAccess.set(p, species(Species.ORIGINAL));
            helper.assertTrue(SpeciesContext.of(p, null).originalsInWorld() == before + 1, "an Original is not counted");
            BloodlineAccess.set(p, species(Species.ORIGINAL_HYBRID));
            helper.assertTrue(SpeciesContext.of(p, null).originalsInWorld() == before + 1, "an Original hybrid is counted twice or not at all");
            BloodlineAccess.set(p, species(Species.WITCH));
            helper.assertTrue(SpeciesContext.of(p, null).originalsInWorld() == before, "a witch is counted as an Original");
        } finally {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** Runs with and without Werewolves (-PnoWerewolves): the hybrid traits follow the mod, the Original protection does not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridTraitsFollowTheWerewolvesMod(GameTestHelper helper) {
        boolean werewolves = ModList.get().isLoaded("werewolves");
        ServerPlayer p = TestPlayers.create(helper);
        try {
            BloodlineAccess.set(p, species(Species.HYBRID));
            helper.assertTrue(p.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(HybridTraits.MODIFIER_ID) == werewolves,
                    "werewolves=" + werewolves + " but the hybrid modifier is " + p.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(HybridTraits.MODIFIER_ID));
            // a modifier saved while Werewolves was there is stripped on the next sync once it is gone
            p.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(
                    new AttributeModifier(HybridTraits.MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
            HybridTraits.sync(p);
            helper.assertTrue(p.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(HybridTraits.MODIFIER_ID) == werewolves,
                    "the saved hybrid modifier outlived the mod: werewolves=" + werewolves);
            BloodlineAccess.set(p, species(Species.ORIGINAL_HYBRID));
            helper.assertTrue(p.getAttribute(Attributes.MAX_HEALTH).hasModifier(OriginalRules.MODIFIER_ID), "an Original hybrid lost the Original protection");
        } finally {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteFactoryUsesTheSpeciesContext(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        SpeciesEligibility.HumanLikeCheck before = SpeciesEligibility.check();
        SpeciesEligibility.setCheck(x -> false);
        try {
            BloodlineContext ctx = FactionRiteHooks.context(p, BaseFaction.WEREWOLF, 77);
            helper.assertTrue(!ctx.humanLike(), "the rite factory's context is not the species context");
            helper.assertTrue(ctx.faction() == BaseFaction.WEREWOLF && ctx.gameTime() == 77, "faction/time were not passed on: " + ctx);
        } finally {
            SpeciesEligibility.setCheck(before);
        }
        helper.succeed();
    }
}
