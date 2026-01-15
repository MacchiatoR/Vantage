package me.horang.vantage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.horang.vantage.data.SceneData;
import me.horang.vantage.util.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GizmoSystem {

    public enum Axis { NONE, X, Y, Z, ALL }

    private static Axis hoveringAxis = Axis.NONE;
    private static Axis draggingAxis = Axis.NONE;

    // 비주얼 설정
    private static final float ARM_LENGTH = 1.0f;
    private static final float ARM_THICKNESS = 0.05f;
    private static final float CENTER_SIZE = 0.25f;

    // --- 드래그 로직용 변수 (변경됨) ---
    // 드래그 시작 시점의 노드 위치와 클릭 위치를 저장하여 '차이(Delta)'를 계산합니다.
    private static Vec3 startNodePos = Vec3.ZERO; // 드래그 시작 시 노드 위치
    private static Vec3 startHitPos = Vec3.ZERO;  // 드래그 시작 시 가상 평면 클릭 위치

    // --- Rendering (기존과 동일) ---
    public static void render(PoseStack stack, Vec3 camPos, double mouseX, double mouseY) {
        SceneData.Node selected = SceneData.get().getSelectedNode();
        if (selected == null) return;

        if (draggingAxis == Axis.NONE) {
            updateHovering(mouseX, mouseY, selected.position);
        }

        stack.pushPose();
        stack.translate(selected.position.x - camPos.x, selected.position.y - camPos.y, selected.position.z - camPos.z);

        // 1. Depth Test는 끕니다 (벽 뚫고 보이기 위함)
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 2. 중앙 박스 그리기 (항상 가장 안쪽에 있으므로 먼저 그립니다)
        boolean hoverAll = (hoveringAxis == Axis.ALL || draggingAxis == Axis.ALL);
        renderBox(stack, -CENTER_SIZE, -CENTER_SIZE, -CENTER_SIZE, CENTER_SIZE, CENTER_SIZE, CENTER_SIZE,
                hoverAll ? 0xAAFFFFFF : 0x44FFFFFF);

        // 3. [Fix] 그리기 순서 정렬 (멀리 있는 축 -> 가까이 있는 축)
        // 카메라로부터 각 축의 끝점까지 거리를 계산해서 정렬합니다.
        List<Axis> axes = new ArrayList<>(Arrays.asList(Axis.X, Axis.Y, Axis.Z));

        // 정렬 기준: (노드위치 + 축방향)과 카메라 사이의 거리
        // 거리가 큰(먼) 순서대로 정렬해야 함 (내림차순)
        axes.sort((a, b) -> {
            Vec3 posA = getAxisEndPos(a);
            Vec3 posB = getAxisEndPos(b);
            // 로컬 좌표계 기준이므로 카메라 위치도 로컬로 변환해서 계산하거나,
            // 단순히 현재 stack이 이동된 상태이므로 (0,0,0)이 노드 위치임을 감안해야 함.
            // 하지만 여기선 간단히 camPos와의 절대 거리로 비교합니다.

            double distA = camPos.distanceToSqr(selected.position.add(posA));
            double distB = camPos.distanceToSqr(selected.position.add(posB));
            return Double.compare(distB, distA); // 먼 거 먼저 (내림차순)
        });

        // 4. 정렬된 순서대로 렌더링
        for (Axis axis : axes) {
            boolean isHoveringOrDragging = (hoveringAxis == axis || draggingAxis == axis);
            int color;
            switch (axis) {
                case X: color = 0xFFFF0000; break;
                case Y: color = 0xFF00FF00; break;
                default: color = 0xFF0000FF; break; // Z
            }
            renderArrow(stack, axis, color, isHoveringOrDragging);
        }

        RenderSystem.enableDepthTest();
        stack.popPose();
    }

    // [New] 정렬을 위해 각 축의 끝점 좌표를 반환하는 헬퍼 메서드
    private static Vec3 getAxisEndPos(Axis axis) {
        switch (axis) {
            case X: return new Vec3(ARM_LENGTH, 0, 0);
            case Y: return new Vec3(0, ARM_LENGTH, 0);
            case Z: return new Vec3(0, 0, ARM_LENGTH);
            default: return Vec3.ZERO;
        }
    }

    // (렌더링 헬퍼 메서드들 - renderArrow, renderBox, addBox, addQuad 등은 기존 코드 유지)
    private static void renderArrow(PoseStack stack, Axis axis, int color, boolean highlight) {
        if (highlight) color = 0xFFFFFF55;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();
        float l = ARM_LENGTH;
        float w = ARM_THICKNESS / 2.0f;
        float start = CENTER_SIZE * 0.8f;
        float minX=0, minY=0, minZ=0, maxX=0, maxY=0, maxZ=0;
        int a = (color >> 24) & 0xFF; int r = (color >> 16) & 0xFF; int g = (color >> 8) & 0xFF; int b = (color) & 0xFF;
        switch (axis) {
            case X: minX = start; maxX = l; minY = -w; maxY = w; minZ = -w; maxZ = w; break;
            case Y: minY = start; maxY = l; minX = -w; maxX = w; minZ = -w; maxZ = w; break;
            case Z: minZ = start; maxZ = l; minX = -w; maxX = w; minY = -w; maxY = w; break;
        }
        addBox(buffer, mat, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    private static void renderBox(PoseStack stack, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();
        int a = (color >> 24) & 0xFF; int r = (color >> 16) & 0xFF; int g = (color >> 8) & 0xFF; int b = (color) & 0xFF;
        addBox(buf, mat, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
        try { BufferUploader.drawWithShader(buf.buildOrThrow()); } catch (Exception e) {}
    }

    private static void addBox(BufferBuilder b, Matrix4f m, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int r, int g, int bl, int a) {
        addQuad(b, m, minX,maxY,maxZ, maxX,maxY,maxZ, maxX,minY,maxZ, minX,minY,maxZ, r,g,bl,a);
        addQuad(b, m, maxX,maxY,minZ, minX,maxY,minZ, minX,minY,minZ, maxX,minY,minZ, r,g,bl,a);
        addQuad(b, m, minX,maxY,minZ, minX,maxY,maxZ, minX,minY,maxZ, minX,minY,minZ, r,g,bl,a);
        addQuad(b, m, maxX,maxY,maxZ, maxX,maxY,minZ, maxX,minY,minZ, maxX,minY,maxZ, r,g,bl,a);
        addQuad(b, m, minX,maxY,minZ, maxX,maxY,minZ, maxX,maxY,maxZ, minX,maxY,maxZ, r,g,bl,a);
        addQuad(b, m, minX,minY,maxZ, maxX,minY,maxZ, maxX,minY,minZ, minX,minY,minZ, r,g,bl,a);
    }

    private static void addQuad(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int bl, int a) {
        b.addVertex(m, x1, y1, z1).setColor(r,g,bl,a); b.addVertex(m, x2, y2, z2).setColor(r,g,bl,a); b.addVertex(m, x3, y3, z3).setColor(r,g,bl,a);
        b.addVertex(m, x3, y3, z3).setColor(r,g,bl,a); b.addVertex(m, x4, y4, z4).setColor(r,g,bl,a); b.addVertex(m, x1, y1, z1).setColor(r,g,bl,a);
    }

    // --- Hover Logic (기존 유지) ---
    private static void updateHovering(double mouseX, double mouseY, Vec3 nodePos) {
        RaycastHelper.Ray ray = RaycastHelper.getMouseRay(mouseX, mouseY);
        if (ray == null) return;

        hoveringAxis = Axis.NONE;

        float s = CENTER_SIZE;
        float w = ARM_THICKNESS * 2.5f;
        float l = ARM_LENGTH;
        float start = s * 0.8f;

        AABB centerBox = new AABB(-s, -s, -s, s, s, s);
        AABB xBox = new AABB(start, -w, -w, l, w, w);
        AABB yBox = new AABB(-w, start, -w, w, l, w);
        AABB zBox = new AABB(-w, -w, start, w, w, l);

        double distC = getIntersectDist(ray, nodePos, centerBox);
        double distX = getIntersectDist(ray, nodePos, xBox);
        double distY = getIntersectDist(ray, nodePos, yBox);
        double distZ = getIntersectDist(ray, nodePos, zBox);

        double minDist = Double.MAX_VALUE;

        if (distC < minDist) { minDist = distC; hoveringAxis = Axis.ALL; }
        if (distX < minDist) { minDist = distX; hoveringAxis = Axis.X; }
        if (distY < minDist) { minDist = distY; hoveringAxis = Axis.Y; }
        if (distZ < minDist) { minDist = distZ; hoveringAxis = Axis.Z; }
    }

    private static double getIntersectDist(RaycastHelper.Ray ray, Vec3 offset, AABB localBox) {
        AABB worldBox = localBox.move(offset);
        return worldBox.clip(ray.origin, ray.origin.add(ray.dir.scale(100.0)))
                .map(vec -> vec.distanceTo(ray.origin))
                .orElse(Double.MAX_VALUE);
    }

    // --- Interaction (개선된 로직) ---

    // 평면과 Ray 교차점 계산
    private static Vec3 getRayPlaneIntersection(double mouseX, double mouseY, Vec3 planeNormal, Vec3 planePoint) {
        RaycastHelper.Ray ray = RaycastHelper.getMouseRay(mouseX, mouseY);
        if (ray == null) return null;

        double denom = ray.dir.dot(planeNormal);
        // 평면과 Ray가 평행하면(수직으로 바라보면) 클릭 불가 -> 매우 작은 값으로 체크
        if (Math.abs(denom) < 1e-6) return null;

        double t = planePoint.subtract(ray.origin).dot(planeNormal) / denom;
        if (t < 0) return null; // 카메라 뒤쪽

        return ray.origin.add(ray.dir.scale(t));
    }

    // [핵심] 현재 카메라가 바라보는 방향을 기준으로 '가상의 유리판(평면)' 위에서의 마우스 좌표를 구함
    private static Vec3 getCameraPlaneHit(double mouseX, double mouseY, Vec3 nodePos) {
        VantageCamera cam = VantageCamera.get();
        // 평면의 법선(Normal) = 카메라가 바라보는 방향
        // 이렇게 하면 항상 마우스 움직임과 1:1로 매칭되는 평면을 얻을 수 있습니다.
        Vec3 planeNormal = cam.getForwardVector();

        return getRayPlaneIntersection(mouseX, mouseY, planeNormal, nodePos);
    }

    public static boolean handlePress(int button, double mouseX, double mouseY) {
        if (button == 0 && hoveringAxis != Axis.NONE) {
            draggingAxis = hoveringAxis;
            SceneData.Node node = SceneData.get().getSelectedNode();

            // 1. 드래그 시작 시점의 노드 위치 저장
            startNodePos = node.position;

            // 2. 드래그 시작 시점의 '가상 평면' 위 마우스 클릭 위치 저장
            //    이때 평면의 기준점(planePoint)은 '현재 노드 위치'입니다.
            Vec3 hit = getCameraPlaneHit(mouseX, mouseY, node.position);

            if (hit != null) {
                startHitPos = hit;
                return true;
            }
        }
        return false;
    }

    public static void handleRelease() {
        draggingAxis = Axis.NONE;
    }

    public static boolean handleDrag(double mouseX, double mouseY) {
        if (draggingAxis == Axis.NONE) return false;

        SceneData.Node node = SceneData.get().getSelectedNode();
        if (node == null) return false;

        // 1. 현재 마우스 위치를 '드래그 시작했을 때 생성한 가상 평면' 위로 투영
        //    중요: 평면의 기준점은 움직이는 노드가 아니라 '처음 클릭했던 위치(startNodePos)'여야 평면이 안 흔들립니다.
        Vec3 currentHitPos = getRayPlaneIntersection(mouseX, mouseY, VantageCamera.get().getForwardVector(), startNodePos);

        if (currentHitPos == null) return true; // 허공을 보고 있거나 계산 불가 시 무시

        // 2. 이동량(Delta) 계산 = 현재 히트 좌표 - 시작 히트 좌표
        Vec3 delta = currentHitPos.subtract(startHitPos);

        // 3. 축에 따라 이동량 제한하여 적용
        //    (기존 위치 + 이동량)
        switch (draggingAxis) {
            case X:
                node.position = new Vec3(startNodePos.x + delta.x, startNodePos.y, startNodePos.z);
                break;
            case Y:
                node.position = new Vec3(startNodePos.x, startNodePos.y + delta.y, startNodePos.z);
                break;
            case Z:
                node.position = new Vec3(startNodePos.x, startNodePos.y, startNodePos.z + delta.z);
                break;
            case ALL:
                node.position = startNodePos.add(delta);
                break;
        }

        return true;
    }

    public static boolean isHovering() { return hoveringAxis != Axis.NONE; }
    public static boolean isDragging() { return draggingAxis != Axis.NONE; }

    public static void handleScroll(double delta) {
        SceneData.Node node = SceneData.get().getSelectedNode();
        if (node == null) return;

        Axis targetAxis = (draggingAxis != Axis.NONE) ? draggingAxis : hoveringAxis;
        if (targetAxis == Axis.NONE) return;

        double speed = 0.5;
        // 스크롤은 단순히 해당 축 방향으로 더하기
        switch (targetAxis) {
            case X: node.position = node.position.add(delta * speed, 0, 0); break;
            case Y: node.position = node.position.add(0, delta * speed, 0); break;
            case Z: node.position = node.position.add(0, 0, delta * speed); break;
            case ALL:
                VantageCamera cam = VantageCamera.get();
                node.position = node.position.add(cam.getForwardVector().scale(delta * speed));
                break;
        }
    }
}