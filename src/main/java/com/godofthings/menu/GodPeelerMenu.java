package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodPeelerBlockEntity;
import com.godofthings.godgrid.AbstractGodGridMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

/**
 * 神之去皮容器（v5.18.0：迁移到统一网格框架）——
 * 17 输入 + 17 输出（一一对应：输入 i → 输出 {@code OUTPUT_SLOT_START + i}）+ 1 加速槽 + 玩家物品栏，
 * 槽位与三个按钮（面配置 / AE / 排序）全部由 {@link AbstractGodGridMenu} 统一提供。
 */
public class GodPeelerMenu extends AbstractGodGridMenu
{
    public GodPeelerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodPeelerBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodPeelerMenu(int containerId, Inventory playerInv, GodPeelerBlockEntity be)
    {
        super(Godofthings.GOD_PEELER_MENU.get(), containerId, playerInv, be);
    }

    @Override
    protected Block gridBlock()
    {
        return Godofthings.GOD_PEELER.get();
    }
}
