package com.godofthings.advancedmemorycard.client;

import appeng.client.gui.style.StyleManager;

import com.godofthings.advancedmemorycard.client.gui.screen.ConfigModeScreen;
import com.godofthings.advancedmemorycard.client.gui.screen.CopyModeScreen;
import com.godofthings.advancedmemorycard.client.renderer.P2PRenderer;
import com.godofthings.advancedmemorycard.menu.AmcMenus;
import com.godofthings.advancedmemorycard.menu.ConfigModeMenu;
import com.godofthings.advancedmemorycard.menu.CopyModeMenu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 高级内存卡的客户端装配（替代上游的 {@code AdvancedMemoryCardClient} @Mod 类）：
 * 屏幕注册 + P2P 高亮渲染器实例化。
 */
@EventBusSubscriber(modid = com.godofthings.Godofthings.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AmcClientSetup {

    private AmcClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        // 实例化 P2P 渲染器（事件处理依赖单例已创建）
        P2PRenderer.getInstance();

        // 屏幕构造器需要 ScreenStyle，由 StyleManager 从样式 JSON 加载
        // （样式文件位于 assets/ae2/screens/godofthings_amc_*.json，文件名加前缀防与 AE2LT 本体冲突）
        event.<CopyModeMenu, CopyModeScreen>register(AmcMenus.COPY_MODE_MENU.get(),
                (menu, inv, title) -> new CopyModeScreen(menu, inv, title,
                        StyleManager.loadStyleDoc("/screens/godofthings_amc_copy_mode_menu.json")));
        event.<ConfigModeMenu, ConfigModeScreen>register(AmcMenus.CONFIG_MODE_MENU.get(),
                (menu, inv, title) -> new ConfigModeScreen(menu, inv, title,
                        StyleManager.loadStyleDoc("/screens/godofthings_amc_config_mode_menu.json")));
    }
}
