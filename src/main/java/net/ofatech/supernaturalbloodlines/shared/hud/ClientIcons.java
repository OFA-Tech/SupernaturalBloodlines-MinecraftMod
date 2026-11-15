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

    public static void draw(GuiGraphics g, ResourceLocation icon, int x, int y, int size, float alpha) {
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
