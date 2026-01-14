package me.horang.vantage.ui.components;

import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;

public class InspectorPanel extends EditorPanel {

    public InspectorPanel() {
        super("PROPERTIES");
    }

    @Override
    public void init() {
        // 나중에 여기에 EditBox(입력창)나 Slider 위젯을 추가할 예정
        // 예: this.addRenderableWidget(new EditBox(...));
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int contentStartY = this.y + 25;
        int textX = this.x + EditorTheme.PADDING;

        // 속성 라벨들 (Mockup)
        drawPropertyRow(guiGraphics, "Position X", "1024.5", textX, contentStartY);
        drawPropertyRow(guiGraphics, "Position Y", "64.0", textX, contentStartY + 20);
        drawPropertyRow(guiGraphics, "Position Z", "-500.2", textX, contentStartY + 40);

        // 구분선
        guiGraphics.hLine(this.x + 5, this.x + this.width - 5, contentStartY + 65, EditorTheme.COLOR_BORDER);

        drawPropertyRow(guiGraphics, "Yaw", "90.0", textX, contentStartY + 75);
        drawPropertyRow(guiGraphics, "Pitch", "15.5", textX, contentStartY + 95);
        drawPropertyRow(guiGraphics, "FOV", "70.0", textX, contentStartY + 115);
    }

    // 헬퍼 메서드: 라벨과 값을 예쁘게 그려줌
    private void drawPropertyRow(GuiGraphics g, String label, String value, int x, int y) {
        // 라벨
        g.drawString(mc.font, label, x, y, EditorTheme.COLOR_TEXT_MUTED, EditorTheme.FONT_SHADOW);

        int valBoxX = this.x + this.width - 60;
        int valBoxW = 50;
        int valBoxH = 12;

        // 배경
        g.fill(valBoxX, y - 2, valBoxX + valBoxW, y - 2 + valBoxH, 0xFF111111);

        // [수정됨] GuiUtils 사용
        GuiUtils.drawBorder(g, valBoxX, y - 2, valBoxW, valBoxH, EditorTheme.COLOR_BORDER);

        // 값
        g.drawString(mc.font, value, valBoxX + 3, y, EditorTheme.COLOR_TEXT, EditorTheme.FONT_SHADOW);
    }
}
