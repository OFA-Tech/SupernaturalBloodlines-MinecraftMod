package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import com.mojang.brigadier.ParseResults;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.Config;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminNet;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminSetPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminSnapshotPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandReplyPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommands;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminRows;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSettings;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingEntry;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingKind;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingValue;

/**
 * The server side of the Admin page, with the real config files of the test run (every test that writes puts the old value back). The key
 * used is {@code werewolves.full_form_level} of the core file (1 to 100): it is always loaded, so no part has to be on.
 */
@PrefixGameTestTemplate(false)
public final class AdminGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String KEY = AdminEntries.CORE_FILE + ":werewolves.full_form_level";

    private AdminGameTests() {}

    /** Counts what reaches the real config. */
    private static final class Counting implements AdminSettings.Writer {
        private final AdminSettings.Writer real;
        int sets;
        int saves;

        Counting(ServerPlayer p) {
            real = new AdminNet.Real(p.server);
        }

        @Override
        public void set(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?> value, Object boxed) {
            sets++;
            real.set(value, boxed);
        }

        @Override
        public void save(net.neoforged.neoforge.common.ModConfigSpec spec) {
            saves++;
            real.save(spec);
        }

        @Override
        public void setRule(SettingEntry rule, Object boxed) {
            sets++;
            real.setRule(rule, boxed);
        }
    }

    private static int level() {
        return Config.FULL_FORM_LEVEL.get();
    }

    private static AdminSettings.Result set(ServerPlayer p, int permission, String id, SettingValue v, Counting w, List<AdminSnapshotPayload> replies) {
        return AdminNet.handleSet(p, permission, new AdminSetPayload(id, v), w, replies::add);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nonOpPayloadIsRefused(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        int before = level();
        for (int permission : new int[] {-1, 0, 1}) {
            Counting w = new Counting(p);
            List<AdminSnapshotPayload> replies = new ArrayList<>();
            AdminSettings.Result r = set(p, permission, KEY, new SettingValue.Int(before == 50 ? 51 : 50), w, replies);
            helper.assertTrue(r.status() == AdminSettings.Status.NOT_OP, "permission " + permission + " got " + r.status());
            helper.assertTrue(w.sets == 0 && w.saves == 0, "permission " + permission + " reached the config (" + w.sets + " sets, " + w.saves + " saves)");
            helper.assertTrue(replies.isEmpty(), "permission " + permission + " was answered: nothing may leak to a non-OP");
        }
        helper.assertTrue(level() == before, "the value changed from " + before + " to " + level());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void opChangeIsAccepted(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        int before = level();
        int wanted = before == 50 ? 51 : 50;
        Counting w = new Counting(p);
        List<AdminSnapshotPayload> replies = new ArrayList<>();
        try {
            AdminSettings.Result r = set(p, 2, KEY, new SettingValue.Int(wanted), w, replies);
            helper.assertTrue(r.ok() && !r.restartRequired(), "an OP change was " + r);
            helper.assertTrue(level() == wanted, "the value is " + level() + ", not " + wanted);
            helper.assertTrue(w.sets == 1 && w.saves == 1, "sets " + w.sets + ", saves " + w.saves);
            helper.assertTrue(replies.size() == 1 && replies.get(0).outcome().map(o -> o.status().equals("OK") && o.id().equals(KEY)).orElse(false), "the reply is " + replies);
            AdminRows.Row row = replies.get(0).rows().stream().filter(x -> x.id().equals(KEY)).findFirst().orElseThrow();
            helper.assertTrue(row.current().equals(new SettingValue.Int(wanted)) && row.ranged() && row.min() == 1 && row.max() == 100, "the row is " + row);
        } finally {
            set(p, 2, KEY, new SettingValue.Int(before), new Counting(p), new ArrayList<>());
        }
        helper.assertTrue(level() == before, "the old value was not put back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void outOfRangeIsRefused(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        int before = level();
        for (SettingValue bad : List.<SettingValue>of(new SettingValue.Int(0), new SettingValue.Int(101), new SettingValue.Int(1L << 33), new SettingValue.Text("5"), new SettingValue.Dbl(5))) {
            Counting w = new Counting(p);
            List<AdminSnapshotPayload> replies = new ArrayList<>();
            AdminSettings.Result r = set(p, 2, KEY, bad, w, replies);
            helper.assertTrue(r.status() == AdminSettings.Status.INVALID, bad + " got " + r.status());
            helper.assertTrue(w.sets == 0 && w.saves == 0, bad + " reached the config");
            helper.assertTrue(replies.size() == 1 && replies.get(0).outcome().get().status().equals("INVALID"), "the refusal was not reported for " + bad);
        }
        helper.assertTrue(level() == before, "the value changed from " + before + " to " + level());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void clientAndHiddenAndMadeUpKeysAreRefused(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        Counting w = new Counting(p);
        List<AdminSnapshotPayload> replies = new ArrayList<>();
        helper.assertTrue(set(p, 2, "supernaturalbloodlines-hud-client.toml:mode", new SettingValue.Text("AUTO"), w, replies).status() == AdminSettings.Status.UNKNOWN, "a client key was accepted");
        helper.assertTrue(set(p, 2, AdminEntries.HUD_FILE + ":mixins.hide_in_controls_mixin", new SettingValue.Bool(false), w, replies).status() == AdminSettings.Status.HIDDEN, "the reserved key was accepted");
        helper.assertTrue(set(p, 2, AdminEntries.CORE_FILE + ":nothing.here", new SettingValue.Bool(false), w, replies).status() == AdminSettings.Status.UNKNOWN, "a made-up key was accepted");
        helper.assertTrue(w.sets == 0 && w.saves == 0, "a refused id reached the config");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void listEntryRejectedByParser(GameTestHelper helper) {
        SettingEntry layers = AdminEntries.find(AdminEntries.SPECIES_TENSURA_FILE + ":layers").orElseThrow();
        if (AdminSpecs.value(layers).isEmpty()) {
            helper.succeed(); // species_tensura is off
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        Object before = AdminSpecs.value(layers).orElseThrow().get();
        Counting w = new Counting(p);
        AdminSettings.Result r = set(p, 2, layers.id(), new SettingValue.Lines(List.of("this is not a layer entry")), w, new ArrayList<>());
        helper.assertTrue(r.status() == AdminSettings.Status.INVALID && r.detail().equals("this is not a layer entry"), "a bad line was " + r);
        helper.assertTrue(w.sets == 0 && w.saves == 0, "the bad line reached the config");
        helper.assertTrue(AdminSpecs.value(layers).orElseThrow().get().equals(before), "the list changed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotCarriesRangesChoicesAndSurvivesTheWire(GameTestHelper helper) {
        AdminSnapshotPayload s = AdminNet.snapshot(Optional.empty());
        Set<String> seen = new HashSet<>();
        for (AdminRows.Row row : s.rows()) {
            helper.assertTrue(seen.add(row.id()), "twice: " + row.id());
            SettingEntry e = AdminEntries.find(row.id()).orElseThrow();
            helper.assertTrue(!e.isHidden(), "a hidden key was sent: " + row.id());
            if (row.kind() == SettingKind.CHOICE) {
                helper.assertTrue(row.choices().size() >= 2 && row.choices().contains(((SettingValue.Text) row.current()).value()), "choices of " + row.id() + ": " + row.choices());
                for (String c : row.choices()) {
                    helper.assertTrue(AdminSpecs.valueSpec(e).orElseThrow().test(c), row.id() + " lists '" + c + "', which its spec refuses");
                }
            }
            if (row.kind() == SettingKind.INT || row.kind() == SettingKind.DOUBLE) {
                helper.assertTrue(row.ranged() && row.min() < row.max(), row.id() + " has no range");
            }
            helper.assertTrue(row.clientCopy() == e.clientCopy() && row.restart() == (e.restart() == SettingEntry.Restart.SERVER_RESTART), "flags of " + row.id());
        }
        helper.assertTrue(seen.contains(KEY) && seen.stream().filter(id -> id.contains(":parts.")).count() == 9, "the core rows are missing: " + seen);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        AdminSnapshotPayload.STREAM_CODEC.encode(buf, s);
        helper.assertTrue(AdminSnapshotPayload.STREAM_CODEC.decode(buf).equals(s), "the snapshot does not survive the wire");
        helper.succeed();
    }

    /** The OP view of a profile has its own payload and is answered only through the Admin permission check; the ordinary request is never an OP view. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void opProfileIsReachableOnlyThroughTheAdminCheck(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        UUID subject = UUID.randomUUID();
        List<net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload> out = new ArrayList<>();
        for (int level : new int[] {-1, 0, 1}) {
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfileNet.handleAdminRequest(p, level, subject, out::add), "level " + level + " got an OP view");
        }
        helper.assertTrue(out.isEmpty(), "a non-OP was answered: nothing may leak");
        helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfileNet.handleAdminRequest(p, 2, subject, out::add)
                && out.size() == 1 && out.get(0).opView() && out.get(0).subject().equals(subject), "an OP was not given the OP view");
        out.clear();
        // the player-facing request takes no permission at all, so even an OP gets only what a player gets
        net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfileNet.handleRequest(p, subject, out::add);
        helper.assertTrue(out.size() == 1 && !out.get(0).opView() && out.get(0).facts().isEmpty(), "the ordinary request of an unknown person was " + out);
        helper.succeed();
    }

    // ---- Task 6: debug actions run as the typed command ----

    private static CommandSourceStack stack(ServerPlayer p, int permission) {
        return new CommandSourceStack(CommandSource.NULL, p.position(), p.getRotationVector(), helperLevel(p), permission, p.getGameProfile().getName(),
                p.getDisplayName(), p.server, p);
    }

    private static net.minecraft.server.level.ServerLevel helperLevel(ServerPlayer p) {
        return (net.minecraft.server.level.ServerLevel) p.level();
    }

    private static boolean executable(ServerPlayer p, int permission, String command) {
        ParseResults<CommandSourceStack> parsed = p.server.getCommands().getDispatcher().parse(command, stack(p, permission));
        return !parsed.getReader().canRead() && parsed.getContext().getCommand() != null;
    }

    private static List<String> sampleArgs(AdminCommands.Row row) {
        List<String> out = new ArrayList<>();
        for (AdminCommands.Arg a : row.args()) {
            out.add(switch (a.name()) {
                case "player", "leader" -> UUID.randomUUID().toString();
                case "token" -> "0123abcd";
                case "value" -> "true";
                case "n" -> "1";
                case "race" -> "vampire";
                default -> "VAMPIRE";
            });
        }
        return out;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void debugButtonsRefusedForNonOp(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        int checked = 0;
        List<String> notParsed = new ArrayList<>();
        for (AdminCommands.Row row : AdminCommands.ALL) {
            if (row.level() < 2) {
                continue;
            }
            // the route refuses an OP row for a level 1 sender before any command is built, whatever Brigadier would say
            List<HubCommandReplyPayload> answered = new ArrayList<>();
            helper.assertTrue(!HubCommands.run(p, 1, new HubCommandPayload(row.id(), sampleArgs(row)), x -> stack(x, 4), answered::add) && answered.isEmpty(),
                    row.id() + " ran or was answered for a level 1 sender");
            String command = AdminCommands.command(row, sampleArgs(row), uuid -> "Steve").orElseThrow(() -> new AssertionError("no command for " + row.id()));
            if (row.id().equals("B2")) {
                continue; // Brigadier lets everyone parse "bloodline tree <player>": the command itself refuses another player's tree below level 2 (SirelineCommandGameTests)
            }
            boolean registered = executable(p, 4, command);
            if (!registered) {
                notParsed.add(command + " => " + p.server.getCommands().getDispatcher().parse(command, stack(p, 4)).getExceptions().values());
                // the part that owns the form is off (the eclipse and the gene need the species part and Werewolves)
                helper.assertTrue(!executable(p, 2, command), row.id() + " parses at level 2 but not at level 4");
                continue;
            }
            checked++;
            helper.assertTrue(executable(p, 2, command), row.id() + " does not parse at level 2: " + command);
            helper.assertTrue(!executable(p, 1, command), row.id() + " parses for a level 1 sender: " + command);
            helper.assertTrue(!executable(p, 0, command), row.id() + " parses for a level 0 sender: " + command);
        }
        helper.assertTrue(checked >= 8, "only " + checked + " OP forms are registered (the debug root alone has 8 without the eclipse): " + notParsed);
        helper.succeed();
    }

    private static HubCommandReplyPayload run(ServerPlayer p, int claimed, int brigadier, String rowId, List<String> args, List<HubCommandReplyPayload> out) {
        HubCommands.run(p, claimed, new HubCommandPayload(rowId, args), x -> stack(x, brigadier), out::add);
        return out.isEmpty() ? null : out.get(out.size() - 1);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hubCommandRouteChecksTheRowTheArgumentsAndBrigadier(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<HubCommandReplyPayload> out = new ArrayList<>();
        String player = UUID.randomUUID().toString();

        // an OP row from a level 1 sender never runs and is never answered
        helper.assertTrue(!HubCommands.run(p, 1, new HubCommandPayload("D4", List.of(player)), x -> stack(x, 4), out::add) && out.isEmpty(), "a level 1 sender ran or was answered for D4: " + out);
        // a row that is not in the table, and arguments that do not fit the row, are answered and not run
        HubCommandReplyPayload unknown = run(p, 4, 4, "ZZ", List.of(), out);
        helper.assertTrue(unknown != null && !unknown.success(), "an unknown row was run");
        out.clear();
        HubCommandReplyPayload bad = run(p, 0, 0, "P4", List.of("Steve"), out);
        helper.assertTrue(bad != null && !bad.success(), "a name was taken where a UUID is needed");
        out.clear();
        HubCommandReplyPayload smuggled = run(p, 0, 0, "P4", List.of(player + " extra"), out);
        helper.assertTrue(smuggled != null && !smuggled.success(), "extra words were taken");
        out.clear();
        // a sender who claims level 2 but whom Brigadier knows at level 0 is stopped by Brigadier: the server check is the command's own
        HubCommandReplyPayload stopped = run(p, 2, 0, "D4", List.of(player), out);
        helper.assertTrue(stopped != null && !stopped.success(), "Brigadier let a level 0 source run an OP command: " + stopped);
        out.clear();
        // a player command runs as the sender and answers (an unknown target is the command's own refusal, not the route's)
        HubCommandReplyPayload player0 = run(p, 0, 0, "P3", List.of(), out);
        helper.assertTrue(player0 != null && player0.rowId().equals("P3"), "the answer names the row: " + player0);
        helper.succeed();
    }

    private static boolean says(HubCommandReplyPayload r, String key) {
        return r != null && !r.success() && r.lines().stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("supernaturalbloodlines.hub.command." + key));
    }

    /** A token nothing is open under any more (answered or run out) is answered "already answered" and never run; a command whose part is off says it is not available. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClosedTokenAndAMissingCommandAreAnsweredNotRun(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<HubCommandReplyPayload> out = new ArrayList<>();
        boolean bloodline = p.server.getCommands().getDispatcher().getRoot().getChild("bloodline") != null;
        if (bloodline) {
            HubCommandReplyPayload closed = run(p, 0, 0, "B4", List.of("deadbeef"), out);
            helper.assertTrue(says(closed, "answered"), "a token nothing is open under was answered " + closed);
        }
        out.clear();
        if (p.server.getCommands().getDispatcher().getRoot().getChild("pack") == null) {
            HubCommandReplyPayload missing = run(p, 0, 0, "P3", List.of(), out);
            helper.assertTrue(says(missing, "unavailable"), "a command of a part that is off was answered " + missing);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerOverTheirRateIsToldTooQuick(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<HubCommandReplyPayload> out = new ArrayList<>();
        boolean ran = HubCommands.dispatch(p, false, 0, new HubCommandPayload("P3", List.of()), x -> stack(x, 0), out::add);
        helper.assertTrue(!ran && out.size() == 1 && says(out.get(0), "too_quick") && out.get(0).rowId().equals("P3"), "over the rate: " + out);
        out.clear();
        HubCommands.dispatch(p, true, 0, new HubCommandPayload("ZZ", List.of()), x -> stack(x, 0), out::add);
        helper.assertTrue(out.size() == 1 && says(out.get(0), "unknown"), "within the rate the route ran: " + out);
        helper.succeed();
    }

    /** Saving a key the clients also read pushes it at once (the file watcher would only get there later); a key they do not read pushes nothing. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSavedClientCopyKeyIsBroadcastAtOnce(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        SettingEntry e = AdminEntries.ALL.stream().filter(SettingEntry::clientCopy).filter(x -> x.kind() == SettingKind.BOOL).findFirst().orElseThrow();
        if (AdminSpecs.value(e).isEmpty()) {
            helper.succeed(); // the part that owns it is off
            return;
        }
        int[] pushed = {0};
        AdminSettings.onClientCopySaved(server -> pushed[0]++);
        boolean before = (Boolean) AdminSpecs.value(e).orElseThrow().get();
        List<AdminSnapshotPayload> replies = new ArrayList<>();
        try {
            helper.assertTrue(set(p, 2, e.id(), new SettingValue.Bool(!before), new Counting(p), replies).ok(), "the OP save failed");
            helper.assertTrue(pushed[0] == 1, "a saved client key was pushed " + pushed[0] + " times");
            int atKey = pushed[0];
            helper.assertTrue(set(p, 2, KEY, new SettingValue.Int(level()), new Counting(p), replies).ok() && pushed[0] == atKey, "a key clients do not read was pushed");
        } finally {
            set(p, 2, e.id(), new SettingValue.Bool(before), new Counting(p), new ArrayList<>());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void opDebugRowRunsAndAnswersInPageAndChat(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<HubCommandReplyPayload> out = new ArrayList<>();
        if (!executable(p, 4, "supernaturalbloodlines debug eclipse start")) {
            helper.succeed(); // the eclipse needs the species part and Werewolves
            return;
        }
        try {
            HubCommandReplyPayload start = run(p, 2, 2, "D9", List.of(), out);
            helper.assertTrue(start != null && start.success() && !start.lines().isEmpty(), "D9 answered " + start);
            HubCommandReplyPayload again = run(p, 2, 2, "D9", List.of(), out);
            helper.assertTrue(again != null && !again.success() && !again.lines().isEmpty(), "a second D9 answered " + again);
        } finally {
            run(p, 2, 2, "D10", List.of(), new ArrayList<>());
        }
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        HubCommandReplyPayload last = out.get(out.size() - 1);
        HubCommandReplyPayload.STREAM_CODEC.encode(buf, last);
        helper.assertTrue(HubCommandReplyPayload.STREAM_CODEC.decode(buf).equals(last), "the reply does not survive the wire");
        helper.succeed();
    }
}
