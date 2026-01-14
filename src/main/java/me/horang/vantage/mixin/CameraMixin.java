package me.horang.vantage.mixin;


import me.horang.vantage.client.VantageCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    // [Shadow] 바닐라 메서드에 접근하기 위한 선언
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    /**
     * 바닐라의 setup 메서드가 실행된 직후(TAIL)에 호출됨.
     * 여기서 바닐라가 계산한 값을 깡그리 무시하고 우리 값을 넣음.
     */
    @Inject(method = "setup", at = @At("TAIL"))
    public void onSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        VantageCamera vantage = VantageCamera.get();

        if (vantage.isActive()) {
            // 1. 위치 강제 주입 (Position Override)
            this.setPosition(vantage.getPosition());

            // 2. 회전 강제 주입 (Rotation Override)
            // 매개변수 순서: Yaw(yRot), Pitch(xRot)
            this.setRotation(vantage.getYRot(), vantage.getXRot());

            // 참고: Roll(기울기)은 Camera 클래스에 필드가 없어서 여기서 설정 불가.
            // ClientHandler의 ViewportEvent나 GameRendererMixin에서 처리해야 함.
        }
    }
}