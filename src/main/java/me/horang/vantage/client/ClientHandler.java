package me.horang.vantage.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.horang.vantage.ui.VantageEditorScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class ClientHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 키가 눌렸을 때 (PRESS) 동작
        // KeyBindings 클래스에 등록된 키와 매핑되는지 확인
        if (event.getKey() == KeyBindings.OPEN_EDITOR_KEY.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS) {
            toggleEditor();
        }
    }

    private static void toggleEditor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.screen instanceof VantageEditorScreen) {
            // [상태 1] 에디터가 켜져있다면 -> 끄기

            // 1. UI 닫기
            mc.setScreen(null);

            // 2. 카메라 하이재킹 해제 (원래 시점으로)
            VantageCamera.get().setActive(false);

            // 3. 1인칭으로 복귀 (편의상 1인칭으로 되돌림)
            mc.options.setCameraType(CameraType.FIRST_PERSON);

        } else if (mc.screen == null) {
            // [상태 2] 아무 창도 없다면 -> 에디터 켜기

            // 1. 에디터 UI 열기
            mc.setScreen(new VantageEditorScreen());

            // 2. 카메라 데이터 초기화 (현재 플레이어 눈 위치에서 시작)
            VantageCamera cam = VantageCamera.get();
            Vec3 eyePos = mc.player.getEyePosition(1.0f); // partialTick 1.0 기준 부드러운 위치

            cam.setTransform(
                    eyePos,
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    0f // Roll 초기화
            );

            // 3. 카메라 하이재킹 활성화
            cam.setActive(true);

            // 4. 3인칭 후방 시점으로 변경
            // (이걸 해야 카메라가 내 몸통 위치에 있어도 내 캐릭터가 렌더링됨)
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }
}