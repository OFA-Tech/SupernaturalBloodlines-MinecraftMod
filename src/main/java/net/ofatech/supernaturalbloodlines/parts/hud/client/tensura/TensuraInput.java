package net.ofatech.supernaturalbloodlines.parts.hud.client.tensura;

import dev.architectury.networking.NetworkManager;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.client.TensuraKeybinds;
import io.github.manasmods.tensura.network.c2s.RequestAbilityModeChangePacket;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import io.github.manasmods.tensura.storage.ability.IAbility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.InputPlan;

/**
 * Tensura mode: a slot key sends what Tensura's own key sends, in the same order ({@link InputPlan} is the tested table): a mode change when Next or
 * Previous ability mode is held, else {@code skillActivationPacket(id, keyNumber, mode)}, and on release {@code skillReleasePacket(id, keyNumber, mode)}.
 * The server (Tensura, ManasCore) validates everything; the right column works because the HUD part's next-preset mixin lets Tensura accept preset N+1.
 * What was pressed is remembered per slot, so a release matches it even if the preset changed meanwhile. Linked to Tensura: reached only after
 * {@code ModList.isLoaded("tensura")}.
 */
public final class TensuraInput {
    private static final ResourceLocation[] HELD_SKILL = new ResourceLocation[6];
    private static final int[] HELD_MODE = new int[6];

    private TensuraInput() {
    }

    public static void press(LocalPlayer p, int hudSlot) {
        AbilitySlot slot = TensuraSlots.slot(p, hudSlot);
        ResourceLocation skill = slot == null || slot.isEmpty() || slot.getSkill() == null ? null : slot.getSkill().getRegistryName();
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        InputPlan.Step step = InputPlan.forPress(hudSlot, skill, slot == null ? 0 : slot.getMode(), TensuraKeybinds.NEXT_ABILITY_MODE.isDown(),
                TensuraKeybinds.PREVIOUS_ABILITY_MODE.isDown(), ab.getActivePreset(), ab.getPresets().size());
        switch (step.kind()) {
            case ACTIVATE -> {
                HELD_SKILL[hudSlot] = step.skill();
                HELD_MODE[hudSlot] = step.mode();
                SkillAPI.skillActivationPacket(step.skill(), step.keyNumber(), step.mode());
            }
            case CHANGE_MODE -> NetworkManager.sendToServer(RequestAbilityModeChangePacket.changeModePacket(step.keyNumber(), step.preset(), step.backwards()));
            default -> {
            }
        }
    }

    public static void release(int hudSlot) {
        InputPlan.Step step = InputPlan.forRelease(hudSlot, HELD_SKILL[hudSlot], HELD_MODE[hudSlot]);
        HELD_SKILL[hudSlot] = null;
        if (step.kind() == InputPlan.Kind.RELEASE) {
            SkillAPI.skillReleasePacket(step.skill(), step.keyNumber(), step.mode());
        }
    }
}
