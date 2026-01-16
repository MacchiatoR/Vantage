package me.horang.vantage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import me.horang.vantage.data.PlaybackManager;
import me.horang.vantage.data.SceneData;
import me.horang.vantage.util.GuiUtils;
import me.horang.vantage.util.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import static me.horang.vantage.client.GizmoSystem.renderBox;

@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class WorldRenderer {

    private static final float NODE_SIZE = 0.5f;

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!VantageCamera.get().isActive()) return;
        if (PlaybackManager.isPlaying()) return;

        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = VantageCamera.get().getInterpolatedPosition(event.getPartialTick().getGameTimeDeltaPartialTick(true));
        SceneData data = SceneData.get();

        renderAnchor(poseStack, camPos, data.getAnchor());

        // --- 렌더링 공통 설정 ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();

        RaycastHelper.Ray mouseRay = RaycastHelper.getMouseRay(mc.mouseHandler.xpos(), mc.mouseHandler.ypos());

        // 1. 노드 렌더링
        for (SceneData.Node node : data.getNodes()) {
            boolean isSelected = (data.getSelectedNode() == node);
            boolean isHovered = false;

            if (mouseRay != null) {
                AABB box = new AABB(node.position.x - NODE_SIZE/2, node.position.y - NODE_SIZE/2, node.position.z - NODE_SIZE/2,
                        node.position.x + NODE_SIZE/2, node.position.y + NODE_SIZE/2, node.position.z + NODE_SIZE/2);
                isHovered = box.clip(mouseRay.origin, mouseRay.origin.add(mouseRay.dir.scale(100))).isPresent();
            }

            int faceColor, frontFaceColor, edgeColor;
            if (isSelected) {
                faceColor = 0x66FF0000; frontFaceColor = 0xAAFF5555; edgeColor = 0xFFFF0000;
            } else if (isHovered) {
                faceColor = 0x66FFFF00; frontFaceColor = 0xAAFFFF55; edgeColor = 0xFFFFFF00;
            } else {
                faceColor = 0x44FFFFFF; frontFaceColor = 0x880000FF; edgeColor = 0x88FFFFFF;
            }

            poseStack.pushPose();
            poseStack.translate(node.position.x - camPos.x, node.position.y - camPos.y, node.position.z - camPos.z);

            float yaw   = normalizeAngle(node.yaw);
            float pitch = normalizeAngle(node.pitch);
            float roll  = normalizeAngle(node.roll);

            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

            GuiUtils.renderNodeGeometry(poseStack, NODE_SIZE, faceColor, frontFaceColor);
            GuiUtils.renderCubeEdges(poseStack, NODE_SIZE, edgeColor);

            poseStack.popPose();
        }

        // 2. 연결선 렌더링 (Cull 꺼도 됨)
        RenderSystem.disableCull();

        for (SceneData.Connection conn : data.getConnections()) {
            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            boolean isSelectedLine = (data.getSelectedConnection() == conn);
            int color = isSelectedLine ? 0xFFFF0000 : 0xFF00FF00;
            RenderSystem.lineWidth(isSelectedLine ? 6.0f : 3.0f);

            // [수정됨] 보간 타입이 LINEAR가 아니면 곡선으로 그리기
            if (conn.interpType != SceneData.InterpolationType.LINEAR) {
                int segments = 20;
                Vec3 prevPoint = conn.start.position;

                for (int i = 1; i <= segments; i++) {
                    float t = (float) i / segments;

                    // [핵심 수정] SceneData.getSplinePoint -> SceneData.getPointOnConnection
                    // 이제 Easing과 Interpolation 설정이 모두 반영된 좌표를 가져옵니다.
                    Vec3 currPoint = SceneData.getPointOnConnection(conn, t, data);

                    GuiUtils.renderLine(poseStack, prevPoint, currPoint, color);
                    prevPoint = currPoint;
                }
            } else {
                // 직선 모드
                GuiUtils.renderLine(poseStack, conn.start.position, conn.end.position, color);
            }

            poseStack.popPose();
        }
        RenderSystem.lineWidth(1.0f);

        // 3. 기즈모
        if (data.getTool() == SceneData.ToolMode.SELECT && data.getSelectedNode() != null) {
            GizmoSystem.render(poseStack, camPos, mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
        }

        // 4. 프리뷰
        if (data.getTool() == SceneData.ToolMode.NODE) {
            Vec3 hitPos = RaycastHelper.getRaycastHit(mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
            if (hitPos != null) {
                RenderSystem.enableCull();
                RenderSystem.disableDepthTest();
                poseStack.pushPose();
                poseStack.translate(hitPos.x - camPos.x, hitPos.y - camPos.y, hitPos.z - camPos.z);
                GuiUtils.renderNodeGeometry(poseStack, NODE_SIZE, 0x88FFFF00, 0xAAFFFF55);
                poseStack.popPose();
                RenderSystem.enableDepthTest();
            }
        }

        RenderSystem.disableBlend();
    }

    private static float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle > 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    public static boolean handleClick(int button) {
        // ... (handleClick 로직은 기존과 동일) ...
        if (button != 0) return false;
        SceneData data = SceneData.get();
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        if (data.getTool() == SceneData.ToolMode.SELECT && GizmoSystem.handlePress(button, mouseX, mouseY)) {
            return true;
        }

        RaycastHelper.Ray ray = RaycastHelper.getMouseRay(mouseX, mouseY);
        if (ray == null) return false;

        for (SceneData.Node node : data.getNodes()) {
            AABB box = new AABB(
                    node.position.x - NODE_SIZE/2, node.position.y - NODE_SIZE/2, node.position.z - NODE_SIZE/2,
                    node.position.x + NODE_SIZE/2, node.position.y + NODE_SIZE/2, node.position.z + NODE_SIZE/2
            );
            if (box.clip(ray.origin, ray.origin.add(ray.dir.scale(100))).isPresent()) {
                if (data.getTool() == SceneData.ToolMode.SELECT) {
                    data.setSelectedNode(node);
                } else if (data.getTool() == SceneData.ToolMode.LINE) {
                    if (data.getSelectedNode() == null) data.setSelectedNode(node);
                    else { data.connectNodes(data.getSelectedNode(), node); data.setSelectedNode(null); }
                }
                return true;
            }
        }

        if (data.getTool() == SceneData.ToolMode.SELECT) {
            double bestDist = 1.0;
            SceneData.Connection targetConn = null;
            for (SceneData.Connection conn : data.getConnections()) {
                double dist = RaycastHelper.getDistanceFromLine(ray, conn.start.position, conn.end.position);
                if (dist < bestDist) {
                    bestDist = dist;
                    targetConn = conn;
                }
            }
            if (targetConn != null) {
                data.setSelectedConnection(targetConn);
                return true;
            }
            data.setSelectedNode(null);
            data.setSelectedConnection(null);
        }

        if (data.getTool() == SceneData.ToolMode.NODE) {
            Vec3 hitPos = RaycastHelper.getRaycastHit(mouseX, mouseY);
            if (hitPos != null) {
                float yaw = mc.player != null ? mc.player.getYRot() : 0;
                float pitch = mc.player != null ? mc.player.getXRot() : 0;
                data.addNode(hitPos, yaw, pitch);
                return true;
            }
        }
        return false;
    }

    private static void renderAnchor(PoseStack stack, Vec3 camPos, Vec3 anchorPos) {
        stack.pushPose();
        stack.translate(anchorPos.x - camPos.x, anchorPos.y - camPos.y, anchorPos.z - camPos.z);
        // [변경] GuiUtils 호출
        GuiUtils.renderWireframeBox(stack, -0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f, 0xFF00FFFF);
        stack.popPose();
    }

    public static SceneData.Node raycastNodes(double mouseX, double mouseY) {
        // 1. 마우스 위치로부터 레이(Ray) 생성
        RaycastHelper.Ray ray = RaycastHelper.getMouseRay(mouseX, mouseY);
        if (ray == null) return null;

        SceneData.Node closestNode = null;
        double closestDistSqr = Double.MAX_VALUE;

        // 2. 모든 노드의 히트박스(AABB)와 레이 충돌 검사
        for (SceneData.Node node : SceneData.get().getNodes()) {
            AABB box = new AABB(
                    node.position.x - NODE_SIZE/2, node.position.y - NODE_SIZE/2, node.position.z - NODE_SIZE/2,
                    node.position.x + NODE_SIZE/2, node.position.y + NODE_SIZE/2, node.position.z + NODE_SIZE/2
            );

            // 레이가 박스를 통과하는지 확인 (최대 거리 100 블록)
            var hit = box.clip(ray.origin, ray.origin.add(ray.dir.scale(100.0)));

            if (hit.isPresent()) {
                // 카메라(ray origin)와의 거리 계산하여 가장 가까운 노드 선택
                double distSqr = hit.get().distanceToSqr(ray.origin);
                if (distSqr < closestDistSqr) {
                    closestDistSqr = distSqr;
                    closestNode = node;
                }
            }
        }
        return closestNode;
    }
}