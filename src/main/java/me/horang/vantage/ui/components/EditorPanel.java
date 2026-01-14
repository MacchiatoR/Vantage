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
        // 배경
        guiGraphics.fill(x, y, x + width, y + height, EditorTheme.COLOR_PANEL);

        // [수정됨] GuiUtils를 사용하여 테두리 그리기
        GuiUtils.drawBorder(guiGraphics, x, y, width, height, EditorTheme.COLOR_BORDER);

        // 타이틀
        guiGraphics.drawString(mc.font, this.title, x + EditorTheme.PADDING, y + EditorTheme.PADDING, EditorTheme.COLOR_TEXT_HEADER, EditorTheme.FONT_SHADOW);
        guiGraphics.hLine(x, x + width, y + 20, EditorTheme.COLOR_BORDER);

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
        if (isMouseOver(mouseX, mouseY)) {
            setFocused(true);
            return true; // 이벤트를 여기서 소비함
        }
        setFocused(false);
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override
    public void updateNarration(NarrationElementOutput output) {}
}