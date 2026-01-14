package me.horang.vantage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;

public class ProjectManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BASE_DIR = "vantage/projects";

    // 데이터 구조체 (DTO)
    public static class CameraSequence {
        public String name;
        public long durationTicks;
        // 여기에 키프레임 리스트 등이 들어감
    }

    public static void saveSequence(String fileName, CameraSequence data) {
        File dir = new File(Minecraft.getInstance().gameDirectory, BASE_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
            // UI에 알림 메시지 띄우기: "Saved to " + file.getPath()
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CameraSequence loadSequence(String fileName) {
        File file = new File(Minecraft.getInstance().gameDirectory, BASE_DIR + "/" + fileName + ".json");
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, CameraSequence.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
