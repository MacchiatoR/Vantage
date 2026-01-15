package me.horang.vantage.ui.components;

import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
public class OutlinerPanel extends EditorPanel {

    private static final int ITEM_HEIGHT = 15;
    private static final int ITEM_PADDING = 5;

    public OutlinerPanel() {
        super("OUTLINER");
    }

    @Override
    public void init() {}

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int startX = this.x + ITEM_PADDING;
        int currentY = this.y + 25; // 타이틀바 높이 고려

        List<SceneData.Node> nodes = SceneData.get().getNodes();
        SceneData data = SceneData.get();

        if (nodes.isEmpty()) {
            guiGraphics.drawString(mc.font, "No Nodes", startX + 5, currentY, EditorTheme.COLOR_TEXT_MUTED, false);
            return;
        }

        // 노드와 연결선을 순차적으로 그리기
        for (int i = 0; i < nodes.size(); i++) {
            SceneData.Node node = nodes.get(i);

            // 1. 노드 그리기
            drawItem(guiGraphics, mouseX, mouseY, currentY, "Node " + (i + 1), node, null);
            currentY += ITEM_HEIGHT;

            // 2. 해당 노드에서 시작하는 연결선(Line)이 있다면 그리기
            SceneData.Connection conn = data.getConnectionFrom(node);
            if (conn != null) {
                // 노드보다 약간 들여쓰기해서 종속 관계 표현
                drawItem(guiGraphics, mouseX, mouseY, currentY, "  └ Line", null, conn);
                currentY += ITEM_HEIGHT;
            }

            // 화면 밖으로 나가면 그리기 중단 (최적화)
            if (currentY > this.y + this.height) break;
        }
    }

    // 아이템(노드 혹은 라인) 하나를 그리는 헬퍼 메서드
    private void drawItem(GuiGraphics g, int mx, int my, int y, String label, SceneData.Node node, SceneData.Connection conn) {
        SceneData data = SceneData.get();

        // 이 아이템이 선택되었는지 확인
        boolean isSelected = (node != null && data.getSelectedNode() == node) ||
                (conn != null && data.getSelectedConnection() == conn);

        // 이 아이템에 마우스가 올라갔는지 확인
        boolean isHovered = GuiUtils.isMouseOver(mx, my, this.x, y, this.width, ITEM_HEIGHT);

        // 1. 배경 그리기
        if (isSelected) {
            // 선택됨: 강조색
            g.fill(this.x + 2, y, this.x + this.width - 2, y + ITEM_HEIGHT, EditorTheme.COLOR_ACCENT & 0x88FFFFFF);
            GuiUtils.drawBorder(g, this.x + 2, y, this.width - 4, ITEM_HEIGHT, EditorTheme.COLOR_ACCENT);
        } else if (isHovered) {
            // 호버됨: 연한 회색
            g.fill(this.x + 2, y, this.x + this.width - 2, y + ITEM_HEIGHT, 0x22FFFFFF);
        }

        // 2. 아이콘 및 텍스트
        int textColor = isSelected ? 0xFFFFFFFF : EditorTheme.COLOR_TEXT;

        if (node != null) {
            // 노드인 경우
            g.drawString(mc.font, "■", this.x + 5, y + 4, isSelected ? 0xFFFFFFFF : EditorTheme.COLOR_TEXT_MUTED, false);
            g.drawString(mc.font, label, this.x + 18, y + 4, textColor, EditorTheme.FONT_SHADOW);

            // 노드 삭제 버튼 (x)
            drawDeleteButton(g, mx, my, y, node);
        } else {
            // 라인인 경우
            g.drawString(mc.font, "↘", this.x + 10, y + 4, isSelected ? 0xFFFFFFFF : 0xFF888888, false);
            g.drawString(mc.font, "Line", this.x + 22, y + 4, textColor, EditorTheme.FONT_SHADOW);
        }
    }

    private void drawDeleteButton(GuiGraphics g, int mx, int my, int y, SceneData.Node node) {
        int delBtnX = this.x + this.width - 15;
        boolean isHoverDel = GuiUtils.isMouseOver(mx, my, delBtnX, y + 2, 10, 10);
        int delColor = isHoverDel ? 0xFFFF0000 : 0xFF888888;
        g.drawString(mc.font, "x", delBtnX + 2, y + 4, delColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !super.isMouseOver(mouseX, mouseY)) return false;

        int currentY = this.y + 25;
        SceneData data = SceneData.get();
        List<SceneData.Node> nodes = data.getNodes();

        for (SceneData.Node node : nodes) {
            // 1. 노드 영역 클릭 체크
            if (checkClick(mouseX, mouseY, currentY, node, null)) return true;
            currentY += ITEM_HEIGHT;

            // 2. 라인 영역 클릭 체크
            SceneData.Connection conn = data.getConnectionFrom(node);
            if (conn != null) {
                if (checkClick(mouseX, mouseY, currentY, null, conn)) return true;
                currentY += ITEM_HEIGHT;
            }
        }
        return false;
    }

    private boolean checkClick(double mx, double my, int y, SceneData.Node node, SceneData.Connection conn) {
        // 아이템 전체 영역 체크
        if (GuiUtils.isMouseOver(mx, my, this.x, y, this.width, ITEM_HEIGHT)) {

            // 노드일 경우 삭제 버튼 체크
            if (node != null) {
                int delBtnX = this.x + this.width - 15;
                if (GuiUtils.isMouseOver(mx, my, delBtnX, y + 2, 10, 10)) {
                    SceneData.get().removeNode(node);
                    return true;
                }
                // 노드 선택
                SceneData.get().setSelectedNode(node);
            }
            // 라인일 경우
            else if (conn != null) {
                // 라인 선택 (노드 선택은 내부적으로 해제됨)
                SceneData.get().setSelectedConnection(conn);
            }
            return true;
        }
        return false;
    }
}