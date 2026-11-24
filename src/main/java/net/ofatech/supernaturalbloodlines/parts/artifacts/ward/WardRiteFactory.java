package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;

/**
 * The Rites of the Ward (R300, R301, R327): a witch-type caster raises a ward on the altar (the area form) or on the nearest door within four blocks (the
 * door form). The owner is whoever sneaks on the circle's centre, else the caster; a player who stands there without sneaking cancels the rite and
 * everything comes back. <b>Only a non-vampiric owner makes a ward</b> (a vampire's home has no barrier). Every other refusal (the switch, a caster who
 * is not a witch type, a vampiric owner, too many wards, claimed ground, no door) is the same message at the same cost, so a refusal never says which
 * condition failed (R325).
 */
public record WardRiteFactory(WardRules.Kind mode) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ward");
    public static final String WONT_TAKE = "supernaturalbloodlines.ward.wont_take";
    public static final String CANCELLED = "supernaturalbloodlines.ward.cancelled";
    public static final String SNEAK_TO_OWN = "supernaturalbloodlines.ward.sneak_to_own";
    public static final String RAISED = "supernaturalbloodlines.ward.raised";
    public static final int DOOR_REACH = 4;
    public static final MapCodec<WardRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("mode", "area").forGetter(f -> f.mode().name().toLowerCase(Locale.ROOT))
    ).apply(i, text -> new WardRiteFactory(WardRules.Kind.valueOf(text.toUpperCase(Locale.ROOT)))));

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new WardRite(base, params, mode);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class WardRite extends Rite {
        private final WardRules.Kind kind;

        WardRite(BaseRiteParams base, RiteParams params, WardRules.Kind kind) {
            super(base, params);
            this.kind = kind;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer caster)) {
                return cancel();
            }
            ServerPlayer owner = caster;
            Optional<ServerPlayer> onCentre = level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).expandTowards(0, 1, 0), p -> p.isAlive() && !p.isSpectator())
                    .stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)));
            if (onCentre.isPresent() && onCentre.get() != caster) {
                if (!onCentre.get().isShiftKeyDown()) {
                    caster.displayClientMessage(Component.translatable(CANCELLED), true);
                    onCentre.get().displayClientMessage(Component.translatable(SNEAK_TO_OWN), true);
                    return cancel();
                }
                owner = onCentre.get();
            }
            Optional<BlockPos> centre = kind == WardRules.Kind.AREA ? Optional.of(pos) : nearestDoor();
            boolean allowed = ArtifactsConfig.wardEnabled() && (kind == WardRules.Kind.AREA || ArtifactsConfig.wardDoorEnabled())
                    && BloodlineAccess.get(caster).species().witchType()
                    && WardRules.mayOwn(Supernatural.isVampiric(owner))
                    && centre.isPresent()
                    && (!ArtifactsConfig.wardUseClaims() || ClaimGuard.allowsBox(level, centre.get(),
                            kind == WardRules.Kind.AREA ? ArtifactsConfig.wardRadius() : 1, owner.getUUID()));
            WardIndex index = WardIndex.get(level);
            Optional<net.ofatech.supernaturalbloodlines.core.ward.WardTable.Ward> ward = allowed
                    ? index.add(kind, WardEnforcer.dimension(level), centre.get().getX(), centre.get().getY(), centre.get().getZ(),
                            kind == WardRules.Kind.AREA ? ArtifactsConfig.wardRadius() : 1, owner.getUUID(), ArtifactsConfig.wardMaxPerOwner())
                    : Optional.empty();
            if (ward.isEmpty()) {
                caster.displayClientMessage(Component.translatable(WONT_TAKE), true);
                return false; // not cancelled: the ingredients and the altar's power are spent, whatever the reason
            }
            if (caster != owner) {
                index.inviteAll(owner.getUUID(), caster.getUUID(), ArtifactsConfig.wardMaxInvites());
                caster.displayClientMessage(Component.translatable(RAISED), true);
            }
            owner.displayClientMessage(Component.translatable(RAISED), true);
            return false;
        }

        private Optional<BlockPos> nearestDoor() {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-DOOR_REACH, -DOOR_REACH, -DOOR_REACH), pos.offset(DOOR_REACH, DOOR_REACH, DOOR_REACH))) {
                var state = level.getBlockState(cell);
                if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES)) {
                    double d = cell.distSqr(pos);
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = cell.immutable();
                    }
                }
            }
            return Optional.ofNullable(best);
        }
    }
}
