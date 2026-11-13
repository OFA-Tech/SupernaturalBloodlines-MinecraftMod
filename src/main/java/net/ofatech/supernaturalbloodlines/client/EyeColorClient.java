package net.ofatech.supernaturalbloodlines.client;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload.Color;
import org.joml.Vector3f;

/**
 * Glowing werewolf eyes (R91): two dust particles at eye height every 10 ticks while the wolf is in a Werewolves form or
 * roaring. Client only: nothing but the payload handler in ModNetwork refers to this class. The eye colour is what the
 * server last said for each player; a render layer on Werewolves' model would do this better and is not done (stretch).
 */
public final class EyeColorClient {
    private static final int PERIOD = 10;
    private static final Map<Integer, Color> COLORS = new HashMap<>();
    // extra reasons to glow, besides a Werewolves form: the Alpha Roar (task 4) adds its own
    private static final List<Predicate<Player>> GLOW = new CopyOnWriteArrayList<>();
    private static boolean listening;

    private EyeColorClient() {}

    public static void set(int playerId, Color color) {
        COLORS.put(playerId, color);
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
            NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> COLORS.clear());
        }
    }

    /** Hook for the Roar: {@code roaring} says whether this player glows right now. */
    public static void addGlowCondition(Predicate<Player> roaring) {
        GLOW.add(roaring);
    }

    static Vector3f rgb(Color c) {
        return switch (c) {
            case GOLD -> new Vector3f(1.0f, 0.78f, 0.1f);
            case BLUE -> new Vector3f(0.25f, 0.55f, 1.0f);
            case RED -> new Vector3f(0.95f, 0.1f, 0.1f);
        };
    }

    private static boolean inWerewolfForm(Player p) {
        return ModList.get().isLoaded("werewolves") && WerewolvesForm.transformed(p);
    }

    private static boolean glows(Player p) {
        return inWerewolfForm(p) || GLOW.stream().anyMatch(c -> c.test(p));
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || COLORS.isEmpty() || level.getGameTime() % PERIOD != 0) {
            return;
        }
        COLORS.forEach((id, color) -> {
            Entity e = level.getEntity(id);
            if (e instanceof Player p && p.isAlive() && !p.isInvisible() && glows(p)) {
                DustParticleOptions dust = new DustParticleOptions(rgb(color), 1.0f);
                Vec3 eye = p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYHeadRot()).scale(0.22));
                Vec3 side = Vec3.directionFromRotation(0, p.getYHeadRot() + 90).scale(0.1);
                level.addParticle(dust, eye.x + side.x, eye.y, eye.z + side.z, 0, 0, 0);
                level.addParticle(dust, eye.x - side.x, eye.y, eye.z - side.z, 0, 0, 0);
            }
        });
    }

    /** Own class so Werewolves is only linked when it is installed. */
    private static final class WerewolvesForm {
        static boolean transformed(Player p) {
            return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer()
                    .filter(f -> f instanceof IWerewolfPlayer w && w.getForm().isTransformed()).isPresent();
        }
    }
}
