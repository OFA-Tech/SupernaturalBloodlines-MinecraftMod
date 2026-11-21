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
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubOpenPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotRequestPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/**
 * Client only: opens the hub, keeps the last snapshot the server sent (asked for on every open, never kept in sync), and brings the hub
 * back after a launcher's screen is closed ({@link HubReturn}).
 */
public final class HubClient {
    private static volatile HubSnapshotPayload snapshot = HubSnapshotPayload.EMPTY;
    private static HubPage lastPage = HubPage.HOME;
    private static final HubReturn RETURN = new HubReturn();

    private HubClient() {
    }

    /**
     * The Alpha Trial was refused for an open True Alpha path (R147): the hub opens on the Rites page and shows the warning; "take the spark" answers the
     * server, which then lets the rite start. The screen is the same one the Rites page and the profile use ({@link HubScreen#warnTrueAlpha}).
     */
    static void onTrialWarning() {
        open(HubPage.RITES);
        if (Minecraft.getInstance().screen instanceof HubScreen s) {
            // the answer goes to the server, and the hub closes: the server's reply is a chat line, which a screen would hide
            s.warnTrueAlpha(() -> {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(net.ofatech.supernaturalbloodlines.core.network.TrialConfirmPayload.INSTANCE);
                Minecraft.getInstance().setScreen(null);
            });
        }
    }

    public static void register() {
        net.ofatech.supernaturalbloodlines.shared.hub.TrialWarnHook.set(HubClient::onTrialWarning);
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.TrackHud.register();
        net.ofatech.supernaturalbloodlines.shared.hud.HudStyle.install(() -> switch (HubClientConfig.theme()) {
            case BLOOD_MOON -> net.ofatech.supernaturalbloodlines.shared.hud.HudStyle.Style.BLOOD_MOON;
            case TENSURA -> net.ofatech.supernaturalbloodlines.shared.hud.HudStyle.Style.TENSURA;
            default -> net.ofatech.supernaturalbloodlines.shared.hud.HudStyle.Style.GRIMOIRE;
        });
        DevShots.registerIfRequested(); // development only: a no-op unless the JVM property is set
        // forget the last snapshot when leaving a world, so another world never shows its names
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            snapshot = HubSnapshotPayload.EMPTY;
            RETURN.clear();
            RequestsClient.clear();
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
        // a name in a request message links here: the hub opens on that person (client only, the server is never asked to run it)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RegisterClientCommandsEvent e) -> e.getDispatcher().register(
                net.minecraft.commands.Commands.literal(net.ofatech.supernaturalbloodlines.shared.supernatural.ProfileLink.COMMAND)
                        .then(net.minecraft.commands.Commands.argument("id", net.minecraft.commands.arguments.UuidArgument.uuid())
                                .then(net.minecraft.commands.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            openProfile(net.minecraft.commands.arguments.UuidArgument.getUuid(ctx, "id"), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name"));
                                            return 1;
                                        })))));
        HubHooks.setHubOpener(HubClient::open);
        HubHooks.setCurrentTheme(HubClient::themeFor);
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
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.book.EnchantedBook.register();
        }
    }

    /**
     * The theme the player has chosen (or the one named by {@code id}, for screenshots), as the hub itself shows it: a theme that is not there, or is of the
     * wrong type, is the Grimoire. Other parts' screens ask for it through {@link HubHooks#currentTheme}.
     */
    static java.util.Optional<net.ofatech.supernaturalbloodlines.shared.hub.HubTheme> themeFor(String id) {
        HubClientConfig.ThemeId wanted = id == null ? HubClientConfig.theme() : HubClientConfig.ThemeId.valueOf(id.toUpperCase(java.util.Locale.ROOT));
        HubClientConfig.ThemeId shown = HubClientConfig.resolve(wanted, other -> HubHooks.theme(other).isPresent());
        return java.util.Optional.of(HubHooks.theme(shown.id()).map(t -> t.factory().get()).orElseGet(GrimoireTheme::new));
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

    private static boolean openedThisSession;

    /** Home the first time in a session and whenever a request waits, else the page the player left (the Settings option "open on" can force either). */
    public static void open() {
        boolean waiting = !RequestsClient.requests().isEmpty();
        boolean home = HubClientConfig.openOnHome() ? !openedThisSession || waiting : waiting;
        openedThisSession = true;
        open(home ? HubPage.HOME : lastPage);
    }

    /** The hub on Home, then on that person's Profile (from a chat name). */
    static void openProfile(java.util.UUID id, String name) {
        open(HubPage.HOME);
        if (Minecraft.getInstance().screen instanceof HubScreen s) {
            s.openProfile(id, name);
        }
    }

    /** The "answer request" key: Home, with the first request focused. */
    public static void openRequests() {
        openedThisSession = true;
        open(HubPage.HOME);
    }

    /** Back in the main menu: the next world starts a new session. */
    public static void newSession() {
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.RitesPage.devSelect(null); // a dev-screenshot selection never outlives its world
        openedThisSession = false;
        lastPage = HubPage.HOME;
    }

    /** Asks for a snapshot again: the server takes that as "still open" (see {@code HubPresence}). */
    static void keepAlive() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && DevShots.demo() == null && connection.hasChannel(HubSnapshotRequestPayload.TYPE)) {
            PacketDistributor.sendToServer(HubSnapshotRequestPayload.INSTANCE);
        }
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
        sendOpen(true);
    }

    /** Runs one of our commands by its row id (see {@code AdminCommands}); the server rebuilds the typed command, Brigadier checks it, and the reply comes back. */
    public static void runCommand(String rowId, java.util.List<String> args) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandPayload.TYPE)) {
            PacketDistributor.sendToServer(new net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandPayload(rowId, args));
        }
    }

    /** Asks for a person's profile; the OP view only goes out from Admin and the server answers it only to permission level 2. */
    static void requestProfile(java.util.UUID id, boolean op) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || DevShots.demo() != null) { // the dev screenshots show made-up people: nothing is asked of the server
            return;
        }
        if (op && connection.hasChannel(net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminProfileRequestPayload.TYPE)) {
            PacketDistributor.sendToServer(new net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminProfileRequestPayload(id));
        } else if (!op && connection.hasChannel(net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfileRequestPayload.TYPE)) {
            PacketDistributor.sendToServer(new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfileRequestPayload(id));
        }
    }

    /** Tells the server whether the hub is open, so a request made to this player waits longer while they can answer it. */
    static void sendOpen(boolean open) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(HubOpenPayload.TYPE)) {
            PacketDistributor.sendToServer(new HubOpenPayload(open));
        }
    }
}
