package me.horang.vantage.util;

import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import me.horang.vantage.client.VantageCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;

public class RaycastHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 마우스가 가리키는 지점을 반환합니다.
     * 1. 블록(땅)이 있으면 그 위치 반환.
     * 2. 허공이면 카메라 앞 10m 지점 반환 (허공 배치 가능하게).
     */
    public static Vec3 getRaycastHit(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        RayData ray = calculateRay(mouseX, mouseY, mc);
        if (ray == null) return null;

        Vec3 start = ray.start;
        Vec3 rayDir = new Vec3(ray.dir.x, ray.dir.y, ray.dir.z);
        double maxDist = 100.0;
        Vec3 end = start.add(rayDir.scale(maxDist));

        // 1. 블록 충돌 검사 (ClipContext)
        BlockHitResult hitResult = mc.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER, // 충돌체 기준
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (hitResult.getType() != HitResult.Type.MISS) {
            // 땅에 닿았으면 그 위치 반환
            return hitResult.getLocation();
        }

        // 2. 허공이면 카메라 앞 10m 지점 반환 (Ghost Node가 보이게 하기 위함)
        return start.add(rayDir.scale(10.0));
    }

    private static class RayData {
        Vec3 start;
        Vector3f dir;
        public RayData(Vec3 start, Vector3f dir) {
            this.start = start;
            this.dir = dir;
        }
    }

    private static RayData calculateRay(double mouseX, double mouseY, Minecraft mc) {
        int winWidth = mc.getWindow().getWidth();
        int winHeight = mc.getWindow().getHeight();

        // 1. NDC 변환
        float ndcX = (float) (2.0 * mouseX / winWidth - 1.0);
        float ndcY = (float) (1.0 - 2.0 * mouseY / winHeight);

        // 2. Projection Matrix (FOV 적용)
        // VantageCamera의 FOV를 가져와야 정확함
        double fov = VantageCamera.get().getInterpolatedFov(mc.getTimer().getGameTimeDeltaPartialTick(true));
        float aspectRatio = (float) winWidth / winHeight;

        Matrix4f projection = new Matrix4f();
        projection.setPerspective((float) Math.toRadians(fov), aspectRatio, 0.05f, 1000.0f);

        // 3. View Matrix (VantageCamera 회전 적용)
        // 중요: mc.gameRenderer 대신 VantageCamera의 회전값을 써야 함!
        float xRot = VantageCamera.get().getInterpolatedXRot(mc.getTimer().getGameTimeDeltaPartialTick(true));
        float yRot = VantageCamera.get().getInterpolatedYRot(mc.getTimer().getGameTimeDeltaPartialTick(true));
        float roll = VantageCamera.get().getInterpolatedRoll(mc.getTimer().getGameTimeDeltaPartialTick(true));

        Matrix4f view = new Matrix4f();
        view.rotate((float) Math.toRadians(roll), 0.0f, 0.0f, 1.0f); // Roll
        view.rotate((float) Math.toRadians(xRot), 1.0f, 0.0f, 0.0f); // Pitch
        view.rotate((float) Math.toRadians(yRot + 180.0f), 0.0f, 1.0f, 0.0f); // Yaw

        // 4. 역행렬 계산
        Matrix4f invViewProj = new Matrix4f(projection).mul(view).invert();

        // 5. Ray Direction 계산
        Vector4f nearVec = new Vector4f(ndcX, ndcY, -1.0f, 1.0f).mul(invViewProj);
        Vector4f farVec = new Vector4f(ndcX, ndcY, 1.0f, 1.0f).mul(invViewProj);

        if (nearVec.w != 0) nearVec.div(nearVec.w);
        if (farVec.w != 0) farVec.div(farVec.w);

        Vector3f rayDir = new Vector3f(farVec.x - nearVec.x, farVec.y - nearVec.y, farVec.z - nearVec.z).normalize();

        // 6. 시작점은 VantageCamera 위치
        Vec3 camPos = VantageCamera.get().getInterpolatedPosition(mc.getTimer().getGameTimeDeltaPartialTick(true));

        return new RayData(camPos, rayDir);
    }

    // Ray의 시작점과 방향을 담는 클래스 (public으로 변경)
    public static class Ray {
        public final Vec3 origin;
        public final Vec3 dir;
        public Ray(Vec3 origin, Vec3 dir) { this.origin = origin; this.dir = dir; }
    }

    /**
     * 현재 마우스 좌표에 해당하는 Ray를 계산하여 반환합니다.
     */
    public static Ray getMouseRay(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int winWidth = mc.getWindow().getWidth();
        int winHeight = mc.getWindow().getHeight();

        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 camPos = VantageCamera.get().getInterpolatedPosition(partialTick);
        float xRot = VantageCamera.get().getInterpolatedXRot(partialTick);
        float yRot = VantageCamera.get().getInterpolatedYRot(partialTick);
        float roll = VantageCamera.get().getInterpolatedRoll(partialTick);
        double fov = VantageCamera.get().getInterpolatedFov(partialTick);

        // 1. NDC 변환
        float ndcX = (float) (2.0 * mouseX / winWidth - 1.0);
        float ndcY = (float) (1.0 - 2.0 * mouseY / winHeight);

        // 2. 행렬 계산 (표준 JOML 방식)
        float aspectRatio = (float) winWidth / winHeight;

        // Projection
        Matrix4f projection = new Matrix4f();
        projection.perspective((float) Math.toRadians(fov), aspectRatio, 0.05f, 1000.0f);

        // View (Camera Rotation)
        // MC 좌표계: +Z=South, +Y=Up.
        // View Matrix는 카메라 변환의 역행렬입니다.
        // 순서: Roll -> Pitch -> Yaw
        Matrix4f view = new Matrix4f();
        view.identity();
        view.rotate(Axis.ZP.rotationDegrees(roll));
        view.rotate(Axis.XP.rotationDegrees(xRot));
        view.rotate(Axis.YP.rotationDegrees(yRot + 180.0f));

        // 3. 결합 및 역행렬
        Matrix4f invViewProj = new Matrix4f(projection).mul(view).invert();

        // 4. Unproject
        Vector4f nearVec = new Vector4f(ndcX, ndcY, -1.0f, 1.0f).mul(invViewProj);
        Vector4f farVec = new Vector4f(ndcX, ndcY, 1.0f, 1.0f).mul(invViewProj);

        if (nearVec.w() != 0) nearVec.div(nearVec.w());
        if (farVec.w() != 0) farVec.div(farVec.w());

        // 5. 방향 계산
        Vector3f dir = new Vector3f(farVec.x() - nearVec.x(), farVec.y() - nearVec.y(), farVec.z() - nearVec.z()).normalize();

        return new Ray(camPos, new Vec3(dir));
    }


    /**
     * Ray와 선분(Start-End) 사이의 최소 거리를 구합니다.
     * 거리가 thickness보다 작으면 충돌로 간주할 수 있습니다.
     */
    public static double getDistanceFromLine(Ray ray, Vec3 p1, Vec3 p2) {
        // 벡터 연산: u = p2 - p1
        Vec3 u = p2.subtract(p1);
        Vec3 v = ray.dir;
        Vec3 w = p1.subtract(ray.origin);

        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
        double e = v.dot(w);
        double D = a * c - b * b;

        double sc, tc;

        // 평행한 경우
        if (D < 1e-8) {
            sc = 0.0;
            tc = (b > c ? d / b : e / c);
        } else {
            sc = (b * e - c * d) / D;
            tc = (a * e - b * d) / D;
        }

        // 선분 안에 있는지 클램핑
        // Ray는 무한하므로 tc는 클램핑하지 않아도 되지만(보통 양수), 선분(sc)은 0~1 사이여야 함.
        if (sc < 0) sc = 0;
        else if (sc > 1) sc = 1;

        // 다시 tc 계산 (sc가 변했을 수 있으므로)
        // 여기선 단순화를 위해 근사치 거리 계산 로직 사용

        Vec3 closestOnLine = p1.add(u.scale(sc));
        // Ray 상의 가장 가까운 점 (수직 내리기)
        // t = (closestOnLine - origin) . dir
        double t = closestOnLine.subtract(ray.origin).dot(ray.dir);
        Vec3 closestOnRay = ray.origin.add(ray.dir.scale(t));

        return closestOnLine.distanceTo(closestOnRay);
    }
}