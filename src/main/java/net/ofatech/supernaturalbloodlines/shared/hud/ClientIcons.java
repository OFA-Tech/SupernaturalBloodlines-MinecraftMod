package net.ofatech.supernaturalbloodlines.shared.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws an icon of any size (Tensura, Vampirism and Werewolves icons are not 16 x 16) into a square. The texture's size is read once from the resource and
 * cached, since {@code blit} needs it. Client only: call it from screens and HUD layers. Used by the HUD and by the hub's Loadout tab.
 */
public final class ClientIcons {
    private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

    private ClientIcons() {
    }

    /** Drawn for an icon that no resource pack has (a mod that does not ship the texture of one of its actions): a small rune, never the magenta square. */
    public static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "textures/gui/hud/unknown.png");
    private static final Map<ResourceLocation, Boolean> EXISTS = new HashMap<>();

    /** The icon itself when a resource has it, else {@link #UNKNOWN} (and one warning in the log per icon). */
    public static ResourceLocation resolve(ResourceLocation icon) {
        boolean ok = EXISTS.computeIfAbsent(icon, i -> {
            boolean has = Minecraft.getInstance().getResourceManager().getResource(i).isPresent();
            if (!has) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("HUD icon {} does not exist, drawing the unknown rune instead", i);
            }
            return has;
        });
        return ok ? icon : UNKNOWN;
    }

    /** Forget which icons exist (a resource reload can add or remove them). */
    public static void reload() {
        EXISTS.clear();
        SIZES.clear();
    }

    public static void draw(GuiGraphics g, ResourceLocation icon, int x, int y, int size, float alpha) {
        icon = resolve(icon);
        int[] d = SIZES.computeIfAbsent(icon, ClientIcons::measure);
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(icon, x, y, size, size, 0f, 0f, d[0], d[1], d[0], d[1]);
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static int[] measure(ResourceLocation icon) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(icon); NativeImage image = NativeImage.read(in)) {
            return new int[] {Math.max(1, image.getWidth()), Math.max(1, image.getHeight())};
        } catch (IOException | RuntimeException e) {
            return new int[] {16, 16};
        }
    }
}
