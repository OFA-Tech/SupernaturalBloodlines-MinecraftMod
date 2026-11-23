package net.ofatech.supernaturalbloodlines.core.wheel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The data file {@code tensura_kinds.json}: where the rule for a Tensura skill's kind gets one wrong, a line here says what it really is ({@code overrides}), and the
 * tiers of one power are listed lowest to highest ({@code chains}: only the highest one the player owns is shown). Pure.
 */
public final class TensuraKinds {
    public static final TensuraKinds EMPTY = new TensuraKinds(Map.of(), List.of());

    private final Map<ResourceLocation, PowerKind> overrides;
    private final List<List<ResourceLocation>> chains;

    private TensuraKinds(Map<ResourceLocation, PowerKind> overrides, List<List<ResourceLocation>> chains) {
        this.overrides = overrides;
        this.chains = chains;
    }

    /** @throws IllegalArgumentException for an unknown kind, an id that does not parse, or an id in two chains */
    public static TensuraKinds parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            Map<ResourceLocation, PowerKind> overrides = new HashMap<>();
            if (root.has("overrides")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("overrides").entrySet()) {
                    PowerKind kind;
                    try {
                        kind = PowerKind.valueOf(e.getValue().getAsString());
                    } catch (IllegalArgumentException bad) {
                        throw new IllegalArgumentException("unknown kind '" + e.getValue().getAsString() + "' for " + e.getKey());
                    }
                    overrides.put(id(e.getKey()), kind);
                }
            }
            List<List<ResourceLocation>> chains = new ArrayList<>();
            Set<ResourceLocation> seen = new HashSet<>();
            if (root.has("chains")) {
                for (JsonElement chainEl : root.getAsJsonArray("chains")) {
                    List<ResourceLocation> chain = new ArrayList<>();
                    for (JsonElement idEl : (JsonArray) chainEl) {
                        ResourceLocation id = id(idEl.getAsString());
                        if (!seen.add(id)) {
                            throw new IllegalArgumentException(id + " is in two chains");
                        }
                        chain.add(id);
                    }
                    chains.add(List.copyOf(chain));
                }
            }
            return new TensuraKinds(Map.copyOf(overrides), List.copyOf(chains));
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("not a valid tensura_kinds.json: " + e.getMessage(), e);
        }
    }

    private static ResourceLocation id(String s) {
        ResourceLocation id = ResourceLocation.tryParse(s);
        if (id == null) {
            throw new IllegalArgumentException("bad id " + s);
        }
        return id;
    }

    /** The kind the file forces for this skill, if any. */
    public java.util.Optional<PowerKind> override(ResourceLocation skill) {
        return java.util.Optional.ofNullable(overrides.get(skill));
    }

    /** True when a higher tier of this skill's chain is owned: the lower one is not listed. */
    public boolean hiddenByHigherTier(ResourceLocation skill, Set<ResourceLocation> owned) {
        for (List<ResourceLocation> chain : chains) {
            int at = chain.indexOf(skill);
            if (at >= 0) {
                for (int i = at + 1; i < chain.size(); i++) {
                    if (owned.contains(chain.get(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
