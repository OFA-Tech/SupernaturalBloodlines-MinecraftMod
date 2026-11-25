package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Development only: demo data for the dev screenshots ({@code -Dsupernaturalbloodlines.devshots.demo=true}), so every populated state of the hub can be
 * photographed in a world that has no pack, no family, no other mods' quests and no learned skills. It rebuilds the client's view of the hub; it
 * changes nothing on the server and is reached only from {@link DevShots}.
 */
final class DevDemo {
    private static boolean installed;
    /** Development only: the demo pack is led by Derek and the viewer is its second Alpha (the challenge shots). */
    static boolean second;

    private DevDemo() {
    }

    /** A made-up person's id: the same name is the same id in every frame. */
    static java.util.UUID id(String name) {
        return java.util.UUID.nameUUIDFromBytes(("devdemo:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static java.util.Map<net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact, List<String>> facts(String... kv) {
        java.util.Map<net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact, List<String>> out = new java.util.EnumMap<>(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.class);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out.put(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.valueOf(kv[i]), kv[i + 1].isEmpty() ? List.of() : List.of(kv[i + 1].split("\\|")));
        }
        return out;
    }

    /** The profiles, requests and admin data of the demo, set once (a client view only). */
    private static void install(HubView v) {
        if (installed) {
            return;
        }
        installed = true;
        String me = v.player().getName().getString();
        var map = new java.util.HashMap<java.util.UUID, net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload>();
        map.put(id("Derek"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Derek"), false, facts("NAME", "Derek", "SPECIES", "werewolf",
                "RANK", "coalpha", "LEVEL", "9", "PACK", me, "PACK_DAYS", "12", "SIRE", "", "LINE", "", "GENES", "werewolf", "ANCHOR", me, "PRESENCE", "online", "BONUS", "+20% attack, +4 max health")));
        map.put(id("Liam"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Liam"), false, facts("NAME", "Liam", "SPECIES", "werewolf",
                "RANK", "beta", "LEVEL", "4", "PACK", me, "PACK_DAYS", "3", "SIRE", "", "LINE", "", "ANCHOR", "Derek", "PRESENCE", "online", "BONUS", "")));
        map.put(id("Stranger"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Stranger"), false, java.util.Map.of()));
        map.put(id("Klaus"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Klaus"), false, facts("NAME", "Klaus", "SPECIES", "vampire", "LEVEL", "14", "PRESENCE", "online")));
        String groupKind = System.getProperty("supernaturalbloodlines.devshots.demo.group", "");
        if (!groupKind.isBlank() && !groupKind.startsWith("empty")) {
            // a mate of the demo group: the Profile names the group and its type
            map.put(id("Group|mate"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Group|mate"), false, facts("NAME", "Rowan", "SPECIES", "witch",
                    "LEVEL", "6", "PACK", me + "|" + groupKind, "PACK_DAYS", "9", "PRESENCE", "online")));
        }
        map.put(id("Tyler"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Tyler"), false, facts("NAME", "Tyler", "SPECIES", "hybrid", "LEVEL", "7", "SIRE", me)));
        map.put(v.player().getUUID(), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(v.player().getUUID(), false, facts("NAME", me, "SPECIES", "original_hybrid",
                "RANK", "alpha", "LEVEL", "12", "PACK", me, "PACK_DAYS", "40", "SIRE", "Klaus", "LINE", "", "GENES", "werewolf", "ANCHOR", "", "PRESENCE", "online", "BONUS", "+25% attack")));
        // the OP view: every field, whatever the OP knows or not
        map.put(id("Derek|op"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Derek|op"), true, facts("NAME", "Derek", "SPECIES", "werewolf", "RANK", "coalpha",
                "LEVEL", "9", "PACK", me, "PACK_DAYS", "12", "SIRE", "", "LINE", "Mikael", "GENES", "werewolf|doppelganger", "ANCHOR", me, "PRESENCE", "online", "BONUS", "+20% attack")));
        // the pack seen as a second Alpha: Derek leads (his own id, so the Profile finds him in the ledger)
        map.put(id("Derek|lead"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Derek|lead"), false, facts("NAME", "Derek", "SPECIES", "werewolf",
                "RANK", "alpha", "LEVEL", "9", "PACK", "Derek", "PACK_DAYS", "21", "SIRE", "", "LINE", "", "GENES", "werewolf", "PRESENCE", "online", "BONUS", "+24% attack, +4.8 max health")));
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.ProfileClient.demo(map);
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient.demo(List.of(
                new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.SIRE_CALL, id("Klaus"), "Klaus", "ab12cd34", 5000),
                new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.PACK_INVITE, id("Derek"), "Derek", "0f0f0f0f", 4000),
                new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.COMPULSION, net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.NOBODY, "", "11223344", 3000)));
        if ("true".equals(System.getProperty("supernaturalbloodlines.devshots.demo.ward"))) { // demo.ward=true: the Path page's Your ward card
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.WardClient.demo(new net.ofatech.supernaturalbloodlines.shared.hub.WardHub.View(List.of(
                    new net.ofatech.supernaturalbloodlines.shared.hub.WardHub.Own("0a1b2c3d", false, false, "minecraft:overworld", -120, 64, 305,
                            List.of(new net.ofatech.supernaturalbloodlines.shared.hub.WardHub.Member(id("Bonnie"), "Bonnie"), new net.ofatech.supernaturalbloodlines.shared.hub.WardHub.Member(id("Elena"), "Elena"))),
                    new net.ofatech.supernaturalbloodlines.shared.hub.WardHub.Own("4e5f6a7b", true, false, "minecraft:overworld", -118, 65, 301, List.of())),
                    List.of("Jeremy")));
        }
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.AdminClient.demo(net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminNet.snapshot(Optional.empty()),
                new net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandReplyPayload("D1", true, List.of(net.minecraft.network.chat.Component.literal(me + ": BloodlineState[species=HYBRID, sire=Klaus]"))));
    }

    /**
     * {@code .lunar=true}: stand-in lunar events for the almanac shots (a Blood Moon in 2 days, a Super Moon in 4, a Blue Moon in 30), because Enhanced
     * Celestials is not in the dev runtime. Goes through the same {@code LunarMoods} the real source uses.
     */
    static void lunar() {
        String dle = net.ofatech.supernaturalbloodlines.core.moon.LunarMood.EC_DEFAULT_NAMESPACE;
        List<net.ofatech.supernaturalbloodlines.core.moon.LunarSource.Upcoming> events = List.of(
                new net.ofatech.supernaturalbloodlines.core.moon.LunarSource.Upcoming(2, net.ofatech.supernaturalbloodlines.core.moon.LunarMood.ofEvent(dle, "blood_moon"), 0xFF5555, "lunar.event." + dle + ".blood_moon"),
                new net.ofatech.supernaturalbloodlines.core.moon.LunarSource.Upcoming(4, net.ofatech.supernaturalbloodlines.core.moon.LunarMood.ofEvent(dle, "super_moon"), 0x9A7BFF, "lunar.event." + dle + ".super_moon"),
                new net.ofatech.supernaturalbloodlines.core.moon.LunarSource.Upcoming(30, net.ofatech.supernaturalbloodlines.core.moon.LunarMood.ofEvent(dle, "blue_moon"), 0x55AAFF, "lunar.event." + dle + ".blue_moon"));
        net.ofatech.supernaturalbloodlines.core.moon.LunarMoods.use(new net.ofatech.supernaturalbloodlines.core.moon.LunarSource() {
            @Override
            public net.ofatech.supernaturalbloodlines.core.moon.LunarMood now(net.minecraft.world.level.Level level) {
                return net.ofatech.supernaturalbloodlines.core.moon.LunarMood.NONE;
            }

            @Override
            public List<Upcoming> ahead(net.minecraft.world.level.Level level, int days) {
                return events.stream().filter(e -> e.days() < days).toList();
            }
        }, null);
    }

    /**
     * {@code .demo.group=clan|coven|brotherhood|empty_clan|empty_coven|empty_brotherhood|empty}: the viewer leads a group of that type
     * (three members, one away, one ally), or is a fit leader of one with nobody yet, or leads nothing. Without the property the demo pack is the pack.
     */
    private static void group(HubView v) {
        String kind = System.getProperty("supernaturalbloodlines.devshots.demo.group");
        if (kind == null || kind.isBlank()) {
            return;
        }
        String me = v.player().getName().getString();
        if (kind.equals("empty")) {
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.GroupClient.demo(net.ofatech.supernaturalbloodlines.core.group.GroupView.none(net.ofatech.supernaturalbloodlines.core.group.GroupType.COVEN));
            return;
        }
        boolean empty = kind.startsWith("empty_");
        var type = net.ofatech.supernaturalbloodlines.core.group.GroupType.parse(empty ? kind.substring(6) : kind).orElseThrow();
        if (empty) {
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.GroupClient.demo(new net.ofatech.supernaturalbloodlines.core.group.GroupView(type, false,
                    net.ofatech.supernaturalbloodlines.core.group.GroupView.LEADER, "", 0, List.of()));
            return;
        }
        var none = net.ofatech.supernaturalbloodlines.core.group.GroupRank.NONE;
        String second = type == net.ofatech.supernaturalbloodlines.core.group.GroupType.COVEN ? "elder" : type == net.ofatech.supernaturalbloodlines.core.group.GroupType.CLAN ? "day_walker" : "member";
        var secondRank = type == net.ofatech.supernaturalbloodlines.core.group.GroupType.COVEN ? net.ofatech.supernaturalbloodlines.core.group.GroupRank.ELDER
                : type == net.ofatech.supernaturalbloodlines.core.group.GroupType.CLAN ? net.ofatech.supernaturalbloodlines.core.group.GroupRank.DAY_WALKER : none;
        if (type == net.ofatech.supernaturalbloodlines.core.group.GroupType.COVEN) {
            coven(me);
        }
        byte viewer = net.ofatech.supernaturalbloodlines.core.group.GroupView.LEADER;
        if (type == net.ofatech.supernaturalbloodlines.core.group.GroupType.CLAN) {
            viewer = clan();
        }
        if (viewer != net.ofatech.supernaturalbloodlines.core.group.GroupView.LEADER) {
            // the viewer is a Day Walker or a Night Walker of Rowan's clan: Rowan is the head, the viewer's own row carries their own rank
            boolean day = viewer == net.ofatech.supernaturalbloodlines.core.group.GroupView.DAY_WALKER;
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.GroupClient.demo(new net.ofatech.supernaturalbloodlines.core.group.GroupView(type, true, viewer, "Rowan", 4, List.of(
                    new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Rowan", "leader", none, true, Optional.of(id("Group|mate"))),
                    new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row(me, day ? "day_walker" : "member", day ? secondRank : none, true, Optional.of(v.player().getUUID())),
                    new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Mira", "member", none, true, Optional.of(id("Mira"))),
                    new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Cass", "member", none, false, Optional.of(id("Cass"))),
                    new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Scott", "ally", none, true, Optional.of(id("Scott"))))));
            return;
        }
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.GroupClient.demo(new net.ofatech.supernaturalbloodlines.core.group.GroupView(type, true,
                viewer, me, 4, List.of(
                        new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row(me, "leader", none, true, Optional.of(v.player().getUUID())),
                        new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Rowan", second, secondRank, true, Optional.of(id("Group|mate"))),
                        new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Mira", "member", none, true, Optional.of(id("Mira"))),
                        new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Cass", "member", none, false, Optional.of(id("Cass"))),
                        new net.ofatech.supernaturalbloodlines.core.group.GroupView.Row("Scott", "ally", none, true, Optional.of(id("Scott"))))));
    }

    /**
     * {@code .demo.clan=<rank>:<state>}: the clan card for the demo clan. Rank {@code head}, {@code day} or {@code night}; state {@code none} (no Charter, can forge), {@code few}
     * (no Charter, cannot), {@code held}, {@code stored}, {@code dropped}, {@code lost}, {@code lostwait}, {@code pending} (a challenge waits), {@code running}, {@code may}
     * (a Day Walker may challenge), {@code cooldown}, {@code young} (too new), {@code loading} (no view has come yet). Returns the rank for the group view. Development only.
     */
    private static byte clan() {
        String[] spec = System.getProperty("supernaturalbloodlines.devshots.demo.clan", "head:none").split(":");
        String rank = spec[0];
        String state = spec.length > 1 ? spec[1] : "none";
        byte viewer = rank.equals("day") ? net.ofatech.supernaturalbloodlines.core.group.GroupView.DAY_WALKER : rank.equals("night") ? net.ofatech.supernaturalbloodlines.core.group.GroupView.MEMBER
                : net.ofatech.supernaturalbloodlines.core.group.GroupView.LEADER;
        if (state.equals("loading")) {
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.ClanClient.clear(); // the server has not answered yet: the cards wait
            return viewer;
        }
        byte charter = switch (state) {
            case "none", "few" -> net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CHARTER_NONE;
            case "stored" -> net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CHARTER_STORED;
            case "dropped" -> net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CHARTER_DROPPED;
            case "lost", "lostwait" -> net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CHARTER_LOST;
            default -> net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CHARTER_HELD;
        };
        var holder = new net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.Charter(charter, charter == net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CHARTER_HELD ? "Rowan" : "", false,
                state.equals("none"), state.equals("lostwait") ? 7200 : 0);
        byte phase = state.equals("pending") ? net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CONTEST_PENDING : state.equals("running")
                ? net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CONTEST_RUNNING : net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CONTEST_NONE;
        int why = state.equals("cooldown") ? 7 : state.equals("young") ? 2 : 0;
        var contest = new net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.Contest(phase, phase == 0 ? "" : "Rowan", phase == net.ofatech.supernaturalbloodlines.core.group.clan.ClanView.CONTEST_PENDING ? 540 : 0,
                state.equals("cooldown") ? 4 : 0, viewer == net.ofatech.supernaturalbloodlines.core.group.GroupView.DAY_WALKER && state.equals("may"), why,
                viewer == net.ofatech.supernaturalbloodlines.core.group.GroupView.LEADER && state.equals("pending"));
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.ClanClient.demo(new net.ofatech.supernaturalbloodlines.core.group.clan.ClanView(true, viewer == net.ofatech.supernaturalbloodlines.core.group.GroupView.MEMBER ? 6 : 30,
                viewer == net.ofatech.supernaturalbloodlines.core.group.GroupView.MEMBER ? 8 : -1, holder, contest, true, 10, 6));
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient.demo(state.equals("pending") && viewer == net.ofatech.supernaturalbloodlines.core.group.GroupView.LEADER
                ? List.of(new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.CHARTER_CONTEST, id("Group|mate"), "Rowan", "", 540 * 20))
                : List.of());
        return viewer;
    }

    /**
     * {@code .demo.coven=none|anchor|joined|called|vote}: the circle card in that state for the demo coven (a tier II coven of five, the meter two thirds full);
     * {@code vote} is {@code anchor} with a shunning vote open (one yes, one no, of three) the viewer has not answered. Development only.
     */
    private static void coven(String me) {
        String state = System.getProperty("supernaturalbloodlines.devshots.demo.coven", "none");
        var circle = switch (state) {
            case "anchor", "vote" -> net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.ANCHOR;
            case "joined" -> net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.JOINED;
            case "called" -> net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.CALLED;
            default -> net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.NONE;
        };
        boolean inside = circle == net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.ANCHOR || circle == net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.JOINED;
        var vote = state.equals("vote") ? Optional.of(new net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Vote(net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind.SHUN,
                 id("Coven vote"), "Cass", 1, 1, 3, net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Ballot.NONE, 17)) : Optional.<net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Vote>empty();
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.CovenClient.demo(new net.ofatech.supernaturalbloodlines.core.group.coven.CovenView(2, 5, circle,
                circle == net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.JOINED || circle == net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Circle.CALLED ? "Rowan" : me,
                inside ? List.of(me, "Rowan", "Mira") : List.of(), 2000, 3000, 240, net.ofatech.supernaturalbloodlines.core.group.circle.Consent.NOSEBLEED,
                 inside ? 840 : 0, 0, vote, state.equals("called") ? "aabbccdd" : ""));
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient.demo(List.of());
        if (state.equals("called") || state.equals("vote")) {
            var kind = state.equals("called") ? net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.CIRCLE_CALL : net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.GROUP_VOTE;
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient.demo(List.of(new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(kind, state.equals("called") ? id("Rowan") : id("Cass"), state.equals("called") ? "Rowan" : "Cass", vote.map(net.ofatech.supernaturalbloodlines.core.group.coven.CovenView.Vote::token).orElse("aabbccdd"), state.equals("called") ? 1100 : 17 * 3600 * 20)));
        }
    }

    static HubView apply(HubView v) {
        String mode = DevShots.demo();
        install(v);
        group(v);
        if ("ghoul".equals(mode)) {
            return ghoul(v);
        }
        HubView base = "empty".equals(mode) ? empty(v) : populated(v);
        if ("two".equals(mode)) {
            List<HubModel.LadderView> both = new ArrayList<>(base.ladders());
            HubModel.LadderView first = both.get(0);
            both.add(new HubModel.LadderView(SupernaturalRace.WITCH, first.rungs(), first.active(), first.tasks()));
            return new HubView(base.player(), base.bloodline(), base.state(), base.races(), both, base.skills(), base.snapshot(), base.context());
        }
        return withOrigin(base);
    }

    /** {@code .origin=original|none|unknown|inline|inline_unknown|free|free_unknown}: the line state of the Bloodline page. */
    private static HubView withOrigin(HubView v) {
        String mode = DevShots.origin();
        if (mode == null) {
            return v;
        }
        HubSnapshotPayload.Family f = v.snapshot().family();
        Optional<String> klaus = Optional.of("npc:klaus");
        Optional<String> none = Optional.empty();
        List<HubSocial.Kin> kids = f.line();
        HubSnapshotPayload.Family fam = switch (mode) {
            case "original" -> new HubSnapshotPayload.Family(none, false, f.sired(), List.of(), kids, new HubSocial.Origin(LineRules.State.ORIGINAL, none, none, HubSocial.FreedKind.NONE, List.of()));
            case "none" -> HubSnapshotPayload.Family.NONE;
            case "unknown" -> new HubSnapshotPayload.Family(none, false, f.sired(), List.of(), kids, new HubSocial.Origin(LineRules.State.SIRE_UNKNOWN, none, none, HubSocial.FreedKind.NONE, List.of()));
            case "inline" -> new HubSnapshotPayload.Family(Optional.of("Tyler"), false, f.sired(), List.of(""), kids, new HubSocial.Origin(LineRules.State.IN_LINE, klaus, none, HubSocial.FreedKind.NONE, List.of(true)));
            case "inline_unknown" -> new HubSnapshotPayload.Family(Optional.of("Tyler"), false, f.sired(), List.of(""), kids, new HubSocial.Origin(LineRules.State.IN_LINE, none, none, HubSocial.FreedKind.NONE, List.of(true)));
            case "free" -> new HubSnapshotPayload.Family(Optional.of("Tyler"), false, f.sired(), List.of(""), kids, new HubSocial.Origin(LineRules.State.FREE, none, klaus, HubSocial.FreedKind.BROKEN_LINE, List.of(true)));
            case "free_unknown" -> new HubSnapshotPayload.Family(Optional.of("Tyler"), false, f.sired(), List.of(""), kids, new HubSocial.Origin(LineRules.State.FREE, none, none, HubSocial.FreedKind.BROKEN_LINE, List.of(true)));
            default -> f;
        };
        HubSnapshotPayload s = v.snapshot();
        return new HubView(v.player(), v.bloodline(), v.state(), v.races(), v.ladders(), v.skills(), new HubSnapshotPayload(fam, s.pack(), s.fiveCount(), s.genes()), v.context());
    }

    /** A Ghoul with 14:20 left to finish turning: Path and Guide open, the rest locked. */
    private static HubView ghoul(HubView v) {
        List<SupernaturalRace> races = List.of(SupernaturalRace.GHOUL);
        HubPage.Context c = v.context();
        HubPage.Context ghoul = new HubPage.Context(SupernaturalRace.GHOUL, Optional.empty(), c.werewolves(), c.enchanted(), c.loadout(), false, false);
        return new HubView(v.player(), v.bloodline().toBuilder().ghoul(true).turningTicksLeft(17_200).build(), v.state(), races,
                HubModel.ladders(LadderTable.clientCopy(), v.state(), races, Map.of()), v.skills(), v.snapshot(), ghoul);
    }

    /** Nothing yet: no pack, no sire or children, no open task (the three empty states). */
    private static HubView empty(HubView v) {
        HubSnapshotPayload.Family none = HubSnapshotPayload.Family.NONE;
        HubSnapshotPayload snap = new HubSnapshotPayload(none, List.of(), 0, Map.of(Genes.WEREWOLF, Genes.Knowledge.TRUE, Genes.DOPPELGANGER, Genes.Knowledge.UNKNOWN));
        List<HubModel.LadderView> done = v.ladders().stream().map(l -> new HubModel.LadderView(l.race(), l.rungs(), Optional.empty(), List.of())).toList();
        return new HubView(v.player(), v.bloodline(), v.state().toBuilder().skills(Set.of()).build(), v.races(), done, v.skills(), snap, v.context());
    }

    private static HubView populated(HubView v) {
        SupernaturalState s = v.state().toBuilder().skills(Set.of("S-HYB-1", "S-HYB-2", "S-VAM-1", "S-VAM-2", "S-WOL-1", "S-WOL-5", "S-WOL-7"))
                .sp(6).spEarned(11).build();
        // the player leads the demo pack (the first of the list), so the kick and ally actions can be photographed
        String me = v.player().getName().getString();
        List<HubSocial.Member> pack = second ? List.of(new HubSocial.Member("Derek", "alpha", true, Optional.of(id("Derek|lead"))), new HubSocial.Member(me, "coalpha", true, Optional.of(v.player().getUUID())),
                new HubSocial.Member("Liam", "beta", true, Optional.of(id("Liam"))), new HubSocial.Member("Malia", "beta", false, Optional.of(id("Malia"))))
                : List.of(new HubSocial.Member(me, "alpha", true, Optional.of(v.player().getUUID())), new HubSocial.Member("Derek", "coalpha", true, Optional.of(id("Derek"))),
                new HubSocial.Member("Liam", "beta", true, Optional.of(id("Liam"))),
                new HubSocial.Member("Malia", "beta", false, Optional.of(id("Malia"))), new HubSocial.Member("Isaac", "beta", true, Optional.of(id("Isaac"))), new HubSocial.Member("Deaton", "emissary", true, Optional.of(id("Deaton"))),
                new HubSocial.Member("Stiles", "packmate", true, Optional.of(id("Stiles"))), new HubSocial.Member("Lydia", "packmate", false, Optional.of(id("Lydia"))), new HubSocial.Member("Scott", "ally", true, Optional.of(id("Scott"))));
        HubSnapshotPayload.Family family = new HubSnapshotPayload.Family(Optional.of("Klaus"), false, List.of("Tyler", "Ana", "Bo", "Cy", "Di"),
                List.of("Mikael"), List.of(new HubSocial.Kin("Tyler", 0), new HubSocial.Kin("Ana", 0), new HubSocial.Kin("Bo", 0), new HubSocial.Kin("Cy", 0),
                        new HubSocial.Kin("Di", 0)), HubSocial.Origin.NONE);
        List<HubSocial.Person> people = new ArrayList<>(List.of(new HubSocial.Person("Klaus", id("Klaus"))));
        family.sired().forEach(n -> people.add(new HubSocial.Person(n, id(n))));
        HubSnapshotPayload snap = new HubSnapshotPayload(family, pack, 2, Map.of(Genes.WEREWOLF, Genes.Knowledge.TRUE, Genes.DOPPELGANGER, Genes.Knowledge.UNKNOWN),
                people, new net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags(true, true, "running".equals(System.getProperty("supernaturalbloodlines.devshots.demo.trial")) ? 2 : 1).withBossNear(new net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.BossNear("fallen".equals(System.getProperty("supernaturalbloodlines.devshots.demo.boss")) ? 2 : 1, "mikael")), npcs(),
                "true".equals(System.getProperty("supernaturalbloodlines.devshots.demo.moongift"))); // demo.moongift=true: the pack's moon gift line
        // one person, everywhere: Dev is the hybrid werewolf who leads the demo pack as an Alpha with a bitten-then-born line
        var wolf = v.bloodline().wolf().toBuilder().lineage(net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage.BORN)
                .rank("beta".equals(System.getProperty("supernaturalbloodlines.devshots.demo.rank")) ? net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank.BETA
                        : net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank.ALPHA).heldSpark(!"beta".equals(System.getProperty("supernaturalbloodlines.devshots.demo.rank"))).sparks(1).weak("true".equals(System.getProperty("supernaturalbloodlines.devshots.demo.weak")) ? new net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak(1, 30000, 6) : net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak.NONE).build(); // demo.weak=true: a weaker spark is growing. An Alpha has held a spark (so no protect switch); demo.rank=beta: a Beta whose path is open
        var dev = v.bloodline().toBuilder().species(net.ofatech.supernaturalbloodlines.core.bloodline.Species.HYBRID).wolf(wolf).build();
        // the Beta of the Alpha Trial shots is a werewolf (the Home notice of the trial is for werewolves)
        List<SupernaturalRace> races = "beta".equals(System.getProperty("supernaturalbloodlines.devshots.demo.rank")) ? List.of(SupernaturalRace.WEREWOLF) : v.races();
        return new HubView(v.player(), dev, s, races, HubModel.ladders(LadderTable.clientCopy(), s, races, HubView.levels(v.player())), v.skills(), snap,
                v.context().withOp(true).withPack(true));
    }

    /** The pack's wolves of the demo: two Brothers of the viewer (one hunting, one following), a Tensura subordinate, and a Brother of another member that the viewer may only look at. */
    private static List<HubSocial.Npc> npcs() {
        return List.of(
                new HubSocial.Npc(1, "Ashen", "werewolves:werewolf_beast", "brother", "guard", true, true, true, 12, 80, 4, "Dev", true, true, 50),
                new HubSocial.Npc(2, "Ember", "werewolves:werewolf_beast", "brother", "follow", false, false, true, 3, 45, 1, "Dev", true, true, 50),
                new HubSocial.Npc(3, "Rimuru's Wolf", "tensura:direwolf", "subordinate", "", false, false, true, 7, 100, 2, "Liam", true, false, 25),
                new HubSocial.Npc(4, "Pale", "werewolves:werewolf_beast", "brother", "rest", false, false, true, 20, 70, 3, "Derek", true, false, 50));
    }
}
