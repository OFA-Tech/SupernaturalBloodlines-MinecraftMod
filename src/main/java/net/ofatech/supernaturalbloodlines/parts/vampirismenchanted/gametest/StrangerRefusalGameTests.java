package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/**
 * R201, R362: a rite worked on someone else tells its caster nothing about them. Whatever is wrong with the one on the centre (their faction, their
 * species, a faction that will not take them), the caster hears the one generic line and the one on the centre hears why.
 */
@PrefixGameTestTemplate(false)
public final class StrangerRefusalGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String ORIGINAL_CURE = "supernaturalbloodlines.refusal.original_cure";

    private StrangerRefusalGameTests() {
    }

    private static void placeApart(GameTestHelper helper, ServerPlayer p) {
        p.setPos(Vec3.atBottomCenterOf(RiteTestSupport.centre(helper)).add(4, 1, 0));
        helper.getLevel().addNewPlayer(p);
    }

    /** Works {@code rite} by a caster standing apart on {@code target}, on a full-moon night; returns the caster. */
    private static RecordingPlayer workOn(GameTestHelper helper, String rite, RecordingPlayer target, List<ItemStack> items) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        long before = helper.getLevel().getDayTime();
        RiteTestSupport.place(helper, target);
        placeApart(helper, caster);
        try {
            helper.getLevel().setDayTime(18000);
            RiteTestSupport.start(helper, rite, caster, items);
        } finally {
            helper.getLevel().setDayTime(before);
            RiteTestSupport.remove(helper, caster);
            RiteTestSupport.remove(helper, target);
        }
        return caster;
    }

    private static void onlyTheGenericLine(GameTestHelper helper, String what, RecordingPlayer caster, RecordingPlayer target, String cause) {
        helper.assertTrue(caster.refusedOnBothChannels(FactionRiteFactory.NO_EFFECT) && caster.heardKeys().equals(java.util.Set.of(FactionRiteFactory.NO_EFFECT)),
                what + ": the caster heard " + caster.heard());
        helper.assertTrue(target.received(cause), what + ": the one on the centre was not told why: " + target.heard());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cleansingBloodOnAnOriginalStrangerTellsTheCasterOnlyThatNothingChanges(GameTestHelper helper) {
        RecordingPlayer original = RecordingPlayer.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(original).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "setup: not a vampire");
        BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
        try {
            RecordingPlayer caster = workOn(helper, "cleansing_blood", original, List.of());
            onlyTheGenericLine(helper, "an Original", caster, original, ORIGINAL_CURE);
        } finally {
            BloodlineAccess.set(original, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cleansingBloodOnAHumanStrangerDoesNotNameTheirFaction(GameTestHelper helper) {
        RecordingPlayer human = RecordingPlayer.create(helper);
        RecordingPlayer caster = workOn(helper, "cleansing_blood", human, List.of());
        onlyTheGenericLine(helper, "a human", caster, human, FactionRiteFactory.WRONG_FACTION);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRiteOfTheMoonOnAWrongFactionStrangerIsGeneric(GameTestHelper helper) {
        if (!ModList.get().isLoaded("werewolves")) {
            SupernaturalBloodlines.LOGGER.info("StrangerRefusal SKIPPED: the Rite of the Moon needs Werewolves");
            helper.succeed();
            return;
        }
        helper.assertTrue(RiteTestSupport.type(helper, "rite_of_the_moon") != null, "the species part is on with Werewolves, but the Rite of the Moon is missing");
        RecordingPlayer hunter = RecordingPlayer.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "setup: not a hunter");
        RecordingPlayer caster = workOn(helper, "rite_of_the_moon", hunter, List.of());
        onlyTheGenericLine(helper, "a hunter", caster, hunter, FactionRiteFactory.WRONG_FACTION);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWardOfTheMoonOnAWrongFactionStrangerIsGeneric(GameTestHelper helper) {
        if (!ModList.get().isLoaded("werewolves") || !EnabledParts.isEnabled("artifacts")) {
            SupernaturalBloodlines.LOGGER.info("StrangerRefusal SKIPPED: the Ward of the Moon needs Werewolves and the artifacts part");
            helper.succeed();
            return;
        }
        helper.assertTrue(RiteTestSupport.type(helper, "ward_of_the_moon") != null, "the artifacts part is on with Werewolves, but the Ward of the Moon is missing");
        RecordingPlayer human = RecordingPlayer.create(helper);
        ItemStack ring = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NS, "moon_ring")));
        RecordingPlayer caster = workOn(helper, "ward_of_the_moon", human, List.of(ring));
        onlyTheGenericLine(helper, "a human", caster, human, FactionRiteFactory.WRONG_FACTION);
        helper.succeed();
    }
}
