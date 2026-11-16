package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
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
 * Open Parties and Claims through reflection: the mod is not a build dependency, so without it nothing here is loaded, and
 * a change in its API only turns the link off (once, with a warning). Every name below was read from the 0.33.0 jar (and the
 * Team Claims 1.5.0 fork, which has the same classes) with javap.
 */
final class OpacBackend implements PartyBackend {
    private final Method apiGet;
    private final Method partyManager;
    private final Method byOwner;
    private final Method createForOwner;
    private final Method removeByOwner;
    private final Method memberStream;
    private final Method memberUuid;
    private final Method addMember;
    private final Method removeMember;
    private final Class<?> rankClass;
    private boolean broken;

    private OpacBackend() throws ReflectiveOperationException {
        Class<?> api = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
        Class<?> manager = Class.forName("xaero.pac.common.server.parties.party.api.IPartyManagerAPI");
        Class<?> party = Class.forName("xaero.pac.common.server.parties.party.api.IServerPartyAPI");
        Class<?> member = Class.forName("xaero.pac.common.parties.party.member.api.IPartyMemberAPI");
        rankClass = Class.forName("xaero.pac.common.parties.party.member.PartyMemberRank");
        apiGet = api.getMethod("get", MinecraftServer.class);
        partyManager = api.getMethod("getPartyManager");
        byOwner = manager.getMethod("getPartyByOwner", UUID.class);
        createForOwner = manager.getMethod("createPartyForOwner", Player.class);
        removeByOwner = manager.getMethod("removePartyByOwner", UUID.class);
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

    private Object manager(MinecraftServer server) throws ReflectiveOperationException {
        return partyManager.invoke(apiGet.invoke(null, server));
    }

    private void fail(Exception e) {
        if (!broken) {
            broken = true;
            SupernaturalBloodlines.LOGGER.warn("The pack-party link stopped: Open Parties and Claims refused a call ({})", e.toString());
        }
    }

    @Override
    @Nullable
    public Party find(MinecraftServer server, List<UUID> candidates) {
        if (broken) {
            return null;
        }
        try {
            Object manager = manager(server);
            for (UUID owner : candidates) {
                Object party = byOwner.invoke(manager, owner);
                if (party != null) {
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
                    return new Party(owner, members);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
        }
        return null;
    }

    @Override
    public boolean create(MinecraftServer server, UUID owner) {
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p == null || broken) {
            return false;
        }
        try {
            return createForOwner.invoke(manager(server), p) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return false;
        }
    }

    @Override
    public void disband(MinecraftServer server, UUID owner) {
        try {
            removeByOwner.invoke(manager(server), owner);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
        }
    }

    @Override
    public void add(MinecraftServer server, UUID owner, UUID member, boolean admin) {
        try {
            Object party = byOwner.invoke(manager(server), owner);
            if (party != null) {
                String name = PackAccess.nameOf(server, member);
                addMember.invoke(party, member, Enum.valueOf(rankClass.asSubclass(Enum.class), admin ? "ADMIN" : "MEMBER"), name != null ? name : member.toString().substring(0, 8));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
        }
    }

    @Override
    public void remove(MinecraftServer server, UUID owner, UUID member) {
        try {
            Object party = byOwner.invoke(manager(server), owner);
            if (party != null) {
                removeMember.invoke(party, member);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
        }
    }
}
