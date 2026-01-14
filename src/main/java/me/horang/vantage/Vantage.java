package me.horang.vantage;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Vantage.MODID)
public class Vantage {
    public static final String MODID = "vantage";
    // 로거는 나중에 디버깅할 때 아주 유용해
    public static final Logger LOGGER = LogUtils.getLogger();

    public Vantage(IEventBus modEventBus, ModContainer modContainer) {
        // [중요] 여기서 'NeoForge.EVENT_BUS.register(this)'를 하면 안 됨!
        // 이유: 이 클래스 안에 @SubscribeEvent 메서드가 없으면 에러가 남.

        // 대신, 라이프사이클 이벤트를 '메서드 참조' 방식으로 등록하는 것이 네오포지의 정석임.
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 공통(서버+클라) 초기화 로직이 필요하면 여기에 작성
        LOGGER.info("Vantage Common Setup");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // 클라이언트 전용 초기화 로직
        // 예: KeyBinding이 제대로 등록됐는지 로그 찍어보기
        LOGGER.info("Vantage Client Setup");
    }
}