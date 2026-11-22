package net.ofatech.supernaturalbloodlines.core;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.shared.supernatural.VampirismSkillAccess;

/**
 * Permanent attribute modifiers are saved with the player. If a part is switched off afterwards its code never runs,
 * so nothing would take its modifiers away. This lives in core, which always loads, and strips them on login.
 * <p>
 * A part that adds permanent (saved) attribute modifiers must list their ids in {@link #OWNED}.
 */
public final class StalePartModifiers {
    private static final Map<Part, Set<ResourceLocation>> OWNED = Map.of(
            Part.SPECIES, Set.of(id("original"), id("augustine"), id("hybrid"), id("hunter_mark"), id("wolf_rank"), id("pack_bonus")),
            Part.SPECIES_TENSURA, Set.of(id("layer_magicule"), id("layer_aura"), id("ep_boost"), id("primeval_true_alpha")));

    /** Modifiers whose id path starts with a prefix (ladder skills: {@code skill/<id>}, any attribute of any mod). */
    private static final Map<Part, Set<String>> OWNED_PREFIXES = Map.of(Part.SUPERNATURAL, Set.of("skill/"));

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
                String namespace = attribute.key().location().getNamespace();
                // Tensura's max magicule and aura carry the species_tensura layer modifiers, its EP gain the evolution boost
                if (namespace.equals(ResourceLocation.DEFAULT_NAMESPACE) || namespace.equals("tensura")) {
                    AttributeInstance instance = p.getAttribute(attribute);
                    if (instance != null) {
                        owned.getValue().forEach(instance::removeModifier);
                    }
                }
            });
        }
        for (Map.Entry<Part, Set<String>> owned : OWNED_PREFIXES.entrySet()) {
            if (enabled.contains(owned.getKey())) {
                continue;
            }
            BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
                AttributeInstance instance = p.getAttribute(attribute);
                if (instance != null) {
                    instance.getModifiers().stream().map(AttributeModifier::id)
                            .filter(id -> id.getNamespace().equals(SupernaturalBloodlines.MODID) && owned.getValue().stream().anyMatch(id.getPath()::startsWith))
                            .toList().forEach(instance::removeModifier);
                }
            });
        }
        // the Vampirism skills S-VAM-7 enabled belong to the supernatural part too; only the ones this mod enabled are taken back
        if (!enabled.contains(Part.SUPERNATURAL) && ModList.get().isLoaded("vampirism")
                && !SupernaturalAccess.get(p).vampirismGranted().isEmpty()) {
            VampirismSkillAccess.apply(p, Set.of());
        }
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }
}
