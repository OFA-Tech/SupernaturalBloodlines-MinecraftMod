package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.Base64;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSealCodec;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The world's blood key (R210): 32 random bytes made the first time they are asked for and kept in the world's saved data. Only the server has it; a client,
 * a saved item copied into another world and a world whose key file was lost all fail to open a seal, so such a bottle counts as unstamped.
 */
public final class BloodKeys extends SavedData {
    private static final String NAME = "supernaturalbloodlines_blood_key";
    private static final String KEY = "key";
    private static volatile MinecraftServer server;
    private static volatile byte[] keyOverride;

    private byte[] key;

    private BloodKeys(byte[] key) {
        this.key = key;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> server = e.getServer());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> server = null);
    }

    /** The key of this world; null on a client, and before a server has started. */
    @Nullable
    public static byte[] key() {
        byte[] pinned = keyOverride;
        if (pinned != null) {
            return pinned;
        }
        MinecraftServer s = server;
        if (s == null) {
            return null;
        }
        return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(() -> fresh(), BloodKeys::load, null), NAME).key;
    }

    /** For tests: pins the key (another world, a lost key); null clears it. */
    @VisibleForTesting
    public static void overrideKey(@Nullable byte[] key) {
        keyOverride = key;
    }

    private static BloodKeys fresh() {
        BloodKeys k = new BloodKeys(BloodSealCodec.newKey());
        k.setDirty();
        return k;
    }

    private static BloodKeys load(CompoundTag tag, HolderLookup.Provider registries) {
        byte[] stored = tag.contains(KEY) ? Base64.getDecoder().decode(tag.getString(KEY)) : new byte[0];
        if (stored.length == BloodSealCodec.KEY_BYTES) {
            return new BloodKeys(stored);
        }
        MinecraftServer s = server;
        SupernaturalBloodlines.LOGGER.error("The blood key of world \"{}\" is {} bytes, not {}: a new one is made, and every sealed blood bottle of this world now reads as common vampire blood",
                s == null ? "?" : s.getWorldData().getLevelName(), stored.length, BloodSealCodec.KEY_BYTES);
        return fresh();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString(KEY, Base64.getEncoder().encodeToString(key));
        return tag;
    }
}
