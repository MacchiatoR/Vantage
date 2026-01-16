package me.horang.vantage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.horang.vantage.data.SceneData;
import me.horang.vantage.util.GuiUtils;
import me.horang.vantage.util.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.*;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class GizmoSystem {

    public enum Axis { NONE, MOVE_X, MOVE_Y, MOVE_Z, MOVE_ALL, ROT_X, ROT_Y, ROT_Z }
    public enum GizmoMode { TRANSLATE, ROTATE }

    private static GizmoMode currentMode = GizmoMode.TRANSLATE;
    private static Axis hoveringAxis = Axis.NONE;
    private static Axis draggingAxis = Axis.NONE;

    // [해결: isDragging 필드 추가]
    private static boolean isDragging = false;

    // --- 비주얼 설정 ---
    private static final float ARROW_LENGTH = 0.8f;
    private static final float GIZMO_THICKNESS = 0.1f;
    private static final float ARROW_TIP_SIZE = 0.2f;
    private static final float RING_RADIUS = 1.0f;
    private static final float CENTER_SIZE = 0.15f;

    // --- 히트박스 설정 ---
    private static final double HIT_RADIUS_ARROW = 0.15;
    private static final double HIT_RADIUS_RING = 0.4;

    // --- 드래그 변수 ---
    private static Vec3 dragStartPos = Vec3.ZERO;
    private static Vec3 dragStartHit = Vec3.ZERO;
    private static Vec3 lastDragHitVector = Vec3.ZERO;
    private static boolean hasRotated = false;

    private static Vec3 markerStartLocal = null;
    private static Vec3 markerCurrentLocal = null;
    private static Vec3 dragPlaneNormal = new Vec3(0, 1, 0);

    // --- 정렬용 헬퍼 ---
    private static class RenderPart implements Comparable<RenderPart> {
        Axis axis;
        double distSq;
        public RenderPart(Axis a, double d) { this.axis = a; this.distSq = d; }
        @Override
        public int compareTo(RenderPart o) { return Double.compare(o.distSq, this.distSq); }
    }

    // --- Public Interface ---
    public static void toggleMode() {
        currentMode = (currentMode == GizmoMode.TRANSLATE) ? GizmoMode.ROTATE : GizmoMode.TRANSLATE;
        markerStartLocal = null;
        markerCurrentLocal = null;
    }
    public static GizmoMode getMode() { return currentMode; }
    public static boolean isHovering() { return hoveringAxis != Axis.NONE; }
    public static boolean isDragging() { return isDragging; } // Getter 수정

    // =================================================================================
    // 1. Rendering
    // =================================================================================

    public static void render(PoseStack stack, Vec3 camPos, double ignoredMouseX, double ignoredMouseY) {
        SceneData.Node selected = SceneData.get().getSelectedNode();
        if (selected == null) return;

        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        if (!isDragging) {
            updateHovering(mouseX, mouseY, selected.position, camPos);
        }

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        stack.pushPose();
        stack.translate(selected.position.x - camPos.x, selected.position.y - camPos.y, selected.position.z - camPos.z);

        if (currentMode == GizmoMode.TRANSLATE) {
            boolean hoverAll = (hoveringAxis == Axis.MOVE_ALL || draggingAxis == Axis.MOVE_ALL);
            renderBox(stack, -CENTER_SIZE/2, -CENTER_SIZE/2, -CENTER_SIZE/2, CENTER_SIZE/2, CENTER_SIZE/2, CENTER_SIZE/2, hoverAll ? 0xAAFFFFFF : 0x44FFFFFF);

            List<RenderPart> parts = new ArrayList<>();
            parts.add(new RenderPart(Axis.MOVE_X, getDistanceToAxis(selected.position, Axis.MOVE_X, camPos)));
            parts.add(new RenderPart(Axis.MOVE_Y, getDistanceToAxis(selected.position, Axis.MOVE_Y, camPos)));
            parts.add(new RenderPart(Axis.MOVE_Z, getDistanceToAxis(selected.position, Axis.MOVE_Z, camPos)));
            Collections.sort(parts);

            for (RenderPart part : parts) {
                float flip = getFlipFactor(part.axis, camPos.subtract(selected.position));
                renderBillboardArrow(stack, part.axis, getAxisColor(part.axis), camPos.subtract(selected.position), flip);
            }
        } else {
            List<RenderPart> parts = new ArrayList<>();
            parts.add(new RenderPart(Axis.ROT_X, getDistanceToAxis(selected.position, Axis.ROT_X, camPos)));
            parts.add(new RenderPart(Axis.ROT_Y, getDistanceToAxis(selected.position, Axis.ROT_Y, camPos)));
            parts.add(new RenderPart(Axis.ROT_Z, getDistanceToAxis(selected.position, Axis.ROT_Z, camPos)));
            Collections.sort(parts);

            for (RenderPart part : parts) {
                renderThickRing(stack, part.axis, getAxisColor(part.axis));
            }
            renderRotationMarkers(stack);
        }

        stack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void renderRotationMarkers(PoseStack stack) {
        if (draggingAxis == Axis.NONE || markerStartLocal == null || markerCurrentLocal == null) return;

        int color = getAxisColor(draggingAxis);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        int a = 100;

        double startAngle = getAngleFromAxis(draggingAxis, markerStartLocal);
        double currentAngle = getAngleFromAxis(draggingAxis, markerCurrentLocal);
        double diff = currentAngle - startAngle;

        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        buffer.addVertex(mat, 0, 0, 0).setColor(r, g, b, a);

        int segments = Math.max(2, (int)(Math.abs(Math.toDegrees(diff)) / 5.0));
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double angle = startAngle + (diff * t);
            Vec3 pos = getVectorFromAngle(draggingAxis, angle, RING_RADIUS);
            buffer.addVertex(mat, (float)pos.x, (float)pos.y, (float)pos.z).setColor(r, g, b, a);
        }
        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
        GuiUtils.renderLine(stack, Vec3.ZERO, markerCurrentLocal, 0xFFFFFFFF);
    }

    // =================================================================================
    // 2. Interaction Logic
    // =================================================================================

    public static boolean handlePress(int button, double ignoredMouseX, double ignoredMouseY) {
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        if (button != 0 || hoveringAxis == Axis.NONE) return false;

        draggingAxis = hoveringAxis;
        isDragging = true;

        SceneData.Node node = SceneData.get().getSelectedNode();
        if (node == null) return false;

        dragStartPos = node.position;
        hasRotated = false;

        // [핵심 수정 1] 드래그에 사용할 평면 법선(Normal)을 미리 결정하고 저장합니다.
        // 드래그 도중 카메라가 미세하게 움직여도 평면이 고정되도록 합니다.
        if (isMoveAxis(draggingAxis)) {
            dragPlaneNormal = VantageCamera.get().getForwardVector();
        } else {
            dragPlaneNormal = getAxisVector(draggingAxis);
        }

        Vec3 hit = getRayPlaneIntersection(mouseX, mouseY, dragPlaneNormal, node.position);

        if (hit != null) {
            dragStartHit = hit;

            if (!isMoveAxis(draggingAxis)) {
                // 회전 마커 로직 (기존과 동일)
                Vec3 axisVec = getAxisVector(draggingAxis);
                Vec3 toHit = hit.subtract(node.position);
                Vec3 projectedToHit = toHit.subtract(axisVec.scale(toHit.dot(axisVec)));

                if (projectedToHit.lengthSqr() < 1e-6) {
                    if (Math.abs(axisVec.y) < 0.9) projectedToHit = new Vec3(0, 1, 0).cross(axisVec);
                    else projectedToHit = new Vec3(1, 0, 0).cross(axisVec);
                }
                Vec3 dir = projectedToHit.normalize();
                markerStartLocal = dir.scale(RING_RADIUS);
                markerCurrentLocal = markerStartLocal;
                lastDragHitVector = dir;
            }
            return true;
        } else {
            draggingAxis = Axis.NONE;
            isDragging = false;
            return false;
        }
    }

    public static boolean handleDrag(double ignoredMouseX, double ignoredMouseY) {
        if (!isDragging) return false;

        // [핵심 수정 2] 매개변수(GUI 좌표) 무시하고, 실제 윈도우 좌표(Raw Mouse) 사용
        // handlePress와 동일한 좌표계를 사용해야 튀지 않습니다.
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        SceneData data = SceneData.get();
        SceneData.Node activeNode = data.getSelectedNode();
        if (activeNode == null) return false;

        Vec3 prevPos = activeNode.position;

        if (currentMode == GizmoMode.TRANSLATE) {
            handleMove(mouseX, mouseY, activeNode);
        } else {
            handleRotate(mouseX, mouseY, activeNode);
            return true;
        }

        // 다중 선택 이동 처리
        Vec3 newPos = activeNode.position;
        Vec3 delta = newPos.subtract(prevPos);

        if (delta.lengthSqr() > 0.000001) {
            for (SceneData.Node node : data.getSelectedNodes()) {
                if (node == activeNode) continue;
                node.position = node.position.add(delta);
            }
        }
        return true;
    }

    private static void handleMove(double mouseX, double mouseY, SceneData.Node node) {
        // [핵심 수정 3] 저장해둔 dragPlaneNormal 사용 (일관성 유지)
        Vec3 currentHit = getRayPlaneIntersection(mouseX, mouseY, dragPlaneNormal, dragStartPos);

        if (currentHit != null) {
            Vec3 delta = currentHit.subtract(dragStartHit);
            switch (draggingAxis) {
                // 기존 위치(dragStartPos)에 델타를 더하는 방식은 정확합니다.
                case MOVE_X: node.position = dragStartPos.add(delta.x, 0, 0); break;
                case MOVE_Y: node.position = dragStartPos.add(0, delta.y, 0); break;
                case MOVE_Z: node.position = dragStartPos.add(0, 0, delta.z); break;
                case MOVE_ALL: node.position = dragStartPos.add(delta); break;
            }
        }
    }

    private static void handleRotate(double mouseX, double mouseY, SceneData.Node node) {
        Vec3 axisNormal = getAxisVector(draggingAxis);
        Vec3 currentHit = getRayPlaneIntersection(mouseX, mouseY, axisNormal, node.position);
        if (currentHit == null) return;

        Vec3 currentDir = currentHit.subtract(node.position).normalize();
        markerCurrentLocal = currentDir.scale(RING_RADIUS);

        Vec3 cross = lastDragHitVector.cross(currentDir);
        double dot = lastDragHitVector.dot(currentDir);
        double signedCross = cross.dot(axisNormal);
        double deltaDeg = Math.toDegrees(Math.atan2(signedCross, dot));

        if (Math.abs(deltaDeg) > 0.001) {
            hasRotated = true;
        }

        switch (draggingAxis) {
            case ROT_X: node.pitch += (float) deltaDeg; break;
            case ROT_Y: node.yaw   += (float) deltaDeg; break;
            case ROT_Z: node.roll  += (float) deltaDeg; break;
        }
        lastDragHitVector = currentDir;
    }

    public static void handleRelease() {
        draggingAxis = Axis.NONE;
        isDragging = false;
    }

    // [해결: 가짜 메서드 제거]
    // updatePositionFromMouse는 handleMove 로직과 중복되므로,
    // handleDrag 내부에서 handleMove를 직접 호출하는 방식으로 수정되었습니다.

    // =================================================================================
    // Helper Methods (나머지는 그대로 유지)
    // =================================================================================

    public static void handleScroll(double delta) {
        if (!isHovering() && !isDragging()) return;
        SceneData.Node node = SceneData.get().getSelectedNode();
        if (node == null || currentMode != GizmoMode.TRANSLATE) return;
        Axis axis = (draggingAxis != Axis.NONE) ? draggingAxis : hoveringAxis;
        if (!isMoveAxis(axis)) return;
        double speed = 0.5;
        switch (axis) {
            case MOVE_X: node.position = node.position.add(delta * speed, 0, 0); break;
            case MOVE_Y: node.position = node.position.add(0, delta * speed, 0); break;
            case MOVE_Z: node.position = node.position.add(0, 0, delta * speed); break;
        }
    }

    private static void updateHovering(double mouseX, double mouseY, Vec3 nodePos, Vec3 camPos) {
        RaycastHelper.Ray ray = RaycastHelper.getMouseRay(mouseX, mouseY);
        if (ray == null) return;

        hoveringAxis = Axis.NONE;
        double minDist = Double.MAX_VALUE;
        Vec3 toCam = camPos.subtract(nodePos);

        if (currentMode == GizmoMode.TRANSLATE) {
            double distC = getBoxIntersect(ray, nodePos, new AABB(-CENTER_SIZE/2, -CENTER_SIZE/2, -CENTER_SIZE/2, CENTER_SIZE/2, CENTER_SIZE/2, CENTER_SIZE/2));
            if (distC < minDist) { minDist = distC; hoveringAxis = Axis.MOVE_ALL; }

            double distX = getArrowIntersectStrict(ray, nodePos, Axis.MOVE_X, getFlipFactor(Axis.MOVE_X, toCam));
            if (distX < minDist) { minDist = distX; hoveringAxis = Axis.MOVE_X; }

            double distY = getArrowIntersectStrict(ray, nodePos, Axis.MOVE_Y, getFlipFactor(Axis.MOVE_Y, toCam));
            if (distY < minDist) { minDist = distY; hoveringAxis = Axis.MOVE_Y; }

            double distZ = getArrowIntersectStrict(ray, nodePos, Axis.MOVE_Z, getFlipFactor(Axis.MOVE_Z, toCam));
            if (distZ < minDist) { minDist = distZ; hoveringAxis = Axis.MOVE_Z; }
        } else {
            double distRotX = getRingIntersectStrict(ray, nodePos, Axis.ROT_X);
            if (distRotX < minDist) { minDist = distRotX; hoveringAxis = Axis.ROT_X; }

            double distRotY = getRingIntersectStrict(ray, nodePos, Axis.ROT_Y);
            if (distRotY < minDist) { minDist = distRotY; hoveringAxis = Axis.ROT_Y; }

            double distRotZ = getRingIntersectStrict(ray, nodePos, Axis.ROT_Z);
            if (distRotZ < minDist) { minDist = distRotZ; hoveringAxis = Axis.ROT_Z; }
        }
    }

    private static boolean isMoveAxis(Axis a) { return a == Axis.MOVE_X || a == Axis.MOVE_Y || a == Axis.MOVE_Z || a == Axis.MOVE_ALL; }
    private static boolean isRotateAxis(Axis a) { return a == Axis.ROT_X || a == Axis.ROT_Y || a == Axis.ROT_Z; }

    private static int getAxisColor(Axis axis) {
        switch(axis) {
            case MOVE_X: case ROT_X: return 0xFFFF0000;
            case MOVE_Y: case ROT_Y: return 0xFF00FF00;
            case MOVE_Z: case ROT_Z: return 0xFF0000FF;
            default: return 0xFFFFFFFF;
        }
    }

    private static Vec3 getAxisVector(Axis axis) {
        switch (axis) {
            case MOVE_X: case ROT_X: return new Vec3(1, 0, 0);
            case MOVE_Y: case ROT_Y: return new Vec3(0, 1, 0);
            case MOVE_Z: case ROT_Z: return new Vec3(0, 0, 1);
            default: return new Vec3(0, 1, 0);
        }
    }

    private static double getDistanceToAxis(Vec3 nodePos, Axis axis, Vec3 camPos) {
        Vec3 dir = getAxisVector(axis);
        Vec3 toCam = camPos.subtract(nodePos);
        if (isRotateAxis(axis)) {
            return nodePos.distanceToSqr(camPos);
        } else {
            float flip = getFlipFactor(axis, toCam);
            Vec3 center = nodePos.add(dir.scale(ARROW_LENGTH * 0.5 * flip));
            return center.distanceToSqr(camPos);
        }
    }

    private static float getFlipFactor(Axis axis, Vec3 toCam) {
        Vec3 axisDir = getAxisVector(axis);
        return axisDir.dot(toCam) < 0 ? -1.0f : 1.0f;
    }

    private static Vec3 getRayPlaneIntersection(double mouseX, double mouseY, Vec3 planeNormal, Vec3 planePoint) {
        RaycastHelper.Ray ray = RaycastHelper.getMouseRay(mouseX, mouseY);
        if (ray == null) return null;
        double denom = ray.dir.dot(planeNormal);
        if (Math.abs(denom) < 1e-4) return null;
        double t = planePoint.subtract(ray.origin).dot(planeNormal) / denom;
        if (t < 0) return null;
        return ray.origin.add(ray.dir.scale(t));
    }

    private static double getArrowIntersectStrict(RaycastHelper.Ray ray, Vec3 nodePos, Axis axis, float flip) {
        Vec3 rawAxis = getAxisVector(axis);
        Vec3 dir = rawAxis.scale(flip);
        Vec3 pStart = nodePos.add(dir.scale(CENTER_SIZE * 0.5));
        Vec3 pEnd = nodePos.add(dir.scale(ARROW_LENGTH));
        double dist = RaycastHelper.getDistanceFromLine(ray, pStart, pEnd);
        if (dist < HIT_RADIUS_ARROW) return ray.origin.distanceTo(pStart);
        return Double.MAX_VALUE;
    }

    private static double getRingIntersectStrict(RaycastHelper.Ray ray, Vec3 nodePos, Axis axis) {
        Vec3 planeNormal = getAxisVector(axis);
        double denom = ray.dir.dot(planeNormal);
        if (Math.abs(denom) < 1e-6) return Double.MAX_VALUE;
        double t = nodePos.subtract(ray.origin).dot(planeNormal) / denom;
        if (t < 0) return Double.MAX_VALUE;
        Vec3 hitPos = ray.origin.add(ray.dir.scale(t));
        double distFromCenter = hitPos.distanceTo(nodePos);
        if (Math.abs(distFromCenter - RING_RADIUS) < HIT_RADIUS_RING) return hitPos.distanceTo(ray.origin);
        return Double.MAX_VALUE;
    }

    private static double getBoxIntersect(RaycastHelper.Ray ray, Vec3 offset, AABB localBox) {
        AABB worldBox = localBox.move(offset);
        return worldBox.clip(ray.origin, ray.origin.add(ray.dir.scale(100.0))).map(vec -> vec.distanceTo(ray.origin)).orElse(Double.MAX_VALUE);
    }

    // --- Drawing Helpers (이전 코드와 동일, 생략 없음) ---
    private static void renderBillboardArrow(PoseStack stack, Axis axis, int color, Vec3 toCam, float flip) {
        boolean highlight = (hoveringAxis == axis || draggingAxis == axis);
        if (highlight) color = 0xFFFFFF55;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();
        Vec3 rawAxis = getAxisVector(axis);
        Vec3 axisDir = rawAxis.scale(flip);
        Vec3 widthDir = axisDir.cross(toCam).normalize().scale(GIZMO_THICKNESS / 2.0f);
        Vec3 pStart = axisDir.scale(CENTER_SIZE * 0.5f);
        Vec3 pEnd = axisDir.scale(ARROW_LENGTH);
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = (color) & 0xFF;
        addQuadVec(buffer, mat, pStart.subtract(widthDir), pEnd.subtract(widthDir), pEnd.add(widthDir), pStart.add(widthDir), r, g, b, a);
        float tipW = ARROW_TIP_SIZE;
        Vec3 tipWidth = axisDir.cross(toCam).normalize().scale(tipW);
        Vec3 pTipEnd = pEnd.add(axisDir.scale(tipW * 1.5));
        addQuadVec(buffer, mat, pEnd.subtract(tipWidth), pTipEnd, pTipEnd, pEnd.add(tipWidth), r, g, b, a);
        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    private static void renderThickRing(PoseStack stack, Axis axis, int color) {
        boolean highlight = (hoveringAxis == axis || draggingAxis == axis);
        if (highlight) color = 0xFFFFFF55;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = (color) & 0xFF;
        int segments = 60;
        float innerRad = RING_RADIUS - (GIZMO_THICKNESS / 2.0f);
        float outerRad = RING_RADIUS + (GIZMO_THICKNESS / 2.0f);
        for (int i = 0; i <= segments; i++) {
            double ang = (Math.PI * 2 * i) / segments;
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);
            float xIn=0, yIn=0, zIn=0, xOut=0, yOut=0, zOut=0;
            switch (axis) {
                case ROT_X: xIn=0; yIn=sin*innerRad; zIn=cos*innerRad; xOut=0; yOut=sin*outerRad; zOut=cos*outerRad; break;
                case ROT_Y: xIn=cos*innerRad; yIn=0; zIn=sin*innerRad; xOut=cos*outerRad; yOut=0; zOut=sin*outerRad; break;
                case ROT_Z: xIn=cos*innerRad; yIn=sin*innerRad; zIn=0; xOut=cos*outerRad; yOut=sin*outerRad; zOut=0; break;
            }
            buffer.addVertex(mat, xIn, yIn, zIn).setColor(r, g, b, a);
            buffer.addVertex(mat, xOut, yOut, zOut).setColor(r, g, b, a);
        }
        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    static void renderBox(PoseStack stack, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = (color) & 0xFF;
        addBoxQuad(buf, mat, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
        try { BufferUploader.drawWithShader(buf.buildOrThrow()); } catch (Exception e) {}
    }

    private static void addBoxQuad(BufferBuilder b, Matrix4f m, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int r, int g, int bl, int a) {
        addQuad(b, m, minX,maxY,maxZ, maxX,maxY,maxZ, maxX,minY,maxZ, minX,minY,maxZ, r,g,bl,a);
        addQuad(b, m, maxX,maxY,minZ, minX,maxY,minZ, minX,minY,minZ, maxX,minY,minZ, r,g,bl,a);
        addQuad(b, m, minX,maxY,minZ, minX,maxY,maxZ, minX,minY,maxZ, minX,minY,minZ, r,g,bl,a);
        addQuad(b, m, maxX,maxY,maxZ, maxX,maxY,minZ, maxX,minY,minZ, maxX,minY,maxZ, r,g,bl,a);
        addQuad(b, m, minX,maxY,minZ, maxX,maxY,minZ, maxX,maxY,maxZ, minX,maxY,maxZ, r,g,bl,a);
        addQuad(b, m, minX,minY,maxZ, maxX,minY,maxZ, maxX,minY,minZ, minX,minY,minZ, r,g,bl,a);
    }

    private static void addQuadVec(BufferBuilder b, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int r, int g, int bl, int a) {
        b.addVertex(m, (float)p1.x, (float)p1.y, (float)p1.z).setColor(r,g,bl,a);
        b.addVertex(m, (float)p2.x, (float)p2.y, (float)p2.z).setColor(r,g,bl,a);
        b.addVertex(m, (float)p3.x, (float)p3.y, (float)p3.z).setColor(r,g,bl,a);
        b.addVertex(m, (float)p3.x, (float)p3.y, (float)p3.z).setColor(r,g,bl,a);
        b.addVertex(m, (float)p4.x, (float)p4.y, (float)p4.z).setColor(r,g,bl,a);
        b.addVertex(m, (float)p1.x, (float)p1.y, (float)p1.z).setColor(r,g,bl,a);
    }

    private static void addQuad(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int bl, int a) {
        b.addVertex(m, x1, y1, z1).setColor(r,g,bl,a); b.addVertex(m, x2, y2, z2).setColor(r,g,bl,a); b.addVertex(m, x3, y3, z3).setColor(r,g,bl,a);
        b.addVertex(m, x3, y3, z3).setColor(r,g,bl,a); b.addVertex(m, x4, y4, z4).setColor(r,g,bl,a); b.addVertex(m, x1, y1, z1).setColor(r,g,bl,a);
    }

    private static double getAngleFromAxis(Axis axis, Vec3 vec) {
        switch (axis) {
            case ROT_X: return Math.atan2(vec.z, vec.y);
            case ROT_Y: return Math.atan2(vec.z, vec.x);
            case ROT_Z: return Math.atan2(vec.y, vec.x);
            default: return 0;
        }
    }

    private static Vec3 getVectorFromAngle(Axis axis, double angle, float radius) {
        double cos = Math.cos(angle) * radius;
        double sin = Math.sin(angle) * radius;
        switch (axis) {
            case ROT_X: return new Vec3(0, cos, sin);
            case ROT_Y: return new Vec3(cos, 0, sin);
            case ROT_Z: return new Vec3(cos, sin, 0);
            default: return Vec3.ZERO;
        }
    }
}