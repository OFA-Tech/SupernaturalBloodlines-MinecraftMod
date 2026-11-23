package net.ofatech.supernaturalbloodlines.parts.hud.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.Dedup;

/**
 * One power the player has, as the wheel needs it. {@code mastery} is -1 for none. {@code modeName} is the current mode's name (empty for a power with no
 * modes) and {@code modeNames} lists every mode in order. The codec bounds every list and string: a decode over a limit throws.
 */
public record WheelEntry(PowerRef ref, PowerGroup group, PowerKind kind, Component name, Component modeName, List<Component> modeNames, ResourceLocation icon,
                         PowerState state, boolean slottable, float mastery, String groupId, Component coverNote) implements Dedup.Item, net.ofatech.supernaturalbloodlines.core.wheel.WheelModel.Entry {
    public static final int MAX_STRING = 256;
    public static final int MAX_MODE_NAMES = 16;

    public static final StreamCodec<RegistryFriendlyByteBuf, WheelEntry> STREAM_CODEC = StreamCodec.of(WheelEntry::encode, WheelEntry::decode);

    private static final StreamCodec<io.netty.buffer.ByteBuf, String> STRING = ByteBufCodecs.stringUtf8(MAX_STRING);

    public WheelEntry {
        modeNames = List.copyOf(modeNames);
        java.util.Objects.requireNonNull(groupId);
        java.util.Objects.requireNonNull(coverNote);
    }

    /** An entry that stands for a group (or none) and has no note of what else it covers. */
    public WheelEntry(PowerRef ref, PowerGroup group, PowerKind kind, Component name, Component modeName, List<Component> modeNames, ResourceLocation icon, PowerState state,
                      boolean slottable, float mastery, String groupId) {
        this(ref, group, kind, name, modeName, modeNames, icon, state, slottable, mastery, groupId, Component.empty());
    }

    /** An entry that stands for one power only (it is in no group). */
    public WheelEntry(PowerRef ref, PowerGroup group, PowerKind kind, Component name, Component modeName, List<Component> modeNames, ResourceLocation icon, PowerState state,
                      boolean slottable, float mastery) {
        this(ref, group, kind, name, modeName, modeNames, icon, state, slottable, mastery, "");
    }

    @Override
    public int modeCount() {
        return state.modeCount();
    }

    public WheelEntry withState(PowerState s) {
        return new WheelEntry(ref, group, kind, name, modeName, modeNames, icon, s, slottable, mastery, groupId, coverNote);
    }

    /** The same entry standing for the group of this id: several mods give this power and this is the one shown (rule C). */
    public WheelEntry withGroup(String id) {
        return new WheelEntry(ref, group, kind, name, modeName, modeNames, icon, state, slottable, mastery, id, coverNote);
    }

    /**
     * What else this entry stands for, for its tooltip and side panel: the other sides of the group that are hidden behind it, so nothing vanishes without a hint. Empty when it covers
     * nothing.
     */
    public WheelEntry withCoverNote(Component note) {
        return new WheelEntry(ref, group, kind, name, modeName, modeNames, icon, state, slottable, mastery, groupId, note);
    }

    private static void encode(RegistryFriendlyByteBuf buf, WheelEntry e) {
        STRING.encode(buf, e.ref.key());
        ByteBufCodecs.VAR_INT.encode(buf, e.group.ordinal());
        ByteBufCodecs.VAR_INT.encode(buf, e.kind.ordinal());
        ComponentSerialization.STREAM_CODEC.encode(buf, e.name);
        ComponentSerialization.STREAM_CODEC.encode(buf, e.modeName);
        ByteBufCodecs.VAR_INT.encode(buf, e.modeNames.size());
        for (Component c : e.modeNames) {
            ComponentSerialization.STREAM_CODEC.encode(buf, c);
        }
        STRING.encode(buf, e.icon.toString());
        PowerState s = e.state;
        buf.writeBoolean(s.on());
        ByteBufCodecs.VAR_LONG.encode(buf, s.cooldownEnd());
        ByteBufCodecs.VAR_INT.encode(buf, s.cooldownTotal());
        ByteBufCodecs.VAR_INT.encode(buf, s.lock().ordinal());
        ByteBufCodecs.VAR_INT.encode(buf, s.mode());
        ByteBufCodecs.VAR_INT.encode(buf, s.modeCount());
        buf.writeBoolean(e.slottable);
        buf.writeFloat(e.mastery);
        STRING.encode(buf, e.groupId);
        ComponentSerialization.STREAM_CODEC.encode(buf, e.coverNote);
    }

    private static <E extends Enum<E>> E enumAt(E[] values, int ordinal, String what) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new io.netty.handler.codec.DecoderException("bad " + what + " " + ordinal);
        }
        return values[ordinal];
    }

    private static WheelEntry decode(RegistryFriendlyByteBuf buf) {
        PowerRef ref = PowerRef.parse(STRING.decode(buf)).orElseThrow(() -> new io.netty.handler.codec.DecoderException("bad power ref"));
        PowerGroup group = enumAt(PowerGroup.values(), ByteBufCodecs.VAR_INT.decode(buf), "group");
        PowerKind kind = enumAt(PowerKind.values(), ByteBufCodecs.VAR_INT.decode(buf), "kind");
        Component name = ComponentSerialization.STREAM_CODEC.decode(buf);
        Component modeName = ComponentSerialization.STREAM_CODEC.decode(buf);
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        if (n < 0 || n > MAX_MODE_NAMES) {
            throw new io.netty.handler.codec.DecoderException("too many mode names " + n);
        }
        List<Component> modes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            modes.add(ComponentSerialization.STREAM_CODEC.decode(buf));
        }
        ResourceLocation icon = ResourceLocation.tryParse(STRING.decode(buf));
        if (icon == null) {
            throw new io.netty.handler.codec.DecoderException("bad icon");
        }
        boolean on = buf.readBoolean();
        long end = ByteBufCodecs.VAR_LONG.decode(buf);
        int total = ByteBufCodecs.VAR_INT.decode(buf);
        LockReason lock = enumAt(LockReason.values(), ByteBufCodecs.VAR_INT.decode(buf), "lock");
        int mode = ByteBufCodecs.VAR_INT.decode(buf);
        int modeCount = ByteBufCodecs.VAR_INT.decode(buf);
        boolean slottable = buf.readBoolean();
        float mastery = buf.readFloat();
        String groupId = STRING.decode(buf);
        Component coverNote = ComponentSerialization.STREAM_CODEC.decode(buf);
        return new WheelEntry(ref, group, kind, name, modeName, modes, icon, new PowerState(on, end, total, lock, mode, modeCount), slottable, mastery, groupId, coverNote);
    }
}
