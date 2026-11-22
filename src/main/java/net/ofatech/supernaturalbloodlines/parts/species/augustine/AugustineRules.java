package net.ofatech.supernaturalbloodlines.parts.species.augustine;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.AugustineBloodRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;

/**
 * What being an Augustine means in play (R200, R203): a little stronger than a vampire (+10% attack, +2 health, both configurable) and a very different
 * eater: triple the blood from a vampire, a third from anyone else. Garlic and holy water are the skills' (see {@link AugustineGifts}).
 */
public final class AugustineRules {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "augustine");

    private AugustineRules() {
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> sync(p));
        // the modifiers are saved with the player; these only repair gaps (and follow a changed config)
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((de.teamlapen.vampirism.api.event.PlayerFactionEvent.FactionLevelChanged e) -> syncIfPlayer(e.getPlayer().getPlayer()));
        // LOWEST: after the bite's other rules (BloodEffects, the vervain refusal, the ash dagger's starving), so a bite something emptied stays 0
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, AugustineRules::noteVictimBlood);
        // an entry is made and used within one bite; a bite that was cancelled in between must not leave one behind
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> VICTIM_BEFORE.clear());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> VICTIM_BEFORE.clear());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AugustineRules::onDrinkBlood);
        AugustineGifts.register();
    }

    public static boolean isAugustine(net.minecraft.world.entity.player.Player p) {
        return BloodlineAccess.get(p).species() == Species.AUGUSTINE;
    }

    /** Idempotent: makes the player's attributes match their species and the config. */
    public static void sync(ServerPlayer p) {
        // a species of the vampire side: out of the vampire faction (Vampirism's own cure, a command) the bonuses go at once, whatever the species still says
        boolean on = isAugustine(p) && VampirismAPI.factionPlayerHandler(p).isInFaction(VReference.VAMPIRE_FACTION);
        apply(p, Attributes.MAX_HEALTH, SpeciesConfig.augustineExtraHealth(), AttributeModifier.Operation.ADD_VALUE, on);
        apply(p, Attributes.ATTACK_DAMAGE, SpeciesConfig.augustineAttackBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
    }

    private static void syncIfPlayer(net.minecraft.world.entity.player.Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, boolean on) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        AttributeModifier now = a.getModifier(MODIFIER_ID);
        if (on && amount > 0) {
            if (now == null || now.amount() != amount) {
                a.removeModifier(MODIFIER_ID);
                a.addPermanentModifier(new AttributeModifier(MODIFIER_ID, amount, operation));
            }
        } else if (now != null) {
            a.removeModifier(MODIFIER_ID);
        }
    }

    /** Which bites of a feed are the first (the x3 is paid once per feed). Server thread only. */
    private static final AugustineBloodRules.FeedChain FEEDS = new AugustineBloodRules.FeedChain();
    private static final String NOTED = "supernaturalbloodlines:augustine_diet_noted";

    /** What a vampire PLAYER who is bitten by an Augustine had before the bite's other rules (the Predator's Thirst drain among them) touched them, by event. */
    private static final java.util.Map<BloodDrinkEvent.PlayerDrinkBloodEvent, Integer> VICTIM_BEFORE = new java.util.IdentityHashMap<>();

    private static int bloodOf(ServerPlayer p) {
        return VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
    }

    private static void noteVictimBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        if (e.getVampire().getRepresentingEntity() instanceof ServerPlayer p && isAugustine(p)
                && e.getBloodSource().getEntity().orElse(null) instanceof ServerPlayer victim && victim != p
                && VampirismAPI.factionRegistry().getFaction(victim) == VReference.VAMPIRE_FACTION) {
            VICTIM_BEFORE.put(e, bloodOf(victim));
        }
    }

    private static void onDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        Integer victimBefore = VICTIM_BEFORE.remove(e);
        if (!(e.getVampire().getRepresentingEntity() instanceof ServerPlayer p) || !isAugustine(p)) {
            return;
        }
        LivingEntity source = e.getBloodSource().getEntity().orElse(null);
        boolean vampire = source != null && VampirismAPI.factionRegistry().getFaction(source) == VReference.VAMPIRE_FACTION; // a hybrid player counts (R2)
        // the x3 is the first bite of a feed's: the bites that follow it within the chain are paid as Vampirism gives them (a third from anyone else stays a third on every bite)
        int given = e.getAmount();
        // only a bite that gives something on a vampire counts as part of a feed: a refused bite must not start (or renew) the window of the next one
        boolean first = FEEDS.startsFeed(given, vampire, p.getUUID(), vampire ? source.getUUID() : p.getUUID(), p.level().getGameTime());
        double fromVampire = first ? SpeciesConfig.augustineBloodFromVampire() : Math.min(1.0, SpeciesConfig.augustineBloodFromVampire());
        int scaled = AugustineBloodRules.scale(given, vampire, fromVampire, SpeciesConfig.augustineBloodFromOther(), p.getRandom().nextDouble());
        e.setAmount(scaled);
        // nothing is made from nothing: a vampire PLAYER who is bitten pays all that the biter gets (what the drain already took counts, and what they cannot pay is not given),
        // so two Augustines cannot feed each other
        if (victimBefore != null && source instanceof ServerPlayer victim) {
            int drained = victimBefore - bloodOf(victim);
            if (scaled > drained) {
                VampirismAPI.vampirePlayer(victim).useBlood(scaled - drained, true);
            }
            e.setAmount(Math.min(scaled, Math.max(0, victimBefore - bloodOf(victim))));
            scaled = e.getAmount();
        }
        if (given > 0 && scaled != given && !p.getPersistentData().getBoolean(NOTED)) {
            p.getPersistentData().putBoolean(NOTED, true);
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("supernaturalbloodlines.augustine.diet"), true);
        }
    }

    /** For tests: makes the note a bite's first listener makes. */
    public static void noteVictimBloodForTest(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        noteVictimBlood(e);
    }

    /** For tests: how many bites are waiting to be settled. */
    public static int pendingBitesForTest() {
        return VICTIM_BEFORE.size();
    }

    /** For tests: what a tick end does to the notes. */
    public static void clearPendingBitesForTest() {
        VICTIM_BEFORE.clear();
    }

    /** For tests: forgets which bites started a feed. */
    public static void forgetFeedsForTest() {
        FEEDS.clear();
    }
}
