package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.werewolves.api.WReference;
import java.util.List;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.HybridTraits;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Original hybrids: the Rite of the Moon, the lineage awakening, the gene kill (R24, R35). Registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class OriginalHybridGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long FULL_MOON_CYCLE = 24000L * 8;
    private static final String NEEDS_FULL_MOON = "supernaturalbloodlines.rite.needs_full_moon";
    private static final WolfState GENE = WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build();

    private OriginalHybridGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static BloodlineState state(Species s, boolean gene) {
        return BloodlineState.NONE.toBuilder().species(s).wolf(gene ? GENE : WolfState.NONE).build();
    }

    private static ServerPlayer vampire(ServerPlayer p, Species s, boolean gene) {
        handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
        BloodlineAccess.set(p, state(s, gene));
        return p;
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    private static boolean werewolf(ServerPlayer p) {
        return handler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION;
    }

    private static boolean has(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, ResourceLocation id) {
        return p.getAttribute(a).hasModifier(id);
    }

    private static void atFullMoon(GameTestHelper helper, boolean full, Runnable body) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, FULL_MOON_CYCLE);
        // a full moon rite needs the night of the full moon (plan 10), not just the day: 18000 is inside Werewolves' window
        level.setDayTime(full ? base + 18000L : base + 24000L * 4);
        try {
            helper.assertTrue((level.getMoonPhase() == 0) == full, "could not set the moon, phase is " + level.getMoonPhase());
            body.run();
        } finally {
            level.setDayTime(before);
        }
    }

    /** Plan 10: the full-moon rites need the night, so noon of a full-moon day is refused with the night message, and the night is not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRitesRefusedAtFullMoonNoon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, FULL_MOON_CYCLE);
        try {
            for (String rite : List.of("rite_of_the_moon", "immortality", "silver_moon")) {
                for (long timeOfDay : new long[] {6000L, 18000L}) {
                    RecordingPlayer caster = RecordingPlayer.create(helper);
                    place(helper, caster);
                    try {
                        level.setDayTime(base + timeOfDay);
                        helper.assertTrue(level.getMoonPhase() == 0, "could not set the moon, phase is " + level.getMoonPhase());
                        start(helper, rite, caster, List.of());
                        boolean told = caster.receivedOnActionBar(NEEDS_FULL_MOON);
                        helper.assertTrue(told == (timeOfDay == 6000L), rite + " at " + timeOfDay + ": the night message was " + (told ? "sent" : "not sent"));
                    } finally {
                        remove(helper, caster);
                    }
                }
            }
        } finally {
            level.setDayTime(before);
        }
        helper.succeed();
    }

    private static List<ItemStack> moonItems() {
        return List.of(new ItemStack(SpeciesRegistries.MOONSTONE.get()), new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get()));
    }

    /** What an Original hybrid is: hybrid body, Original protection, our downed state for a lethal blow. */
    private static void assertOriginalHybrid(GameTestHelper helper, ServerPlayer p, boolean bound) {
        BloodlineState s = BloodlineAccess.get(p);
        helper.assertTrue(s.species() == Species.ORIGINAL_HYBRID, "species is " + s.species());
        helper.assertTrue(s.hybridCurseBound() == bound, "hybridCurseBound is " + s.hybridCurseBound());
        helper.assertTrue(s.wolf().lineage() == WolfState.Lineage.BORN && s.wolf().rank() == WolfState.Rank.OMEGA, "wolf layer is " + s.wolf());
        helper.assertTrue(werewolf(p), "faction is " + handler(p).getCurrentFaction());
        helper.assertTrue(has(p, Attributes.MOVEMENT_SPEED, HybridTraits.MODIFIER_ID) && has(p, Attributes.ATTACK_DAMAGE, HybridTraits.MODIFIER_ID),
                "no hybrid traits");
        helper.assertTrue(has(p, Attributes.MAX_HEALTH, OriginalRules.MODIFIER_ID) && has(p, Attributes.ATTACK_DAMAGE, OriginalRules.MODIFIER_ID),
                "no Original protection");
    }

    private static void assertGoesDownNotDead(GameTestHelper helper, ServerPlayer p) {
        p.hurt(p.damageSources().generic(), 1000f);
        helper.assertTrue(!p.isDeadOrDying() && Downed.isDowned(p), "a lethal blow did not leave the Original hybrid downed, health " + p.getHealth());
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRiteDataLoads(GameTestHelper helper) {
        RiteType rite = type(helper, "rite_of_the_moon");
        helper.assertTrue(rite != null && rite.getPower() == 15000, "rite_of_the_moon missing or wrong power");
        helper.assertTrue(rite.getItems().size() == 2 && rite.getItems().stream().anyMatch(i -> i.is(SpeciesRegistries.MOONSTONE.get()))
                && rite.getItems().stream().anyMatch(i -> i.is(SpeciesRegistries.ORIGINAL_BLOOD.get())), "wrong ingredients: " + rite.getItems());
        helper.assertTrue(rite.getEntities().size() == 2 && rite.getEntities().contains(EntityType.WOLF) && rite.getEntities().contains(EntityType.VILLAGER),
                "wrong sacrifices: " + rite.getEntities());
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NS, "moonstone")).isPresent(),
                "the moonstone recipe is missing");
        helper.succeed();
    }

    // ---- Rite of the Moon ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalWithGeneCompletingMoonRiteBecomesOriginalHybrid(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            vampire(caster, Species.ORIGINAL, true);
            atFullMoon(helper, true, () -> start(helper, "rite_of_the_moon", caster, moonItems()));
            assertOriginalHybrid(helper, caster, false);
        } finally {
            remove(helper, caster);
            reset(caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRiteRefusedWithoutGene(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            vampire(caster, Species.ORIGINAL, false);
            atFullMoon(helper, true, () -> start(helper, "rite_of_the_moon", caster, moonItems()));
            helper.assertTrue(caster.receivedOnActionBar("supernaturalbloodlines.refusal.no_gene"), "no no_gene refusal was sent");
            helper.assertTrue(BloodlineAccess.get(caster).species() == Species.ORIGINAL, "species is " + BloodlineAccess.get(caster).species());
            helper.assertTrue(handler(caster).getCurrentFaction() == VReference.VAMPIRE_FACTION, "faction is " + handler(caster).getCurrentFaction());
        } finally {
            remove(helper, caster);
            reset(caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRiteNeedsAFullMoon(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            vampire(caster, Species.ORIGINAL, true);
            atFullMoon(helper, false, () -> start(helper, "rite_of_the_moon", caster, moonItems()));
            helper.assertTrue(caster.receivedOnActionBar("supernaturalbloodlines.rite.needs_full_moon"), "the caster was not told it needs a full moon");
            helper.assertTrue(BloodlineAccess.get(caster).species() == Species.ORIGINAL, "species is " + BloodlineAccess.get(caster).species());
        } finally {
            remove(helper, caster);
            reset(caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRiteUnbindsBoundHybrid(GameTestHelper helper) {
        for (Species s : List.of(Species.ORIGINAL_HYBRID, Species.HYBRID)) {
            RecordingPlayer caster = RecordingPlayer.create(helper);
            place(helper, caster);
            try {
                handler(caster).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
                BloodlineAccess.set(caster, state(s, true).toBuilder().hybridCurseBound(true).build());
                atFullMoon(helper, true, () -> start(helper, "rite_of_the_moon", caster, moonItems()));
                BloodlineState after = BloodlineAccess.get(caster);
                helper.assertTrue(after.species() == s && !after.hybridCurseBound(), s + " is " + after.species() + ", bound " + after.hybridCurseBound());
                helper.assertTrue(werewolf(caster), s + " left the werewolves: " + handler(caster).getCurrentFaction());
                helper.assertTrue(!caster.receivedOnActionBar("supernaturalbloodlines.rite.wrong_faction"), s + " was refused by faction");
            } finally {
                remove(helper, caster);
                reset(caster);
            }
        }
        helper.succeed();
    }

    // ---- Rite of Lineage (the rule path; the rite itself is plan 9's) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineageAwakenOnOriginalGivesBoundOriginalHybrid(GameTestHelper helper) {
        ServerPlayer p = vampire(TestPlayers.createMortal(helper), Species.ORIGINAL, false);
        try {
            OutcomeExecutor.run(p, BloodlineAccess.applyEvent(p, BloodlineEvent.RITE_OF_LINEAGE_AWAKEN, SpeciesContext.of(p, null)));
            assertOriginalHybrid(helper, p, true);
            helper.assertTrue(BloodlineAccess.get(p).wolf().latentGene(), "the lineage rite did not give the gene");
            assertGoesDownNotDead(helper, p);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalHybridKeepsBothModifiersApart(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            handler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
            // old enough for the full +2 attack damage (turnedAt 1, far in the past)
            BloodlineAccess.set(p, state(Species.ORIGINAL_HYBRID, false).toBuilder().turnedAt(1).build());
            HybridTraits.sync(p, 1 + HybridTraits.AGE_TICKS);
            helper.assertTrue(p.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().stream()
                            .filter(m -> m.id().equals(HybridTraits.MODIFIER_ID) || m.id().equals(OriginalRules.MODIFIER_ID)).count() == 2,
                    "the two attack modifiers share an id");
            // Original's +20% is multiplied on top of everything else (base, the werewolf faction, the hybrid +2)
            double with = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            p.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(OriginalRules.MODIFIER_ID);
            double without = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            OriginalRules.sync(p);
            helper.assertTrue(without > 2.0 && Math.abs(with - without * 1.2) < 1e-6, "attack damage is " + with + ", without the Original bonus " + without);
            helper.assertTrue(p.getMaxHealth() == 24f, "max health is " + p.getMaxHealth());
            assertGoesDownNotDead(helper, p);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    // ---- gene kill (R24) ----

    private static void kill(ServerPlayer killer, Entity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent((net.minecraft.world.entity.LivingEntity) victim, killer.damageSources().playerAttack(killer)));
    }

    private static Villager villager(GameTestHelper helper) {
        return helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalWithGeneKillingAVillagerBecomesBoundOriginalHybrid(GameTestHelper helper) {
        ServerPlayer killer = vampire(TestPlayers.createMortal(helper), Species.ORIGINAL, true);
        try {
            kill(killer, villager(helper));
            assertOriginalHybrid(helper, killer, true);
        } finally {
            reset(killer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireWithGeneKillingAPlayerBecomesBoundHybrid(GameTestHelper helper) {
        ServerPlayer killer = vampire(TestPlayers.createMortal(helper), Species.NONE, true);
        ServerPlayer victim = TestPlayers.createMortal(helper);
        try {
            kill(killer, victim);
            BloodlineState s = BloodlineAccess.get(killer);
            helper.assertTrue(s.species() == Species.HYBRID && s.hybridCurseBound(), "killer is " + s.species() + ", bound " + s.hybridCurseBound());
            helper.assertTrue(werewolf(killer), "faction is " + handler(killer).getCurrentFaction());
        } finally {
            reset(killer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void realVillagerKillAwakensToo(GameTestHelper helper) {
        ServerPlayer killer = vampire(TestPlayers.createMortal(helper), Species.ORIGINAL, true);
        try {
            Villager v = villager(helper);
            v.hurt(killer.damageSources().playerAttack(killer), 1000f);
            helper.assertTrue(v.isDeadOrDying(), "the villager survived");
            helper.assertTrue(BloodlineAccess.get(killer).species() == Species.ORIGINAL_HYBRID, "killer is " + BloodlineAccess.get(killer).species());
        } finally {
            reset(killer);
        }
        helper.succeed();
    }

    private static int werewolfCap() {
        return WReference.WEREWOLF_FACTION.getHighestReachableLevel();
    }

    private static int vampireCap() {
        return VReference.VAMPIRE_FACTION.getHighestReachableLevel();
    }

    private static ServerPlayer vampireAt(GameTestHelper helper, Species s, boolean gene, int level) {
        ServerPlayer p = vampire(TestPlayers.createMortal(helper), s, gene);
        handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, level);
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void geneKillCarriesTheVampireLevelOver(GameTestHelper helper) {
        ServerPlayer low = vampireAt(helper, Species.ORIGINAL, true, 3);
        ServerPlayer top = vampireAt(helper, Species.NONE, true, vampireCap());
        try {
            kill(low, villager(helper));
            helper.assertTrue(werewolf(low) && handler(low).getCurrentLevel() == Math.max(1, Math.min(3, werewolfCap())),
                    "level is " + handler(low).getCurrentLevel());
            kill(top, villager(helper));
            helper.assertTrue(werewolf(top) && handler(top).getCurrentLevel() == Math.max(1, Math.min(vampireCap(), werewolfCap())),
                    "capped level is " + handler(top).getCurrentLevel() + " (werewolf cap " + werewolfCap() + ")");
        } finally {
            reset(low);
            reset(top);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRiteCarriesTheVampireLevelOver(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            vampire(caster, Species.ORIGINAL, true);
            handler(caster).setFactionAndLevel(VReference.VAMPIRE_FACTION, 4);
            atFullMoon(helper, true, () -> start(helper, "rite_of_the_moon", caster, moonItems()));
            helper.assertTrue(werewolf(caster) && handler(caster).getCurrentLevel() == Math.max(1, Math.min(4, werewolfCap())),
                    "level is " + handler(caster).getCurrentLevel());
        } finally {
            remove(helper, caster);
            reset(caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverMoonCarriesTheWerewolfLevelOver(GameTestHelper helper) {
        ServerPlayer low = TestPlayers.createMortal(helper);
        ServerPlayer top = TestPlayers.createMortal(helper);
        try {
            for (var entry : List.of(java.util.Map.entry(low, 2), java.util.Map.entry(top, werewolfCap()))) {
                ServerPlayer p = entry.getKey();
                handler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, entry.getValue());
                BloodlineAccess.set(p, state(Species.HYBRID, false));
                OutcomeExecutor.run(p, BloodlineAccess.applyEvent(p, BloodlineEvent.RITE_OF_THE_SILVER_MOON, SpeciesContext.of(p, null)));
                int expected = Math.max(1, Math.min(entry.getValue(), vampireCap()));
                helper.assertTrue(handler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION && handler(p).getCurrentLevel() == expected,
                        "faction " + handler(p).getCurrentFaction() + " level " + handler(p).getCurrentLevel() + ", expected " + expected);
            }
        } finally {
            reset(low);
            reset(top);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void geneKillIgnoredWithoutGeneOrWithOtherVictims(GameTestHelper helper) {
        ServerPlayer noGene = vampire(TestPlayers.createMortal(helper), Species.ORIGINAL, false);
        ServerPlayer wrongVictim = vampire(TestPlayers.createMortal(helper), Species.ORIGINAL, true);
        ServerPlayer heretic = vampire(TestPlayers.createMortal(helper), Species.HERETIC, true);
        try {
            // a human with the gene awakens too, but that is plan 9's GeneHandler (WerewolfPackGameTests), not this rule
            kill(wrongVictim, helper.spawn(EntityType.WANDERING_TRADER, new BlockPos(1, 1, 1)));
            helper.assertTrue(BloodlineAccess.get(wrongVictim).species() == Species.ORIGINAL && !werewolf(wrongVictim), "killing a wandering trader awakened");
            kill(noGene, villager(helper));
            helper.assertTrue(BloodlineAccess.get(noGene).species() == Species.ORIGINAL && !werewolf(noGene), "a killer without the gene awakened");
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            kill(wrongVictim, zombie);
            helper.assertTrue(BloodlineAccess.get(wrongVictim).species() == Species.ORIGINAL && !werewolf(wrongVictim), "killing a zombie awakened");
            kill(heretic, villager(helper));
            helper.assertTrue(BloodlineAccess.get(heretic).species() == Species.HERETIC && !werewolf(heretic), "a heretic awakened");
        } finally {
            reset(noGene);
            reset(wrongVictim);
            reset(heretic);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cancelledDeathsDoNotAwaken(GameTestHelper helper) {
        ServerPlayer killer = vampire(TestPlayers.createMortal(helper), Species.ORIGINAL, true);
        ServerPlayer duelist = vampire(TestPlayers.createMortal(helper), Species.ORIGINAL, true);
        ServerPlayer downedVictim = TestPlayers.createDuelist(helper);
        try {
            Villager v = villager(helper);
            LivingDeathEvent cancelled = new LivingDeathEvent(v, killer.damageSources().playerAttack(killer));
            cancelled.setCanceled(true);
            NeoForge.EVENT_BUS.post(cancelled);
            helper.assertTrue(BloodlineAccess.get(killer).species() == Species.ORIGINAL, "a cancelled death awakened the killer");

            // the real thing: an Original hybrid goes down instead of dying, and that is no kill
            handler(downedVictim).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
            BloodlineAccess.set(downedVictim, state(Species.ORIGINAL_HYBRID, false));
            net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
            boolean pvp = server.isPvpAllowed();
            server.setPvpAllowed(true);
            try {
                downedVictim.hurt(duelist.damageSources().playerAttack(duelist), 1000f);
            } finally {
                server.setPvpAllowed(pvp);
            }
            helper.assertTrue(Downed.isDowned(downedVictim), "the victim did not go down");
            helper.assertTrue(BloodlineAccess.get(duelist).species() == Species.ORIGINAL, "a downing awakened the attacker");
        } finally {
            reset(killer);
            reset(duelist);
            reset(downedVictim);
        }
        helper.succeed();
    }
}
