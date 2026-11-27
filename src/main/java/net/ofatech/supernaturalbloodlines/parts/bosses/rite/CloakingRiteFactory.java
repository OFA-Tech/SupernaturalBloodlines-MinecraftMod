package net.ofatech.supernaturalbloodlines.parts.bosses.rite;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.boss.Cloak;
import net.ofatech.supernaturalbloodlines.core.boss.CloakAccess;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.item.KolGrimoireItem;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;

/**
 * The Rite of Cloaking (R172): a witch-type performer, with a Kol's Grimoire that has been read among the ingredients, is hidden from the Vampire Compass and
 * from a Hunter of the Five's sense for {@code kol_cloak_minutes}. The grimoire is given back. Otherwise the caster is told why and Enchanted gives every
 * ingredient back.
 */
public record CloakingRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "cloaking");
    public static final String NOT_WITCH = "supernaturalbloodlines.refusal.cloak_not_witch";
    public static final String UNREAD = "supernaturalbloodlines.refusal.cloak_unread";
    public static final String DONE = "supernaturalbloodlines.rite.cloak_done";
    public static final MapCodec<CloakingRiteFactory> CODEC = MapCodec.unit(CloakingRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new CloakingRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class CloakingRite extends Rite {
        CloakingRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer caster)) {
                return cancel(); // silent: the performer left
            }
            if (!SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species())) {
                return refuse(caster, NOT_WITCH);
            }
            ItemStack grimoire = params.consumedItems.stream().filter(s -> s.is(BossItems.KOL_GRIMOIRE.get())).findFirst().orElse(ItemStack.EMPTY);
            if (grimoire.isEmpty() || !KolGrimoireItem.isRead(grimoire)) {
                return refuse(caster, UNREAD);
            }
            long now = caster.level().getGameTime();
            CloakAccess.set(caster, Cloak.renewed(CloakAccess.until(caster), now, BossesConfig.kolCloakMinutes()));
            // the grimoire is a key, not an ingredient: it goes back to the circle
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, grimoire.copy()));
            RiteTell.say(caster, DONE, BossesConfig.kolCloakMinutes());
            return false;
        }

        private boolean refuse(ServerPlayer caster, String key) {
            RiteTell.refuse(caster, key);
            return cancel();
        }
    }
}
