package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.unique.ITrulyUnique;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.Recipes;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.AddonProbe;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.FoxAblazeProbe;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Hooks;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Probes;

/**
 * The readers of the Raphael merge (plan 19b task 3). The owner rule runs on Tensura's real unique storage; what needs an addon jar is gated on the jar.
 */
@PrefixGameTestTemplate(false)
public final class RaphaelProbeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation PROBE_SKILL = ResourceLocation.fromNamespaceAndPath(NS, "test_raphael_owner");

    private RaphaelProbeGameTests() {
    }

    private static boolean loaded(String modId) {
        return Recipes.modLoaded(modId);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOwnerRuleReadsTheRealUniqueStorage(GameTestHelper helper) {
        ITrulyUnique storage = TensuraStorages.getUniqueStorageFrom(helper.getLevel().getServer().overworld());
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        try {
            helper.assertTrue(!FoxAblazeProbe.ownedBy(storage, other, PROBE_SKILL), "an unowned skill reads as owned");
            storage.addSkill(PROBE_SKILL, owner);
            helper.assertTrue(FoxAblazeProbe.ownedBy(storage, other, PROBE_SKILL), "a skill owned by another reads as free");
            helper.assertTrue(!FoxAblazeProbe.ownedBy(storage, owner, PROBE_SKILL), "a skill owned by the asker reads as owned by another");
            helper.assertTrue(FoxAblazeProbe.ownedBy(storage, other, ResourceLocation.fromNamespaceAndPath(NS, "nothing"), PROBE_SKILL), "the second id of the pair was not asked");
            helper.assertTrue(!FoxAblazeProbe.ownedBy(storage, other, ResourceLocation.fromNamespaceAndPath(NS, "nothing")), "a skill nobody owns reads as owned");
        } finally {
            storage.removeSkill(PROBE_SKILL);
            storage.markDirty();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTestHooksOverrideWhatTheProbeReads(GameTestHelper helper) {
        try {
            FoxAblazeProbe.loadedHook = () -> true;
            helper.assertTrue(FoxAblazeProbe.loaded(), "the loaded hook was ignored (true)");
            FoxAblazeProbe.loadedHook = () -> false;
            helper.assertTrue(!FoxAblazeProbe.loaded(), "the loaded hook was ignored (false)");
        } finally {
            FoxAblazeProbe.loadedHook = null;
        }
        helper.assertTrue(FoxAblazeProbe.loaded() == loaded("foxablazeultimate"), "without a hook the probe does not read the loaded mods");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGreatSageProbeIsKnownByIdAndOverridesOnlyWithFoxAblaze(GameTestHelper helper) {
        Optional<AddonProbe> probe = Probes.of("greatsage.raphael");
        helper.assertTrue(probe.isPresent(), "the probe id greatsage.raphael is unknown");
        try {
            FoxAblazeProbe.loadedHook = () -> false;
            helper.assertTrue(probe.get().grantOverride().isEmpty(), "an override without FoxAblaze: " + probe.get().grantOverride());
            FoxAblazeProbe.loadedHook = () -> true;
            helper.assertTrue(probe.get().grantOverride().equals(Optional.of("foxablazeultimate:raphael")), "the override with FoxAblaze: " + probe.get().grantOverride());
        } finally {
            FoxAblazeProbe.loadedHook = null;
        }
        helper.assertTrue(probe.get().alsoHolds().equals(List.of("greatsage:raphael", "foxablazeultimate:raphael")), "also holds: " + probe.get().alsoHolds());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutGreatSageTheProbeNamesWhatIsMissingAndTheRoadIsClosed(GameTestHelper helper) {
        if (loaded("greatsage")) {
            helper.succeed(); // the other test reads the real jar
            return;
        }
        AddonProbe probe = Probes.of("greatsage.raphael").orElseThrow();
        helper.assertTrue(probe.missingHook().isPresent(), "no hook is missing without Great Sage");
        helper.assertTrue(!probe.roadOpen(), "the road is open without Great Sage");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGreatSageProbeReadsTheRealRoadAndTheHooksResolve(GameTestHelper helper) {
        if (!loaded("greatsage")) {
            helper.succeed();
            return;
        }
        AddonProbe probe = Probes.of("greatsage.raphael").orElseThrow();
        helper.assertTrue(probe.missingHook().isEmpty(), "a Great Sage hook moved: " + probe.missingHook());
        Object obtainable = Hooks.staticField("dev.dedek.greatsage.server.CapstoneGate", "OBTAINABLE").value().orElseThrow();
        helper.assertTrue(probe.roadOpen() == Boolean.TRUE.equals(obtainable), "the probe says " + probe.roadOpen() + ", the gate says " + obtainable);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFoxAblazeRuleKeyAndSkillsResolve(GameTestHelper helper) {
        if (!loaded("foxablazeultimate")) {
            helper.succeed();
            return;
        }
        helper.assertTrue(Hooks.staticField("com.foxablazeultimate.world.FoxAblazeGameRules", "RAPHAEL_TRULY_UNIQUE").found(), "the single-owner rule moved");
        for (String id : List.of("foxablazeultimate:raphael", "foxablazeultimate:ciel")) {
            helper.assertTrue(io.github.manasmods.manascore.skill.api.SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id)) != null, id + " is not a registered skill");
        }
        helper.succeed();
    }
}
