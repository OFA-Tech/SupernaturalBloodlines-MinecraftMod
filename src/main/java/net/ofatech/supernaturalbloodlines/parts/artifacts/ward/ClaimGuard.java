package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.FailureBreaker;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The only thing Open Parties and Claims decides about a ward (R301): whether the ground may be built on by this owner, that is unclaimed, claimed
 * by the owner, or claimed by somebody in the owner's party. It never defines the home. Reflection, as the pack-party link: every name below was
 * read from the 0.33.0 jar with javap, the mod is not a build dependency, and without it, or when its API is not the one read, there is no check.
 * Five failed calls in a row turn the check off (fail open, one warning), one answer starts the count again.
 */
public final class ClaimGuard {
    /** What a test installs in place of the mod. */
    public interface Check {
        boolean allows(ServerLevel level, BlockPos pos, UUID owner);
    }

    private static final int FAIL_LIMIT = 5;
    private static volatile Check override;
    @Nullable
    private static Opac opac;
    private static boolean tried;

    private ClaimGuard() {
    }

    @VisibleForTesting
    public static void install(@Nullable Check check) {
        override = check;
    }

    /** Whether {@code owner} may raise a ward at {@code pos}. True when there is no claims mod, the link is off, or the mod's API failed. */
    public static boolean allows(ServerLevel level, BlockPos pos, UUID owner) {
        Check pinned = override;
        if (pinned != null) {
            return pinned.allows(level, pos, owner);
        }
        Opac mod = opac();
        return mod == null || mod.allows(level.getServer(), level.dimension().location(), pos, owner);
    }

    @Nullable
    private static synchronized Opac opac() {
        if (!tried) {
            tried = true;
            try {
                opac = new Opac();
            } catch (ReflectiveOperationException | LinkageError missing) {
                opac = null;
            }
        }
        return opac;
    }

    private static final class Opac {
        private final Method apiGet;
        private final Method claimsManager;
        private final Method claimAt;
        private final Method claimOwner;
        private final Method partyManager;
        private final Method byMember;
        private final Method byOwner;
        private final Method partyId;
        private final FailureBreaker breaker = new FailureBreaker(FAIL_LIMIT);

        Opac() throws ReflectiveOperationException {
            Class<?> api = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
            Class<?> claims = Class.forName("xaero.pac.common.server.claims.api.IServerClaimsManagerAPI");
            Class<?> claim = Class.forName("xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI");
            Class<?> parties = Class.forName("xaero.pac.common.server.parties.party.api.IPartyManagerAPI");
            Class<?> party = Class.forName("xaero.pac.common.server.parties.party.api.IServerPartyAPI");
            apiGet = api.getMethod("get", MinecraftServer.class);
            claimsManager = api.getMethod("getServerClaimsManager");
            claimAt = claims.getMethod("get", net.minecraft.resources.ResourceLocation.class, BlockPos.class);
            claimOwner = claim.getMethod("getPlayerId");
            partyManager = api.getMethod("getPartyManager");
            byMember = parties.getMethod("getPartyByMember", UUID.class);
            byOwner = parties.getMethod("getPartyByOwner", UUID.class);
            partyId = party.getMethod("getId");
        }

        boolean allows(MinecraftServer server, net.minecraft.resources.ResourceLocation dimension, BlockPos pos, UUID owner) {
            if (breaker.tripped()) {
                return true;
            }
            try {
                Object serverApi = apiGet.invoke(null, server);
                Object claim = claimAt.invoke(claimsManager.invoke(serverApi), dimension, pos);
                if (claim == null) {
                    breaker.record(true);
                    return true;
                }
                UUID claimant = (UUID) claimOwner.invoke(claim);
                boolean ok = owner.equals(claimant) || sameParty(partyManager.invoke(serverApi), owner, claimant);
                breaker.record(true);
                return ok;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                if (breaker.record(false)) {
                    SupernaturalBloodlines.LOGGER.warn("The ward claim check is off: Open Parties and Claims failed {} calls in a row (last: {})", FAIL_LIMIT, e.toString());
                }
                return true;
            }
        }

        private boolean sameParty(Object manager, UUID a, UUID b) throws ReflectiveOperationException {
            Object partyA = partyOf(manager, a);
            Object partyB = partyOf(manager, b);
            return partyA != null && partyB != null && partyId.invoke(partyA).equals(partyId.invoke(partyB));
        }

        private Object partyOf(Object manager, UUID id) throws ReflectiveOperationException {
            Object party = byMember.invoke(manager, id);
            return party != null ? party : byOwner.invoke(manager, id);
        }
    }
}
