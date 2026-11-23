package net.ofatech.supernaturalbloodlines.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;

/**
 * A development-only screenshot driver for the dying of the turning (plan 18b), inert unless the JVM has {@code -Dsupernaturalbloodlines.devshots.dying=true}.
 * In the singleplayer world it puts vampire blood in the player and deals a lethal blow (the real path: the server catches it and sends the real payload, so the
 * real screen opens), photographs the dying screen, lets the 30 s run out and photographs the wake-up, into {@code <game dir>/screenshots/dyingshots/}. It
 * opens the world itself and leaves the game when done.
 */
public final class DyingShots {
    private static final String PROPERTY = "supernaturalbloodlines.devshots.dying";
    private int ticks;
    private boolean done;
    private boolean opened;

    private DyingShots() {
    }

    public static void registerIfRequested() {
        if (Boolean.getBoolean(PROPERTY)) {
            NeoForge.EVENT_BUS.addListener(new DyingShots()::tick);
            SupernaturalBloodlines.LOGGER.warn("DYINGSHOTS on (development only)");
        }
    }

    /** {@code -Dsupernaturalbloodlines.devshots.dying.scale=4}: the GUI scale of the pictures (0 leaves it as it is). */
    private static final int SCALE = Integer.getInteger(PROPERTY + ".scale", 0);

    /** {@code -Dsupernaturalbloodlines.devshots.dying.snapped=true}: photograph the "Out cold" screen of a snapped neck instead of the dying one. */
    private static final boolean SNAPPED = Boolean.getBoolean(PROPERTY + ".snapped");

    private void shot(Minecraft mc, String name) {
        String file = name + (SCALE > 0 ? "-s" + SCALE : "");
        new File(mc.gameDirectory, "screenshots/dyingshots").mkdirs();
        Screenshot.grab(mc.gameDirectory, "dyingshots/" + file + ".png", mc.getMainRenderTarget(), c -> { });
        SupernaturalBloodlines.LOGGER.warn("DYINGSHOTS dyingshots/{}.png ({}x{} GUI)", file, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (done) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            if (mc.screen instanceof TitleScreen && !opened) {
                opened = true;
                mc.createWorldOpenFlows().openWorld(System.getProperty("supernaturalbloodlines.devshots.world", "New World"), () -> mc.setScreen(new TitleScreen()));
            }
            return;
        }
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.getTutorial().setStep(TutorialSteps.NONE);
        ticks++;
        var server = mc.getSingleplayerServer();
        if (server == null) {
            done = true;
            return;
        }
        if (ticks == 1 && SCALE > 0) {
            mc.options.guiScale().set(SCALE);
            mc.resizeDisplay();
            if (mc.getWindow().getGuiScale() < SCALE) {
                mc.getWindow().setGuiScale(SCALE); // the game stops at what the window allows (3 at 1280x720); the picture is of the scale asked for
            }
        }
        if (ticks < 220) {
            mc.getToasts().clear(); // the recipe toast of a fresh world would sit over the pictures
            mc.gui.getChat().clearMessages(true);
            mc.options.particles().set(net.minecraft.client.ParticleStatus.MINIMAL);
        }
        if (ticks == 100 && SNAPPED) {
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                Downed.startSnapped(sp, 600);
            });
        }
        if (ticks == 140 && SNAPPED) {
            shot(mc, "snapped");
        }
        if (SNAPPED) {
            if (ticks == 150) {
                done = true;
                mc.stop();
            }
            return;
        }
        if (ticks == 100) {
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                                BloodlineAccess.set(sp, BloodlineState.NONE.toBuilder().vampireBloodUntil(sp.level().getGameTime() + 100000).build());
                de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(sp).leaveFaction(true); // only a mortal (no faction) can be turned
                sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // a creative player cannot be hurt
                sp.getAbilities().invulnerable = false;
                sp.invulnerableTime = 0;
                sp.hurt(sp.damageSources().generic(), 1000f);
            });
        }
        if (ticks == 140) {
            shot(mc, "dying");
        }
        if (ticks == 150) {
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                sp.getAbilities().invulnerable = true; // the dev world is full of mobs: the picture is of the wake-up, not of what comes after
                for (int i = 0; i < Downed.TICKS; i++) {
                    Downed.tick(sp);
                }
            });
        }
        if (ticks == 175) {
            shot(mc, "woke");
        }
        if (ticks == 190) {
            done = true;
            mc.stop();
        }
    }
}
