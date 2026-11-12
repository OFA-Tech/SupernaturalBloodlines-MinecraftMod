package net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.datamaps.IEntityBlood;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.data.existence.EntityExistenceData;
import io.github.manasmods.tensura.race.vampire.VampireRace;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.HolyDamageBridge;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSync;

/**
 * Registered by the part itself (RegisterGameTestsEvent), so these only exist when the part is enabled.
 * Not a @GameTestHolder: that would register them even when Vampirism or Tensura is missing.
 */
@PrefixGameTestTemplate(false)
public final class VampirismTensuraGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private VampirismTensuraGameTests() {
    }

    private static void setRace(net.minecraft.world.entity.LivingEntity e, String id) {
        RaceAPI.getRaceFrom(e).setRace(ResourceLocation.parse(id), false);
    }

    private static String race(net.minecraft.world.entity.LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static boolean isVampire(ServerPlayer p) {
        return handler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION;
    }

    private static String id(ManasRaceInstance r) {
        return r == null ? "none" : r.getRaceId().toString();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void raceHookOrderAndNamespace(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<String> seen = new ArrayList<>();
        RaceEvents.SetRaceEvent probe = (oldR, entity, newR, flag, changeable, message) -> {
            if (entity == p) {
                seen.add(id(oldR) + ">" + id(newR));
            }
            return EventResult.pass();
        };
        RaceEvents.SET_RACE.register(probe);
        try {
            setRace(p, "tensura:human");
            setRace(p, "tensura:elf");
        } finally {
            RaceEvents.SET_RACE.unregister(probe);
        }
        helper.assertTrue(List.of("none>tensura:human", "tensura:human>tensura:elf").equals(seen), "SET_RACE saw " + seen);
        helper.assertTrue("tensura:elf".equals(race(p)), "race after the hook was " + race(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingFactionReportsNullFaction(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<String> seen = new ArrayList<>();
        Consumer<PlayerFactionEvent.FactionLevelChanged> probe = ev -> {
            if (ev.getPlayer().getPlayer() == p) {
                seen.add(ev.getOldFaction() + ":" + ev.getOldLevel() + ">" + ev.getCurrentFaction() + ":" + ev.getNewLevel());
            }
        };
        NeoForge.EVENT_BUS.addListener(probe);
        try {
            handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
            handler(p).setFactionAndLevel(null, 0);
        } finally {
            NeoForge.EVENT_BUS.unregister(probe);
        }
        helper.assertTrue(seen.size() == 2, "FactionLevelChanged fired " + seen.size() + " times: " + seen);
        helper.assertTrue(seen.get(1).contains(">null:0"), "leaving reported " + seen.get(1));
        helper.assertTrue(seen.get(1).startsWith(String.valueOf(VReference.VAMPIRE_FACTION)), "old faction was " + seen.get(1));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void joiningVampireFactionKeepsRace(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:elf");
        helper.assertTrue(handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not join the vampire faction");
        helper.assertTrue(isVampire(p) && handler(p).getCurrentLevel() == 3, "faction/level changed");
        helper.assertTrue("tensura:elf".equals(race(p)), "race after joining was " + race(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void settingTensuraVampireLordJoinsAtSeven(GameTestHelper helper) {
        ServerPlayer lord = TestPlayers.create(helper);
        setRace(lord, "tensura:vampire_lord");
        helper.assertTrue(isVampire(lord) && handler(lord).getCurrentLevel() == 7,
                "vampire_lord: vampire=" + isVampire(lord) + " level=" + handler(lord).getCurrentLevel());

        ServerPlayer plain = TestPlayers.create(helper);
        setRace(plain, "tensura:vampire");
        helper.assertTrue(isVampire(plain) && handler(plain).getCurrentLevel() == 1,
                "vampire: level=" + handler(plain).getCurrentLevel());

        ServerPlayer divine = TestPlayers.create(helper);
        setRace(divine, "tensura:divine_vampire");
        int max = VReference.VAMPIRE_FACTION.getHighestReachableLevel();
        helper.assertTrue(isVampire(divine) && handler(divine).getCurrentLevel() == max,
                "divine_vampire: level=" + handler(divine).getCurrentLevel() + " max=" + max);

        ServerPlayer high = TestPlayers.create(helper);
        handler(high).setFactionAndLevel(VReference.VAMPIRE_FACTION, 10);
        setRace(high, "tensura:vampire_lord");
        helper.assertTrue(handler(high).getCurrentLevel() == 10, "a higher level was lowered to " + handler(high).getCurrentLevel());

        ServerPlayer ghoul = TestPlayers.create(helper);
        setRace(ghoul, "tensura:ghoul");
        helper.assertTrue(handler(ghoul).getCurrentFaction() == null, "ghoul joined a faction");

        ServerPlayer elf = TestPlayers.create(helper);
        setRace(elf, "tensura:elf");
        helper.assertTrue(handler(elf).getCurrentFaction() == null, "elf joined a faction");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hunterIsNotMadeAVampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        handler(p).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        setRace(p, "tensura:vampire_lord");
        helper.assertTrue(handler(p).getCurrentFaction() == VReference.HUNTER_FACTION && handler(p).getCurrentLevel() == 2,
                "a hunter was changed: " + handler(p).getCurrentFaction());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingFactionCuresRace(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:vampire_overcomer");
        handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 7);
        handler(p).setFactionAndLevel(null, 0);
        helper.assertTrue("tensura:human".equals(race(p)), "cured vampire-line race became " + race(p));

        ServerPlayer elf = TestPlayers.create(helper);
        setRace(elf, "tensura:elf");
        handler(elf).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
        handler(elf).setFactionAndLevel(null, 0);
        helper.assertTrue("tensura:elf".equals(race(elf)), "a cure overwrote a non-vampire race: " + race(elf));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void commandFactionChangeSyncsOnce(GameTestHelper helper) throws Exception {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:vampire_lord");
        handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 7);

        int[] calls = {0};
        RaceEvents.SetRaceEvent counter = (oldR, entity, newR, flag, changeable, message) -> {
            if (entity == p) {
                calls[0]++;
            }
            return EventResult.pass();
        };
        RaceEvents.SET_RACE.register(counter);
        try {
            var source = p.createCommandSourceStack().withPermission(4);
            helper.getLevel().getServer().getCommands().getDispatcher().execute("vampirism level none @s", source);
        } finally {
            RaceEvents.SET_RACE.unregister(counter);
        }
        helper.assertTrue(handler(p).getCurrentFaction() == null, "command did not remove the faction");
        helper.assertTrue(calls[0] == 1, "SET_RACE fired " + calls[0] + " times");
        helper.assertTrue("tensura:human".equals(race(p)), "race after the command was " + race(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void mobRaceChangeIgnored(GameTestHelper helper) {
        ServerPlayer bystander = TestPlayers.create(helper);
        ServerPlayer hunter = TestPlayers.create(helper);
        setRace(bystander, "tensura:elf");
        handler(bystander).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
        handler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        Mob mob = helper.spawn(EntityType.ZOMBIE, 1, 2, 1);

        int before = VampireSync.handledCount();
        setRace(mob, "tensura:vampire_lord");
        helper.assertTrue(VampireSync.handledCount() == before, "the sync acted on a mob race change");
        helper.assertTrue("tensura:vampire_lord".equals(race(mob)), "the mob race was not set: " + race(mob));
        helper.assertTrue(isVampire(bystander) && handler(bystander).getCurrentLevel() == 3, "a vampire player's level changed");
        helper.assertTrue("tensura:elf".equals(race(bystander)), "a player race changed: " + race(bystander));
        helper.assertTrue(handler(hunter).getCurrentFaction() == VReference.HUNTER_FACTION && handler(hunter).getCurrentLevel() == 2,
                "a hunter changed");

        // control: the same change on a player is counted, so the check above can fail
        setRace(hunter, "tensura:vampire_lord");
        helper.assertTrue(VampireSync.handledCount() == before + 1, "the sync did not count a player race change");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sameFactionRaceChangeRaisesLevel(GameTestHelper helper) {
        ServerPlayer lord = TestPlayers.create(helper);
        handler(lord).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
        setRace(lord, "tensura:vampire_lord");
        helper.assertTrue(isVampire(lord) && handler(lord).getCurrentLevel() == 7,
                "vampire_lord at level 3: vampire=" + isVampire(lord) + " level=" + handler(lord).getCurrentLevel());

        ServerPlayer overcomer = TestPlayers.create(helper);
        handler(overcomer).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
        setRace(overcomer, "tensura:vampire_overcomer");
        helper.assertTrue(isVampire(overcomer) && handler(overcomer).getCurrentLevel() == 7,
                "vampire_overcomer at level 3: vampire=" + isVampire(overcomer) + " level=" + handler(overcomer).getCurrentLevel());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void tensuraBurnIsOffOnlyForFactionVampires(GameTestHelper helper) {
        ServerPlayer faction = TestPlayers.create(helper);
        setRace(faction, "tensura:vampire");
        handler(faction).setFactionAndLevel(VReference.VAMPIRE_FACTION, 5);
        helper.assertTrue(!VampireRace.shouldBurn(faction), "a faction vampire still burns under Tensura");

        // a Tensura vampire who is in another faction (the sync never moves a hunter) keeps Tensura's burn
        ServerPlayer hunter = TestPlayers.create(helper);
        handler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        setRace(hunter, "tensura:vampire");
        helper.assertTrue(VampireRace.shouldBurn(hunter), "a Tensura-only vampire no longer burns");

        ServerPlayer none = TestPlayers.create(helper);
        helper.assertTrue(VampireRace.shouldBurn(none), "a player without a faction no longer burns");
        helper.succeed();
    }

    private static void tickSun(ServerPlayer p, int ticks) throws Exception {
        Object factionPlayer = handler(p).getCurrentFactionPlayer().orElse(null);
        Method vampirismUpdate = null;
        for (Class<?> c = factionPlayer == null ? null : factionPlayer.getClass(); c != null && vampirismUpdate == null; c = c.getSuperclass()) {
            try {
                vampirismUpdate = c.getDeclaredMethod("onUpdate");
                vampirismUpdate.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
                // keep climbing
            }
        }
        for (int i = 0; i < ticks; i++) {
            p.tickCount++;
            p.invulnerableTime = 0;
            if (vampirismUpdate != null) {
                vampirismUpdate.invoke(factionPlayer);
            }
            RaceAPI.getRaceFrom(p).getRace().ifPresent(inst -> inst.getRace().onTick(inst, p));
        }
    }

    private static List<String> sunTest(GameTestHelper helper, ServerPlayer p, List<String> damage) throws Exception {
        helper.getLevel().setDayTime(6000);
        p.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        helper.assertTrue(helper.getLevel().canSeeSky(p.blockPosition()), "no sky above the test player at " + p.blockPosition());
        Consumer<LivingIncomingDamageEvent> recorder = ev -> {
            if (ev.getEntity() == p) {
                damage.add(ev.getSource().getMsgId());
            }
        };
        NeoForge.EVENT_BUS.addListener(recorder);
        try {
            tickSun(p, 200);
        } finally {
            NeoForge.EVENT_BUS.unregister(recorder);
        }
        return damage;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void factionVampireTakesOnlyVampirismSunDamage(GameTestHelper helper) throws Exception {
        ServerPlayer faction = TestPlayers.createVulnerable(helper);
        setRace(faction, "tensura:vampire");
        handler(faction).setFactionAndLevel(VReference.VAMPIRE_FACTION, 10);
        List<String> damage = new ArrayList<>();
        sunTest(helper, faction, damage);
        helper.assertTrue(!damage.isEmpty(), "Vampirism's own sun damage never applied (hp=" + faction.getHealth() + ")");
        helper.assertTrue(damage.stream().allMatch("sun"::equals), "damage taken in the sun: " + damage);
        helper.assertTrue(faction.getRemainingFireTicks() <= 0, "Tensura set a faction vampire on fire");
        helper.assertTrue(!hasTensuraSunDebuff(faction), "Tensura's sun debuffs hit a faction vampire");

        // control: the same sun hurts a Tensura-only vampire the Tensura way, so the check above can fail
        ServerPlayer plain = TestPlayers.create(helper);
        handler(plain).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        setRace(plain, "tensura:vampire");
        sunTest(helper, plain, new ArrayList<>());
        helper.assertTrue(plain.getRemainingFireTicks() > 0 && hasTensuraSunDebuff(plain),
                "control: Tensura's burn did not run (fire=" + plain.getRemainingFireTicks() + ")");
        helper.succeed();
    }

    private static boolean hasTensuraSunDebuff(ServerPlayer p) {
        var slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return slow != null && slow.getAmplifier() == 3;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void undeadCannotJoinHunters(GameTestHelper helper) {
        for (String raceId : List.of("tensura:ghoul", "tensura:lesser_daemon")) {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, raceId);
            helper.assertTrue(!handler(p).canJoin(VReference.HUNTER_FACTION), raceId + " could join the hunters");
        }
        // control: the same query is allowed for a plain race, so the checks above can fail
        for (String raceId : List.of("tensura:human", "tensura:elf")) {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, raceId);
            helper.assertTrue(handler(p).canJoin(VReference.HUNTER_FACTION), "control: " + raceId + " could not join the hunters");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hunterBecomingUndeadLeavesFaction(GameTestHelper helper) {
        for (String raceId : List.of("tensura:ghoul", "tensura:lesser_daemon")) {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, "tensura:human");
            handler(p).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
            helper.assertTrue(handler(p).getCurrentFaction() == VReference.HUNTER_FACTION, "could not make a hunter");
            setRace(p, raceId);
            helper.assertTrue(handler(p).getCurrentFaction() == null, "a hunter that became " + raceId + " is still " + handler(p).getCurrentFaction());
            helper.assertTrue(raceId.equals(race(p)), "race after the change was " + race(p));
        }

        ServerPlayer elf = TestPlayers.create(helper);
        setRace(elf, "tensura:human");
        handler(elf).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        setRace(elf, "tensura:elf");
        helper.assertTrue(handler(elf).getCurrentFaction() == VReference.HUNTER_FACTION && handler(elf).getCurrentLevel() == 2,
                "control: a hunter that became an elf changed to " + handler(elf).getCurrentFaction());

        // only hunters are affected by this rule; a vampire turning ghoul keeps the faction
        ServerPlayer vampire = TestPlayers.create(helper);
        handler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
        setRace(vampire, "tensura:ghoul");
        helper.assertTrue(isVampire(vampire), "a vampire that became a ghoul left the faction");
        helper.succeed();
    }

    private static DamageSource tensuraHoly(GameTestHelper helper) {
        return helper.getLevel().damageSources().source(TensuraDamageTypes.HOLY_DAMAGE);
    }

    private static DamageSource holyWater(GameTestHelper helper) {
        return helper.getLevel().damageSources().source(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:holy_water")));
    }

    /** The damage the target sees after every normal-priority listener (ours included) has run. */
    private static float hurtAmount(ServerPlayer p, DamageSource source, float amount) {
        float[] seen = {-1};
        Consumer<LivingIncomingDamageEvent> recorder = ev -> {
            if (ev.getEntity() == p) {
                seen[0] = ev.getAmount();
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, recorder);
        try {
            p.invulnerableTime = 0;
            p.hurt(source, amount);
        } finally {
            NeoForge.EVENT_BUS.unregister(recorder);
        }
        return seen[0];
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void tensuraHolyHurtsVampireMore(GameTestHelper helper) {
        ServerPlayer vampire = TestPlayers.createVulnerable(helper);
        handler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 5);
        ServerPlayer hunter = TestPlayers.createVulnerable(helper);
        handler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 2);

        float control = hurtAmount(hunter, tensuraHoly(helper), 4.0F);
        float seen = hurtAmount(vampire, tensuraHoly(helper), 4.0F);
        helper.assertTrue(control > 0, "control: the hunter was not hurt by Tensura holy damage (" + control + ")");
        helper.assertTrue(Math.abs(seen - control * 1.5F) < 0.001F, "vampire saw " + seen + ", a hunter " + control + " (want x1.5)");

        // other damage is not touched
        float fall = hurtAmount(vampire, helper.getLevel().damageSources().generic(), 4.0F);
        float fallControl = hurtAmount(hunter, helper.getLevel().damageSources().generic(), 4.0F);
        helper.assertTrue(Math.abs(fall - fallControl) < 0.001F, "non-holy damage differed: " + fall + " vs " + fallControl);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void holyWaterCountsAsTensuraHoly(GameTestHelper helper) {
        helper.assertTrue(TensuraDamageHelper.isHoly(holyWater(helper)), "Tensura does not see vampirism:holy_water as holy");

        // the bridge must not multiply holy water a second time
        ServerPlayer vampire = TestPlayers.create(helper);
        handler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 5);
        helper.assertTrue(HolyDamageBridge.factor(vampire, holyWater(helper)) == 1.0F, "holy water was multiplied by the bridge");
        helper.assertTrue(HolyDamageBridge.factor(vampire, tensuraHoly(helper)) == 1.5F, "control: Tensura holy damage was not multiplied");
        helper.succeed();
    }

    private static final String BLOODLESS_NS = "tensura";

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodlessPlayerCannotBeBitten(GameTestHelper helper) {
        ServerPlayer biter = TestPlayers.create(helper);
        handler(biter).setFactionAndLevel(VReference.VAMPIRE_FACTION, 5);
        biter.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 1.5)), 0, 0);
        IVampirePlayer vampire = VampirismAPI.vampirePlayer(biter);

        // the test server has PvP off, and Vampirism refuses to bite a player then
        var server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        server.setPvpAllowed(true);
        try {
            for (String raceId : List.of("tensura:human", "tensura:elf")) {
                ServerPlayer victim = victimBehind(helper, raceId);
                helper.assertTrue(vampire.determineBiteType(victim) == IVampirePlayer.BITE_TYPE.SUCK_BLOOD_PLAYER,
                        "control: a " + raceId + " victim gave " + vampire.determineBiteType(victim));
            }
            for (String raceId : List.of("tensura:slime", "tensura:ghoul", "tensura:metal_slime")) {
                ServerPlayer victim = victimBehind(helper, raceId);
                helper.assertTrue(vampire.determineBiteType(victim) == IVampirePlayer.BITE_TYPE.NONE,
                        "a " + raceId + " victim gave " + vampire.determineBiteType(victim));
            }
        } finally {
            server.setPvpAllowed(pvp);
        }
        helper.succeed();
    }

    /** Facing away from the biter (south), 2 blocks in front of them, because a victim that can see the vampire cannot be bitten. */
    private static ServerPlayer victimBehind(GameTestHelper helper, String raceId) {
        ServerPlayer victim = TestPlayers.create(helper);
        victim.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 3.5)), 0, 0);
        victim.setYHeadRot(0);
        setRace(victim, raceId);
        return victim;
    }

    @SuppressWarnings("unchecked")
    private static int entityBlood(String entityId) throws ReflectiveOperationException {
        DataMapType<EntityType<?>, IEntityBlood> map = (DataMapType<EntityType<?>, IEntityBlood>)
                Class.forName("de.teamlapen.vampirism.core.ModDataMaps").getField("ENTITY_BLOOD_MAP").get(null);
        IEntityBlood entry = BuiltInRegistries.ENTITY_TYPE.getData(map, ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(entityId)));
        return entry == null ? -1 : entry.blood();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodlessEntitiesHaveNoBlood(GameTestHelper helper) throws Exception {
        // control: the data map is readable and our file did not replace Vampirism's own entries
        helper.assertTrue(entityBlood("minecraft:cow") > 0, "cow blood is " + entityBlood("minecraft:cow"));

        var tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("tensura:no_blood"));
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (Holder<EntityType<?>> h : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(h.value());
            if (BLOODLESS_NS.equals(key.getNamespace())) {
                checked++;
                if (entityBlood(key.toString()) != 0) {
                    wrong.add(key + "=" + entityBlood(key.toString()));
                }
            }
        }
        helper.assertTrue(checked > 10, "only " + checked + " Tensura entities in tensura:no_blood");
        helper.assertTrue(wrong.isEmpty(), "bloodless Tensura entities with blood: " + wrong);
        helper.succeed();
    }

    private static EntityExistenceData existence(GameTestHelper helper, String entityId) {
        return helper.getLevel().registryAccess().registryOrThrow(TensuraCustomData.ENTITY_EXISTENCE).stream()
                .filter(d -> d.entity().toString().equals(entityId)).findFirst().orElse(null);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampirismMobsHaveEp(GameTestHelper helper) {
        // control: Tensura's own data is found the same way
        helper.assertTrue(existence(helper, "tensura:orc") != null, "control: no existence data for tensura:orc");
        for (String id : List.of("vampirism:vampire", "vampirism:advanced_vampire", "vampirism:vampire_baron",
                "vampirism:hunter", "vampirism:advanced_hunter")) {
            EntityExistenceData d = existence(helper, id);
            helper.assertTrue(d != null, "no existence data for " + id);
            helper.assertTrue(d.maxMagicule() + d.maxAura() > 0 && d.spiritualHP() > 0, "empty existence data for " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void speciesPlayerIsSkipped(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        p.setData(ModAttachments.BLOODLINE, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        setRace(p, "tensura:vampire_lord");
        helper.assertTrue(handler(p).getCurrentFaction() == null, "a species player was synced");
        helper.assertTrue(BloodlineAccess.get(p).species() == Species.WITCH, "species changed");
        helper.succeed();
    }
}
