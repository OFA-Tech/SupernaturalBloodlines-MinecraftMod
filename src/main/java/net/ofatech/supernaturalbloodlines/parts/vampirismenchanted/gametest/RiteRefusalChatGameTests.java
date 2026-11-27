package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.Cloak;
import net.ofatech.supernaturalbloodlines.core.boss.CloakAccess;
import net.ofatech.supernaturalbloodlines.parts.bosses.rite.CloakingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;
import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;

/** R357: a rite refusal reaches the action bar and chat once each; a success stays on the action bar; an oracle refusal is one line for every cause. */
@PrefixGameTestTemplate(false)
public final class RiteRefusalChatGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private RiteRefusalChatGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteTellRefusesOnBothChannelsOnceAndSaysOnTheBarOnly(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        RiteTell.refuse(p, FactionRiteFactory.NEEDS_FULL_MOON);
        RiteTell.say(p, FactionRiteFactory.RITE_TAKES_YOU);
        RiteTell.refuseGeneric(p, FactionRiteFactory.NO_EFFECT);
        RiteTell.refuse(null, FactionRiteFactory.NEEDS_FULL_MOON);
        helper.assertTrue(p.refusedOnBothChannels(FactionRiteFactory.NEEDS_FULL_MOON), "a refusal: " + p.heard());
        helper.assertTrue(p.refusedOnBothChannels(FactionRiteFactory.NO_EFFECT), "a generic refusal: " + p.heard());
        helper.assertTrue(p.countOnActionBar(FactionRiteFactory.RITE_TAKES_YOU) == 1 && p.countInChat(FactionRiteFactory.RITE_TAKES_YOU) == 0, "a notice went to chat: " + p.heard());
        helper.succeed();
    }

    /** R201: whatever the target is, a caster who is not the target hears the same one line, now in chat too; the target hears its own. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOracleRefusalIsTheSameLineForEveryCause(GameTestHelper helper) {
        for (String cause : List.of("supernaturalbloodlines.refusal.no_gene", FactionRiteFactory.WRONG_FACTION, "supernaturalbloodlines.refusal.not_original")) {
            RecordingPlayer caster = RecordingPlayer.create(helper);
            RecordingPlayer target = RecordingPlayer.create(helper);
            RiteTargets.refuseAbout(caster, target, cause);
            helper.assertTrue(caster.refusedOnBothChannels(FactionRiteFactory.NO_EFFECT) && !caster.received(cause), cause + ": the caster heard " + caster.heard());
            helper.assertTrue(target.refusedOnBothChannels(cause), cause + ": the target heard " + target.heard());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSuccessIsNotInChat(GameTestHelper helper) {
        if (RiteTestSupport.type(helper, "rite_of_cloaking") == null) {
            SupernaturalBloodlines.LOGGER.info("RiteRefusalChat SKIPPED: the bosses part is off");
            helper.succeed();
            return;
        }
        RecordingPlayer witch = RecordingPlayer.create(helper);
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        RiteTestSupport.place(helper, witch);
        try {
            ItemStack grimoire = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NS, "kol_grimoire")));
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, grimoire, t -> t.putBoolean("read", true));
            RiteTestSupport.start(helper, "rite_of_cloaking", witch, List.of(grimoire, new ItemStack(Items.PHANTOM_MEMBRANE, 2), new ItemStack(Items.ENDER_PEARL)));
            String done = CloakingRiteFactory.DONE;
            helper.assertTrue(witch.countOnActionBar(done) == 1 && witch.countInChat(done) == 0, "the success line: " + witch.heard());
        } finally {
            CloakAccess.set(witch, Cloak.NONE);
            RiteTestSupport.remove(helper, witch);
        }
        helper.succeed();
    }

    /**
     * Every rite of this mod, worked by a plain human at noon with no ingredients: nothing is said twice on a channel, and every action-bar line such a
     * caster gets is a refusal (nothing succeeds for them), so it is in chat too. An answer that was always a chat line (the lineage reveal) stays one.
     */
    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 400)
    public static void everyRiteRefusesAPlainCasterOnBothChannels(GameTestHelper helper) {
        long timeBefore = helper.getLevel().getDayTime();
        helper.getLevel().setDayTime(6000);
        var registry = helper.getLevel().registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY);
        Map<String, String> told = new TreeMap<>();
        List<String> wrong = new ArrayList<>();
        int bothChannels = 0;
        for (ResourceLocation id : registry.keySet()) {
            if (!id.getNamespace().equals(NS)) {
                continue;
            }
            RecordingPlayer p = RecordingPlayer.create(helper);
            RiteTestSupport.place(helper, p);
            try {
                RiteTestSupport.start(helper, id.getPath(), p, List.of());
            } catch (RuntimeException e) {
                wrong.add(id.getPath() + " threw " + e);
            } finally {
                RiteTestSupport.remove(helper, p);
            }
            told.put(id.getPath(), p.heard());
            boolean refusedHere = false;
            for (String key : p.heardKeys()) {
                int bar = p.countOnActionBar(key);
                int chat = p.countInChat(key);
                if (bar > 1 || chat > 1) {
                    wrong.add(id.getPath() + ": " + key + " bar=" + bar + " chat=" + chat);
                }
                refusedHere |= bar == 1 && chat == 1;
                if (bar == 1 && chat == 0) {
                    wrong.add(id.getPath() + ": " + key + " came to a plain caster at noon on the action bar only");
                }
            }
            bothChannels += refusedHere ? 1 : 0;
        }
        helper.getLevel().setDayTime(timeBefore);
        SupernaturalBloodlines.LOGGER.info("RiteRefusalChat sweep {}", told);
        helper.assertTrue(wrong.isEmpty(), "rites that said a line more than once or threw: " + wrong);
        helper.assertTrue(bothChannels > 0, "no rite refused a plain caster on both channels: " + told);
        helper.succeed();
    }

    /** Without Werewolves no rite of the werewolves is registered (the data conditions), so a player never meets the in-code guard. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutWerewolvesNoWerewolfRiteIsThere(GameTestHelper helper) {
        if (ModList.get().isLoaded("werewolves")) {
            SupernaturalBloodlines.LOGGER.info("RiteRefusalChat SKIPPED: Werewolves is loaded");
            helper.succeed();
            return;
        }
        for (String path : List.of("alpha_trial", "rite_of_lineage_awaken", "rite_of_lineage_reveal", "werewolf_brother", "silver_moon", "rite_of_the_moon", "rite_of_the_frequency")) {
            helper.assertTrue(RiteTestSupport.type(helper, path) == null, path + " is registered without Werewolves");
        }
        helper.succeed();
    }
}
