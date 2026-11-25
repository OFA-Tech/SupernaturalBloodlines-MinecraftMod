package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterRules;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanTime;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanWire;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules;
import net.ofatech.supernaturalbloodlines.core.group.clan.SirelineBonus;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.ContestRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** The viewer's own clan, as the hub draws it (R281): their own rank clock, the Charter, the contest and two counts. Only members' names, never an id, a species or a root. */
public final class ClanViews {

    private ClanViews() {
    }

    private static int clampDays(long days) {
        return (int) Math.max(0, Math.min(ClanWire.MAX_DAYS, days));
    }

    private static int clampSeconds(long millis) {
        return (int) Math.max(0, Math.min(ClanWire.MAX_SECONDS, millis / 1000));
    }

    private static java.util.function.Function<UUID, String> names;

    /** For tests: the name a player has in the view; null restores the server's own. */
    public static void overrideNames(java.util.function.Function<UUID, String> function) {
        names = function;
    }

    private static String nameIfMember(ServerPlayer viewer, UUID group, UUID id) {
        if (id == null || !Charters.isMember(viewer.server, id, group)) {
            return "";
        }
        String name = names != null ? names.apply(id) : PackAccess.nameOf(viewer.server, id);
        return name == null ? "" : name;
    }

    public static ClanView of(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ClanScope.Clan clan = ClanScope.of(level, p.getUUID());
        if (clan == null) {
            return ClanView.NONE;
        }
        PackIndex packs = PackIndex.get(level);
        GroupIndex groups = GroupIndex.get(level);
        long now = PackIndex.now();
        boolean head = p.getUUID().equals(clan.leader());
        boolean on = GroupsConfig.clanPerksOn();
        long since = head ? groups.typeSince(clan.group()) : packs.inPackSince(p.getUUID()).orElse(now);
        int days = since <= 0 ? 0 : clampDays((now - since) / ClanTime.DAY);
        GroupRank rank = groups.rankOf(p.getUUID(), GroupType.CLAN);
        int auto = -1;
        boolean dayWalker = ClanScope.dayWalker(level, p.getUUID());
        if (on && !head && rank == GroupRank.NONE && ClanScope.mayBeDayWalker(level, p.getUUID()) && GroupsConfig.daywalkerDays() > 0 && groups.cooldownUntil(clan.group(), ClanDiscipline.noPromoteKey(p.getUUID())) == 0) {
            auto = clampDays(Math.max(0, GroupsConfig.daywalkerDays() - (now - since) / ClanTime.DAY));
        }
        int members = packs.roster(clan.leader()).size();

        ClanView.Charter charter = ClanView.Charter.NONE;
        Optional<CharterRow> row = groups.charterOf(clan.group());
        boolean charterOn = GroupsConfig.charterActive();
        if (row.isPresent()) {
            CharterRow r = row.get();
            byte state = !charterOn ? ClanView.CHARTER_INERT : switch (r.state()) {
                case HELD -> ClanView.CHARTER_HELD;
                case STORED -> ClanView.CHARTER_STORED;
                case DROPPED -> ClanView.CHARTER_DROPPED;
                case LOST -> ClanView.CHARTER_LOST;
            };
            boolean mine = r.holder().filter(p.getUUID()::equals).isPresent();
            int wait = r.state() == CharterState.LOST ? clampSeconds(r.lostAt() + CharterRules.REFORGE_WAIT - now) : 0;
            charter = new ClanView.Charter(state, nameIfMember(p, clan.group(), r.holder().orElse(null)), mine, false, wait);
        } else if (head && charterOn) {
            charter = new ClanView.Charter(ClanView.CHARTER_NONE, "", false, members >= GroupsConfig.charterMinMembers(), 0);
        }

        ClanView.Contest contest = ClanView.Contest.NONE;
        if (charterOn) {
            Optional<ContestRow> c = groups.contestOf(clan.group());
            long cooldown = groups.cooldownUntil(clan.group(), Contests.COOLDOWN_KEY);
            int cooldownDays = cooldown > now ? clampDays((cooldown - now + ClanTime.DAY - 1) / ClanTime.DAY) : 0;
            if (c.isPresent()) {
                ContestRow cr = c.get();
                long left = cr.phase() == ContestRow.Phase.PENDING ? cr.acceptBy() - now : 0;
                contest = new ClanView.Contest(cr.phase() == ContestRow.Phase.PENDING ? ClanView.CONTEST_PENDING : ClanView.CONTEST_RUNNING,
                        nameIfMember(p, clan.group(), cr.challenger()), clampSeconds(left), cooldownDays, false, 0, head && cr.phase() == ContestRow.Phase.PENDING);
            } else if (!head && dayWalker) {
                Optional<ContestRules.Refusal> no = Contests.refusalFor(level, p.getUUID());
                // what keeps the viewer out lasts as long as it says: the cooldown, or the days still to be in the clan before they may challenge
                int wait = no.filter(r -> r == ContestRules.Refusal.TOO_NEW).isPresent() ? clampDays(Math.max(1, GroupsConfig.challengerMinDays() - days)) : cooldownDays;
                contest = new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, wait, no.isEmpty(), ContestRules.Refusal.wire(no), false);
            } else {
                contest = new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, cooldownDays, false, 0, false);
            }
        }

        int bonus = on ? (int) Math.round(100 * SirelineBonus.of(SirelineBonuses.matesNear(p, new HashMap<>()), GroupsConfig.sirelineBonusPercent(), GroupsConfig.sirelineBonusMaxPercent())) : 0;
        boolean feed = GroupsConfig.feedShareActive();
        return new ClanView(true, days, auto, charter, contest, feed, feed ? GroupsConfig.feedSharePercent() : 0, Math.min(bonus, ClanWire.MAX_PERCENT));
    }
}
