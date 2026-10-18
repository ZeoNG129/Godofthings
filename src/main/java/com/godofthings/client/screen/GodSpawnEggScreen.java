package com.godofthings.client.screen;

import com.godofthings.godgrid.AbstractGodGridScreen;
import com.godofthings.menu.GodSpawnEggMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之怪蛋界面（v5.18.0：迁移到统一网格框架）——
 * 17 输入（刷怪蛋）→ 17 输出（复制产物）的网格布局、箭头、加速槽与三个按钮全部由
 * {@link AbstractGodGridScreen} 绘制。
 */
public class GodSpawnEggScreen extends AbstractGodGridScreen<GodSpawnEggMenu>
{
    public GodSpawnEggScreen(GodSpawnEggMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }
}
