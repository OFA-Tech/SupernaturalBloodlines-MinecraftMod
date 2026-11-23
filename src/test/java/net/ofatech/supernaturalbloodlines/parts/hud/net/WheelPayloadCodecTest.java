package net.ofatech.supernaturalbloodlines.parts.hud.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import org.junit.jupiter.api.Test;

class WheelPayloadCodecTest {
    private static RegistryFriendlyByteBuf buf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private static WheelEntry entry(int i) {
        return new WheelEntry(new PowerRef(PowerSource.TENSURA, ResourceLocation.parse("tensura:skill_" + i)), PowerGroup.MAGIC, PowerKind.HELD,
                Component.literal("Name " + i), Component.literal("Mode"), List.of(Component.literal("A"), Component.literal("B")),
                ResourceLocation.parse("tensura:textures/skill/x.png"), new PowerState(true, 5_000_000_000L, 100, LockReason.NEEDS_SLOT, 1, 2), true, 0.5f);
    }

    @Test
    void entryRoundTripsThroughARegistryBuffer() {
        WheelEntry in = entry(3);
        RegistryFriendlyByteBuf b = buf();
        WheelEntry.STREAM_CODEC.encode(b, in);
        WheelEntry out = WheelEntry.STREAM_CODEC.decode(b);
        assertEquals(in.ref(), out.ref());
        assertEquals(in.group(), out.group());
        assertEquals(in.kind(), out.kind());
        assertEquals(in.name().getString(), out.name().getString());
        assertEquals(in.modeName().getString(), out.modeName().getString());
        assertEquals(2, out.modeNames().size());
        assertEquals("B", out.modeNames().get(1).getString());
        assertEquals(in.icon(), out.icon());
        assertEquals(in.state(), out.state());
        assertEquals(in.slottable(), out.slottable());
        assertEquals(in.mastery(), out.mastery());
        assertEquals(0, b.readableBytes());
    }

    @Test
    void catalogueRoundTripsAndKeepsTheRevision() {
        WheelCataloguePayload in = new WheelCataloguePayload(7, List.of(entry(1), entry(2)));
        RegistryFriendlyByteBuf b = buf();
        WheelCataloguePayload.STREAM_CODEC.encode(b, in);
        WheelCataloguePayload out = WheelCataloguePayload.STREAM_CODEC.decode(b);
        assertEquals(7, out.revision());
        assertEquals(2, out.entries().size());
        assertEquals(in.entries().get(1).ref(), out.entries().get(1).ref());
    }

    @Test
    void catalogueOverTheEntryCapFailsToDecode() {
        List<WheelEntry> ok = new ArrayList<>();
        for (int i = 0; i < WheelCataloguePayload.MAX_ENTRIES; i++) {
            ok.add(entry(i));
        }
        RegistryFriendlyByteBuf fits = buf();
        WheelCataloguePayload.STREAM_CODEC.encode(fits, new WheelCataloguePayload(1, ok));
        assertEquals(512, WheelCataloguePayload.STREAM_CODEC.decode(fits).entries().size());

        ok.add(entry(513));
        RegistryFriendlyByteBuf b = buf();
        WheelCataloguePayload.STREAM_CODEC.encode(b, new WheelCataloguePayload(1, ok));
        assertThrows(DecoderException.class, () -> WheelCataloguePayload.STREAM_CODEC.decode(b));
    }

    @Test
    void tooManyModeNamesFailToDecode() {
        List<Component> names = new ArrayList<>();
        for (int i = 0; i < WheelEntry.MAX_MODE_NAMES + 1; i++) {
            names.add(Component.literal("m" + i));
        }
        WheelEntry e = entry(1);
        WheelEntry big = new WheelEntry(e.ref(), e.group(), e.kind(), e.name(), e.modeName(), names, e.icon(), e.state(), e.slottable(), e.mastery());
        RegistryFriendlyByteBuf b = buf();
        WheelEntry.STREAM_CODEC.encode(b, big);
        assertThrows(DecoderException.class, () -> WheelEntry.STREAM_CODEC.decode(b));
    }

    @Test
    void useAndModePayloadsRoundTrip() {
        UsePowerPayload use = new UsePowerPayload("tensura/tensura:coercion", 2, 99, true);
        ByteBuf b = Unpooled.buffer();
        UsePowerPayload.STREAM_CODEC.encode(b, use);
        assertEquals(use, UsePowerPayload.STREAM_CODEC.decode(b));
        SetPowerModePayload mode = new SetPowerModePayload("bloodline/supernaturalbloodlines:bite", -1);
        ByteBuf m = Unpooled.buffer();
        SetPowerModePayload.STREAM_CODEC.encode(m, mode);
        assertEquals(mode, SetPowerModePayload.STREAM_CODEC.decode(m));
        ByteBuf v = Unpooled.buffer();
        WheelViewPayload.STREAM_CODEC.encode(v, new WheelViewPayload(true));
        assertEquals(new WheelViewPayload(true), WheelViewPayload.STREAM_CODEC.decode(v));
    }

    @Test
    void stringOverTheLimitFailsToDecode() {
        ByteBuf raw = Unpooled.buffer();
        net.minecraft.network.FriendlyByteBuf f = new net.minecraft.network.FriendlyByteBuf(raw);
        f.writeUtf("x".repeat(300), 32767);
        f.writeVarInt(0);
        f.writeVarInt(0);
        f.writeBoolean(false);
        assertThrows(DecoderException.class, () -> UsePowerPayload.STREAM_CODEC.decode(raw));
        ByteBuf raw2 = Unpooled.buffer();
        new net.minecraft.network.FriendlyByteBuf(raw2).writeUtf("x".repeat(300), 32767);
        new net.minecraft.network.FriendlyByteBuf(raw2).writeVarInt(1);
        assertThrows(DecoderException.class, () -> SetPowerModePayload.STREAM_CODEC.decode(raw2));
    }
}
