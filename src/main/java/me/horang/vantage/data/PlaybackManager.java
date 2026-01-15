package me.horang.vantage.data;

import me.horang.vantage.client.VantageCamera;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class PlaybackManager {

    private static boolean isPlaying = false;
    private static SceneData.Connection currentConnection = null;
    private static float elapsedTime = 0f;

    // 재생 시작
    public static void play() {
        SceneData data = SceneData.get();
        if (data.getNodes().isEmpty()) return;

        // 1. 시작점: 선택된 노드 혹은 첫 번째 노드
        SceneData.Node startNode = data.getSelectedNode();
        if (startNode == null) startNode = data.getNodes().get(0);

        // 2. 연결선 찾기
        currentConnection = data.getConnectionFrom(startNode);

        if (currentConnection != null) {
            isPlaying = true;
            elapsedTime = 0f;
            VantageCamera.get().setActive(true); // 카메라 강제 제어 시작

            // 시작 순간 카메라 위치 잡기
            updateCamera(0f);
        }
    }

    public static void stop() {
        isPlaying = false;
        currentConnection = null;
    }

    public static boolean isPlaying() { return isPlaying; }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) { // [Fix] .Post 이벤트 사용
        if (!isPlaying || currentConnection == null) return;

        // 프레임당 시간 더하기 (초 단위)
        elapsedTime += 0.05f; // 1 tick = 0.05 sec

        if (elapsedTime >= currentConnection.duration) {
            // 해당 구간 종료 -> 다음 구간으로
            float overflow = elapsedTime - currentConnection.duration;
            SceneData.Node nextNode = currentConnection.end;

            // 다음 연결선 찾기
            currentConnection = SceneData.get().getConnectionFrom(nextNode);

            if (currentConnection != null) {
                elapsedTime = overflow; // 남은 시간 이월
            } else {
                // 더 이상 갈 곳이 없음 -> 종료
                stop();
                return;
            }
        }

        // 보간 계산
        float t = elapsedTime / currentConnection.duration;
        updateCamera(t);
    }

    private static void updateCamera(float t) {
        if (currentConnection == null) return;

        SceneData.Node start = currentConnection.start;
        SceneData.Node end = currentConnection.end;

        Vec3 pos;

        // [Fix] Tension이 0보다 크면 곡선 이동, 아니면 직선 이동
        if (currentConnection.tension > 0.001f) {
            SceneData data = SceneData.get();

            // P0: 이전 노드 (없으면 현재 시작점 사용)
            SceneData.Connection prevConn = data.getConnectionTo(start);
            Vec3 p0 = (prevConn != null) ? prevConn.start.position : start.position;

            // P3: 다음 노드 (없으면 현재 끝점 사용)
            SceneData.Connection nextConn = data.getConnectionFrom(end);
            Vec3 p3 = (nextConn != null) ? nextConn.end.position : end.position;

            // 스플라인 계산
            pos = SceneData.getSplinePoint(t, p0, start.position, end.position, p3);
        } else {
            // 직선 이동 (Linear Interpolation)
            pos = start.position.lerp(end.position, t);
        }

        // 회전 보간 (회전은 선형 유지)
        float yaw = rotLerp(start.yaw, end.yaw, t);
        float pitch = rotLerp(start.pitch, end.pitch, t);

        VantageCamera.get().setTransform(pos, yaw, pitch, 0f);
    }

    // 각도 보간 유틸
    private static float rotLerp(float start, float end, float t) {
        float diff = end - start;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return start + t * diff;
    }
}