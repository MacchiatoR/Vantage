package me.horang.vantage.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.UUID;

public class NetworkManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    
    // [패킷 등록] Mod Event Bus에서 호출됨
    public static void register(final RegisterPayloadHandlersEvent event) {
        // .optional()을 꼭 붙여야 함
        final PayloadRegistrar registrar = event.registrar("1").optional();

        registrar.playToServer(
                C2SStringMessage.TYPE,
                C2SStringMessage.STREAM_CODEC,
                NetworkManager::handleC2S
        );

        registrar.playToClient(
                S2CJsonMessage.TYPE,
                S2CJsonMessage.STREAM_CODEC,
                NetworkManager::handleS2C
        );
    }

    private static void handleC2S(final C2SStringMessage message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer) {
                LOGGER.info("[Integrated Server] Received: " + message.message());
            }
        });
    }

    private static void handleS2C(final S2CJsonMessage message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            String jsonStr = message.jsonContent();
            LOGGER.info("[Client] Received JSON: " + jsonStr); // 디버그용

            try {
                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

                if (json.has("action")) {

                }

            } catch (Exception e) {
                System.err.println("[Client] JSON 파싱 실패: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // =================================================================
    // [핵심] 강제 전송 헬퍼 (검문소 우회)
    // =================================================================
    public static void sendToServer(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        try {
            // 1. 패킷 생성
            C2SStringMessage payload = new C2SStringMessage(message);
            ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(payload);

            // 2. 'Netty Connection' 객체를 직접 가져옴
            Connection rawConnection = mc.getConnection().getConnection();

            // 3. 네오포지/마크의 로직(리스너)을 거치지 않고 다이렉트 전송
            rawConnection.send(packet);

        } catch (Exception e) {
            System.err.println("[Vantage] Packet Send Error: " + e.getMessage());
        }
    }
}