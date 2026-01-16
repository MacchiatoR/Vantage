package me.horang.vantage.ui;

import me.horang.vantage.client.GizmoSystem;
import me.horang.vantage.client.VantageCamera;
import me.horang.vantage.client.WorldRenderer;
import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.components.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class VantageEditorScreen extends Screen {

    // --- UI Components ---
    private TopMenuBar topMenuBar;
    private ToolBarPanel toolBarPanel;
    private OutlinerPanel outlinerPanel;
    private InspectorPanel inspectorPanel;
    private TimelinePanel timelinePanel;

    // --- State Management ---
    // View 메뉴를 통해 아웃라이너를 켜고 끌 수 있는 전역 플래그
    public static boolean showOutliner = true;

    // 마우스 조작 상태
    private boolean isCameraRotating = false;
    private boolean wasDragging = false;

    public VantageEditorScreen() {
        super(Component.literal("Vantage Editor"));
    }

    @Override
    protected void init() {
        // 1. 컴포넌트 초기화 (싱글톤처럼 유지하고 싶다면 static으로 관리해도 되지만,
        // 화면 크기 변경 대응을 위해 init에서 재생성하거나 bounds를 갱신해야 함)

        if (topMenuBar == null) topMenuBar = new TopMenuBar(this.width);
        if (toolBarPanel == null) toolBarPanel = new ToolBarPanel();
        if (outlinerPanel == null) outlinerPanel = new OutlinerPanel();
        if (inspectorPanel == null) inspectorPanel = new InspectorPanel();
        if (timelinePanel == null) timelinePanel = new TimelinePanel();

        // 2. 레이아웃 치수 정의
        int topHeight = 24;
        int bottomHeight = 60;
        int leftToolbarWidth = 40;
        int leftOutlinerWidth = 150;
        int rightInspectorWidth = 200;

        int workspaceHeight = this.height - topHeight - bottomHeight;

        // 3. 각 패널 위치 및 크기 설정
        // 상단 메뉴바
        topMenuBar.setBounds(0, 0, this.width, topHeight);

        // 좌측 툴바 (아이콘)
        toolBarPanel.setBounds(0, topHeight, leftToolbarWidth, workspaceHeight);

        // 좌측 아웃라이너 (툴바 바로 옆)
        outlinerPanel.setBounds(leftToolbarWidth, topHeight, leftOutlinerWidth, workspaceHeight);

        // 우측 인스펙터 (속성창)
        inspectorPanel.setBounds(this.width - rightInspectorWidth, topHeight, rightInspectorWidth, workspaceHeight);

        // 하단 타임라인
        timelinePanel.setBounds(0, this.height - bottomHeight, this.width, bottomHeight);

        // 4. 위젯 등록 (렌더링 및 이벤트 처리를 위해 필수)
        this.addRenderableWidget(topMenuBar);
        this.addRenderableWidget(toolBarPanel);
        this.addRenderableWidget(outlinerPanel);
        this.addRenderableWidget(inspectorPanel);
        this.addRenderableWidget(timelinePanel);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. UI 패널들의 가시성(Visibility) 동적 제어

        // 아웃라이너: View 메뉴 변수에 따름
        outlinerPanel.visible = showOutliner;

        // [Fix] 인스펙터 표시 조건 수정
        // 기존: 노드가 선택되었을 때만 표시
        // 수정: 노드 또는 연결선(Line) 중 하나라도 선택되어 있으면 표시
        SceneData data = SceneData.get();
        inspectorPanel.visible = (data.getSelectedNode() != null || data.getSelectedConnection() != null);

        // 상단바, 툴바, 타임라인은 항상 표시
        topMenuBar.visible = true;
        toolBarPanel.visible = true;
        timelinePanel.visible = true;

        // 2. 위젯 렌더링
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 배경을 검게 칠하거나 블러 처리하는 기본 동작을 무력화합니다.
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Do nothing -> 투명 배경
    }

    // --- Input Handling ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. 마인크래프트 기본 위젯(슬라이더, 버튼 등) 클릭 처리
        // 내부 컴포넌트가 이벤트를 소비했다면(true), 여기서 중단합니다.
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 2. 커스텀 UI 패널 클릭 감지 (패널 위를 클릭했을 때 월드가 클릭되는 것을 방지)
        // 패널 내부의 로직(Outliner 클릭 등)은 각 패널의 mouseClicked에서 이미 처리되었거나,
        // super.mouseClicked가 처리하지 못한 배경 클릭 등을 여기서 막습니다.
        if (isMouseOverUI(mouseX, mouseY)) {
            return true;
        }

        SceneData data = SceneData.get();
        boolean isLeftClick = (button == 0);

        // 3. SELECT 모드일 때의 로직 (기즈모 및 노드 선택)
        if (data.getTool() == SceneData.ToolMode.SELECT && isLeftClick) {

            // A. 기즈모(화살표) 클릭 체크
            // 기즈모를 클릭했다면 드래그 모드로 진입하고 종료
            if (GizmoSystem.handlePress(button, mouseX, mouseY)) {
                wasDragging = true;
                return true;
            }

            // B. 3D 월드 노드 클릭 체크 (Raycast)
            SceneData.Node hitNode = WorldRenderer.raycastNodes(mouseX, mouseY);

            if (hitNode != null) {
                boolean isCtrl = Screen.hasControlDown(); // Ctrl 키 상태
                boolean isShift = Screen.hasShiftDown();  // Shift 키 상태

                if (isCtrl) {
                    // [CTRL] 토글 선택 (선택됨 -> 해제, 안됨 -> 선택)
                    data.toggleNodeSelection(hitNode);
                }
                else if (isShift) {
                    // [SHIFT] 범위 선택 (마지막 선택된 노드부터 현재까지)
                    data.selectRange(hitNode);
                }
                else {
                    // [일반 클릭]
                    // 만약 이미 선택된 노드를 클릭했다면? -> 드래그 준비일 수 있으므로 선택을 즉시 해제하지 않음
                    // 만약 선택되지 않은 노드를 클릭했다면? -> 단일 선택으로 변경
                    if (!data.isSelected(hitNode)) {
                        data.selectNode(hitNode, false); // false = 기존 선택 모두 해제하고 얘만 선택
                    } else {
                        // 이미 선택된 그룹 중 하나를 클릭함.
                        // (이후 mouseDragged가 발생하면 다 같이 이동,
                        //  mouseReleased가 발생하면 얘만 남기고 나머지 해제하는 로직이 필요할 수 있음.
                        //  여기서는 단순하게 활성 노드(ActiveNode)만 갱신해줍니다.)
                        data.selectNode(hitNode, true); // true = 기존 선택 유지
                    }
                }
                return true; // 노드를 클릭했으므로 카메라 회전 등으로 넘어가지 않음
            }
        }

        // 4. 월드 빈 공간 클릭 처리 (카메라 회전 시작)
        // UI도 아니고, 기즈모도 아니고, 노드도 아닌 허공을 클릭했을 때
        if (isLeftClick) {
            // 허공을 클릭하면 선택 해제 (단, Ctrl/Shift가 없을 때만)
            if (!Screen.hasControlDown() && !Screen.hasShiftDown()) {
                data.clearSelection();
            }

            isCameraRotating = true;
            wasDragging = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isCameraRotating = false;
            GizmoSystem.handleRelease();
            // [Fix] 드래그가 아니었고, && 마우스를 뗄 때도 UI 위가 아니어야 함!
            if (!wasDragging && !isMouseOverUI(mouseX, mouseY)) {
                WorldRenderer.handleClick(button);
            }

            wasDragging = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // UI 드래그 처리 (슬라이더 등)
        if (SceneData.get().getTool() == SceneData.ToolMode.SELECT && GizmoSystem.handleDrag(mouseX, mouseY)) {
            wasDragging = true;
            return true;
        }
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        // 월드 카메라 회전 (좌클릭 드래그 시)
        if (isCameraRotating && button == 0) {
            wasDragging = true; // 드래그 중임을 표시 (클릭 이벤트 방지)

            float sensitivity = 0.3f; // 회전 감도
            // 마우스 이동량을 카메라에 전달
            VantageCamera.get().addRotation((float)dragX * sensitivity, (float)dragY * sensitivity);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { // 1.21 등 버전에 따라 파라미터 다를 수 있음 (deltaX, deltaY)
        // 네오포지 버전에 따라: double deltaX, double deltaY 혹은 double amount
        // 보통 scrollY가 휠 값입니다.

        double wheelDelta = scrollY; // 1.20+ 기준

        // 1. 기즈모/축이 호버 중이면 -> 노드 이동
        if (GizmoSystem.isHovering() || GizmoSystem.isDragging()) {
            GizmoSystem.handleScroll(wheelDelta);
            return true;
        }

        // 2. 아니면 -> 카메라 앞뒤 이동 (M 모드 등)
        double speed = VantageCamera.get().getMoveSpeed(); // 혹은 고정 속도
        VantageCamera.get().moveForward(wheelDelta * speed);

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // [New] 'R' 키로 기즈모 모드 전환 (Translate <-> Rotate)
        if (keyCode == GLFW.GLFW_KEY_R) {
            GizmoSystem.toggleMode();
            return true;
        }

        // 기존 UI 패널 입력 처리
        if (topMenuBar.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (toolBarPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (outlinerPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (inspectorPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (timelinePanel.keyPressed(keyCode, scanCode, modifiers)) return true;

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // --- Lifecycle ---

    /**
     * ESC 키 등을 눌러 에디터를 닫을 때 호출됩니다.
     */
    @Override
    public void onClose() {
        shutdownEditor();
        super.onClose();
    }

    /**
     * 에디터 종료 시 정리 작업
     * - 카메라 하이재킹 해제
     * - 1인칭 시점 복귀
     */
    public void shutdownEditor() {
        VantageCamera.get().setActive(false);
        if (this.minecraft.player != null) {
            this.minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        }
    }

    /**
     * false를 반환하여 에디터가 켜져 있어도 게임 시간이 흐르도록(배경이 움직이도록) 함.
     * true면 싱글플레이 시 월드가 멈춤.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isMouseOverUI(double mouseX, double mouseY) {
        return (topMenuBar.visible && topMenuBar.isMouseOver(mouseX, mouseY)) ||
                (toolBarPanel.visible && toolBarPanel.isMouseOver(mouseX, mouseY)) ||
                (outlinerPanel.visible && outlinerPanel.isMouseOver(mouseX, mouseY)) ||
                (inspectorPanel.visible && inspectorPanel.isMouseOver(mouseX, mouseY)) ||
                (timelinePanel.visible && timelinePanel.isMouseOver(mouseX, mouseY));
    }

}