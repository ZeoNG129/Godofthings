package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodDropBlockEntity;
import com.godofthings.godgrid.AbstractGodGridMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

/**
 * 神之掉落容器（v5.18.0：迁移到统一网格框架）——
 * 17 个刷怪蛋输入槽 + 17 个产物输出槽 + 1 加速槽 + 玩家物品栏，
 * 槽位与三个按钮（面配置 / AE / 排序）全部由 {@link AbstractGodGridMenu} 统一提供。
 */
public class GodDropMenu extends AbstractGodGridMenu
{
    public GodDropMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodDropBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodDropMenu(int containerId, Inventory playerInv, GodDropBlockEntity be)
    {
        super(Godofthings.GOD_DROP_MENU.get(), containerId, playerInv, be);
    }

    @Override
    protected Block gridBlock()
    {
        return Godofthings.GOD_DROP.get();
    }
}
