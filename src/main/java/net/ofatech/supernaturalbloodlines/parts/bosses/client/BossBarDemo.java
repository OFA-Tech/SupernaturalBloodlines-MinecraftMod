package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload;

/**
 * Development only (the boss bar screenshots): puts bars of ours on the client without a boss, by the name a shot gave
 * ({@code klaus_human}, {@code deucalion_shifting}, {@code peter_beast}, {@code beast_host}, {@code mikael}, {@code stack3}, {@code stack3_other}).
 * Client only.
 */
final class BossBarDemo {
    private static final List<UUID> SHOWN = new ArrayList<>();

    private BossBarDemo() {
    }

    static void show(Minecraft mc, String want, String previous) {
        var events = mc.gui.getBossOverlay().events;
        for (UUID id : SHOWN) {
            events.remove(id);
            BossBarClient.forget(id);
        }
        SHOWN.clear();
        if (want == null) {
            return;
        }
        if (want.startsWith("stack3")) {
            add(mc, BossId.KLAUS, BossBarForms.shifter(BossForm.HUMAN), false, 1640, 2000);
            add(mc, BossId.DEUCALION, BossBarForms.shifter(BossForm.SHIFTING), false, 1100, 2000);
            add(mc, BossId.BEAST_OF_GEVAUDAN, BossBarForms.host(1), false, 620, 2000);
            if (want.equals("stack3_other")) {
                UUID other = UUID.randomUUID();
                events.put(other, new LerpingBossEvent(other, Component.literal("Another mod's boss"), 0.6f, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS, false, false, false));
                SHOWN.add(other);
            }
            return;
        }
        String[] parts = want.toLowerCase(Locale.ROOT).split("_", 2);
        BossId boss = switch (parts[0]) {
            case "klaus" -> BossId.KLAUS;
            case "deucalion" -> BossId.DEUCALION;
            case "peter" -> BossId.PETER_HALE;
            case "beast" -> BossId.BEAST_OF_GEVAUDAN;
            default -> BossId.byId(parts[0]).orElse(BossId.MIKAEL);
        };
        String form = parts.length > 1 ? parts[1] : "";
        Look look = switch (BossBarForms.setOf(boss)) {
            case SHIFTER -> BossBarForms.shifter(form.equals("shifting") ? BossForm.SHIFTING : form.equals("beast") ? BossForm.BEAST : BossForm.HUMAN);
            case HOST -> BossBarForms.host(form.equals("host") ? 0 : form.equals("spirit") ? 2 : 1);
            case NONE -> Look.NONE;
        };
        add(mc, boss, look, false, look.shifting() ? 1100 : 1640, 2000);
    }

    private static void add(Minecraft mc, BossId boss, Look look, boolean fallen, int hp, int max) {
        UUID id = UUID.randomUUID();
        Component name = Component.translatable("entity.supernaturalbloodlines." + boss.id());
        mc.gui.getBossOverlay().events.put(id, new LerpingBossEvent(id, name, hp / (float) max, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS, false, false, false));
        BossBarClient.accept(BossBarInfoPayload.of(id, boss, look, fallen, hp, max));
        SHOWN.add(id);
    }
}
