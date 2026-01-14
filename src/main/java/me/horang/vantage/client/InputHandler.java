package me.horang.vantage.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.horang.vantage.ui.VantageEditorScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class InputHandler {

    // 이동 속도
    private static final double FLY_SPEED = 0.5;
    private static final double FAST_MODIFIER = 2.0; // Ctrl 키 누르면 2배

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        VantageCamera cam = VantageCamera.get();

        // 1. 에디터 모드이고 카메라가 켜져있을 때만 작동
        if (cam.isActive() && mc.screen == null) {
            // 주의: mc.screen == null은 '채팅창이나 메뉴가 없을 때'를 의미함.
            // 하지만 우리는 'VantageEditorScreen'을 켜놓고 조작하고 싶지?
            // 그렇다면 조건은 아래와 같아야 함:
        }

        // 조건 수정: VantageEditorScreen이 켜져있거나, 모드가 활성화 상태일 때
        boolean isVantageOpen = mc.screen instanceof VantageEditorScreen;

        if (cam.isActive() && isVantageOpen) {
            handleCameraMovement(mc);
        }
    }

    private static void handleCameraMovement(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        VantageCamera cam = VantageCamera.get();

        double speed = FLY_SPEED;

        // Ctrl 누르면 가속
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)) {
            speed *= FAST_MODIFIER;
        }

        double forward = 0;
        double strafe = 0;
        double up = 0;

        // WASD 입력 감지 (GLFW 직접 호출)
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W)) forward -= 1;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S)) forward += 1;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A)) strafe += 1; // 왼쪽
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D)) strafe -= 1; // 오른쪽
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_SPACE)) up += 1;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)) up -= 1;

        // 이동 입력이 있을 때만 계산
        if (forward != 0 || strafe != 0 || up != 0) {
            cam.moveRelative(forward, up, strafe, speed);
        }

        // 마우스 회전은?
        // Screen 클래스에서는 마우스가 UI 조작에 쓰여야 하므로,
        // 보통 '우클릭을 누른 상태'에서만 화면이 돌아가게 하는 게 에디터 국룰임.
    }
}
