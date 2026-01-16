package me.horang.vantage.ui.components;

import me.horang.vantage.data.SceneData;
import me.horang.vantage.ui.EditorTheme;
import me.horang.vantage.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InspectorPanel extends EditorPanel {

    private EditBox startX, startY, startZ;
    private EditBox yaw, pitch, roll;

    private EditBox durationBox;
    private SimpleButton interpBtn;
    private FloatSlider tensionSlider;
    private CurvePreviewWidget curvePreview; // [New] 그래프 위젯
    private SimpleButton easingBtn;
    private FloatSlider easeStrengthSlider;
    private FloatSlider angleSlider;
    private List<GuiEventListener> nodeWidgets = new ArrayList<>();
    private List<GuiEventListener> lineWidgets = new ArrayList<>();

    public InspectorPanel() {
        super("PROPERTIES");
    }

    @Override
    public void init() {
        nodeWidgets.clear();
        lineWidgets.clear();

        int w = 50;
        int h = 16;
        int fullW = 100;

        // Node Widgets
        startX = createBox(nodeWidgets, "0", w, h);
        startY = createBox(nodeWidgets, "0", w, h);
        startZ = createBox(nodeWidgets, "0", w, h);
        yaw = createBox(nodeWidgets, "0", w, h);
        pitch = createBox(nodeWidgets, "0", w, h);
        roll = createBox(nodeWidgets, "0", w, h);

        // Line Widgets
        durationBox = createBox(lineWidgets, "2.0", w, h);

        interpBtn = new SimpleButton(0, 0, fullW, h, "Linear", (SimpleButton btn) -> {
            SceneData.Connection c = SceneData.get().getSelectedConnection();
            if (c != null) {
                c.interpType = c.interpType.next();
                btn.label = c.interpType.label;
            }
        });
        lineWidgets.add(interpBtn);

        tensionSlider = new FloatSlider(0, 0, fullW, h, "Curvature", 0.0f, 1.0f, 0.5f);
        lineWidgets.add(tensionSlider);

        // [New] 그래프 위젯 추가 (크기 100x40)
        curvePreview = new CurvePreviewWidget(0, 0, fullW, 40);
        lineWidgets.add(curvePreview);

        angleSlider = new FloatSlider(0, 0, 100, 16, "Angle", 0.0f, 360.0f, 0.0f);
        lineWidgets.add(angleSlider);

        easingBtn = new SimpleButton(0, 0, fullW, h, "Linear", (SimpleButton btn) -> {
            SceneData.Connection c = SceneData.get().getSelectedConnection();
            if (c != null) {
                c.easingType = c.easingType.next();
                btn.label = c.easingType.label;
            }
        });
        lineWidgets.add(easingBtn);

        easeStrengthSlider = new FloatSlider(0, 0, fullW, h, "Strength", 0.0f, 3.0f, 1.0f);
        lineWidgets.add(easeStrengthSlider);
    }

    private EditBox createBox(List<GuiEventListener> list, String defaultVal, int w, int h) {
        EditBox box = new EditBox(mc.font, 0, 0, w, h, Component.literal(""));
        box.setMaxLength(10);
        box.setValue(defaultVal);
        box.setBordered(true);
        box.setTextColor(0xFFFFFFFF);
        box.setFilter(s -> s.isEmpty() || s.matches("-?[0-9.]*"));
        list.add(box);
        return box;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        SceneData data = SceneData.get();
        int labelX = this.x + 10;
        int inputX = this.x + 80;
        int startY = this.y + 25;
        int gap = 20;

        if (data.getSelectedNode() != null) {
            SceneData.Node node = data.getSelectedNode();
            updateNodeFields(node);

            drawLabel(guiGraphics, "POSITION", labelX, startY);
            drawField(guiGraphics, "X", startX, labelX, inputX, startY + 12, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Y", this.startY, labelX, inputX, startY + 12 + gap, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Z", startZ, labelX, inputX, startY + 12 + gap * 2, mouseX, mouseY, partialTick);

            int rotY = startY + 12 + gap * 3 + 10;
            drawLabel(guiGraphics, "ROTATION", labelX, rotY);
            drawField(guiGraphics, "Yaw", yaw, labelX, inputX, rotY + 12, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Pitch", pitch, labelX, inputX, rotY + 12 + gap, mouseX, mouseY, partialTick);
            drawField(guiGraphics, "Roll", roll, labelX, inputX, rotY + 12 + gap * 2, mouseX, mouseY, partialTick);

            applyNodeChanges(node);
        }
        else if (data.getSelectedConnection() != null) {
            SceneData.Connection conn = data.getSelectedConnection();
            updateLineFields(conn);

            int y = startY;
            drawLabel(guiGraphics, "TIMING", labelX, y);
            drawField(guiGraphics, "Duration (s)", durationBox, labelX, inputX, y + 12, mouseX, mouseY, partialTick);
            y += 40;

            drawLabel(guiGraphics, "PATH SHAPE", labelX, y);
            interpBtn.x = this.x + 10;
            interpBtn.y = y + 12;
            interpBtn.render(guiGraphics, mouseX, mouseY, partialTick);
            y += 12 + gap + 5;

            // [New] 그래프 렌더링
            if (conn.interpType != SceneData.InterpolationType.LINEAR) {
                // 1. 그래프
                curvePreview.x = this.x + 10;
                curvePreview.y = y;
                curvePreview.render(guiGraphics, mouseX, mouseY, partialTick);
                y += 45; // 그래프 높이 + 여백

                // 2. Tension 슬라이더
                tensionSlider.x = this.x + 10;
                tensionSlider.y = y;
                tensionSlider.label = (conn.interpType == SceneData.InterpolationType.BEZIER) ? "Height" : "Tension";
                tensionSlider.render(guiGraphics, mouseX, mouseY, partialTick);
                conn.tension = tensionSlider.getValue();
                y += gap;

                // 3. [New] Angle 슬라이더 (BEZIER 모드일 때만 표시)
                if (conn.interpType == SceneData.InterpolationType.BEZIER) {
                    angleSlider.x = this.x + 10;
                    angleSlider.y = y;
                    angleSlider.render(guiGraphics, mouseX, mouseY, partialTick);
                    conn.trajectoryAngle = angleSlider.getValue();
                    y += gap; // 슬라이더 높이만큼 Y 증가
                }
            } else {
                // LINEAR일 때는 아무것도 안 그리지만, 위에서 이미 기본 y 증가를 했으므로
                // VELOCITY 라벨과 겹치지 않게 됩니다.
            }
            y += 5;

            drawLabel(guiGraphics, "VELOCITY", labelX, y);
            easingBtn.x = this.x + 10;
            easingBtn.y = y + 12;
            easingBtn.render(guiGraphics, mouseX, mouseY, partialTick);

            if (conn.easingType != SceneData.EasingType.LINEAR) {
                easeStrengthSlider.x = this.x + 10;
                easeStrengthSlider.y = y + 12 + gap;
                easeStrengthSlider.render(guiGraphics, mouseX, mouseY, partialTick);
                conn.easingStrength = easeStrengthSlider.getValue();
            }

            applyLineChanges(conn);
        } else {
            guiGraphics.drawString(mc.font, "No Selection", labelX, startY, EditorTheme.COLOR_TEXT_MUTED, false);
        }
    }

    // ... (updateNodeFields, updateLineFields, apply 등 헬퍼 메서드는 기존과 동일) ...
    private void updateNodeFields(SceneData.Node node) {
        Vec3 relPos = SceneData.get().toRelative(node.position);
        if (!startX.isFocused()) startX.setValue(fmt(relPos.x));
        if (!startY.isFocused()) startY.setValue(fmt(relPos.y));
        if (!startZ.isFocused()) startZ.setValue(fmt(relPos.z));
        if (!yaw.isFocused()) yaw.setValue(fmt(node.yaw));
        if (!pitch.isFocused()) pitch.setValue(fmt(node.pitch));
        if (!roll.isFocused()) roll.setValue(fmt(node.roll));
    }
    private void updateLineFields(SceneData.Connection conn) {
        if (!durationBox.isFocused()) durationBox.setValue(fmt(conn.duration));
        interpBtn.label = conn.interpType.label;
        easingBtn.label = conn.easingType.label;
        if (!tensionSlider.isDragging()) tensionSlider.setValue(conn.tension);
        if (!easeStrengthSlider.isDragging()) easeStrengthSlider.setValue(conn.easingStrength);

        // [New]
        if (!angleSlider.isDragging()) angleSlider.setValue(conn.trajectoryAngle);
    }
    private void applyNodeChanges(SceneData.Node node) {
        try {
            node.position = SceneData.get().toWorld(new Vec3(pD(startX), pD(startY), pD(startZ)));
            node.yaw = pF(yaw); node.pitch = pF(pitch); node.roll = pF(roll);
        } catch (Exception e) {}
    }
    private void applyLineChanges(SceneData.Connection conn) {
        try { conn.duration = Math.max(0.1f, pF(durationBox)); } catch (Exception e) {}
    }
    private void drawLabel(GuiGraphics g, String text, int x, int y) { g.drawString(mc.font, text, x, y, EditorTheme.COLOR_ACCENT, false); }
    private void drawField(GuiGraphics g, String label, EditBox box, int lx, int ix, int y, int mx, int my, float pt) {
        g.drawString(mc.font, label, lx, y + 4, EditorTheme.COLOR_TEXT, false);
        box.setX(ix); box.setY(y); box.render(g, mx, my, pt);
        if (box.isFocused()) GuiUtils.drawBorder(g, ix - 1, y - 1, box.getWidth() + 2, box.getHeight() + 2, 0xFFFFFF00);
    }
    private String fmt(double val) { return String.format("%.2f", val); }
    private double pD(EditBox b) { return Double.parseDouble(b.getValue()); }
    private float pF(EditBox b) { return Float.parseFloat(b.getValue()); }

    // --- Input Handling ---
    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible) return false;
        List<GuiEventListener> widgets = (SceneData.get().getSelectedNode() != null) ? nodeWidgets : lineWidgets;
        for (GuiEventListener w : widgets) {
            if (w.mouseClicked(mx, my, btn)) { setFocused(true); if(w instanceof EditBox)((EditBox)w).setFocused(true); return true; }
            else if(w instanceof EditBox)((EditBox)w).setFocused(false);
        }
        return super.mouseClicked(mx, my, btn);
    }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (!visible) return false;
        List<GuiEventListener> widgets = (SceneData.get().getSelectedNode() != null) ? nodeWidgets : lineWidgets;
        for (GuiEventListener w : widgets) if (w.mouseDragged(mx, my, btn, dx, dy)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int btn) {
        if (!visible) return false;
        List<GuiEventListener> widgets = (SceneData.get().getSelectedNode() != null) ? nodeWidgets : lineWidgets;
        for (GuiEventListener w : widgets) if (w.mouseReleased(mx, my, btn)) return true;
        return super.mouseReleased(mx, my, btn);
    }
    @Override public boolean charTyped(char c, int mod) {
        if (!visible) return false;
        List<GuiEventListener> widgets = (SceneData.get().getSelectedNode() != null) ? nodeWidgets : lineWidgets;
        for (GuiEventListener w : widgets) if (w.charTyped(c, mod)) return true;
        return super.charTyped(c, mod);
    }
    @Override public boolean keyPressed(int k, int s, int m) {
        if (!visible) return false;
        List<GuiEventListener> widgets = (SceneData.get().getSelectedNode() != null) ? nodeWidgets : lineWidgets;
        for (GuiEventListener w : widgets) if (w.keyPressed(k, s, m)) return true;
        return super.keyPressed(k, s, m);
    }

    // --- Custom Widgets ---

    // [New] 그래프 위젯 (곡선 미리보기)
    private static class CurvePreviewWidget implements GuiEventListener, Renderable {
        public int x, y, width, height;

        public CurvePreviewWidget(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            // 배경
            g.fill(x, y, x + width, y + height, 0xFF222222);
            GuiUtils.drawBorder(g, x, y, width, height, 0xFF666666);

            // 그래프 그리기
            SceneData.Connection conn = SceneData.get().getSelectedConnection();
            if (conn == null) return;

            // 2D 시뮬레이션:
            // Start: (x, y+height) -> 좌하단
            // End: (x+width, y+height) -> 우하단 (Arc 모드일 때 위로 휘게 하기 위해)
            // Spline은 중앙선 기준으로 휨

            int segments = 20;
            double prevX = x;
            double prevY = y + height - 5; // 바닥 기준

            // 시뮬레이션을 위해 가짜 Vec3 사용
            // Arc 모드: 시작(0,0,0) -> 끝(100,0,0). Y축으로 휨.
            Vec3 pStart = new Vec3(0, 0, 0);
            Vec3 pEnd = new Vec3(width, 0, 0);

            // 그래프 중앙 라인
            g.hLine(x, x + width, y + height/2, 0xFF444444);

            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;

                // 수학 계산만 SceneData 로직을 빌려씀
                Vec3 p;
                if (conn.interpType == SceneData.InterpolationType.BEZIER) {
                    // 베지어: 위로 솟는 아치 (Height가 Y축 반영)
                    // SceneData.calculateBezier 로직을 2D로 흉내
                    p = calculateBezier2D(t, pStart, pEnd, conn.tension * 50.0f); // 50은 스케일
                    double curX = x + p.x;
                    double curY = (y + height - 5) - p.y; // Y축 뒤집기 (화면좌표)

                    if (i > 0) {
                        // 선 그리기 (점과 점 사이)
                        g.fill((int)prevX, (int)prevY, (int)prevX+1, (int)prevY+1, 0xFF00FF00); // 점선처럼
                        // 또는 Bresenham line 알고리즘을 써야하지만, 간단히 점만 찍어도 됨
                        // 여기선 점을 좀 촘촘히 찍거나, 그냥 점만 표시
                        g.fill((int)curX, (int)curY, (int)curX+2, (int)curY+2, 0xFF00FF00);
                    }
                    prevX = curX;
                    prevY = curY;

                } else if (conn.interpType == SceneData.InterpolationType.SPLINE) {
                    // 스플라인: S자 곡선 등을 보여주기 어려움 (주변 노드가 없어서)
                    // 그래서 그냥 Easing 그래프(속도)를 보여주거나, Tension에 따른 휨을 보여줌
                    // 여기서는 Easing 그래프를 보여주는 게 더 유용할 수 있음

                    // Easing Graph 모드로 전환
                    float val = applyEasingPreview(t, conn.easingType, conn.easingStrength);
                    double curX = x + (t * width);
                    double curY = (y + height - 5) - (val * (height - 10));

                    g.fill((int)curX, (int)curY, (int)curX+2, (int)curY+2, 0xFF00FFFF); // 보라색
                }
            }

            // 라벨
            String type = (conn.interpType == SceneData.InterpolationType.BEZIER) ? "Shape" : "Speed (Easing)";
            g.drawString(Minecraft.getInstance().font, type, x + 2, y + 2, 0xFFAAAAAA, false);
        }

        // 2D 베지어 계산 (미리보기용)
        private Vec3 calculateBezier2D(float t, Vec3 start, Vec3 end, float height) {
            Vec3 mid = start.add(end).scale(0.5);
            Vec3 control = mid.add(0, height, 0);
            double u = 1 - t;
            double tt = t * t;
            double uu = u * u;
            double x = uu * start.x + 2 * u * t * control.x + tt * end.x;
            double y = uu * start.y + 2 * u * t * control.y + tt * end.y;
            return new Vec3(x, y, 0);
        }

        private float applyEasingPreview(float t, SceneData.EasingType type, float strength) {
            switch (type) {
                case EASE_IN: return (float) Math.pow(t, 1.0 + strength);
                case EASE_OUT: return 1.0f - (float) Math.pow(1.0 - t, 1.0 + strength);
                case EASE_IN_OUT: return (t < 0.5f) ? (float) (Math.pow(2 * t, 1.0 + strength) / 2) : (float) (1 - Math.pow(-2 * t + 2, 1.0 + strength) / 2);
                default: return t;
            }
        }

        @Override public boolean mouseClicked(double mx, double my, int btn) { return false; }
        @Override public void setFocused(boolean f) {}
        @Override public boolean isFocused() { return false; }
    }

    private static class SimpleButton implements GuiEventListener, Renderable {
        public int x, y, width, height;
        public String label;
        public Consumer<SimpleButton> onClick;
        public SimpleButton(int x, int y, int w, int h, String l, Consumer<SimpleButton> onClick) {
            this.x = x; this.y = y; this.width = w; this.height = h; this.label = l; this.onClick = onClick;
        }
        @Override public void render(GuiGraphics g, int mx, int my, float pt) {
            boolean h = (mx >= x && mx < x + width && my >= y && my < y + height);
            g.fill(x, y, x + width, y + height, h ? 0xFF444444 : 0xFF333333);
            GuiUtils.drawBorder(g, x, y, width, height, h ? 0xFFFFFFFF : 0xFF888888);
            g.drawCenteredString(Minecraft.getInstance().font, label, x + width / 2, y + 4, 0xFFFFFFFF);
        }
        @Override public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0 && mx >= x && mx < x + width && my >= y && my < y + height) {
                onClick.accept(this);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            } return false;
        }
        @Override public void setFocused(boolean f) {}
        @Override public boolean isFocused() { return false; }
    }

    private static class FloatSlider implements GuiEventListener, Renderable {
        public int x, y, width, height;
        public String label;
        private float value, min, max;
        private boolean dragging = false;
        public FloatSlider(int x, int y, int w, int h, String l, float min, float max, float def) {
            this.x = x; this.y = y; this.width = w; this.height = h; this.label = l; this.min = min; this.max = max; this.value = def;
        }
        public void setValue(float v) { this.value = Mth.clamp(v, min, max); }
        public float getValue() { return value; }
        public boolean isDragging() { return dragging; }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            // 배경
            g.fill(x, y, x + width, y + height, 0xFF222222);
            GuiUtils.drawBorder(g, x, y, width, height, 0xFF666666);

            // 게이지 바 (값에 따라 채워짐)
            float ratio = (value - min) / (max - min);
            int barW = (int)(width * ratio);
            g.fill(x + 1, y + 1, x + 1 + barW, y + height - 1, 0xFF5555AA);

            // 텍스트 표시 (Label: Value)
            String text = String.format("%s: %.2f", label, value);
            g.drawCenteredString(Minecraft.getInstance().font, text, x + width / 2, y + 4, 0xFFFFFFFF);
        }

        // 마우스 위치에 따라 값 업데이트
        private void updateValue(double mx) {
            float ratio = (float)((mx - x) / width);
            this.value = Mth.clamp(min + ratio * (max - min), min, max);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0 && mx >= x && mx < x + width && my >= y && my < y + height) {
                dragging = true;
                updateValue(mx);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            if (dragging) {
                updateValue(mx);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int btn) {
            dragging = false;
            return false;
        }

        @Override public void setFocused(boolean f) {}
        @Override public boolean isFocused() { return dragging; }
    }
}