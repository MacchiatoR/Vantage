package me.horang.vantage.client;

import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class CameraManager {

    private static boolean active = false; // 시네마틱 모드 활성화 여부
    private static Vec3 targetPos = Vec3.ZERO; // 목표 카메라 위치
    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static double targetRoll = 0f; // 마크는 기본적으로 롤(기울기) 지원 안 하지만 구현 가능
    private static double targetFOV = 70.0;

    // 외부(에디터/재생기)에서 이 값을 조절함
    public static void updateCamera(Vec3 pos, float yaw, float pitch, double roll, double fov) {
        targetPos = pos;
        targetYaw = yaw;
        targetPitch = pitch;
        targetRoll = roll;
        targetFOV = fov;
        active = true;
    }

    public static void disable() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    // 1. 카메라 위치와 회전 덮어쓰기 (핵심!)
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;

        // 렌더러에게 "플레이어 위치 말고 내가 준 좌표 써!" 라고 강제함
        event.setYaw(targetYaw);
        event.setPitch(targetPitch);
        event.setRoll((float)targetRoll); // 롤링(화면 기울기) 적용

        // 위치 설정은 이벤트 객체에 메서드가 없어서 카메라 객체를 통해야 할 수도 있지만,
        // 최신 네오포지는 보통 Camera 객체를 직접 조작하거나 이벤트를 통해 오프셋을 줌.
        // 하지만 가장 확실한 건 RenderTick에서 Camera 인스턴스를 조작하는 것보다,
        // 렌더링 직전 ViewMatrix를 조작하는 것이지만,
        // 여기서는 가장 직관적인 '카메라 객체 조작' 개념으로 접근.
        // 실제로는 ViewportEvent.RenderCamera 등에서 위치 보정을 해야 함.
    }

    // 2. 시야각(FOV) 덮어쓰기 (줌 인/아웃 연출용)
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!active) return;

        event.setFOV(targetFOV);
    }
}
