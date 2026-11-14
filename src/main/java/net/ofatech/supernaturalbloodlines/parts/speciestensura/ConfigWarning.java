package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Config entries that could not be used (layers, rank_skills) are logged at ERROR with their text when the config loads
 * ({@link SpeciesTensuraConfig#rejectedEntries}), and an op (permission level 2 or more) is told once per login.
 */
public final class ConfigWarning {
    public static final String KEY = "supernaturalbloodlines.species_tensura.bad_config";
    private static final int OP_LEVEL = 2;

    private ConfigWarning() {}

    static void register(IEventBus modBus) {
        modBus.addListener((ModConfigEvent.Loading e) -> validate(e));
        modBus.addListener((ModConfigEvent.Reloading e) -> validate(e));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                notifyOp(p, SpeciesTensuraConfig.rejectedEntries());
            }
        });
    }

    private static void validate(ModConfigEvent e) {
        if (e.getConfig().getSpec() == SpeciesTensuraConfig.SPEC) {
            SpeciesTensuraConfig.rejectedEntries();
        }
    }

    /** Tells the player how many entries were ignored, if there are any and they are an op. Returns whether it did. */
    public static boolean notifyOp(ServerPlayer p, List<String> rejected) {
        if (rejected.isEmpty() || !p.hasPermissions(OP_LEVEL)) {
            return false;
        }
        p.sendSystemMessage(Component.translatable(KEY, rejected.size()));
        return true;
    }
}
