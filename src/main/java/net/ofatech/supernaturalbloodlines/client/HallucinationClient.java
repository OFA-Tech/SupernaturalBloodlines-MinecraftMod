package net.ofatech.supernaturalbloodlines.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.network.HallucinationPayload.Kind;

/**
 * Plays a {@link Kind} on the local player. Client only: nothing but the payload handler in ModNetwork refers to this
 * class, and that lambda only runs when a HallucinationPayload arrives, so a dedicated server never loads it.
 * <p>
 * Everything here stays on this client: the phantom is a zombie that is added to the client level only, and the
 * darkness is a local effect instance the server never hears about.
 */
public final class HallucinationClient {
    private static final int PHANTOM_TICKS = 100; // 5 seconds
    private static final int DARKNESS_TICKS = 40;

    private record Phantom(Zombie zombie, long until) {}

    private static final List<Phantom> PHANTOMS = new ArrayList<>();
    // far above anything the server hands out, so a phantom never shares an id with a real entity
    private static int nextId = Integer.MAX_VALUE;
    private static boolean listening;

    private HallucinationClient() {}

    public static void show(Kind kind) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return;
        }
        switch (kind) {
            case PHANTOM -> phantom(level, player);
            case WHISPER -> level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.AMBIENT, 1.0f, 0.7f + level.random.nextFloat() * 0.3f, false);
            case DARKNESS -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0, true, false, false));
        }
    }

    private static void phantom(ClientLevel level, LocalPlayer player) {
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = 5 + level.random.nextDouble() * 3;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos feet = standingPlace(level, BlockPos.containing(x, player.getY(), z));
            if (feet == null) {
                continue;
            }
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) {
                return;
            }
            float yaw = (float) (Mth.atan2(player.getZ() - z, player.getX() - x) * (180 / Math.PI)) - 90f;
            zombie.setId(nextId--);
            zombie.moveTo(x, feet.getY(), z, yaw, 0f);
            zombie.setYHeadRot(yaw);
            zombie.setYBodyRot(yaw);
            zombie.setNoAi(true);
            zombie.setSilent(true);
            zombie.setInvulnerable(true);
            level.addEntity(zombie);
            PHANTOMS.add(new Phantom(zombie, level.getGameTime() + PHANTOM_TICKS));
            return;
        }
    }

    /** Free space with a floor, searching three blocks down and two up from {@code around}. */
    private static BlockPos standingPlace(ClientLevel level, BlockPos around) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = around.offset(0, dy, 0);
            if (level.getBlockState(pos.below()).blocksMotion() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static void tick() {
        if (PHANTOMS.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        PHANTOMS.removeIf(p -> {
            boolean gone = level == null || p.zombie.level() != level || level.getGameTime() >= p.until;
            if (gone && level != null) {
                level.removeEntity(p.zombie.getId(), Entity.RemovalReason.DISCARDED);
            }
            return gone;
        });
    }
}
