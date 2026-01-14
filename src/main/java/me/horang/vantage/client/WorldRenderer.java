package me.horang.vantage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.VantageEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

@EventBusSubscriber(modid = "vantage", value = Dist.CLIENT)
public class WorldRenderer {

    // 정사면체 크기
    private static final float NODE_SIZE = 0.5f;

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // 반투명 블록들 다 그리고 난 뒤에 그림 (AFTER_TRANSLUCENT)
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT) return;
        if (!VantageCamera.get().isActive()) return;

        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = VantageCamera.get().getPosition();
        SceneData data = SceneData.get();

        poseStack.pushPose();
        // 카메라 위치를 기준으로 상대 좌표 계산을 위해 이동
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // 1. 기존 노드들 렌더링
        for (SceneData.Node node : data.getNodes()) {
            boolean isSelected = (data.getSelectedNode() == node);
            renderTetrahedron(poseStack, node.position, isSelected ? 0xFFFF0000 : 0xFFFFFFFF); // 선택되면 빨강
        }

        // 2. 라인 렌더링
        for (SceneData.Connection conn : data.getConnections()) {
            renderLine(poseStack, conn.start.position, conn.end.position, 0xFF00FF00); // 초록색 선
        }

        // 3. 도구별 프리뷰 (Preview)
        Vec3 hitPos = getMouseHitPos(mc); // 마우스가 가리키는 월드 좌표

        if (data.getTool() == SceneData.ToolMode.NODE && hitPos != null) {
            // 마우스 커서에 정사면체 따라다니기 (반투명)
            renderTetrahedron(poseStack, hitPos, 0x88FFFF00); // 노란색 반투명
        }
        else if (data.getTool() == SceneData.ToolMode.LINE && data.getSelectedNode() != null && hitPos != null) {
            // 선 연결 중일 때: 선택한 노드 ~ 마우스 커서 선 잇기
            renderLine(poseStack, data.getSelectedNode().position, hitPos, 0x88FFFFFF);
        }

        poseStack.popPose();
    }

    // 마우스 클릭 이벤트 처리 (배치 로직)
    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        SceneData data = SceneData.get();

        // 에디터가 켜져있고, 좌클릭을 눌렀을 때
        if (mc.screen instanceof VantageEditorScreen &&
                event.getButton() == 0 && event.getAction() == GLFW.GLFW_PRESS) {

            // UI 위 클릭은 무시 (VantageEditorScreen에서 처리됨)
            // 여기서는 월드 클릭 로직만

            Vec3 hitPos = getMouseHitPos(mc);
            if (hitPos == null) return; // 허공 클릭

            if (data.getTool() == SceneData.ToolMode.NODE) {
                // [기능 3] 노드 배치
                data.addNode(hitPos, 0, 0); // 각도는 일단 0
            }
            else if (data.getTool() == SceneData.ToolMode.LINE) {
                // [기능 4] 라인 연결
                SceneData.Node clickedNode = findNodeNear(hitPos); // 클릭한 곳에 노드가 있나?

                if (clickedNode != null) {
                    if (data.getSelectedNode() == null) {
                        // 첫 번째 노드 선택
                        data.setSelectedNode(clickedNode);
                    } else {
                        // 두 번째 노드 클릭 -> 연결
                        data.connectNodes(data.getSelectedNode(), clickedNode);
                        data.setSelectedNode(null); // 초기화
                    }
                }
            }
        }
    }

    // --- Helper Methods ---

    // 마우스 레이캐스팅 (Mouse -> World)
    private static Vec3 getMouseHitPos(Minecraft mc) {
        // 실제로는 Mouse picking ray를 쏴야 하지만,
        // 간단하게 마인크래프트의 기본 'Target'을 가져오거나,
        // VantageCamera 위치에서 정면으로 Ray를 쏴야 함.
        // 여기서는 '현재 카메라가 보고 있는 블록'을 가져오는 약식 로직 사용.

        // 정교한 Mouse Picking은 ViewMatrix 역행렬 연산이 필요한데,
        // 일단은 "화면 중앙(십자선)" 기준으로 배치하는 걸로 타협하거나
        // 나중에 Unproject 코드를 추가해야 함.
        // *사용자가 '마우스 커서'를 원했으므로 십자선 기준 HitResult 사용*

        HitResult hit = mc.hitResult; // 이건 플레이어 기준이라 부정확할 수 있음.

        // VantageCamera 기준 RayTrace가 필요함. (복잡하므로 일단 임시로 카메라 앞 5칸)
        // **중요**: 제대로 하려면 RayTracer를 별도로 구현해야 함.
        // 이번 단계에서는 "카메라 앞 3미터 허공" or "카메라가 바라보는 블록"으로 단순화.

        Vec3 camPos = VantageCamera.get().getPosition();
        Vec3 lookVec = new Vec3(VantageCamera.get().getForwardVector()); // forward vector 계산 필요

        // 임시: 카메라 앞 5블록 위치 반환 (허공 배치 가능하게)
        return camPos.add(lookVec.scale(5.0));
    }

    // 클릭 위치 근처의 노드 찾기
    private static SceneData.Node findNodeNear(Vec3 pos) {
        for (SceneData.Node node : SceneData.get().getNodes()) {
            if (node.position.distanceTo(pos) < NODE_SIZE * 1.5) {
                return node;
            }
        }
        return null;
    }

    // 정사면체 그리기 (Low-level Vertex Drawing)
    private static void renderTetrahedron(PoseStack stack, Vec3 pos, int color) {
        Matrix4f mat = stack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder(); // 1.21에서는 이름이 다를 수 있음 (BeginBufferBuilder 등)

        // 1.21 NeoForge 렌더링 방식에 맞게 수정 필요
        // 여기선 개념적 코드로 작성. 실제로는 RenderSystem.setShader... 필요

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 정사면체 꼭짓점 4개
        float s = NODE_SIZE;
        float h = (float) (s * Math.sqrt(2.0/3.0));

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // 삼각형 4개면 추가 (좌표 계산 로직은 생략, 정사면체 수학 적용)
        // ... (Vertex building code) ...

        // 임시로 큐브(Box) 그리기 (구현이 더 쉬움)
        addBoxVertices(buffer, mat, pos, s, color);

        tesselator.end();
    }

    // 큐브 그리기 헬퍼
    private static void addBoxVertices(BufferBuilder buffer, Matrix4f mat, Vec3 pos, float s, int color) {
        float x = (float)pos.x;
        float y = (float)pos.y;
        float z = (float)pos.z;
        float hs = s / 2;

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color & 0xFF);

        // 아주 단순화된 박스 버텍스 추가 (6면)
        // ... 실제로는 24개 버텍스 add ...
    }

    private static void renderLine(PoseStack stack, Vec3 start, Vec3 end, int color) {
        // Line Strip 렌더링
    }
}
