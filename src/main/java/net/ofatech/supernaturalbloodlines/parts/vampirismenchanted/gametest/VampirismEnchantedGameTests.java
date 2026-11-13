package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import java.util.List;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** The shared faction rite and the Cleansing Blood rite. The Werewolves side is in VampirismEnchantedWerewolfGameTests. */
@PrefixGameTestTemplate(false)
public final class VampirismEnchantedGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    static final String WRONG_FACTION = "supernaturalbloodlines.rite.wrong_faction";
    static final String ORIGINAL_CURE = "supernaturalbloodlines.refusal.original_cure";

    private VampirismEnchantedGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static List<ItemStack> garlic() {
        return List.of(new ItemStack(item("vampirism:garlic")));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ritesLoad(GameTestHelper helper) {
        RiteType cleansing = type(helper, "cleansing_blood");
        helper.assertTrue(cleansing != null && cleansing.getPower() == 4000, "cleansing_blood missing or wrong power");
        helper.assertTrue(cleansing.getItems().size() == 3, "cleansing_blood has " + cleansing.getItems().size() + " ingredients");
        helper.assertTrue((type(helper, "silver_moon") != null) == ModList.get().isLoaded("werewolves"),
                "silver_moon is registered exactly when Werewolves is loaded");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cleansingCuresPlainVampire(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            helper.assertTrue(handler(caster).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
            start(helper, "cleansing_blood", caster, garlic());
            helper.assertTrue(handler(caster).getCurrentFaction() == null, "still in faction " + handler(caster).getCurrentFaction());
            helper.assertTrue(BloodlineAccess.get(caster).species() == Species.NONE, "species changed");
            helper.assertTrue(!caster.receivedOnActionBar(WRONG_FACTION), "a good cure sent the wrong faction message");
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 0, "a good cure gave the ingredients back");
        } finally {
            remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cureRiteOnNonVampireFails(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer human = RecordingPlayer.create(helper);
        place(helper, human);
        try {
            start(helper, "cleansing_blood", human, garlic());
            helper.assertTrue(human.receivedOnActionBar(WRONG_FACTION), "the caster was not told the faction is wrong");
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 1, "the ingredients were not given back: " + dropped(helper, "vampirism:garlic"));
            clearDropped(helper);

            // a hunter is not a vampire either, and stays a hunter
            helper.assertTrue(handler(human).setFactionAndLevel(VReference.HUNTER_FACTION, 2), "could not make a hunter");
            start(helper, "cleansing_blood", human, garlic());
            helper.assertTrue(handler(human).getCurrentFaction() == VReference.HUNTER_FACTION, "the hunter lost the faction");
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 1, "the hunter's ingredients were not given back");
        } finally {
            clearDropped(helper);
            remove(helper, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cureRiteRespectsBloodlineOutcome(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer original = RecordingPlayer.create(helper);
        place(helper, original);
        try {
            helper.assertTrue(handler(original).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
            BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            start(helper, "cleansing_blood", original, garlic());
            helper.assertTrue(original.receivedOnActionBar(ORIGINAL_CURE), "no original_cure refusal sent");
            helper.assertTrue(handler(original).getCurrentFaction() == VReference.VAMPIRE_FACTION, "the Original left the vampires");
            helper.assertTrue(BloodlineAccess.get(original).species() == Species.ORIGINAL, "the Original's species changed");
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 1, "the ingredients were not given back");
        } finally {
            clearDropped(helper);
            remove(helper, original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cleansingTargetsThePlayerOnTheCircle(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer patient = RecordingPlayer.create(helper);
        // the caster stands apart, the patient on the centre block
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
        helper.getLevel().addNewPlayer(caster);
        place(helper, patient);
        try {
            handler(caster).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
            handler(patient).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
            start(helper, "cleansing_blood", caster, garlic());
            helper.assertTrue(handler(patient).getCurrentFaction() == null, "the player on the circle was not cured");
            helper.assertTrue(handler(caster).getCurrentFaction() == VReference.VAMPIRE_FACTION, "the caster was cured instead");
        } finally {
            remove(helper, caster);
            remove(helper, patient);
        }
        helper.succeed();
    }
}
