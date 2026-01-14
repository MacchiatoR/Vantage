package me.horang.vantage.ui.components;

import me.horang.vantage.ui.EditorTheme;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

public class OutlinerPanel extends EditorPanel {

    // 임시 데이터: 나중에는 ProjectManager에서 데이터를 가져와야 함
    private List<String> keyframeNames;

    public OutlinerPanel() {
        super("OUTLINER");
        keyframeNames = new ArrayList<>();
        // 더미 데이터 추가
        keyframeNames.add("Start Point");
        keyframeNames.add("Chase Sequence");
        keyframeNames.add("Wide Shot");
        keyframeNames.add("End Point");
    }

    @Override
    public void init() {
        // 리스트 스크롤 위젯 등을 초기화할 곳
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int startY = this.y + 25;
        int itemHeight = 15;

        for (int i = 0; i < keyframeNames.size(); i++) {
            int itemY = startY + (i * itemHeight);
            String name = keyframeNames.get(i);

            // 마우스 호버 효과
            boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + itemHeight;
            if (isHovered) {
                guiGraphics.fill(x + 2, itemY, x + width - 2, itemY + itemHeight, 0x33FFFFFF); // 밝은 하이라이트
            }

            // 아이콘 (단순한 점으로 표현)
            guiGraphics.drawString(mc.font, "●", x + 8, itemY + 4, isHovered ? EditorTheme.COLOR_ACCENT : EditorTheme.COLOR_TEXT_MUTED, false);

            // 텍스트
            guiGraphics.drawString(mc.font, name, x + 20, itemY + 4, EditorTheme.COLOR_TEXT, EditorTheme.FONT_SHADOW);
        }
    }
}
