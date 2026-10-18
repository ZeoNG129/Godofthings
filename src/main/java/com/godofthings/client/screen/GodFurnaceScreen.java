package com.godofthings.client.screen;

import com.godofthings.godgrid.AbstractGodGridScreen;
import com.godofthings.menu.GodFurnaceMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之熔炉界面：布局、槽框、箭头、三个按钮全部由 {@link AbstractGodGridScreen} 绘制，
 * 本类只负责绑定菜单类型。
 */
public class GodFurnaceScreen extends AbstractGodGridScreen<GodFurnaceMenu>
{
    public GodFurnaceScreen(GodFurnaceMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }
}
