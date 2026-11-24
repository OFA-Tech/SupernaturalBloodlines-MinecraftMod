package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.group.circle.ChannelMeter;
import net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenText;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import org.jetbrains.annotations.Nullable;

/** What the rite cost hook asks of the circle: the plan staged when a rite starts, and the meters charged when it has started. */
public final class CircleCost {
    /** A member's base Max EP, for the floor of the plan. The evolution rites part (which has Tensura) installs the real one; without it no rite takes EP, so 0 is never read. */
    public static volatile java.util.function.ToDoubleFunction<ServerPlayer> baseMaxEp = p -> 0.0;

    private CircleCost() {
    }

    /** A member's channel capacity: a witch type has the full cap, everyone else a share of it. */
    public static int capacityOf(ServerPlayer p) {
        return ChannelMeter.capacity(SeveranceRiteFactory.isWitchType(BloodlineAccess.get(p).species()), GroupsConfig.channelCap(), GroupsConfig.channelOtherFactor());
    }

    /** The meter now: what is stored refilled for the time that passed, or full when nothing is stored. */
    public static int meterOf(GroupIndex groups, UUID member, int cap, long nowMillis) {
        GroupIndex.ChannelRow row = groups.channelOf(member);
        return row == null ? cap : ChannelMeter.valueAt(row.value(), row.at(), nowMillis, cap, GroupsConfig.channelRegenMinutes());
    }

    /**
     * Stages the plan for the rite starting at {@code chalk} when {@code caster} is the anchor of a circle there, and returns what the altar is asked
     * for. Nothing is charged here. Members who walked out of the circle, are offline, dead or under half health are left out (and told).
     */
    public static int stage(ServerLevel level, UUID caster, BlockPos chalk, @Nullable RiteType type, int altarBill) {
        CircleStage.set(null);
        if (!GroupsConfig.linkEnabled() || !GroupsConfig.covensEnabled()) {
            return altarBill;
        }
        CircleLink.Session session = CircleLink.current(level, chalk, caster);
        if (session == null) {
            return altarBill;
        }
        long now = PackIndex.now();
        GroupIndex groups = GroupIndex.get(level);
        List<CirclePlan.Member> members = new ArrayList<>();
        for (UUID id : session.members()) {
            ServerPlayer p = level.getPlayerByUUID(id) instanceof ServerPlayer sp ? sp : null;
            if (p == null) {
                continue;
            }
            String left = !p.isAlive() ? "hurt" : !CircleSites.inside(chalk, p) ? "away" : p.getHealth() < p.getMaxHealth() * 0.5f ? "hurt" : null;
            if (left != null) {
                p.sendSystemMessage(CovenText.of("link_dropped." + left));
                continue;
            }
            int cap = capacityOf(p);
            members.add(new CirclePlan.Member(id, meterOf(groups, id, cap, now), cap, groups.consentOf(id), baseMaxEp.applyAsDouble(p), id.equals(session.anchor())));
        }
        double epTotal = EpPriced.priceOf(type, level, chalk).orElse(0.0);
        CirclePlan.Result result = CirclePlan.plan(altarBill, epTotal, members, GroupsConfig.linkMemberShare(), GroupsConfig.epKeepPercent());
        for (CirclePlan.Dropped d : result.dropped()) {
            if (level.getPlayerByUUID(d.id()) instanceof ServerPlayer p) {
                p.sendSystemMessage(CovenText.of("link_dropped." + d.reason().name().toLowerCase(java.util.Locale.ROOT)));
            }
        }
        CircleStage.set(new CircleStage.Staged(caster, level.dimension(), chalk, result));
        return result.altarPays();
    }

    /** The rite has started: each member's share leaves their meter (and hurts when the meter was over). */
    public static void pay(CircleStage.Staged staged) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = server == null ? null : server.getLevel(staged.dimension());
        if (level == null) {
            return;
        }
        long now = PackIndex.now();
        GroupIndex groups = GroupIndex.get(level);
        for (Map.Entry<UUID, CirclePlan.Share> e : staged.result().shares().entrySet()) {
            if (!(level.getPlayerByUUID(e.getKey()) instanceof ServerPlayer p)) {
                continue;
            }
            int cap = capacityOf(p);
            int meter = meterOf(groups, e.getKey(), cap, now);
            groups.setChannel(e.getKey(), Math.max(0, meter - e.getValue().power()), now, cap);
            Strain.apply(p, e.getValue(), cap);
            net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.changed(p);
        }
        CircleLink.Session session = CircleLink.current(level, staged.chalk(), staged.anchor());
        if (session != null) {
            CircleLink.touch(session);
        }
        LinkWindow.open(level, staged);
    }
}