package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodSpawnEggBlockEntity;
import com.godofthings.godgrid.AbstractGodGridMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

/**
 * 神之怪蛋容器（v5.18.0：迁移到统一网格框架）——
 * 17 个刷怪蛋输入槽 + 17 个复制产物输出槽 + 1 加速槽 + 玩家物品栏，
 * 槽位与三个按钮（面配置 / AE / 排序）全部由 {@link AbstractGodGridMenu} 统一提供。
 */
public class GodSpawnEggMenu extends AbstractGodGridMenu
{
    public GodSpawnEggMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodSpawnEggBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodSpawnEggMenu(int containerId, Inventory playerInv, GodSpawnEggBlockEntity be)
    {
        super(Godofthings.GOD_SPAWN_EGG_MENU.get(), containerId, playerInv, be);
    }

    @Override
    protected Block gridBlock()
    {
        return Godofthings.GOD_SPAWN_EGG.get();
    }
}
