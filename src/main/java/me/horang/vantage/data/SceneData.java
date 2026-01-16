package me.horang.vantage.data;

import net.minecraft.world.phys.Vec3;

import java.util.*;
public class SceneData {
    private static final SceneData INSTANCE = new SceneData();
    public static SceneData get() { return INSTANCE; }

    // --- State ---
    private final List<Node> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    // 다중 선택 관리
    private final Set<Node> selectedNodes = new LinkedHashSet<>();
    private Node activeNode = null; // 기즈모의 기준이 되는, 가장 최근 선택된 노드
    private Connection selectedConnection = null;

    // 툴 모드
    public enum ToolMode { SELECT, NODE, LINE }
    private ToolMode currentTool = ToolMode.SELECT;
    private Vec3 anchorPosition = Vec3.ZERO;
    // --- Enums ---

    public enum InterpolationType {
        LINEAR("Linear"),
        SPLINE("Smooth"),
        BEZIER("Arc");

        public final String label;
        InterpolationType(String label) { this.label = label; }
        public InterpolationType next() { return values()[(this.ordinal() + 1) % values().length]; }
    }

    public enum EasingType {
        LINEAR("Linear"),
        EASE_IN("Ease In"),
        EASE_OUT("Ease Out"),
        EASE_IN_OUT("Ease I/O");

        public final String label;
        EasingType(String label) { this.label = label; }
        public EasingType next() { return values()[(this.ordinal() + 1) % values().length]; }
    }

    // --- Inner Classes ---

    public static class Node {
        public UUID id = UUID.randomUUID();
        public Vec3 position;
        public float yaw, pitch, roll;

        public Node(Vec3 pos, float yaw, float pitch, float roll) {
            this.position = pos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }
    }

    public static class Connection {
        public Node start;
        public Node end;
        public float duration = 2.0f;

        public InterpolationType interpType = InterpolationType.SPLINE;
        public float tension = 0.5f; // Spline: Tension, Bezier: Height
        public float trajectoryAngle = 0.0f; // Bezier: Rotation Angle (0~360)

        public EasingType easingType = EasingType.LINEAR;
        public float easingStrength = 1.0f;

        public Connection(Node s, Node e) { this.start = s; this.end = e; }
    }

    // --- Selection Management (Multi-Select) ---

    public void selectNode(Node node, boolean multiSelect) {
        if (node == null) return;
        if (!multiSelect) {
            selectedNodes.clear();
        }
        selectedNodes.add(node);
        activeNode = node;
        selectedConnection = null;
    }

    public void toggleNodeSelection(Node node) {
        if (selectedNodes.contains(node)) {
            selectedNodes.remove(node);
            // 활성 노드가 해제되면 남은 것 중 하나를 활성으로 설정
            if (activeNode == node) {
                activeNode = selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
            }
        } else {
            selectedNodes.add(node);
            activeNode = node;
        }
        selectedConnection = null;
    }

    public void selectRange(Node targetNode) {
        if (activeNode == null || !nodes.contains(activeNode) || !nodes.contains(targetNode)) {
            selectNode(targetNode, false);
            return;
        }

        int startIdx = nodes.indexOf(activeNode);
        int endIdx = nodes.indexOf(targetNode);
        int min = Math.min(startIdx, endIdx);
        int max = Math.max(startIdx, endIdx);

        selectedNodes.clear();
        for (int i = min; i <= max; i++) {
            selectedNodes.add(nodes.get(i));
        }
        activeNode = targetNode; // 끝점을 활성 노드로
        selectedConnection = null;
    }

    public void clearSelection() {
        selectedNodes.clear();
        activeNode = null;
        selectedConnection = null;
    }

    public boolean isSelected(Node node) { return selectedNodes.contains(node); }
    public Set<Node> getSelectedNodes() { return selectedNodes; }

    // 단일 반환 메서드 (Inspector용) - 활성 노드 반환
    public Node getSelectedNode() { return activeNode; }
    // 편의상 활성 노드 설정 (단일 선택으로 간주)
    public void setSelectedNode(Node n) { selectNode(n, false); }

    public Connection getSelectedConnection() { return selectedConnection; }
    public void setSelectedConnection(Connection c) {
        this.selectedConnection = c;
        if (c != null) {
            selectedNodes.clear();
            activeNode = null;
        }
    }

