package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;

/**
 * Server to client: the ladder files the server accepted, as JSON text by race id, sent on login and after every data reload, with
 * the mods and parts the server had when it pruned them. The client parses with the server's sets, not its own: a client without a
 * mod would otherwise drop tasks the server counts and show a different ladder.
 */
public record LadderTablePayload(Map<String, String> files, List<String> mods, List<String> parts) implements CustomPacketPayload {
    public static final Type<LadderTablePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ladder_table"));

    private static final Gson GSON = new Gson();

    private static final StreamCodec<ByteBuf, List<String>> NAMES = ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(256));

    public static final StreamCodec<ByteBuf, LadderTablePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, String, String, Map<String, String>>map(HashMap::new,
                    ByteBufCodecs.stringUtf8(32), ByteBufCodecs.stringUtf8(1 << 17)), LadderTablePayload::files,
            NAMES, LadderTablePayload::mods,
            NAMES, LadderTablePayload::parts,
            LadderTablePayload::new);

    /** @param modLoaded the server's answer for a mod id; @param partEnabled the server's answer for a part config key */
    public static LadderTablePayload of(Map<String, JsonElement> sources, Predicate<String> modLoaded, Predicate<String> partEnabled) {
        Map<String, String> text = new HashMap<>();
        Set<String> mods = new TreeSet<>();
        Set<String> parts = new TreeSet<>();
        sources.forEach((race, json) -> {
            text.put(race, GSON.toJson(json));
            names(json, mods, parts);
        });
        mods.removeIf(m -> !modLoaded.test(m));
        parts.removeIf(p -> !partEnabled.test(p));
        return new LadderTablePayload(text, List.copyOf(mods), List.copyOf(parts));
    }

    /** Every mod id and part key the file mentions (task and rung {@code mod}, {@code mods}, {@code part}, {@code parts}). */
    private static void names(JsonElement e, Set<String> mods, Set<String> parts) {
        if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(x -> names(x, mods, parts));
        } else if (e.isJsonObject()) {
            e.getAsJsonObject().entrySet().forEach(en -> {
                JsonElement v = en.getValue();
                switch (en.getKey()) {
                    case "mod" -> mods.add(v.isJsonPrimitive() ? v.getAsString() : "");
                    case "part" -> parts.add(v.isJsonPrimitive() ? v.getAsString() : "");
                    case "mods" -> v.getAsJsonArray().forEach(x -> mods.add(x.getAsString()));
                    case "parts" -> v.getAsJsonArray().forEach(x -> parts.add(x.getAsString()));
                    default -> names(v, mods, parts);
                }
            });
        }
    }

    /** The table as the server pruned it: its mods and parts decide, whatever this client has installed. */
    public LadderTable.Parsed parseWithServerSets() {
        return LadderTable.parse(parsed(), mods::contains, parts::contains);
    }

    /** A file that is not JSON is dropped, never a crash: the table is then missing that race. */
    public Map<String, JsonElement> parsed() {
        Map<String, JsonElement> out = new HashMap<>();
        files.forEach((race, text) -> {
            try {
                out.put(race, JsonParser.parseString(text));
            } catch (RuntimeException e) {
                SupernaturalBloodlines.LOGGER.warn("Ladder {} from the server is not JSON, skipped", race);
            }
        });
        return out;
    }

    @Override
    public Type<LadderTablePayload> type() {
        return TYPE;
    }
}
