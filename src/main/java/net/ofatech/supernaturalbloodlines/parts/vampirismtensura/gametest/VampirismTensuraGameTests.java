package net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
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
    public static void speciesPlayerIsSkipped(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        p.setData(ModAttachments.BLOODLINE, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        setRace(p, "tensura:vampire_lord");
        helper.assertTrue(handler(p).getCurrentFaction() == null, "a species player was synced");
        helper.assertTrue(BloodlineAccess.get(p).species() == Species.WITCH, "species changed");
        helper.succeed();
    }
}
