package me.horang.vantage.ui.components;

import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;

public class TopMenuBar extends EditorPanel {

    public TopMenuBar(int width) {
        super("MENU"); // 타이틀은 안 쓰지만 부모 생성자 호출
        // 위치 고정
        this.setBounds(0, 0, width, 24);
    }

    @Override
    public void init() {
        // 나중에 여기에 "File", "Edit" 버튼 객체들을 추가할 예정
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 텍스트 대신 실제 버튼처럼 동작할 영역 정의 (Mockup)
        // 나중에는 실제 Button 위젯을 addRenderableWidget으로 넣어야 함.

        int startX = 60;
        int gap = 40;

        drawMenuText(guiGraphics, "File", startX, mouseX, mouseY);
        drawMenuText(guiGraphics, "Edit", startX + gap, mouseX, mouseY);
        drawMenuText(guiGraphics, "View", startX + gap * 2, mouseX, mouseY);

        // 로고 (왼쪽)
        guiGraphics.drawString(mc.font, "VANTAGE", 10, 8, EditorTheme.COLOR_ACCENT, EditorTheme.FONT_SHADOW);
    }

    private void drawMenuText(GuiGraphics g, String text, int x, int mouseX, int mouseY) {
        int y = 8;
        int width = mc.font.width(text);

        // 호버 효과
        boolean hovered = GuiUtils.isMouseOver(mouseX, mouseY, x - 2, 0, width + 4, this.height);
        int color = hovered ? EditorTheme.COLOR_TEXT_HEADER : EditorTheme.COLOR_TEXT;

        g.drawString(mc.font, text, x, y, color, EditorTheme.FONT_SHADOW);
    }

    // EditorPanel의 기본 테두리/배경 그리기 로직을 오버라이드해서
    // 상단바에 맞는 스타일로 변경 (테두리는 아래쪽에만)
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 배경
        guiGraphics.fill(x, y, x + width, y + height, EditorTheme.COLOR_PANEL);

        // 하단 테두리만 그리기
        guiGraphics.hLine(x, x + width, y + height - 1, EditorTheme.COLOR_BORDER);

        renderContent(guiGraphics, mouseX, mouseY, partialTick);
    }
}