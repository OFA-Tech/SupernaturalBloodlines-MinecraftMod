package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.favouriteless.enchanted.api.curses.Curse;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/** Every 20 ticks takes 0.5% of the player's max magicules per strength level (at least 1), never below 0. */
public final class MagiculeDrainCurse implements Curse {
    private static final int INTERVAL = 20;

    // counts our own calls (once per server tick): Enchanted 4.2.7 advances the `age` it passes by 2 a tick
    // and does not restore it on load, so it cannot time a period. Not saved: a reload just restarts the period.
    private int ticks;

    @Override
    public void tick(ServerPlayer target, int strength, long age) {
        if (++ticks < INTERVAL) {
            return;
        }
        ticks = 0;
        IExistence existence = TensuraStorages.getExistenceFrom(target);
        double current = existence.getMagicule();
        if (current <= 0) {
            return;
        }
        AttributeInstance max = target.getAttribute(TensuraAttributes.MAX_MAGICULE);
        double drain = Math.max(1.0, (max == null ? 0 : max.getValue()) * 0.005 * (strength + 1));
        existence.setMagicule(Math.max(0, current - drain));
        existence.markDirty();
    }

    @Override
    public Type<?> type() {
        return Curses.MAGICULE_DRAIN;
    }
}
