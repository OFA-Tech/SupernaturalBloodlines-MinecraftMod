package net.ofatech.supernaturalbloodlines.parts.bosses.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.LinkIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.Links;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/**
 * The Linking Spell (R177): exactly two living creatures stand in the circle (the caster is not one of them), neither an Original, neither linked already; for
 * {@code link_seconds} they share damage. An Original in the circle and an already linked creature get the same plain answer, so a refusal never tells
 * a player who is an Original. Esther's Grimoire is a key: it is given back.
 */
public record LinkingRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "linking_spell");
    public static final String DONE = "supernaturalbloodlines.rite.link_done";
    public static final MapCodec<LinkingRiteFactory> CODEC = MapCodec.unit(LinkingRiteFactory::new);
    /** The medium circle is about this wide. */
    public static final double RADIUS = 6.0;

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new LinkingRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    /** Whether this creature is an Original, an NPC one or a player. */
    public static boolean isOriginal(LivingEntity e) {
        return e instanceof NpcOriginalBoss || e instanceof ServerPlayer p && OriginalRules.isOriginal(BloodlineAccess.get(p).species());
    }

    private static final class LinkingRite extends Rite {
        LinkingRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : null;
            List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS),
                    e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && (caster == null || e != caster));
            LinkIndex index = LinkIndex.get(level.getServer());
            long now = level.getGameTime();
            Optional<String> refusal = LinkRules.refusal(inside.size(), inside.stream().anyMatch(LinkingRiteFactory::isOriginal),
                    inside.stream().anyMatch(e -> index.linked(e.getUUID(), now)));
            if (refusal.isPresent()) {
                if (caster != null) {
                    caster.displayClientMessage(Component.translatable(refusal.get()), true);
                }
                return cancel();
            }
            Links.link(level.getServer(), inside.get(0).getUUID(), inside.get(1).getUUID(), BossesConfig.linkSeconds());
            params.consumedItems.stream().filter(s -> s.is(BossItems.ESTHER_GRIMOIRE.get())).findFirst()
                    .ifPresent(g -> level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, g.copy())));
            if (caster != null) {
                caster.displayClientMessage(Component.translatable(DONE, BossesConfig.linkSeconds()), true);
            }
            return false;
        }
    }
}
