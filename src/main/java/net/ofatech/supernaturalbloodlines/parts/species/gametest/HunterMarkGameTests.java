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
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.StalePartModifiers;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
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
            r.clearPendingPotentialChoice(p.getUUID());
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
            helper.assertTrue(roster(hunter).pendingPotentialChoice().containsKey(hunter.getUUID()), "no potential was owed");
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
            helper.assertTrue(roster(lost).pendingPotentialChoice().containsKey(lost.getUUID()), "the choice was not kept");
            helper.assertTrue(BloodlineAccess.get(lost).species() == Species.NONE, "the dead hunter was chosen");

            // an eligible hunter joins: the pending choice is retried
            online(lost, later);
            handler(later).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            helper.assertTrue(BloodlineAccess.get(later).species() == Species.POTENTIAL, "joining the hunters chose " + BloodlineAccess.get(later).species());
            helper.assertTrue(roster(lost).pendingPotentialChoice().isEmpty(), "the choice is still pending");

            // the same hunter losing the mark twice while nobody is online owes two potentials, not one
            BloodlineAccess.set(later, BloodlineState.NONE);
            online();
            PotentialChooser.owe(server, lost.getUUID());
            PotentialChooser.owe(server, lost.getUUID());
            helper.assertTrue(roster(lost).pendingPotentialChoice().get(lost.getUUID()) == 2,
                    "owed " + roster(lost).pendingPotentialChoice().get(lost.getUUID()) + " instead of 2");
            online(second);
            PotentialChooser.retryPending(server);
            helper.assertTrue(BloodlineAccess.get(second).species() == Species.POTENTIAL, "the retry chose " + BloodlineAccess.get(second).species());
            helper.assertTrue(roster(lost).pendingPotentialChoice().get(lost.getUUID()) == 1, "one should still be owed");
            online(second, later);
            PotentialChooser.retryPending(server);
            helper.assertTrue(BloodlineAccess.get(later).species() == Species.POTENTIAL, "the second potential: " + BloodlineAccess.get(later).species());
            helper.assertTrue(roster(lost).pendingPotentialChoice().isEmpty(), "pending after both were chosen");
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

    // ---- prey comes from the Vampirism factions, not from artifacts (run with -PpartsOff=artifacts to prove it) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void factionVampireIsPrey(GameTestHelper helper) {
        ServerPlayer vampire = vampire(helper);
        ServerPlayer hunter = huntNone(helper);
        ServerPlayer human = human(helper);
        try {
            helper.assertTrue(Supernatural.isPrey(vampire) && Supernatural.isVampiric(vampire) && Supernatural.is(vampire), "a faction vampire is not prey");
            helper.assertTrue(!Supernatural.isPrey(hunter) && !Supernatural.is(hunter), "a hunter is prey or supernatural");
            helper.assertTrue(!Supernatural.isPrey(human), "a human is prey");
        } finally {
            reset(vampire, hunter, human);
        }
        helper.succeed();
    }

    // ---- kill credit through Vampirism's dying state ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killCreditCountsWhenTheSourceHasNoAttacker(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        ServerPlayer victim = vampire(helper);
        ServerPlayer lost = marked(helper, 3);
        ServerPlayer killer = vampire(helper);
        try {
            online();
            victim.setLastHurtByPlayer(hunter);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, victim.damageSources().genericKill()));
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 1, "credit kill gave " + BloodlineAccess.get(hunter).markKills());

            lost.setLastHurtByMob(killer);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(lost, lost.damageSources().fall()));
            helper.assertTrue(BloodlineAccess.get(lost).species() == Species.NONE, "a hunter pushed to death by a vampire kept the mark");
            helper.assertTrue(BloodlineAccess.get(killer).hunterCurse(), "the credited vampire was not cursed");
        } finally {
            clearHooks();
            reset(hunter, victim, lost, killer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void dyingStateKillGivesTheHunterTheMark(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        MinecraftServer server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        try {
            online(hunter); // a player attacker is found again through the player list
            handler(victim).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            server.setPvpAllowed(true);
            victim.hurt(hunter.damageSources().playerAttack(hunter), 1000f);
            helper.assertTrue(VampirismAPI.vampirePlayer(victim).isDBNO(), "the vampire is not in Vampirism's dying state");
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 0, "being downed counted as a kill");

            // the kill credit has run out by the time the dying state ends, and the end has no attacker
            victim.setLastHurtByPlayer(null);
            victim.setLastHurtByMob(null);
            helper.assertTrue(victim.getKillCredit() == null, "the kill credit is still there");
            try {
                VampirismAPI.vampirePlayer(victim).getClass().getMethod("giveUpDBNO").invoke(VampirismAPI.vampirePlayer(victim));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
            helper.assertTrue(victim.isDeadOrDying(), "giving up did not kill");
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 1, "the hunter's mark is " + BloodlineAccess.get(hunter).markKills());
        } finally {
            server.setPvpAllowed(pvp);
            clearHooks();
            reset(hunter, victim);
        }
        helper.succeed();
    }

    /** The attacker of a dying player is remembered by UUID: a player who logged out and in again is a new object, and one who is gone gets no credit. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void dyingStateAttackerIsFoundByUuid(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        ServerPlayer relogged = new FakePlayer(helper.getLevel(), hunter.getGameProfile());
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        MinecraftServer server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        try {
            BloodlineAccess.set(relogged, state(Species.HUNTER_OF_THE_FIVE, 0));
            handler(victim).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            server.setPvpAllowed(true);
            victim.hurt(hunter.damageSources().playerAttack(hunter), 1000f);
            helper.assertTrue(VampirismAPI.vampirePlayer(victim).isDBNO(), "the vampire is not in Vampirism's dying state");
            victim.setLastHurtByPlayer(null);
            victim.setLastHurtByMob(null);
            online(relogged); // the attacker logged in again: the list has the new object
            giveUp(victim);
            helper.assertTrue(BloodlineAccess.get(relogged).markKills() == 1, "the player in the list got " + BloodlineAccess.get(relogged).markKills());
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 0, "the stale player object got credit");
        } finally {
            server.setPvpAllowed(pvp);
            clearHooks();
            reset(hunter, relogged, victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void dyingStateAttackerWhoLeftGetsNoCredit(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        MinecraftServer server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        try {
            handler(victim).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            server.setPvpAllowed(true);
            victim.hurt(hunter.damageSources().playerAttack(hunter), 1000f);
            victim.setLastHurtByPlayer(null);
            victim.setLastHurtByMob(null);
            online(); // nobody online
            giveUp(victim);
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 0, "an attacker who is not online got credit");
        } finally {
            server.setPvpAllowed(pvp);
            clearHooks();
            reset(hunter, victim);
        }
        helper.succeed();
    }

    private static void giveUp(ServerPlayer victim) {
        try {
            VampirismAPI.vampirePlayer(victim).getClass().getMethod("giveUpDBNO").invoke(VampirismAPI.vampirePlayer(victim));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** A potential who leaves the hunters ends the choice that made them one: the world owes another. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void potentialLeavingTheHuntersOwesAnother(GameTestHelper helper) {
        ServerPlayer leaver = huntNone(helper);
        ServerPlayer next = huntNone(helper);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            BloodlineAccess.set(leaver, state(Species.POTENTIAL, 0));
            online(); // nobody to take it yet
            handler(leaver).leaveFaction(false);
            helper.assertTrue(BloodlineAccess.get(leaver).species() == Species.NONE, "species is " + BloodlineAccess.get(leaver).species());
            helper.assertTrue(roster(leaver).pendingPotentialChoice().getOrDefault(leaver.getUUID(), 0) == 1, "owed: " + roster(leaver).pendingPotentialChoice());
            helper.assertTrue(!roster(leaver).isPotential(leaver.getUUID()), "the leaver is still a potential");

            online(next);
            PotentialChooser.retryPending(server);
            helper.assertTrue(BloodlineAccess.get(next).species() == Species.POTENTIAL, "the next hunter is " + BloodlineAccess.get(next).species());
            helper.assertTrue(roster(leaver).pendingPotentialChoice().isEmpty(), "still owed: " + roster(leaver).pendingPotentialChoice());

            // and with someone online, the new potential is made at once
            ServerPlayer third = huntNone(helper);
            ServerPlayer second = huntNone(helper);
            try {
                BloodlineAccess.set(second, state(Species.POTENTIAL, 0));
                online(third);
                handler(second).leaveFaction(false);
                helper.assertTrue(BloodlineAccess.get(third).species() == Species.POTENTIAL, "the online hunter is " + BloodlineAccess.get(third).species());
            } finally {
                reset(third, second);
            }
        } finally {
            clearHooks();
            reset(leaver, next);
        }
        helper.succeed();
    }

    // ---- minor ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curseTickPrunesTheCursedList(GameTestHelper helper) {
        ServerPlayer killer = vampire(helper);
        ServerPlayer lost = marked(helper, 1);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            online(killer);
            kill(killer, lost);
            helper.assertTrue(roster(killer).cursed().contains(killer.getUUID()), "not listed as cursed");
            BloodlineAccess.set(killer, BloodlineState.NONE); // cured some other way
            HunterCurse.tick(server, HunterCurse.nextDue(killer).getAsLong());
            helper.assertTrue(HunterCurse.nextDue(killer).isEmpty(), "the timer stayed");
            helper.assertTrue(!roster(killer).cursed().contains(killer.getUUID()), "the cursed list kept a freed player");
        } finally {
            clearHooks();
            reset(killer, lost);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEventThatChangesMarkKillsSyncsThePerks(GameTestHelper helper) {
        ServerPlayer p = marked(helper, 4);
        try {
            helper.assertTrue(mark(p, Attributes.ATTACK_DAMAGE) != null && Math.abs(mark(p, Attributes.ATTACK_DAMAGE).amount() - 0.04) < EPS,
                    "setting the state did not set the perks: " + mark(p, Attributes.ATTACK_DAMAGE));
            // what `/supernaturalbloodlines debug event` does
            BloodlineAccess.applyEvent(p, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent.KILLED_PREY, SpeciesContext.of(p, null));
            assertPerks(helper, p, 0.05, 0.025);
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
