package net.ofatech.supernaturalbloodlines.core;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Permanent attribute modifiers are saved with the player. If a part is switched off afterwards its code never runs,
 * so nothing would take its modifiers away. This lives in core, which always loads, and strips them on login.
 * <p>
 * A part that adds permanent (saved) attribute modifiers must list their ids in {@link #OWNED}.
 */
public final class StalePartModifiers {
    private static final Map<Part, Set<ResourceLocation>> OWNED = Map.of(
            Part.SPECIES, Set.of(id("original"), id("hybrid"), id("hunter_mark"), id("wolf_rank")));

    private StalePartModifiers() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, path);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                strip(p, EnabledParts.snapshot());
            }
        });
    }

    /** Removes, from every vanilla attribute of {@code p}, the modifiers owned by a part that is not in {@code enabled}. */
    public static void strip(ServerPlayer p, Set<Part> enabled) {
        for (Map.Entry<Part, Set<ResourceLocation>> owned : OWNED.entrySet()) {
            if (enabled.contains(owned.getKey())) {
                continue;
            }
            BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
                if (attribute.key().location().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                    AttributeInstance instance = p.getAttribute(attribute);
                    if (instance != null) {
                        owned.getValue().forEach(instance::removeModifier);
                    }
                }
            });
        }
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }
}
