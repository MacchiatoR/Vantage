package me.horang.vantage.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "vantage")
public class KeyBindings {

    public static final KeyMapping OPEN_EDITOR_KEY = new KeyMapping(
            "key.vantage.editor",          // Lang file key
            InputConstants.Type.KEYSYM,    // Input type
            GLFW.GLFW_KEY_RIGHT_SHIFT,     // Default Key
            "category.vantage"             // Category
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR_KEY);
    }
}