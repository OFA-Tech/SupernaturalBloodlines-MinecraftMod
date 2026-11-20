package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the game bus, on the server thread, by the Rite of the Cure when it has made The Cure (U2): where (the circle's centre) and who cast it.
 * The one signal Rebekah answers; a Cure that was given or dropped is not an event. Not cancellable.
 */
public class CureMadeEvent extends Event {
    private final ServerLevel level;
    private final BlockPos pos;
    @Nullable
    private final UUID caster;

    public CureMadeEvent(ServerLevel level, BlockPos pos, @Nullable UUID caster) {
        this.level = level;
        this.pos = pos.immutable();
        this.caster = caster;
    }

    public ServerLevel level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public UUID caster() {
        return caster;
    }
}