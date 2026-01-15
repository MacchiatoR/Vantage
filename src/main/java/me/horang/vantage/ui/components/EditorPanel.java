package me.horang.vantage.ui.components;

import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public abstract class EditorPanel implements Renderable, GuiEventListener, NarratableEntry {
    protected int x, y, width, height;
    protected String title;
    protected Minecraft mc = Minecraft.getInstance();

    // [New] 포커스 상태 관리 변수
    private boolean focused = false;
    public boolean visible = true;
    public EditorPanel(String title) {
        this.title = title;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        init();
    }

    public abstract void init();

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // [New] 보이지 않는 상태면 렌더링 중단
        if (!visible) return;

        // 배경
        guiGraphics.fill(x, y, x + width, y + height, EditorTheme.COLOR_PANEL);
        // 테두리
        GuiUtils.drawBorder(guiGraphics, x, y, width, height, EditorTheme.COLOR_BORDER);

        // 타이틀 (내용이 있을 때만)
        if (this.title != null && !this.title.isEmpty()) {
            guiGraphics.drawString(mc.font, this.title, x + EditorTheme.PADDING, y + EditorTheme.PADDING, EditorTheme.COLOR_TEXT_HEADER, EditorTheme.FONT_SHADOW);
            guiGraphics.hLine(x, x + width, y + 20, EditorTheme.COLOR_BORDER);
        }

        renderContent(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    // [Fix] GuiEventListener 필수 구현 메서드
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [New] 안 보이면 클릭도 안 됨
        if (!visible) return false;

        if (isMouseOver(mouseX, mouseY)) {
            setFocused(true);
            return true;
        }
        setFocused(false);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible) return false;
        return false; // 기본적으로 드래그 처리 안 함 (자식에서 오버라이드)
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // [New] 안 보이면 마우스가 위에 있어도 무시
        if (!visible) return false;
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override
    public void updateNarration(NarrationElementOutput output) {}
}