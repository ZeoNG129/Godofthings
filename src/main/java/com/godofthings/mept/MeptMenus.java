package com.godofthings.mept;

import com.godofthings.Godofthings;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ME 放置工具的菜单类型注册（照抄上游 ModMenus）。
 */
public final class MeptMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Godofthings.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<WandMenu>> WAND_MENU =
            MENUS.register("mept_wand_menu", () ->
                    IMenuTypeExtension.create((id, inv, buf) -> new WandMenu(id, inv, buf))
            );

    public static final DeferredHolder<MenuType<?>, MenuType<CableToolMenu>> CABLE_TOOL_MENU =
            MENUS.register("mept_cable_tool_menu", () ->
                    IMenuTypeExtension.create((id, inv, buf) -> new CableToolMenu(id, inv, buf))
            );

    private MeptMenus()
    {
    }

    /** 由 Godofthings 构造器调用，把注册器挂到 mod 事件总线。 */
    public static void register(IEventBus modEventBus)
    {
        MENUS.register(modEventBus);
    }
}
