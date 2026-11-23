package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.boss.BeastRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.PikeRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import org.jetbrains.annotations.Nullable;

/**
 * What the Beast's three bodies suffer (R184 to R189). The Beast: shots do 0.6 of their damage, a wolfsbane pulse takes a heart at most, nothing but the void and
 * {@code /kill} can take it below 1 health (it collapses first, {@link BeastDirector#watch}), and a death that does come is a collapse. The damage players deal it is
 * counted in the index (a pet's for its owner), for the whole encounter. The Spirit: only the Valet Pike hurts it, and one blow ends it. The Host: only what bypasses
 * invulnerability kills it, and then the encounter is off ({@link BeastDirector#hostLost}).
 */
public final class BeastEvents {
    private static final ResourceKey<DamageType> WOLFSBANE_POISON = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "wolfsbane_poison"));

    private BeastEvents() {
    }

    public static void register() {
        // the Beast is supernatural, prey of mountain ash and of wolfsbane, only while its switch says so (a Hearing Device stuns it either way)
        Supernatural.register(e -> e instanceof BeastEntity, e -> false, e -> e instanceof BeastEntity && BossesConfig.beastRespectsAsh());
        Supernatural.registerWerewolfLike(e -> e instanceof BeastEntity);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingIncomingDamageEvent.class, BeastEvents::onIncoming);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.Pre.class, BeastEvents::onPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, true, LivingDamageEvent.Post.class, BeastEvents::onDamaged);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, BeastEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.class,
                e -> {
                    if (e.getEntity() instanceof ServerPlayer p) {
                        BeastDirector.deliverOwed(p);
                    }
                });
    }

    /**
     * Whether the blow is a direct blow of the Valet Pike: the striker made it themselves (not a snowball, an arrow or a trident thrown while they hold the pike) and the
     * weapon the damage source names is the pike. Nothing else counts.
     */
    public static boolean pike(DamageSource source) {
        ItemStack weapon = source.getWeaponItem();
        return PikeRules.hurtsSpirit(source.isDirect() && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK), weapon != null && weapon.is(BossItems.VALET_PIKE.get()));
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource source = e.getSource();
        if (e.getEntity() instanceof GevaudanSpiritEntity spirit) {
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return;
            }
            if (pike(source)) {
                if (!net.ofatech.supernaturalbloodlines.core.death.DamageProbe.active()) {
                    spirit.markPikeHit(); // a probe deals nothing: no Pike hit is counted (the answer, the amount and the cancel, are the same)
                }
                e.setAmount(Math.max(e.getAmount(), spirit.getMaxHealth()));
            } else {
                e.setCanceled(true);
            }
        } else if (e.getEntity() instanceof BeastEntity) {
            if (source.is(WOLFSBANE_POISON)) {
                e.setAmount(BeastRules.wolfsbaneDamage(e.getAmount()));
            } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                e.setAmount(BeastRules.damageTaken(true, e.getAmount()));
            }
        }
    }

    /** The Beast does not die of blows: what would take it to 0 leaves it at 1, and the next look collapses it. */
    private static void onPre(LivingDamageEvent.Pre e) {
        if (e.getEntity() instanceof BeastEntity beast && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setNewDamage(Math.min(e.getNewDamage(), Math.max(0F, beast.getHealth() - 1F)));
        }
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof BeastEntity beast && beast.level() instanceof ServerLevel level) {
            ServerPlayer who = Deals.playerBehind(level.getServer(), e.getSource().getEntity());
            if (who != null) {
                BossIndex.get(level.getServer()).of(BossId.BEAST_OF_GEVAUDAN).tally.add(who.getUUID(), e.getNewDamage());
                BossIndex.get(level.getServer()).setDirty();
            }
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (e.getEntity() instanceof BeastEntity beast) {
            // the void or /kill: the Beast falls instead, the way it does at a quarter of its health
            e.setCanceled(true);
            beast.setHealth(1F);
            BeastDirector.collapse(level, beast);
        } else if (e.getEntity() instanceof GevaudanSpiritEntity spirit) {
            if (spirit.pikeHit()) {
                BeastDirector.killed(level, spirit, striker(level, e.getSource()));
            } else {
                BeastDirector.escape(level, spirit);
            }
        } else if (e.getEntity() instanceof GevaudanHostEntity host) {
            BeastDirector.hostLost(level, host);
        }
    }

    @Nullable
    private static ServerPlayer striker(ServerLevel level, DamageSource source) {
        return Deals.playerBehind(level.getServer(), source.getEntity());
    }
}
