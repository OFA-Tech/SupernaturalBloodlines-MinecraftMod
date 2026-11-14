package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.shared.tensura.LayerQuery;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;

/**
 * Which Tensura races may be hunters or werewolves, and which are bloodless. The race tags are ManasCore race tags
 * (data/tensura/tags/manascore_race/races), not entity-type tags. Werewolves is matched by faction id, so nothing
 * here links Werewolves classes.
 */
public final class FactionRaceRules {
    public static final String HUNTER_LEFT = "supernaturalbloodlines.vampirism_tensura.hunter_left";
    public static final String BITE_BLOODLESS = "supernaturalbloodlines.vampirism_tensura.bite_bloodless";
    public static final ResourceLocation WEREWOLF_FACTION_ID = ResourceLocation.parse("werewolves:werewolf");

    private static final TagKey<ManasRace> NO_BLOOD = raceTag("no_blood");

    private FactionRaceRules() {}

    private static TagKey<ManasRace> raceTag(String path) {
        return TagKey.create(RaceAPI.getRaceRegistryKey(), ResourceLocation.fromNamespaceAndPath("tensura", path));
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(FactionRaceRules::onCanJoinFaction);
        RaceEvents.SET_RACE.register(FactionRaceRules::onSetRace);
    }

    /** A player whose Tensura race has no blood: the vampire bite is refused (see VampirePlayerBiteMixin). Bitable races (the apex races) always have blood. */
    public static boolean isBloodless(Player player) {
        return noBlood(RaceAPI.getRaceFrom(player).getRace().orElse(null));
    }

    private static boolean noBlood(ManasRaceInstance race) {
        return race != null && race.is(NO_BLOOD) && !RaceTags.isBitable(race);
    }

    private static void onCanJoinFaction(PlayerFactionEvent.CanJoinFaction event) {
        if (!(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide) {
            return;
        }
        ManasRaceInstance race = RaceAPI.getRaceFrom(p).getRace().orElse(null);
        boolean apex = race != null && LayerQuery.isApexVampireType(p, race.getRaceId()); // R103: apex races of vampire-type species
        if (event.getFactionToJoin() == VReference.HUNTER_FACTION) {
            if (RaceTags.isUndeadOrDaemon(race) && !apex) {
                event.setBehavior(PlayerFactionEvent.CanJoinFaction.Behavior.DENY);
            }
        } else if (WEREWOLF_FACTION_ID.equals(event.getFactionToJoin().getID())
                && (RaceTags.isUndeadOrDaemon(race) || noBlood(race)) && !apex) {
            event.setBehavior(PlayerFactionEvent.CanJoinFaction.Behavior.DENY);
        }
    }

    // SET_RACE fires before the change, so the new race is the one to look at. Leaving the hunters does not
    // touch the race, so it cannot bounce back into the "vampire_sync" listeners and needs no guard.
    private static EventResult onSetRace(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace,
                                         boolean flag, Changeable<Boolean> changeable, Changeable<MutableComponent> message) {
        if (entity instanceof ServerPlayer p && !p.level().isClientSide && RaceTags.isUndeadOrDaemon(newRace)
                && !LayerQuery.isApexVampireType(p, newRace.getRaceId())) { // R103
            IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(p);
            if (handler.getCurrentFaction() == VReference.HUNTER_FACTION) {
                handler.leaveFaction(false);
                p.sendSystemMessage(Component.translatable(HUNTER_LEFT));
            }
        }
        return EventResult.pass();
    }
}
