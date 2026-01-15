package me.horang.vantage.data;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SceneData {
    private static final SceneData INSTANCE = new SceneData();
    public static SceneData get() { return INSTANCE; }
    private Vec3 anchorPosition = Vec3.ZERO;
    private Connection selectedConnection = null;

    public static class Node {
        public UUID id = UUID.randomUUID();
        public Vec3 position;
        public float yaw;
        public float pitch;

        public Node(Vec3 pos, float yaw, float pitch) {
            this.position = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static class Connection {
        public Node start;
        public Node end;

        // [New] 속성 추가
        public float duration = 2.0f; // 이동 시간 (초)
        public float tension = 0.0f;  // 0: 직선, 높을수록 곡선 (Bezier 구현 시 사용)

        public Connection(Node s, Node e) { this.start = s; this.end = e; }
    }

    private List<Node> nodes = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();

    // [수정] NONE -> SELECT (기본값)
    public enum ToolMode { SELECT, NODE, LINE }
    private ToolMode currentTool = ToolMode.SELECT;
    private Node selectedNode = null;

    // --- Actions ---

    public void addNode(Vec3 pos, float yaw, float pitch) {
        nodes.add(new Node(pos, yaw, pitch));
    }

    // [New] 노드 삭제 로직 (연결된 선도 같이 삭제)
    public void removeNode(Node node) {
        if (node == null) return;

        // 1. 해당 노드와 연결된 모든 선 제거
        connections.removeIf(c -> c.start == node || c.end == node);

        // 2. 노드 제거
        nodes.remove(node);

        // 3. 선택된 노드였다면 선택 해제
        if (selectedNode == node) {
            selectedNode = null;
        }
    }

    public void connectNodes(Node start, Node end) {
        if (start != end) {
            connections.add(new Connection(start, end));
        }
    }

    // --- Getters & Setters ---
    public List<Node> getNodes() { return nodes; }
    public List<Connection> getConnections() { return connections; }
    public ToolMode getTool() { return currentTool; }
    public void setTool(ToolMode tool) {
        this.currentTool = tool;
        this.selectedNode = null;
    }
    public Node getSelectedNode() { return selectedNode; }

    // --- Anchor Logic ---
    public void setAnchor(Vec3 pos) {
        this.anchorPosition = pos;
    }

    public Vec3 getAnchor() {
        return this.anchorPosition;
    }

    /**
     * 월드 절대 좌표를 앵커 기준 상대 좌표로 변환
     */
    public Vec3 toRelative(Vec3 worldPos) {
        return worldPos.subtract(anchorPosition);
    }

    /**
     * 앵커 기준 상대 좌표를 월드 절대 좌표로 변환
     */
    public Vec3 toWorld(Vec3 relativePos) {
        return anchorPosition.add(relativePos);
    }

    // [New] 선 선택 메서드
    public Connection getSelectedConnection() { return selectedConnection; }

    // [New] 특정 노드에서 시작하는 연결선 찾기 (재생용)
    public Connection getConnectionFrom(Node startNode) {
        for (Connection c : connections) {
            if (c.start == startNode) return c;
        }
        return null; // 연결 끊김
    }

    public void setSelectedNode(Node n) {
        this.selectedNode = n;
        // 노드 선택 시 선 선택 해제
        if (n != null) this.selectedConnection = null;
    }

    public void setSelectedConnection(Connection c) {
        this.selectedConnection = c;
        // [문제 4 해결 보완] 선 선택 시 노드 선택 확실히 해제
        if (c != null) {
            this.selectedNode = null;
        }
    }

    public Connection getConnectionTo(Node endNode) {
        for (Connection c : connections) {
            if (c.end == endNode) return c;
        }
        return null;
    }
    /**
     * [New] 스플라인(곡선) 상의 점 계산 함수
     * @param t 진행률 (0.0 ~ 1.0)
     * @param p0 이전 점 (없으면 p1)
     * @param p1 시작 점
     * @param p2 끝 점
     * @param p3 다음 점 (없으면 p2)
     */
    public static Vec3 getSplinePoint(float t, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        double t2 = t * t;
        double t3 = t2 * t;

        // Catmull-Rom Spline 공식
        // f(t) = 0.5 * ( (2*p1) + (-p0 + p2)*t + (2*p0 - 5*p1 + 4*p2 - p3)*t2 + (-p0 + 3*p1 - 3*p2 + p3)*t3 )

        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);

        return new Vec3(x, y, z);
    }
}