package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse;

import com.mojang.serialization.MapCodec;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import net.favouriteless.enchanted.api.curses.Curse;
import net.favouriteless.enchanted.api.curses.CurseManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class Curses {
    public static final Curse.Type<MagiculeDrainCurse> MAGICULE_DRAIN = new Curse.Type<>(
            id("magicule_drain"), MagiculeDrainCurse::new, MapCodec.unit(MagiculeDrainCurse::new));
    public static final Curse.Type<SkillSealCurse> SKILL_SEAL = new Curse.Type<>(
            id("skill_seal"), SkillSealCurse::new, SkillSealCurse.CODEC);

    private Curses() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, path);
    }

    public static void register() {
        CurseManager.get().register(MAGICULE_DRAIN);
        CurseManager.get().register(SKILL_SEAL);

        SkillEvents.ACTIVATE_SKILL.register((skill, owner, key, mode) ->
                owner instanceof ServerPlayer p && skill.isPresent() && SkillSealCurse.blocks(p, skill.get().getSkillId())
                        ? EventResult.interruptFalse() : EventResult.pass());
        // switching a sealed skill off stays possible, otherwise a skill that was already on could never be stopped
        SkillEvents.TOGGLE_SKILL.register((skill, owner) ->
                owner instanceof ServerPlayer p && skill.isPresent() && !skill.get().isToggled()
                        && SkillSealCurse.blocks(p, skill.get().getSkillId())
                        ? EventResult.interruptFalse() : EventResult.pass());

        // Enchanted keeps curses through death; ours end with it, through removeCurse so onRemove runs
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                CurseManager.get().removeCurse(MAGICULE_DRAIN, p.getUUID(), p.serverLevel());
                CurseManager.get().removeCurse(SKILL_SEAL, p.getUUID(), p.serverLevel());
            }
        });
    }
}
