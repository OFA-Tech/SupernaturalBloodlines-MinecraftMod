package net.ofatech.supernaturalbloodlines.parts.species.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.Imprisonment;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/**
 * Rite of the Imprisoned Original (R126): harder and costlier than the Broken Line. The Original stands on the circle's centre inside a
 * closed mountain ash ring for the whole rite ({@code imprisonment_rite_seconds}); then the target and their whole branch leave the
 * Original's line, never the Original's whole line. It is checked at the start (a refusal gives the ingredients back) and every half
 * second after: a broken ring, an Original who steps out, the eclipse ending, the target leaving or the caster going ends the rite
 * with no effect, and the ingredients are spent.
 */
public record ImprisonedOriginalRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "imprisoned_original");
    public static final String DONE = "supernaturalbloodlines.rite.imprisoned_done";
    public static final String RING_BROKEN = LineRules.REFUSAL_PREFIX + "ring_broken";
    public static final MapCodec<ImprisonedOriginalRiteFactory> CODEC = MapCodec.unit(ImprisonedOriginalRiteFactory::new);
    private static final int CHECK_EVERY = 10;

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new ImprisonedRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class ImprisonedRite extends Rite {
        private UUID caster;
        private UUID target;
        private UUID root;
        private int ticksLeft;

        ImprisonedRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        /** The first refusal in the pure order; {@code targetBlood} is null when the ingredients hold no bound blood. */
        private Optional<String> refusal(ServerPlayer performer, @Nullable UUID targetBlood) {
            MinecraftServer server = level.getServer();
            boolean witch = SeveranceRiteFactory.isWitchType(BloodlineAccess.get(performer).species());
            Optional<UUID> trapped = Imprisonment.trappedRoot(level, getPos());
            boolean ring = Imprisonment.ringClosed(level, getPos(), SpeciesConfig.ringRadius());
            if (targetBlood == null) {
                return LineRules.imprisonedRefusal(new LineRules.ImprisonedFacts(witch, LunarEclipse.active(server), Optional.empty(),
                        false, false, Optional.empty(), trapped, ring));
            }
            return LineRules.imprisonedRefusal(new LineRules.ImprisonedFacts(witch, LunarEclipse.active(server), Optional.of(targetBlood),
                    SireCall.player(server, targetBlood) != null, Lines.isOriginalId(server, targetBlood), Lines.rootOf(server, targetBlood),
                    trapped, ring));
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer performer)) {
                return cancel();
            }
            if (!SpeciesConfig.lineRitesEnabled()) {
                return refuse(performer, BrokenLineRiteFactory.RITES_OFF);
            }
            List<UUID> bloods = LineRites.bloodSources(params.consumedItems);
            UUID blood = bloods.isEmpty() ? null : bloods.get(0);
            Optional<String> refusal = refusal(performer, blood);
            if (refusal.isPresent()) {
                return refuse(performer, refusal.get());
            }
            caster = params.caster;
            target = blood;
            root = Lines.rootOf(level.getServer(), blood).orElseThrow();
            ticksLeft = SpeciesConfig.imprisonmentSeconds() * 20;
            return true;
        }

        @Override
        protected boolean onTick(RiteParams params) {
            ticksLeft--;
            if (ticksLeft > 0 && ticksLeft % CHECK_EVERY != 0) {
                return true;
            }
            ServerPlayer performer = findEntity(caster) instanceof ServerPlayer p ? p : null;
            Optional<String> failure = performer == null ? Optional.of(RING_BROKEN) : refusal(performer, target);
            if (failure.isPresent()) {
                if (performer != null) {
                    performer.displayClientMessage(Component.translatable(RING_BROKEN), true);
                }
                return false;
            }
            if (ticksLeft > 0) {
                return true;
            }
            MinecraftServer server = level.getServer();
            Lines.freeBranch(server, target, root);
            if (SireCall.player(server, target) instanceof ServerPlayer freed) {
                Lines.learn(freed, root);
                freed.displayClientMessage(Component.translatable(DONE, Lines.rootName(server, root)), true);
            }
            return false;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, ServerLevel level) {
            if (caster != null) {
                tag.put("Caster", NbtUtils.createUUID(caster));
                tag.put("Target", NbtUtils.createUUID(target));
                tag.put("Root", NbtUtils.createUUID(root));
                tag.putInt("TicksLeft", ticksLeft);
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, ServerLevel level) {
            if (tag.contains("Caster")) {
                caster = NbtUtils.loadUUID(tag.get("Caster"));
                target = NbtUtils.loadUUID(tag.get("Target"));
                root = NbtUtils.loadUUID(tag.get("Root"));
                ticksLeft = tag.getInt("TicksLeft");
            }
        }

        private boolean refuse(ServerPlayer performer, String key) {
            performer.displayClientMessage(Component.translatable(key), true);
            return cancel();
        }
    }
}
