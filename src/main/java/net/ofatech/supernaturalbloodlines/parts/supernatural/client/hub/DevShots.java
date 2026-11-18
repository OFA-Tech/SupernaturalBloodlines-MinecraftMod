package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.DevShotsGate;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import org.lwjgl.glfw.GLFW;

/**
 * A development-only screenshot driver, so the hub can be looked at without anyone clicking. It exists only when the JVM is started with
 * {@code -Dsupernaturalbloodlines.devshots=<theme>:<page>,...} (themes grimoire, blood_moon, tensura; pages by name or all); it is not registered
 * otherwise, the server never touches it, and nothing here is reachable in a normal run.
 * <p>
 * Other properties: {@code .scales=2,4} (GUI scales, default the current one), {@code .size=1920x1080} (the window, default 1920x1080: GUI
 * scale 4 is then the 480 x 270 GUI the design is checked at), {@code .world=New World} (a world to open from the title screen) and
 * {@code .demo=true} (fill the hub with demo data: a pack, a family, learned skills, rite items; see {@link DevDemo}) and
 * {@code .quit=true} (leave the game when done). It sets the tutorial off, opens each hub page, waits some frames and grabs the frame with vanilla's
 * {@code Screenshot.grab} into {@code run/screenshots/devshots/<theme>-<page>-s<scale>.png}.
 */
public final class DevShots {
    public static final String PROPERTY = "supernaturalbloodlines.devshots";
    private static final int SETTLE_TICKS = 12;
    private static final int WORLD_TICKS = 100;

    /** @param hud null for a hub page; otherwise "normal", "alt" or "idle": the ability HUD in game with demo slots, in the look of {@code theme} */
    private record Shot(String theme, HubPage page, int scale, boolean menu, String hud, String variant) {
        Shot(String theme, HubPage page, int scale, boolean menu) {
            this(theme, page, scale, menu, null, null);
        }

        Shot(String theme, HubPage page, int scale, boolean menu, String hud) {
            this(theme, page, scale, menu, hud, null);
        }
    }

    private final List<Shot> shots;
    private final boolean quit;
    private final String world;
    private final int width;
    private final int height;
    private int index;
    private int wait;
    private int worldTicks;
    private boolean sized;
    private boolean lunarSet;
    private boolean opened;
    private boolean done;

    /** The demo mode: "true" (a populated hub), or "ghoul", "two" (two ladders) or "empty"; null when off. */
    static String demo() {
        String v = System.getProperty(PROPERTY + ".demo");
        return v == null || v.equalsIgnoreCase("false") ? null : v.toLowerCase(Locale.ROOT);
    }

    /** Set by a {@code bloodline_<state>} shot, so one run photographs every line state. */
    private static volatile String originOverride;

    static String origin() {
        if (originOverride != null) {
            return originOverride;
        }
        String v = System.getProperty(PROPERTY + ".origin");
        return v == null || v.isBlank() ? null : v.toLowerCase(Locale.ROOT);
    }

    private DevShots(List<Shot> shots, String world, boolean quit, int width, int height) {
        this.shots = shots;
        this.world = world;
        this.quit = quit;
        this.width = width;
        this.height = height;
    }

    /** Registers the driver when the property is set; a no-op otherwise. A bad flag is logged and the driver stays off: it must never stop the game from starting. */
    public static void registerIfRequested() {
        String spec = System.getProperty(PROPERTY);
        if (spec == null || spec.isBlank()) {
            return;
        }
        try {
            register(spec);
        } catch (RuntimeException bad) {
            SupernaturalBloodlines.LOGGER.error("DEVSHOTS: cannot read -D{}={} ({}); the screenshot driver is off", PROPERTY, spec, bad.toString());
        }
    }

    private static void register(String spec) {
        List<Integer> scales = new ArrayList<>();
        for (String s : System.getProperty(PROPERTY + ".scales", "0").split(",")) {
            scales.add(Integer.parseInt(s.trim()));
        }
        List<Shot> shots = new ArrayList<>();
        for (String item : spec.split(",")) {
            boolean menuShots = false;
            String[] parts = item.trim().split(":");
            if (parts[0].equalsIgnoreCase("hud")) {
                // hud:<look>[:alt|idle], for example hud:blood_moon:alt
                for (int scale : scales) {
                    shots.add(new Shot(parts[1].toLowerCase(Locale.ROOT), HubPage.PATH, scale, false, parts.length > 2 ? parts[2].toLowerCase(Locale.ROOT) : "normal"));
                }
                continue;
            }
            String theme = parts[0].toLowerCase(Locale.ROOT);
            HubClientConfig.ThemeId.valueOf(theme.toUpperCase(Locale.ROOT)); // a look that does not exist is a bad flag now, not a crash in the middle of the run
            List<HubPage> pages = new ArrayList<>();
            String variant = null;
            if (parts.length > 1 && (parts[1].toLowerCase(Locale.ROOT).startsWith("profile") || parts[1].equalsIgnoreCase("true_alpha_dialog") || parts[1].equalsIgnoreCase("pack_invite") || parts[1].toLowerCase(Locale.ROOT).startsWith("admin_"))) {
                // a route or a section of a page: profile, profile_beta, profile_stranger, profile_op, admin_debug, admin_speciesTensura, admin_search_max ...
                variant = parts[1].toLowerCase(Locale.ROOT).equals(parts[1]) ? parts[1] : parts[1];
                pages.add(variant.startsWith("admin_") ? HubPage.ADMIN : HubPage.PACK);
            } else if (parts.length > 1 && parts[1].toLowerCase(Locale.ROOT).startsWith("line_")) {
                // the Bloodline page in one line state: line_original, line_none, line_unknown, line_inline, line_inline_unknown, line_free, line_free_unknown
                variant = parts[1].toLowerCase(Locale.ROOT);
                pages.add(HubPage.BLOODLINE);
            } else if (parts.length < 2 || parts[1].equalsIgnoreCase("all")) {
                pages.addAll(List.of(HubPage.values()));
            } else if (parts[1].equalsIgnoreCase("menu")) {
                pages.add(HubPage.PATH); // the look menu open over the Path
                menuShots = true;
            } else {
                pages.add(HubPage.valueOf(parts[1].toUpperCase(Locale.ROOT)));
            }
            for (int scale : scales) {
                for (HubPage p : pages) {
                    shots.add(new Shot(theme, p, scale, menuShots, null, variant));
                }
            }
        }
        String[] size = System.getProperty(PROPERTY + ".size", "1920x1080").split("x");
        DevShots d = new DevShots(shots, System.getProperty(PROPERTY + ".world", "New World"), Boolean.getBoolean(PROPERTY + ".quit"),
                Integer.parseInt(size[0].trim()), Integer.parseInt(size[1].trim()));
        if (demo() != null) {
            HubDev.view(DevDemo::apply);
            HubDev.handsFull(true);
        }
        HubDev.noTensura(Boolean.getBoolean(PROPERTY + ".noTensura"));
        HubDev.quiet(true);
        NeoForge.EVENT_BUS.addListener(d::tick);
        SupernaturalBloodlines.LOGGER.warn("DEVSHOTS on: {} shots queued (development only)", shots.size());
    }

