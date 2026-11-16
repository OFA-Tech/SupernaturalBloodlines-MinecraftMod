package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotRequestPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/**
 * Client only: opens the hub, keeps the last snapshot the server sent (asked for on every open, never kept in sync), and brings the hub
 * back after a launcher's screen is closed ({@link HubReturn}).
 */
public final class HubClient {
    private static volatile HubSnapshotPayload snapshot = HubSnapshotPayload.EMPTY;
    private static HubPage lastPage = HubPage.PATH;
    private static final HubReturn RETURN = new HubReturn();

    private HubClient() {
    }

    public static void register() {
        // forget the last snapshot when leaving a world, so another world never shows its names
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            snapshot = HubSnapshotPayload.EMPTY;
            RETURN.clear();
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (e.getNewScreen() instanceof DeathScreen) {
                RETURN.clear();
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            HubReturn.OnScreen now = mc.screen == null ? HubReturn.OnScreen.NONE
                    : mc.screen instanceof HubScreen ? HubReturn.OnScreen.HUB : HubReturn.OnScreen.OTHER;
            RETURN.tick(now, mc.screen == null ? null : mc.screen.getClass().getName()).ifPresent(HubClient::open);
        });
        HubHooks.setHubOpener(HubClient::open);
        HubHooks.addTheme(new HubHooks.Theme(HubClientConfig.ThemeId.GRIMOIRE.id(), () -> true, GrimoireTheme::new));
        HubHooks.addTheme(new HubHooks.Theme(HubClientConfig.ThemeId.BLOOD_MOON.id(), () -> true, BloodMoonTheme::new));
        HubHooks.addLauncher(new HubHooks.Launcher("vampirism", "V", 0xFFf4c9c9, Component.translatable("supernaturalbloodlines.hub.launcher.vampirism"),
                VAMPIRISM_SCREENS, () -> factionNamespace().equals("vampirism"), HubClient::openSkillScreen));
        if (ModList.get().isLoaded("werewolves")) {
            HubHooks.addLauncher(new HubHooks.Launcher("werewolves", "W", 0xFFf0c890, Component.translatable("supernaturalbloodlines.hub.launcher.werewolves"),
                    VAMPIRISM_SCREENS, () -> factionNamespace().equals("werewolves"), HubClient::openSkillScreen));
        }
        if (ModList.get().isLoaded("enchanted")) {
            HubModel.ritesCountWhen(() -> net.ofatech.supernaturalbloodlines.parts.supernatural.task.RiteSignal.countable());
        }
        if (ModList.get().isLoaded("enchanted") && ModList.get().isLoaded("modopedia")) {
            HubHooks.addLauncher(new HubHooks.Launcher("enchanted", "E", 0xFFb7f0c4, Component.translatable("supernaturalbloodlines.hub.launcher.enchanted"),
                    "net.favouriteless.", () -> true, HubClient::openEnchantedBook));
        }
    }

    // Werewolves draws its skill tree with Vampirism's own skills screen, so both seals open it and both own its package
    private static final String VAMPIRISM_SCREENS = "de.teamlapen.";

    private static String factionNamespace() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "";
        }
        var faction = VampirismAPI.factionPlayerHandler(mc.player).getCurrentFaction();
        return faction == null ? "" : faction.getID().getNamespace();
    }

    /**
     * Vampirism's skills screen for the current faction (Werewolves uses the same one), as its own key opens it.
     * shortcut: by reflection, because only Vampirism's api jar is on our compile classpath; switch to a direct call if the full jar is added.
     */
    private static void openSkillScreen() {
        Minecraft mc = Minecraft.getInstance();
        var factionPlayer = VampirismAPI.factionPlayerHandler(mc.player).getCurrentFactionPlayer().orElseThrow();
        try {
            Class<?> screen = Class.forName("de.teamlapen.vampirism.client.gui.screens.skills.SkillsScreen");
            mc.setScreen((net.minecraft.client.gui.screens.Screen) screen
                    .getConstructor(de.teamlapen.vampirism.api.entity.player.IFactionPlayer.class, net.minecraft.client.gui.screens.Screen.class)
                    .newInstance(factionPlayer, null));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Vampirism's SkillsScreen(IFactionPlayer, Screen) is not there", e);
        }
    }

    /**
     * Enchanted's book, The Art of Witchcraft, is a Modopedia book: opened the way its item opens it.
     * shortcut: by reflection, because Modopedia is not on our compile classpath; add it as compileOnly if more of its API is needed.
     */
    private static void openEnchantedBook() {
        try {
            Class.forName("net.favouriteless.modopedia.client.BookOpenHandler").getMethod("tryOpenBook", ResourceLocation.class)
                    .invoke(null, ResourceLocation.fromNamespaceAndPath("enchanted", "art_of_witchcraft"));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Modopedia's BookOpenHandler.tryOpenBook is not there", e);
        }
    }

    public static void receive(HubSnapshotPayload payload) {
        snapshot = payload;
    }

    public static HubSnapshotPayload snapshot() {
        return snapshot;
    }

    static void remember(HubPage page) {
        lastPage = page;
    }

    /** Opens another mod's screen from the hub; Esc out of it brings the hub back on {@code page}. */
    static void launch(HubHooks.Launcher launcher, HubPage page) {
        RETURN.launched(page, launcher.screenPackage());
        try {
            launcher.open().run();
        } catch (RuntimeException | LinkageError e) {
            // a launcher reaches into another mod: if that mod changed, say so once in the log and stay in the hub
            SupernaturalBloodlines.LOGGER.warn("Hub launcher {} failed", launcher.id(), e);
            RETURN.clear();
        }
    }

    public static void open() {
        open(lastPage);
    }

    static void open(HubPage page) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        if (mc.getConnection().hasChannel(HubSnapshotRequestPayload.TYPE)) {
            PacketDistributor.sendToServer(HubSnapshotRequestPayload.INSTANCE);
        }
        lastPage = page;
        mc.setScreen(new HubScreen(page));
    }
}
