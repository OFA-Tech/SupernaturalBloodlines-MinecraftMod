package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.ArmsBonus;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Brothers in arms (R292): each other brother who counts and stands within the radius adds a little attack damage, up to a cap. A live transient
 * modifier, never saved, refreshed every 20 ticks and removed when alone, when the group is not a brotherhood, when perks are off, at logout, when the
 * player does not fit or is a Code-breaker. Nothing here reads or writes a mark or a kill. The count the hub shows comes from the same call.
 */
public final class BrotherhoodArms {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "brothers_in_arms");
    private static Supplier<List<ServerPlayer>> players;
    private static int looks;

    private BrotherhoodArms() {
    }

    /** For tests: the players the refresh reads instead of the server's list; null restores it. */
    public static void overridePlayers(Supplier<List<ServerPlayer>> supplier) {
        players = supplier;
    }

    /** For tests: how many brothers the last refresh looked at in all. */
    public static int lastLooks() {
        return looks;
    }

    /** The other brothers who count for {@code p} and stand near; 0 for somebody the bonus does not reach. */
    public static int brothersNear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, p.getUUID());
        if (b == null || !receives(p, b)) {
            return 0;
        }
        int radius = GroupsConfig.armsRadius();
        int count = 0;
        for (UUID id : PackIndex.get(level).roster(b.leader())) {
            if (id.equals(p.getUUID())) {
                continue;
            }
            looks++;
            ServerPlayer mate = SireCall.player(p.server, id);
            boolean here = mate != null;
            if (ArmsBonus.counts(here && BrotherhoodScope.appliesTo(level, b.leader(), mate), here && Codes.isBreaker(p.server, id), here, here && mate.isAlive(),
                    here && mate.isSpectator(), here && mate.level() == p.level(), here ? mate.distanceToSqr(p) : Double.MAX_VALUE, radius)) {
                count++;
            }
        }
        return count;
    }

    /** The player gets the bonus at all: perks on, they fit, are alive, not a spectator and no Code-breaker. */
    private static boolean receives(ServerPlayer p, BrotherhoodScope.Brotherhood b) {
        return p.isAlive() && !p.isSpectator() && BrotherhoodScope.appliesTo(p.serverLevel(), b.leader(), p) && !Codes.isBreaker(p.server, p.getUUID());
    }

    /** The bonus {@code p} has now, as a fraction (0.12 is 12%): what the modifier holds and what the hub shows. Perks off or a group of another type make {@link #brothersNear} 0. */
    public static double bonusFor(ServerPlayer p) {
        return ArmsBonus.of(brothersNear(p), GroupsConfig.armsPercent(), GroupsConfig.armsMaxPercent());
    }

    public static void refresh(MinecraftServer server) {
        looks = 0;
        for (ServerPlayer p : players != null ? players.get() : server.getPlayerList().getPlayers()) {
            set(p, bonusFor(p));
        }
    }

    /** At logout, and anywhere the player stops being counted. */
    public static void remove(ServerPlayer p) {
        set(p, 0);
    }

    static void set(ServerPlayer p, double amount) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (a == null) {
            return;
        }
        AttributeModifier current = a.getModifier(MODIFIER_ID);
        if (amount <= 0) {
            if (current != null) {
                a.removeModifier(MODIFIER_ID);
            }
        } else if (current == null || current.amount() != amount) {
            a.removeModifier(MODIFIER_ID);
            a.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount, Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}