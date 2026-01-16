package me.horang.vantage.util;


import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class GuiUtils {

    // =================================================================================
    // 2D GUI Rendering
    // =================================================================================

    /**
     * 속이 빈 사각형 테두리를 그립니다.
     */
    public static void drawBorder(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.hLine(x, x + width - 1, y, color);
        g.hLine(x, x + width - 1, y + height - 1, color);
        g.vLine(x, y, y + height - 1, color);
        g.vLine(x + width - 1, y, y + height - 1, color);
    }

    /**
     * 마우스가 특정 영역 안에 있는지 확인합니다.
     */
    public static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // =================================================================================
    // 3D World Rendering (Moved from WorldRenderer)
    // =================================================================================

    /**
     * 3D 공간에 선을 그립니다.
     */
    public static void renderLine(PoseStack stack, Vec3 start, Vec3 end, int color) {
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

    /**
     * 노드(큐브)의 면을 그립니다.
     * @param size 노드의 크기 (예: 0.5f)
     */
    public static void renderNodeGeometry(PoseStack stack, float size, int bodyColor, int frontColor) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        float s = size / 2.0f;

        int a = (bodyColor >> 24) & 0xFF; int r = (bodyColor >> 16) & 0xFF; int g = (bodyColor >> 8) & 0xFF; int b = (bodyColor) & 0xFF;
        int fa = (frontColor >> 24) & 0xFF; int fr = (frontColor >> 16) & 0xFF; int fg = (frontColor >> 8) & 0xFF; int fb = (frontColor) & 0xFF;

        // --- 1. 메인 큐브 바디 ---
        addQuad(buffer, mat, s, s, -s, -s, s, -s, -s, -s, -s, s, -s, -s, r, g, b, a); // Back
        addQuad(buffer, mat, -s, s, -s, -s, s, s, -s, -s, s, -s, -s, -s, r, g, b, a); // Left
        addQuad(buffer, mat, s, s, s, s, s, -s, s, -s, -s, s, -s, s, r, g, b, a);     // Right
        addQuad(buffer, mat, -s, s, -s, s, s, -s, s, s, s, -s, s, s, r, g, b, a);     // Top
        addQuad(buffer, mat, -s, -s, s, s, -s, s, s, -s, -s, -s, -s, -s, r, g, b, a); // Bottom
        addQuad(buffer, mat, -s, s, s, s, s, s, s, -s, s, -s, -s, s, fr, fg, fb, fa); // Front

        // --- 2. 방향 표시 돌기 (Nose) ---
        float noseSize = s * 0.4f;
        float noseDepth = s + 0.1f;

        // 돌기 앞면
        addQuad(buffer, mat, -noseSize, noseSize, noseDepth, noseSize, noseSize, noseDepth,
                noseSize, -noseSize, noseDepth, -noseSize, -noseSize, noseDepth, fr, fg, fb, fa);

        // 돌기 옆면들
        addQuad(buffer, mat, -noseSize, noseSize, s, noseSize, noseSize, s, noseSize, noseSize, noseDepth, -noseSize, noseSize, noseDepth, fr, fg, fb, fa); // Top
        addQuad(buffer, mat, -noseSize, -noseSize, noseDepth, noseSize, -noseSize, noseDepth, noseSize, -noseSize, s, -noseSize, -noseSize, s, fr, fg, fb, fa); // Bottom
        addQuad(buffer, mat, noseSize, noseSize, s, noseSize, noseSize, noseDepth, noseSize, -noseSize, noseDepth, noseSize, -noseSize, s, fr, fg, fb, fa); // Right
        addQuad(buffer, mat, -noseSize, noseSize, noseDepth, -noseSize, noseSize, s, -noseSize, -noseSize, s, -noseSize, -noseSize, noseDepth, fr, fg, fb, fa); // Left

        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    /**
     * 노드(큐브)의 테두리 선을 그립니다.
     */
    public static void renderCubeEdges(PoseStack stack, float size, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        float s = size / 2.0f;
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

    /**
     * 와이어프레임 박스를 그립니다. (Anchor 등 렌더링용)
     */
    public static void renderWireframeBox(PoseStack stack, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = stack.last().pose();

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;

        // Bottom rect
        addLine(buffer, mat, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(buffer, mat, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Top rect
        addLine(buffer, mat, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, mat, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(buffer, mat, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Pillars
        addLine(buffer, mat, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, mat, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, mat, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);

        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); } catch (Exception e) {}
    }

    // --- Private Helpers ---

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

    public static void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hovered ? 0xFF666666 : 0xFF444444);

        // [수정됨] 테두리 그리기
        drawOutline(g, x, y, w, h, 0xFF888888);

        g.drawCenteredString(Minecraft.getInstance().font, label, x + w/2, y + 6, 0xFFFFFFFF);
    }

    // [New] 테두리를 그리는 헬퍼 메서드 추가
    public static void drawOutline(GuiGraphics g, int x, int y, int width, int height, int color) {
        // 상단
        g.fill(x, y, x + width, y + 1, color);
        // 하단
        g.fill(x, y + height - 1, x + width, y + height, color);
        // 좌측
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        // 우측
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}