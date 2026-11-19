package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackSkillRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackSkillRules.Refusal;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Moonblood Surge and Wolf Spirits (R146), the two pack abilities of plan 15b. Both are decided here from server state ({@link PackSkillRules}); the
 * hub and {@code /sb pack surge|spirits} only ask. A refusal costs nothing. Cooldowns are kept in memory per player, like the Alpha Roar's, and
 * forgotten when the server stops (the tick count restarts then).
 * <p>
 * Surge: a werewolf in a pack buffs themselves and the pack within 24 blocks (Regeneration II, Absorption II, Strength I, Speed I) and pays for it
 * afterwards (Weakness I and Slowness I for 30 s). Spirits: an Alpha calls three spirit wolves tamed to them; they glow, drop nothing, never hurt the
 * pack ({@link Friends}) and leave after three minutes or when their owner is gone ({@link NpcPackBrain}).
 */
public final class PackSkills {
    private static final String KEY = "supernaturalbloodlines.pack.";
    private static final Map<UUID, Long> SURGE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SPIRITS = new ConcurrentHashMap<>();

    private PackSkills() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> reset());
    }

    @VisibleForTesting
    public static void reset() {
        SURGE.clear();
        SPIRITS.clear();
    }

    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator();
    }

    @Nullable
    public static Component surge(ServerPlayer p) {
        return surge(p, p.server.getTickCount());
    }

    /** The error to show, or null when the Surge happened. {@code now} is the server tick count. */
    @Nullable
    public static Component surge(ServerPlayer p, long now) {
        PackIndex index = PackIndex.get(p.serverLevel());
        long last = SURGE.getOrDefault(p.getUUID(), -1L);
        Refusal r = PackSkillRules.surge(SpeciesConfig.npc().surge(), VampirismSupernatural.isWerewolf(p), index.inPack(p.getUUID()),
                LunarEclipse.active(p.server), available(p), now, last, PackSkillRules.SURGE_COOLDOWN_TICKS);
        if (r != Refusal.NONE) {
            return refusal("surge", r, now, last, PackSkillRules.SURGE_COOLDOWN_TICKS);
        }
        SURGE.put(p.getUUID(), now);
        for (UUID id : index.pack(p.getUUID())) {
            ServerPlayer member = id.equals(p.getUUID()) ? p : SireCall.player(p.server, id);
            if (member != null && member.isAlive() && member.level() == p.level() && member.distanceToSqr(p) <= PackSkillRules.SURGE_RANGE * PackSkillRules.SURGE_RANGE) {
                member.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                member.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
                member.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 0));
                member.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 0));
            }
        }
        for (net.minecraft.world.entity.Mob member : NpcMembers.near(p, PackSkillRules.SURGE_RANGE)) {
            member.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
            member.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
            member.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 0));
            member.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 0));
        }
        // the price: it is paid by whoever called it
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PackSkillRules.SURGE_AFTER_TICKS, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PackSkillRules.SURGE_AFTER_TICKS, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 3.0f, 0.8f);
        return null;
    }

    @Nullable
    public static Component spirits(ServerPlayer p) {
        return spirits(p, p.server.getTickCount());
    }

    @Nullable
    public static Component spirits(ServerPlayer p, long now) {
        long last = SPIRITS.getOrDefault(p.getUUID(), -1L);
        Refusal r = PackSkillRules.spirits(SpeciesConfig.npc().spirits(), VampirismSupernatural.isWerewolf(p), PackAccess.wolf(p).rank(),
                LunarEclipse.active(p.server), available(p), now, last, PackSkillRules.SPIRITS_COOLDOWN_TICKS);
        if (r != Refusal.NONE) {
            return refusal("spirits", r, now, last, PackSkillRules.SPIRITS_COOLDOWN_TICKS);
        }
        SPIRITS.put(p.getUUID(), now);
        ServerLevel level = p.serverLevel();
        long expires = level.getGameTime() + PackSkillRules.SPIRITS_LIFE_TICKS;
        for (int i = 0; i < PackSkillRules.SPIRITS_COUNT; i++) {
            Wolf wolf = EntityType.WOLF.spawn(level, spiritSpot(level, p), MobSpawnType.MOB_SUMMONED);
            if (wolf != null) {
                wolf.tame(p);
                wolf.setOrderedToSit(false);
                wolf.setPersistenceRequired();
                wolf.setGlowingTag(true);
                wolf.setData(SpeciesRegistries.NPC_MARK, NpcMark.spirit(p.getUUID(), expires));
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 2.0f, 1.2f);
        return null;
    }

    /** A free spot within two blocks of the caster for a spirit (a few tries, so it is not put inside a wall), else the caster's own. */
    public static BlockPos spiritSpot(ServerLevel level, ServerPlayer p) {
        for (int i = 0; i < 6; i++) {
            BlockPos at = p.blockPosition().offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
            if (level.noCollision(EntityType.WOLF.getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))) {
                return at;
            }
        }
        return p.blockPosition();
    }

    private static Component refusal(String skill, Refusal r, long now, long last, long cooldown) {
        return switch (r) {
            case COOLDOWN -> Component.translatable(KEY + skill + "_cooldown", PackSkillRules.secondsLeft(now, last, cooldown));
            case OFF -> Component.translatable(KEY + skill + "_off");
            case NOT_WEREWOLF -> Component.translatable(KEY + "not_werewolf");
            case NOT_ALPHA -> Component.translatable(KEY + "not_alpha");
            case NO_PACK -> Component.translatable(KEY + "surge_no_pack");
            case ECLIPSE -> Component.translatable(KEY + "roar_eclipse");
            default -> Component.translatable(KEY + "roar_unavailable");
        };
    }
}
