package net.ofatech.supernaturalbloodlines.core.attachment;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
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

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
