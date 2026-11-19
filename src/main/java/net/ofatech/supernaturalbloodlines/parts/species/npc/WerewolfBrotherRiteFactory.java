package net.ofatech.supernaturalbloodlines.parts.species.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.teamlapen.werewolves.core.ModEntities;
import de.teamlapen.werewolves.api.entities.werewolf.IWerewolfAlpha;
import java.util.Comparator;
import java.util.List;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialBoss;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * The Rite of the Werewolf Brother (R148): a small circle; the performer is the caster (a werewolf, any rank), and the wolf is the nearest recruitable
 * werewolf mob standing in the circle. The rite does not list the mob as an ingredient, so Enchanted never consumes it: it finds the wolf itself (spike
 * U1 of plan 15: no mob needs to be an ingredient). Every refusal cancels the rite, so Enchanted gives the ingredients back. The decision is
 * {@link NpcRules#recruit}.
 */
public record WerewolfBrotherRiteFactory(String faction) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "werewolf_brother");
    /** {@code faction} is only what the Rites page reads to list it first for werewolves. */
    public static final MapCodec<WerewolfBrotherRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("faction", "werewolf").forGetter(WerewolfBrotherRiteFactory::faction)
    ).apply(i, WerewolfBrotherRiteFactory::new));
    /** The wolf must stand within this many blocks of the circle's centre. */
    public static final double REACH = 4.0;
    private static final String KEY = "supernaturalbloodlines.npc.";

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new BrotherRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    /** A beast form of the Werewolves mod (the beast and the survivalist): the human form, the Alpha and the minion are not what this rite takes. */
    public static boolean isBeast(Mob m) {
        return m.getType() == ModEntities.WEREWOLF_BEAST.get() || m.getType() == ModEntities.WEREWOLF_SURVIVALIST.get();
    }

    /** Whether a mob is the kind the rite could take at all (a beast, alive, with no bond and no trial mark), before health and caps are looked at. */
    public static boolean candidate(Mob m) {
        return m.isAlive() && isBeast(m) && !(m instanceof IWerewolfAlpha) && !AlphaTrialBoss.isBoss(m) && NpcMembers.bondOf(m).isEmpty();
    }

    /** The nearest wolf the rite could take within {@link #REACH} of {@code centre} (a bonded wolf, an Alpha or a boss that stands nearer is skipped), or null. */
    public static Mob wolfNear(net.minecraft.server.level.ServerLevel level, BlockPos centre) {
        return level.getEntitiesOfClass(Mob.class, new AABB(centre).inflate(REACH), WerewolfBrotherRiteFactory::candidate).stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5))).orElse(null);
    }

    /** Whether {@code owner} may take {@code target} in now; the refusal says why. */
    public static NpcRules.Refusal check(ServerPlayer owner, Mob target) {
        net.minecraft.server.level.ServerLevel level = owner.serverLevel();
        SpeciesConfig.MemberTuning t = SpeciesConfig.members();
        return NpcRules.recruit(t.recruit(), VampirismSupernatural.isWerewolf(owner), isBeast(target) || target instanceof IWerewolfAlpha, target instanceof IWerewolfAlpha,
                AlphaTrialBoss.isBoss(target), NpcMembers.bondOf(target).isPresent(), target.getHealth() / Math.max(1f, target.getMaxHealth()),
                NpcMembers.brothers(level, owner.getUUID()), t.perPlayer(), NpcMembers.brothersInPack(level, owner.getUUID()), t.perPack());
    }

    /** Recruits {@code target} for {@code owner} if the rules allow. @return the refusal key to show, or null when it worked */
    public static String recruit(ServerPlayer owner, Mob target) {
        NpcRules.Refusal r = check(owner, target);
        if (r != NpcRules.Refusal.NONE) {
            return KEY + "refuse." + r.name().toLowerCase(java.util.Locale.ROOT);
        }
        NpcMembers.bind(target, owner, NpcBond.brother(owner.getUUID(), owner.level().getGameTime(), 0));
        owner.sendSystemMessage(Component.translatable(KEY + "recruited", target.getDisplayName()));
        return null;
    }

    private static final class BrotherRite extends Rite {
        BrotherRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!ModList.get().isLoaded("werewolves")) {
                return cancel();
            }
            if (!(level.getPlayerByUUID(params.caster) instanceof ServerPlayer caster)) {
                return cancel();
            }
            Mob wolf = wolfNear(level, getPos());
            String refusal = wolf == null ? KEY + "refuse.no_wolf" : recruit(caster, wolf);
            if (refusal != null) {
                caster.displayClientMessage(Component.translatable(refusal), true);
                return cancel();
            }
            return false;
        }
    }
}
