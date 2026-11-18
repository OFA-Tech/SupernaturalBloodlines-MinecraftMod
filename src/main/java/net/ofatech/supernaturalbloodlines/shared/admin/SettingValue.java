package net.ofatech.supernaturalbloodlines.shared.admin;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What a client sends for a setting. The server checks it against the real spec; nothing here is trusted. */
public sealed interface SettingValue {
    int MAX_LINES = 64;
    int MAX_LINE = 256;
    int MAX_TOTAL = 4096;

    record Bool(boolean value) implements SettingValue {}

    /** A long, so a value past the int range reaches the check and is refused there instead of wrapping. */
    record Int(long value) implements SettingValue {}

    record Dbl(double value) implements SettingValue {}

    record Text(String value) implements SettingValue {}

    record Lines(List<String> value) implements SettingValue {
        public Lines {
            value = List.copyOf(value);
        }
    }

    StreamCodec<ByteBuf, List<String>> LINE_LIST = ByteBufCodecs.stringUtf8(MAX_LINE).apply(ByteBufCodecs.list(MAX_LINES));

    StreamCodec<ByteBuf, SettingValue> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SettingValue decode(ByteBuf buf) {
            return switch (buf.readByte()) {
                case 0 -> new Bool(ByteBufCodecs.BOOL.decode(buf));
                case 1 -> new Int(ByteBufCodecs.VAR_LONG.decode(buf));
                case 2 -> new Dbl(ByteBufCodecs.DOUBLE.decode(buf));
                case 3 -> new Text(ByteBufCodecs.stringUtf8(MAX_LINE).decode(buf));
                case 4 -> new Lines(new ArrayList<>(LINE_LIST.decode(buf)));
                default -> throw new io.netty.handler.codec.DecoderException("unknown setting value type");
            };
        }

        @Override
        public void encode(ByteBuf buf, SettingValue v) {
            switch (v) {
                case Bool b -> {
                    buf.writeByte(0);
                    ByteBufCodecs.BOOL.encode(buf, b.value());
                }
                case Int i -> {
                    buf.writeByte(1);
                    ByteBufCodecs.VAR_LONG.encode(buf, i.value());
                }
                case Dbl d -> {
                    buf.writeByte(2);
                    ByteBufCodecs.DOUBLE.encode(buf, d.value());
                }
                case Text t -> {
                    buf.writeByte(3);
                    ByteBufCodecs.stringUtf8(MAX_LINE).encode(buf, t.value());
                }
                case Lines l -> {
                    buf.writeByte(4);
                    LINE_LIST.encode(buf, l.value());
                }
            }
        }
    };
}
