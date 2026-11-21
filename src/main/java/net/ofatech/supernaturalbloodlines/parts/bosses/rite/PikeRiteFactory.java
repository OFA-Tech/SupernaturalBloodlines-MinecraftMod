package net.ofatech.supernaturalbloodlines.parts.bosses.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.boss.PikeRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/**
 * The Rite of the Valet Pike (R188): a witch-type performer, on a full moon night, turns iron, wolfsbane and mountain ash dust and the bottled blood of somebody
 * strong into the Valet Pike. How strong the blood must be is {@link PikeRules}: a plain player's is not enough. Every refusal reads the same ("the blood will not
 * take") and costs the same, so the rite cannot be used to find out what somebody is.
 */
public record PikeRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "valet_pike");
    public static final String NOT_WITCH = "supernaturalbloodlines.refusal.pike_not_witch";
    public static final String WEAK = "supernaturalbloodlines.refusal.pike_weak";
    public static final String DONE = "supernaturalbloodlines.rite.pike_done";
    public static final MapCodec<PikeRiteFactory> CODEC = MapCodec.unit(PikeRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new PikeRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(BossItems.VALET_PIKE.get()));
    }

    /** Whom the blood belonged to: an NPC Original, a player who is here, or nobody who counts. */
    public static PikeRules.Giver giver(MinecraftServer server, @Nullable UUID source) {
        if (source == null) {
            return PikeRules.Giver.of(PikeRules.Kind.ABSENT);
        }
        if (NpcOriginal.byUuid(source).isPresent()) {
            return PikeRules.Giver.of(PikeRules.Kind.NPC_ORIGINAL);
        }
        ServerPlayer p = SireCall.player(server, source);
        if (p == null) {
            return PikeRules.Giver.of(PikeRules.Kind.ABSENT);
        }
        var state = SupernaturalAccess.get(p);
        return new PikeRules.Giver(PikeRules.Kind.PLAYER, BloodlineAccess.get(p).species(), state.rung(SupernaturalRace.VAMPIRE), state.rung(SupernaturalRace.WEREWOLF),
                PackAccess.isAlpha(PackAccess.wolf(p).rank()));
    }

    private static final class PikeRite extends Rite {
        PikeRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer caster)) {
                return cancel();
            }
            if (!SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species())) {
                caster.displayClientMessage(Component.translatable(NOT_WITCH), true);
                return cancel();
            }
            ItemStack blood = params.consumedItems.stream().filter(s -> s.is(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.SIRES_BLOOD.get())).findFirst()
                    .orElse(ItemStack.EMPTY);
            if (!PikeRules.qualifies(giver(level.getServer(), blood.isEmpty() ? null : SiresBloodItem.sourceOf(blood)))) {
                // the same message and the same cost whoever the blood was: nothing is learnt about the giver
                caster.displayClientMessage(Component.translatable(WEAK), true);
                return false;
            }
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, new ItemStack(BossItems.VALET_PIKE.get())));
            caster.displayClientMessage(Component.translatable(DONE), true);
            return false;
        }
    }
}
