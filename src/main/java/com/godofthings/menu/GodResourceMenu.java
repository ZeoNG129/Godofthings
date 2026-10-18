package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodResourceBlockEntity;
import com.godofthings.block.entity.GodResourceVariant;
import com.godofthings.godgrid.AbstractGodGridMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

/**
 * 神之资源三机（矿物 / 作物 / 复制）共用的容器：网格槽位（17 输入 + 17 输出 + 加速槽）、
 * 玩家物品栏、AE / 排序 / 面配置按钮全部由 {@link AbstractGodGridMenu} 统一提供。
 *
 * <p>三台机器共用同一个方块实体与同一个菜单类型，唯一的区别是
 * {@link GodResourceBlockEntity#getVariant()}（决定 {@link #gridBlock()} 与产出表）；
 * 界面标题也按变体取自各方块注册名，因此三者仍然可区分。</p>
 */
public class GodResourceMenu extends AbstractGodGridMenu
{
    public GodResourceMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodResourceBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodResourceMenu(int containerId, Inventory playerInv, GodResourceBlockEntity be)
    {
        super(Godofthings.GOD_RESOURCE_MENU.get(), containerId, playerInv, be);
    }

    /** 按方块实体的变体返回对应方块：矿物 / 作物 / 复制各认自己那一台（stillValid 校验用）。 */
    @Override
    protected Block gridBlock()
    {
        GodResourceVariant variant = ((GodResourceBlockEntity) blockEntity).getVariant();
        return switch (variant)
        {
            case ORE -> Godofthings.GOD_ORE_MACHINE.get();
            case CROP -> Godofthings.GOD_CROP_MACHINE.get();
            case DUPLICATE -> Godofthings.GOD_DUPLICATE_MACHINE.get();
        };
    }
}
