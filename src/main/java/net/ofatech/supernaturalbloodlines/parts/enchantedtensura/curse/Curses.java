package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse;

import com.mojang.serialization.MapCodec;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import net.favouriteless.enchanted.api.curses.Curse;
import net.favouriteless.enchanted.api.curses.CurseManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class Curses {
    public static final String SKILL_SEALED = "supernaturalbloodlines.enchanted_tensura.skill_sealed";
    public static final Curse.Type<MagiculeDrainCurse> MAGICULE_DRAIN = new Curse.Type<>(
            id("magicule_drain"), MagiculeDrainCurse::new, MapCodec.unit(MagiculeDrainCurse::new));
    public static final Curse.Type<SkillSealCurse> SKILL_SEAL = new Curse.Type<>(
            id("skill_seal"), SkillSealCurse::new, SkillSealCurse.CODEC);

    private Curses() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, path);
    }

    // Tensura's own listener may interrupt first, so the tests call these two directly instead of firing the events
    public static EventResult onActivate(Changeable<ManasSkillInstance> skill, LivingEntity owner) {
        if (owner instanceof ServerPlayer p && skill.isPresent() && SkillSealCurse.blocks(p, skill.get().getSkillId())) {
            p.displayClientMessage(Component.translatable(SKILL_SEALED), true);
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }

    // switching a sealed skill off stays possible, otherwise a skill that was already on could never be stopped
    public static EventResult onToggle(Changeable<ManasSkillInstance> skill, LivingEntity owner) {
        if (owner instanceof ServerPlayer p && skill.isPresent() && !skill.get().isToggled()
                && SkillSealCurse.blocks(p, skill.get().getSkillId())) {
            p.displayClientMessage(Component.translatable(SKILL_SEALED), true);
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }

    public static void register() {
        CurseManager.get().register(MAGICULE_DRAIN);
        CurseManager.get().register(SKILL_SEAL);

        SkillEvents.ACTIVATE_SKILL.register((skill, owner, key, mode) -> onActivate(skill, owner));
        SkillEvents.TOGGLE_SKILL.register(Curses::onToggle);

        // Enchanted keeps curses through death; ours end with it, through removeCurse so onRemove runs
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                CurseManager.get().removeCurse(MAGICULE_DRAIN, p.getUUID(), p.serverLevel());
                CurseManager.get().removeCurse(SKILL_SEAL, p.getUUID(), p.serverLevel());
            }
        });
    }
}
