package net.ofatech.supernaturalbloodlines.parts.evolutionrites.merge;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.evolution.RaphaelRules;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoGrant;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.FoxAblazeProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The merge of the two Raphaels (spec D14, 19b). With FoxAblaze installed its Raphael is the real one: any learn of Great Sage's Raphael (Great Sage's own
 * evolution, a command, a later Great Sage update, our dormant rite) is turned into a free learn of FoxAblaze's, which registers its own single owner when it
 * is learned. Great Sage's skill is never stored. A world that already holds Great Sage's is converted at login.
 *
 * <p>The one cost, accepted by the owner: Great Sage's evolution forgets Great Sage and Degenerate <em>before</em> it grants Raphael and ignores the result, so a
 * refused redirect leaves the player without both. They are given back at default mastery (only what is missing); the mastery they had is lost.
 * No mixin: a {@code SkillEvents.UNLOCK_SKILL} listener is all it takes.
 */
public final class RaphaelMerge {
    public static final String NOT_READY = "supernaturalbloodlines.refusal.evolution.not_ready";
    public static final String RESTORED = "supernaturalbloodlines.raphael.restored";
    private static final Logger LOGGER = LoggerFactory.getLogger("supernaturalbloodlines");
    private static final Set<UUID> WARNED = ConcurrentHashMap.newKeySet();

    /**
     * @param from      the skill that is redirected (Great Sage's Raphael)
     * @param to        the skill that is learned instead (FoxAblaze's Raphael)
     * @param family    every skill that counts as holding Raphael
     * @param restore   what a refusal gives back when it is missing (what Great Sage's evolution forgot)
     * @param active    whether the merge is on (the other addon is installed)
     */
    public record Merge(ResourceLocation from, ResourceLocation to, List<ResourceLocation> family, List<ResourceLocation> restore, BooleanSupplier active) {
    }

    private static final Merge PRODUCTION = new Merge(
            ResourceLocation.fromNamespaceAndPath("greatsage", "raphael"), FoxAblazeProbe.RAPHAEL,
            List.of(ResourceLocation.fromNamespaceAndPath("greatsage", "raphael"), FoxAblazeProbe.RAPHAEL),
            List.of(ResourceLocation.fromNamespaceAndPath("tensura", "degenerate"), ResourceLocation.fromNamespaceAndPath("tensura", "great_sage")),
            FoxAblazeProbe::loaded);

    private static volatile Merge override;

    private RaphaelMerge() {
    }

    /** A test seam: stand-in skills for the real ones. Null in play. */
    public static void override(Merge merge) {
        override = merge;
        WARNED.clear();
    }

    private static Merge merge() {
        Merge o = override;
        return o != null ? o : PRODUCTION;
    }

    public static void register() {
        SkillEvents.UNLOCK_SKILL.register(RaphaelMerge::onUnlock);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                sweep(player);
            }
        });
    }

    private static boolean held(ServerPlayer p, ResourceLocation id) {
        return SkillAPI.getSkillsFrom(p).getSkill(id).isPresent();
    }

    private static boolean holdsAny(ServerPlayer p, List<ResourceLocation> ids) {
        return ids.stream().anyMatch(id -> held(p, id));
    }

    private static EventResult onUnlock(ManasSkillInstance instance, LivingEntity who, Changeable<MutableComponent> message) {
        Merge m = merge();
        if (!(who instanceof ServerPlayer player) || !instance.getSkillId().equals(m.from()) || !m.active().getAsBoolean()) {
            return EventResult.pass();
        }
        ManasSkill target = SkillAPI.getSkillRegistry().get(m.to());
        if (target == null) {
            return EventResult.pass(); // the other addon is not really there: leave the skill as it is
        }
        switch (RaphaelRules.ofLearn(new RaphaelRules.Facts(true, holdsAny(player, m.family()), FoxAblazeProbe.ownedByOther(player)))) {
            case PASS -> {
                return EventResult.pass();
            }
            case ALREADY_HELD -> {
                return EventResult.interruptFalse();
            }
            case REFUSE -> refuse(player, m);
            case REDIRECT -> {
                if (!EvoGrant.learnFree(player, target)) {
                    refuse(player, m);
                }
            }
        }
        return EventResult.interruptFalse();
    }

    /** One line whatever the cause (another owner, a vetoed learn): which of the two it was is not the player's to learn. */
    private static void refuse(ServerPlayer player, Merge m) {
        player.displayClientMessage(Component.translatable(NOT_READY), true);
        boolean gaveBack = false;
        for (ResourceLocation id : m.restore()) {
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            if (skill != null && !held(player, id) && EvoGrant.learnFree(player, skill)) {
                gaveBack = true;
            }
        }
        if (gaveBack) {
            player.sendSystemMessage(Component.translatable(RESTORED));
        }
    }

    /** What the login does for a player who holds the redirected skill (called for every login; does nothing without the other addon). */
    public static void sweep(ServerPlayer player) {
        Merge m = merge();
        if (!m.active().getAsBoolean()) {
            return;
        }
        boolean source = held(player, m.from());
        boolean target = held(player, m.to());
        switch (RaphaelRules.onLogin(true, source, target, source && !target && FoxAblazeProbe.ownedByOther(player))) {
            case NOTHING -> {
            }
            case DROP_SOURCE -> SkillAPI.getSkillsFrom(player).forgetSkill(m.from());
            case LEAVE -> warnOnce(player, "another player owns FoxAblaze's Raphael");
            case CONVERT -> {
                ManasSkill skill = SkillAPI.getSkillRegistry().get(m.to());
                if (skill != null && EvoGrant.learnFree(player, skill)) {
                    SkillAPI.getSkillsFrom(player).forgetSkill(m.from()); // only after the new one is learned: a veto leaves the player with the old one
                } else {
                    warnOnce(player, "the learn of FoxAblaze's Raphael was refused");
                }
            }
        }
    }

    private static void warnOnce(ServerPlayer player, String why) {
        if (WARNED.add(player.getUUID())) {
            LOGGER.warn("{} still holds Great Sage's Raphael: {}.", player.getGameProfile().getName(), why);
        }
    }
}
