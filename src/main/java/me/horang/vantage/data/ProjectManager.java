package me.horang.vantage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class ProjectManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE_EXT = ".vantage";
    private static final String BASE_DIR = "vantage/projects";

    // 데이터 구조체
    public static class ProjectData {
        public List<NodeData> nodes = new ArrayList<>();
        public List<ConnectionData> connections = new ArrayList<>();
    }

    public static class NodeData {
        public String id;
        public double x, y, z; // 상대 좌표로 저장됨
        public float yaw, pitch, roll;

        // [수정] 생성자에서 기준점(origin)을 받아 상대 좌표 계산
        public NodeData(SceneData.Node node, Vec3 origin) {
            this.id = node.id.toString();
            // 기준점을 뺌 -> (0,0,0) 기준의 로컬 좌표가 됨
            this.x = node.position.x - origin.x;
            this.y = node.position.y - origin.y;
            this.z = node.position.z - origin.z;
            this.yaw = node.yaw;
            this.pitch = node.pitch;
            this.roll = node.roll;
        }
    }

    public static class ConnectionData {
        public String startNodeId;
        public String endNodeId;
        public float duration;
        public String interpType;
        public float tension;
        public float trajectoryAngle;
        public String easingType;
        public float easingStrength;

        public ConnectionData(SceneData.Connection c) {
            this.startNodeId = c.start.id.toString();
            this.endNodeId = c.end.id.toString();
            this.duration = c.duration;
            this.interpType = c.interpType.name();
            this.tension = c.tension;
            this.trajectoryAngle = c.trajectoryAngle;
            this.easingType = c.easingType.name();
            this.easingStrength = c.easingStrength;
        }
    }

    // === File Operations ===

    public static void saveProject(File file, SceneData scene) {
        ProjectData data = new ProjectData();

        List<SceneData.Node> sceneNodes = scene.getNodes();
        if (sceneNodes.isEmpty()) return; // 저장할 게 없음

        // [중요] 첫 번째 노드를 기준점(Origin)으로 설정
        Vec3 origin = sceneNodes.get(0).position;

        // 1. Convert Nodes (상대 좌표로 변환)
        for (SceneData.Node node : sceneNodes) {
            data.nodes.add(new NodeData(node, origin));
        }

        // 2. Convert Connections
        for (SceneData.Connection conn : scene.getConnections()) {
            data.connections.add(new ConnectionData(conn));
        }

        // 3. Write to File
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // [수정] 로드 시 '배치할 기준 위치(loadOrigin)'를 받음
    public static void loadProject(File file, Vec3 loadOrigin) {
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            ProjectData data = GSON.fromJson(reader, ProjectData.class);
            // SceneData에 데이터 주입 시 기준 위치 전달
            SceneData.get().loadFromProjectData(data, loadOrigin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}