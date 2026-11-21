package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.AdminClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminRequestPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminSetPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEdit;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminRows;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingKind;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingValue;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TextEdit;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Admin (operators only, permission level 2 or more, drawn only then): the server's settings by part with a search, the debug actions with
 * a player picker and a reply area, and the status of the parts. The server checks every payload and every command again; nothing here is
 * trusted. A change applies on Apply or Enter, never per keystroke, and a part switch asks first.
 */
public final class AdminPage implements HubPageView {
    static final String KEY = PageKit.HUB + "admin.";
    static final String DEBUG = "debug";
    static final String PARTS = "parts";
    static final int MAX_FIELD = 4096;

    private String section = AdminEntries.SECTIONS.get(0);
    private final TextEdit search = new TextEdit("", false, 48);
    private boolean searching;
    @Nullable
    private String editing;
    private TextEdit editor = new TextEdit("", false, MAX_FIELD);
    private final Map<String, String> drafts = new HashMap<>();
    private final Set<String> refused = new HashSet<>();
    private final Set<String> saved = new HashSet<>();
    private final Map<String, String> why = new HashMap<>();
    @Nullable
    private AdminSnapshotPayload seen;
    private long lastRender;
    private long lastSent;
    private UUID player;
    private String playerName = "";
    private int speciesAt;
    private int eventAt;
    private int raceAt;
    private int bossAt;
    private int number = 100;

    private static volatile String devSection;


    /** Development only (the dev screenshots): the section the next frame shows, or "search:text" to type into the search box. */
    public static void devSection(String section) {
        devSection = section;
    }

    /** What the page does with a snapshot that carries the outcome of an edit: saved, or refused with the server's words. */
    private void absorb(AdminSnapshotPayload snap) {
        seen = snap;
        snap.outcome().ifPresent(o -> {
            if (o.status().equals("OK")) {
                saved.add(o.id());
                refused.remove(o.id());
                drafts.remove(o.id());
                why.remove(o.id());
            } else {
                saved.remove(o.id());
                refused.add(o.id());
                why.put(o.id(), o.key().isEmpty() ? o.status() : o.key() + (o.detail().isEmpty() ? "" : "|" + o.detail()));
            }
        });
    }

