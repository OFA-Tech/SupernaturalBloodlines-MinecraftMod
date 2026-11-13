package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.BloodlineCommand;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallFollowers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * The {@code /bloodline} commands (plan 8, Task 2). Test players are not in the player list, so the call looks them up
 * through a map the test pins; the command output is captured by a command source of the test's own.
 */
@PrefixGameTestTemplate(false)
public final class SirelineCommandGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SirelineCommandGameTests() {
    }

    static final class Capture implements CommandSource {
        final List<Component> got = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component c) {
            got.add(c);
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        String text() {
            StringBuilder b = new StringBuilder();
            for (Component c : got) {
                flatten(c, b);
                b.append('\n');
            }
            return b.toString();
        }

        private static void flatten(Component c, StringBuilder b) {
            if (c.getContents() instanceof TranslatableContents t) {
                b.append(t.getKey());
                for (Object arg : t.getArgs()) {
                    b.append(' ');
                    if (arg instanceof Component ac) {
                        flatten(ac, b);
                    } else {
                        b.append(arg);
                    }
                }
            } else {
                b.append(c.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents p ? p.text() : "");
            }
            c.getSiblings().forEach(s -> flatten(s, b));
        }
    }

    static CommandSourceStack stack(ServerPlayer p, int permission, Capture out) {
        return new CommandSourceStack(out, p.position(), p.getRotationVector(), (ServerLevel) p.level(), permission,
                p.getGameProfile().getName(), p.getDisplayName(), p.server, p);
    }

    static int run(ServerPlayer p, int permission, Capture out, String command) {
        try {
            return p.server.getCommands().getDispatcher().execute(command, stack(p, permission, out));
        } catch (CommandSyntaxException e) {
            out.got.add(Component.literal("SYNTAX: " + e.getMessage()));
            return 0;
        }
    }

    static ServerPlayer named(GameTestHelper helper, ServerLevel level, String name) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(level, new GameProfile(id, name + "-" + id.toString().substring(0, 4)));
    }

    /** A dimension change asks the connection which channels it has; a fake player's connection has no netty channel. */
    private static void withChannel(ServerPlayer p) {
        try {
            Field f = Connection.class.getDeclaredField("channel");
            f.setAccessible(true);
            f.set(p.connection.getConnection(), new EmbeddedChannel());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void species(ServerPlayer p, Species s) {
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(s).build());
    }

    static void place(GameTestHelper helper, Entity e, int x) {
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2, 4.5));
        e.moveTo(at.x, at.y, at.z);
    }

    static void online(ServerPlayer... ps) {
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
    }

    static void cleanUp(ServerPlayer... ps) {
        SireCall.overridePlayerLookup(null);
        for (ServerPlayer p : ps) {
            BloodlineAccess.set(p, BloodlineState.NONE);
            p.removeData(SpeciesRegistries.LAST_CALL);
            SireCall.requests().forget(p.getUUID());
            CallFollowers.forget(p.getUUID());
        }
    }

    // ---- tree ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void treeShowsChildren(GameTestHelper helper) {
        ServerPlayer sire = named(helper, helper.getLevel(), "sire");
        ServerPlayer child = named(helper, helper.getLevel(), "child");
        ServerPlayer grandchild = named(helper, helper.getLevel(), "grand");
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        try {
            SireRecorder.record(sire, child);
            SireRecorder.record(child, grandchild);
            SireRecorder.record(sire, cow);
            Capture out = new Capture();
            helper.assertTrue(run(sire, 0, out, "bloodline tree") == 1, "tree failed: " + out.text());
            String text = out.text();
            String c = child.getGameProfile().getName();
            String g = grandchild.getGameProfile().getName();
            helper.assertTrue(text.contains(c) && text.contains(g) && text.contains("minecraft:cow"), "children missing: " + text);
            helper.assertTrue(text.indexOf(c) < text.indexOf(g), "the grandchild comes before the child: " + text);
            helper.assertTrue(text.indexOf(g) < text.indexOf("minecraft:cow"), "depth-first order broken: " + text);

            Capture none = new Capture();
            helper.assertTrue(run(grandchild, 0, none, "bloodline tree") == 1, "an empty tree must still succeed: " + none.text());
            helper.assertTrue(none.text().contains(child.getGameProfile().getName()), "the sire line is missing: " + none.text());
        } finally {
            cleanUp(sire, child, grandchild);
            cow.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void treeShowsOfflineSire(GameTestHelper helper) {
        ServerPlayer root = named(helper, helper.getLevel(), "root");
        ServerPlayer middle = named(helper, helper.getLevel(), "mid");
        ServerPlayer leaf = named(helper, helper.getLevel(), "leaf");
        ServerPlayer orphan = named(helper, helper.getLevel(), "orphan");
        try {
            SireRecorder.record(root, middle);
            SireRecorder.record(middle, leaf);
            // nobody here is in the player list: the middle one is "offline", known only by the index
            Capture out = new Capture();
            helper.assertTrue(run(root, 0, out, "bloodline tree") == 1, "tree failed: " + out.text());
            helper.assertTrue(out.text().contains(middle.getGameProfile().getName()) && out.text().contains(leaf.getGameProfile().getName()),
                    "an offline node hid its line: " + out.text());

            // a sire the server has never heard of: last known name only
            SireRecorder.record(UUID.randomUUID(), "Ghost", orphan);
            Capture o = new Capture();
            helper.assertTrue(run(orphan, 0, o, "bloodline tree") == 1, "tree with a gone sire failed: " + o.text());
            helper.assertTrue(o.text().contains("Ghost"), "the gone sire is not named: " + o.text());
            // a node whose name was never kept still prints
            SirelineIndex.get(helper.getLevel()).add(UUID.randomUUID(), orphan.getUUID(), "minecraft:player", orphan.getGameProfile().getName());
            helper.assertTrue(run(orphan, 0, new Capture(), "bloodline tree") == 1, "an unnamed sire crashed the tree");
        } finally {
            cleanUp(root, middle, leaf, orphan);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void treeStopsOnCycle(GameTestHelper helper) {
        ServerPlayer a = named(helper, helper.getLevel(), "a");
        ServerPlayer b = named(helper, helper.getLevel(), "b");
        try {
            SireRecorder.record(a, b);
            // a was cured, then turned by b
            SireRecorder.record(b, a);
            Capture out = new Capture();
            helper.assertTrue(run(a, 0, out, "bloodline tree") == 1, "tree failed: " + out.text());
            String text = out.text();
            String bn = b.getGameProfile().getName();
            helper.assertTrue(count(text, "- " + bn) == 1, "b is not listed exactly once: " + text);
            helper.assertTrue(count(text, "- ") == 1, "the cycle was walked: " + text);
        } finally {
            cleanUp(a, b);
        }
        helper.succeed();
    }

    private static int count(String s, String part) {
        int n = 0;
        for (int i = s.indexOf(part); i >= 0; i = s.indexOf(part, i + part.length())) {
            n++;
        }
        return n;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void treeOfSomeoneElseNeedsOp(GameTestHelper helper) {
        ServerPlayer sire = named(helper, helper.getLevel(), "sire");
        ServerPlayer nosy = named(helper, helper.getLevel(), "nosy");
        ServerPlayer child = named(helper, helper.getLevel(), "child");
        try {
            SireRecorder.record(sire, child);
            // the argument resolves names through the server's profile cache, which a game test server does not have
            GameProfile target = sire.getGameProfile();
            Capture denied = new Capture();
            helper.assertTrue(BloodlineCommand.tree(stack(nosy, 0, denied), target) == 0, "a player read someone else's tree: " + denied.text());
            helper.assertTrue(denied.text().contains("denied"), "no refusal message: " + denied.text());
            Capture ok = new Capture();
            helper.assertTrue(BloodlineCommand.tree(stack(nosy, 2, ok), target) == 1 && ok.text().contains(child.getGameProfile().getName()),
                    "an op could not read it: " + ok.text());
            helper.assertTrue(BloodlineCommand.tree(stack(sire, 0, new Capture()), target) == 1, "reading their own tree was refused");
        } finally {
            cleanUp(sire, nosy, child);
        }
        helper.succeed();
    }

    // ---- call ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void callNeedsSpecies(GameTestHelper helper) {
        ServerPlayer caller = named(helper, helper.getLevel(), "caller");
        ServerPlayer sired = named(helper, helper.getLevel(), "sired");
        try {
            SireRecorder.record(caller, sired);
            online(caller, sired);
            for (Species s : new Species[] {Species.NONE, Species.WITCH, Species.SIPHONER, Species.HERETIC, Species.POTENTIAL, Species.HUNTER_OF_THE_FIVE}) {
                species(caller, s);
                Capture out = new Capture();
                helper.assertTrue(run(caller, 0, out, "bloodline call") == 0, s + " could call: " + out.text());
                helper.assertTrue(!caller.hasData(SpeciesRegistries.LAST_CALL), s + " started a cooldown");
                helper.assertTrue(SireCall.requests().tokenOf(caller.getUUID(), sired.getUUID()).isEmpty(), s + " opened a request");
            }
            for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID}) {
                cleanUp(caller);
                online(caller, sired);
                species(caller, s);
                Capture out = new Capture();
                helper.assertTrue(run(caller, 0, out, "bloodline call") == 1, s + " could not call: " + out.text());
            }
        } finally {
            cleanUp(caller, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void callHasCooldown(GameTestHelper helper) {
        ServerPlayer caller = named(helper, helper.getLevel(), "caller");
        ServerPlayer sired = named(helper, helper.getLevel(), "sired");
        try {
            SireRecorder.record(caller, sired);
            online(caller, sired);
            species(caller, Species.ORIGINAL);
            Capture first = new Capture();
            helper.assertTrue(run(caller, 0, first, "bloodline call") == 1, "the first call failed: " + first.text());
            helper.assertTrue(caller.getData(SpeciesRegistries.LAST_CALL) == helper.getLevel().getGameTime(), "lastCall is " + caller.getData(SpeciesRegistries.LAST_CALL));
            Capture second = new Capture();
            helper.assertTrue(run(caller, 0, second, "bloodline call") == 0, "the cooldown did not hold: " + second.text());
            helper.assertTrue(second.text().contains("cooling"), "no cooldown message: " + second.text());
            // 12000 ticks later it works again
            caller.setData(SpeciesRegistries.LAST_CALL, helper.getLevel().getGameTime() - SireCall.COOLDOWN_TICKS);
            helper.assertTrue(run(caller, 0, new Capture(), "bloodline call") == 1, "the cooldown never ended");
        } finally {
            cleanUp(caller, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void callAcrossDimensionsAfterAccept(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server has no nether");
        ServerPlayer caller = named(helper, helper.getLevel(), "caller");
        ServerPlayer sired = named(helper, nether, "sired");
        withChannel(sired);
        ServerPlayer stranger = named(helper, helper.getLevel(), "stranger");
        try {
            SireRecorder.record(caller, sired);
            online(caller, sired, stranger);
            species(caller, Species.ORIGINAL_HYBRID);
            place(helper, caller, 4);
            Capture out = new Capture();
            helper.assertTrue(run(caller, 0, out, "bloodline call") == 1, "the call failed: " + out.text());
            helper.assertTrue(sired.level() == nether, "the sired was moved before they accepted");
            String token = SireCall.requests().tokenOf(caller.getUUID(), sired.getUUID()).orElseThrow(() -> new AssertionError("no request"));

            Component prompt = SireCall.requestMessage(caller.getGameProfile().getName(), token);
            helper.assertTrue(clicks(prompt, "/bloodline accept " + token), "the prompt has no [Accept] click: " + prompt);

            helper.assertTrue(run(stranger, 0, new Capture(), "bloodline accept " + token) == 0, "a stranger used the token");
            helper.assertTrue(sired.level() == nether, "a stranger moved the sired");

            Capture accept = new Capture();
            helper.assertTrue(run(sired, 0, accept, "bloodline accept " + token) == 1, "accept failed: " + accept.text());
            helper.assertTrue(sired.level() == helper.getLevel(), "the sired is still in " + sired.level().dimension().location());
            // (a fake player's network handler ignores the final position, so the spot is checked on its own)
            Vec3 floor = helper.absoluteVec(new Vec3(4.5, 0, 4.5)); // the template has a barrier ceiling at y 2
            caller.moveTo(floor.x, floor.y, floor.z);
            Vec3 spot = SireCall.spotNear(helper.getLevel(), caller);
            helper.assertTrue(spot.distanceTo(caller.position()) <= 2.0001, "the spot is " + spot.distanceTo(caller.position()) + " blocks away");
            helper.assertTrue(spot.distanceTo(caller.position()) > 1, "the spot is the caller's own: " + spot);
            helper.assertTrue(helper.getLevel().noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(spot.x, spot.y, spot.z)), "the spot is inside a block: " + spot + " caller " + caller.position() + " feet " + helper.getLevel().getBlockState(net.minecraft.core.BlockPos.containing(spot)) + " head " + helper.getLevel().getBlockState(net.minecraft.core.BlockPos.containing(spot).above()));
            helper.assertTrue(run(sired, 0, new Capture(), "bloodline accept " + token) == 0, "a token worked twice");
        } finally {
            cleanUp(caller, sired, stranger);
            sired.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void callInTheSameDimensionAndCallerGone(GameTestHelper helper) {
        ServerPlayer caller = named(helper, helper.getLevel(), "caller");
        ServerPlayer sired = named(helper, helper.getLevel(), "sired");
        try {
            SireRecorder.record(caller, sired);
            online(caller, sired);
            species(caller, Species.HYBRID);
            place(helper, caller, 2);
            place(helper, sired, 12);
            Vec3 before = sired.position();
            helper.assertTrue(run(caller, 0, new Capture(), "bloodline call") == 1, "the call failed");
            String token = SireCall.requests().tokenOf(caller.getUUID(), sired.getUUID()).orElseThrow();
            helper.assertTrue(sired.position().equals(before), "moved without accepting");
            // the caller is offline when the answer comes
            online(sired);
            Capture out = new Capture();
            helper.assertTrue(run(sired, 0, out, "bloodline accept " + token) == 0, "accepted a call from an offline caller: " + out.text());
            helper.assertTrue(sired.position().equals(before), "moved to an offline caller");
            online(caller, sired);
            helper.assertTrue(run(sired, 0, new Capture(), "bloodline accept " + token) == 0, "a refused answer should not leave the request open");

            helper.assertTrue(run(caller, 0, new Capture(), "bloodline call") == 0, "cooldown should still apply");
            caller.removeData(SpeciesRegistries.LAST_CALL);
            helper.assertTrue(run(caller, 0, new Capture(), "bloodline call") == 1, "second call failed");
            String again = SireCall.requests().tokenOf(caller.getUUID(), sired.getUUID()).orElseThrow();
            // (a fake player's network handler ignores same-dimension teleports, so only the answer is checked here)
            helper.assertTrue(run(sired, 0, new Capture(), "bloodline accept " + again) == 1, "accept in the same dimension failed");
        } finally {
            cleanUp(caller, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deadSiredCannotAccept(GameTestHelper helper) {
        ServerPlayer caller = named(helper, helper.getLevel(), "caller");
        ServerPlayer sired = named(helper, helper.getLevel(), "sired");
        try {
            SireRecorder.record(caller, sired);
            online(caller, sired);
            species(caller, Species.ORIGINAL);
            place(helper, caller, 4);
            helper.assertTrue(run(caller, 0, new Capture(), "bloodline call") == 1, "the call failed");
            String token = SireCall.requests().tokenOf(caller.getUUID(), sired.getUUID()).orElseThrow();
            sired.setHealth(0);
            Capture out = new Capture();
            helper.assertTrue(run(sired, 0, out, "bloodline accept " + token) == 0, "a dead player accepted: " + out.text());
            helper.assertTrue(out.text().contains("dead"), "no refusal message: " + out.text());
            sired.setHealth(sired.getMaxHealth());
            helper.assertTrue(run(sired, 0, new Capture(), "bloodline accept " + token) == 1, "the request was lost with the refusal");
        } finally {
            cleanUp(caller, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void callerInTheAirLandsTheSiredOnTheGroundBelow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer caller = named(helper, level, "caller");
        ServerPlayer sired = named(helper, level, "sired");
        try {
            for (int x = 1; x <= 7; x++) {
                for (int z = 1; z <= 7; z++) {
                    helper.setBlock(new BlockPos(x, 20, z), Blocks.STONE);
                }
            }
            SireRecorder.record(caller, sired);
            online(caller, sired);
            species(caller, Species.ORIGINAL);
            Vec3 up = helper.absoluteVec(new Vec3(4.5, 24, 4.5));
            caller.moveTo(up.x, up.y, up.z);
            Vec3 spot = SireCall.landing(level, caller);
            helper.assertTrue(spot != null, "no landing found three blocks above the ground");
            helper.assertTrue(spot.y == helper.absoluteVec(new Vec3(0, 21, 0)).y, "the landing is at y " + spot.y);
            helper.assertTrue(level.noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(spot.x, spot.y, spot.z)), "the landing is inside a block");
            helper.assertTrue(run(caller, 0, new Capture(), "bloodline call") == 1, "the call failed");
            String token = SireCall.requests().tokenOf(caller.getUUID(), sired.getUUID()).orElseThrow();
            Capture out = new Capture();
            helper.assertTrue(run(sired, 0, out, "bloodline accept " + token) == 1, "accept was refused: " + out.text());
        } finally {
            for (int x = 1; x <= 7; x++) {
                for (int z = 1; z <= 7; z++) {
                    helper.setBlock(new BlockPos(x, 20, z), Blocks.AIR);
                }
            }
            cleanUp(caller, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void callerOverNothingRefusesTheAccept(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer caller = named(helper, level, "caller");
        ServerPlayer sired = named(helper, level, "sired");
        try {
            SireRecorder.record(caller, sired);
            online(caller, sired);
            species(caller, Species.ORIGINAL);
            Vec3 up = helper.absoluteVec(new Vec3(4.5, 40, 4.5));
            caller.moveTo(up.x, up.y, up.z);
            helper.assertTrue(SireCall.landing(level, caller) == null, "a landing was found in mid-air");
            helper.assertTrue(run(caller, 0, new Capture(), "bloodline call") == 1, "the call failed");
            String token = SireCall.requests().tokenOf(caller.getUUID(), sired.getUUID()).orElseThrow();
            Vec3 before = sired.position();
            Capture out = new Capture();
            helper.assertTrue(run(sired, 0, out, "bloodline accept " + token) == 0, "accept worked over nothing: " + out.text());
            helper.assertTrue(out.text().contains("no_floor"), "no refusal message: " + out.text());
            helper.assertTrue(sired.position().equals(before), "the sired was moved");
        } finally {
            cleanUp(caller, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unloadedMobsUntouched(GameTestHelper helper) {
        ServerPlayer caller = named(helper, helper.getLevel(), "caller");
        ServerLevel level = helper.getLevel();
        // a creature the index knows, in a chunk that is not loaded: no entity answers to its id
        UUID far = UUID.randomUUID();
        Cow near = helper.spawn(EntityType.COW, new Vec3(8.5, 2, 4.5));
        try {
            species(caller, Species.ORIGINAL);
            place(helper, caller, 2);
            SirelineIndex.get(level).add(caller.getUUID(), far, "minecraft:cow", "Far Cow");
            int loaded = level.getChunkSource().getLoadedChunksCount();
            Capture out = new Capture();
            helper.assertTrue(run(caller, 0, out, "bloodline call") == 0, "an unloaded mob counted as someone to call: " + out.text());
            helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded, "the call loaded chunks");
            helper.assertTrue(!caller.hasData(SpeciesRegistries.LAST_CALL), "calling nobody cost a cooldown");
            helper.assertTrue(level.getEntity(far) == null, "the unloaded mob appeared");
            helper.assertTrue(SirelineIndex.get(level).sireOf(far).isPresent(), "the call forgot an unloaded mob");

            // a loaded one answers
            SireRecorder.record(caller, near);
            Capture ok = new Capture();
            helper.assertTrue(run(caller, 0, ok, "bloodline call") == 1, "the loaded mob was not called: " + ok.text());
            helper.assertTrue(CallFollowers.isFollowing(near.getUUID()), "the loaded mob is not on its way");
            // and the call ends after 30 seconds
            CallFollowers.tick(level.getServer(), level.getGameTime() + 31 * 20L);
            helper.assertTrue(!CallFollowers.isFollowing(near.getUUID()), "the mob follows forever");
        } finally {
            cleanUp(caller);
            near.discard();
            SirelineIndex.get(level).remove(far);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void callOnlyReachesMobsOfTheCallersDimension(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        ServerPlayer caller = named(helper, nether, "caller");
        Cow cow = helper.spawn(EntityType.COW, new Vec3(8.5, 2, 4.5));
        try {
            species(caller, Species.ORIGINAL);
            SireRecorder.record(caller, cow);
            Capture out = new Capture();
            helper.assertTrue(run(caller, 0, out, "bloodline call") == 0, "a mob of another dimension was called: " + out.text());
            helper.assertTrue(!CallFollowers.isFollowing(cow.getUUID()), "a mob of another dimension follows");
        } finally {
            cleanUp(caller);
            cow.discard();
        }
        helper.succeed();
    }

    // ---- the dead leave the tree ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deadSiredMobLeavesTheIndexButPlayersStay(GameTestHelper helper) {
        ServerPlayer sire = named(helper, helper.getLevel(), "sire");
        ServerPlayer child = TestPlayers.createMortal(helper);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(6.5, 2, 4.5));
        try {
            SireRecorder.record(sire, cow);
            SireRecorder.record(sire, child);
            SirelineIndex index = SirelineIndex.get(helper.getLevel());
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(child, helper.getLevel().damageSources().generic()));
            helper.assertTrue(index.sireOf(child.getUUID()).isPresent(), "a dead player left the index");
            // an event that something else cancels is not a death
            LivingDeathEvent cancelled = new LivingDeathEvent(cow, helper.getLevel().damageSources().generic());
            cancelled.setCanceled(true);
            NeoForge.EVENT_BUS.post(cancelled);
            helper.assertTrue(index.sireOf(cow.getUUID()).isPresent(), "a cancelled death pruned the mob");
            cow.hurt(helper.getLevel().damageSources().genericKill(), 1000);
            helper.assertTrue(!cow.isAlive(), "the cow did not die");
            helper.assertTrue(index.sireOf(cow.getUUID()).isEmpty(), "a dead mob stayed in the index");
            helper.assertTrue(index.childrenOf(sire.getUUID()).stream().noneMatch(n -> n.id().equals(cow.getUUID())), "the sire still lists the dead mob");
        } finally {
            cleanUp(sire, child);
            cow.discard();
        }
        helper.succeed();
    }

    private static boolean clicks(Component c, String command) {
        if (c.getStyle().getClickEvent() != null && c.getStyle().getClickEvent().getAction() == ClickEvent.Action.RUN_COMMAND
                && command.equals(c.getStyle().getClickEvent().getValue())) {
            return true;
        }
        return c.getSiblings().stream().anyMatch(s -> clicks(s, command));
    }
}
