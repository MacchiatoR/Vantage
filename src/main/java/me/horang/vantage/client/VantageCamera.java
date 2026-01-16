package me.horang.vantage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 시네마틱 카메라의 상태(위치, 회전, 줌)를 관리하는 싱글톤 클래스.
 * Mixin과 WorldRenderer는 이 클래스의 데이터를 참조하여 화면을 그린다.
 * 틱(Tick) 단위 업데이트와 프레임(Frame) 단위 보간을 지원한다.
 */

@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class VantageCamera {

    private static final VantageCamera INSTANCE = new VantageCamera();
    public static VantageCamera get() { return INSTANCE; }

    private boolean active = false;

    // Position
    private Vec3 position = Vec3.ZERO;
    private Vec3 prevPosition = Vec3.ZERO;

    // Rotation (Yaw, Pitch, Roll)
    private float yaw = 0f;
    private float prevYaw = 0f;

    private float pitch = 0f;
    private float prevPitch = 0f;

    private float roll = 0f;
    private float prevRoll = 0f;

    // [New] FOV (Field of View)
    private double fov = 70.0;
    private double prevFov = 70.0;

    private double moveSpeed = 0.5;

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                this.position = mc.player.getPosition(1.0f).add(0, mc.player.getEyeHeight(), 0);
                this.prevPosition = this.position;
                this.yaw = mc.player.getYRot();
                this.prevYaw = this.yaw;
                this.pitch = mc.player.getXRot();
                this.prevPitch = this.pitch;
                this.roll = 0f;
                this.prevRoll = 0f;
                this.fov = mc.options.fov().get();
                this.prevFov = this.fov;
            }
        }
    }

    public boolean isActive() { return active; }

    public void setTransform(Vec3 pos, float yaw, float pitch, float roll) {
        this.position = pos;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;

        // 순간이동이므로 이전 위치도 현재 위치로 강제 동기화 (잔상 제거)
        this.prevPosition = pos;
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        this.prevRoll = roll;
    }

    // [New] 재생용: 부드러운 이동 (보간 유지)
    // 이 메서드는 prevPosition을 건드리지 않습니다! (tick() 메서드가 처리함)
    public void setPlaybackState(Vec3 pos, float yaw, float pitch, float roll) {
        this.position = pos;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    // [New] FOV 설정 메서드
    public void setFov(double fov) {
        this.fov = fov;
        this.prevFov = fov;
    }

    // --- Interpolation Methods (요청하신 부분) ---

    public Vec3 getInterpolatedPosition(float partialTick) {
        double x = Mth.lerp(partialTick, prevPosition.x, position.x);
        double y = Mth.lerp(partialTick, prevPosition.y, position.y);
        double z = Mth.lerp(partialTick, prevPosition.z, position.z);
        return new Vec3(x, y, z);
    }

    public float getInterpolatedXRot(float partialTick) { // Pitch
        return Mth.lerp(partialTick, prevPitch, pitch);
    }

    public float getInterpolatedYRot(float partialTick) { // Yaw
        return Mth.lerp(partialTick, prevYaw, yaw);
    }

    public float getInterpolatedRoll(float partialTick) { // Roll
        return Mth.lerp(partialTick, prevRoll, roll);
    }

    public double getInterpolatedFov(float partialTick) { // FOV
        return Mth.lerp(partialTick, prevFov, fov);
    }

    // --- Movement Logic ---
    public void addRotation(float dYaw, float dPitch) {
        this.yaw += dYaw;
        this.pitch += dPitch;
        this.pitch = Math.max(-90, Math.min(90, this.pitch));
    }

    public void moveForward(double distance) {
        Vec3 forward = getForwardVector();
        this.position = this.position.add(forward.scale(distance));
    }

    public Vec3 getForwardVector() {
        float f = this.pitch * ((float)Math.PI / 180F);
        float f1 = -this.yaw * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = -Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3((double)(f3 * f4), (double)f5, (double)(f2 * f4));
    }

    public double getMoveSpeed() { return moveSpeed; }

    // --- Event Handlers ---

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        VantageCamera cam = get();
        if (!cam.active) return;

        event.setYaw(cam.getInterpolatedYRot((float)event.getPartialTick()));
        event.setPitch(cam.getInterpolatedXRot((float)event.getPartialTick()));
        event.setRoll(cam.getInterpolatedRoll((float)event.getPartialTick()));
    }

    // [New] FOV 적용 이벤트
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        VantageCamera cam = get();
        if (!cam.active) return;

        event.setFOV(cam.getInterpolatedFov((float)event.getPartialTick()));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        VantageCamera cam = get();
        if (!cam.active) return;

        // 이전 프레임 값 저장 (보간용)
        cam.prevPosition = cam.position;
        cam.prevYaw = cam.yaw;
        cam.prevPitch = cam.pitch;
        cam.prevRoll = cam.roll;
        cam.prevFov = cam.fov; // [New]

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            // handleInput(cam, mc); // WASD 로직이 있다면 여기
        }
    }

    public void tick() {
        this.prevPosition = this.position;
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;
        this.prevRoll = this.roll;
        this.prevFov = this.fov;
    }

    /**
     * 카메라 시점을 기준으로 상대적인 이동을 수행합니다.
     * @param forward 앞뒤 이동 (W/S) - 보는 방향 기준
     * @param up 위아래 이동 (Space/Shift) - 절대 좌표 Y축 기준
     * @param strafe 좌우 이동 (A/D) - 보는 방향의 수직(오른쪽) 기준
     * @param speed 이동 속도
     */
    public void moveRelative(double forward, double up, double strafe, double speed) {
        // 1. 정면 벡터 (바라보는 방향)
        Vec3 fwdVec = getForwardVector();

        // 2. 우측 벡터 (바라보는 방향의 90도 회전, 수평 이동을 위해 Y축 제거)
        // Yaw를 라디안으로 변환 (-90도 해줘야 오른쪽이 됨)
        float f = -this.yaw * ((float)Math.PI / 180F) - (float)(Math.PI / 2);
        float hCos = Mth.cos(f);
        float hSin = Mth.sin(f);
        Vec3 rightVec = new Vec3(hSin, 0, hCos).normalize();

        // 3. 상단 벡터 (글로벌 Y축, Space/Shift는 항상 수직으로 움직이는 게 편함)
        Vec3 upVec = new Vec3(0, 1, 0);

        // 4. 벡터 합성
        Vec3 motion = Vec3.ZERO
                .add(fwdVec.scale(forward))   // 앞뒤 (보는 방향대로)
                .add(rightVec.scale(strafe))  // 좌우 (수평 유지)
                .add(upVec.scale(up));        // 위아래 (수직 유지)

        // 5. 정규화 후 이동 적용 (대각선 이동 시 속도 증가 방지)
        if (motion.lengthSqr() > 0) {
            this.position = this.position.add(motion.normalize().scale(speed));
        }
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getRoll() {
        return this.roll;
    }

    public double getFov() {
        return this.fov;
    }
}