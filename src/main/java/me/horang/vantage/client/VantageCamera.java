package me.horang.vantage.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 시네마틱 카메라의 상태(위치, 회전, 줌)를 관리하는 싱글톤 클래스.
 * Mixin과 Renderer는 이 클래스의 데이터를 참조하여 화면을 그린다.
 */
public class VantageCamera {

    private static final VantageCamera INSTANCE = new VantageCamera();

    // --- State Fields ---
    private boolean active = false;       // 모드 활성화 여부
    private Vec3 position = Vec3.ZERO;    // 현재 카메라 위치 (World Coordinates)

    // Minecraft Rotation:
    // yRot (Yaw): 좌우 회전 (0=South, 90=West, 180=North, 270=East)
    // xRot (Pitch): 상하 회전 (-90=Up, 90=Down)
    private float yRot = 0f;
    private float xRot = 0f;
    private float roll = 0f;              // 화면 기울기 (Z축 회전)

    private double fov = 70.0;            // 시야각 (기본 70)

    // --- Singleton Access ---
    public static VantageCamera get() {
        return INSTANCE;
    }

    private VantageCamera() {
        // Prevent instantiation
    }

    // --- Getters & Setters ---

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Vec3 getPosition() { return position; }
    public float getYRot() { return yRot; }
    public float getXRot() { return xRot; }
    public float getRoll() { return roll; }
    public double getFov() { return fov; }

    /**
     * 외부(재생기, 에디터)에서 카메라의 전체 상태를 강제로 설정할 때 사용
     */
    public void setTransform(Vec3 pos, float yRot, float xRot, float roll) {
        this.position = pos;
        this.yRot = yRot;
        this.xRot = xRot;
        this.roll = roll;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }

    // --- Movement Logic (에디터 조작용 수학) ---

    /**
     * 현재 바라보는 방향을 기준으로 위치를 이동시킴 (Free Cam 조작용)
     * @param forward 앞뒤 이동 (+앞, -뒤)
     * @param up      수직 이동 (+위, -아래)
     * @param strafe  좌우 이동 (+왼쪽, -오른쪽) -> 마인크래프트는 왼쪽이 양수
     * @param speed   이동 속도 계수
     */
    public void moveRelative(double forward, double up, double strafe, double speed) {
        // 1. 삼각함수 계산 (Degree -> Radian 변환)
        float f = this.xRot * ((float)Math.PI / 180F);
        float f1 = -this.yRot * ((float)Math.PI / 180F);

        float cosYaw = Mth.cos(f1);
        float sinYaw = Mth.sin(f1);
        float cosPitch = -Mth.cos(f);
        float sinPitch = Mth.sin(f);

        // 2. 방향 벡터 계산
        // Forward Vector (바라보는 방향)
        Vec3 forwardVec = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);

        // Strafe Vector (옆 방향 - Yaw 기준 90도 회전)
        // 마인크래프트의 Strafe 계산 로직을 따름
        Vec3 strafeVec = new Vec3(Mth.sin(f1 + (float)Math.PI / 2F), 0.0, Mth.cos(f1 + (float)Math.PI / 2F));

        // Up Vector (Global Y)
        Vec3 upVec = new Vec3(0, 1, 0);

        // 3. 최종 이동 벡터 합산
        Vec3 motion = Vec3.ZERO;

        if (forward != 0) motion = motion.add(forwardVec.scale(forward * speed));
        if (strafe != 0)  motion = motion.add(strafeVec.scale(strafe * speed));
        if (up != 0)      motion = motion.add(upVec.scale(up * speed));

        // 4. 위치 적용
        this.position = this.position.add(motion);
    }

    /**
     * 마우스 입력 등으로 회전값을 더할 때 사용
     */
    public void addRotation(float dYaw, float dPitch) {
        this.yRot += dYaw;
        this.xRot += dPitch;

        // Pitch 제한 (-90 ~ 90도: 수직 위 ~ 수직 아래)
        this.xRot = Mth.clamp(this.xRot, -90.0F, 90.0F);

        // Yaw 정규화 (선택사항, 360도 넘어가도 상관없지만 깔끔하게 관리하려면)
        this.yRot = this.yRot % 360.0F;
    }
}
