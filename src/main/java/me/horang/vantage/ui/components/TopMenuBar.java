package me.horang.vantage.ui.components;

import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.ui.VantageEditorScreen;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class TopMenuBar extends EditorPanel {

    private boolean isFileMenuOpen = false;

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
        int startX = 60;
        int gap = 40;

        // 메뉴 버튼들 그리기
        drawMenuText(guiGraphics, "File", startX, mouseX, mouseY);
        drawMenuText(guiGraphics, "Edit", startX + gap, mouseX, mouseY);
        drawMenuText(guiGraphics, "View", startX + gap * 2, mouseX, mouseY);

        guiGraphics.drawString(mc.font, "VANTAGE", 10, 8, EditorTheme.COLOR_ACCENT, EditorTheme.FONT_SHADOW);

        // [Fix] 드롭다운 렌더링 수정
        if (isFileMenuOpen) {
            // 중요: 드롭다운이 다른 패널에 가려지지 않도록 Z축을 500만큼 앞으로 당김
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0f, 0.0f, 500.0f);

            int menuX = startX;
            int menuY = 24;
            int menuW = 80;
            int menuH = 44;

            // 배경
            guiGraphics.fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xFF333333);

            // 테두리 (직접 그리기)
            int borderCol = 0xFF888888;
            guiGraphics.fill(menuX, menuY, menuX + menuW, menuY + 1, borderCol); // 상
            guiGraphics.fill(menuX, menuY + menuH - 1, menuX + menuW, menuY + menuH, borderCol); // 하
            guiGraphics.fill(menuX, menuY + 1, menuX + 1, menuY + menuH - 1, borderCol); // 좌
            guiGraphics.fill(menuX + menuW - 1, menuY + 1, menuX + menuW, menuY + menuH - 1, borderCol); // 우

            // Save Item
            boolean hoverSave = GuiUtils.isMouseOver(mouseX, mouseY, menuX, menuY, menuW, 22);
            guiGraphics.fill(menuX + 1, menuY + 1, menuX + menuW - 1, menuY + 21, hoverSave ? 0xFF555555 : 0xFF333333);
            guiGraphics.drawString(mc.font, "Save", menuX + 10, menuY + 7, 0xFFFFFFFF);

            // Load Item
            boolean hoverLoad = GuiUtils.isMouseOver(mouseX, mouseY, menuX, menuY + 22, menuW, 22);
            guiGraphics.fill(menuX + 1, menuY + 23, menuX + menuW - 1, menuY + 43, hoverLoad ? 0xFF555555 : 0xFF333333);
            guiGraphics.drawString(mc.font, "Load", menuX + 10, menuY + 29, 0xFFFFFFFF);

            // 중요: Z축 복구
            guiGraphics.pose().popPose();
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = 60;

        // 1. 드롭다운이 열려있을 때의 클릭 처리
        if (isFileMenuOpen) {
            int menuX = startX;
            int menuY = 24;

            // Save Click
            if (GuiUtils.isMouseOver(mouseX, mouseY, menuX, menuY, 80, 22)) {
                isFileMenuOpen = false;
                // 현재 화면(VantageEditorScreen)을 부모로 넘겨서 끝나면 돌아오게 함
                Minecraft.getInstance().setScreen(new FileBrowserScreen(Minecraft.getInstance().screen, true));
                return true;
            }
            // Load Click
            if (GuiUtils.isMouseOver(mouseX, mouseY, menuX, menuY + 22, 80, 22)) {
                isFileMenuOpen = false;
                Minecraft.getInstance().setScreen(new FileBrowserScreen(Minecraft.getInstance().screen, false));
                return true;
            }

            // 드롭다운 외부 클릭 시 닫기
            isFileMenuOpen = false;
            return true;
        }

        // 2. 상단 바 메뉴 클릭 처리
        if (super.isMouseOver(mouseX, mouseY)) {
            // File Button Click
            if (GuiUtils.isMouseOver(mouseX, mouseY, startX, 0, 40, 24)) {
                isFileMenuOpen = !isFileMenuOpen;
                return true;
            }
            // View Button Click
            if (GuiUtils.isMouseOver(mouseX, mouseY, startX + 80, 0, 40, 24)) {
                VantageEditorScreen.showOutliner = !VantageEditorScreen.showOutliner;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}