package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Server to client: the ladder files the server accepted, as JSON text by race id, sent on login and after every data reload. The
 * client parses them with its own mods and parts, so the hub shows the same tasks the server counts.
 */
public record LadderTablePayload(Map<String, String> files) implements CustomPacketPayload {
    public static final Type<LadderTablePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ladder_table"));

    private static final Gson GSON = new Gson();

    public static final StreamCodec<ByteBuf, LadderTablePayload> STREAM_CODEC = ByteBufCodecs.<ByteBuf, String, String, Map<String, String>>map(HashMap::new,
            ByteBufCodecs.stringUtf8(32), ByteBufCodecs.stringUtf8(1 << 17)).map(LadderTablePayload::new, LadderTablePayload::files);

    public static LadderTablePayload of(Map<String, JsonElement> sources) {
        Map<String, String> text = new HashMap<>();
        sources.forEach((race, json) -> text.put(race, GSON.toJson(json)));
        return new LadderTablePayload(text);
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