    // --- Graph & Data Operations ---

    public void addNode(Vec3 pos, float yaw, float pitch) {
        Node newNode = new Node(pos, yaw, pitch, 0f);
        nodes.add(newNode);
        selectNode(newNode, false); // 추가 시 자동 선택
    }

    public void removeNode(Node node) {
        if (node == null) return;

        // 연결된 커넥션 삭제
        connections.removeIf(c -> c.start == node || c.end == node);

        // 리스트에서 삭제
        nodes.remove(node);

        // 선택셋에서 삭제
        selectedNodes.remove(node);
        if (activeNode == node) activeNode = null;
    }

    public void connectNodes(Node start, Node end) {
        if (start != end && getConnectionFrom(start) == null) { // 간단하게 start에서 하나만 나가도록 제한 (필요시 수정)
            connections.add(new Connection(start, end));
        }
    }

    public List<Node> getNodes() { return nodes; }
    public List<Connection> getConnections() { return connections; }

    public ToolMode getTool() { return currentTool; }
    public void setTool(ToolMode tool) {
        this.currentTool = tool;
        clearSelection();
    }

    public Connection getConnectionTo(Node endNode) {
        for (Connection c : connections) if (c.end == endNode) return c;
        return null;
    }
    public Connection getConnectionFrom(Node startNode) {
        for (Connection c : connections) if (c.start == startNode) return c;
        return null;
    }

    // --- Math & Interpolation ---

    public static Vec3 getPointOnConnection(Connection conn, float t, SceneData data) {
        float easedT = applyEasing(t, conn.easingType, conn.easingStrength);

        Node p1 = conn.start;
        Node p2 = conn.end;

        if (conn.interpType == InterpolationType.LINEAR) {
            return p1.position.lerp(p2.position, easedT);
        }
        else if (conn.interpType == InterpolationType.BEZIER) {
            // 아치형 베지어 (회전 포함)
            return calculateBezier(easedT, p1.position, p2.position, conn.tension, conn.trajectoryAngle);
        }
        else {
            // 캣멀롬 스플라인
            Connection prevConn = data.getConnectionTo(p1);
            Vec3 p0 = (prevConn != null) ? prevConn.start.position : p1.position; // 이전 점이 없으면 현재 점 반복

            Connection nextConn = data.getConnectionFrom(p2);
            Vec3 p3 = (nextConn != null) ? nextConn.end.position : p2.position; // 다음 점이 없으면 현재 점 반복

            return calculateSpline(easedT, p0, p1.position, p2.position, p3, conn.tension);
        }
    }

