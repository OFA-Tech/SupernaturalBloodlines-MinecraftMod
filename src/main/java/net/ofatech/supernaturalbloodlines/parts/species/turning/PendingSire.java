package net.ofatech.supernaturalbloodlines.parts.species.turning;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;

/**
 * The Original whose blood is in someone is their <em>pending</em> sire (see {@code BloodDrink}); it becomes the sire when a turning completes (the rules do that,
 * from {@code BloodlineState.pendingSire}). These two steps bracket the completion: {@link #settle} decides which source counts and drops one whose line is gone,
 * {@link #completed} tells the player when they now belong to a line they have not discovered.
 */
public final class PendingSire {
    public static final String LINE_GONE = "supernaturalbloodlines.blood.line_gone";
    public static final String HIDDEN_LINE = "supernaturalbloodlines.blood.hidden_line";

    private PendingSire() {
    }

    /**
     * Before a turning completes. {@code lateDrink}: Original's blood drunk after the death, while the effect is still on, counts (the last drink wins);
     * the hybrid transition passes false (a second dose changes nothing). A source whose line is dead is dropped, and the player is told.
     */
    public static void settle(ServerPlayer p, boolean lateDrink) {
        BloodlineState s = BloodlineAccess.get(p);
        UUID source = s.pendingSire();
        if (lateDrink && p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && p.hasData(SpeciesRegistries.BLOOD_SOURCE)) {
            source = p.getData(SpeciesRegistries.BLOOD_SOURCE);
        }
        if (source != null && LineIndex.get(p.server).isDead(source)) {
            source = null;
            p.sendSystemMessage(Component.translatable(LINE_GONE));
        }
        if (!Objects.equals(source, s.pendingSire())) {
            BloodlineAccess.set(p, s.toBuilder().pendingSire(source).build());
        }
    }

    /** After a turning completed: {@code settled} is the state {@link #settle} left. One line when they now have a sire whose line they have not discovered. */
    public static void completed(ServerPlayer p, BloodlineState settled) {
        UUID pending = settled.pendingSire();
        if (pending != null && pending.equals(BloodlineAccess.get(p).sire()) && !pending.equals(settled.sire())
                && Lines.rootOf(p.server, p.getUUID()).isPresent() && Lines.visibleRoot(p).isEmpty()) {
            p.sendSystemMessage(Component.translatable(HIDDEN_LINE));
        }
    }
}
