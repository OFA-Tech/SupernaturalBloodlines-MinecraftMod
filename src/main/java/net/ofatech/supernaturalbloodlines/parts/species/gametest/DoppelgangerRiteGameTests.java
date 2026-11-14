package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.init.EItems;
import net.favouriteless.enchanted.common.items.component.EDataComponents;
import net.favouriteless.enchanted.common.items.component.EntityRefData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.rite.DoppelgangerBloodRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.DoppelgangerRiteRules;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Plan 12b, task 3: the Rite of Doppelgänger Blood and whose taglock it takes. Werewolves only (the rite's data needs it). */
@PrefixGameTestTemplate(false)
public final class DoppelgangerRiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String BOTTLE = NS + ":doppelganger_blood";

    private DoppelgangerRiteGameTests() {
    }

    static ItemStack taglock(UUID owner) {
        ItemStack taglock = new ItemStack(EItems.TAGLOCK_FILLED.get());
        taglock.set(EDataComponents.ENTITY_REF.get(), EntityRefData.of(owner, "owner"));
        return taglock;
    }

    private static List<ItemStack> ingredients(ItemStack taglock) {
        return List.of(taglock, new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.AMETHYST_SHARD));
    }

    private static Villager villager(GameTestHelper helper, double doppelgangerChance) {
        SpeciesConfig.overrideDoppelgangerVillagerChance(doppelgangerChance);
        try {
            return helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        } finally {
            SpeciesConfig.overrideDoppelgangerVillagerChance(null);
        }
    }

    private static boolean skip(GameTestHelper helper) {
        if (type(helper, "doppelganger_blood") == null) {
            helper.succeed(); // the rite's data is gated on the species part and Werewolves: nothing to test when either is off
            return true;
        }
        return false;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteDataLoadsWithTheNewFactory(GameTestHelper helper) {
        RiteType type = type(helper, "doppelganger_blood");
        helper.assertTrue(type != null, "doppelganger_blood is not in Enchanted's rite registry");
        helper.assertTrue(type.getPower() == 6000, "power is " + type.getPower());
        helper.assertTrue(type.getItems().size() == 3 && type.getItems().stream().anyMatch(s -> s.is(EItems.TAGLOCK_FILLED.get())),
                "the ingredients changed");
        helper.assertTrue(type.getOutputs().size() == 1 && type.getOutputs().get(0).is(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.DOPPELGANGER_BLOOD.get()),
                "the rite no longer advertises the bottle");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteRefusesNonDoppelgangerTaglock(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer plainPlayer = RecordingPlayer.create(helper);
        Villager plainVillager = villager(helper, 0.0);
        place(helper, caster);
        place(helper, plainPlayer);
        try {
            BloodlineAccess.set(plainPlayer, BloodlineState.NONE);
            for (UUID owner : List.of(plainVillager.getUUID(), plainPlayer.getUUID())) {
                start(helper, "doppelganger_blood", caster, ingredients(taglock(owner)));
                helper.assertTrue(dropped(helper, BOTTLE) == 0, "a bottle was made for a taglock of a non-doppelgänger");
                helper.assertTrue(dropped(helper, "enchanted:taglock") == 1, "the taglock was not given back");
                clearDropped(helper);
            }
            helper.assertTrue(caster.receivedOnActionBar(DoppelgangerRiteRules.NOT_DOPPELGANGER), "the caster was not told why");
            helper.assertTrue(BloodlineAccess.get(plainPlayer).revealedGenes().isEmpty() && !plainPlayer.received(DoppelgangerBloodRiteFactory.REVEALED_NO),
                    "a refused rite revealed or told the owner");
        } finally {
            remove(helper, caster);
            remove(helper, plainPlayer);
            plainVillager.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteAcceptsDoppelgangerVillager(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        Villager doppelganger = villager(helper, 1.0);
        place(helper, caster);
        try {
            start(helper, "doppelganger_blood", caster, ingredients(taglock(doppelganger.getUUID())));
            helper.assertTrue(dropped(helper, BOTTLE) == 1, "the rite made " + dropped(helper, BOTTLE) + " bottles");
            helper.assertTrue(dropped(helper, "enchanted:taglock") == 0, "the taglock came back from a good rite");
        } finally {
            remove(helper, caster);
            doppelganger.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteAcceptsGenePlayerAndReveals(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer owner = RecordingPlayer.create(helper);
        RecordingPlayer watcher = RecordingPlayer.create(helper);
        place(helper, caster);
        place(helper, owner);
        place(helper, watcher);
        try {
            BloodlineAccess.set(owner, BloodlineState.NONE.toBuilder().doppelgangerGene(true).doppelgangerRolled(true).build());
            start(helper, "doppelganger_blood", caster, ingredients(taglock(owner.getUUID())));
            helper.assertTrue(dropped(helper, BOTTLE) == 1, "no bottle for a player with the gene");
            helper.assertTrue(BloodlineAccess.get(owner).revealedGenes().equals(Set.of(Genes.DOPPELGANGER)),
                    "revealed: " + BloodlineAccess.get(owner).revealedGenes());
            helper.assertTrue(owner.received(DoppelgangerBloodRiteFactory.REVEALED_YES), "the owner was not told");
            helper.assertTrue(!caster.received(DoppelgangerBloodRiteFactory.REVEALED_YES) && !watcher.received(DoppelgangerBloodRiteFactory.REVEALED_YES),
                    "someone else was told the owner's result");
            helper.assertTrue(BloodlineAccess.get(caster).revealedGenes().isEmpty(), "the caster's genes changed");
        } finally {
            remove(helper, caster);
            remove(helper, owner);
            remove(helper, watcher);
            BloodlineAccess.set(owner, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanModeAcceptsAnyHumanLike(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer human = RecordingPlayer.create(helper);
        RecordingPlayer vampire = RecordingPlayer.create(helper);
        Villager plainVillager = villager(helper, 0.0);
        place(helper, caster);
        place(helper, human);
        place(helper, vampire);
        SpeciesConfig.overrideDoppelgangerRiteHuman(true);
        try {
            BloodlineAccess.set(human, BloodlineState.NONE);
            for (UUID owner : List.of(plainVillager.getUUID(), human.getUUID())) {
                start(helper, "doppelganger_blood", caster, ingredients(taglock(owner)));
                helper.assertTrue(dropped(helper, BOTTLE) == 1, "human mode refused a human-like owner");
                clearDropped(helper);
            }
            helper.assertTrue(human.received(DoppelgangerBloodRiteFactory.REVEALED_NO), "a human told it has no gene was not told");
            helper.assertTrue(BloodlineAccess.get(human).revealedGenes().equals(Set.of(Genes.DOPPELGANGER)), "the human's gene was not revealed");

            // a vampire is not human-like, gene or not
            BloodlineAccess.set(vampire, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).doppelgangerGene(true).build());
            start(helper, "doppelganger_blood", caster, ingredients(taglock(vampire.getUUID())));
            helper.assertTrue(dropped(helper, BOTTLE) == 0, "human mode took a vampire's taglock");
            helper.assertTrue(caster.receivedOnActionBar(DoppelgangerRiteRules.NOT_HUMAN_LIKE), "the caster was not told why");
        } finally {
            SpeciesConfig.overrideDoppelgangerRiteHuman(null);
            remove(helper, caster);
            remove(helper, human);
            remove(helper, vampire);
            BloodlineAccess.set(human, BloodlineState.NONE);
            BloodlineAccess.set(vampire, BloodlineState.NONE);
            plainVillager.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ownerThatIsNotLoadedOrOnlineIsRefused(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            for (boolean human : new boolean[]{false, true}) {
                SpeciesConfig.overrideDoppelgangerRiteHuman(human);
                start(helper, "doppelganger_blood", caster, ingredients(taglock(UUID.randomUUID())));
                helper.assertTrue(dropped(helper, BOTTLE) == 0, "a bottle was made for an owner nobody can find");
                clearDropped(helper);
            }
            helper.assertTrue(caster.receivedOnActionBar(DoppelgangerRiteRules.OWNER_AWAY), "the caster was not told the owner must be nearby or online");

            // an unfilled taglock (no owner) is refused with its own reason
            RecordingPlayer second = RecordingPlayer.create(helper);
            place(helper, second);
            start(helper, "doppelganger_blood", second, ingredients(new ItemStack(EItems.TAGLOCK_FILLED.get())));
            helper.assertTrue(second.receivedOnActionBar(DoppelgangerRiteRules.NO_OWNER) && dropped(helper, BOTTLE) == 0, "a taglock without an owner was not refused");
            remove(helper, second);
        } finally {
            SpeciesConfig.overrideDoppelgangerRiteHuman(null);
            remove(helper, caster);
        }
        helper.succeed();
    }
}
