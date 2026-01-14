package me.horang.vantage.ui;

import me.horang.vantage.client.VantageCamera;
import me.horang.vantage.ui.components.InspectorPanel;
import me.horang.vantage.ui.components.TimelinePanel;
import me.horang.vantage.ui.components.TopMenuBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VantageEditorScreen extends Screen {

    // 각 구역별 패널 인스턴스
    private InspectorPanel inspectorPanel;
    private TimelinePanel timelinePanel;
    private TopMenuBar topMenuBar;
    private ToolBarPanel toolBarPanel;
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
        if (toolBarPanel == null) toolBarPanel = new ToolBarPanel();
        // 2. 레이아웃
        int workspaceHeight = this.height - 24 - 60; // topHeight, timelineHeight

        topMenuBar.setBounds(0, 0, this.width, 24); // [New]
        inspectorPanel.setBounds(this.width - 200, 24, 200, workspaceHeight);
        timelinePanel.setBounds(0, this.height - 60, this.width, 60);
        toolBarPanel.setBounds(0, 24, 40, this.height - 24 - 60);

        // 3. 위젯 등록 (등록 순서가 렌더링 순서에 영향)
        // 배경 위에 그려져야 하므로 addRenderableWidget 사용
        this.addRenderableWidget(topMenuBar); // [New]
        this.addRenderableWidget(inspectorPanel);
        this.addRenderableWidget(timelinePanel);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 자식 위젯(패널들) 렌더링 - super.render가 addRenderableWidget 된 것들을 그려줌
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 중요: false로 해야 게임 시간이 멈추지 않고 배경이 움직임
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // UI(패널) 위에 마우스가 있는지 확인
        boolean isHoveringUI =
                (toolBarPanel != null && toolBarPanel.isMouseOver(mouseX, mouseY)) ||
                        (inspectorPanel != null && inspectorPanel.isMouseOver(mouseX, mouseY)) ||
                        (timelinePanel != null && timelinePanel.isMouseOver(mouseX, mouseY)); // TopBar 등 추가 확인 필요

        // UI 위가 아니고, 좌클릭(0)이면 -> 카메라 회전 모드
        if (!isHoveringUI && button == 0) {
            isCameraRotating = true;
            this.minecraft.mouseHandler.grabMouse();
            return true;
        }

        // [수정 3] 3D 월드 상의 노드 클릭/배치 로직은 여기서 호출
        if (!isHoveringUI && button == 0) {
            // SceneController.handleClick(...) 나중에 구현
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isCameraRotating) { // 좌클릭 떼면 회전 종료
            isCameraRotating = false;
            this.minecraft.mouseHandler.releaseMouse();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 좌클릭(0) 드래그 시 회전
        if (isCameraRotating && button == 0) {
            float sensitivity = 0.3f;
            VantageCamera.get().addRotation((float)dragX * sensitivity, (float)dragY * sensitivity);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}