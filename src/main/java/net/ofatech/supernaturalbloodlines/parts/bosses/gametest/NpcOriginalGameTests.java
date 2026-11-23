package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.line.Imprisonment;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * The NPC Original base (plan 17b) with the real entity (Mikael): R151 to R153 as they play out, the line glue ({@code LineDeath}, {@code LineWarning},
 * {@code Imprisonment}) and the tags that make a mountain ash line stop him.
 */
@PrefixGameTestTemplate(false)
public final class NpcOriginalGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String HIDDEN = "supernaturalbloodlines.line.warning_desiccated_hidden";
    private static final String NAMED = "supernaturalbloodlines.line.warning_desiccated_named";

    private NpcOriginalGameTests() {
    }

    private static LineTestPlayer member(GameTestHelper helper, boolean discovered) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(Species.HYBRID).build());
        LineIndex.get(helper.getLevel().getServer()).setRoot(p.getUUID(), BossTestRig.MIKAEL, true);
        if (discovered) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().knownLine(BossTestRig.MIKAEL).build());
        }
        return p;
    }

    private static void online(LineTestPlayer... ps) {
        java.util.Map<java.util.UUID, net.minecraft.server.level.ServerPlayer> m = new java.util.HashMap<>();
        for (LineTestPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
    }

    private static void forget(GameTestHelper helper, LineTestPlayer... ps) {
        for (LineTestPlayer p : ps) {
            LineIndex.get(helper.getLevel().getServer()).remove(p.getUUID());
            BloodlineAccess.set(p, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLethalBlowThatIsNotWhiteOakDesiccatesHimAndWarnsTheLine(GameTestHelper helper) {
        LineTestPlayer knows = member(helper, true);
        LineTestPlayer doesNot = member(helper, false);
        try (BossTestRig rig = new BossTestRig(helper)) {
            online(knows, doesNot);
            MikaelEntity m = rig.spawn();
            helper.assertTrue(m.isAlive() && !m.isDesiccated(), "control: he is standing");
            helper.assertTrue(m.hurt(rig.plainBlow(), 10000F), "the blow did nothing");
            helper.assertTrue(m.isAlive(), "a lethal blow killed him instead of desiccating him");
            helper.assertTrue(m.isDesiccated() && m.getHealth() == 1.0F, "not desiccated, health " + m.getHealth());
            helper.assertTrue(m.isNoAi(), "he still has his AI");
            helper.assertTrue(m.getData(SpeciesRegistries.NPC_OPEN) && m.getData(SpeciesRegistries.ORIGINAL_ROOT).equals("mikael"), "he is not an open Original to the line rules");
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.OPEN, "the index says " + rig.index.state(BossId.MIKAEL));
            helper.assertTrue(knows.received(NAMED) && doesNot.received(HIDDEN), "the line was not warned by name and without it: " + knows.heard() + " / " + doesNot.heard());
        } finally {
            forget(helper, knows, doesNot);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowThatDoesNotKillOnlyHurtsHim(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            float before = m.getHealth();
            m.hurt(rig.plainBlow(), 5F);
            helper.assertTrue(!m.isDesiccated() && m.getHealth() < before, "a small blow desiccated him or did nothing: " + m.getHealth());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakOnAStandingOneOnlyHurts(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.hurt(rig.whiteOakBlow(), 10000F);
            helper.assertTrue(m.isAlive() && !m.isDesiccated(), "white oak killed or desiccated a standing Original");
            helper.assertTrue(m.getHealth() >= 1.0F && m.getHealth() < m.getMaxHealth(), "it did not even hurt: " + m.getHealth());
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "the index says " + rig.index.state(BossId.MIKAEL));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakKillsAStandingOneWhenTheConfigSaysSo(GameTestHelper helper) {
        LineTestPlayer child = member(helper, false);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("npc_original_stake_needs_open", false);
            online(child);
            MikaelEntity m = rig.spawn();
            m.hurt(rig.whiteOakBlow(), 10000F);
            helper.assertTrue(!m.isAlive(), "white oak did not kill him with npc_original_stake_needs_open off");
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.DEAD, "the index says " + rig.index.state(BossId.MIKAEL));
        } finally {
            BossesConfig.override("npc_original_stake_needs_open", null);
            forget(helper, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakKillsAnOpenOneAndTheLineDiesWithHim(GameTestHelper helper) {
        LineTestPlayer child = member(helper, false);
        LineTestPlayer freed = member(helper, false);
        try (BossTestRig rig = new BossTestRig(helper)) {
            online(child, freed);
            // a member freed by a rite is not in the line any more
            LineIndex.get(helper.getLevel().getServer()).free(freed.getUUID(), BossTestRig.MIKAEL, LineIndex.Kind.IMPRISONED);
            MikaelEntity m = rig.spawn();
            m.desiccate();
            m.hurt(rig.whiteOakBlow(), 10000F);
            helper.assertTrue(!m.isAlive(), "white oak did not kill an open Original");
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.DEAD, "the index says " + rig.index.state(BossId.MIKAEL));
            helper.assertTrue(LineIndex.get(rig.server).isDead(BossTestRig.MIKAEL), "his line is not marked dead");
            helper.assertTrue(child.isDeadOrDying(), "a member of his line did not die");
            helper.assertTrue(!freed.isDeadOrDying(), "someone freed from his line died with it");
        } finally {
            forget(helper, child, freed);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillThatIsNotWhiteOakEndsHimButNotHisLine(GameTestHelper helper) {
        LineTestPlayer child = member(helper, false);
        try (BossTestRig rig = new BossTestRig(helper)) {
            online(child);
            MikaelEntity m = rig.spawn();
            m.kill();
            helper.assertTrue(!m.isAlive() && rig.index.state(BossId.MIKAEL) == BossState.DEAD, "/kill did not end him");
            helper.assertTrue(!child.isDeadOrDying(), "the line died without white oak");
        } finally {
            forget(helper, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAshDaggerDesiccatesAStandingOne(GameTestHelper helper) {
        Item dagger = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NS, "white_oak_ash_dagger")).orElse(null);
        if (dagger == null) {
            helper.succeed(); // the artifacts part is off: there is no dagger
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.hurt(rig.blowWith(dagger), 1F);
            helper.assertTrue(m.isAlive() && m.isDesiccated(), "the Ash Dagger did not desiccate him");
            m.hurt(rig.blowWith(dagger), 10000F);
            helper.assertTrue(m.isAlive(), "the Ash Dagger killed an open Original");
        }
        helper.succeed();
    }

    /** R33 as changed by plan 18g: the plain dagger no longer lodges in an Original hybrid, but an NPC Original (Klaus included) takes either dagger as before. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void npcOriginalBossesAcceptEitherDagger(GameTestHelper helper) {
        for (String name : List.of("white_oak_ash_dagger", "cursed_white_oak_ash_dagger")) {
            Item dagger = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NS, name)).orElse(null);
            if (dagger == null) {
                helper.succeed(); // the artifacts part is off: there is no dagger
                return;
            }
            try (BossTestRig rig = new BossTestRig(helper)) {
                net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity klaus = rig.spawnKlaus();
                klaus.hurt(rig.blowWith(dagger), 1F);
                helper.assertTrue(klaus.isAlive() && klaus.isDesiccated(), "the " + name + " did not desiccate Klaus");
                klaus.hurt(rig.blowWith(dagger), 10000F);
                helper.assertTrue(klaus.isAlive() && klaus.getHealth() == 1.0F, "the " + name + " killed an open Klaus: " + klaus.getHealth());
            }
        }
        helper.succeed();
    }

    /** R158 beside R33: an open Original is killed by white oak a player holds, never by a dagger, and a mob holding a dagger is no different. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobWithACursedDaggerDoesNotKillAnOpenOne(GameTestHelper helper) {
        Item dagger = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NS, "cursed_white_oak_ash_dagger")).orElse(null);
        if (dagger == null) {
            helper.succeed();
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.desiccate();
            net.minecraft.world.entity.monster.Zombie z = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new net.minecraft.core.BlockPos(1, 1, 1));
            z.setNoAi(true);
            z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(dagger));
            m.hurt(helper.getLevel().damageSources().mobAttack(z), 10000F);
            helper.assertTrue(m.isAlive() && m.getHealth() == 1.0F, "a mob's dagger killed an open Original: " + m.getHealth());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOpenOneCannotBeFinishedByAnythingButWhiteOak(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.desiccate();
            m.hurt(rig.plainBlow(), 10000F);
            helper.assertTrue(m.isAlive() && m.getHealth() == 1.0F, "an open Original died to something that is not white oak");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heGetsUpAfterTheDaysAndNotBefore(GameTestHelper helper) {
        LineTestPlayer child = member(helper, false);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("original_return_days", 5);
            online(child);
            rig.day(10);
            MikaelEntity m = rig.spawn();
            m.desiccate();
            rig.day(14);
            m.checkReturn();
            helper.assertTrue(m.isDesiccated(), "he got up a day early");
            rig.day(15);
            m.checkReturn();
            helper.assertTrue(!m.isDesiccated() && m.getHealth() == m.getMaxHealth() && !m.isNoAi(), "he did not get up on the day");
            helper.assertTrue(!m.getData(SpeciesRegistries.NPC_OPEN) && rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "he is still open in the books");
            // the line is warned again the next time he goes down
            int before = child.count(HIDDEN);
            m.desiccate();
            helper.assertTrue(child.count(HIDDEN) == before + 1, "the line was not warned a second time");
        } finally {
            forget(helper, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisBloodDropsAtMostOncePerWindow(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            rig.day(10);
            MikaelEntity m = rig.spawn();
            m.desiccate();
            helper.assertTrue(rig.bloodBottles() == 1, "no bottle of his blood dropped: " + rig.bloodBottles());
            m.recover();
            m.desiccate();
            helper.assertTrue(rig.bloodBottles() == 1, "a second bottle dropped inside the window: " + rig.bloodBottles());
            m.recover();
            rig.day(16);
            m.desiccate();
            helper.assertTrue(rig.bloodBottles() == 2, "no bottle dropped in the next window: " + rig.bloodBottles());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRewardDropsOnceAndNotAgainAfterAReturn(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.desiccate();
            helper.assertTrue(rig.dropsOf(BossItems.MIKAELS_STAKE.get()) == 1, "no reward on the first defeat: " + rig.dropsOf(BossItems.MIKAELS_STAKE.get()));
            m.recover();
            m.desiccate();
            helper.assertTrue(rig.dropsOf(BossItems.MIKAELS_STAKE.get()) == 1, "the reward dropped a second time");
            helper.assertTrue(rig.index.of(BossId.MIKAEL).rewarded, "the index does not remember it was paid");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withRewardOnDeathNothingDropsUntilHeDies(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("boss_reward_on", "death");
            MikaelEntity m = rig.spawn();
            m.desiccate();
            helper.assertTrue(rig.dropsOf(BossItems.MIKAELS_STAKE.get()) == 0, "the reward dropped on desiccation with boss_reward_on = death");
            m.hurt(rig.whiteOakBlow(), 10000F);
            helper.assertTrue(rig.dropsOf(BossItems.MIKAELS_STAKE.get()) == 1, "the reward did not drop on his death");
        } finally {
            BossesConfig.override("boss_reward_on", null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theImprisonedOriginalRiteSeesHimAsAnOriginalAndAsDownOnlyWhenDesiccated(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            var centre = m.blockPosition();
            helper.assertTrue(Imprisonment.trappedRoot(helper.getLevel(), centre).filter(BossTestRig.MIKAEL::equals).isPresent(), "the rite does not find him on the circle");
            helper.assertTrue(!Imprisonment.isDown(m), "a standing Original counts as brought down");
            m.desiccate();
            helper.assertTrue(Imprisonment.isDown(m), "a desiccated Original does not count as brought down");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMountainAshLineStopsHimAndTheLineRulesLeaveHimAlone(GameTestHelper helper) {
        TagKey<EntityType<?>> mobs = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(NS, "supernatural_mobs"));
        TagKey<EntityType<?>> exempt = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(NS, "line_exempt"));
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            helper.assertTrue(m.getType().is(mobs), "mountain ash would not stop him: he is not in #supernatural_mobs");
            helper.assertTrue(m.getType().is(exempt), "line death or an NPC line could reach him: he is not in #line_exempt");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisStakeIsWhiteOakAndNeverBreaks(GameTestHelper helper) {
        var stake = new net.minecraft.world.item.ItemStack(BossItems.MIKAELS_STAKE.get());
        helper.assertTrue(stake.is(net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak.TAG), "his stake is not in #white_oak");
        helper.assertTrue(stake.has(net.minecraft.core.component.DataComponents.UNBREAKABLE), "his stake can break");
        helper.succeed();
    }
}
