package com.godofthings.client.screen;

import com.godofthings.godgrid.AbstractGodGridScreen;
import com.godofthings.menu.GodPeelerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之去皮界面（v5.18.0：迁移到统一网格框架）——
 * 17 输入 → 17 输出的网格布局、箭头、加速槽与三个按钮全部由 {@link AbstractGodGridScreen} 绘制。
 */
public class GodPeelerScreen extends AbstractGodGridScreen<GodPeelerMenu>
{
    public GodPeelerScreen(GodPeelerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }
}
