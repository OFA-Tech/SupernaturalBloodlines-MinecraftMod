package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * Every command form of ours, once, with where the hub holds it. A form is the typed command with {@code <arg>} for a required argument
 * and {@code [arg]} for an optional one. The hub runs a form by its row id (see {@link #command}): the server rebuilds the typed command
 * from the row and the checked arguments, so a client can run nothing that is not in this table. {@code CommandCoverageGameTests} walks the
 * real dispatcher and fails the build for a form that has no row (and for a row that no form has).
 */
public final class AdminCommands {
    /** Where the player or the OP uses it. */
    /** {@code PAGE}: the command only shows something (the tree, the pack info), which its page already draws, so it has no button of its own. */
    public enum Ui { HUB_BUTTON, ADMIN_ACTION, CHAT_ONLY, PAGE }

    /** @param level the permission level Brigadier asks; {@code reason} is why a {@code CHAT_ONLY} row has no button; {@code page} is the hub place that holds it */
    public record Row(String id, String form, int level, Ui ui, @Nullable String reason, String page) {
        /** The first word: the command root. */
        public String root() {
            return form.substring(0, form.indexOf(' '));
        }

        /** The arguments in order: required ones first as written, optional ones marked. */
        public List<Arg> args() {
            List<Arg> out = new ArrayList<>();
            for (String word : form.split(" ")) {
                if (word.startsWith("<")) {
                    out.add(new Arg(word.substring(1, word.length() - 1), false));
                } else if (word.startsWith("[")) {
                    out.add(new Arg(word.substring(1, word.length() - 1), true));
                }
            }
            return out;
        }

        /** The shapes the dispatcher has for this row, each argument as {@code <>}: an optional one gives a shape with it and one without. */
        public List<String> shapes() {
            List<String> out = new ArrayList<>(List.of(""));
            for (String word : form.split(" ")) {
                boolean optional = word.startsWith("[");
                String w = word.startsWith("<") || optional ? "<>" : word;
                List<String> next = new ArrayList<>();
                for (String s : out) {
                    next.add(s.isEmpty() ? w : s + " " + w);
                    if (optional) {
                        next.add(s);
                    }
                }
                out = next;
            }
            return out;
        }
    }

    /** An argument by its name in the form; the name decides what the server accepts. */
    public record Arg(String name, boolean optional) {
        private static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.-]{1,32}");
        private static final Pattern TOKEN = Pattern.compile("[0-9a-f]{8}");
        private static final Pattern INTEGER = Pattern.compile("-?\\d+");
        private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_:.-]{1,64}");

        public boolean isPerson() {
            return name.equals("player") || name.equals("leader");
        }

        /** Whether a person argument is a UUID or a plain name once the server has looked the UUID up (a selector or a space is neither). */
        boolean safeAfterLookup(String value) {
            return isPerson() ? UUID_TEXT.matcher(value).matches() || NAME.matcher(value).matches() : accepts(value);
        }

        /**
         * Whether {@code value} is safe to put in the command: no space, no selector, no quote. A person is always named by UUID (the
         * commands take one in place of a name), so the hub never depends on a name and the route never carries one.
         */
        public boolean accepts(String value) {
            return switch (name) {
                case "player", "leader" -> UUID_TEXT.matcher(value).matches();
                case "token" -> TOKEN.matcher(value).matches();
                case "value" -> value.equals("true") || value.equals("false");
                case "n" -> {
                    // Brigadier's integer reader takes a leading "-" but not "+", and no spaces: Integer.parseInt alone would let "+5" through
                    if (!INTEGER.matcher(value).matches()) {
                        yield false;
                    }
                    try {
                        Integer.parseInt(value);
                        yield true;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                default -> WORD.matcher(value).matches();
            };
        }
    }

    private static final String ROOT = "supernaturalbloodlines debug";
    private static final String BOSS = "supernaturalbloodlines boss";

    public static final List<Row> ALL = List.of(
            row("B1", "bloodline tree", 0, Ui.PAGE, "BLOODLINE"),
            row("B2", "bloodline tree <player>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("B3", "bloodline call", 0, Ui.HUB_BUTTON, "BLOODLINE"),
            row("B4", "bloodline accept <token>", 0, Ui.HUB_BUTTON, "REQUESTS"),
            row("B5", "bloodline compel <token> discount", 0, Ui.HUB_BUTTON, "REQUESTS"),
            row("B6", "bloodline compel <token> follow", 0, Ui.HUB_BUTTON, "REQUESTS"),
            row("B7", "bloodline gene doppelganger <player> <value>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("B8", "bloodline rites list <player>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("B9", "bloodline rites learn <player> <rite>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("B10", "bloodline rites forget <player> <rite>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("P1", "pack invite <player>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P2", "pack accept [token]", 0, Ui.HUB_BUTTON, "REQUESTS"),
            row("P3", "pack leave", 0, Ui.HUB_BUTTON, "PACK"),
            row("P4", "pack kick <player>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P5", "pack ally invite <player>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P6", "pack ally accept <token>", 0, Ui.HUB_BUTTON, "REQUESTS"),
            row("P7", "pack ally remove <player>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P8", "pack challenge <leader>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P9", "pack challenge accept", 0, Ui.HUB_BUTTON, "PACK"),
            row("P10", "pack challenge decline", 0, Ui.HUB_BUTTON, "PACK"),
            row("P11", "pack heal <player>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P12", "pack info", 0, Ui.PAGE, "PACK"),
            row("P13", "pack emissary [player]", 0, Ui.HUB_BUTTON, "PACK"),
            row("P14", "pack roar", 0, Ui.HUB_BUTTON, "PACK"),
            row("P15", "pack anchor [player]", 0, Ui.HUB_BUTTON, "PACK"),
            row("P16", "pack protect on", 0, Ui.HUB_BUTTON, "PACK"),
            row("P17", "pack protect off confirm", 0, Ui.HUB_BUTTON, "PACK"),
            row("P19", "pack surge", 0, Ui.HUB_BUTTON, "PACK"),
            row("P20", "pack spirits", 0, Ui.HUB_BUTTON, "PACK"),
            row("P21", "pack npc order <npc> <order>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P22", "pack npc hunt <npc>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P23", "pack npc sort <npc>", 0, Ui.HUB_BUTTON, "PACK"),
            row("P24", "pack npc release <npc>", 0, Ui.HUB_BUTTON, "PACK"),
            new Row("P25", "pack npc list", 0, Ui.CHAT_ONLY, "the Pack page lists the wolves as rows; the command only prints the same list in chat", "PACK"),
            new Row("P18", "pack protect off", 0, Ui.CHAT_ONLY, "only a reminder to add confirm: the hub asks first and then runs P17", "PACK"),
            row("D1", ROOT + " get <player>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D2", ROOT + " set <player> <species>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D3", ROOT + " event <player> <event>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D4", ROOT + " supernatural <player> show", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D5", ROOT + " supernatural <player> backfill", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D6", ROOT + " supernatural <player> reset", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D7", ROOT + " supernatural <player> sp <n>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D8", ROOT + " supernatural <player> rung <race> <n>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D9", ROOT + " eclipse start", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D10", ROOT + " eclipse stop", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D11", ROOT + " npcpack", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D12", ROOT + " npcs list", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("D13", ROOT + " npcs release <player>", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("X1", BOSS + " <boss> spawn", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("X2", BOSS + " <boss> state", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("X3", BOSS + " <boss> desiccate", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("X4", BOSS + " <boss> return", 2, Ui.ADMIN_ACTION, "ADMIN"),
            row("X5", BOSS + " <boss> reset", 2, Ui.ADMIN_ACTION, "ADMIN"));

    private AdminCommands() {}

    private static Row row(String id, String form, int level, Ui ui, String page) {
        return new Row(id, form, level, ui, null, page);
    }

    public static Optional<Row> byId(String id) {
        return ALL.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    /** The typed command for a row and its arguments (optional ones may be left out), or empty when an argument is missing, extra or unsafe. */
    public static Optional<String> command(Row row, List<String> args) {
        return command(row, args, Function.identity());
    }

    /**
     * Like {@link #command(Row, List)}, but each person (a UUID on the wire) goes through {@code lookup} first: some commands take only the name
     * of an online player ({@code EntityArgument.player} refuses a UUID), so the server swaps in the name it knows. The result must still be safe.
     */
    public static Optional<String> command(Row row, List<String> args, Function<String, String> lookup) {
        List<Arg> wanted = row.args();
        long required = wanted.stream().filter(a -> !a.optional()).count();
        if (args.size() < required || args.size() > wanted.size()) {
            return Optional.empty();
        }
        StringBuilder out = new StringBuilder();
        int next = 0;
        for (String word : row.form().split(" ")) {
            if (word.startsWith("<") || word.startsWith("[")) {
                if (next >= args.size()) {
                    break; // an optional argument left out: every later one is optional too
                }
                Arg a = wanted.get(next);
                String value = args.get(next++);
                if (!a.accepts(value)) {
                    return Optional.empty();
                }
                String used = a.isPerson() ? lookup.apply(value) : value;
                if (used == null || !a.safeAfterLookup(used)) {
                    return Optional.empty();
                }
                word = used;
            }
            out.append(out.isEmpty() ? "" : " ").append(word);
        }
        return Optional.of(out.toString());
    }
}
