package me.horang.vantage.ui.components;

import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class TimelinePanel extends EditorPanel {

    private float currentTime = 0f; // 현재 재생 시간 (초 단위 or 틱 단위)
    private float maxTime = 100f;   // 전체 길이

    // 드래그 상태 관리
    private boolean isScrubbing = false;

    public TimelinePanel() {
        super("TIMELINE");
    }

    @Override
    public void init() {}

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int trackY = this.y + 30;
        int trackHeight = 20;
        int trackWidth = this.width - 20;
        int trackX = this.x + 10;

        // 1. 타임라인 트랙 배경 (어두운 홈)
        guiGraphics.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, 0xFF111111);
        GuiUtils.drawBorder(guiGraphics, trackX, trackY, trackWidth, trackHeight, EditorTheme.COLOR_BORDER);

        // 2. 눈금 (Ticks) 그리기
        for (int i = 0; i <= 10; i++) {
            int tickX = trackX + (int)((float)trackWidth * (i / 10f));
            guiGraphics.vLine(tickX, trackY + trackHeight - 5, trackY + trackHeight, 0xFF888888);
        }

        // 3. 플레이 헤드 (재생 위치 표시)
        float progress = currentTime / maxTime;
        int headX = trackX + (int)(trackWidth * progress);

        // 헤드 선 (붉은색 or 액센트색)
        guiGraphics.vLine(headX, this.y + 25, this.y + height - 5, 0xFFFF0000); // 빨간색 지시선

        // 헤드 손잡이
        guiGraphics.fill(headX - 4, trackY - 2, headX + 4, trackY + 8, EditorTheme.COLOR_TEXT_HEADER);

        // 4. 시간 텍스트 표시
        String timeStr = String.format("%.2f / %.2f", currentTime, maxTime);
        guiGraphics.drawString(mc.font, timeStr, this.x + this.width - 80, this.y + 8, EditorTheme.COLOR_TEXT_MUTED, EditorTheme.FONT_SHADOW);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            // 타임라인 트랙 영역을 클릭했는지 확인
            int trackY = this.y + 30;
            if (mouseY >= trackY && mouseY <= trackY + 20) {
                isScrubbing = true;
                updateScrubber(mouseX);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrubbing = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrubbing) {
            updateScrubber(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateScrubber(double mouseX) {
        int trackX = this.x + 10;
        int trackWidth = this.width - 20;

        // 마우스 위치를 0.0 ~ 1.0 비율로 변환
        double relativeX = mouseX - trackX;
        float progress = (float) Mth.clamp(relativeX / trackWidth, 0.0, 1.0);

        this.currentTime = progress * maxTime;
    }
}