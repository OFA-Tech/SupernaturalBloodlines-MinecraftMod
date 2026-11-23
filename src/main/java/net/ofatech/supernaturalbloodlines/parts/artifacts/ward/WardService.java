package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import org.jetbrains.annotations.Nullable;

/**
 * What an owner does with their wards (R305, R306, R308): invite, withdraw, drop. The command, the hub button and the spoken phrase all end here, so
 * the three cannot differ. Every failure is one of two replies that name nothing about anybody else's ward (R325): the owner has no ward, or no such name.
 * A person is invited into every standing ward of the owner at once (a home is one place for them).
 */
public final class WardService {
    public static final String KEY = "supernaturalbloodlines.ward.";
    public static final String INVITED_YOU = KEY + "invited_you";
    public static final String YOU_INVITED = KEY + "you_invited";
    public static final String NOT_WELCOME = KEY + "not_welcome";
    public static final String YOU_WITHDREW = KEY + "you_withdrew";
    public static final String DROPPED = KEY + "dropped";
    private static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private WardService() {
    }

    static Component error(String key) {
        return Component.translatable(KEY + key);
    }

    /** The player if they are online (on the player list, or in this level: a test's fake player is only in the level). */
    @Nullable
    static ServerPlayer find(ServerLevel level, UUID id) {
        ServerPlayer listed = level.getServer().getPlayerList().getPlayer(id);
        return listed != null ? listed : level.getPlayerByUUID(id) instanceof ServerPlayer inLevel ? inLevel : null;
    }

    private static String name(ServerLevel level, UUID id) {
        MinecraftServer server = level.getServer();
        ServerPlayer online = find(level, id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getProfileCache() == null ? id.toString().substring(0, 8)
                : server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName).orElse(id.toString().substring(0, 8));
    }

    /** @return null when it worked (or the person was already on the lists), else the reply */
    @Nullable
    public static Component invite(ServerPlayer owner, ServerPlayer who) {
        Optional<WardRules.InviteResult> result = WardIndex.get(owner.serverLevel()).inviteAll(owner.getUUID(), who.getUUID(), ArtifactsConfig.wardMaxInvites());
        if (result.isEmpty()) {
            return error("no_ward");
        }
        switch (result.get()) {
            case FULL -> {
                return error("list_full");
            }
            case ADDED -> {
                if (who != owner) {
                    who.sendSystemMessage(Component.translatable(INVITED_YOU, owner.getGameProfile().getName()));
                    owner.sendSystemMessage(Component.translatable(YOU_INVITED, who.getGameProfile().getName()));
                }
            }
            default -> {
            }
        }
        return null;
    }

    /** Withdraws a name or a UUID from every ward of the owner; the vampire inside is thrown out by the next push, or left alone until they leave (R308). */
    @Nullable
    public static Component uninvite(ServerPlayer owner, String word) {
        UUID id = match(owner, word);
        return id == null ? error("no_such") : withdraw(owner, id);
    }

    @Nullable
    static Component withdraw(ServerPlayer owner, UUID id) {
        WardIndex index = WardIndex.get(owner.serverLevel());
        ServerPlayer who = find(owner.serverLevel(), id);
        if (who != null && !ArtifactsConfig.wardExpelOnRevoke()) {
            WardEnforcer.tolerateInside(index.table().ownedBy(owner.getUUID()), who);
        }
        if (!index.uninviteAll(owner.getUUID(), id)) {
            return error("no_such");
        }
        owner.sendSystemMessage(Component.translatable(YOU_WITHDREW, name(owner.serverLevel(), id)));
        if (who != null) {
            who.sendSystemMessage(Component.translatable(NOT_WELCOME));
        }
        return null;
    }

    @Nullable
    public static Component drop(ServerPlayer owner, String id) {
        if (!WardIndex.get(owner.serverLevel()).drop(id, owner.getUUID())) {
            return error("no_such");
        }
        WardEnforcer.forgetWard(id);
        owner.sendSystemMessage(Component.translatable(DROPPED));
        return null;
    }

    /** A UUID, or a name among the people on the owner's lists. */
    @Nullable
    static UUID match(ServerPlayer owner, String word) {
        List<WardTable.Ward> mine = WardIndex.get(owner.serverLevel()).table().ownedBy(owner.getUUID());
        if (UUID_TEXT.matcher(word).matches()) {
            UUID id = UUID.fromString(word);
            return mine.stream().anyMatch(w -> w.invited().contains(id)) ? id : null;
        }
        for (WardTable.Ward ward : mine) {
            for (UUID id : ward.invited()) {
                if (name(owner.serverLevel(), id).equalsIgnoreCase(word)) {
                    return id;
                }
            }
        }
        return null;
    }
}
