package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.ClaimGuard;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardEnforcer;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ward.WardIndex;

/** What the ward GameTests share: people placed in the cell, wards raised straight in the index, and a clean-up that leaves nothing for the next test. */
final class WardTestSupport {
    private static final List<String> SETTINGS = List.of("ward_enabled", "ward_radius", "ward_max_per_owner", "ward_max_invites", "ward_falls_on_owner_death",
            "ward_affects_mobs", "ward_blocks_werewolves", "ward_expel_on_revoke", "ward_expel_seconds", "ward_spoken_invites", "ward_door_enabled",
            "ward_alert_owner", "ward_use_claims");

    private WardTestSupport() {
    }

    /** The middle of the cell, where wards are raised in these tests. */
    static BlockPos centre(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(8, 2, 8));
    }

    /** A person standing {@code dx}, {@code dz} blocks from the cell's centre. */
    static TalkingDuelist person(GameTestHelper helper, double dx, double dz) {
        TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(Vec3.atBottomCenterOf(centre(helper)).add(dx, 0, dz));
        // a fake player is not in the level by itself, and the ward looks people up there
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    static TalkingDuelist vampire(GameTestHelper helper, double dx, double dz) {
        TalkingDuelist p = person(helper, dx, dz);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    static void species(ServerPlayer p, Species species) {
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
    }

    /** An area ward of {@code half} cells at the centre of the cell, owned by {@code owner}. */
    static WardTable.Ward ward(GameTestHelper helper, ServerPlayer owner, int half) {
        return WardIndex.get(helper.getLevel()).add(WardRules.Kind.AREA, WardEnforcer.dimension(helper.getLevel()), centre(helper).getX(), centre(helper).getY(),
                centre(helper).getZ(), half, owner.getUUID(), 99).orElseThrow();
    }

    static WardTable.Ward wardAt(GameTestHelper helper, ServerPlayer owner, WardRules.Kind kind, BlockPos at, int half) {
        return WardIndex.get(helper.getLevel()).add(kind, WardEnforcer.dimension(helper.getLevel()), at.getX(), at.getY(), at.getZ(), half, owner.getUUID(), 99).orElseThrow();
    }

    static void invite(GameTestHelper helper, ServerPlayer owner, ServerPlayer who) {
        WardIndex.get(helper.getLevel()).inviteAll(owner.getUUID(), who.getUUID(), 99);
    }

    static int wardsOf(GameTestHelper helper, ServerPlayer owner) {
        return WardIndex.get(helper.getLevel()).table().ownedBy(owner.getUUID()).size();
    }

    /** Drops every ward of these people, gives them back their settings and takes them out of every faction. */
    static void clean(GameTestHelper helper, ServerPlayer... people) {
        WardIndex index = WardIndex.get(helper.getLevel());
        for (ServerPlayer p : people) {
            for (WardTable.Ward w : index.table().ownedBy(p.getUUID())) {
                index.drop(w.id(), p.getUUID());
            }
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        SETTINGS.forEach(key -> ArtifactsConfig.override(key, null));
        ClaimGuard.install(null);
        WardEnforcer.reset();
    }
}
