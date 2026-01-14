package me.horang.vantage.util;


import net.minecraft.client.gui.GuiGraphics;

public class GuiUtils {

    /**
     * 속이 빈 사각형 테두리를 그립니다.
     * GuiGraphics의 fill()은 속을 채우지만, 이건 외곽선만 그립니다.
     */
    public static void drawBorder(GuiGraphics g, int x, int y, int width, int height, int color) {
        // 상단 가로선
        g.hLine(x, x + width - 1, y, color);
        // 하단 가로선
        g.hLine(x, x + width - 1, y + height - 1, color);
        // 좌측 세로선
        g.vLine(x, y, y + height - 1, color);
        // 우측 세로선
        g.vLine(x + width - 1, y, y + height - 1, color);
    }

    /**
     * 마우스가 특정 영역 안에 있는지 확인합니다.
     */
    public static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // 나중에 추가할 수 있는 유용한 메서드들 예시:
    // public static void drawDashedLine(...)
    // public static void drawGradientBorder(...)
}
