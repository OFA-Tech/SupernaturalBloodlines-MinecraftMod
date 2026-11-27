package net.ofatech.supernaturalbloodlines.parts.species.rite;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;

/**
 * The Great Harvest (R266, tier 3): the linked members split {@code harvest_xp_levels} of experience levels and every crop within the reach gets up to three
 * steps of bone meal, touching at most {@code harvest_max_blocks} blocks. A member who cannot pay their share refuses the rite before anyone is charged.
 * It never hurts a creature and never takes one.
 */
public record HarvestRiteFactory(int minCircle) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "great_harvest");
    public static final String LEVELS = "supernaturalbloodlines.rite.grand.levels";
    public static final String DONE = "supernaturalbloodlines.rite.grand.harvest_done";
    public static final int PASSES = 3;
    public static final MapCodec<HarvestRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("min_circle", 0).forGetter(HarvestRiteFactory::minCircle)).apply(i, HarvestRiteFactory::new));

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new HarvestRite(base, params, minCircle);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class HarvestRite extends Rite {
        private final int minCircle;

        HarvestRite(BaseRiteParams base, RiteParams params, int minCircle) {
            super(base, params);
            this.minCircle = minCircle;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            String id = GrandGate.idOf(level, type);
            ServerPlayer caster = level.getPlayerByUUID(params.caster) instanceof ServerPlayer sp ? sp : null;
            Optional<String> refused = GrandGate.check(level, params.caster, id, minCircle);
            if (refused.isPresent()) {
                if (caster != null) {
                    RiteTell.refuse(caster, refused.get());
                }
                return cancel();
            }
            Map<UUID, ServerPlayer> payers = new java.util.LinkedHashMap<>();
            CircleStage.Staged staged = CircleStage.current();
            List<UUID> order = staged == null ? List.of() : new ArrayList<>(staged.result().shares().keySet());
            for (UUID member : order) {
                if (level.getPlayerByUUID(member) instanceof ServerPlayer p) {
                    payers.put(member, p);
                }
            }
            long[] shares = CirclePlan.split(GroupsConfig.harvestXpLevels(), payers.size());
            int i = 0;
            for (ServerPlayer p : payers.values()) {
                if (p.experienceLevel < shares[i++]) {
                    if (caster != null) {
                        RiteTell.refuse(caster, LEVELS, GroupsConfig.harvestXpLevels(), java.util.Arrays.stream(shares).max().orElse(0));
                    }
                    return cancel();
                }
            }
            i = 0;
            for (ServerPlayer p : payers.values()) {
                p.giveExperienceLevels(-(int) shares[i++]);
            }
            grow(FeteRiteFactory.radius(), GroupsConfig.harvestMaxBlocks());
            GrandGate.stamp(level, params.caster, id);
            if (caster != null) {
                RiteTell.say(caster, DONE);
            }
            return false;
        }

        /** Up to {@link #PASSES} steps of bone meal on every crop in the reach, over at most {@code cap} different blocks. */
        private void grow(int radius, int cap) {
            Set<BlockPos> touched = new LinkedHashSet<>();
            double r2 = (double) radius * radius;
            for (int pass = 0; pass < PASSES; pass++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dz * dz > r2) {
                            continue;
                        }
                        for (int dy = -2; dy <= 4; dy++) {
                            BlockPos at = pos.offset(dx, dy, dz);
                            if (!touched.contains(at) && touched.size() >= cap || !level.isLoaded(at)) {
                                continue;
                            }
                            BlockState state = level.getBlockState(at);
                            if (state.getBlock() instanceof CropBlock crop && crop instanceof BonemealableBlock grower && grower.isValidBonemealTarget(level, at, state)) {
                                grower.performBonemeal(level, level.random, at, state);
                                touched.add(at);
                            }
                        }
                    }
                }
            }
        }
    }
}