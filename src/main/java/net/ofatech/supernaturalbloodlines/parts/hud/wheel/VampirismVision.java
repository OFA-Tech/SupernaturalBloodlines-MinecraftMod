package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Vampirism's vision key: it cycles the vampire's night vision, bat vision and so on through {@code VampirePlayer.switchVision()}, which is on the concrete class and not in
 * the API. We reach it by method handle once; if it is not there (Vampirism changed) there is one warning and no entry. Linked to Vampirism: reached only after
 * {@code ModList.isLoaded("vampirism")}.
 */
public final class VampirismVision {
    public static final ResourceLocation ID = WheelService.VISION_ID;
    public static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/unknown.png");

    private static MethodHandle handle;
    private static boolean resolved;
    private static boolean forcedMissing;
    private static int warnings;

    private VampirismVision() {
    }

    /** Test seam: make the handle fail to build, once, and forget what was resolved. */
    public static void forceMissingForTest(boolean missing) {
        forcedMissing = missing;
        resolved = false;
        handle = null;
        warnings = 0;
    }

    public static int warnings() {
        return warnings;
    }

    private static MethodHandle handle() {
        if (!resolved) {
            resolved = true;
            try {
                if (forcedMissing) {
                    throw new NoSuchMethodException("forced by a test");
                }
                Class<?> cls = Class.forName("de.teamlapen.vampirism.entity.player.vampire.VampirePlayer");
                handle = MethodHandles.publicLookup().findVirtual(cls, "switchVision", MethodType.methodType(void.class));
            } catch (ReflectiveOperationException | LinkageError e) {
                warnings++;
                SupernaturalBloodlines.LOGGER.warn("Wheel: Vampirism's vision switch was not found ({}); the Vision entry is off", e.toString());
            }
        }
        return handle;
    }

    public static boolean available() {
        return handle() != null;
    }

    /** The name of the vision that is on now, empty for none. */
    public static Component activeName(ServerPlayer p) {
        var v = VampirismAPI.vampirePlayer(p).getActiveVision();
        return v == null ? Component.empty() : Component.translatable(v.getTranslationKey());
    }

    private static java.lang.reflect.Field visionStatus;
    private static java.lang.reflect.Field unlockedVisions;
    private static boolean visionsResolved;

    /**
     * Whether the vampire has unlocked any vision (a skill gives each one): Vampirism keeps the set in {@code VampirePlayer.vision.unlockedVisions},
     * not in its API, so it is read by reflection, resolved once. If that is gone (Vampirism changed) the answer is true, so the entry is not lost and
     * the key itself answers as before; one warning.
     */
    public static boolean unlocked(ServerPlayer p) {
        if (!visionsResolved) {
            visionsResolved = true;
            try {
                Class<?> cls = Class.forName("de.teamlapen.vampirism.entity.player.vampire.VampirePlayer");
                visionStatus = cls.getDeclaredField("vision");
                visionStatus.setAccessible(true);
                unlockedVisions = visionStatus.getType().getDeclaredField("unlockedVisions");
                unlockedVisions.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                warnings++;
                visionStatus = null;
                SupernaturalBloodlines.LOGGER.warn("Wheel: Vampirism's unlocked visions were not found ({}); the Vision entry no longer checks them", e.toString());
            }
        }
        if (visionStatus == null) {
            return true;
        }
        try {
            Object status = visionStatus.get(VampirismAPI.vampirePlayer(p));
            return status != null && !((java.util.Collection<?>) unlockedVisions.get(status)).isEmpty();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    public static boolean active(ServerPlayer p) {
        return VampirismAPI.vampirePlayer(p).getActiveVision() != null;
    }

    /** Cycles the vision like the key does. @return false when it could not be run */
    public static boolean cycle(ServerPlayer p) {
        MethodHandle h = handle();
        if (h == null) {
            return false;
        }
        try {
            h.invoke(VampirismAPI.vampirePlayer(p));
            return true;
        } catch (Throwable t) {
            SupernaturalBloodlines.LOGGER.warn("Wheel: Vampirism's vision switch threw: {}", t.toString());
            return false;
        }
    }
}
