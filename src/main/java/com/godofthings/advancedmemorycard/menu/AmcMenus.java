package com.godofthings.advancedmemorycard.menu;

import com.godofthings.advancedmemorycard.AdvancedMemoryCardMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 高级内存卡的两个菜单（复制模式 / P2P 配置模式）注册，
 * 替代上游的 {@code client.gui.ModMenu}（菜单类型是双端的，不该放 client 包）。
 */
public final class AmcMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, AdvancedMemoryCardMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CopyModeMenu>> COPY_MODE_MENU =
            MENU_TYPES.register("card_menu", () -> IMenuTypeExtension.create(CopyModeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ConfigModeMenu>> CONFIG_MODE_MENU =
            MENU_TYPES.register("config_menu", () -> IMenuTypeExtension.create(ConfigModeMenu::new));

    private AmcMenus() {
    }

    /** 由 Godofthings 构造器调用，把注册器挂到 mod 事件总线。 */
    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
