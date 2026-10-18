package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodFurnaceBlockEntity;
import com.godofthings.godgrid.AbstractGodGridMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

/**
 * 神之熔炉的容器：槽位、玩家物品栏、AE/排序/侧面配置三个按钮全部由
 * {@link AbstractGodGridMenu} 统一提供，这里只声明方块与构造方式。
 */
public class GodFurnaceMenu extends AbstractGodGridMenu
{
    public GodFurnaceMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodFurnaceBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodFurnaceMenu(int containerId, Inventory playerInv, GodFurnaceBlockEntity be)
    {
        super(Godofthings.GOD_FURNACE_MENU.get(), containerId, playerInv, be);
    }

    @Override
    protected Block gridBlock()
    {
        return Godofthings.GOD_FURNACE.get();
    }
}