    /** Moves the pointer only in a focused window, and never in hidden mode (the owner's real cursor must not move). */
    private static void parkCursor(Minecraft mc, double x, double y) {
        long window = mc.getWindow().getWindow();
        boolean focused = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        if (DevShotsGate.mayMoveCursor(DevShotsGate.hidden(), focused)) {
            GLFW.glfwSetCursorPos(window, x, y);
        }
    }

    private void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (done) {
            return;
        }
        if (!lunarSet && Boolean.getBoolean(PROPERTY + ".lunar")) {
            lunarSet = true;
            DevDemo.lunar(); // not at registration: the celestials part installs its (empty) source after it
        }
        if (!sized && mc.getWindow() != null) {
            if (DevShotsGate.mayResizeWindow(DevShotsGate.hidden())) {
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), width, height);
            }
            sized = true;
        }
        if (mc.player == null || mc.level == null) {
            if (mc.screen instanceof TitleScreen && !opened && sized) {
                opened = true;
                mc.createWorldOpenFlows().openWorld(world, () -> mc.setScreen(new TitleScreen()));
            }
            return;
        }
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.getTutorial().setStep(TutorialSteps.NONE);
        if (worldTicks++ < WORLD_TICKS) {
            return;
        }
        if (index >= shots.size()) {
            done = true;
            net.ofatech.supernaturalbloodlines.shared.hud.DevHud.on = false;
            SupernaturalBloodlines.LOGGER.warn("DEVSHOTS done");
            if (quit) {
                mc.stop();
            }
            return;
        }
        Shot s = shots.get(index);
        if (wait == 0) {
            if (s.scale() > 0 && mc.options.guiScale().get() != s.scale()) {
                mc.options.guiScale().set(s.scale());
                mc.resizeDisplay();
            }
            HubClientConfig.ThemeId theme = HubClientConfig.ThemeId.valueOf(s.theme().toUpperCase(Locale.ROOT));
            HubClientConfig.setTheme(theme);
            mc.setScreen(null);
            net.ofatech.supernaturalbloodlines.shared.hud.DevHud.on = s.hud() != null;
            net.ofatech.supernaturalbloodlines.shared.hud.DevHud.alt = "alt".equals(s.hud());
            net.ofatech.supernaturalbloodlines.shared.hud.DevHud.idle = "idle".equals(s.hud());
            if (s.hud() != null) {
                parkCursor(mc, 1, 1);
                wait = 1;
                return;
            }
            parkCursor(mc, 1, 1); // a hover tooltip would cover the page
            originOverride = s.variant() != null && s.variant().startsWith("line_") ? s.variant().substring(5) : null;
            DevDemo.second = "profile_leader".equals(s.variant());
            HubClient.remember(s.page());
            HubClient.open(s.page());
            if (s.menu() && mc.screen instanceof HubScreen hs) {
                hs.devOpenMenu();
            }
            if (s.variant() != null && mc.screen instanceof HubScreen hs) {
                hs.devVariant(s.variant());
            }
            wait = 1;
            return;
        }
        // the pointer in the screen's corner, so no stray tooltip is in the picture
        parkCursor(mc, 0, 0);
        if (++wait < SETTLE_TICKS) {
            return;
        }
        File dir = new File(mc.gameDirectory, "screenshots/devshots");
        dir.mkdirs();
        String tag = demo() == null || demo().equals("true") ? "" : "-" + demo();
        String name = "devshots/" + (s.hud() != null ? "hud-" : "") + s.theme() + "-" + (s.hud() != null ? s.hud() : s.menu() ? "menu" : s.variant() != null ? s.variant() : s.page().name().toLowerCase(Locale.ROOT)) + tag + "-s" + mc.options.guiScale().get() + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), c -> { });
        SupernaturalBloodlines.LOGGER.warn("DEVSHOTS {}", name);
        index++;
        wait = 0;
    }
}
