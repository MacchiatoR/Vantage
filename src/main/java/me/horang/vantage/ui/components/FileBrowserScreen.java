package me.horang.vantage.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import me.horang.vantage.data.ProjectManager;
import me.horang.vantage.data.SceneData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class FileBrowserScreen extends Screen {

    private final Screen parentScreen;
    private final boolean isSaveMode;
    private File currentDir;
    private EditBox fileNameField;
    private List<File> fileList = new ArrayList<>();
    private File selectedFile = null;

    private int listScrollOffset = 0;
    private static final int ITEM_HEIGHT = 16;

    public FileBrowserScreen(Screen parent, boolean saveMode) {
        super(Component.literal("File Browser"));
        this.parentScreen = parent;
        this.isSaveMode = saveMode;
        this.currentDir = Minecraft.getInstance().gameDirectory;
        refreshFileList();
    }

    private void refreshFileList() {
        fileList.clear();
        File[] files = currentDir.listFiles();
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            for (File f : files) {
                // [수정] 숨김 파일 제외, 디렉토리이거나 .vantage 확장자만 표시
                boolean isProjectFile = f.getName().endsWith(ProjectManager.FILE_EXT);
                if (!f.isHidden() && (f.isDirectory() || isProjectFile)) {
                    fileList.add(f);
                }
            }
        }
        listScrollOffset = 0;
    }

    @Override
    protected void init() {
        int w = 200;
        int h = 20;
        fileNameField = new EditBox(font, 0, 0, w, h, Component.literal("Filename"));
        fileNameField.setMaxLength(256);
        if (isSaveMode) {
            fileNameField.setValue("my_scene"); // 기본 파일명 예시 변경
        }
        this.addRenderableWidget(fileNameField);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 배경 수동 그리기
        g.fill(0, 0, this.width, this.height, 0xAA000000);

        // Z축 앞으로 당기기
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 50.0F);

        int winW = 340;
        int winH = 260;
        int winX = this.width / 2 - winW / 2;
        int winY = this.height / 2 - winH / 2;

        // 창 배경
        g.fill(winX, winY, winX + winW, winY + winH, 0xFF222222);
        drawOutline(g, winX, winY, winW, winH, 0xFF444444);
        g.fill(winX, winY, winX + winW, winY + 24, 0xFF333333);
        g.drawCenteredString(font, isSaveMode ? "SAVE PROJECT" : "LOAD PROJECT", this.width / 2, winY + 8, 0xFFFFFFFF);

        // 경로 텍스트
        String pathStr = "Path: " + currentDir.getAbsolutePath();
        g.drawString(font, truncateString(pathStr, winW - 20), winX + 10, winY + 35, 0xFFAAAAAA, false);

        // 리스트 영역
        int listWidth = 300;
        int listHeight = 150;
        int listLeft = this.width / 2 - listWidth / 2;
        int listTop = winY + 50;
        int listBottom = listTop + listHeight;

        g.fill(listLeft, listTop, listLeft + listWidth, listBottom, 0xFF111111);
        drawOutline(g, listLeft, listTop, listWidth, listHeight, 0xFF444444);

        // 리스트 내용 (Scissor)
        double scale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int)(listLeft * scale),
                (int)((this.height - listBottom) * scale),
                (int)(listWidth * scale),
                (int)((listBottom - listTop) * scale)
        );

        int y = listTop + 2 - listScrollOffset;

        // [..] Parent Directory
        if (y + ITEM_HEIGHT > listTop && y < listBottom) {
            boolean hovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= y && mouseY < y + ITEM_HEIGHT;
            g.drawString(font, "[..] Parent Directory", listLeft + 5, y + 4, hovered ? 0xFFFFFF00 : 0xFF88FF88, false);
        }
        y += ITEM_HEIGHT;

        for (File f : fileList) {
            if (y > listBottom) break;
            if (y + ITEM_HEIGHT < listTop) {
                y += ITEM_HEIGHT;
                continue;
            }

            boolean isSelected = (selectedFile != null && selectedFile.equals(f));
            boolean hovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= y && mouseY < y + ITEM_HEIGHT;

            // [New] 색상 분기 처리
            int color;
            if (f.isDirectory()) {
                color = 0xFFFFAA00; // 폴더: 주황색
            } else if (f.getName().endsWith(ProjectManager.FILE_EXT)) {
                color = 0xFF55FF55; // .vantage 파일: 밝은 연두색 (눈에 띔)
            } else {
                color = 0xFFAAAAAA; // 기타 파일: 회색
            }

            if (isSelected || hovered) {
                g.fill(listLeft + 1, y, listLeft + listWidth - 1, y + ITEM_HEIGHT, isSelected ? 0xFF004488 : 0xFF333333);
            }

            String name = f.isDirectory() ? "[" + f.getName() + "]" : f.getName();
            g.drawString(font, truncateString(name, listWidth - 10), listLeft + 5, y + 4, color, false);
            y += ITEM_HEIGHT;
        }
        RenderSystem.disableScissor();

        // 하단 버튼
        int btnW = 80;
        int btnH = 20;
        int btnY = winY + winH - 30;

        drawButton(g, this.width / 2 - btnW - 10, btnY, btnW, btnH, isSaveMode ? "Save" : "Load", mouseX, mouseY);
        drawButton(g, this.width / 2 + 10, btnY, btnW, btnH, "Cancel", mouseX, mouseY);

        // EditBox 수동 렌더링
        if (fileNameField != null) {
            if (isSaveMode) {
                fileNameField.setX(listLeft);
                fileNameField.setY(btnY - 25);
                fileNameField.setWidth(listWidth);
                fileNameField.setVisible(true);
                g.drawString(font, "Filename (" + ProjectManager.FILE_EXT + "):", listLeft, btnY - 37, 0xFFDDDDDD, false);
                fileNameField.render(g, mouseX, mouseY, partialTick);
            } else {
                fileNameField.setVisible(false);
            }
        }

        g.pose().popPose();
    }

    // --- Helpers ---

    private void drawOutline(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hovered ? 0xFF555555 : 0xFF333333);
        drawOutline(g, x, y, w, h, 0xFF666666);
        g.drawCenteredString(font, label, x + w / 2, y + 6, 0xFFFFFFFF);
    }

    private String truncateString(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        String reversed = new StringBuilder(text).reverse().toString();
        StringBuilder result = new StringBuilder();
        int currentW = ellipsisW;
        for (char c : reversed.toCharArray()) {
            int charW = font.width(String.valueOf(c));
            if (currentW + charW > maxWidth) break;
            currentW += charW;
            result.append(c);
        }
        return ellipsis + result.reverse().toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int winW = 340; int winH = 260; int winY = this.height / 2 - winH / 2;
        int listWidth = 300; int listHeight = 150;
        int listLeft = this.width / 2 - listWidth / 2; int listTop = winY + 50; int listBottom = listTop + listHeight;
        int btnY = winY + winH - 30;

        // 리스트 클릭
        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listBottom) {
            int localY = (int)mouseY - (listTop + 2) + listScrollOffset;
            int index = localY / ITEM_HEIGHT;
            if (index == 0) {
                File parent = currentDir.getParentFile();
                if (parent != null) { currentDir = parent; refreshFileList(); }
                return true;
            }
            int fileIndex = index - 1;
            if (fileIndex >= 0 && fileIndex < fileList.size()) {
                File clicked = fileList.get(fileIndex);
                if (clicked.isDirectory()) { currentDir = clicked; refreshFileList(); }
                else {
                    selectedFile = clicked;
                    // [수정] 파일 선택 시 확장자 제거하고 텍스트박스에 표시
                    if (isSaveMode) fileNameField.setValue(clicked.getName().replace(ProjectManager.FILE_EXT, ""));
                    else fileNameField.setValue(clicked.getName());
                }
                return true;
            }
        }
        // 버튼 클릭
        int btnW = 80;
        if (mouseY >= btnY && mouseY < btnY + 20) {
            int centerX = this.width / 2;
            if (mouseX >= centerX - btnW - 10 && mouseX < centerX - 10) { performAction(); return true; }
            if (mouseX >= centerX + 10 && mouseX < centerX + 10 + btnW) { onClose(); return true; }
        }
        return false;
    }

    private void performAction() {
        if (isSaveMode) {
            String name = fileNameField.getValue();
            if (name.isEmpty()) return;
            if (!name.endsWith(ProjectManager.FILE_EXT)) name += ProjectManager.FILE_EXT;

            File target = new File(currentDir, name);
            // 저장할 때는 내부에서 첫 노드를 기준으로 잡으므로 추가 인자 불필요
            ProjectManager.saveProject(target, SceneData.get());
        } else {
            if (selectedFile != null && selectedFile.exists()) {
                // [수정] 로드 시 현재 플레이어의 위치를 기준점으로 전달
                net.minecraft.world.phys.Vec3 playerPos = Minecraft.getInstance().player.position();
                ProjectManager.loadProject(selectedFile, playerPos);
            }
        }
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        listScrollOffset -= (int)(scrollY * ITEM_HEIGHT);
        listScrollOffset = Math.max(0, listScrollOffset);
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parentScreen);
    }
}