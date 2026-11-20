package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.Cloak;
import net.ofatech.supernaturalbloodlines.core.boss.CloakAccess;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.LinkIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.Links;
import net.ofatech.supernaturalbloodlines.parts.bosses.rite.CloakingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterInstinct;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/** The two grimoire rites (plan 17e, R172 and R177): who can work them, what they hide or share, and that neither can ever touch an Original. */
@PrefixGameTestTemplate(false)
public final class CloakLinkGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CloakLinkGameTests() {
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        p.setPos(helper.absoluteVec(new Vec3(3.5, 1, 3.5)));
        return p;
    }

    private static ItemStack read(boolean read) {
        ItemStack g = new ItemStack(BossItems.KOL_GRIMOIRE.get());
        if (read) {
            net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, g, t -> t.putBoolean("read", true));
        }
        return g;
    }

    private static List<ItemStack> cloakItems(boolean read) {
        return List.of(read(read), new ItemStack(net.minecraft.world.item.Items.PHANTOM_MEMBRANE, 2), new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));
    }

    private static List<ItemStack> linkItems() {
        return List.of(new ItemStack(BossItems.ESTHER_GRIMOIRE.get()), new ItemStack(net.minecraft.world.item.Items.LEAD, 2), new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD, 2));
    }

    private static Optional<GlobalPos> target(ItemStack compass) {
        var t = compass.get(DataComponents.LODESTONE_TRACKER);
        return t == null ? null : t.target();
    }

    // ---- the Cloaking rite (R172) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCloakingRiteHidesAWitchFromTheCompassAndTheHuntersSenseAndGivesTheGrimoireBack(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        ServerPlayer hunter = TestPlayers.createMortal(helper);
        ServerPlayer vampire = vampire(helper);
        // the faction first (it sets the species), then a heretic: a witch type a hunter can still sense (a plain witch is never prey, R81)
        helper.assertTrue(VampirismAPI.factionPlayerHandler(caster).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make the caster a vampire");
        BloodlineAccess.set(caster, BloodlineState.NONE.toBuilder().species(Species.HERETIC).witchLayer(true).build());
        RiteTestSupport.place(helper, caster);
        caster.setPos(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        try {
            BloodlineAccess.set(hunter, BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(8).build());
            hunter.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
            ItemStack compass = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
            vampire.setPos(helper.absoluteVec(new Vec3(12.5, 1, 12.5)));
            helper.getLevel().addNewPlayer(vampire);
            VampireCompassItem.update(compass, helper.getLevel(), hunter);
            Optional<GlobalPos> before = target(compass);
            helper.assertTrue(before != null && before.isPresent() && before.get().pos().equals(caster.blockPosition()), "control: the compass does not find the caster first: " + before);
            helper.assertTrue(HunterInstinct.senses(hunter, caster), "control: the hunter does not sense the caster");

            RiteTestSupport.start(helper, "rite_of_cloaking", caster, cloakItems(true));
            helper.assertTrue(CloakAccess.cloaked(caster), "the rite did not cloak the caster: " + caster.heard());
            helper.assertTrue(CloakAccess.until(caster) == caster.level().getGameTime() + BossesConfig.kolCloakMinutes() * 1200L, "the cloak does not last the setting: " + CloakAccess.until(caster));
            helper.assertTrue(caster.receivedOnActionBar(CloakingRiteFactory.DONE), "the caster was not told");
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:kol_grimoire") == 1, "the grimoire was not given back");
            VampireCompassItem.update(compass, helper.getLevel(), hunter);
            Optional<GlobalPos> after = target(compass);
            helper.assertTrue(after != null && after.isPresent() && after.get().pos().equals(vampire.blockPosition()), "the compass still points at the cloaked caster: " + after);
            helper.assertTrue(!HunterInstinct.senses(hunter, caster), "the hunter senses the cloaked caster");
            helper.assertTrue(HunterInstinct.senses(hunter, vampire), "the hunter no longer senses a vampire who is not cloaked");
            // it ends
            CloakAccess.set(caster, caster.level().getGameTime() - 1);
            helper.assertTrue(!CloakAccess.cloaked(caster) && HunterInstinct.senses(hunter, caster), "the cloak did not end");
        } finally {
            CloakAccess.set(caster, Cloak.NONE);
            RiteTestSupport.remove(helper, caster);
            helper.getLevel().removePlayerImmediately(vampire, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCloakingRiteRefusesAPlainPlayerAndAGrimoireThatWasNotRead(GameTestHelper helper) {
        RecordingPlayer plain = RecordingPlayer.create(helper);
        RecordingPlayer witch = RecordingPlayer.create(helper);
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        RiteTestSupport.place(helper, plain);
        try {
            RiteTestSupport.start(helper, "rite_of_cloaking", plain, cloakItems(true));
            helper.assertTrue(plain.receivedOnActionBar(CloakingRiteFactory.NOT_WITCH), "the plain caster was not told why");
            helper.assertTrue(!CloakAccess.cloaked(plain), "a plain player was cloaked");
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:kol_grimoire") == 1 && RiteTestSupport.dropped(helper, "minecraft:ender_pearl") == 1, "the ingredients were not given back");
            RiteTestSupport.clearDropped(helper);
            RiteTestSupport.remove(helper, plain);
            RiteTestSupport.place(helper, witch);
            RiteTestSupport.start(helper, "rite_of_cloaking", witch, cloakItems(false));
            helper.assertTrue(witch.receivedOnActionBar(CloakingRiteFactory.UNREAD), "the witch was not told the grimoire is unread");
            helper.assertTrue(!CloakAccess.cloaked(witch), "an unread grimoire cloaked a witch");
        } finally {
            RiteTestSupport.remove(helper, witch);
        }
        helper.succeed();
    }

    // ---- the Linking Spell (R177) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLinkingSpellRefusesOneCreatureThreeAndAnyOriginalWithOneAnswerForTheLastTwo(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RiteTestSupport.place(helper, caster);
        BlockPos at = RiteTestSupport.centre(helper);
        try {
            Cow a = helper.getLevel().getEntitiesOfClass(Cow.class, new net.minecraft.world.phys.AABB(at).inflate(5)).stream().findFirst().orElse(null);
            helper.assertTrue(a == null, "control: the test cell is not empty");
            // none
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(caster.receivedOnActionBar(LinkRules.NEEDS_TWO), "no creature: " + caster.heard());
            RiteTestSupport.clearDropped(helper);
            // one
            Cow one = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 1, 7));
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(caster.count(LinkRules.NEEDS_TWO) == 2, "one creature was accepted");
            RiteTestSupport.clearDropped(helper);
            // three
            Sheep two = helper.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(9, 1, 7));
            Zombie three = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(8, 1, 9));
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(caster.count(LinkRules.NEEDS_TWO) == 3, "three creatures were accepted");
            helper.assertTrue(LinkIndex.get(helper.getLevel().getServer()).count() == 0, "a link was made from a refused circle");
            RiteTestSupport.clearDropped(helper);
            three.discard();
            two.discard();
            // an NPC Original, and a player Original: one answer
            var elijah = net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity.create(helper.getLevel());
            elijah.moveTo(helper.absoluteVec(new Vec3(8.5, 1, 9.5)));
            helper.getLevel().addFreshEntity(elijah);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(caster.count(LinkRules.WONT_HOLD) == 1, "an NPC Original in the circle was linked or answered differently: " + caster.heard());
            elijah.discard();
            RiteTestSupport.clearDropped(helper);
            RecordingPlayer original = RecordingPlayer.create(helper);
            BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            RiteTestSupport.place(helper, original);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(caster.count(LinkRules.WONT_HOLD) == 2 && caster.count(LinkRules.NEEDS_TWO) == 3, "a player Original in the circle was not refused the same way as an NPC one: " + caster.heard());
            RiteTestSupport.remove(helper, original);
            one.discard();
        } finally {
            RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLinkingSpellLinksTwoCreaturesAndGivesTheGrimoireBack(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RiteTestSupport.place(helper, caster);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 1, 7));
            Sheep b = helper.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(9, 1, 7));
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(index.linked(a.getUUID(), now) && index.linked(b.getUUID(), now), "the two are not linked");
            helper.assertTrue(index.of(a.getUUID()).orElseThrow().until() == now + BossesConfig.linkSeconds() * 20L, "the link does not last the setting");
            helper.assertTrue(caster.receivedOnActionBar("supernaturalbloodlines.rite.link_done"), "the caster was not told");
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:esther_grimoire") == 1, "the grimoire was not given back");
            // a creature that is linked already is not linked again
            RiteTestSupport.clearDropped(helper);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(caster.count(LinkRules.WONT_HOLD) == 1 && index.count() == 1, "a linked creature was linked a second time");
        } finally {
            index.clear();
            RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void linkedDamageIsSharedOnceAndNeverLoopsAndEndsWithADeathOrItsTime(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 1, 7));
            Sheep b = helper.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(9, 1, 7));
            a.setHealth(a.getMaxHealth());
            b.setHealth(b.getMaxHealth());
            Links.link(helper.getLevel().getServer(), a.getUUID(), b.getUUID(), 60);
            float bHealth = b.getHealth();
            a.hurt(helper.getLevel().damageSources().generic(), 4.0F);
            float lostA = a.getMaxHealth() - a.getHealth();
            float lostB = bHealth - b.getHealth();
            helper.assertTrue(Math.abs(lostA - 4.0F) < 0.01F, "the hurt one lost " + lostA);
            helper.assertTrue(Math.abs(lostB - 4.0F * (float) BossesConfig.linkDamageShare()) < 0.01F, "the partner lost " + lostB + ", not the share of 4");
            helper.assertTrue(Math.abs(a.getMaxHealth() - a.getHealth() - 4.0F) < 0.01F, "the damage came back round to the first: " + (a.getMaxHealth() - a.getHealth()));
            // the other way
            float aHealth = a.getHealth();
            b.invulnerableTime = 0;
            b.hurt(helper.getLevel().damageSources().generic(), 2.0F);
            helper.assertTrue(Math.abs((aHealth - a.getHealth()) - 2.0F * (float) BossesConfig.linkDamageShare()) < 0.01F, "the share did not go the other way");
            // a death ends it
            b.kill();
            helper.assertTrue(index.of(a.getUUID()).isEmpty(), "the link outlived a death");
            // time ends it
            Cow c = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 1, 9));
            Sheep d = helper.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(9, 1, 9));
            Links.link(helper.getLevel().getServer(), c.getUUID(), d.getUUID(), 5);
            index.expire(helper.getLevel().getServer().overworld().getGameTime() + 5 * 20 + 1);
            helper.assertTrue(index.count() == 0, "the link did not run out");
        } finally {
            index.clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPartnerThatWasNotLoadedTakesTheDamageWhenItComesBackAndAtMostTheCap(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 1, 7));
            java.util.UUID away = java.util.UUID.randomUUID();
            Links.link(helper.getLevel().getServer(), a.getUUID(), away, 60);
            for (int i = 0; i < 20; i++) {
                a.invulnerableTime = 0;
                a.hurt(helper.getLevel().damageSources().generic(), 2.0F);
                a.setHealth(a.getMaxHealth());
            }
            float owed = index.settle(away);
            helper.assertTrue(owed > 0F && owed <= LinkRules.MAX_PENDING + 0.01F, "the owed damage is " + owed + ", expected 0 < owed <= " + LinkRules.MAX_PENDING);
            helper.assertTrue(index.settle(away) == 0F, "the owed damage was not forgotten once settled");
        } finally {
            index.clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLinksAndWhatIsOwedSurviveASave(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            java.util.UUID a = java.util.UUID.randomUUID();
            java.util.UUID b = java.util.UUID.randomUUID();
            index.add(a, b, 99999L);
            index.owe(a, 3.5F);
            LinkIndex again = LinkIndex.load(index.save(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
            helper.assertTrue(again.of(a).isPresent() && again.of(b).orElseThrow().until() == 99999L, "the link was lost through a save");
            helper.assertTrue(Math.abs(again.settle(a) - 3.5F) < 0.01F, "what was owed was lost through a save");
        } finally {
            index.clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothRitesAreLoadedWithThePart(GameTestHelper helper) {
        for (String id : new String[] {"rite_of_cloaking", "rite_of_linking"}) {
            helper.assertTrue(RiteTestSupport.type(helper, id) != null, id + " is not loaded");
        }
        helper.succeed();
    }
}
