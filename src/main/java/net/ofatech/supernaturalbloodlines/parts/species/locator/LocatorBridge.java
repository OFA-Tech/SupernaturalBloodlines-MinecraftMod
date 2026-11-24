package net.ofatech.supernaturalbloodlines.parts.species.locator;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSealCodec;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.boss.CloakAccess;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodKeys;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp;
import net.ofatech.supernaturalbloodlines.shared.species.LocatorTargets;

/**
 * What the species part tells the locator (R315, R316): the settings, who is where, whose blood a sealed bottle holds, and the seal that keeps the target of a
 * compass from the client. The seal reuses the blood bottle's own key, so it opens only on this server.
 */
public final class LocatorBridge implements LocatorTargets.Source {
    private LocatorBridge() {
    }

    public static void register() {
        LocatorTargets.install(new LocatorBridge());
    }

    @Override
    public LocatorTargets.Settings settings() {
        return new LocatorTargets.Settings(SpeciesConfig.locatorEnabled(), SpeciesConfig.locatorMinutes(), SpeciesConfig.locatorRange(), SpeciesConfig.locatorBlur(),
                SpeciesConfig.locatorTellTarget());
    }

    /** A player online in any level (a test's fake player is in its level but not on the player list). */
    public static ServerPlayer player(MinecraftServer server, UUID id) {
        ServerPlayer listed = server.getPlayerList().getPlayer(id);
        if (listed != null) {
            return listed;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer p) {
                return p;
            }
        }
        return null;
    }

    @Override
    public Optional<LocatorTargets.Located> find(MinecraftServer server, UUID target) {
        ServerPlayer p = player(server, target);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(new LocatorTargets.Located(true, p.isAlive(), p.level().dimension().location(), p.position(), CloakAccess.cloaked(p), p.isCreative() || p.isSpectator()));
    }

    @Override
    public Optional<UUID> bloodOf(ItemStack bottle) {
        return BloodStamp.read(bottle).map(BloodSource::id);
    }

    @Override
    public Optional<String> seal(UUID target) {
        byte[] key = BloodKeys.key();
        return key == null ? Optional.empty() : Optional.of(BloodSealCodec.seal(key, new BloodSource(target, "", false, BloodKind.COMMON_VAMPIRE)));
    }

    @Override
    public Optional<UUID> open(String seal) {
        byte[] key = BloodKeys.key();
        return key == null ? Optional.empty() : BloodSealCodec.open(key, seal).map(BloodSource::id);
    }
}
