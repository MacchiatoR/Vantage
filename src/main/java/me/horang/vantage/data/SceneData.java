package me.horang.vantage.data;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SceneData {
    private static final SceneData INSTANCE = new SceneData();
    public static SceneData get() { return INSTANCE; }

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

    // 연결 정보 (Line)
    public static class Connection {
        public Node start;
        public Node end;

        public Connection(Node s, Node e) { this.start = s; this.end = e; }
    }

    private List<Node> nodes = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();

    // --- 도구 상태 관리 ---
    public enum ToolMode { NONE, NODE, LINE }
    private ToolMode currentTool = ToolMode.NONE;
    private Node selectedNode = null; // 라인 연결 시 첫 번째 노드 저장용

    public void addNode(Vec3 pos, float yaw, float pitch) {
        nodes.add(new Node(pos, yaw, pitch));
    }

    public void connectNodes(Node start, Node end) {
        if (start != end) {
            connections.add(new Connection(start, end));
        }
    }

    // Getters & Setters
    public List<Node> getNodes() { return nodes; }
    public List<Connection> getConnections() { return connections; }
    public ToolMode getTool() { return currentTool; }
    public void setTool(ToolMode tool) {
        this.currentTool = tool;
        this.selectedNode = null; // 툴 바꿀 때 선택 초기화
    }
    public Node getSelectedNode() { return selectedNode; }
    public void setSelectedNode(Node n) { this.selectedNode = n; }
}
