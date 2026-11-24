package net.ofatech.supernaturalbloodlines.parts.species.group.coven;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan;
import net.ofatech.supernaturalbloodlines.core.group.circle.Tiers;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenView;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleCost;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.EpPriced;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.jetbrains.annotations.Nullable;

/**
 * The viewer's own place in their coven (R250 to R268): the circle, their meter, consent and share, their shunning and the open vote as counts. Everything
 * is read for the viewer alone; another member's meter, consent or ballot never goes into the view, and only the names of the viewer's own coven do.
 */
public final class CovenViews {
    private static final long HOUR = 60L * 60 * 1000;

    private CovenViews() {
    }

    public static CovenView of(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        GroupIndex groups = GroupIndex.get(level);
        long now = PackIndex.now();
        long banned = groups.banUntil(CovenGovernance.SHUN_SCOPE, p.getUUID());
        int shunned = banned <= now ? 0 : (int) Math.max(1, (banned - now + PackAbsence.DAY_MILLIS - 1) / PackAbsence.DAY_MILLIS);
        UUID leader = CircleLink.covenOf(p);
        if (leader == null) {
            return CovenView.NONE.shunned(shunned);
        }
        PackIndex packs = PackIndex.get(level);
        UUID me = p.getUUID();
        int size = 1 + packs.betasOf(leader).size();
        int tier = Tiers.of(size, GroupsConfig.tier1Members(), GroupsConfig.tier2Members(), GroupsConfig.tier3Members());

        CircleLink.Session session = CircleLink.of(me);
        CovenView.Circle circle = CovenView.Circle.NONE;
        String anchorName = "";
        List<String> names = List.of();
        if (session != null) {
            circle = session.anchor().equals(me) ? CovenView.Circle.ANCHOR : CovenView.Circle.JOINED;
            anchorName = name(p, session.anchor());
            names = session.members().stream().map(id -> name(p, id)).toList();
        } else {
            var call = CircleLink.calls().latestFor(me, level.getGameTime());
            if (call.isPresent()) {
                circle = CovenView.Circle.CALLED;
                var request = CircleLink.calls().peek(call.get(), me, level.getGameTime());
                anchorName = request.map(r -> name(p, r.caller())).orElse("");
            }
        }

        int cap = CircleCost.capacityOf(p);
        int meter = CircleCost.meterOf(groups, me, cap, now);
        int regen = cap <= 0 ? 0 : (int) Math.ceil((double) (cap - meter) / cap * GroupsConfig.channelRegenMinutes() * 60);

        int sharePower = 0;
        long shareEp = 0;
        boolean known = false;
        if (session != null) {
            int n = Math.max(1, session.members().size());
            int index = Math.max(0, session.members().indexOf(me));
            RiteType type = RiteType.getFirstMatching(level, session.key().chalk());
            known = type != null;
            int power = known ? type.getPower() : worstPower(level);
            int pool = (int) Math.floor(power * GroupsConfig.linkMemberShare() + 1e-9);
            sharePower = (int) CirclePlan.split(pool, n)[index];
            if (known) {
                double ep = EpPriced.priceOf(type, level, session.key().chalk()).orElse(0.0);
                shareEp = CirclePlan.split((long) Math.floor(ep), n)[index];
            }
        }
        return new CovenView(tier, size, circle, anchorName, names, meter, cap, regen, groups.consentOf(me), sharePower, shareEp, known, shunned, vote(p, groups, leader, now));
    }

    private static String name(ServerPlayer viewer, UUID id) {
        String n = PackAccess.nameOf(viewer.server, id);
        return n != null ? n : id.toString().substring(0, 8);
    }

    private static Object worstFor;
    private static int worstCached;

    /** The dearest rite's power: the registry is walked once per registry, not on every push. */
    private static synchronized int worstPower(ServerLevel level) {
        var registry = level.registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY);
        if (worstFor != registry) {
            int worst = 0;
            for (RiteType t : registry) {
                worst = Math.max(worst, t.getPower());
            }
            worstFor = registry;
            worstCached = worst;
        }
        return worstCached;
    }

    private static Optional<CovenView.Vote> vote(ServerPlayer p, GroupIndex groups, UUID leader, long now) {
        UUID group = groups.existingGroupOf(leader).orElse(null);
        if (group == null) {
            return Optional.empty();
        }
        PackIndex packs = PackIndex.get(p.serverLevel());
        List<UUID> electors = new ArrayList<>();
        electors.add(leader);
        for (UUID id : packs.betasOf(leader)) {
            if (groups.rankOf(id, GroupType.COVEN) == GroupRank.ELDER) {
                electors.add(id);
            }
        }
        List<GroupIndex.Vote> open = groups.openVotes(group);
        // the first vote the viewer has not answered comes first, so a card never shows a ballot already cast while another waits
        GroupIndex.Vote shown = open.stream().filter(v -> !v.ballots.containsKey(p.getUUID())).findFirst().orElse(open.isEmpty() ? null : open.get(0));
        for (GroupIndex.Vote v : shown == null ? List.<GroupIndex.Vote>of() : List.of(shown)) {
            int yes = (int) electors.stream().filter(id -> Boolean.TRUE.equals(v.ballots.get(id))).count();
            int no = (int) electors.stream().filter(id -> Boolean.FALSE.equals(v.ballots.get(id))).count();
            Boolean mine = v.ballots.get(p.getUUID());
            CovenView.Ballot ballot = mine == null ? CovenView.Ballot.NONE : mine ? CovenView.Ballot.YES : CovenView.Ballot.NO;
            long left = Math.max(0, v.opened + GroupsConfig.voteHours() * HOUR - now);
            return Optional.of(new CovenView.Vote(v.kind, name(p, v.subject), yes, no, electors.size(), ballot, (int) Math.ceil(left / (double) HOUR)));
        }
        return Optional.empty();
    }
}