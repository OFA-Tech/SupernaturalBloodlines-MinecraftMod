package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.curse;

import com.mojang.serialization.MapCodec;
import java.util.OptionalInt;
import net.favouriteless.enchanted.api.curses.Curse;
import net.favouriteless.enchanted.api.curses.CurseInstance;
import net.favouriteless.enchanted.api.curses.CurseManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.curse.SusceptibilityRules;
import net.ofatech.supernaturalbloodlines.core.death.DamageProbe;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismEnchantedConfig;

/**
 * The Curse of Insanity and the Curse of Susceptibility (R320 to R324), built as the two Tensura curses are: a {@code Curse.Type} registered with
 * Enchanted's {@code CurseManager}, cast by the rite data through {@code enchanted:apply_curse}, lifted by Enchanted's remove-curse rite, and ended
 * by death (Enchanted keeps curses through it; ours end with it, through {@code removeCurse} so {@code onRemove} runs). They need Vampirism and Enchanted,
 * not Tensura.
 */
public final class PlagueCurses {
    public static final Curse.Type<InsanityCurse> INSANITY = new Curse.Type<>(id("insanity"), InsanityCurse::new, MapCodec.unit(InsanityCurse::new));
    public static final Curse.Type<SusceptibilityCurse> SUSCEPTIBILITY = new Curse.Type<>(id("susceptibility"), SusceptibilityCurse::new, MapCodec.unit(SusceptibilityCurse::new));

    private PlagueCurses() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, path);
    }

    public static void register() {
        CurseManager.get().register(INSANITY);
        CurseManager.get().register(SUSCEPTIBILITY);
        // after the others, and not for a blow another mod already cancelled
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingIncomingDamageEvent.class, PlagueCurses::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                CurseManager.get().removeCurse(INSANITY, p.getUUID(), p.serverLevel());
                CurseManager.get().removeCurse(SUSCEPTIBILITY, p.getUUID(), p.serverLevel());
            }
        });
    }

    /** The strength of the player's Curse of Susceptibility, if they have one. */
    public static OptionalInt susceptibility(ServerPlayer p) {
        for (CurseInstance i : CurseManager.get().getCurses(p.getUUID(), p.serverLevel())) {
            if (!i.isRemoved() && i.getCurse() instanceof SusceptibilityCurse) {
                return OptionalInt.of(i.getStrength());
            }
        }
        return OptionalInt.empty();
    }

    static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !VampirismEnchantedConfig.susceptibilityEnabled()) {
            return;
        }
        OptionalInt strength = susceptibility(p);
        if (strength.isEmpty()) {
            return;
        }
        double multiplier = SusceptibilityRules.multiplier(strength.getAsInt(), VampirismEnchantedConfig.susceptibilityExtraPercent(),
                VampirismEnchantedConfig.susceptibilityMaxPercent());
        e.setAmount(SusceptibilityRules.apply(e.getAmount(), multiplier, e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY), DamageProbe.active()));
    }
}
