package me.horang.vantage.data;

import me.horang.vantage.client.VantageCamera;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class PlaybackManager {

    private static boolean isPlaying = false;
    private static SceneData.Connection currentConnection = null;

    private static float elapsedTime = 0f; // 누적 재생 시간 (초)
    private static long lastFrameTime = 0; // 델타 타임 계산용 (나노초)

    // 재생 시작
    public static void play() {
        SceneData data = SceneData.get();
        if (data.getNodes().isEmpty()) return;

        SceneData.Node startNode = data.getSelectedNode();
        if (startNode == null) startNode = data.getNodes().get(0);

        currentConnection = data.getConnectionFrom(startNode);

        if (currentConnection != null) {
            isPlaying = true;
            elapsedTime = 0f;
            lastFrameTime = System.nanoTime(); // [New] 현재 시간 초기화

            VantageCamera.get().setActive(true);
            updateCamera(0f);
        }
    }

    public static void stop() {
        isPlaying = false;
        currentConnection = null;
    }

    public static boolean isPlaying() { return isPlaying; }

    /**
     * [Core Fix] 틱(ClientTick) 대신 렌더 프레임(RenderFrameEvent)마다 업데이트
     * 이렇게 하면 FPS만큼(예: 144Hz) 카메라 위치를 갱신하므로 극도로 부드럽습니다.
     */
    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Pre event) {
        if (!isPlaying || currentConnection == null) return;

        // 1. 델타 타임 계산 (실제 흐른 시간, 초 단위)
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = currentTime;

        // 프레임 드랍 방지 (최대 0.1초 제한)
        if (deltaTime > 0.1f) deltaTime = 0.1f;

        elapsedTime += deltaTime;

        // 2. 구간 종료 체크
        if (elapsedTime >= currentConnection.duration) {
            float overflow = elapsedTime - currentConnection.duration;
            SceneData.Node nextNode = currentConnection.end;
            currentConnection = SceneData.get().getConnectionFrom(nextNode);

            if (currentConnection != null) {
                elapsedTime = overflow;
            } else {
                // 종료 시 정확히 마지막 위치로 이동 후 정지
                elapsedTime = (currentConnection != null) ? currentConnection.duration : 0;
                if (currentConnection == null) {
                    updateCamera(1.0f); // 마지막 노드 위치 적용
                    stop();
                    return;
                }
            }
        }

        // 3. 보간 비율 계산
        float t = elapsedTime / currentConnection.duration;
        updateCamera(t);
    }

    private static void updateCamera(float t) {
        if (currentConnection == null) return;

        SceneData data = SceneData.get();
        SceneData.Node start = currentConnection.start;
        SceneData.Node end = currentConnection.end;

        // [Fix 1] 위치 계산: getPointOnConnection 사용
        // 이 메서드 내부에서 Easing(속도 조절)과 Interpolation(직선/곡선)을 모두 처리합니다.
        Vec3 pos = SceneData.getPointOnConnection(currentConnection, t, data);

        // [Fix 2] 회전 보간: Roll 추가 및 Easing 적용 고려
        // (참고: SceneData.getPointOnConnection은 위치에 Easing을 적용하지만,
        //  여기서 t는 선형입니다. 회전 속도도 이동 속도와 맞추려면 Easing된 t가 필요합니다.
        //  완벽한 동기화를 위해선 SceneData의 applyEasing을 public으로 열어서 사용해야 하지만,
        //  여기선 일단 선형 t를 사용하여 부드럽게 회전시킵니다.)

        float yaw = rotLerp(start.yaw, end.yaw, t);
        float pitch = rotLerp(start.pitch, end.pitch, t);
        float roll = rotLerp(start.roll, end.roll, t); // [New] Roll도 보간

        // 카메라 업데이트
        VantageCamera.get().setTransform(pos, yaw, pitch, roll);
    }

    private static float rotLerp(float start, float end, float t) {
        float diff = end - start;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return start + t * diff;
    }
}