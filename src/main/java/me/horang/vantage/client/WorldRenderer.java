package me.horang.vantage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.horang.vantage.data.PlaybackManager;
import me.horang.vantage.data.SceneData;
import me.horang.vantage.util.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

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

        // [중요] 노드(큐브)를 그릴 때는 뒷면을 숨겨야(Cull) 투명도 겹침 문제가 사라집니다.
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();

        RaycastHelper.Ray mouseRay = RaycastHelper.getMouseRay(mc.mouseHandler.xpos(), mc.mouseHandler.ypos());

        // 1. 노드 렌더링
        for (SceneData.Node node : data.getNodes()) {
            boolean isSelected = (data.getSelectedNode() == node);

            // 호버링 체크 (단순 거리 체크 방식이 더 빠를 수 있음, 여기선 기존 로직 유지)
            boolean isHovered = false;
            if (mouseRay != null) {
                AABB box = new AABB(node.position.x - NODE_SIZE/2, node.position.y - NODE_SIZE/2, node.position.z - NODE_SIZE/2,
                        node.position.x + NODE_SIZE/2, node.position.y + NODE_SIZE/2, node.position.z + NODE_SIZE/2);
                isHovered = box.clip(mouseRay.origin, mouseRay.origin.add(mouseRay.dir.scale(100))).isPresent();
            }

            // 색상 설정
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

            // 회전 적용
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-node.yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(node.pitch));

            // [수정된 함수 호출] 큐브와 "방향 표시 돌기"를 함께 그립니다.
            renderNodeGeometry(poseStack, faceColor, frontFaceColor);

            // 테두리 (선) - 뎁스 테스트 살짝 꺼서 잘 보이게 하거나, PolygonOffset 사용 권장 (여기선 단순화)
            renderCubeEdges(poseStack, edgeColor);

            poseStack.popPose();
        }

        // 2. 연결선 및 기타 렌더링 (Cull 꺼도 됨)
        RenderSystem.disableCull();

        for (SceneData.Connection conn : data.getConnections()) {
            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            boolean isSelectedLine = (data.getSelectedConnection() == conn);
            int color = isSelectedLine ? 0xFFFF0000 : 0xFF00FF00;
            RenderSystem.lineWidth(isSelectedLine ? 6.0f : 3.0f);

            // [Fix] 곡선(Tension > 0)일 경우 쪼개서 그리기
            if (conn.tension > 0.001f) {
                // 이전/다음 점 구하기 (PlaybackManager와 동일 로직)
                SceneData.Connection prevConn = data.getConnectionTo(conn.start);
                Vec3 p0 = (prevConn != null) ? prevConn.start.position : conn.start.position;

                SceneData.Connection nextConn = data.getConnectionFrom(conn.end);
                Vec3 p3 = (nextConn != null) ? nextConn.end.position : conn.end.position;

                // 20등분하여 곡선 그리기
                int segments = 20;
                Vec3 prevPoint = conn.start.position;

                for (int i = 1; i <= segments; i++) {
                    float t = (float) i / segments;
                    Vec3 currPoint = SceneData.getSplinePoint(t, p0, conn.start.position, conn.end.position, p3);

                    renderLine(poseStack, prevPoint, currPoint, color);
                    prevPoint = currPoint;
                }
            } else {
                // 직선 그리기
                renderLine(poseStack, conn.start.position, conn.end.position, color);
            }

            poseStack.popPose();
        }
        RenderSystem.lineWidth(1.0f);

        // 3. 기즈모 (항상 보이게 Depth Test 끄기)
        if (data.getTool() == SceneData.ToolMode.SELECT && data.getSelectedNode() != null) {
            GizmoSystem.render(poseStack, camPos, mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
        }

        // 4. 프리뷰 (N 모드)
        if (data.getTool() == SceneData.ToolMode.NODE) {
            Vec3 hitPos = RaycastHelper.getRaycastHit(mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
            if (hitPos != null) {
                RenderSystem.enableCull(); // 프리뷰도 깔끔하게
                RenderSystem.disableDepthTest();
                poseStack.pushPose();
                poseStack.translate(hitPos.x - camPos.x, hitPos.y - camPos.y, hitPos.z - camPos.z);
                renderNodeGeometry(poseStack, 0x88FFFF00, 0xAAFFFF55);
                poseStack.popPose();
                RenderSystem.enableDepthTest();
            }
        }

        RenderSystem.disableBlend();
    }

    public static boolean handleClick(int button) {
        if (button != 0) return false;

        SceneData data = SceneData.get();
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        // 1. 기즈모 처리
        if (data.getTool() == SceneData.ToolMode.SELECT && GizmoSystem.handlePress(button, mouseX, mouseY)) {
            return true;
        }

        RaycastHelper.Ray ray = RaycastHelper.getMouseRay(mouseX, mouseY);
        if (ray == null) return false;

        // 2. 노드 선택
        for (SceneData.Node node : data.getNodes()) {
            AABB box = new AABB(
                    node.position.x - NODE_SIZE/2, node.position.y - NODE_SIZE/2, node.position.z - NODE_SIZE/2,
                    node.position.x + NODE_SIZE/2, node.position.y + NODE_SIZE/2, node.position.z + NODE_SIZE/2
            );
            if (box.clip(ray.origin, ray.origin.add(ray.dir.scale(100))).isPresent()) {
                if (data.getTool() == SceneData.ToolMode.SELECT) {
                    data.setSelectedNode(node); // 노드 선택 (선 선택은 SceneData에서 자동 해제됨)
                } else if (data.getTool() == SceneData.ToolMode.LINE) {
                    if (data.getSelectedNode() == null) data.setSelectedNode(node);
                    else { data.connectNodes(data.getSelectedNode(), node); data.setSelectedNode(null); }
                }
                return true;
            }
        }

        // 3. 선(Line) 선택 [문제 4 해결]
        if (data.getTool() == SceneData.ToolMode.SELECT) {
            // [Fix] 판정 범위를 0.3 -> 1.0으로 대폭 늘림 (마우스가 살짝 빗나가도 선택되게)
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
                data.setSelectedConnection(targetConn); // 선 선택
                return true;
            }

            // 허공 클릭 시 해제
            data.setSelectedNode(null);
            data.setSelectedConnection(null);
        }

        // 4. 노드 생성
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

    // --- Render Helpers (누락되었던 메서드들 포함) ---

    // [Fix] 누락되었던 renderLine 추가
    private static void renderLine(PoseStack stack, Vec3 start, Vec3 end, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;

        buffer.addVertex(mat, (float)start.x, (float)start.y, (float)start.z).setColor(r, g, b, a);
        buffer.addVertex(mat, (float)end.x, (float)end.y, (float)end.z).setColor(r, g, b, a);

        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    private static void renderAnchor(PoseStack stack, Vec3 camPos, Vec3 anchorPos) {
        stack.pushPose();
        stack.translate(anchorPos.x - camPos.x, anchorPos.y - camPos.y, anchorPos.z - camPos.z);
        renderWireframeBox(stack, -0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f, 0xFF00FFFF);
        stack.popPose();
    }

    private static void renderWireframeBox(PoseStack stack, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;

        addLine(buffer, mat, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(buffer, mat, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        addLine(buffer, mat, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, mat, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(buffer, mat, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        addLine(buffer, mat, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, mat, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);

        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    private static void renderNodeGeometry(PoseStack stack, int bodyColor, int frontColor) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        float s = NODE_SIZE / 2.0f; // 0.25

        // 색상 분해
        int a = (bodyColor >> 24) & 0xFF; int r = (bodyColor >> 16) & 0xFF; int g = (bodyColor >> 8) & 0xFF; int b = (bodyColor) & 0xFF;
        int fa = (frontColor >> 24) & 0xFF; int fr = (frontColor >> 16) & 0xFF; int fg = (frontColor >> 8) & 0xFF; int fb = (frontColor) & 0xFF;

        // --- 1. 메인 큐브 바디 (Cull 켰을 때 보이도록 CCW 순서 주의) ---
        // Back (-Z)
        addQuad(buffer, mat, s, s, -s, -s, s, -s, -s, -s, -s, s, -s, -s, r, g, b, a);
        // Left (-X)
        addQuad(buffer, mat, -s, s, -s, -s, s, s, -s, -s, s, -s, -s, -s, r, g, b, a);
        // Right (+X)
        addQuad(buffer, mat, s, s, s, s, s, -s, s, -s, -s, s, -s, s, r, g, b, a);
        // Top (+Y)
        addQuad(buffer, mat, -s, s, -s, s, s, -s, s, s, s, -s, s, s, r, g, b, a);
        // Bottom (-Y)
        addQuad(buffer, mat, -s, -s, s, s, -s, s, s, -s, -s, -s, -s, -s, r, g, b, a);
        // Front (+Z) -> 정면 색상 적용
        addQuad(buffer, mat, -s, s, s, s, s, s, s, -s, s, -s, -s, s, fr, fg, fb, fa);

        // --- 2. 방향 표시 돌기 (Nose) ---
        // 정면(+Z) 중앙에 작게 튀어나온 박스를 추가합니다.
        float noseSize = s * 0.4f;   // 큐브 크기의 40%
        float noseDepth = s + 0.1f;  // 큐브 앞면보다 0.1만큼 더 튀어나옴

        // 돌기 앞면
        addQuad(buffer, mat, -noseSize, noseSize, noseDepth, noseSize, noseSize, noseDepth,
                noseSize, -noseSize, noseDepth, -noseSize, -noseSize, noseDepth, fr, fg, fb, fa);

        // 돌기 옆면들 (Top/Bottom/Left/Right) - 입체감 있게 연결
        // Top
        addQuad(buffer, mat, -noseSize, noseSize, s, noseSize, noseSize, s, noseSize, noseSize, noseDepth, -noseSize, noseSize, noseDepth, fr, fg, fb, fa);
        // Bottom
        addQuad(buffer, mat, -noseSize, -noseSize, noseDepth, noseSize, -noseSize, noseDepth, noseSize, -noseSize, s, -noseSize, -noseSize, s, fr, fg, fb, fa);
        // Right (+X)
        addQuad(buffer, mat, noseSize, noseSize, s, noseSize, noseSize, noseDepth, noseSize, -noseSize, noseDepth, noseSize, -noseSize, s, fr, fg, fb, fa);
        // Left (-X)
        addQuad(buffer, mat, -noseSize, noseSize, noseDepth, -noseSize, noseSize, s, -noseSize, -noseSize, s, -noseSize, -noseSize, noseDepth, fr, fg, fb, fa);

        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    private static void renderCubeEdges(PoseStack stack, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        float s = NODE_SIZE / 2.0f;
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;

        // 12 edges
        addLine(buffer, mat, -s,-s,-s, s,-s,-s, r,g,b,a);
        addLine(buffer, mat, s,-s,-s, s,-s,s, r,g,b,a);
        addLine(buffer, mat, s,-s,s, -s,-s,s, r,g,b,a);
        addLine(buffer, mat, -s,-s,s, -s,-s,-s, r,g,b,a);
        addLine(buffer, mat, -s,s,-s, s,s,-s, r,g,b,a);
        addLine(buffer, mat, s,s,-s, s,s,s, r,g,b,a);
        addLine(buffer, mat, s,s,s, -s,s,s, r,g,b,a);
        addLine(buffer, mat, -s,s,s, -s,s,-s, r,g,b,a);
        addLine(buffer, mat, -s,-s,-s, -s,s,-s, r,g,b,a);
        addLine(buffer, mat, s,-s,-s, s,s,-s, r,g,b,a);
        addLine(buffer, mat, s,-s,s, s,s,s, r,g,b,a);
        addLine(buffer, mat, -s,-s,s, -s,s,s, r,g,b,a);

        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    private static void addQuad(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int bl, int a) {
        b.addVertex(m, x1, y1, z1).setColor(r,g,bl,a);
        b.addVertex(m, x2, y2, z2).setColor(r,g,bl,a);
        b.addVertex(m, x3, y3, z3).setColor(r,g,bl,a);

        b.addVertex(m, x3, y3, z3).setColor(r,g,bl,a);
        b.addVertex(m, x4, y4, z4).setColor(r,g,bl,a);
        b.addVertex(m, x1, y1, z1).setColor(r,g,bl,a);
    }

    private static void addLine(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int bl, int a) {
        b.addVertex(m, x1, y1, z1).setColor(r,g,bl,a);
        b.addVertex(m, x2, y2, z2).setColor(r,g,bl,a);
    }
}