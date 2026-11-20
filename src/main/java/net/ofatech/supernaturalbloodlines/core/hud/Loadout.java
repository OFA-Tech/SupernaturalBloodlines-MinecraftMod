package net.ofatech.supernaturalbloodlines.core.hud;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.Nullable;

/**
 * What the player put in the six Supernatural-mode slots: a Vampirism or Werewolves action id, or {@link #BITE}, or nothing. The ids are only names:
 * the server checks that an id may be assigned, and reads the id from here (never from a trigger packet). Duplicates are allowed. A new player has
 * nothing in any slot: Bite goes into slot 1 once, when the player first becomes a type that can bite ({@link #withBiteOffered}). Once cleared or moved
 * it is never put back on its own, so the saved form always lists all six slots.
 *
 * @param offered the actions that were already given to this player (or deliberately not, because there was no room): each is offered once and never again,
 *                so one the player cleared stays cleared. A save made before this existed has Bite in it, so a Bite the player cleared stays cleared.
 */
public record Loadout(List<Optional<ResourceLocation>> slots, Set<ResourceLocation> offered) {
    public static final int SLOTS = 6;
    /** The pseudo action our own Bite uses. */
    public static final ResourceLocation BITE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "bite");

    public static final Loadout DEFAULT = new Loadout(emptySlots(), Set.of());

    /** An empty string is an empty slot, so a cleared Bite survives a save; an id that does not parse is an empty slot too. */
    public static final Codec<Loadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("slots").forGetter(l -> Optional.of(l.slots().stream().map(s -> s.map(ResourceLocation::toString).orElse("")).toList())),
            Codec.STRING.listOf().optionalFieldOf("offered").forGetter(l -> Optional.of(l.offered().stream().map(ResourceLocation::toString).sorted().toList())),
            Codec.BOOL.optionalFieldOf("bite_offered").forGetter(l -> Optional.empty())
    ).apply(i, (saved, offered, legacyBite) -> saved.map(s -> fromStrings(s, offered, legacyBite)).orElse(DEFAULT)));

    public Loadout {
        if (slots.size() != SLOTS) {
            throw new IllegalArgumentException("a loadout has " + SLOTS + " slots, not " + slots.size());
        }
        slots = List.copyOf(slots);
        offered = Set.copyOf(offered);
    }

    private static List<Optional<ResourceLocation>> emptySlots() {
        List<Optional<ResourceLocation>> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            list.add(Optional.empty());
        }
        return list;
    }

    /** A save with a list of offered ids has it; one from before it existed has Bite offered (its flag, or the old default), so a cleared Bite stays cleared. */
    private static Loadout fromStrings(List<String> saved, Optional<List<String>> offered, Optional<Boolean> legacyBite) {
        List<Optional<ResourceLocation>> list = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            String text = i < saved.size() ? saved.get(i) : "";
            ResourceLocation id = text.isEmpty() ? null : ResourceLocation.tryParse(text);
            list.add(Optional.ofNullable(id));
        }
        Set<ResourceLocation> ids = offered.map(l -> l.stream().map(ResourceLocation::tryParse).filter(java.util.Objects::nonNull).collect(Collectors.toSet()))
                .orElseGet(() -> legacyBite.orElse(true) ? Set.of(BITE) : Set.of());
        return new Loadout(list, ids);
    }

    public boolean biteOffered() {
        return offered.contains(BITE);
    }

    public static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOTS;
    }

    public Optional<ResourceLocation> get(int slot) {
        return slots.get(slot);
    }

    public Loadout with(int slot, @Nullable ResourceLocation id) {
        List<Optional<ResourceLocation>> list = new ArrayList<>(slots);
        list.set(slot, Optional.ofNullable(id));
        return new Loadout(list, offered);
    }

    /**
     * Called when the player can bite: Bite goes into slot 1 the first time, and only if that slot is empty (a slot the player set is never overwritten).
     * Either way it is not offered again, so a Bite the player clears later stays cleared. The same loadout when it was offered before.
     */
    public Loadout withBiteOffered() {
        if (biteOffered()) {
            return this;
        }
        List<Optional<ResourceLocation>> list = new ArrayList<>(slots);
        if (list.get(0).isEmpty()) {
            list.set(0, Optional.of(BITE));
        }
        return new Loadout(list, withOffered(BITE));
    }

    /**
     * Called when the player may have {@code id} (the Alpha Roar for an Alpha): it goes into the first empty slot, once. With Bite waiting for slot 1 this is
     * called after it, so it takes the next empty one. A full loadout takes nothing and the id is spent anyway; a slot the player set is never overwritten.
     */
    public Loadout withActionOffered(ResourceLocation id) {
        if (offered.contains(id)) {
            return this;
        }
        List<Optional<ResourceLocation>> list = new ArrayList<>(slots);
        for (int i = 0; i < SLOTS; i++) {
            if (list.get(i).isEmpty()) {
                list.set(i, Optional.of(id));
                break;
            }
        }
        return new Loadout(list, withOffered(id));
    }

    private Set<ResourceLocation> withOffered(ResourceLocation id) {
        Set<ResourceLocation> next = new java.util.HashSet<>(offered);
        next.add(id);
        return next;
    }

    /**
     * The fallback for a server that runs the hud part without the supernatural part (so there is no hub to assign slots in): the empty slots 2 to 6 show,
     * in order, the unlocked actions that are not already in the list. Slot 1 stays Bite or empty. Never stored: server and client both compute it.
     */
    public Loadout withFallback(List<ResourceLocation> unlocked) {
        List<Optional<ResourceLocation>> list = new ArrayList<>(slots);
        int next = 0;
        for (int i = 1; i < SLOTS; i++) {
            if (list.get(i).isPresent()) {
                continue;
            }
            while (next < unlocked.size() && list.contains(Optional.of(unlocked.get(next)))) {
                next++;
            }
            if (next >= unlocked.size()) {
                break;
            }
            list.set(i, Optional.of(unlocked.get(next++)));
        }
        return new Loadout(list, offered);
    }

    public boolean contains(ResourceLocation id) {
        return slots.contains(Optional.of(id));
    }
}
