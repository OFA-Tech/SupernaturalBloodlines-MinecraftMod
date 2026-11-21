package net.ofatech.supernaturalbloodlines.parts.bosses.rite;

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
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.FrequencyRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;

/**
 * The forcing "frequency" rite (R190): the Valet Pike in the circle (it is given back) and a Hearing Device, on a full moon night, bring the Host of the Beast near the
 * caster if nothing has started (once; again only if the first Host died) or make the waiting Host change now. {@link FrequencyRules} says what happens; whatever it
 * says, the caster is told in a sentence, and a refusal gives the ingredients back.
 */
public record FrequencyRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "frequency");
    public static final MapCodec<FrequencyRiteFactory> CODEC = MapCodec.unit(FrequencyRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new FrequencyRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class FrequencyRite extends Rite {
        FrequencyRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer caster)) {
                return cancel();
            }
            BossIndex.Entry entry = BossIndex.get(level.getServer()).of(BossId.BEAST_OF_GEVAUDAN);
            FrequencyRules.Result result = FrequencyRules.act(entry.phase, entry.hostsMade, BossDirector.modsPresent(BossId.BEAST_OF_GEVAUDAN),
                    BossesConfig.enabled(BossId.BEAST_OF_GEVAUDAN), net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural.isWerewolf(caster),
                    level.dimension() == net.minecraft.world.level.Level.OVERWORLD, BossDirector.fullMoonNightNow(level.getServer().overworld()));
            boolean done = switch (result) {
                case MAKE_HOST -> BeastDirector.spawnHost(level, caster, null).isPresent();
                case FORCE_CHANGE -> BeastDirector.change(level.getServer().overworld());
                default -> false;
            };
            if (!done) {
                // a refusal tells the caster why and gives everything back: a Host with no ground to stand on has its own answer, one that could not be reached is "silent"
                FrequencyRules.Result why = result == FrequencyRules.Result.MAKE_HOST ? FrequencyRules.Result.NO_GROUND : result == FrequencyRules.Result.FORCE_CHANGE ? FrequencyRules.Result.BUSY : result;
                caster.displayClientMessage(Component.translatable(FrequencyRules.messageKey(why)), true);
                return cancel();
            }
            // the pike is a key, not an ingredient: it goes back to the circle
            params.consumedItems.stream().filter(s -> s.is(BossItems.VALET_PIKE.get())).findFirst()
                    .ifPresent(pike -> level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, pike.copy())));
            caster.displayClientMessage(Component.translatable(FrequencyRules.messageKey(result)), true);
            return false;
        }
    }
}
