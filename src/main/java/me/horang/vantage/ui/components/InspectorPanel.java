package me.horang.vantage.ui.components;

import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
public class InspectorPanel extends EditorPanel {

    // 노드용 필드
    private EditBox startX, startY, startZ;
    private EditBox yaw, pitch;

    // 라인용 필드
    private EditBox duration;
    private EditBox tension;

    private List<EditBox> nodeBoxes = new ArrayList<>();
    private List<EditBox> lineBoxes = new ArrayList<>();

    public InspectorPanel() {
        super("PROPERTIES");
    }

    @Override
    public void init() {
        nodeBoxes.clear();
        lineBoxes.clear();

        int w = 60; // 너비를 조금 더 넓힘
        int h = 16; // 높이도 조금 키움 (클릭 편하게)

        // 노드 속성
        startX = createBox(nodeBoxes, "0", w, h);
        startY = createBox(nodeBoxes, "0", w, h);
        startZ = createBox(nodeBoxes, "0", w, h);
        yaw = createBox(nodeBoxes, "0", w, h);
        pitch = createBox(nodeBoxes, "0", w, h);

        // 라인 속성
        duration = createBox(lineBoxes, "2.0", w, h);
        tension = createBox(lineBoxes, "0.0", w, h);
    }

    private EditBox createBox(List<EditBox> list, String defaultVal, int w, int h) {
        EditBox box = new EditBox(mc.font, 0, 0, w, h, Component.literal(""));
        box.setMaxLength(10);
        box.setValue(defaultVal);
        box.setBordered(true); // 테두리 켜기
        box.setTextColor(0xFFFFFFFF); // 텍스트 흰색
        // 숫자, 소수점, 마이너스만 입력 가능
        box.setFilter(s -> s.isEmpty() || s.matches("-?[0-9.]*"));
        list.add(box);
        return box;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        SceneData data = SceneData.get();

        // 레이아웃 변수
        int labelX = this.x + 10;
        int inputX = this.x + 80;
        int startY = this.y + 25;
        int gap = 22; // 간격 벌림

        // --- CASE 1: 노드 선택됨 ---
        if (data.getSelectedNode() != null) {
            SceneData.Node node = data.getSelectedNode();
            Vec3 relPos = data.toRelative(node.position);

            // 값 동기화 (포커스 아닐 때만)
            if (!startX.isFocused()) startX.setValue(String.format("%.2f", relPos.x));
            if (!this.startY.isFocused()) this.startY.setValue(String.format("%.2f", relPos.y));
            if (!startZ.isFocused()) startZ.setValue(String.format("%.2f", relPos.z));
            if (!yaw.isFocused()) yaw.setValue(String.format("%.2f", node.yaw));
            if (!pitch.isFocused()) pitch.setValue(String.format("%.2f", node.pitch));

            // 그리기
            drawField(guiGraphics, "Rel X", startX, labelX, inputX, startY, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Rel Y", this.startY, labelX, inputX, startY + gap, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Rel Z", startZ, labelX, inputX, startY + gap * 2, mouseX, mouseY, partialTick);

            guiGraphics.hLine(this.x + 5, this.x + width - 5, startY + gap * 3 + 2, EditorTheme.COLOR_BORDER);

            drawField(guiGraphics, "Yaw", yaw, labelX, inputX, startY + gap * 3 + 8, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Pitch", pitch, labelX, inputX, startY + gap * 4 + 8, mouseX, mouseY, partialTick);

            // 매 프레임 적용 (실시간 반영을 원할 경우)
            // 만약 엔터 칠 때만 적용하고 싶으면 이 줄을 제거하고 keyPressed에서 처리해야 함
            applyNodeChanges(node);
        }

        // --- CASE 2: 라인 선택됨 ---
        else if (data.getSelectedConnection() != null) {
            SceneData.Connection conn = data.getSelectedConnection();

            if (!duration.isFocused()) duration.setValue(String.format("%.2f", conn.duration));
            if (!tension.isFocused()) tension.setValue(String.format("%.2f", conn.tension));

            guiGraphics.drawString(mc.font, "CONNECTION", labelX, startY, EditorTheme.COLOR_ACCENT, false);

            drawField(guiGraphics, "Duration (s)", duration, labelX, inputX, startY + gap, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Tension", tension, labelX, inputX, startY + gap * 2, mouseX, mouseY, partialTick);

            guiGraphics.drawString(mc.font, "0=Linear, >0=Curve", labelX, startY + gap * 3 + 10, EditorTheme.COLOR_TEXT_MUTED, false);

            applyLineChanges(conn);
        }

        // --- CASE 3: 선택 없음 ---
        else {
            guiGraphics.drawString(mc.font, "No Selection", labelX, startY, EditorTheme.COLOR_TEXT_MUTED, false);
        }
    }

    private void drawField(GuiGraphics g, String label, EditBox box, int lx, int ix, int y, int mx, int my, float pt) {
        g.drawString(mc.font, label, lx, y + 4, EditorTheme.COLOR_TEXT, false);

        // 1. 박스 위치 강제 업데이트 (매우 중요: 클릭 판정을 위해)
        box.setX(ix);
        box.setY(y);

        // 2. 포커스 시각적 피드백 (노란 테두리)
        if (box.isFocused()) {
            GuiUtils.drawBorder(g, ix - 1, y - 1, box.getWidth() + 2, box.getHeight() + 2, 0xFFFFFF00);
        }

        // 3. 박스 렌더링
        box.render(g, mx, my, pt);
    }

    // --- 데이터 적용 로직 ---

    private void applyNodeChanges(SceneData.Node node) {
        try {
            double x = parseDouble(startX);
            double y = parseDouble(startY);
            double z = parseDouble(startZ);
            float yRot = parseFloat(yaw);
            float xRot = parseFloat(pitch);

            node.position = SceneData.get().toWorld(new Vec3(x, y, z));
            node.yaw = yRot;
            node.pitch = xRot;
        } catch (Exception e) {}
    }

    private void applyLineChanges(SceneData.Connection conn) {
        try {
            float dur = parseFloat(duration);
            float ten = parseFloat(tension);

            if (dur < 0.1f) dur = 0.1f; // 최소값 방어
            conn.duration = dur;
            conn.tension = ten;
        } catch (Exception e) {}
    }

    private double parseDouble(EditBox box) throws NumberFormatException {
        String s = box.getValue();
        if (s.isEmpty() || s.equals("-")) throw new NumberFormatException();
        return Double.parseDouble(s);
    }

    private float parseFloat(EditBox box) throws NumberFormatException {
        String s = box.getValue();
        if (s.isEmpty() || s.equals("-")) throw new NumberFormatException();
        return Float.parseFloat(s);
    }

    // --- 입력 이벤트 핸들링 (중요) ---

    private List<EditBox> getActiveBoxes() {
        SceneData data = SceneData.get();
        if (data.getSelectedNode() != null) return nodeBoxes;
        if (data.getSelectedConnection() != null) return lineBoxes;
        return new ArrayList<>();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        List<EditBox> activeBoxes = getActiveBoxes();
        boolean clickedAny = false;

        // [Fix] 클릭 시점의 박스 좌표가 정확해야 함.
        // renderContent에서 setX/setY를 하지만, 클릭 이벤트가 렌더링보다 먼저 올 수 있음.
        // 안전을 위해 여기서도 대략적인 위치를 잡아주거나, 렌더링 루프가 1회 돈 후라고 가정.
        // 여기선 renderContent가 좌표를 잡은 상태를 믿되, activeBoxes를 순회함.

        for (EditBox box : activeBoxes) {
            // EditBox의 내부 mouseClicked 호출 (여기서 포커스 상태가 바뀜)
            if (box.mouseClicked(mouseX, mouseY, button)) {
                setFocused(true); // 패널 포커스
                box.setFocused(true); // 박스 포커스 확실히
                clickedAny = true;
            } else {
                box.setFocused(false);
            }
        }

        if (clickedAny) return true;

        // 빈 곳 클릭 시 포커스 해제
        for (EditBox box : activeBoxes) box.setFocused(false);
        setFocused(false);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        List<EditBox> activeBoxes = getActiveBoxes();
        for (EditBox box : activeBoxes) {
            if (box.keyPressed(keyCode, scanCode, modifiers)) {
                // 엔터키 누르면 포커스 해제 (입력 완료)
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    box.setFocused(false);
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!visible) return false;

        List<EditBox> activeBoxes = getActiveBoxes();
        for (EditBox box : activeBoxes) {
            if (box.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}