package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionHandler;
import net.ofatech.supernaturalbloodlines.parts.species.item.OriginalBloodEffect;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/** A hybrid's sire is the Original whose blood started the transition. Registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class SirelineHybridGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SirelineHybridGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        return p;
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridRecordsOriginalSire(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        UUID original = UUID.randomUUID();
        try {
            p.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
            p.setData(SpeciesRegistries.BLOOD_SOURCE, original);
            p.hurt(helper.getLevel().damageSources().generic(), 1000f);
            helper.assertTrue(BloodlineAccess.get(p).inTransition(), "no transition: " + BloodlineAccess.get(p));
            helper.assertTrue(index(helper).sireOf(p.getUUID()).isEmpty(), "sired before the transition ended");
            TransitionHandler.drankDoppelganger(p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.species() == Species.HYBRID && original.equals(s.sire()), "state is " + s);
            helper.assertTrue(original.equals(index(helper).sireOf(p.getUUID()).orElse(null)), "index sire is " + index(helper).sireOf(p.getUUID()));
            helper.assertTrue(index(helper).childrenOf(original).stream().anyMatch(n -> n.id().equals(p.getUUID())),
                    "the Original's children are " + index(helper).childrenOf(original));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    /** The completion rule sets the sire itself (it does not ask the recorder), and the index mirrors the state. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridCompletionMovesTheIndexWithTheSire(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        UUID earlier = UUID.randomUUID();
        UUID original = UUID.randomUUID();
        try {
            SireRecorder.record(earlier, "Earlier", p);
            helper.assertTrue(earlier.equals(index(helper).sireOf(p.getUUID()).orElse(null)), "not indexed under the first sire");
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().inTransition(true).pendingSire(original).transitionTicksLeft(9000).build());
            TransitionHandler.drankDoppelganger(p);
            helper.assertTrue(original.equals(BloodlineAccess.get(p).sire()), "sire is " + BloodlineAccess.get(p).sire());
            helper.assertTrue(original.equals(index(helper).sireOf(p.getUUID()).orElse(null)), "index sire is " + index(helper).sireOf(p.getUUID()));
            helper.assertTrue(index(helper).childrenOf(earlier).isEmpty(), "the old sire still lists the hybrid");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    private static SirelineIndex index(GameTestHelper helper) {
        return SirelineIndex.get(helper.getLevel());
    }
}
