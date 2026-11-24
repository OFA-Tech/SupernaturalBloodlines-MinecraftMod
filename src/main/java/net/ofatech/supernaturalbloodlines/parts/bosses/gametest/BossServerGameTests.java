package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.DealRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;

/**
 * Plan 17b, "U6": the boss entities load and run on a dedicated server (this run has no client classes at all, so any reference to one from an entity,
 * a goal, the registries or an item would fail here), and the world's record of them survives a save, a restart and a damaged entry.
 */
@PrefixGameTestTemplate(false)
public final class BossServerGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BossServerGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBossEntitiesLoadAndTickOnADedicatedServer(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist == Dist.DEDICATED_SERVER, "this run is " + FMLEnvironment.dist + ", not a dedicated server");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "mikael")), "the entity type is not registered");
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            for (int i = 0; i < 25; i++) {
                m.tick();
            }
            helper.assertTrue(m.isAlive() && m.getType() == BossRegistries.MIKAEL.get(), "the entity did not survive its first second");
            helper.assertTrue(m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) > 0, "it has no attributes");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void klausAndHisGeckoLibFormLoadAndRunOnADedicatedServerWhereTheRendererCannotLoad(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist == Dist.DEDICATED_SERVER, "this run is " + FMLEnvironment.dist + ", not a dedicated server");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "klaus")), "the entity type is not registered");
        // the control: no client class is here, so a boss class that named one could not have loaded
        helper.assertTrue(!loads("net.minecraft.client.Minecraft"), "client classes are present in this run, so it proves nothing");
        helper.assertTrue(!loads("software.bernie.geckolib.renderer.GeoEntityRenderer"), "GeckoLib's renderer loads on the server: the proof needs a run without client classes");
        helper.assertTrue(!loads("net.ofatech.supernaturalbloodlines.parts.bosses.client.ShiftRenderer"), "the shift renderer loads on the server");
        helper.assertTrue(loads("software.bernie.geckolib.animatable.GeoEntity"), "GeckoLib's common animation API is not here");
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            for (int i = 0; i < 25; i++) {
                k.tick();
            }
            helper.assertTrue(k.isAlive() && k.getType() == BossRegistries.KLAUS.get(), "the entity did not survive its first second");
            k.startShift();
            for (int i = 0; i < ShiftRules.SHIFT_TICKS + 5; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.BEAST, "the shift did not run on a server: " + k.form());
            helper.assertTrue(k.getAnimatableInstanceCache() != null && k.getAnimatableInstanceCache().getManagerForId(k.getId()) != null, "GeckoLib's animation manager is not there");
            k.doHurtTarget(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class, k.getBoundingBox().inflate(20)).stream().findFirst()
                    .orElseGet(() -> helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(5, 1, 5)))); // the attack animation is triggered on a server too
            CompoundTag tag = new CompoundTag();
            k.saveWithoutId(tag);
            KlausEntity again = KlausEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.form() == BossForm.BEAST, "the form was lost through a save");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deucalionAndHisDemonWolfLoadAndRunOnADedicatedServerWhereTheRendererAndTheGlassesCannotLoad(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist == Dist.DEDICATED_SERVER, "this run is " + FMLEnvironment.dist + ", not a dedicated server");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "deucalion")), "the entity type is not registered");
        helper.assertTrue(!loads("net.minecraft.client.Minecraft"), "client classes are present in this run, so it proves nothing");
        helper.assertTrue(!loads("net.ofatech.supernaturalbloodlines.parts.bosses.client.ShiftRenderer") && !loads("net.ofatech.supernaturalbloodlines.parts.bosses.client.HumanoidBossRenderer"),
                "a renderer loads on the server");
        try (BossTestRig rig = new BossTestRig(helper)) {
            net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector.useMods(b -> true);
            var d = net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            for (int i = 0; i < 25; i++) {
                d.tick();
            }
            helper.assertTrue(d.isAlive() && d.getType() == BossRegistries.DEUCALION.get(), "the entity did not survive its first second");
            helper.assertTrue(d.hatVisible(), "the glasses are not drawn for the man (the rule is common code, the layer is the client's)");
            d.startShift();
            for (int i = 0; i < ShiftRules.SHIFT_TICKS + 5; i++) {
                d.tick();
            }
            helper.assertTrue(d.form() == BossForm.BEAST && !d.hatVisible(), "the shift did not run on a server: " + d.form());
            helper.assertTrue(d.getAnimatableInstanceCache() != null && d.getAnimatableInstanceCache().getManagerForId(d.getId()) != null, "GeckoLib's animation manager is not there");
            CompoundTag tag = new CompoundTag();
            d.saveWithoutId(tag);
            var again = net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.form() == BossForm.BEAST, "the form was lost through a save");
            d.dismissPack();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void peterAndHisAlphaAndTheHouseLoadAndRunOnADedicatedServerWhereTheRendererCannotLoad(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist == Dist.DEDICATED_SERVER, "this run is " + FMLEnvironment.dist + ", not a dedicated server");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "peter_hale")), "the entity type is not registered");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "memory_claws")), "the Memory Claws are not registered");
        helper.assertTrue(BuiltInRegistries.STRUCTURE_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "hale_house")), "the structure type is not registered");
        helper.assertTrue(!loads("net.minecraft.client.Minecraft"), "client classes are present in this run, so it proves nothing");
        helper.assertTrue(!loads("net.ofatech.supernaturalbloodlines.parts.bosses.client.ShiftRenderer") && !loads("net.ofatech.supernaturalbloodlines.parts.bosses.client.HumanoidBossRenderer"),
                "a renderer loads on the server");
        try (BossTestRig rig = new BossTestRig(helper)) {
            var p = net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector.spawnPeterAt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            for (int i = 0; i < 25; i++) {
                p.tick();
            }
            helper.assertTrue(p.isAlive() && p.getType() == BossRegistries.PETER_HALE.get(), "the entity did not survive its first second");
            p.startShift();
            for (int i = 0; i < ShiftRules.SHIFT_TICKS + 5; i++) {
                p.tick();
            }
            helper.assertTrue(p.form() == BossForm.BEAST, "the shift did not run on a server: " + p.form());
            helper.assertTrue(p.getAnimatableInstanceCache() != null && p.getAnimatableInstanceCache().getManagerForId(p.getId()) != null, "GeckoLib's animation manager is not there");
            CompoundTag tag = new CompoundTag();
            p.saveWithoutId(tag);
            var again = net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterHaleEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.form() == BossForm.BEAST, "the form was lost through a save");
            p.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostTheBeastAndTheSpiritLoadAndRunOnADedicatedServerWhereTheRenderersCannotLoad(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist == Dist.DEDICATED_SERVER, "this run is " + FMLEnvironment.dist + ", not a dedicated server");
        for (String id : new String[] {"gevaudan_host", "beast_of_gevaudan", "gevaudan_spirit"}) {
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath(NS, id)), "the entity type " + id + " is not registered");
        }
        for (String id : new String[] {"valet_pike", "mantle_of_the_beast", "sebastien_tricorn"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(NS, id)), "the item " + id + " is not registered");
        }
        helper.assertTrue(!loads("net.minecraft.client.Minecraft"), "client classes are present in this run, so it proves nothing");
        for (String client : new String[] {"ShiftRenderer", "SpiritRenderer", "HumanoidBossRenderer", "MorphPlayerModel"}) {
            helper.assertTrue(!loads("net.ofatech.supernaturalbloodlines.parts.bosses.client." + client), "the renderer " + client + " loads on the server");
        }
        helper.assertTrue(loads("net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector") && loads("net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEvents")
                && loads("net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastRoar") && loads("net.ofatech.supernaturalbloodlines.parts.bosses.item.MantleEvents"), "the server side of the Beast does not load");
        try (BossTestRig rig = new BossTestRig(helper)) {
            var host = net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector.spawnHost(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            for (int i = 0; i < 25; i++) {
                host.tick();
            }
            helper.assertTrue(host.isAlive() && host.getType() == BossRegistries.GEVAUDAN_HOST.get(), "the Host did not survive its first second");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector.change(helper.getLevel()), "the Host did not change on a server");
            var beast = (net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEntity) net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector.anyBossBody(rig.server, net.ofatech.supernaturalbloodlines.core.boss.BossId.BEAST_OF_GEVAUDAN);
            for (int i = 0; i < 25; i++) {
                beast.tick();
            }
            helper.assertTrue(beast.isAlive() && beast.getType() == BossRegistries.BEAST.get(), "the Beast did not survive its first second");
            helper.assertTrue(beast.getAnimatableInstanceCache() != null && beast.getAnimatableInstanceCache().getManagerForId(beast.getId()) != null, "GeckoLib's animation manager is not there");
            net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector.collapse(helper.getLevel(), beast);
            var spirit = helper.getLevel().getEntity(rig.index.of(net.ofatech.supernaturalbloodlines.core.boss.BossId.BEAST_OF_GEVAUDAN).spirit);
            helper.assertTrue(spirit != null && spirit.getType() == BossRegistries.GEVAUDAN_SPIRIT.get(), "the Spirit is not there");
            for (int i = 0; i < 25; i++) {
                spirit.tick();
            }
            CompoundTag tag = new CompoundTag();
            beast.saveWithoutId(tag);
            var again = net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.collapsing(), "the collapse was lost through a save");
            net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector.clear(rig.server);
            spirit.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void elijahRebekahAndTheDealLoadOnADedicatedServerWhereTheDialogCannot(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist == Dist.DEDICATED_SERVER, "this run is " + FMLEnvironment.dist + ", not a dedicated server");
        helper.assertTrue(!loads("net.minecraft.client.gui.screens.Screen"), "client classes are present in this run, so it proves nothing");
        helper.assertTrue(!loads("net.ofatech.supernaturalbloodlines.parts.bosses.client.ElijahDealScreen"), "the deal dialog loads on the server");
        helper.assertTrue(loads("net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals") && loads("net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload"),
                "the server side of the deal does not load");
        try (BossTestRig rig = new BossTestRig(helper)) {
            for (var boss : new net.minecraft.world.entity.LivingEntity[] {rig.spawnElijah(), rig.spawnRebekah()}) {
                for (int i = 0; i < 25; i++) {
                    boss.tick();
                }
                helper.assertTrue(boss.isAlive(), boss.getType() + " did not survive its first second on a server");
                CompoundTag tag = new CompoundTag();
                boss.saveWithoutId(tag);
                helper.assertTrue(!tag.isEmpty(), "nothing was saved");
            }
            // the deal's answer reaches the server through its own handler, with no client class in the way
            var elijah = net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector.bossEntity(rig.server, net.ofatech.supernaturalbloodlines.core.boss.BossId.ELIJAH);
            helper.assertTrue(elijah != null && elijah.getType() == BossRegistries.ELIJAH.get(), "Elijah is not the entity of his index entry");
        }
        helper.succeed();
    }

    private static boolean loads(String name) {
        try {
            Class.forName(name, false, BossServerGameTests.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError | RuntimeException absent) { // the dist cleaner refuses a client class with a RuntimeException
            return false;
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEntityRemembersItsPreyAndItsDesiccationThroughASave(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            UUID prey = UUID.randomUUID();
            m.hunt(prey);
            m.desiccate();
            CompoundTag tag = new CompoundTag();
            m.saveWithoutId(tag);
            MikaelEntity again = MikaelEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(prey.equals(again.prey()), "the prey was forgotten");
            helper.assertTrue(again.isDesiccated(), "the desiccation was forgotten");
            helper.assertTrue(again.isNoAi(), "a desiccated Mikael got his AI back through a save");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theIndexKeepsEveryFieldAndSkipsABadEntryNotTheFile(GameTestHelper helper) {
        BossIndex index = new BossIndex();
        BossIndex.Entry e = index.of(BossId.MIKAEL);
        UUID entity = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        e.state = BossState.OPEN;
        e.entity = entity;
        e.dimension = "minecraft:overworld";
        e.pos = new BlockPos(5, 64, -9);
        e.returnDay = 42;
        e.rewarded = true;
        e.appearances = 3;
        e.bloodDay = 40;
        e.lastRollDay = 17;
        e.target = target;
        e.arriveAt = 12345;
        e.reminded = 30;
        e.hostile = true;
        index.setOath(target, 77);
        index.setOath(entity, DealRules.BROKEN);
        var access = helper.getLevel().getServer().registryAccess();
        CompoundTag saved = index.save(new CompoundTag(), access);
        ListTag list = saved.getList("bosses", 10);
        CompoundTag bad = new CompoundTag();
        bad.putString("boss", "mikael_the_second");
        list.add(bad);
        CompoundTag worse = new CompoundTag();
        worse.putString("boss", "klaus");
        worse.putString("state", "NOT_A_STATE");
        list.add(worse);
        BossIndex back = BossIndex.load(saved, access);
        BossIndex.Entry r = back.of(BossId.MIKAEL);
        helper.assertTrue(r.state == BossState.OPEN && entity.equals(r.entity) && target.equals(r.target), "state or ids lost: " + r.state);
        helper.assertTrue(r.dimension.equals("minecraft:overworld") && r.pos.equals(new BlockPos(5, 64, -9)), "place lost: " + r.pos);
        helper.assertTrue(r.returnDay == 42 && r.rewarded && r.appearances == 3 && r.bloodDay == 40 && r.lastRollDay == 17, "numbers lost");
        helper.assertTrue(r.arriveAt == 12345, "the arrival that was put off was lost: " + r.arriveAt);
        helper.assertTrue(r.reminded == 30, "the last reminder shown was lost: " + r.reminded);
        helper.assertTrue(back.of(BossId.MIKAEL).hostile && !new BossIndex().of(BossId.ELIJAH).hostile, "Elijah's anger was lost, or a new world starts angry");
        helper.assertTrue(back.oathOf(target) == 77 && back.oathOf(entity) == DealRules.BROKEN && back.oathOf(UUID.randomUUID()) == DealRules.NEVER, "the oaths were lost: " + back.oathOf(target));
        helper.assertTrue(new BossIndex().of(BossId.KLAUS).arriveAt == -1, "a boss that has not been announced waits for an arrival");
        helper.assertTrue(back.state(BossId.KLAUS) == BossState.UNSEEN, "a bad entry was kept");
        helper.assertTrue(back.bossOf(entity).orElseThrow() == BossId.MIKAEL, "the entity is not found by its uuid");
        helper.succeed();
    }
}
