package me.horang.vantage.ui;

import me.horang.vantage.client.VantageCamera;
import me.horang.vantage.data.ProjectManager;
import me.horang.vantage.ui.components.InspectorPanel;
import me.horang.vantage.ui.components.OutlinerPanel;
import me.horang.vantage.ui.components.TimelinePanel;
import me.horang.vantage.ui.components.TopMenuBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VantageEditorScreen extends Screen {

    // 각 구역별 패널 인스턴스
    private InspectorPanel inspectorPanel;
    private TimelinePanel timelinePanel;
    private OutlinerPanel outlinerPanel;
    private TopMenuBar topMenuBar;

    // 레이아웃 상수
    private static final int TOP_BAR_HEIGHT = 24;
    private static final int TIMELINE_HEIGHT = 60;
    private static final int SIDEBAR_WIDTH = 200;
    private static final int OUTLINER_WIDTH = 150;

    private boolean isCameraRotating = false;

    public VantageEditorScreen() {
        super(Component.literal("Vantage Editor"));
    }

    @Override
    protected void init() {
        // 1. 패널 생성
        if (topMenuBar == null) topMenuBar = new TopMenuBar(this.width); // [New]
        if (inspectorPanel == null) inspectorPanel = new InspectorPanel();
        if (timelinePanel == null) timelinePanel = new TimelinePanel();
        if (outlinerPanel == null) outlinerPanel = new OutlinerPanel();

        // 2. 레이아웃
        int workspaceHeight = this.height - 24 - 60; // topHeight, timelineHeight

        topMenuBar.setBounds(0, 0, this.width, 24); // [New]
        outlinerPanel.setBounds(0, 24, 150, workspaceHeight);
        inspectorPanel.setBounds(this.width - 200, 24, 200, workspaceHeight);
        timelinePanel.setBounds(0, this.height - 60, this.width, 60);

        // 3. 위젯 등록 (등록 순서가 렌더링 순서에 영향)
        // 배경 위에 그려져야 하므로 addRenderableWidget 사용
        this.addRenderableWidget(topMenuBar); // [New]
        this.addRenderableWidget(outlinerPanel);
        this.addRenderableWidget(inspectorPanel);
        this.addRenderableWidget(timelinePanel);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 전체 배경 (어둡게 처리) - 월드가 살짝 비치게
        guiGraphics.fill(0, 0, this.width, this.height, EditorTheme.COLOR_BACKGROUND);

        // 3. 자식 위젯(패널들) 렌더링 - super.render가 addRenderableWidget 된 것들을 그려줌
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 중요: false로 해야 게임 시간이 멈추지 않고 배경이 움직임
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 우클릭(1)이면 카메라 회전 모드 진입
        if (button == 1) {
            isCameraRotating = true;
            // 마우스 커서 숨기고 고정 (선택사항)
            this.minecraft.mouseHandler.grabMouse();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1) {
            isCameraRotating = false;
            this.minecraft.mouseHandler.releaseMouse();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // 마우스 이동 감지 (화면 회전용)
    // Screen 클래스에는 mouseMoved 대신 mouseDragged 같은 걸 쓰거나
    // 직접 InputHandler에서 Delta 값을 가져와야 함.
    // 하지만 가장 쉬운 건 mouseDragged 오버라이드임.

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // mouseMoved는 클릭 안 했을 때.
        // 우클릭 드래그는 mouseDragged에서 처리됨.
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isCameraRotating && button == 1) {
            float sensitivity = 0.3f;
            // dragX, dragY는 마우스 이동량
            VantageCamera.get().addRotation((float)dragX * sensitivity, (float)dragY * sensitivity);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void saveCurrentWork() {
        ProjectManager.CameraSequence currentSeq = new ProjectManager.CameraSequence();
        currentSeq.name = "MyEpicCinematic";
        // currentSeq.keyframes = timelinePanel.getKeyframes(); // 타임라인에서 데이터 가져오기

        ProjectManager.saveSequence("scene_01", currentSeq);
    }
}