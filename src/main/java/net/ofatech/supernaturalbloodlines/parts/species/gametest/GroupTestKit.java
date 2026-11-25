package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** The players and the clean-up the group GameTests share. A fake player is told through translation keys a test can read. */
public final class GroupTestKit {
    private GroupTestKit() {
    }

    public static TalkingDuelist human(GameTestHelper helper, double x) {
        TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        return p;
    }

    public static TalkingDuelist witch(GameTestHelper helper, double x) {
        TalkingDuelist p = human(helper, x);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        return p;
    }

    static TalkingDuelist vampire(GameTestHelper helper, double x) {
        TalkingDuelist p = human(helper, x);
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
        return p;
    }

    static TalkingDuelist hunter(GameTestHelper helper, double x) {
        TalkingDuelist p = human(helper, x);
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
        return p;
    }

    public static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
    }

    /** Invite and accept; false when either was refused. */
    public static boolean join(ServerPlayer leader, ServerPlayer target) {
        if (PackCommand.invite(leader, target) != null) {
            return false;
        }
        String token = PackCommand.requests().tokenOf(leader.getUUID(), target.getUUID()).orElseThrow();
        return PackCommand.accept(target, token) == null;
    }

    static String key(Component c) {
        return c == null ? "" : c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    public static void clean(GameTestHelper helper, ServerPlayer... players) {
        PackIndex index = PackIndex.get(helper.getLevel());
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        SireCall.overridePlayerLookup(null);
        PackIndex.overrideClock(null);
        net.ofatech.supernaturalbloodlines.parts.species.group.clan.CharterScan.overridePlayers(null);
        net.ofatech.supernaturalbloodlines.parts.species.group.clan.Contests.overrideDayTime(null);
        net.ofatech.supernaturalbloodlines.parts.species.group.clan.SharedFeeding.reset();
        for (String k : List.of("groups_enabled", "clans_enabled", "covens_enabled", "open_founding", "max_members", "daywalker_days", "exile_days", "charter_enabled", "charter_min_members", "challenger_min_days", "contest_cooldown_days", "feed_share_enabled", "sireline_bonus_percent", "sun_ring_day_walker_only", "contest_accept_minutes", "contest_min_minutes", "contest_max_hours", "feed_share_percent", "feed_share_max", "feed_share_radius", "feed_share_cap_per_minute", "sireline_bonus_max_percent", "charter_forge_cost", "charter_lost_days")) {
            GroupsConfig.override(k, null);
        }
        for (ServerPlayer p : players) {
            for (UUID member : index.betasOf(p.getUUID())) {
                index.leave(member);
                groups.clearRanks(member);
            }
            for (UUID ally : List.copyOf(index.alliesOf(p.getUUID()))) {
                index.unally(p.getUUID(), ally);
            }
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            index.takeDissolved(p.getUUID());
            index.takeNotices(p.getUUID());
            groups.drop(p.getUUID());
            groups.clearRanks(p.getUUID());
            PackCommand.forget(p.getUUID());
        }
        groups.prune(index);
    }
}
