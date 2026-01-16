package me.horang.vantage.network;

import io.netty.buffer.ByteBuf;
import me.horang.vantage.Vantage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CJsonMessage(String jsonContent) implements CustomPacketPayload {

    public static final Type<S2CJsonMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Vantage.MODID, "s2c_json"));

    public static final StreamCodec<ByteBuf, S2CJsonMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, S2CJsonMessage::jsonContent,
            S2CJsonMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
