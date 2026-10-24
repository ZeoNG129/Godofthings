package com.godofthings.overloadedinterface.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import appeng.client.gui.style.ScreenStyle;
import appeng.init.client.InitScreens;

import com.godofthings.Godofthings;
import com.godofthings.overloadedinterface.OverloadedInterfaceMenu;

/**
 * 过载接口屏幕注册。
 *
 * <p>屏幕样式沿用 AE2 自带的接口界面 json（{@code /screens/interface.json}），
 * 因为我们的菜单继承 AE2 原生 {@code InterfaceMenu}、槽位布局完全一致，
 * 只多画左侧工具栏的四个模式按钮——不需要额外的背景贴图。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class OverloadedInterfaceScreens
{
    private OverloadedInterfaceScreens()
    {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        InitScreens.register(event, OverloadedInterfaceMenu.TYPE,
                OverloadedInterfaceScreens::create,
                "/screens/godofthings_overloaded_interface.json");
    }

    /** 具名工厂，避免 InitScreens.register 的重载 lambda 泛型推断歧义。 */
    private static OverloadedInterfaceScreen create(OverloadedInterfaceMenu menu,
            Inventory playerInventory, Component title, ScreenStyle style)
    {
        return new OverloadedInterfaceScreen(menu, playerInventory, title, style);
    }
}
