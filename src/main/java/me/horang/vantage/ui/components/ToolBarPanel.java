package me.horang.vantage.ui.components;

import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;

public class ToolBarPanel extends EditorPanel {

    public ToolBarPanel() {
        super("");
    }

    @Override
    public void init() {}

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int startY = this.y + 10;
        int btnSize = 30;
        int gap = 5;
        int x = this.x + 5;

        // 1. SELECT (M) 버튼
        drawToolButton(guiGraphics, mouseX, mouseY, x, startY, "M", SceneData.ToolMode.SELECT);

        // 2. NODE (N) 버튼
        drawToolButton(guiGraphics, mouseX, mouseY, x, startY + btnSize + gap, "N", SceneData.ToolMode.NODE);

        // 3. LINE (L) 버튼
        drawToolButton(guiGraphics, mouseX, mouseY, x, startY + (btnSize + gap) * 2, "L", SceneData.ToolMode.LINE);
    }

    private void drawToolButton(GuiGraphics g, int mx, int my, int x, int y, String label, SceneData.ToolMode mode) {
        int size = 30;
        boolean isSelected = SceneData.get().getTool() == mode;
        boolean isHovered = GuiUtils.isMouseOver(mx, my, x, y, size, size);

        int color = isSelected ? EditorTheme.COLOR_ACCENT : (isHovered ? 0xFF444444 : EditorTheme.COLOR_PANEL);

        g.fill(x, y, x + size, y + size, color);
        GuiUtils.drawBorder(g, x, y, size, size, EditorTheme.COLOR_BORDER);

        int textWidth = mc.font.width(label);
        int textColor = isSelected ? 0xFFFFFFFF : EditorTheme.COLOR_TEXT;
        g.drawString(mc.font, label, x + (size - textWidth) / 2, y + 11, textColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !super.isMouseOver(mouseX, mouseY)) return false;

        int startY = this.y + 10;
        int btnSize = 30;
        int gap = 5;
        int x = this.x + 5;

        // Select (M)
        if (GuiUtils.isMouseOver(mouseX, mouseY, x, startY, btnSize, btnSize)) {
            SceneData.get().setTool(SceneData.ToolMode.SELECT);
            return true;
        }
        // Node (N)
        if (GuiUtils.isMouseOver(mouseX, mouseY, x, startY + btnSize + gap, btnSize, btnSize)) {
            SceneData.get().setTool(SceneData.ToolMode.NODE);
            return true;
        }
        // Line (L)
        if (GuiUtils.isMouseOver(mouseX, mouseY, x, startY + (btnSize + gap) * 2, btnSize, btnSize)) {
            SceneData.get().setTool(SceneData.ToolMode.LINE);
            return true;
        }

        return false; // 툴바 배경 클릭은 무시 (이벤트 전파 방지 위해 true 리턴 권장하지만 여기선 false)
    }
}