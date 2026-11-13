package net.ofatech.supernaturalbloodlines.parts.species.original;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;

/**
 * What being an Original (ORIGINAL, ORIGINAL_HYBRID) means in play: +20% attack damage and +4 max health (R31), and
 * the dying state (R32). Lethal damage does not kill them: a vampire goes into Vampirism's own dying state (which
 * Vampirism starts by itself), a hybrid into our {@link Downed}. White oak, the void, /kill and everything in
 * {@code #minecraft:bypasses_invulnerability} (which holds {@code failed_transition}) still kill, and a white oak
 * blow also ends a dying state that is already running.
 */
public final class OriginalRules {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "original");
    private static final AttributeModifier MAX_HEALTH = new AttributeModifier(MODIFIER_ID, 4.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier ATTACK = new AttributeModifier(MODIFIER_ID, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private OriginalRules() {}

    public static void register() {
        BloodlineAccess.addListener((p, change) -> sync(p));
        // the modifiers are saved with the player (so the health saved above 20 survives a relog); these only repair gaps
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                restore(p, !e.isEndConquered());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, OriginalRules::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingIncomingDamageEvent.class, OriginalRules::onWhiteOakWhileDying);
        // HIGH: Vampirism's onLivingDeathFirst is HIGHEST, so we always run right after it (it has already cancelled the
        // death if it starts its dying state), but before every NORMAL/HIGH handler that would see a "real" death
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, true, LivingDeathEvent.class, OriginalRules::onDeath);
        Downed.register();
    }

    public static boolean isOriginal(Species s) {
        return s == Species.ORIGINAL || s == Species.ORIGINAL_HYBRID;
    }

    /** Idempotent: makes the player's attributes match their species. */
    public static void sync(ServerPlayer p) {
        boolean original = isOriginal(BloodlineAccess.get(p).species());
        apply(p, Attributes.MAX_HEALTH, MAX_HEALTH, original);
        apply(p, Attributes.ATTACK_DAMAGE, ATTACK, original);
    }

    /** After a respawn: a fresh entity starts at 20 health, so an Original is topped up to their real maximum. */
    public static void restore(ServerPlayer p, boolean heal) {
        sync(p);
        if (heal && isOriginal(BloodlineAccess.get(p).species())) {
            p.setHealth(p.getMaxHealth());
        }
    }

    private static void syncIfPlayer(net.minecraft.world.entity.player.Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attribute, AttributeModifier modifier, boolean on) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        if (on && !a.hasModifier(MODIFIER_ID)) {
            a.addPermanentModifier(modifier);
        } else if (!on) {
            a.removeModifier(MODIFIER_ID);
        }
    }

    /** A melee hit (not thorns, not a projectile) from someone holding a weapon in {@code #supernaturalbloodlines:white_oak}. */
    static boolean isWhiteOak(DamageSource source) {
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        return melee && source.getDirectEntity() == source.getEntity()
                && source.getDirectEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().is(WhiteOak.TAG);
    }

    /** What the protection never applies to. */
    static boolean bypassesProtection(DamageSource source) {
        return isWhiteOak(source) || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(OutcomeExecutor.FAILED_TRANSITION);
    }

    /** Vampirism blocks all damage to a player in its dying state, white oak included, so the blow is caught before that. */
    private static void onWhiteOakWhileDying(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && isOriginal(BloodlineAccess.get(p).species()) && !Downed.isLettingDie(p)
                && isWhiteOak(e.getSource()) && VampirismAPI.vampirePlayer(p).isDBNO()) {
            e.setCanceled(true);
            endVampirismDying(p, e.getSource());
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && Downed.isDowned(p) && !bypassesProtection(e.getSource())) {
            e.setCanceled(true);
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isOriginal(BloodlineAccess.get(p).species()) || Downed.isLettingDie(p)) {
            return;
        }
        DamageSource source = e.getSource();
        if (bypassesProtection(source)) {
            if (!e.isCanceled()) {
                Downed.clear(p); // dying for real
            } else if (isWhiteOak(source)) {
                endVampirismDying(p, source);
            }
            return;
        }
        if (e.isCanceled()) {
            return; // Vampirism's dying state (or another mod's save) already has it
        }
        e.setCanceled(true);
        Downed.start(p, source);
    }

    /**
     * White oak ends Vampirism's dying state: the player dies from this blow. The kill comes first, with a
     * {@code generic_kill} source (it is in {@code #bypasses_invulnerability}, so Vampirism's hook lets it through)
     * that still names the attacker for kill credit; Vampirism's state is only cleaned up afterwards, so a missing
     * {@code giveUpDBNO} leaves a dead player, never a stuck one.
     */
    static void endVampirismDying(ServerPlayer p, DamageSource blow) {
        IVampirePlayer vampire = VampirismAPI.vampirePlayer(p);
        if (!vampire.isDBNO()) {
            return; // cancelled by something else: not ours to undo
        }
        Entity attacker = blow.getEntity();
        DamageSource kill = attacker != null ? p.damageSources().source(DamageTypes.GENERIC_KILL, attacker) : p.damageSources().genericKill();
        Downed.letDie(p, () -> {
            p.setHealth(0.0F);
            p.die(kill);
            try {
                // giveUpDBNO is on the implementation only, not in the API jar; it resets the timer and pose
                vampire.getClass().getMethod("giveUpDBNO").invoke(vampire);
            } catch (ReflectiveOperationException ex) {
                SupernaturalBloodlines.LOGGER.warn("Could not reset Vampirism's dying state of {}", p.getGameProfile().getName(), ex);
            }
        });
    }
}