    // 3D 회전이 적용된 2차 베지어
    private static Vec3 calculateBezier(float t, Vec3 start, Vec3 end, float height, float angleDeg) {
        // 1. 기본 정보 계산
        Vec3 mid = start.add(end).scale(0.5);
        Vec3 dir = end.subtract(start).normalize(); // 축(Axis)
        double dist = start.distanceTo(end);

        // 2. 기준 수직 벡터(Up) 구하기
        // 선이 수직(Y축)에 가까우면 X축을 기준으로, 아니면 Y축을 기준으로 임시 수직 벡터 생성
        Vec3 refUp = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(refUp).normalize(); // 축에 수직인 벡터 1
        Vec3 trueUp = right.cross(dir).normalize(); // 축에 수직인 벡터 2 (기본 솟는 방향)

        // 3. 각도에 따른 회전 (Rodrigues' rotation formula 단순화)
        double rad = Math.toRadians(angleDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        // RotatedVector = V * cos + (Axis x V) * sin
        // trueUp(V), dir(Axis)
        Vec3 rotatedUp = trueUp.scale(cos).add(dir.cross(trueUp).scale(sin));

        // 4. 제어점(Control Point) 결정
        Vec3 control = mid.add(rotatedUp.scale(dist * height));

        // 5. 2차 베지어 공식
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;

        double x = uu * start.x + 2 * u * t * control.x + tt * end.x;
        double y = uu * start.y + 2 * u * t * control.y + tt * end.y;
        double z = uu * start.z + 2 * u * t * control.z + tt * end.z;

        return new Vec3(x, y, z);
    }

    // Catmull-Rom Spline
    private static Vec3 calculateSpline(float t, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float tension) {
        double t2 = t * t;
        double t3 = t2 * t;

        // tension 조정 (0.5 기본값 기준 스케일링)
        double s = tension * 1.5;

        double h1 = 2 * t3 - 3 * t2 + 1;
        double h2 = -2 * t3 + 3 * t2;
        double h3 = t3 - 2 * t2 + t;
        double h4 = t3 - t2;

        Vec3 m1 = p2.subtract(p0).scale(s);
        Vec3 m2 = p3.subtract(p1).scale(s);

        return new Vec3(
                h1 * p1.x + h2 * p2.x + h3 * m1.x + h4 * m2.x,
                h1 * p1.y + h2 * p2.y + h3 * m1.y + h4 * m2.y,
                h1 * p1.z + h2 * p2.z + h3 * m1.z + h4 * m2.z
        );
    }

    private static float applyEasing(float t, EasingType type, float strength) {
        switch (type) {
            case EASE_IN: return (float) Math.pow(t, 1.0 + strength);
            case EASE_OUT: return 1.0f - (float) Math.pow(1.0 - t, 1.0 + strength);
            case EASE_IN_OUT:
                if (t < 0.5f) return (float) (Math.pow(2 * t, 1.0 + strength) / 2);
                else return (float) (1 - Math.pow(-2 * t + 2, 1.0 + strength) / 2);
            case LINEAR: default: return t;
        }
    }

    // --- Save/Load Logic (ProjectManager 연동) ---

    /**
     * ProjectManager에서 호출.
     * 저장된 DTO 데이터를 받아 실제 인스턴스로 변환하며,
     * loadOrigin(플레이어 위치)을 더해 상대 좌표를 월드 좌표로 배치합니다.
     */
    public void loadFromProjectData(ProjectManager.ProjectData data, Vec3 loadOrigin) {
        clearSelection();
        this.nodes.clear();
        this.connections.clear();

        Map<UUID, Node> idMap = new HashMap<>();

        // 1. 노드 복구 (상대 좌표 + 기준 위치 = 월드 좌표)
        for (ProjectManager.NodeData nd : data.nodes) {
            double worldX = nd.x + loadOrigin.x;
            double worldY = nd.y + loadOrigin.y;
            double worldZ = nd.z + loadOrigin.z;

            Node node = new Node(new Vec3(worldX, worldY, worldZ), nd.yaw, nd.pitch, nd.roll);
            try { node.id = UUID.fromString(nd.id); } catch(Exception e) {} // ID 유지

            this.nodes.add(node);
            idMap.put(node.id, node);
        }

        // 2. 연결 복구
        for (ProjectManager.ConnectionData cd : data.connections) {
            Node start = idMap.get(UUID.fromString(cd.startNodeId));
            Node end = idMap.get(UUID.fromString(cd.endNodeId));

            if (start != null && end != null) {
                Connection conn = new Connection(start, end);
                conn.duration = cd.duration;
                try { conn.interpType = InterpolationType.valueOf(cd.interpType); } catch(Exception e){}
                conn.tension = cd.tension;
                conn.trajectoryAngle = cd.trajectoryAngle;
                try { conn.easingType = EasingType.valueOf(cd.easingType); } catch(Exception e){}
                conn.easingStrength = cd.easingStrength;
                this.connections.add(conn);
            }
        }
    }

    public Vec3 getAnchor() {
        return anchorPosition;
    }

    public void setAnchor(Vec3 pos) {
        this.anchorPosition = pos;
    }

    /**
     * 월드 좌표를 앵커 기준 상대 좌표로 변환합니다.
     * (InspectorPanel 등에서 좌표를 0,0,0 기준으로 표시할 때 사용)
     */
    public Vec3 toRelative(Vec3 worldPos) {
        return worldPos.subtract(anchorPosition);
    }

    /**
     * 상대 좌표를 월드 좌표로 변환합니다.
     * (InspectorPanel에서 입력한 값을 실제 월드 위치로 적용할 때 사용)
     */
    public Vec3 toWorld(Vec3 relativePos) {
        return anchorPosition.add(relativePos);
    }
}