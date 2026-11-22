package net.ofatech.supernaturalbloodlines.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp;

/**
 * A development-only screenshot driver for blood bottles (plan 18a), inert unless the JVM has {@code -Dsupernaturalbloodlines.devshots.blood=true}. In the
 * singleplayer world it makes the player a hybrid, fills the inventory with one bottle of each kind, then draws the tooltips of what the <em>client</em>
 * holds (so the picture proves what a client is told) into {@code <game dir>/screenshots/bloodshots/tooltips.png}. The hub's Slots page is photographed by
 * the hub driver in the same run ({@code -Dsupernaturalbloodlines.devshots=grimoire:loadout}), after this one has made the player a vampire.
 */
public final class BloodShots {
    private static final String PROPERTY = "supernaturalbloodlines.devshots.blood";
    private int ticks;
    private boolean done;

    private BloodShots() {
    }

    public static void registerIfRequested() {
        if (Boolean.getBoolean(PROPERTY)) {
            NeoForge.EVENT_BUS.addListener(new BloodShots()::tick);
            SupernaturalBloodlines.LOGGER.warn("BLOODSHOTS on (development only)");
        }
    }

    private static ItemStack bottle(Item item, BloodKind kind, String name) {
        ItemStack s = new ItemStack(item);
        BloodStamp.write(s, new BloodSource(UUID.randomUUID(), name, false, kind));
        return s;
    }

    private void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (done || mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        if (ticks == 5) {
            var server = mc.getSingleplayerServer();
            if (server == null) {
                done = true;
                return;
            }
            Item vampireBottle = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("vampirism:vampire_blood_bottle"));
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                BloodlineAccess.set(sp, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
                sp.getInventory().clearContent();
                sp.getInventory().add(new ItemStack(vampireBottle));
                sp.getInventory().add(bottle(vampireBottle, BloodKind.COMMON_VAMPIRE, "Advanced Vampire"));
                for (BloodKind k : List.of(BloodKind.ORIGINAL, BloodKind.HYBRID, BloodKind.ORIGINAL_HYBRID)) {
                    sp.getInventory().add(bottle(vampireBottle, k, "Elena Gilbert"));
                }
                sp.getInventory().add(bottle(SpeciesRegistries.ORIGINAL_BLOOD.get(), BloodKind.ORIGINAL, "Elena Gilbert"));
                sp.getInventory().add(bottle(SpeciesRegistries.HYBRID_BLOOD.get(), BloodKind.HYBRID, "Elena Gilbert"));
            });
        }
        if (ticks >= 50 && ticks < 90) {
            mc.getToasts().clear(); // the recipe toast of a fresh world would sit over the tooltips
        }
        if (ticks == 50) {
            mc.setScreen(new Panel());
        }
        if (ticks == 80) {
            new File(mc.gameDirectory, "screenshots/bloodshots").mkdirs();
            Screenshot.grab(mc.gameDirectory, "bloodshots/tooltips.png", mc.getMainRenderTarget(), c -> { });
            SupernaturalBloodlines.LOGGER.warn("BLOODSHOTS bloodshots/tooltips.png");
        }
        if (ticks == 90) {
            mc.setScreen(null);
            done = true;
        }
    }

    /** The tooltip of every stack the client holds, side by side. */
    private static final class Panel extends Screen {
        Panel() {
            super(Component.empty());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, width, height, 0xFF20202A);
            Minecraft mc = Minecraft.getInstance();
            var inv = mc.player.getInventory();
            int col = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty()) {
                    continue;
                }
                List<Component> lines = new ArrayList<>(s.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL));
                int x = 6 + (col % 3) * (width / 3);
                int y = 22 + (col / 3) * 60;
                g.renderItem(s, x, y - 20);
                g.renderTooltip(font, lines, Optional.empty(), x, y);
                col++;
            }
        }
    }
}
