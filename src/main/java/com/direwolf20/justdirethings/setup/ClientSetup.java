package com.direwolf20.justdirethings.setup;

import com.direwolf20.justdirethings.JustDireThings;
import com.direwolf20.justdirethings.client.events.RenderLevelLast;
import com.direwolf20.justdirethings.client.screens.BlockBreakerT1Screen;
import com.direwolf20.justdirethings.client.screens.BlockBreakerT2Screen;
import com.direwolf20.justdirethings.client.screens.BlockPlacerT1Screen;
import com.direwolf20.justdirethings.client.screens.BlockPlacerT2Screen;
import com.direwolf20.justdirethings.client.screens.BlockSwapperT1Screen;
import com.direwolf20.justdirethings.client.screens.BlockSwapperT2Screen;
import com.direwolf20.justdirethings.client.screens.ClickerT1Screen;
import com.direwolf20.justdirethings.client.screens.ClickerT2Screen;
import com.direwolf20.justdirethings.client.screens.ItemCollectorScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 精简客户端注册：仅注册 5 个机器（含 T1/T2）的界面。
 */
@Mod.EventBusSubscriber(modid = JustDireThings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(Registration.Item_Collector_Container.get(), ItemCollectorScreen::new);
            MenuScreens.register(Registration.BlockBreakerT1_Container.get(), BlockBreakerT1Screen::new);
            MenuScreens.register(Registration.BlockBreakerT2_Container.get(), BlockBreakerT2Screen::new);
            MenuScreens.register(Registration.BlockPlacerT1_Container.get(), BlockPlacerT1Screen::new);
            MenuScreens.register(Registration.BlockPlacerT2_Container.get(), BlockPlacerT2Screen::new);
            MenuScreens.register(Registration.ClickerT1_Container.get(), ClickerT1Screen::new);
            MenuScreens.register(Registration.ClickerT2_Container.get(), ClickerT2Screen::new);
            MenuScreens.register(Registration.BlockSwapperT1_Container.get(), BlockSwapperT1Screen::new);
            MenuScreens.register(Registration.BlockSwapperT2_Container.get(), BlockSwapperT2Screen::new);
        });
        // 注册区域预览渲染（“显示区域”功能）
        MinecraftForge.EVENT_BUS.register(RenderLevelLast.class);
    }
}
