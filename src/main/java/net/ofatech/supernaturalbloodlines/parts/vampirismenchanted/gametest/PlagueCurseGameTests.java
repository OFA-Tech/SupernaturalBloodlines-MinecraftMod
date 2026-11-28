package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import java.util.ArrayList;
import java.util.List;
import net.favouriteless.enchanted.api.curses.Curse;
import net.favouriteless.enchanted.api.curses.CurseInstance;
import net.favouriteless.enchanted.api.curses.CurseManager;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.favouriteless.enchanted.common.init.EData;
import net.favouriteless.enchanted.common.init.EItems;
import net.favouriteless.enchanted.common.items.component.EDataComponents;
import net.favouriteless.enchanted.common.items.component.EntityRefData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.death.DamageProbe;
import net.ofatech.supernaturalbloodlines.core.network.HallucinationPayload;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismEnchantedConfig;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.curse.InsanityCurse;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.curse.PlagueCurses;

/** The Curse of Insanity and the Curse of Susceptibility (R320 to R324): the damage, the hallucinations, the death, the rites and the settings. */
@PrefixGameTestTemplate(false)
public final class PlagueCurseGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private PlagueCurseGameTests() {
    }

    private static <T extends Curse> T apply(ServerPlayer p, Curse.Type<T> type, int strength) {
        CurseManager manager = CurseManager.get();
        // a first application always starts at strength 0 in Enchanted 4.2.7, a second one raises it
        manager.applyCurse(type, p.getUUID(), 0, p.serverLevel());
        manager.applyCurse(type, p.getUUID(), strength, p.serverLevel());
        return active(p, type);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Curse> T active(ServerPlayer p, Curse.Type<T> type) {
        for (CurseInstance i : CurseManager.get().getCurses(p.getUUID(), p.serverLevel())) {
            if (!i.isRemoved() && i.getCurse().type() == type) {
                return (T) i.getCurse();
            }
        }
        return null;
    }

    private static int count(ServerPlayer p, Curse.Type<?> type) {
        int n = 0;
        for (CurseInstance i : CurseManager.get().getCurses(p.getUUID(), p.serverLevel())) {
            if (!i.isRemoved() && i.getCurse().type() == type) {
                n++;
            }
        }
        return n;
    }

    private static void lift(ServerPlayer p) {
        CurseManager.get().removeCurse(PlagueCurses.INSANITY, p.getUUID(), p.serverLevel());
        CurseManager.get().removeCurse(PlagueCurses.SUSCEPTIBILITY, p.getUUID(), p.serverLevel());
        VampirismEnchantedConfig.override("insanity_enabled", null);
        VampirismEnchantedConfig.override("insanity_interval_seconds", null);
        VampirismEnchantedConfig.override("susceptibility_enabled", null);
        VampirismEnchantedConfig.override("susceptibility_extra_percent", null);
        VampirismEnchantedConfig.override("susceptibility_max_percent", null);
        HallucinationPayload.overrideSpy(null);
    }

    /** How much of a 4 point blow the player lost. */
    private static float taken(ServerPlayer p, DamageSource source) {
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0; // a second blow within half a second of the last would be ignored
        float before = p.getHealth();
        p.hurt(source, 4f);
        return before - p.getHealth();
    }

    // ---- susceptibility ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void susceptibilityRaisesTheBlowByTheLevelAndAPlainPlayerTakesItAsIs(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            DamageSource hit = helper.getLevel().damageSources().generic();
            helper.assertTrue(Math.abs(taken(p, hit) - 4f) < 1e-3, "control: an uncursed player did not take 4: " + taken(p, hit));
            apply(p, PlagueCurses.SUSCEPTIBILITY, 0);
            helper.assertTrue(Math.abs(taken(p, hit) - 5f) < 1e-3, "strength 0 is not +25%: " + taken(p, hit));
            apply(p, PlagueCurses.SUSCEPTIBILITY, 1);
            helper.assertTrue(Math.abs(taken(p, hit) - 6f) < 1e-3, "strength 1 is not +50%: " + taken(p, hit));
            apply(p, PlagueCurses.SUSCEPTIBILITY, 2);
            helper.assertTrue(Math.abs(taken(p, hit) - 7f) < 1e-3, "strength 2 is not +75%: " + taken(p, hit));
            VampirismEnchantedConfig.override("susceptibility_max_percent", 150);
            helper.assertTrue(Math.abs(taken(p, hit) - 6f) < 1e-3, "the cap of 150% did not hold: " + taken(p, hit));
        } finally {
            lift(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void susceptibilityNeverRaisesAProbeAKillOrAnOffSwitch(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            apply(p, PlagueCurses.SUSCEPTIBILITY, 1);
            float probe = DamageProbe.run(() -> taken(p, helper.getLevel().damageSources().generic()));
            helper.assertTrue(Math.abs(probe - 4f) < 1e-3, "a probe was raised: " + probe);
            helper.assertTrue(Math.abs(taken(p, helper.getLevel().damageSources().genericKill()) - 4f) < 1e-3, "a blow that bypasses invulnerability was raised");
            VampirismEnchantedConfig.override("susceptibility_enabled", false);
            helper.assertTrue(Math.abs(taken(p, helper.getLevel().damageSources().generic()) - 4f) < 1e-3, "susceptibility_enabled = false still raised the blow");
            VampirismEnchantedConfig.override("susceptibility_enabled", null);
            VampirismEnchantedConfig.override("susceptibility_extra_percent", 0);
            helper.assertTrue(Math.abs(taken(p, helper.getLevel().damageSources().generic()) - 4f) < 1e-3, "an extra of 0% still raised the blow");
        } finally {
            lift(p);
        }
        helper.succeed();
    }

    // ---- insanity ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void insanitySendsAHallucinationEveryIntervalAndNotBefore(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<HallucinationPayload.Kind> seen = new ArrayList<>();
        try {
            HallucinationPayload.overrideSpy((who, kind) -> seen.add(kind));
            EnchantedCursesConfig.override("insanity_interval_seconds", 1);
            InsanityCurse curse = apply(p, PlagueCurses.INSANITY, 0);
            for (int i = 1; i <= 12; i++) {
                curse.tick(p, 0, i);
            }
            helper.assertTrue(seen.isEmpty(), "a hallucination came before the shortest delay (13 ticks): " + seen.size());
            for (int i = 13; i <= 27; i++) {
                curse.tick(p, 0, i);
            }
            helper.assertTrue(seen.size() == 1, "no hallucination after the longest delay (26 ticks): " + seen.size());
            for (int i = 28; i <= 54; i++) {
                curse.tick(p, 0, i);
            }
            helper.assertTrue(seen.size() == 2, "the second one did not come: " + seen.size());
            helper.assertTrue(HallucinationPayload.Kind.valueOf(seen.get(0).name()) != null, "not a kind of ours");
        } finally {
            lift(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void insanityOffSendsNothingAndAHigherStrengthSendsMore(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<HallucinationPayload.Kind> seen = new ArrayList<>();
        try {
            HallucinationPayload.overrideSpy((who, kind) -> seen.add(kind));
            EnchantedCursesConfig.override("insanity_interval_seconds", 1);
            VampirismEnchantedConfig.override("insanity_enabled", false);
            InsanityCurse curse = apply(p, PlagueCurses.INSANITY, 0);
            for (int i = 1; i <= 200; i++) {
                curse.tick(p, 0, i);
            }
            helper.assertTrue(seen.isEmpty(), "insanity_enabled = false still sent: " + seen.size());
            VampirismEnchantedConfig.override("insanity_enabled", null);
            List<HallucinationPayload.Kind> weak = new ArrayList<>();
            for (int i = 1; i <= 400; i++) {
                curse.tick(p, 0, i);
            }
            weak.addAll(seen);
            seen.clear();
            InsanityCurse strong = new InsanityCurse();
            for (int i = 1; i <= 400; i++) {
                strong.tick(p, 2, i);
            }
            helper.assertTrue(seen.size() > weak.size(), "strength 2 (" + seen.size() + ") did not send more than strength 0 (" + weak.size() + ")");
        } finally {
            lift(p);
        }
        helper.succeed();
    }

    // ---- both ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothCursesEndAtDeathAndDoNotStack(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            apply(p, PlagueCurses.INSANITY, 0);
            apply(p, PlagueCurses.INSANITY, 1);
            apply(p, PlagueCurses.SUSCEPTIBILITY, 0);
            apply(p, PlagueCurses.SUSCEPTIBILITY, 1);
            helper.assertTrue(count(p, PlagueCurses.INSANITY) == 1 && count(p, PlagueCurses.SUSCEPTIBILITY) == 1, "a second application stacked a second curse");
            CommonHooks.onLivingDeath(p, helper.getLevel().damageSources().generic());
            helper.assertTrue(count(p, PlagueCurses.INSANITY) == 0 && count(p, PlagueCurses.SUSCEPTIBILITY) == 0, "a curse survived the death");
        } finally {
            lift(p);
        }
        helper.succeed();
    }

    // ---- the rites ----

    private static RiteType rite(GameTestHelper helper, String path) {
        return helper.getLevel().registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY).get(ResourceLocation.fromNamespaceAndPath(NS, path));
    }

    private static void cast(GameTestHelper helper, String path, ServerPlayer caster, ServerPlayer victim) {
        ItemStack taglock = new ItemStack(EItems.TAGLOCK_FILLED.get());
        taglock.set(EDataComponents.ENTITY_REF.get(), EntityRefData.of(victim.getUUID(), "victim"));
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Rite rite = rite(helper, path).create(helper.getLevel(), pos, caster.getUUID(), List.of(taglock));
        rite.start();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRitesLoadAndCurseTheTaglockOwnerOnly(GameTestHelper helper) {
        ServerPlayer caster = TestPlayers.create(helper);
        ServerPlayer victim = TestPlayers.create(helper);
        try {
            for (String path : List.of("curse_insanity", "curse_susceptibility")) {
                RiteType type = rite(helper, path);
                helper.assertTrue(type != null, "rite " + path + " is not in Enchanted's rite registry");
                helper.assertTrue(type.getItems().stream().anyMatch(s -> s.is(EItems.TAGLOCK_FILLED.get())), path + " needs a filled taglock");
                helper.assertTrue(type.getShapes().size() == 1, path + " should have exactly one circle");
            }
            cast(helper, "curse_insanity", caster, victim);
            helper.assertTrue(active(victim, PlagueCurses.INSANITY) != null, "the rite did not curse the taglock's owner");
            helper.assertTrue(active(caster, PlagueCurses.INSANITY) == null, "the caster was cursed");
            cast(helper, "curse_susceptibility", caster, victim);
            helper.assertTrue(active(victim, PlagueCurses.SUSCEPTIBILITY) != null, "the second rite did not curse the taglock's owner");
            helper.assertTrue(active(caster, PlagueCurses.SUSCEPTIBILITY) == null, "the caster was cursed by the second");
        } finally {
            lift(caster);
            lift(victim);
        }
        helper.succeed();
    }
}
