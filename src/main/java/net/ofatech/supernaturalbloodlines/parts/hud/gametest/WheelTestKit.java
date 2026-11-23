package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import io.github.manasmods.manascore.skill.api.EntityEvents;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** What the wheel's Tensura GameTests share. Linked to Tensura and ManasCore: only the tests that need them touch it. */
final class WheelTestKit {
    private WheelTestKit() {
    }

    /** Learns a skill the way a quest would, without paying its magicule (a fake player has none). @return the learned instance */
    static ManasSkillInstance learn(ServerPlayer p, ResourceLocation id) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
        if (skill == null) {
            throw new IllegalStateException("no skill " + id);
        }
        ManasSkillInstance inst = skill.createDefaultInstance();
        CompoundTag tag = inst.getOrCreateTag();
        tag.putBoolean("NoMagiculeCost", true);
        inst.setTag(tag);
        SkillAPI.getSkillsFrom(p).learnSkill(inst);
        return SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow(() -> new IllegalStateException(id + " was not learned"));
    }

    /** One player tick as the game runs it: NeoForge's pre event, ManasCore's own living tick (it ticks held skills, toggles and cooldowns), the post event. */
    static void tick(ServerPlayer p) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(p));
        EntityEvents.LIVING_POST_TICK.invoker().tick(p);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    static void tick(ServerPlayer p, int times) {
        for (int i = 0; i < times; i++) {
            tick(p);
        }
    }
}
