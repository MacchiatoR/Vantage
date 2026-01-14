package me.horang.vantage.ui.components;

import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;

public class ToolBarPanel extends EditorPanel {

    public ToolBarPanel() {
        super(""); // 타이틀 없음
    }

    @Override
    public void init() {}

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int startY = this.y + 10;
        int btnSize = 30;
        int gap = 5;

        // 1. NODE 버튼
        drawToolButton(guiGraphics, mouseX, mouseY, startY, "N", SceneData.ToolMode.NODE);

        // 2. LINE 버튼
        drawToolButton(guiGraphics, mouseX, mouseY, startY + btnSize + gap, "L", SceneData.ToolMode.LINE);
    }

    private void drawToolButton(GuiGraphics g, int mx, int my, int y, String label, SceneData.ToolMode mode) {
        int x = this.x + 5;
        int size = 30;

        boolean isSelected = SceneData.get().getTool() == mode;
        boolean isHovered = GuiUtils.isMouseOver(mx, my, x, y, size, size);

        // 배경색 결정
        int color = EditorTheme.COLOR_PANEL;
        if (isSelected) color = EditorTheme.COLOR_ACCENT; // 선택되면 액센트 컬러
        else if (isHovered) color = 0xFF444444;           // 호버 시 밝게

        g.fill(x, y, x + size, y + size, color);
        GuiUtils.drawBorder(g, x, y, size, size, EditorTheme.COLOR_BORDER);

        // 아이콘 (일단 텍스트로 대체)
        int textWidth = mc.font.width(label);
        g.drawString(mc.font, label, x + (size - textWidth) / 2, y + 11, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.isMouseOver(mouseX, mouseY)) return false;

        int startY = this.y + 10;
        int btnSize = 30;
        int gap = 5;
        int x = this.x + 5;

        // Node 버튼 클릭
        if (GuiUtils.isMouseOver(mouseX, mouseY, x, startY, btnSize, btnSize)) {
            SceneData.get().setTool(SceneData.ToolMode.NODE);
            return true;
        }

        // Line 버튼 클릭
        if (GuiUtils.isMouseOver(mouseX, mouseY, x, startY + btnSize + gap, btnSize, btnSize)) {
            SceneData.get().setTool(SceneData.ToolMode.LINE);
            return true;
        }

        return true;
    }
}