package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.StalePartModifiers;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.network.HallucinationPayload;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.FiveRoster;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterCurse;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterEvents;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.PotentialChooser;

/** Hunters of the Five: the mark, the potentials and the Hunter's Curse. Players are posted a death by hand, as in the other species tests. */
@PrefixGameTestTemplate(false)
public final class HunterMarkGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final double EPS = 1e-9;

    private HunterMarkGameTests() {}

    // ---- helpers (shared with HunterWerewolfGameTests) ----

    static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    static FiveRoster roster(ServerPlayer p) {
        return FiveRoster.get(p.serverLevel());
    }

    static BloodlineState state(Species s, int kills) {
        return BloodlineState.NONE.toBuilder().species(s).markKills(kills).build();
    }

    static ServerPlayer human(GameTestHelper helper) {
        return TestPlayers.createMortal(helper);
    }

    static ServerPlayer huntNone(GameTestHelper helper) {
        ServerPlayer p = human(helper);
        handler(p).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
        return p;
    }

    static ServerPlayer marked(GameTestHelper helper, int kills) {
        ServerPlayer p = huntNone(helper);
        BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE, kills));
        return p;
    }

    static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = human(helper);
        handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
        return p;
    }

    /** Dies as if killed by {@code killer}; GENERIC_KILL so Vampirism's dying state does not hold a vampire back. */
    static void kill(ServerPlayer killer, LivingEntity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, victim.damageSources().source(DamageTypes.GENERIC_KILL, killer)));
    }

    static void reset(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            HunterCurse.onLogout(p);
            BloodlineAccess.set(p, BloodlineState.NONE);
            handler(p).leaveFaction(false);
            FiveRoster r = roster(p);
            r.setCursed(p.getUUID(), false);
            r.setLiftedCurse(p.getUUID(), false);
            r.setPendingPotentialChoice(p.getUUID(), false);
        }
    }

    static void online(ServerPlayer... players) {
        List<ServerPlayer> list = List.of(players);
        HunterEvents.overrideOnline(server -> list);
    }

    static void clearHooks() {
        HunterEvents.overrideOnline(null);
        HallucinationPayload.overrideSpy(null);
    }

    static AttributeModifier mark(ServerPlayer p, Holder<Attribute> a) {
        AttributeInstance i = p.getAttribute(a);
        return i == null ? null : i.getModifier(MarkTracker.MODIFIER_ID);
    }

    // ---- mark ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markGrowsOnVampireKill(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        ServerPlayer victim = vampire(helper);
        try {
            kill(hunter, victim);
            BloodlineState s = BloodlineAccess.get(hunter);
            helper.assertTrue(s.species() == Species.HUNTER_OF_THE_FIVE && s.markKills() == 1, "mark is " + s.markKills() + " (" + s.species() + ")");
            helper.assertTrue(s.lastVampireKill() == hunter.level().getGameTime(), "lastVampireKill is " + s.lastVampireKill());
            kill(hunter, victim);
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 2, "second kill gave " + BloodlineAccess.get(hunter).markKills());
        } finally {
            reset(hunter, victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void witchKillDoesNotCount(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 3);
        ServerPlayer witch = human(helper);
        ServerPlayer siphoner = human(helper);
        ServerPlayer oddWitch = vampire(helper); // R81: species wins over faction
        ServerPlayer plain = human(helper);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
            BloodlineAccess.set(siphoner, BloodlineState.NONE.toBuilder().species(Species.SIPHONER).build());
            BloodlineAccess.set(oddWitch, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
            kill(hunter, witch);
            kill(hunter, siphoner);
            kill(hunter, oddWitch);
            kill(hunter, plain);
            kill(hunter, helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1)));
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 3, "a non-prey kill counted: " + BloodlineAccess.get(hunter).markKills());
        } finally {
            reset(hunter, witch, siphoner, oddWitch, plain);
        }
        helper.succeed();
    }

    /** A death something else cancels (a Resurrection Ring, Vampirism's dying state) is not a kill. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void cancelledDeathDoesNotCount(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 3);
        ServerPlayer victim = vampire(helper);
        ServerPlayer lost = marked(helper, 2);
        Consumer<LivingDeathEvent> saver = e -> e.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, saver);
        try {
            online();
            kill(hunter, victim);
            kill(victim, lost);
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 3, "a saved victim counted: " + BloodlineAccess.get(hunter).markKills());
            helper.assertTrue(BloodlineAccess.get(lost).species() == Species.HUNTER_OF_THE_FIVE, "a saved hunter lost the mark");
            helper.assertTrue(!BloodlineAccess.get(victim).hunterCurse(), "a saved hunter cursed the killer");
        } finally {
            NeoForge.EVENT_BUS.unregister(saver);
            clearHooks();
            reset(hunter, victim, lost);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markedHunterKilledByVampireLosesMark(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 7);
        ServerPlayer killer = vampire(helper);
        int before = roster(hunter).size();
        try {
            helper.assertTrue(roster(hunter).contains(hunter.getUUID()), "the hunter is not on the roster");
            helper.assertTrue(mark(hunter, Attributes.ATTACK_DAMAGE) != null, "no perk before the death");
            online();
            kill(killer, hunter);
            BloodlineState s = BloodlineAccess.get(hunter);
            helper.assertTrue(s.species() == Species.NONE && s.markKills() == 0, "hunter is " + s.species() + " with " + s.markKills() + " kills");
            helper.assertTrue(!roster(hunter).contains(hunter.getUUID()) && roster(hunter).size() == before - 1, "the slot was not freed");
            helper.assertTrue(mark(hunter, Attributes.ATTACK_DAMAGE) == null && mark(hunter, Attributes.MOVEMENT_SPEED) == null, "the perks stayed");
            helper.assertTrue(roster(hunter).pendingPotentialChoice().contains(hunter.getUUID()), "no potential was owed");
        } finally {
            clearHooks();
            reset(hunter, killer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markedHunterKilledByAHumanKeepsTheMark(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 4);
        ServerPlayer human = human(helper);
        try {
            online();
            kill(human, hunter);
            BloodlineState s = BloodlineAccess.get(hunter);
            helper.assertTrue(s.species() == Species.HUNTER_OF_THE_FIVE && s.markKills() == 4, "hunter is " + s.species() + " with " + s.markKills());
            helper.assertTrue(roster(hunter).pendingPotentialChoice().isEmpty(), "a potential was owed");
        } finally {
            clearHooks();
            reset(hunter, human);
        }
        helper.succeed();
    }

    // ---- potentials ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void potentialChosenFromOnlineHunters(GameTestHelper helper) {
        ServerPlayer lost = marked(helper, 5);
        ServerPlayer killer = vampire(helper);
        ServerPlayer hunter = huntNone(helper);
        ServerPlayer plain = human(helper);
        ServerPlayer another = marked(helper, 1);
        ServerPlayer witchHunter = huntNone(helper);
        try {
            BloodlineAccess.set(witchHunter, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            // `lost` is online too: the hunter who just lost the mark must not be picked
            online(lost, hunter, plain, another, witchHunter);
            kill(killer, lost);
            helper.assertTrue(BloodlineAccess.get(hunter).species() == Species.POTENTIAL, "the hunter is " + BloodlineAccess.get(hunter).species());
            helper.assertTrue(BloodlineAccess.get(lost).species() == Species.NONE, "the dead hunter is " + BloodlineAccess.get(lost).species());
            helper.assertTrue(BloodlineAccess.get(plain).species() == Species.NONE, "a non-hunter was chosen");
            helper.assertTrue(BloodlineAccess.get(another).species() == Species.HUNTER_OF_THE_FIVE, "a marked hunter was changed");
            helper.assertTrue(BloodlineAccess.get(witchHunter).species() == Species.WITCH, "a witch was changed");
            helper.assertTrue(roster(hunter).pendingPotentialChoice().isEmpty(), "the choice is still pending");
            helper.assertTrue(!roster(hunter).contains(hunter.getUUID()), "a potential took a slot");

            // with several eligible hunters the seeded random decides
            ServerPlayer a = huntNone(helper);
            ServerPlayer b = huntNone(helper);
            online(a, b);
            Boolean[] picks = new Boolean[2];
            for (int run = 0; run < 2; run++) {
                helper.assertTrue(PotentialChooser.choose(helper.getLevel().getServer(), UUID.randomUUID(), RandomSource.create(42)),
                        "no one was chosen");
                boolean aWon = BloodlineAccess.get(a).species() == Species.POTENTIAL;
                helper.assertTrue(aWon != (BloodlineAccess.get(b).species() == Species.POTENTIAL), "not exactly one was chosen");
                picks[run] = aWon;
                reset(a, b);
                handler(a).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
                handler(b).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            }
            helper.assertTrue(picks[0].equals(picks[1]), "the same seed picked differently");
            reset(a, b);
        } finally {
            clearHooks();
            reset(lost, killer, hunter, plain, another, witchHunter);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void potentialChosenLaterWhenNoneOnline(GameTestHelper helper) {
        ServerPlayer lost = marked(helper, 5);
        ServerPlayer killer = vampire(helper);
        ServerPlayer later = human(helper);
        ServerPlayer second = huntNone(helper);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            online(lost); // nobody else
            kill(killer, lost);
            helper.assertTrue(roster(lost).pendingPotentialChoice().contains(lost.getUUID()), "the choice was not kept");
            helper.assertTrue(BloodlineAccess.get(lost).species() == Species.NONE, "the dead hunter was chosen");

            // an eligible hunter joins: the pending choice is retried
            online(lost, later);
            handler(later).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            helper.assertTrue(BloodlineAccess.get(later).species() == Species.POTENTIAL, "joining the hunters chose " + BloodlineAccess.get(later).species());
            helper.assertTrue(roster(lost).pendingPotentialChoice().isEmpty(), "the choice is still pending");

            // and a second loss with nobody left waits too, then a login (retry) picks
            BloodlineAccess.set(later, BloodlineState.NONE);
            online();
            helper.assertTrue(!PotentialChooser.choose(server, lost.getUUID()), "chose from an empty server");
            helper.assertTrue(roster(lost).pendingPotentialChoice().contains(lost.getUUID()), "not pending after an empty choice");
            online(second);
            PotentialChooser.retryPending(server);
            helper.assertTrue(BloodlineAccess.get(second).species() == Species.POTENTIAL, "the retry chose " + BloodlineAccess.get(second).species());
            helper.assertTrue(roster(lost).pendingPotentialChoice().isEmpty(), "pending after the retry");
        } finally {
            clearHooks();
            reset(lost, killer, later, second);
        }
        helper.succeed();
    }

    // ---- curse ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void potentialFirstKillLiftsCurse(GameTestHelper helper) {
        ServerPlayer potential = huntNone(helper);
        ServerPlayer cursed = vampire(helper);
        ServerPlayer victim = vampire(helper);
        ServerPlayer offline = vampire(helper);
        ServerPlayer lost = marked(helper, 2);
        try {
            BloodlineAccess.set(potential, state(Species.POTENTIAL, 0));
            online(potential, cursed, victim, lost);
            kill(cursed, lost);
            helper.assertTrue(BloodlineAccess.get(cursed).hunterCurse(), "not cursed");
            helper.assertTrue(HunterCurse.nextDue(cursed).isPresent(), "no hallucination scheduled");
            // a curse of someone who is not online right now
            BloodlineAccess.set(offline, BloodlineState.NONE.toBuilder().hunterCurse(true).build());
            roster(offline).setCursed(offline.getUUID(), true);

            kill(potential, victim);
            BloodlineState s = BloodlineAccess.get(potential);
            helper.assertTrue(s.species() == Species.HUNTER_OF_THE_FIVE && s.markKills() == 1, "potential is " + s.species() + " with " + s.markKills());
            helper.assertTrue(roster(potential).contains(potential.getUUID()), "no slot");
            helper.assertTrue(!BloodlineAccess.get(cursed).hunterCurse(), "the online curse stayed");
            helper.assertTrue(HunterCurse.nextDue(cursed).isEmpty(), "the hallucinations went on");
            helper.assertTrue(roster(offline).liftedCurses().contains(offline.getUUID()), "the offline curse was not marked lifted");
            helper.assertTrue(!roster(offline).cursed().contains(offline.getUUID()), "the offline player is still listed as cursed");

            // at login the offline player is freed
            HunterCurse.onLogin(offline);
            helper.assertTrue(!BloodlineAccess.get(offline).hunterCurse(), "login did not lift the curse");
            helper.assertTrue(!roster(offline).liftedCurses().contains(offline.getUUID()), "the lifted mark stayed after login");
            helper.assertTrue(HunterCurse.nextDue(offline).isEmpty(), "a lifted player was scheduled");

            // only the potential's first kill lifts it: a later kill by the same hunter does not
            BloodlineAccess.set(cursed, BloodlineState.NONE.toBuilder().hunterCurse(true).build());
            kill(potential, victim);
            helper.assertTrue(BloodlineAccess.get(potential).markKills() == 2, "second kill: " + BloodlineAccess.get(potential).markKills());
            helper.assertTrue(BloodlineAccess.get(cursed).hunterCurse(), "a second kill lifted the curse");
        } finally {
            clearHooks();
            reset(potential, cursed, victim, offline, lost);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curseDoesNotStack(GameTestHelper helper) {
        ServerPlayer killer = vampire(helper);
        ServerPlayer first = marked(helper, 2);
        ServerPlayer second = marked(helper, 3);
        MinecraftServer server = helper.getLevel().getServer();
        List<String> sent = new ArrayList<>();
        try {
            online(killer);
            HallucinationPayload.overrideSpy((p, kind) -> sent.add(p.getUUID() + ":" + kind));
            long now = server.getTickCount();
            kill(killer, first);
            helper.assertTrue(BloodlineAccess.get(killer).hunterCurse(), "not cursed");
            OptionalLong due = HunterCurse.nextDue(killer);
            helper.assertTrue(due.isPresent() && due.getAsLong() >= now + 400 && due.getAsLong() <= now + 800,
                    "first hallucination due " + due + " from " + now + " (600 +- 200)");
            kill(killer, second);
            helper.assertTrue(BloodlineAccess.get(killer).hunterCurse(), "the second kill lifted the flag");
            helper.assertTrue(HunterCurse.nextDue(killer).equals(due), "the second curse moved the timer: " + HunterCurse.nextDue(killer) + " vs " + due);

            HunterCurse.tick(server, due.getAsLong() - 1);
            helper.assertTrue(sent.isEmpty(), "a hallucination came early");
            HunterCurse.tick(server, due.getAsLong());
            helper.assertTrue(sent.size() == 1, "sent " + sent.size() + " at the due tick");
            HunterCurse.tick(server, due.getAsLong());
            helper.assertTrue(sent.size() == 1, "the same tick sent again: " + sent.size());
            long next = HunterCurse.nextDue(killer).getAsLong();
            helper.assertTrue(next >= due.getAsLong() + 400 && next <= due.getAsLong() + 800, "next due " + next + " after " + due);
            HunterCurse.tick(server, next);
            helper.assertTrue(sent.size() == 2, "sent " + sent.size() + " after the second interval");

            // logout stops the timer; login of a still-cursed player starts it again
            HunterCurse.onLogout(killer);
            helper.assertTrue(HunterCurse.nextDue(killer).isEmpty(), "logout kept the timer");
            HunterCurse.tick(server, next + 5000);
            helper.assertTrue(sent.size() == 2, "an offline player was haunted");
            HunterCurse.onLogin(killer);
            helper.assertTrue(HunterCurse.nextDue(killer).isPresent(), "login did not reschedule");
        } finally {
            clearHooks();
            reset(killer, first, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanKillerNotCursed(GameTestHelper helper) {
        ServerPlayer human = human(helper);
        ServerPlayer witch = human(helper);
        ServerPlayer hunterKiller = marked(helper, 1);
        ServerPlayer first = marked(helper, 2);
        ServerPlayer second = marked(helper, 2);
        ServerPlayer third = marked(helper, 2);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
            online();
            kill(human, first);
            kill(witch, second);
            kill(hunterKiller, third);
            for (ServerPlayer h : new ServerPlayer[] {first, second, third}) {
                helper.assertTrue(BloodlineAccess.get(h).species() == Species.HUNTER_OF_THE_FIVE, "a mark was lost to a non-prey killer");
            }
            for (ServerPlayer k : new ServerPlayer[] {human, witch, hunterKiller}) {
                helper.assertTrue(!BloodlineAccess.get(k).hunterCurse() && HunterCurse.nextDue(k).isEmpty(), "a non-prey killer was cursed");
            }
            helper.assertTrue(roster(human).pendingPotentialChoice().isEmpty(), "a potential was owed");
        } finally {
            clearHooks();
            reset(human, witch, hunterKiller, first, second, third);
        }
        helper.succeed();
    }

    // ---- perks ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markPerksScaleAndCap(GameTestHelper helper) {
        ServerPlayer p = huntNone(helper);
        try {
            helper.assertTrue(mark(p, Attributes.ATTACK_DAMAGE) == null, "a perk without a mark");
            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE, 10));
            MarkTracker.sync(p);
            assertPerks(helper, p, 0.10, 0.05);

            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE, 25));
            MarkTracker.sync(p);
            assertPerks(helper, p, 0.20, 0.10);
            helper.assertTrue(p.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().stream()
                    .filter(m -> m.id().equals(MarkTracker.MODIFIER_ID)).count() == 1, "the perk was added twice");

            // a real kill past the cap keeps the cap, and every kill below it scales
            ServerPlayer victim = vampire(helper);
            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE, 0));
            for (int i = 1; i <= 3; i++) {
                kill(p, victim);
            }
            assertPerks(helper, p, 0.03, 0.015);
            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE, 25));
            kill(p, victim);
            assertPerks(helper, p, 0.20, 0.10);
            reset(victim);

            // it is a saved modifier, and a part that is switched off strips it
            helper.assertTrue(p.getAttribute(Attributes.ATTACK_DAMAGE).save().toString().contains(MarkTracker.MODIFIER_ID.toString()),
                    "the perk is not saved with the player");
            StalePartModifiers.strip(p, Set.of(Part.ARTIFACTS));
            helper.assertTrue(mark(p, Attributes.ATTACK_DAMAGE) == null && mark(p, Attributes.MOVEMENT_SPEED) == null, "a stale perk survived");
            MarkTracker.sync(p);

            // losing the species takes the perks away
            BloodlineAccess.set(p, state(Species.NONE, 0));
            helper.assertTrue(mark(p, Attributes.ATTACK_DAMAGE) == null && mark(p, Attributes.MOVEMENT_SPEED) == null, "the perks outlived the mark");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    private static void assertPerks(GameTestHelper helper, ServerPlayer p, double attack, double speed) {
        AttributeModifier a = mark(p, Attributes.ATTACK_DAMAGE);
        AttributeModifier s = mark(p, Attributes.MOVEMENT_SPEED);
        helper.assertTrue(a != null && a.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL && Math.abs(a.amount() - attack) < EPS,
                "attack perk is " + a + ", expected +" + attack + " total");
        helper.assertTrue(s != null && s.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE && Math.abs(s.amount() - speed) < EPS,
                "speed perk is " + s + ", expected +" + speed + " base");
    }
}
