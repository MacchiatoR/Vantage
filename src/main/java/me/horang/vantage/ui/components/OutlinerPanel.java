package me.horang.vantage.ui.components;

import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

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

            // [Modified] 다중 선택 여부 확인
            boolean isNodeSelected = data.isSelected(node);

            drawItem(guiGraphics, mouseX, mouseY, currentY, "Node " + (i + 1), node, null, isNodeSelected);
            currentY += ITEM_HEIGHT;

            SceneData.Connection conn = data.getConnectionFrom(node);
            if (conn != null) {
                boolean isConnSelected = (data.getSelectedConnection() == conn);
                drawItem(guiGraphics, mouseX, mouseY, currentY, "  └ Line", null, conn, isConnSelected);
                currentY += ITEM_HEIGHT;
            }
            if (currentY > this.y + this.height) break;
        }
    }

    // 아이템(노드 혹은 라인) 하나를 그리는 헬퍼 메서드
    private void drawItem(GuiGraphics g, int mx, int my, int y, String label, SceneData.Node node, SceneData.Connection conn, boolean isSelected) {
        SceneData data = SceneData.get();

        boolean isHovered = GuiUtils.isMouseOver(mx, my, this.x, y, this.width, ITEM_HEIGHT);

        // 1. 배경 그리기
        if (isSelected) {
            g.fill(this.x + 2, y, this.x + this.width - 2, y + ITEM_HEIGHT, EditorTheme.COLOR_ACCENT & 0x88FFFFFF);
            GuiUtils.drawBorder(g, this.x + 2, y, this.width - 4, ITEM_HEIGHT, EditorTheme.COLOR_ACCENT);
        } else if (isHovered) {
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

        // 키보드 상태 확인 (Screen 클래스 정적 메서드 사용)
        boolean isCtrl = Screen.hasControlDown();
        boolean isShift = Screen.hasShiftDown();

        for (SceneData.Node node : nodes) {
            // 1. 노드 클릭 체크
            if (GuiUtils.isMouseOver(mouseX, mouseY, this.x, currentY, this.width, ITEM_HEIGHT)) {
                // 삭제 버튼
                int delBtnX = this.x + this.width - 15;
                if (GuiUtils.isMouseOver(mouseX, mouseY, delBtnX, currentY + 2, 10, 10)) {
                    data.removeNode(node);
                    return true;
                }

                // [Modified] 선택 로직 분기
                if (isCtrl) {
                    data.toggleNodeSelection(node);
                } else if (isShift) {
                    data.selectRange(node);
                } else {
                    // 이미 선택된 노드를 클릭했고 드래그 의도일 수 있으니,
                    // 단일 선택으로 바꾸지 않고 유지만 하는 로직이 필요할 수도 있지만,
                    // 아웃라이너에서는 보통 클릭 시 바로 선택 변경이 자연스러움.
                    data.selectNode(node, false); // false = 기존 선택 초기화 후 선택
                }
                return true;
            }
            currentY += ITEM_HEIGHT;

            // 2. 라인 클릭 체크
            SceneData.Connection conn = data.getConnectionFrom(node);
            if (conn != null) {
                if (GuiUtils.isMouseOver(mouseX, mouseY, this.x, currentY, this.width, ITEM_HEIGHT)) {
                    // 라인은 다중 선택 지원 안 함 (단일 선택)
                    data.clearSelection(); // 노드 선택 해제
                    data.setSelectedConnection(conn);
                    return true;
                }
                currentY += ITEM_HEIGHT;
            }
        }

        // 빈 공간 클릭 시 선택 해제
        if (mouseY > currentY) {
            data.clearSelection();
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