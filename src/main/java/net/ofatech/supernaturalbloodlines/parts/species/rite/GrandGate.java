package net.ofatech.supernaturalbloodlines.parts.species.rite;

import java.util.Optional;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.coven.GrandRites;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/**
 * The one gate of the three grand rites (R255, R268), asked first in each rite's {@code onStart}: the rites on, a coven, enough linked members in the plan
 * the cost hook staged (after its drops), and the coven's cooldown for this rite. The answer is one line to the performer and says nothing about anybody
 * outside the performer's own coven; the rite then cancels itself, so the ingredients come back and no member pays.
 */
public final class GrandGate {
    public static final String OFF = "supernaturalbloodlines.rite.grand.off";
    public static final String CIRCLE = "supernaturalbloodlines.rite.grand.circle";
    public static final String COOLDOWN = "supernaturalbloodlines.rite.grand.cooldown";

    private GrandGate() {
    }

    /** The rite file's own name, {@code fete_des_benedictions}. */
    public static String idOf(ServerLevel level, RiteType type) {
        ResourceLocation id = level.registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY).getKey(type);
        return id == null ? "" : id.getPath();
    }

    /** The lang key of the refusal, or empty when the rite may go on. A solo caster, no coven, a circle that is too small and a link that is off all answer {@link #CIRCLE}. */
    public static Optional<String> check(ServerLevel level, UUID caster, String rite, int fileMin) {
        if (!GroupsConfig.grandRitesEnabled()) {
            return Optional.of(OFF);
        }
        ServerPlayer p = level.getPlayerByUUID(caster) instanceof ServerPlayer sp ? sp : null;
        UUID leader = p == null || !GroupsConfig.covensEnabled() ? null : CircleLink.covenOf(p);
        int need = GrandRites.minCircle(rite, fileMin, GroupsConfig.tier1Members(), GroupsConfig.tier2Members(), GroupsConfig.tier3Members());
        if (leader == null || CircleStage.size() < need) {
            return Optional.of(CIRCLE);
        }
        GroupIndex groups = GroupIndex.get(level);
        UUID group = groups.existingGroupOf(leader).orElse(null);
        if (group != null && groups.cooldownUntil(group, rite) > PackIndex.now()) {
            return Optional.of(COOLDOWN);
        }
        return Optional.empty();
    }

    /** Stamped only when the rite really worked. */
    public static void stamp(ServerLevel level, UUID caster, String rite) {
        ServerPlayer p = level.getPlayerByUUID(caster) instanceof ServerPlayer sp ? sp : null;
        UUID leader = p == null ? null : CircleLink.covenOf(p);
        GroupIndex groups = GroupIndex.get(level);
        UUID group = leader == null ? null : groups.existingGroupOf(leader).orElse(null);
        if (group != null && GroupsConfig.grandRiteCooldownHours() > 0) {
            groups.setCooldown(group, rite, GrandRites.cooldownUntil(PackIndex.now(), GroupsConfig.grandRiteCooldownHours()));
        }
    }
}