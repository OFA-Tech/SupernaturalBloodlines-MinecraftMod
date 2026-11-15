package net.ofatech.supernaturalbloodlines.parts.hud.bite;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IBloodStats;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.race.RaceUtils;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.ofatech.supernaturalbloodlines.core.hud.TensuraFeedRules;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;

/**
 * Tensura's vampire race ability, used on a creature by a Vampirism vampire, fills Vampirism blood too (plan 13c). Linked to Tensura and Vampirism:
 * registered only when both are loaded. The route is events, no mixin: ManasCore's {@code RaceEvents.ACTIVATE_ABILITY} marks the player for this
 * tick, and the {@code tensura:blood_drain} damage that Tensura's feed deals to the victim in the same tick is what triggers the credit. Blood is taken
 * from the victim's Vampirism blood (a drained victim gives nothing, as in the hybrid feed) and given with Vampirism's own {@code drinkBlood}, so every
 * other Vampirism rule (and our turning hook on {@code PlayerDrinkBloodEvent}) sees it as a drink. Switch: {@code tensura_feed_fills_blood}.
 */
public final class TensuraFeed {
    /** The game time of the tick in which a player's race ability was activated. */
    private static final Map<UUID, Long> ACTIVATED = new ConcurrentHashMap<>();

    private TensuraFeed() {
    }

    public static void register() {
        RaceEvents.ACTIVATE_ABILITY.register((instance, entity) -> {
            if (entity instanceof ServerPlayer p) {
                ACTIVATED.put(p.getUUID(), p.serverLevel().getGameTime());
            }
            return EventResult.pass();
        });
        NeoForge.EVENT_BUS.addListener(TensuraFeed::onDamage);
    }

    private static void onDamage(LivingDamageEvent.Post e) {
        if (!HudConfig.tensuraFeedFillsBlood() || !e.getSource().is(TensuraDamageTypes.BLOOD_DRAIN) || !(e.getSource().getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Long when = ACTIVATED.get(p.getUUID());
        if (when == null || when != p.serverLevel().getGameTime() || VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != VReference.VAMPIRE_FACTION) {
            return;
        }
        LivingEntity victim = e.getEntity();
        int credit = TensuraFeedRules.credit(VampirismBite.blood(victim), RaceUtils.isBloodless(victim));
        if (credit > 0) {
            VampirismBite.drain(victim, credit);
            VampirismAPI.vampirePlayer(p).drinkBlood(credit, IBloodStats.LOW_SATURATION, new Source(victim));
        }
    }

    public static void forget(ServerPlayer p) {
        ACTIVATED.remove(p.getUUID());
    }

    private record Source(LivingEntity victim) implements IDrinkBloodContext {
        @Override
        public Optional<LivingEntity> getEntity() {
            return Optional.of(victim);
        }

        @Override
        public Optional<ItemStack> getStack() {
            return Optional.empty();
        }

        @Override
        public Optional<BlockState> getBlockState() {
            return Optional.empty();
        }

        @Override
        public Optional<BlockPos> getBlockPos() {
            return Optional.empty();
        }
    }
}
