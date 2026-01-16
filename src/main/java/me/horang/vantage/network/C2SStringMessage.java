package me.horang.vantage.network;

import io.netty.buffer.ByteBuf;
import me.horang.vantage.Vantage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SStringMessage(String message) implements CustomPacketPayload {

    // 1.21.1: Type을 사용하여 식별
    public static final Type<C2SStringMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Vantage.MODID, "c2s_string"));

    // PacketCodec -> StreamCodec (ByteBufCodecs.STRING_UTF8 사용)
    public static final StreamCodec<ByteBuf, C2SStringMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, C2SStringMessage::message,
            C2SStringMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}