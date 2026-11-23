package net.ofatech.supernaturalbloodlines.core.attachment;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SupernaturalBloodlines.MODID);

    /** Kept on death; the rules decide what a death resets (the DIED event). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BloodlineState>> BLOODLINE =
            ATTACHMENTS.register("bloodline", () -> AttachmentType.builder(() -> BloodlineState.NONE)
                    .serialize(BloodlineState.CODEC)
                    .copyOnDeath()
                    .build());

    /** Ladder progress, skill points and skills (plan 12); kept on death, a rung is never lost. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SupernaturalState>> SUPERNATURAL =
            ATTACHMENTS.register("supernatural", () -> AttachmentType.builder(() -> SupernaturalState.NONE)
                    .serialize(SupernaturalState.CODEC)
                    .copyOnDeath()
                    .build());

    /**
     * A villager's doppelgänger roll (plan 12b, R14a); saved with the entity and rolled once, so a reload never rolls again.
     * {@code copyOnDeath} is also what makes NeoForge copy it when a mob converts (villager to zombie villager and cured back).
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DoppelgangerRoll>> DOPPELGANGER_VILLAGER =
            ATTACHMENTS.register("doppelganger_villager", () -> AttachmentType.builder(() -> DoppelgangerRoll.NONE)
                    .serialize(DoppelgangerRoll.CODEC)
                    .copyOnDeath()
                    .build());

    /** The six Supernatural-mode slots of the ability HUD (plan 13); kept on death, and never put back to the default once the player changed it. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Loadout>> HUD_LOADOUT =
            ATTACHMENTS.register("hud_loadout", () -> AttachmentType.builder(() -> Loadout.DEFAULT)
                    .serialize(Loadout.CODEC)
                    .copyOnDeath()
                    .build());

    /** The mode the wheel last chose for a power that is in no Tensura slot (a slot keeps its own mode); kept on death. Skill id to mode index. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<java.util.Map<net.minecraft.resources.ResourceLocation, Integer>>> WHEEL_MODES =
            ATTACHMENTS.register("wheel_modes", () -> AttachmentType.<java.util.Map<net.minecraft.resources.ResourceLocation, Integer>>builder(() -> java.util.Map.of())
                    .serialize(com.mojang.serialization.Codec.unboundedMap(net.minecraft.resources.ResourceLocation.CODEC, com.mojang.serialization.Codec.INT))
                    .copyOnDeath()
                    .build());

    /** The Tensura race this mod replaced (a werewolf's Beastfolk line), put back on a cure (plan 13d); kept on death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RaceMemory>> RACE_MEMORY =
            ATTACHMENTS.register("race_memory", () -> AttachmentType.builder(() -> RaceMemory.NONE)
                    .serialize(RaceMemory.CODEC)
                    .copyOnDeath()
                    .build());

    /** Until which game time the Rite of Cloaking hides this player from the compass and the hunters' sense (R172); not kept on death: a death ends the cloak. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> CLOAK_UNTIL =
            ATTACHMENTS.register("cloak_until", () -> AttachmentType.builder(() -> 0L).serialize(com.mojang.serialization.Codec.LONG).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
