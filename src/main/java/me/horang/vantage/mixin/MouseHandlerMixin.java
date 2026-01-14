package me.horang.vantage.mixin;

import me.horang.vantage.client.VantageCamera;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    /**
     * turnPlayer 메서드는 마우스 이동값(dx, dy)을 받아 플레이어의 시선을 돌림.
     * 이를 HEAD(시작 부분)에서 가로채서 실행을 취소(Cancel)시킴.
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    public void onTurnPlayer(CallbackInfo ci) {
        // Vantage 카메라가 활성화 상태라면?
        if (VantageCamera.get().isActive()) {
            // 바닐라의 고개 돌리기 로직을 아예 실행하지 않음.
            ci.cancel();

            // 만약 나중에 "에디터 모드에서 마우스로 자유 시점을 조절"하고 싶다면,
            // 여기서 VantageCamera.get().addRotation(...) 같은 걸 호출해서
            // 마우스 입력을 우리 카메라로 납치해오면 됨.
        }
    }
}