    private static void send(String id, SettingValue v) {
        var c = Minecraft.getInstance().getConnection();
        if (c != null && c.hasChannel(AdminSetPayload.TYPE)) {
            PacketDistributor.sendToServer(new AdminSetPayload(id, v));
        }
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        long now = System.currentTimeMillis();
        if (net.ofatech.supernaturalbloodlines.shared.admin.RequestPace.due(lastSent, lastRender, AdminClient.snapshot() != null, now)) {
            var c = Minecraft.getInstance().getConnection();
            if (c != null && c.hasChannel(AdminRequestPayload.TYPE)) {
                PacketDistributor.sendToServer(AdminRequestPayload.INSTANCE);
                lastSent = now;
            }
        }
        lastRender = now;
        // "Open in Admin" on a profile: the debug actions, with that person already picked
        var handed = net.ofatech.supernaturalbloodlines.shared.hub.AdminHandoff.take();
        if (handed.isPresent()) {
            player = handed.get().id();
            playerName = handed.get().name();
            section = DEBUG;
        }
        String dev = devSection;
        if (dev != null) {
            devSection = null;
            while (!search.text().isEmpty()) {
                search.backspace();
            }
            if (dev.startsWith("search_")) {
                for (char ch : dev.substring(7).toCharArray()) {
                    search.type(ch);
                }
            } else {
                section = dev;
            }
        }
        AdminSnapshotPayload snap = AdminClient.snapshot();
        if (snap != null && snap != seen) {
            absorb(snap);
        }
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        y = chips(f, r, y);
        y = searchBox(f, r, y + 3);
        if (snap == null) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "loading"), r.x(), y + 4, r.w(), t.muted());
        } else if (section.equals(DEBUG) && search.text().isEmpty()) {
            y = debug(f, r, y + 4);
        } else if (section.equals(PARTS) && search.text().isEmpty()) {
            y = parts(f, r, y + 4, snap);
        } else {
            y = settings(f, r, y + 4, snap);
        }
        f.end(y);
        PageWidgets.info(f, Component.translatable(KEY + "title"), Component.translatable(KEY + "debug_note"));

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = side(f, s, sy);
        f.end(sy);
    }

    private int chips(HubScreen.Frame f, Rect r, int y) {
        List<String> ids = new ArrayList<>(AdminEntries.SECTIONS);
        ids.add(DEBUG);
        ids.add(PARTS);
        if (!AdminEntries.RULES.isEmpty()) {
            ids.add("gamerules");
        }
        List<Component> labels = ids.stream().map(s -> (Component) Component.translatable(KEY + "section." + s)).toList();
        Flow.Result flow = Flow.wrap(new Rect(r.x(), y, r.w(), 0), labels.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList(), PageWidgets.H, 3);
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            PageWidgets.button(f, "stage:admin:section:" + id, flow.rects().get(i), labels.get(i), true, id.equals(section), null, () -> {
                section = id;
                editing = null;
            });
        }
        return flow.bottom();
    }

    private int searchBox(HubScreen.Frame f, Rect r, int y) {
        Rect box = new Rect(r.x(), y, r.w(), PageWidgets.H);
        String shown = search.text().isEmpty() && !searching ? Component.translatable(KEY + "search").getString() : search.text();
        PageWidgets.field(f, "stage:admin:search", box, shown, search.caret(), searching, false, Component.translatable(KEY + "search_tip"), () -> {
            searching = true;
            editing = null;
        });
        return box.bottom();
    }

    private int settings(HubScreen.Frame f, Rect r, int y, AdminSnapshotPayload snap) {
        HubTheme t = f.theme();
        List<AdminRows.Row> rows = snap.rows().stream().filter(row -> search.text().isEmpty()
                ? AdminEntries.find(row.id()).map(e -> e.section().equals(section)).orElse(false) : true).toList();
        rows = AdminEdit.filter(rows, search.text(), row -> row.id() + " " + AdminEdit.humanize(row.id()));
        if (rows.isEmpty()) {
            return PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        for (AdminRows.Row row : rows) {
            y = row(f, r, y + 3, row);
        }
        return y;
    }

    private String draftOf(AdminRows.Row row) {
        return drafts.getOrDefault(row.id(), AdminEdit.format(row.current()));
    }

    private int row(HubScreen.Frame f, Rect r, int y, AdminRows.Row row) {
        HubTheme t = f.theme();
        String id = "stage:admin:row:" + row.id();
        Component label = Component.translatableWithFallback(KEY + "key." + row.id(), AdminEdit.humanize(row.id()));
        Component idLine = Component.literal(row.id()).withStyle(ChatFormatting.GRAY);
        boolean inline = row.kind() == SettingKind.BOOL;
        int labelW = inline ? r.w() - 2 - 40 : r.w() - 2;
        int labelTop = y;
        y = PageKit.wrapped(f, label, r.x() + 2, y, labelW, t.ink());
        List<Component> badges = new ArrayList<>();
        AdminEdit.State state = AdminEdit.state(row.current(), row.def(), draftOf(row), refused.contains(row.id()), saved.contains(row.id()));
        if (row.restart()) {
            badges.add(Component.translatable(KEY + "restart"));
        }
        if (state != AdminEdit.State.DEFAULT) {
            badges.add(Component.translatable(KEY + "state." + state.name().toLowerCase(Locale.ROOT)));
        }
        Component range = row.ranged() ? Component.translatable(KEY + "range", fmt(row, row.min()), fmt(row, row.max())) : Component.empty();
        List<Component> tip = new ArrayList<>(List.of(label, idLine, range, Component.translatable(KEY + "default", AdminEdit.format(row.def()))));
        if (refused.contains(row.id())) {
            tip.add(refusal(row.id()));
        }
        int controlsY = y;
        switch (row.kind()) {
            case BOOL -> {
                boolean on = Boolean.parseBoolean(AdminEdit.format(row.current()));
                Rect b = new Rect(r.right() - 36, labelTop, 36, PageWidgets.H);
                PageWidgets.toggle(f, id, b, on, Component.translatable(KEY + "toggle_tip"), () -> {
                    if (row.restart()) {
                        f.confirm(Component.translatable(KEY + "restart_title"), Component.translatable(KEY + "restart_body", label),
                                Component.translatable(KEY + "apply"), () -> send(row.id(), new SettingValue.Bool(!on)));
                    } else {
                        send(row.id(), new SettingValue.Bool(!on));
                    }
                });
                controlsY = Math.max(y, b.bottom());
            }
            case CHOICE -> {
                List<Integer> widths = new ArrayList<>();
                List<Component> labels = new ArrayList<>();
                for (String c : row.choices()) {
                    Component l = Component.translatableWithFallback(KEY + "choice." + c, HubModel.pretty(c));
                    labels.add(l);
                    widths.add(PageWidgets.widthOf(f.font(), l));
                }
                Flow.Result flow = Flow.wrap(new Rect(r.x() + 2, y, r.w() - 2, 0), widths, PageWidgets.H, PageWidgets.GAP);
                for (int i = 0; i < row.choices().size(); i++) {
                    String c = row.choices().get(i);
                    PageWidgets.button(f, id + ":" + c, flow.rects().get(i), labels.get(i), true, c.equals(AdminEdit.format(row.current())), null,
                            () -> send(row.id(), new SettingValue.Text(c)));
                }
                controlsY = flow.bottom();
            }
            default -> controlsY = typed(f, r, y, row, id, label, tip);
        }
        y = controlsY + 1;
        if (badges.isEmpty() && !refused.contains(row.id())) {
            f.target(id + ":info", new Rect(r.x(), labelTop, r.w() - 40, Math.max(PageKit.LINE, y - labelTop)), tip, null);
            return y;
        }
        StringBuilder line = new StringBuilder();
        for (Component b : badges) {
            line.append(line.length() == 0 ? "" : " \u00b7 ").append(b.getString());
        }
        int color = refused.contains(row.id()) ? PageWidgets.warn(t) : t.muted();
        y = PageKit.wrapped(f, Component.literal(line.toString()), r.x() + 2, y, r.w() - 2, color);
        if (refused.contains(row.id())) {
            y = PageKit.wrapped(f, refusal(row.id()), r.x() + 2, y, r.w() - 2, PageWidgets.warn(t));
        }
        f.target(id + ":info", new Rect(r.x(), y - PageKit.LINE, r.w(), PageKit.LINE), tip, null);
        return y;
    }

    private static String fmt(AdminRows.Row row, double v) {
        return row.kind() == SettingKind.INT ? String.valueOf((long) v) : AdminEdit.format(new SettingValue.Dbl(v));
    }

    /** The server's refusal in words: a lang key from the rules, with the offending line when it names one. */
    private Component refusal(String id) {
        String w = why.getOrDefault(id, "");
        int bar = w.indexOf('|');
        String key = bar < 0 ? w : w.substring(0, bar);
        Component base = Component.translatableWithFallback(key, w.isEmpty() ? KEY + "refused" : key);
        return bar < 0 ? base : Component.empty().append(base).append(": ").append(w.substring(bar + 1));
    }

    /** A number, text or list: a box you click to type into, stepper arrows for numbers, and Apply. */
    private int typed(HubScreen.Frame f, Rect r, int y, AdminRows.Row row, String id, Component label, List<Component> tip) {
        boolean number = row.kind() == SettingKind.INT || row.kind() == SettingKind.DOUBLE;
        boolean list = row.kind() == SettingKind.LIST;
        boolean on = row.id().equals(editing);
        int x = r.x() + 2;
        if (number) {
            Rect minus = new Rect(x, y, PageWidgets.H, PageWidgets.H);
            PageWidgets.stepper(f, id + ":minus", minus, Component.literal("-"), true, Component.translatable(KEY + "less", label), () -> step(row, -1));
            x = minus.right() + 2;
        }
        int applyW = PageWidgets.widthOf(f.font(), Component.translatable(KEY + "apply"));
        int plusW = number ? PageWidgets.H + 2 : 0;
        Rect box = new Rect(x, y, Math.max(24, r.right() - x - applyW - plusW - 4), PageWidgets.H);
        String text = on ? editor.text() : draftOf(row);
        PageWidgets.field(f, id, box, text, on ? editor.caret() : 0, on, refused.contains(row.id()), list ? Component.translatable(KEY + "list_tip") : label, () -> {
            editing = row.id();
            searching = false;
            editor = new TextEdit(draftOf(row), list, MAX_FIELD);
        });
        x = box.right() + 2;
        if (number) {
            Rect plus = new Rect(x, y, PageWidgets.H, PageWidgets.H);
            PageWidgets.stepper(f, id + ":plus", plus, Component.literal("+"), true, Component.translatable(KEY + "more", label), () -> step(row, 1));
            x = plus.right() + 2;
        }
        Rect apply = new Rect(x, y, applyW, PageWidgets.H);
        PageWidgets.button(f, id + ":apply", apply, Component.translatable(KEY + "apply"), true, true, null, () -> commit(row));
        return box.bottom();
    }

    private void step(AdminRows.Row row, int dir) {
        Optional<SettingValue> v = AdminEdit.parse(row.kind(), draftOf(row));
        double now = v.map(x -> x instanceof SettingValue.Int i ? (double) i.value() : x instanceof SettingValue.Dbl d ? d.value() : 0.0).orElse(0.0);
        double delta = row.kind() == SettingKind.INT ? dir : dir * Math.max(0.01, (row.max() - row.min()) / 20.0);
        double next = AdminEdit.step(now, delta, row.min(), row.max());
        drafts.put(row.id(), row.kind() == SettingKind.INT ? String.valueOf((long) next) : AdminEdit.format(new SettingValue.Dbl(next)));
        saved.remove(row.id());
        refused.remove(row.id());
    }

    private void commit(AdminRows.Row row) {
        String text = row.id().equals(editing) ? editor.text() : draftOf(row);
        Optional<SettingValue> v = AdminEdit.parse(row.kind(), text);
        editing = null;
        if (v.isEmpty()) {
            refused.add(row.id());
            why.put(row.id(), "supernaturalbloodlines.admin.refused.type");
            return;
        }
        drafts.put(row.id(), text);
        send(row.id(), v.get());
    }

    private int parts(HubScreen.Frame f, Rect r, int y, AdminSnapshotPayload snap) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "parts_note"), r.x(), y, r.w(), t.muted());
        for (AdminRows.Row row : snap.rows()) {
            if (row.id().contains(":parts.")) {
                boolean on = Boolean.parseBoolean(AdminEdit.format(row.current()));
                Component name = Component.literal(AdminEdit.humanize(row.id()));
                Component state = Component.translatable(PageKit.HUB + "switch." + (on ? "on" : "off"));
                f.g.drawString(f.font(), name, r.x() + 4, y + 2, t.ink(), false);
                f.g.drawString(f.font(), state, r.right() - f.font().width(state), y + 2, on ? t.good() : t.muted(), false);
                f.target("stage:admin:part:" + row.id(), new Rect(r.x(), y, r.w(), PageKit.LINE + 3), List.of(name, state, Component.translatable(KEY + "restart")), null);
                y += PageKit.LINE + 3;
            }
        }
        return y;
    }

    private <E> E cycle(List<E> all, int at) {
        return all.get(Math.floorMod(at, all.size()));
    }

    private int debug(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "debug_note"), r.x(), y, r.w(), t.muted());
        y = PageKit.wrapped(f, Component.translatable(KEY + "pick").withStyle(ChatFormatting.BOLD), r.x(), y + 3, r.w(), t.accent());
        List<PlayerInfo> online = new ArrayList<>(Minecraft.getInstance().getConnection() == null ? List.of() : Minecraft.getInstance().getConnection().getOnlinePlayers());
        online.sort((a, b) -> a.getProfile().getName().compareToIgnoreCase(b.getProfile().getName()));
        List<Integer> widths = online.stream().map(p -> PageWidgets.widthOf(f.font(), Component.literal(p.getProfile().getName()))).toList();
        Flow.Result flow = Flow.wrap(new Rect(r.x(), y, r.w(), 0), widths, PageWidgets.H, 3);
        for (int i = 0; i < online.size(); i++) {
            PlayerInfo p = online.get(i);
            PageWidgets.button(f, "stage:admin:pick:" + p.getProfile().getName(), flow.rects().get(i), Component.literal(p.getProfile().getName()), true,
                    p.getProfile().getId().equals(player), null, () -> {
                        player = p.getProfile().getId();
                        playerName = p.getProfile().getName();
                    });
        }
        y = flow.bottom() + 4;
        boolean picked = player != null;
        Component need = Component.translatable(KEY + "pick_first");
        List<Species> species = List.of(Species.values());
        List<BloodlineEvent> events = List.of(BloodlineEvent.values());
        List<SupernaturalRace> races = List.of(SupernaturalRace.values());
        record Act(String id, String rowId, List<String> extra) {
        }
        List<Act> acts = new ArrayList<>();
        acts.add(new Act("get", "D1", List.of()));
        acts.add(new Act("show", "D4", List.of()));
        acts.add(new Act("backfill", "D5", List.of()));
        acts.add(new Act("reset", "D6", List.of()));
        acts.add(new Act("tree", "B2", List.of()));
        acts.add(new Act("set", "D2", List.of(cycle(species, speciesAt).name())));
        acts.add(new Act("event", "D3", List.of(cycle(events, eventAt).name())));
        acts.add(new Act("sp", "D7", List.of(String.valueOf(number))));
        acts.add(new Act("rung", "D8", List.of(cycle(races, raceAt).id(), String.valueOf(Math.max(0, Math.min(20, number))))));
        acts.add(new Act("gene_on", "B7", List.of("true")));
        acts.add(new Act("gene_off", "B7", List.of("false")));
        if (net.neoforged.fml.ModList.get().isLoaded("werewolves")) {
            acts.add(new Act("npc_release", "D13", List.of()));
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "params").withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), t.accent());
        List<Component> pl = List.of(Component.literal(cycle(species, speciesAt).name()), Component.literal(cycle(events, eventAt).name()),
                Component.literal(cycle(races, raceAt).id()), Component.literal(String.valueOf(number)));
        List<Runnable> pr = List.of(() -> speciesAt++, () -> eventAt++, () -> raceAt++, () -> number = number >= 1000 ? -1000 : number + 100);
        Flow.Result pf = Flow.wrap(new Rect(r.x(), y, r.w(), 0), pl.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList(), PageWidgets.H, 3);
        for (int i = 0; i < pl.size(); i++) {
            PageWidgets.button(f, "stage:admin:param:" + i, pf.rects().get(i), pl.get(i), true, false, Component.translatable(KEY + "param_tip"), pr.get(i));
        }
        y = pf.bottom() + 4;
        List<Component> labels = acts.stream().map(a -> (Component) Component.translatable(KEY + "act." + a.id())).toList();
        Flow.Result af = Flow.wrap(new Rect(r.x(), y, r.w(), 0), labels.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList(), PageWidgets.H, 3);
        for (int i = 0; i < acts.size(); i++) {
            Act a = acts.get(i);
            List<String> args = new ArrayList<>();
            args.add(picked ? player.toString() : "");
            args.addAll(a.extra());
            boolean danger = a.id().equals("reset");
            Runnable run = () -> {
                if (danger) {
                    f.confirm(Component.translatable(KEY + "act.reset"), Component.translatable(KEY + "confirm_reset", playerName), Component.translatable(KEY + "act.reset"),
                            () -> HubClient.runCommand(a.rowId(), args));
                } else {
                    HubClient.runCommand(a.rowId(), args);
                }
            };
            PageWidgets.button(f, "stage:admin:act:" + a.id(), af.rects().get(i), labels.get(i), picked, false, need, run);
        }
        y = af.bottom() + 4;
        // the pack command exists only with Werewolves (the species part registers it then), so the button does too
        boolean npc = net.neoforged.fml.ModList.get().isLoaded("werewolves");
        List<Component> eclipse = npc ? List.of(Component.translatable(KEY + "act.eclipse_start"), Component.translatable(KEY + "act.eclipse_stop"), Component.translatable(KEY + "act.npc_pack"), Component.translatable(KEY + "act.npc_list"))
                : List.of(Component.translatable(KEY + "act.eclipse_start"), Component.translatable(KEY + "act.eclipse_stop"));
        Flow.Result ef = Flow.wrap(new Rect(r.x(), y, r.w(), 0), eclipse.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList(), PageWidgets.H, 3);
        PageWidgets.button(f, "stage:admin:act:eclipse_start", ef.rects().get(0), eclipse.get(0), true, false, null, () -> HubClient.runCommand("D9", List.of()));
        PageWidgets.button(f, "stage:admin:act:eclipse_stop", ef.rects().get(1), eclipse.get(1), true, false, null, () -> HubClient.runCommand("D10", List.of()));
        if (npc) {
            PageWidgets.button(f, "stage:admin:act:npc_pack", ef.rects().get(2), eclipse.get(2), true, false, null, () -> HubClient.runCommand("D11", List.of()));
            PageWidgets.button(f, "stage:admin:act:npc_list", ef.rects().get(3), eclipse.get(3), true, false, null, () -> HubClient.runCommand("D12", List.of()));
        }
        y = ef.bottom();
        return net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("bosses") ? bosses(f, r, y + 4) : y;
    }

    /** The bosses part's hands on the director (rows X1 to X6): pick a boss, then spawn it here, read its state, bring it down, bring it back, or forget it. */
    private int bosses(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        List<net.ofatech.supernaturalbloodlines.core.boss.BossId> landed = java.util.Arrays.stream(net.ofatech.supernaturalbloodlines.core.boss.BossId.values())
                .filter(net.ofatech.supernaturalbloodlines.core.boss.BossId::landed).toList();
        String pick = cycle(landed, bossAt).id();
        y = PageKit.wrapped(f, Component.translatable(KEY + "bosses").withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), t.accent());
        // "Change" is the Beast's own: the waiting Host becomes the Beast now
        boolean beast = pick.equals(net.ofatech.supernaturalbloodlines.core.boss.BossId.BEAST_OF_GEVAUDAN.id());
        List<Component> labels = new java.util.ArrayList<>(List.of(Component.translatable("supernaturalbloodlines.boss.name." + pick), Component.translatable(KEY + "act.boss_spawn"),
                Component.translatable(KEY + "act.boss_state"), Component.translatable(KEY + "act.boss_desiccate"), Component.translatable(KEY + "act.boss_return"),
                Component.translatable(KEY + "act.boss_reset")));
        if (beast) {
            labels.add(Component.translatable(KEY + "act.boss_change"));
        }
        Flow.Result flow = Flow.wrap(new Rect(r.x(), y, r.w(), 0), labels.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList(), PageWidgets.H, 3);
        PageWidgets.button(f, "stage:admin:boss:pick", flow.rects().get(0), labels.get(0), true, false, Component.translatable(KEY + "param_tip"), () -> bossAt++);
        PageWidgets.button(f, "stage:admin:act:boss_spawn", flow.rects().get(1), labels.get(1), true, false, null, () -> HubClient.runCommand("X1", List.of(pick)));
        PageWidgets.button(f, "stage:admin:act:boss_state", flow.rects().get(2), labels.get(2), true, false, null, () -> HubClient.runCommand("X2", List.of(pick)));
        PageWidgets.button(f, "stage:admin:act:boss_desiccate", flow.rects().get(3), labels.get(3), true, false, null, () -> HubClient.runCommand("X3", List.of(pick)));
        PageWidgets.button(f, "stage:admin:act:boss_return", flow.rects().get(4), labels.get(4), true, false, null, () -> HubClient.runCommand("X4", List.of(pick)));
        PageWidgets.button(f, "stage:admin:act:boss_reset", flow.rects().get(5), labels.get(5), true, false, null,
                () -> f.confirm(labels.get(5), Component.translatable(KEY + "confirm_boss_reset", labels.get(0)), labels.get(5), () -> HubClient.runCommand("X5", List.of(pick))));
        if (beast) {
            PageWidgets.button(f, "stage:admin:act:boss_change", flow.rects().get(6), labels.get(6), true, false, Component.translatable(KEY + "act.boss_change.tip"),
                    () -> HubClient.runCommand("X6", List.of(pick)));
        }
        return flow.bottom();
    }

    private int side(HubScreen.Frame f, Rect s, int y) {
        HubTheme t = f.theme();
        y = PageKit.title(f, Component.translatable(KEY + "side"), s.x(), y, s.w(), t.accent());
        t.drawRule(f.g, s.x(), y + 2, s.w());
        y += 6;
        if (editing != null && editor.multiline()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "list_help"), s.x(), y, s.w(), t.muted());
            String[] lines = editor.text().split("\n", -1);
            int shownLines = Math.min(lines.length, 14);
            Rect box = new Rect(s.x(), y + 2, s.w(), shownLines * PageKit.LINE + 6);
            f.g.fill(box.x(), box.y(), box.right(), box.bottom(), t.accent());
            f.g.fill(box.x() + 1, box.y() + 1, box.right() - 1, box.bottom() - 1, 0xFF000000 | t.paper());
            int caretLine = editor.text().substring(0, editor.caret()).split("\n", -1).length - 1;
            int first = Math.max(0, Math.min(caretLine - shownLines + 1, lines.length - shownLines));
            for (int i = 0; i < shownLines; i++) {
                String line = f.font().plainSubstrByWidth(lines[first + i], s.w() - 8, false);
                f.g.drawString(f.font(), line, box.x() + 4, box.y() + 3 + i * PageKit.LINE, t.ink(), false);
            }
            y = box.bottom() + 2;
        }
        if (player != null) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "picked", playerName), s.x(), y + 2, s.w(), t.ink());
            Component op = Component.translatable(KEY + "open_profile");
            Rect b = new Rect(s.x(), y + 1, Math.min(s.w(), PageWidgets.widthOf(f.font(), op)), PageWidgets.H);
            UUID id = player;
            PageWidgets.button(f, "stage:admin:profile", b, op, true, false, null, () -> f.screen().openProfile(id, playerName, true));
            y = b.bottom() + 2;
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "reply").withStyle(ChatFormatting.BOLD), s.x(), y + 4, s.w(), t.accent());
        int after = PageWidgets.reply(f, s, y);
        return after == y ? PageKit.wrapped(f, Component.translatable(KEY + "no_reply"), s.x(), y, s.w(), t.muted()) : after;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searching) {
            search.type(c);
            return true;
        }
        if (editing != null) {
            editor.type(c);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        TextEdit e = searching ? search : editing != null ? editor : null;
        if (e == null) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                // Esc reverts the field first, then the page closes as usual
                searching = false;
                editing = null;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (e.multiline() && (modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
                    e.newline();
                } else if (searching) {
                    searching = false;
                } else {
                    AdminSnapshotPayload snap = AdminClient.snapshot();
                    String id = editing;
                    if (snap != null) {
                        snap.rows().stream().filter(row -> row.id().equals(id)).findFirst().ifPresent(this::commit);
                    }
                }
            }
            case GLFW.GLFW_KEY_BACKSPACE -> e.backspace();
            case GLFW.GLFW_KEY_DELETE -> e.delete();
            case GLFW.GLFW_KEY_LEFT -> e.left();
            case GLFW.GLFW_KEY_RIGHT -> e.right();
            case GLFW.GLFW_KEY_HOME -> e.home();
            case GLFW.GLFW_KEY_END -> e.end();
            default -> {
            }
        }
        return true; // a field being typed into takes every key: digits are text, not page jumps
    }
}

