package me.horang.vantage.ui;

import me.horang.vantage.data.ProjectManager;
import me.horang.vantage.ui.components.InspectorPanel;
import me.horang.vantage.ui.components.OutlinerPanel;
import me.horang.vantage.ui.components.TimelinePanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VantageEditorScreen extends Screen {

    // 각 구역별 패널 인스턴스
    private InspectorPanel inspectorPanel;
    private TimelinePanel timelinePanel;
    private OutlinerPanel outlinerPanel;

    // 레이아웃 상수
    private static final int TOP_BAR_HEIGHT = 24;
    private static final int TIMELINE_HEIGHT = 60;
    private static final int SIDEBAR_WIDTH = 200;
    private static final int OUTLINER_WIDTH = 150;

    public VantageEditorScreen() {
        super(Component.literal("Vantage Editor"));
    }

    @Override
    protected void init() {
        // 1. 패널 인스턴스 생성 (없으면)
        if (inspectorPanel == null) inspectorPanel = new InspectorPanel();
        if (timelinePanel == null) timelinePanel = new TimelinePanel();
        if (outlinerPanel == null) outlinerPanel = new OutlinerPanel();

        // 2. 동적 레이아웃 계산 (반응형 UI)
        int workspaceHeight = this.height - TOP_BAR_HEIGHT - TIMELINE_HEIGHT;
        int workspaceWidth = this.width - OUTLINER_WIDTH - SIDEBAR_WIDTH;

        // 좌측: 아웃라이너
        outlinerPanel.setBounds(0, TOP_BAR_HEIGHT, OUTLINER_WIDTH, workspaceHeight);

        // 우측: 인스펙터
        inspectorPanel.setBounds(this.width - SIDEBAR_WIDTH, TOP_BAR_HEIGHT, SIDEBAR_WIDTH, workspaceHeight);

        // 하단: 타임라인
        timelinePanel.setBounds(0, this.height - TIMELINE_HEIGHT, this.width, TIMELINE_HEIGHT);

        // 3. 이벤트 리스너 등록 (클릭 등 처리)
        this.addRenderableWidget(outlinerPanel);
        this.addRenderableWidget(inspectorPanel);
        this.addRenderableWidget(timelinePanel);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 전체 배경 (어둡게 처리) - 월드가 살짝 비치게
        guiGraphics.fill(0, 0, this.width, this.height, EditorTheme.COLOR_BACKGROUND);

        // 2. 상단 메뉴바 그리기 (간단해서 별도 클래스 없이 여기서 직접 그림)
        renderTopBar(guiGraphics);

        // 3. 자식 위젯(패널들) 렌더링 - super.render가 addRenderableWidget 된 것들을 그려줌
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTopBar(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, TOP_BAR_HEIGHT, EditorTheme.COLOR_PANEL);
        guiGraphics.hLine(0, this.width, TOP_BAR_HEIGHT - 1, EditorTheme.COLOR_BORDER);

        // 로고 / 타이틀
        guiGraphics.drawString(this.font, "VANTAGE", 10, 8, EditorTheme.COLOR_ACCENT, EditorTheme.FONT_SHADOW);

        // 메뉴 버튼 시늉 (나중에 실제 버튼으로 교체)
        guiGraphics.drawString(this.font, "File", 70, 8, EditorTheme.COLOR_TEXT, EditorTheme.FONT_SHADOW);
        guiGraphics.drawString(this.font, "Edit", 100, 8, EditorTheme.COLOR_TEXT, EditorTheme.FONT_SHADOW);
        guiGraphics.drawString(this.font, "View", 130, 8, EditorTheme.COLOR_TEXT, EditorTheme.FONT_SHADOW);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 중요: false로 해야 게임 시간이 멈추지 않고 배경이 움직임
    }

    private void saveCurrentWork() {
        ProjectManager.CameraSequence currentSeq = new ProjectManager.CameraSequence();
        currentSeq.name = "MyEpicCinematic";
        // currentSeq.keyframes = timelinePanel.getKeyframes(); // 타임라인에서 데이터 가져오기

        ProjectManager.saveSequence("scene_01", currentSeq);
    }
}