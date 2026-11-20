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
import net.ofatech.supernaturalbloodlines.parts.bosses.item.CloakEvents;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.LinkIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.Links;
import net.ofatech.supernaturalbloodlines.parts.bosses.rite.CloakingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.bosses.rite.LinkingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
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
            // the compass is the artifacts part's: without it only the hunter's sense is tested
            boolean compassOn = net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "vampire_compass"));
            ItemStack compass = compassOn ? new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get()) : ItemStack.EMPTY;
            vampire.setPos(helper.absoluteVec(new Vec3(12.5, 1, 12.5)));
            helper.getLevel().addNewPlayer(vampire);
            if (compassOn) {
                VampireCompassItem.update(compass, helper.getLevel(), hunter);
                Optional<GlobalPos> before = target(compass);
                helper.assertTrue(before != null && before.isPresent() && before.get().pos().equals(caster.blockPosition()), "control: the compass does not find the caster first: " + before);
            }
            helper.assertTrue(HunterInstinct.senses(hunter, caster), "control: the hunter does not sense the caster");

            RiteTestSupport.start(helper, "rite_of_cloaking", caster, cloakItems(true));
            helper.assertTrue(CloakAccess.cloaked(caster), "the rite did not cloak the caster: " + caster.heard());
            helper.assertTrue(CloakAccess.until(caster) == caster.level().getGameTime() + BossesConfig.kolCloakMinutes() * 1200L, "the cloak does not last the setting: " + CloakAccess.until(caster));
            helper.assertTrue(caster.receivedOnActionBar(CloakingRiteFactory.DONE), "the caster was not told");
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:kol_grimoire") == 1, "the grimoire was not given back");
            if (compassOn) {
                VampireCompassItem.update(compass, helper.getLevel(), hunter);
                Optional<GlobalPos> after = target(compass);
                helper.assertTrue(after != null && after.isPresent() && after.get().pos().equals(vampire.blockPosition()), "the compass still points at the cloaked caster: " + after);
            }
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

    private static final String WONT_HOLD = LinkRules.WONT_HOLD;

    /** A witch performing the rite from outside the circle (so she is not one of the two), with her messages recorded. */
    private static RecordingPlayer witch(GameTestHelper helper) {
        RecordingPlayer c = RecordingPlayer.create(helper);
        BloodlineAccess.set(c, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        RiteTestSupport.place(helper, c);
        BlockPos at = RiteTestSupport.centre(helper);
        c.setPos(at.getX() + 0.5 + LinkingRiteFactory.RADIUS + 3, at.getY() + 1, at.getZ() + 0.5);
        return c;
    }

    private static Cow cow(GameTestHelper helper, int x, int y, int z) {
        return helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(x, y, z));
    }

    private static Sheep sheep(GameTestHelper helper, int x, int y, int z) {
        return helper.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(x, y, z));
    }

    /** What a refusal does, every time: one message, the same one; nothing linked; the lead and the amethyst gone; the grimoire back. */
    private static void assertRefusedTheSameWay(GameTestHelper helper, RecordingPlayer caster, int refusals, String why) {
        assertRefusedTheSameWay(helper, caster, refusals, why, 0);
    }

    private static void assertRefusedTheSameWay(GameTestHelper helper, RecordingPlayer caster, int refusals, String why, int links) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        helper.assertTrue(caster.count(WONT_HOLD) == refusals, why + ": the answer was not given exactly once more: " + caster.heard());
        helper.assertTrue(caster.heard().replace(WONT_HOLD, "").replace(",", "").isBlank(), why + ": the caster was told something else too: " + caster.heard());
        helper.assertTrue(index.count() == links, why + ": a link was made: " + index.count());
        helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:esther_grimoire") == 1, why + ": the grimoire did not come back");
        helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:lead") == 0 && RiteTestSupport.dropped(helper, "minecraft:amethyst_shard") == 0,
                why + ": the refusal was free: the ingredients came back");
        RiteTestSupport.clearDropped(helper);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyRefusalOfTheLinkingSpellIsTheSameMessageAndCostsTheSameSoItNeverNamesAnOriginalOrABoss(GameTestHelper helper) {
        RecordingPlayer caster = witch(helper);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try (BossTestRig rig = new BossTestRig(helper)) {
            index.clear();
            int n = 0;
            // none, one, three
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "no creature");
            Cow one = cow(helper, 7, 1, 7);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "one creature");
            Sheep two = sheep(helper, 9, 1, 7);
            Zombie three = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(8, 1, 9));
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "three creatures");
            three.discard();
            // an NPC Original and a boss of ours
            var elijah = rig.spawnElijah();
            elijah.moveTo(helper.absoluteVec(new Vec3(8.5, 1, 9.5)));
            two.discard();
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "an NPC Original");
            elijah.discard();
            var kol = rig.spawnKol();
            kol.moveTo(helper.absoluteVec(new Vec3(8.5, 1, 9.5)));
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "a boss");
            kol.discard();
            // a boss that is not one of ours and not an Original: the Wither, which cannot change dimensions and is in the boss tag
            var wither = helper.spawnWithNoFreeWill(EntityType.WITHER, new BlockPos(8, 1, 9));
            wither.setNoAi(true);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "the Wither");
            wither.discard();
            // a player Original, standing in the circle (sneaking: she would consent, but she is an Original)
            RecordingPlayer original = RecordingPlayer.create(helper);
            BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            RiteTestSupport.place(helper, original);
            original.setShiftKeyDown(true);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "a player Original");
            RiteTestSupport.remove(helper, original);
            // already linked: the circle holds a cow that is linked to something far away, and a free sheep
            Sheep partner = sheep(helper, 9, 1, 7);
            Links.link(helper.getLevel().getServer(), one.getUUID(), java.util.UUID.randomUUID(), 60);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "an already linked creature", 1);
            index.clear();
            partner.discard();
            // another player who does not agree, and one who does but where PvP is off
            LineTestPlayer other = LineTestPlayer.create(helper);
            RiteTestSupport.place(helper, other);
            boolean pvp = helper.getLevel().getServer().isPvpAllowed();
            helper.getLevel().getServer().setPvpAllowed(true); // so only the sneaking decides
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            assertRefusedTheSameWay(helper, caster, ++n, "a player who is not sneaking");
            other.setShiftKeyDown(true);
            helper.getLevel().getServer().setPvpAllowed(false);
            try {
                RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
                assertRefusedTheSameWay(helper, caster, ++n, "a player where PvP is off");
            } finally {
                helper.getLevel().getServer().setPvpAllowed(pvp);
            }
            RiteTestSupport.remove(helper, other);
            one.discard();
        } finally {
            index.clear();
            RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerWhoSneaksInTheCircleAgreesAndIsLinkedAndToldAndToldAgainWhenItEnds(GameTestHelper helper) {
        RecordingPlayer caster = witch(helper);
        RecordingPlayer other = RecordingPlayer.create(helper);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(true);
        try {
            index.clear();
            RiteTestSupport.place(helper, other);
            other.setShiftKeyDown(true);
            Cow a = cow(helper, 7, 1, 7);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(index.linked(other.getUUID(), now) && index.linked(a.getUUID(), now), "the sneaking player was not linked: " + caster.heard());
            helper.assertTrue(index.of(other.getUUID()).orElseThrow().isPlayer(other.getUUID()) && !index.of(a.getUUID()).orElseThrow().isPlayer(a.getUUID()), "the link does not know who is a player");
            helper.assertTrue(other.received(Links.LINKED), "the linked player was not told: " + other.heard());
            helper.assertTrue(caster.received(LinkingRiteFactory.DONE), "the witch was not told");
            Links.expire(helper.getLevel().getServer(), helper.getLevel().getServer().overworld().getGameTime() + 61 * 20);
            helper.assertTrue(other.received(Links.ENDED) && index.count() == 0, "the player was not told the link ended: " + other.heard());
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            index.clear();
            RiteTestSupport.remove(helper, other);
            RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothLinkedPlayersAreToldWhicheverSideTheyAreOn(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer other = RecordingPlayer.create(helper);
        BloodlineAccess.set(caster, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        RiteTestSupport.place(helper, caster);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(true);
        try {
            index.clear();
            RiteTestSupport.place(helper, other);
            other.setShiftKeyDown(true);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(index.count() == 1, "the two players were not linked: " + caster.heard());
            helper.assertTrue(caster.received(Links.LINKED) && other.received(Links.LINKED), "one of the two linked players was not told: " + caster.heard() + " / " + other.heard());
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            index.clear();
            RiteTestSupport.remove(helper, other);
            RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWitchCanLinkHerselfToACreatureByStandingInTheCircle(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        BloodlineAccess.set(caster, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        RiteTestSupport.place(helper, caster);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = cow(helper, 7, 1, 7);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(index.linked(caster.getUUID(), now) && index.linked(a.getUUID(), now), "she could not link herself to a creature: " + caster.heard());
            helper.assertTrue(caster.received(Links.LINKED), "she was not told she is linked");
        } finally {
            index.clear();
            RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyAWitchTypeCanWorkTheLinkingSpellAndAPlainPlayerGetsEverythingBack(GameTestHelper helper) {
        RecordingPlayer plain = RecordingPlayer.create(helper);
        RiteTestSupport.place(helper, plain);
        BlockPos at = RiteTestSupport.centre(helper);
        plain.setPos(at.getX() + 0.5 + LinkingRiteFactory.RADIUS + 3, at.getY() + 1, at.getZ() + 0.5);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            cow(helper, 7, 1, 7);
            sheep(helper, 9, 1, 7);
            RiteTestSupport.start(helper, "rite_of_linking", plain, linkItems());
            helper.assertTrue(plain.receivedOnActionBar(LinkRules.NOT_WITCH), "the plain player was not told why: " + plain.heard());
            helper.assertTrue(index.count() == 0, "a plain player linked two creatures");
            helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:lead") == 2 && RiteTestSupport.dropped(helper, "supernaturalbloodlines:esther_grimoire") == 1,
                    "the ingredients were not all given back: a refusal about the performer says nothing about the circle");
            helper.assertTrue(plain.count(WONT_HOLD) == 0, "the plain player got the circle's answer");
        } finally {
            index.clear();
            RiteTestSupport.remove(helper, plain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCircleIsFlatAndSlimNotACubeAHighCreatureAndAFarOneAreNotInIt(GameTestHelper helper) {
        RecordingPlayer caster = witch(helper);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = cow(helper, 7, 1, 7);
            Sheep b = sheep(helper, 9, 1, 7);
            Cow high = cow(helper, 8, 6, 8); // five blocks above the circle
            Cow far = cow(helper, 15, 1, 8); // seven blocks along it, beyond the chalk
            RiteTestSupport.centre(helper); // (the corner of the square that holds the circle is checked below)
            high.setNoGravity(true);
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(index.linked(a.getUUID(), now) && index.linked(b.getUUID(), now), "the two in the circle were not linked: " + caster.heard());
            helper.assertTrue(!index.linked(high.getUUID(), now) && !index.linked(far.getUUID(), now), "a creature outside the circle was counted");
            index.clear();
            far.discard();
            BlockPos centre = RiteTestSupport.centre(helper);
            Cow corner = helper.spawnWithNoFreeWill(EntityType.COW, helper.absolutePos(new BlockPos(0, 0, 0)).equals(centre) ? new BlockPos(5, 1, 5) : new BlockPos(centre.getX() - helper.absolutePos(BlockPos.ZERO).getX() + 5, 1, centre.getZ() - helper.absolutePos(BlockPos.ZERO).getZ() + 5));
            helper.assertTrue(corner.distanceToSqr(centre.getX() + 0.5, corner.getY(), centre.getZ() + 0.5) > LinkingRiteFactory.RADIUS * LinkingRiteFactory.RADIUS, "control: the corner cow is inside the circle");
            RiteTestSupport.start(helper, "rite_of_linking", caster, linkItems());
            helper.assertTrue(!index.linked(corner.getUUID(), helper.getLevel().getGameTime()), "a creature in the corner of the square, outside the round circle, was linked");
            helper.assertTrue(caster.count(WONT_HOLD) == 0, "the creatures above and beyond the circle made it refuse");
        } finally {
            index.clear();
            RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSharedHitNeverSkipsAPlayersOwnHitCooldownButDoesForACreature(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        player.setPos(helper.absoluteVec(new Vec3(5.5, 1, 5.5)));
        helper.getLevel().addNewPlayer(player);
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = cow(helper, 7, 1, 7);
            Sheep s = sheep(helper, 9, 1, 7);
            Links.link(helper.getLevel().getServer(), a, player, 60);
            player.hurt(helper.getLevel().damageSources().generic(), 6.0F); // a hit of its own: now it has a hit cooldown
            index.clear();
            Links.link(helper.getLevel().getServer(), a, player, 60);
            float before = player.getHealth();
            a.invulnerableTime = 0;
            a.hurt(helper.getLevel().damageSources().generic(), 4.0F); // a share of 2 lands inside the player's cooldown of a 6 hit
            helper.assertTrue(player.getHealth() == before, "the share went through the player's own hit cooldown: " + before + " to " + player.getHealth());
            index.clear();
            a.setHealth(a.getMaxHealth());
            Links.link(helper.getLevel().getServer(), a, s, 60);
            s.hurt(helper.getLevel().damageSources().generic(), 2.0F); // the sheep has a hit cooldown of its own now
            a.setHealth(a.getMaxHealth());
            float sheepBefore = s.getHealth();
            a.invulnerableTime = 0;
            a.hurt(helper.getLevel().damageSources().generic(), 4.0F);
            helper.assertTrue(s.getHealth() < sheepBefore, "the share did not reach a creature inside its cooldown");
        } finally {
            index.clear();
            helper.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nothingIsEverOwedByAPlayerWhoIsNotLoadedAndACreatureOwesAtMostHalfItsHealthWhenItLoads(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = cow(helper, 7, 1, 7);
            java.util.UUID offlinePlayer = java.util.UUID.randomUUID();
            index.add(a.getUUID(), offlinePlayer, helper.getLevel().getServer().overworld().getGameTime() + 1200, false, true);
            for (int i = 0; i < 5; i++) {
                a.invulnerableTime = 0;
                a.hurt(helper.getLevel().damageSources().generic(), 2.0F);
                a.setHealth(a.getMaxHealth());
            }
            helper.assertTrue(index.settle(offlinePlayer) == 0F, "damage was stored for a player who was not loaded");
            index.clear();
            java.util.UUID offlineCow = java.util.UUID.randomUUID();
            index.add(a.getUUID(), offlineCow, helper.getLevel().getServer().overworld().getGameTime() + 1200, false, false);
            for (int i = 0; i < 5; i++) {
                a.invulnerableTime = 0;
                a.hurt(helper.getLevel().damageSources().generic(), 2.0F);
                a.setHealth(a.getMaxHealth());
            }
            helper.assertTrue(index.settle(offlineCow) > 0F, "nothing was stored for a creature that was not loaded");
        } finally {
            index.clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCreatureThatLoadsTakesAtMostHalfItsMaximumHealthOfWhatItOwed(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        index.clear();
        Cow cow = EntityType.COW.create(helper.getLevel());
        index.owe(cow.getUUID(), 20.0F);
        cow.moveTo(helper.absoluteVec(new Vec3(7.5, 1, 7.5)));
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        helper.runAfterDelay(5, () -> {
            float lost = cow.getMaxHealth() - cow.getHealth();
            helper.assertTrue(lost > 0F && lost <= cow.getMaxHealth() * 0.5F + 0.01F, "the cow lost " + lost + " of " + cow.getMaxHealth() + ", not at most half");
            index.clear();
            cow.discard();
            helper.succeed();
        });
    }

    // ---- the cloak runs out ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCloakThatRanOutIsAnnouncedOnceAndOneStillRunningIsNot(GameTestHelper helper) {
        RecordingPlayer ran = RecordingPlayer.create(helper);
        RecordingPlayer running = RecordingPlayer.create(helper);
        RecordingPlayer none = RecordingPlayer.create(helper);
        long now = helper.getLevel().getGameTime();
        CloakAccess.set(ran, now - 1);
        CloakAccess.set(running, now + 5000);
        try {
            CloakEvents.expire(List.of(ran, running, none), now);
            helper.assertTrue(ran.receivedOnActionBar(CloakEvents.FADED), "nobody was told the cloak faded");
            helper.assertTrue(CloakAccess.until(ran) == Cloak.NONE, "the faded cloak was not forgotten");
            helper.assertTrue(!running.received(CloakEvents.FADED) && CloakAccess.cloaked(running), "a running cloak was announced or ended");
            helper.assertTrue(!none.received(CloakEvents.FADED), "somebody who never had a cloak was told it faded");
            CloakEvents.expire(List.of(ran), now + 10);
            helper.assertTrue(ran.count(CloakEvents.FADED) == 1, "the fading was announced twice");
        } finally {
            CloakAccess.set(running, Cloak.NONE);
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
    public static void anExpiredLinkSharesNothingAndIsRemovedByTheNextHit(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 1, 7));
            Sheep b = helper.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(9, 1, 7));
            index.add(a.getUUID(), b.getUUID(), helper.getLevel().getServer().overworld().getGameTime() - 1);
            float before = b.getHealth();
            a.hurt(helper.getLevel().damageSources().generic(), 4.0F);
            helper.assertTrue(b.getHealth() == before, "an expired link still shared damage");
            helper.assertTrue(index.count() == 0, "an expired link was not removed by the hit");
        } finally {
            index.clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLinkEndsWhenOneOfTheTwoTravelsToAnotherDimension(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        try {
            index.clear();
            Cow a = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 1, 7));
            Sheep b = helper.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(9, 1, 7));
            Links.link(helper.getLevel().getServer(), a.getUUID(), b.getUUID(), 60);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent(a, net.minecraft.world.level.Level.NETHER));
            helper.assertTrue(index.count() == 0, "the link outlived a trip to another dimension");
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
            Links.link(helper.getLevel().getServer(), a.getUUID(), away, 60); // a creature that is not loaded
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
    public static void whatAnUnloadedPartnerOwesIsDealtWhenItJoinsTheLevel(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        index.clear();
        Cow cow = EntityType.COW.create(helper.getLevel());
        index.owe(cow.getUUID(), 3.0F);
        cow.moveTo(helper.absoluteVec(new Vec3(7.5, 1, 7.5)));
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(cow.getHealth() < cow.getMaxHealth(), "the owed damage was not dealt when it joined: " + cow.getHealth());
            helper.assertTrue(index.settle(cow.getUUID()) == 0F, "it still owes after paying");
            index.clear();
            cow.discard();
            helper.succeed();
        });
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
