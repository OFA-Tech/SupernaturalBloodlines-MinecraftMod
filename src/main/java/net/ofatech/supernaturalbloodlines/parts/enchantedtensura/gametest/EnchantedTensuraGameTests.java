package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.ArrayList;
import java.util.List;
import net.favouriteless.enchanted.api.curses.Curse;
import net.favouriteless.enchanted.api.curses.CurseInstance;
import net.favouriteless.enchanted.api.curses.CurseManager;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.favouriteless.enchanted.common.enchanted.curses.CurseManagerImpl;
import net.favouriteless.enchanted.common.init.EData;
import net.favouriteless.enchanted.common.init.EItems;
import net.favouriteless.enchanted.common.items.component.EDataComponents;
import net.favouriteless.enchanted.common.items.component.EntityRefData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.NbtOps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse.Curses;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse.MagiculeDrainCurse;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse.SkillSealCurse;

/** Curses and their rites. Registered by the part itself, so these only exist when the part is enabled. */
@PrefixGameTestTemplate(false)
public final class EnchantedTensuraGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private EnchantedTensuraGameTests() {
    }

    // ---- helpers ----

    private static <T extends Curse> T applyCurse(ServerPlayer p, Curse.Type<T> type, int strength) {
        CurseManager manager = CurseManager.get();
        // a first application always starts at strength 0 in Enchanted 4.2.7, a second one raises it
        manager.applyCurse(type, p.getUUID(), 0, p.serverLevel());
        manager.applyCurse(type, p.getUUID(), strength, p.serverLevel());
        return curseOf(p, type);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Curse> T curseOf(ServerPlayer p, Curse.Type<T> type) {
        for (CurseInstance i : CurseManager.get().getCurses(p.getUUID(), p.serverLevel())) {
            if (i.getCurse().type() == type) {
                return (T) i.getCurse();
            }
        }
        return null;
    }

    /** Runs Enchanted's own curse tick, which is what calls {@code onRemove}; it only sees players in the level. */
    private static void runCurseManagerTick(ServerLevel level, ServerPlayer p) {
        level.addNewPlayer(p);
        try {
            CurseManagerImpl.INSTANCE.tick(level);
        } finally {
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static double maxMagicule(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
        return a == null ? 0 : a.getValue();
    }

    private static void tickCurse(Curse c, ServerPlayer p, int strength, int times) {
        for (int i = 1; i <= times; i++) {
            c.tick(p, strength, i);
        }
    }

    /** Teaches the player {@code count} more skills; Tensura refuses some of them for a player without a race. */
    private static List<ManasSkill> learn(ServerPlayer p, int count) {
        List<ManasSkill> skills = new ArrayList<>();
        for (ManasSkill s : SkillAPI.getSkillRegistry()) {
            if (skills.size() == count) {
                return skills;
            }
            if (SkillAPI.getSkillsFrom(p).learnSkill(s.createDefaultInstance())) {
                skills.add(s);
            }
        }
        throw new IllegalStateException("could only give the test player " + skills.size() + " of " + count + " skills");
    }

    private static void forgetAll(ServerPlayer p) {
        new ArrayList<>(SkillAPI.getSkillsFrom(p).getLearnedSkills())
                .forEach(i -> SkillAPI.getSkillsFrom(p).forgetSkill(i));
    }

    // Tensura's own ACTIVATE_SKILL listener interrupts first for these test players (no magicules), so firing the
    // event would pass whether or not our seal works; the seal's own listener bodies are called instead
    private static EventResult fireActivate(ServerPlayer p, ResourceLocation skill) {
        ManasSkillInstance inst = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
        return Curses.onActivate(Changeable.of(inst), p);
    }

    // ---- Magicule Drain ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void drainReducesMagicules(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        IExistence ex = TensuraStorages.getExistenceFrom(p);
        ex.setMagicule(1000);
        double start = ex.getMagicule();
        helper.assertTrue(start > 0, "could not give the test player magicules");
        MagiculeDrainCurse curse = applyCurse(p, Curses.MAGICULE_DRAIN, 1);

        tickCurse(curse, p, 1, 19);
        helper.assertTrue(ex.getMagicule() == start, "drained before 20 ticks: " + ex.getMagicule());
        curse.tick(p, 1, 20);
        double expected = start - Math.max(1.0, maxMagicule(p) * 0.005 * 2);
        helper.assertTrue(Math.abs(ex.getMagicule() - expected) < 1e-6,
                "after 20 ticks expected " + expected + " but have " + ex.getMagicule());
        tickCurse(curse, p, 1, 20);
        helper.assertTrue(Math.abs(ex.getMagicule() - (expected - Math.max(1.0, maxMagicule(p) * 0.005 * 2))) < 1e-6,
                "the second 20 ticks did not drain again: " + ex.getMagicule());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neverBelowZero(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        IExistence ex = TensuraStorages.getExistenceFrom(p);
        MagiculeDrainCurse curse = applyCurse(p, Curses.MAGICULE_DRAIN, 2);

        ex.setMagicule(0.25);
        tickCurse(curse, p, 2, 20);
        helper.assertTrue(ex.getMagicule() == 0, "0.25 magicules drained to " + ex.getMagicule());
        tickCurse(curse, p, 2, 100);
        helper.assertTrue(ex.getMagicule() == 0, "an empty pool went to " + ex.getMagicule());
        helper.succeed();
    }

    // ---- Skill Seal ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sealChoiceIsDeterministic(GameTestHelper helper) {
        List<ResourceLocation> a = List.of(ResourceLocation.parse("t:c"), ResourceLocation.parse("t:a"), ResourceLocation.parse("t:b"));
        List<ResourceLocation> b = List.of(ResourceLocation.parse("t:b"), ResourceLocation.parse("t:c"), ResourceLocation.parse("t:a"));
        for (long seed : new long[] {0, 1, 2, -7, Long.MIN_VALUE, Long.MAX_VALUE}) {
            ResourceLocation x = SkillSealCurse.pick(seed, a);
            helper.assertTrue(x != null && a.contains(x), "seed " + seed + " picked " + x);
            helper.assertTrue(x.equals(SkillSealCurse.pick(seed, b)), "the order of the input changed the choice for seed " + seed);
        }
        helper.assertTrue(SkillSealCurse.pick(5, List.of()) == null, "an empty list must pick nothing");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void skillSealBlocksChosenSkill(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        forgetAll(p);
        List<ManasSkill> skills = learn(p, 3);
        SkillSealCurse curse = applyCurse(p, Curses.SKILL_SEAL, 0);
        curse.tick(p, 0, 1);

        ResourceLocation sealed = curse.sealedSkill();
        helper.assertTrue(sealed != null, "no skill was sealed although the player knows 3");
        helper.assertTrue(skills.stream().anyMatch(s -> s.getRegistryName().equals(sealed)), "sealed a skill the player does not know: " + sealed);
        helper.assertTrue(SkillSealCurse.blocks(p, sealed), "blocks() does not report the sealed skill");
        helper.assertTrue(fireActivate(p, sealed).isFalse(), "ACTIVATE_SKILL was not interrupted for " + sealed);
        ManasSkillInstance inst = SkillAPI.getSkillsFrom(p).getSkill(sealed).orElseThrow();
        helper.assertTrue(Curses.onToggle(Changeable.of(inst), p).isFalse(), "toggling on was not interrupted for " + sealed);

        int others = 0;
        for (ManasSkill s : skills) {
            if (!s.getRegistryName().equals(sealed)) {
                others++;
                helper.assertTrue(!SkillSealCurse.blocks(p, s.getRegistryName()), "also blocked " + s.getRegistryName());
                helper.assertTrue(!fireActivate(p, s.getRegistryName()).isFalse(), "the seal also interrupted " + s.getRegistryName());
            }
        }
        helper.assertTrue(others == skills.size() - 1, "expected exactly one sealed skill");

        // the choice sticks while the curse lasts, even when more skills are learned
        learn(p, 1);
        tickCurse(curse, p, 0, 5);
        helper.assertTrue(sealed.equals(curse.sealedSkill()), "the sealed skill changed to " + curse.sealedSkill());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void skillSealTogglesOffTheSealedSkill(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        forgetAll(p);
        learn(p, 3);
        List<ManasSkillInstance> learned = new ArrayList<>(SkillAPI.getSkillsFrom(p).getLearnedSkills());
        learned.forEach(i -> i.setToggled(true));
        SkillSealCurse curse = applyCurse(p, Curses.SKILL_SEAL, 0);
        curse.tick(p, 0, 1);

        ResourceLocation sealed = curse.sealedSkill();
        helper.assertTrue(sealed != null, "no skill was sealed");
        for (ManasSkillInstance i : learned) {
            boolean isSealed = i.getSkillId().equals(sealed);
            helper.assertTrue(i.isToggled() != isSealed,
                    (isSealed ? "the sealed skill is still toggled on: " : "an unsealed skill was toggled off: ") + i.getSkillId());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cursesSurviveSaving(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        forgetAll(p);
        learn(p, 3);
        SkillSealCurse seal = applyCurse(p, Curses.SKILL_SEAL, 0);
        seal.tick(p, 0, 1);
        helper.assertTrue(seal.sealedSkill() != null, "no skill sealed");

        // Enchanted saves and loads curses through this codec
        Curse loaded = Curse.codec().parse(NbtOps.INSTANCE, Curse.codec().encodeStart(NbtOps.INSTANCE, seal).getOrThrow()).getOrThrow();
        helper.assertTrue(loaded instanceof SkillSealCurse s && seal.sealedSkill().equals(s.sealedSkill()),
                "the sealed skill did not survive saving: " + loaded);
        Curse drain = Curse.codec().parse(NbtOps.INSTANCE,
                Curse.codec().encodeStart(NbtOps.INSTANCE, new MagiculeDrainCurse()).getOrThrow()).getOrThrow();
        helper.assertTrue(drain instanceof MagiculeDrainCurse, "magicule drain did not survive saving");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void skillSealWithNoSkillsIsNoop(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        forgetAll(p);
        SkillSealCurse curse = applyCurse(p, Curses.SKILL_SEAL, 0);
        curse.onInitialise(p, 0, 0);
        tickCurse(curse, p, 0, 5);
        helper.assertTrue(curse.sealedSkill() == null, "sealed " + curse.sealedSkill() + " with no skills");
        curse.onRemove(p, 0, 5);

        // a skill learned later is sealed from then on
        ManasSkill later = learn(p, 1).get(0);
        curse.tick(p, 0, 6);
        helper.assertTrue(later.getRegistryName().equals(curse.sealedSkill()), "a skill learned later was not sealed");
        helper.assertTrue(SkillSealCurse.blocks(p, later.getRegistryName()), "the later skill is not blocked");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void removingSkillSealRestoresSkill(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        forgetAll(p);
        learn(p, 3);
        SkillSealCurse curse = applyCurse(p, Curses.SKILL_SEAL, 0);
        curse.tick(p, 0, 1);
        ResourceLocation sealed = curse.sealedSkill();
        helper.assertTrue(sealed != null && fireActivate(p, sealed).isFalse(), "the skill was not sealed to begin with");

        // what Enchanted's remove-curse rite does
        CurseManager.get().removeCurse(Curses.SKILL_SEAL, p.getUUID(), p.serverLevel());
        helper.assertTrue(!SkillSealCurse.blocks(p, sealed), "the skill is still blocked once the curse is marked removed");
        runCurseManagerTick(helper.getLevel(), p);
        helper.assertTrue(curseOf(p, Curses.SKILL_SEAL) == null, "the curse instance is still on the player");
        helper.assertTrue(curse.sealedSkill() == null, "onRemove did not clear the sealed skill");
        helper.assertTrue(!SkillSealCurse.blocks(p, sealed), "the skill is blocked after the curse was removed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sealedSkillGivesFeedback(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        forgetAll(p);
        learn(p, 3);
        SkillSealCurse curse = applyCurse(p, Curses.SKILL_SEAL, 0);
        curse.tick(p, 0, 1);
        ResourceLocation sealed = curse.sealedSkill();
        helper.assertTrue(sealed != null, "no skill sealed");
        helper.assertTrue(!p.received(Curses.SKILL_SEALED), "told the player before any activation was blocked");

        helper.assertTrue(fireActivate(p, sealed).isFalse(), "the sealed skill was not blocked");
        helper.assertTrue(p.received(Curses.SKILL_SEALED), "the player was not told the skill is sealed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cursesIgnoreRemovedPlayers(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        forgetAll(p);
        learn(p, 2);
        IExistence ex = TensuraStorages.getExistenceFrom(p);
        ex.setMagicule(1000);
        MagiculeDrainCurse drain = applyCurse(p, Curses.MAGICULE_DRAIN, 0);
        SkillSealCurse seal = applyCurse(p, Curses.SKILL_SEAL, 0);

        p.setRemoved(Entity.RemovalReason.DISCARDED);
        tickCurse(drain, p, 0, 40);
        seal.tick(p, 0, 1);
        helper.assertTrue(ex.getMagicule() == 1000, "the drain ran on a removed player: " + ex.getMagicule());
        helper.assertTrue(seal.sealedSkill() == null, "the seal chose a skill for a removed player");
        helper.succeed();
    }

    // ---- death ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cursesAreClearedOnDeath(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        forgetAll(p);
        learn(p, 3);
        applyCurse(p, Curses.MAGICULE_DRAIN, 0);
        SkillSealCurse seal = applyCurse(p, Curses.SKILL_SEAL, 0);
        seal.tick(p, 0, 1);
        ResourceLocation sealed = seal.sealedSkill();
        helper.assertTrue(sealed != null, "no skill sealed");

        NeoForge.EVENT_BUS.post(new LivingDeathEvent(p, p.damageSources().generic()));
        helper.assertTrue(!SkillSealCurse.blocks(p, sealed), "the seal still blocks after death");
        runCurseManagerTick(helper.getLevel(), p);
        helper.assertTrue(curseOf(p, Curses.MAGICULE_DRAIN) == null, "magicule drain survived death");
        helper.assertTrue(curseOf(p, Curses.SKILL_SEAL) == null, "skill seal survived death");
        helper.assertTrue(seal.sealedSkill() == null, "onRemove did not run for the seal on death");
        helper.succeed();
    }

    // ---- rites ----

    private static RiteType rite(GameTestHelper helper, String path) {
        return helper.getLevel().registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY)
                .get(ResourceLocation.fromNamespaceAndPath(NS, path));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curseRitesLoad(GameTestHelper helper) {
        for (String path : List.of("curse_magicule_drain", "curse_skill_seal")) {
            RiteType type = rite(helper, path);
            helper.assertTrue(type != null, "rite " + path + " is not in Enchanted's rite registry");
            helper.assertTrue(type.getPower() == 2000, path + " power is " + type.getPower());
            helper.assertTrue(type.getItems().size() == 3, path + " has " + type.getItems().size() + " items");
            helper.assertTrue(type.getItems().stream().anyMatch(s -> s.is(EItems.TAGLOCK_FILLED.get())), path + " needs a filled taglock");
            helper.assertTrue(type.getShapes().size() == 1, path + " should have exactly one circle");
        }
        helper.succeed();
    }

    private static Rite startRite(GameTestHelper helper, String path, ServerPlayer caster, ServerPlayer victim) {
        ItemStack taglock = new ItemStack(EItems.TAGLOCK_FILLED.get());
        taglock.set(EDataComponents.ENTITY_REF.get(), EntityRefData.of(victim.getUUID(), "victim"));
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Rite rite = rite(helper, path).create(helper.getLevel(), pos, caster.getUUID(), List.of(taglock));
        rite.start();
        return rite;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ritesCurseTheTaglockTarget(GameTestHelper helper) {
        ServerPlayer caster = TestPlayers.create(helper);
        ServerPlayer victim = TestPlayers.create(helper);
        forgetAll(victim);
        learn(victim, 3);
        IExistence ex = TensuraStorages.getExistenceFrom(victim);
        ex.setMagicule(1000);
        double start = ex.getMagicule();

        startRite(helper, "curse_magicule_drain", caster, victim);
        MagiculeDrainCurse drain = curseOf(victim, Curses.MAGICULE_DRAIN);
        helper.assertTrue(drain != null, "the magicule drain rite did not curse the taglock target");
        helper.assertTrue(curseOf(caster, Curses.MAGICULE_DRAIN) == null, "the caster was cursed");
        tickCurse(drain, victim, 0, 20);
        helper.assertTrue(ex.getMagicule() < start, "the cursed player did not lose magicules");

        startRite(helper, "curse_skill_seal", caster, victim);
        SkillSealCurse seal = curseOf(victim, Curses.SKILL_SEAL);
        helper.assertTrue(seal != null, "the skill seal rite did not curse the taglock target");
        seal.tick(victim, 0, 1);
        helper.assertTrue(seal.sealedSkill() != null && fireActivate(victim, seal.sealedSkill()).isFalse(),
                "the cursed player can still use the sealed skill");

        // and Enchanted's remove-curse path undoes it
        CurseManager.get().removeCurse(Curses.SKILL_SEAL, victim.getUUID(), victim.serverLevel());
        runCurseManagerTick(helper.getLevel(), victim);
        helper.assertTrue(curseOf(victim, Curses.SKILL_SEAL) == null, "the seal was not removed");
        helper.succeed();
    }
}
