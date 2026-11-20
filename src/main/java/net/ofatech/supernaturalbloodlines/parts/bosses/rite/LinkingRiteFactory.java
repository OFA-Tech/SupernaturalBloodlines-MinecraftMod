package net.ofatech.supernaturalbloodlines.parts.bosses.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
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
import net.neoforged.neoforge.common.Tags;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.LinkIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.Links;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;

/**
 * The Linking Spell (R177): a witch-type performer; exactly two living creatures stand in the circle (the performer counts if they stand in it); neither is an
 * NPC Original or a boss, neither is linked, and every other player (a player Original too) is sneaking inside the circle with a lead in the main hand to
 * agree, with PvP on. For {@code link_seconds} they share damage.
 * <b>Every refusal is the same message and costs the same</b> (the lead, the amethyst and the altar's power are spent; the grimoire always comes back), so a
 * refusal never tells a player who is a boss. A performer who is not a witch type learns nothing about the circle and gets everything back.
 */
public record LinkingRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "linking_spell");
    public static final String DONE = "supernaturalbloodlines.rite.link_done";
    public static final MapCodec<LinkingRiteFactory> CODEC = MapCodec.unit(LinkingRiteFactory::new);
    /** How far from the centre of the circle (blocks, flat) a creature counts as inside: the medium circle is 11 wide, so 5.5 from the middle. Its feet may be this far above or below the altar. */
    public static final double RADIUS = 5.5;
    public static final double HEIGHT = 2.0;

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

    /** Whether this creature is one of the NPC Originals: visibly a boss, so refusing it tells nobody anything. A player Original may be linked like anyone who consents. */
    public static boolean isNpcOriginal(LivingEntity e) {
        return e instanceof NpcOriginalBoss;
    }

    /** Whether this creature is a boss: ours, or any a pack puts in the boss tag, or anything that cannot change dimensions (the dragon, the wither). */
    public static boolean isBoss(LivingEntity e) {
        return e instanceof BossNpc || e.getType().is(Tags.EntityTypes.BOSSES) || !e.canChangeDimensions(e.level(), e.level());
    }

    private static final class LinkingRite extends Rite {
        LinkingRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer caster)) {
                return cancel();
            }
            if (!SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species())) {
                caster.displayClientMessage(Component.translatable(LinkRules.NOT_WITCH), true);
                return cancel();
            }
            // links that ran out are taken out first: an old one must never stand in front of the new one
            Links.expire(level.getServer(), level.getGameTime());
            double rr = RADIUS * RADIUS;
            List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS, HEIGHT + 2, RADIUS),
                    e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && Math.abs(e.getY() - pos.getY()) <= HEIGHT
                            && (e.getX() - (pos.getX() + 0.5)) * (e.getX() - (pos.getX() + 0.5)) + (e.getZ() - (pos.getZ() + 0.5)) * (e.getZ() - (pos.getZ() + 0.5)) <= rr);
            LinkIndex index = LinkIndex.get(level.getServer());
            long now = level.getGameTime();
            boolean pvp = level.getServer().isPvpAllowed();
            boolean refuses = LinkRules.refuses(inside.size(),
                    inside.stream().anyMatch(LinkingRiteFactory::isNpcOriginal),
                    inside.stream().anyMatch(LinkingRiteFactory::isBoss),
                    inside.stream().anyMatch(e -> index.linked(e.getUUID(), now)),
                    inside.stream().anyMatch(e -> e instanceof ServerPlayer p && !LinkRules.consents(p == caster, p.isShiftKeyDown(), p.getMainHandItem().is(net.minecraft.world.item.Items.LEAD), pvp)));
            // the grimoire is a key, not an ingredient: it goes back to the circle whatever happens
            params.consumedItems.stream().filter(s -> s.is(BossItems.ESTHER_GRIMOIRE.get())).findFirst()
                    .ifPresent(g -> level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, g.copy())));
            if (refuses) {
                caster.displayClientMessage(Component.translatable(LinkRules.WONT_HOLD), true);
                return false; // not cancelled: the rest of the ingredients and the power are spent
            }
            Links.link(level.getServer(), inside.get(0), inside.get(1), BossesConfig.linkSeconds(), caster.getUUID());
            caster.displayClientMessage(Component.translatable(DONE, BossesConfig.linkSeconds()), true);
            return false;
        }
    }
}
