package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SiresBlood;

/** The Rite of Severance and Sire's Blood (R101b). What a severed bond changes is in {@link SeveranceGameTests}. */
@PrefixGameTestTemplate(false)
public final class SeveranceRiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String RITE = "rite_of_severance";
    private static final String OTHERWHERE = "enchanted:spirit_of_otherwhere";
    private static final long FULL_MOON_CYCLE = 24000L * 8;
    private static final long NIGHT = 14000;

    private SeveranceRiteGameTests() {}

    private static void atFullMoon(GameTestHelper helper, boolean full, Runnable body) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, FULL_MOON_CYCLE);
        level.setDayTime(full ? base + NIGHT : base + 24000L * 4 + NIGHT);
        try {
            helper.assertTrue((level.getMoonPhase() == 0) == full, "could not set the moon, phase is " + level.getMoonPhase());
            body.run();
        } finally {
            level.setDayTime(before);
        }
    }

    /** In the level, three blocks from the circle: the rite finds the caster by id but the circle's player is the nearest one to its centre. */
    private static RecordingPlayer caster(GameTestHelper helper, Species species) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        p.setPos(Vec3.atBottomCenterOf(centre(helper)).add(3, 1, 0));
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    private static RecordingPlayer sired(GameTestHelper helper, UUID sire, boolean severed) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        BloodlineState s = BloodlineState.NONE.toBuilder().species(Species.HYBRID).sire(sire).build();
        BloodlineAccess.set(p, severed ? s.toBuilder().severedSire(sire).build() : s);
        place(helper, p);
        return p;
    }

    private static List<ItemStack> items(UUID bloodOf) {
        return List.of(SiresBloodItem.of(bloodOf, "Sire"), new ItemStack(item(OTHERWHERE)));
    }

    private static void done(GameTestHelper helper, RecordingPlayer... ps) {
        for (RecordingPlayer p : ps) {
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceDataLoads(GameTestHelper helper) {
        var rite = type(helper, RITE);
        helper.assertTrue(rite != null, "the rite_of_severance rite did not load");
        helper.assertTrue(rite.getPower() == 6000, "power is " + rite.getPower());
        helper.assertTrue(rite.getItems().size() == 2 && rite.getItems().stream().anyMatch(i -> i.is(SpeciesRegistries.SIRES_BLOOD.get()))
                && rite.getItems().stream().anyMatch(i -> i.is(item(OTHERWHERE))), "ingredients are " + rite.getItems());
        helper.assertTrue(new ItemStack(SpeciesRegistries.SILVER_DAGGER.get()).is(SiresBlood.SILVER_BLADES), "the silver dagger is not a silver blade");
        helper.assertTrue(!new ItemStack(Items.IRON_SWORD).is(SiresBlood.SILVER_BLADES), "an iron sword is a silver blade");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NS, "silver_dagger")).isPresent(),
                "the silver dagger recipe is missing");
        helper.succeed();
    }

    /** Altar cost: 6000 power, a witch pays x0.75 (R61). Own height (280: the others use 40, 80, 120, 160, 200 and 240) and batch, like the other altar tests. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "severance_cost")
    public static void severanceCostsSixThousandAltarPower(GameTestHelper helper) {
        int power = type(helper, RITE).getPower();
        BlockPos base = helper.absolutePos(new BlockPos(0, 280, 4));
        AltarBlockEntity altar = WitchGameTests.altar(helper, base, 7000);
        BlockPos chalkPos = base.offset(0, 0, -2);
        RecordingPlayer witch = RecordingPlayer.create(helper);
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        GoldChalkBlockEntity chalk = WitchGameTests.chalk(helper, chalkPos, witch.getUUID(), base);
        WitchGameTests.put(helper, witch, chalkPos);
        try {
            helper.assertTrue(chalk.tryConsumePower(power), "the altar did not pay " + power);
            helper.assertTrue(WitchGameTests.power(altar) == 7000 - 4500, "a witch pays 4500 of 6000, altar left with " + WitchGameTests.power(altar));
            helper.assertTrue(!chalk.tryConsumePower(power), "an altar with 2500 left paid 6000");
        } finally {
            remove(helper, witch);
            BloodlineAccess.set(witch, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- the rite ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceBreaksTheBond(GameTestHelper helper) {
        UUID sire = UUID.randomUUID();
        RecordingPlayer witch = caster(helper, Species.WITCH);
        RecordingPlayer sired = sired(helper, sire, false);
        try {
            atFullMoon(helper, true, () -> start(helper, RITE, witch, items(sire)));
            BloodlineState s = BloodlineAccess.get(sired);
            helper.assertTrue(s.bondBroken() && s.boundSire() == null, "the bond is not broken");
            helper.assertTrue(sire.equals(s.sire()), "the sire was forgotten: " + s.sire());
            helper.assertTrue(s.species() == Species.HYBRID, "the species changed to " + s.species());
            helper.assertTrue(sired.receivedOnActionBar(SeveranceRiteFactory.DONE), "the freed player was not told");
            helper.assertTrue(dropped(helper, OTHERWHERE) == 0 && dropped(helper, "supernaturalbloodlines:sires_blood") == 0, "ingredients were given back");
        } finally {
            done(helper, witch, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyWitchTypeMayPerformIt(GameTestHelper helper) {
        for (Species species : new Species[] {Species.WITCH, Species.SIPHONER, Species.HERETIC, Species.TRIBRID}) {
            UUID sire = UUID.randomUUID();
            RecordingPlayer performer = caster(helper, species);
            RecordingPlayer sired = sired(helper, sire, false);
            try {
                atFullMoon(helper, true, () -> start(helper, RITE, performer, items(sire)));
                helper.assertTrue(BloodlineAccess.get(sired).bondBroken(), species + " could not perform the rite");
            } finally {
                done(helper, performer, sired);
            }
        }
        helper.succeed();
    }

    private static void refused(GameTestHelper helper, Species performer, boolean fullMoon, UUID sireOfTarget, UUID bloodOf, boolean severed,
                                String message) {
        RecordingPlayer witch = caster(helper, performer);
        RecordingPlayer sired = sired(helper, sireOfTarget, severed);
        BloodlineState before = BloodlineAccess.get(sired);
        try {
            atFullMoon(helper, fullMoon, () -> start(helper, RITE, witch, items(bloodOf)));
            helper.assertTrue(witch.receivedOnActionBar(message), "the caster was not told " + message);
            helper.assertTrue(BloodlineAccess.get(sired).equals(before), "a refused rite changed the state: " + BloodlineAccess.get(sired));
            helper.assertTrue(dropped(helper, OTHERWHERE) == 1, "the rare ingredient was not given back");
        } finally {
            done(helper, witch, sired);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceNeedsAFullMoon(GameTestHelper helper) {
        UUID sire = UUID.randomUUID();
        refused(helper, Species.WITCH, false, sire, sire, false, "supernaturalbloodlines.rite.needs_full_moon");
        helper.succeed();
    }

    /** Noon on the full-moon day has the phase but no moon up: refused. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceNeedsTheNightNotJustTheDay(GameTestHelper helper) {
        UUID sire = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long day = before - Math.floorMod(before, FULL_MOON_CYCLE);
        RecordingPlayer witch = caster(helper, Species.WITCH);
        RecordingPlayer sired = sired(helper, sire, false);
        try {
            for (long noon : new long[] {day + 6000, day + 1000, day + 23500}) {
                level.setDayTime(noon);
                helper.assertTrue(level.getMoonPhase() == 0, "control: phase is " + level.getMoonPhase() + " at " + noon);
                start(helper, RITE, witch, items(sire));
                helper.assertTrue(witch.receivedOnActionBar("supernaturalbloodlines.rite.needs_full_moon"), "not told at day time " + noon);
                helper.assertTrue(!BloodlineAccess.get(sired).bondBroken(), "the bond broke at day time " + noon);
            }
        } finally {
            level.setDayTime(before);
            done(helper, witch, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyAWitchTypeMayPerformIt(GameTestHelper helper) {
        UUID sire = UUID.randomUUID();
        for (Species species : new Species[] {Species.NONE, Species.HYBRID, Species.ORIGINAL, Species.HUNTER_OF_THE_FIVE}) {
            refused(helper, species, true, sire, sire, false, SeveranceRiteFactory.NOT_WITCH);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceNeedsTheBloodOfTheirOwnSire(GameTestHelper helper) {
        refused(helper, Species.WITCH, true, UUID.randomUUID(), UUID.randomUUID(), false, "supernaturalbloodlines.refusal.wrong_sire_blood");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceNeedsSireBloodAtAll(GameTestHelper helper) {
        UUID sire = UUID.randomUUID();
        RecordingPlayer witch = caster(helper, Species.WITCH);
        RecordingPlayer sired = sired(helper, sire, false);
        try {
            atFullMoon(helper, true, () -> start(helper, RITE, witch, List.of(new ItemStack(item(OTHERWHERE)))));
            helper.assertTrue(witch.receivedOnActionBar(SeveranceRiteFactory.NO_BLOOD), "the caster was not told the blood is missing");
            helper.assertTrue(!BloodlineAccess.get(sired).bondBroken(), "the bond broke without blood");
        } finally {
            done(helper, witch, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severanceNeedsASiredPlayerWhoseBondIsWhole(GameTestHelper helper) {
        UUID sire = UUID.randomUUID();
        refused(helper, Species.WITCH, true, sire, sire, true, "supernaturalbloodlines.refusal.bond_already_broken");

        // nobody in the circle but the caster, who has no sire
        RecordingPlayer witch = caster(helper, Species.WITCH);
        try {
            atFullMoon(helper, true, () -> start(helper, RITE, witch, items(sire)));
            helper.assertTrue(witch.receivedOnActionBar("supernaturalbloodlines.refusal.not_sired"), "an unsired target was not refused");
        } finally {
            done(helper, witch);
        }
        helper.succeed();
    }

    // ---- Sire's Blood ----

    private static ServerPlayer taker(GameTestHelper helper, ItemStack blade, ItemStack offhand) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, blade);
        p.setItemInHand(InteractionHand.OFF_HAND, offhand);
        return p;
    }

    private static ItemStack bloodIn(ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) {
            if (s.is(SpeciesRegistries.SIRES_BLOOD.get())) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The GameTest server has PvP off, and a player hit by a player is then not hurt at all. */
    private static void hit(ServerPlayer attacker, ServerPlayer victim) {
        var server = victim.getServer();
        boolean pvp = server.isPvpAllowed();
        server.setPvpAllowed(true);
        try {
            victim.setHealth(victim.getMaxHealth());
            victim.hurt(attacker.damageSources().playerAttack(attacker), 2f);
        } finally {
            server.setPvpAllowed(pvp);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSilverBladeAndABottleTakeSireBlood(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        ServerPlayer taker = taker(helper, new ItemStack(SpeciesRegistries.SILVER_DAGGER.get()), new ItemStack(Items.GLASS_BOTTLE, 2));
        hit(taker, victim);
        ItemStack blood = bloodIn(taker);
        helper.assertTrue(!blood.isEmpty(), "no blood was taken");
        helper.assertTrue(victim.getUUID().equals(SiresBloodItem.sourceOf(blood)), "the blood is bound to " + SiresBloodItem.sourceOf(blood));
        helper.assertTrue(victim.getGameProfile().getName().equals(blood.get(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT)), "the name is " + blood.get(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT));
        helper.assertTrue(taker.getOffhandItem().getCount() == 1, "the bottle count is " + taker.getOffhandItem().getCount());

        // a mob too
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        zombie.hurt(taker.damageSources().playerAttack(taker), 1f);
        helper.assertTrue(taker.getOffhandItem().isEmpty(), "the second bottle was not spent on a zombie");
        helper.assertTrue(taker.getInventory().items.stream().anyMatch(s -> s.is(SpeciesRegistries.SIRES_BLOOD.get())
                && zombie.getUUID().equals(SiresBloodItem.sourceOf(s))), "no blood of the zombie");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noBloodWithoutTheRightBladeAndBottle(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        ServerPlayer ironBlade = taker(helper, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.GLASS_BOTTLE));
        hit(ironBlade, victim);
        helper.assertTrue(bloodIn(ironBlade).isEmpty() && ironBlade.getOffhandItem().getCount() == 1, "an iron sword took blood");

        ServerPlayer noBottle = taker(helper, new ItemStack(SpeciesRegistries.SILVER_DAGGER.get()), new ItemStack(Items.STICK));
        hit(noBottle, victim);
        helper.assertTrue(bloodIn(noBottle).isEmpty(), "blood without a bottle");

        ServerPlayer bottleInMainHand = taker(helper, new ItemStack(Items.GLASS_BOTTLE), new ItemStack(SpeciesRegistries.SILVER_DAGGER.get()));
        hit(bottleInMainHand, victim);
        helper.assertTrue(bloodIn(bottleInMainHand).isEmpty(), "the hands were swapped and it still worked");

        // a hit that does no damage takes nothing: the victim is dead already
        ServerPlayer taker = taker(helper, new ItemStack(SpeciesRegistries.SILVER_DAGGER.get()), new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(!SiresBlood.take(taker, taker), "a player took their own blood");
        helper.succeed();
    }
}
