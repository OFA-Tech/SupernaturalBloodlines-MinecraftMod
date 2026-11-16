package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Open Parties and Claims through reflection: the mod is not a build dependency, so without it nothing here is loaded. Every
 * name below was read from the 0.33.0 jar (and the Team Claims 1.5.0 fork, which has the same classes) with javap. A call that
 * throws is counted by the {@link CallGuard}; {@link #FAIL_LIMIT} in a row turn the link off (once, with a warning), one success
 * starts the count again.
 */
final class OpacBackend implements PartyBackend {
    static final int FAIL_LIMIT = 5;

    private final Method apiGet;
    private final Method partyManager;
    private final Method byOwner;
    private final Method byMember;
    private final Method createForOwner;
    private final Method partyId;
    private final Method memberStream;
    private final Method memberUuid;
    private final Method addMember;
    private final Method removeMember;
    private final Class<?> rankClass;
    private final CallGuard guard = new CallGuard(FAIL_LIMIT,
            e -> SupernaturalBloodlines.LOGGER.warn("The pack-party link is off: Open Parties and Claims failed {} calls in a row (last: {})", FAIL_LIMIT, e.toString()));

    private OpacBackend() throws ReflectiveOperationException {
        Class<?> api = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
        Class<?> manager = Class.forName("xaero.pac.common.server.parties.party.api.IPartyManagerAPI");
        Class<?> party = Class.forName("xaero.pac.common.server.parties.party.api.IServerPartyAPI");
        Class<?> member = Class.forName("xaero.pac.common.parties.party.member.api.IPartyMemberAPI");
        rankClass = Class.forName("xaero.pac.common.parties.party.member.PartyMemberRank");
        apiGet = api.getMethod("get", MinecraftServer.class);
        partyManager = api.getMethod("getPartyManager");
        byOwner = manager.getMethod("getPartyByOwner", UUID.class);
        byMember = manager.getMethod("getPartyByMember", UUID.class);
        createForOwner = manager.getMethod("createPartyForOwner", Player.class);
        partyId = party.getMethod("getId");
        memberStream = party.getMethod("getMemberInfoStream");
        memberUuid = member.getMethod("getUUID");
        addMember = party.getMethod("addMember", UUID.class, rankClass, String.class);
        removeMember = party.getMethod("removeMember", UUID.class);
    }

    /** The backend, or null when the mod is missing or its API is not the one read from the jar. */
    @Nullable
    static OpacBackend tryCreate() {
        try {
            return new OpacBackend();
        } catch (ReflectiveOperationException | LinkageError e) {
            SupernaturalBloodlines.LOGGER.warn("Open Parties and Claims is loaded but its party API is not the one this mod was written against; the pack-party link is off ({})", e.toString());
            return null;
        }
    }

    boolean isOff() {
        return guard.off();
    }

    private Object manager(MinecraftServer server) throws ReflectiveOperationException {
        return partyManager.invoke(apiGet.invoke(null, server));
    }

    @Override
    public Found find(MinecraftServer server, UUID owner, UUID id) {
        CallGuard.Answer<Found> a = guard.call(() -> {
            Object party = byOwner.invoke(manager(server), owner);
            if (party == null || !id.equals(partyId.invoke(party))) {
                return new Found(true, null);
            }
            Set<UUID> members = new HashSet<>();
            members.add(owner);
            try (Stream<?> s = (Stream<?>) memberStream.invoke(party)) {
                members.addAll(s.map(m -> {
                    try {
                        return (UUID) memberUuid.invoke(m);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }).collect(Collectors.toSet()));
            }
            return new Found(true, new Party(owner, members));
        });
        return a.ok() ? a.value() : new Found(false, null);
    }

    @Override
    public Optional<UUID> create(MinecraftServer server, UUID owner) {
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p == null || guard.off()) {
            return Optional.empty();
        }
        return guard.call(() -> {
            Object manager = manager(server);
            // a party the owner has already is theirs: it is never adopted, and never replaced
            if (byOwner.invoke(manager, owner) != null || byMember.invoke(manager, owner) != null) {
                return Optional.<UUID>empty();
            }
            Object party = createForOwner.invoke(manager, p);
            return party == null ? Optional.<UUID>empty() : Optional.of((UUID) partyId.invoke(party));
        }).toOptional().flatMap(o -> o);
    }

    @Override
    public boolean add(MinecraftServer server, UUID owner, UUID member) {
        CallGuard.Answer<Boolean> a = guard.call(() -> {
            Object manager = manager(server);
            Object party = byOwner.invoke(manager, owner);
            if (party == null) {
                return false;
            }
            Object theirs = byMember.invoke(manager, member);
            if (theirs != null) {
                // already in a party (ours counts as done, another one is not ours to move them out of)
                return partyId.invoke(theirs).equals(partyId.invoke(party));
            }
            String name = PackAccess.nameOf(server, member);
            return addMember.invoke(party, member, Enum.valueOf(rankClass.asSubclass(Enum.class), "MEMBER"), name != null ? name : member.toString().substring(0, 8)) != null;
        });
        return a.ok() && Boolean.TRUE.equals(a.value());
    }

    @Override
    public void remove(MinecraftServer server, UUID owner, UUID member) {
        guard.call(() -> {
            Object party = byOwner.invoke(manager(server), owner);
            if (party != null) {
                removeMember.invoke(party, member);
            }
            return null;
        });
    }
}
