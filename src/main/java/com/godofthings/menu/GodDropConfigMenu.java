package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodDropBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 神之掉落机的面配置界面菜单（照抄神之熔炉面配置）：无槽位，只有 6 个面模式 DataSlot。
 * 按钮 0-5 = 循环切换对应面模式；按钮 6 = 返回主掉落机界面。
 */
public class GodDropConfigMenu extends AbstractContainerMenu
{
    private final GodDropBlockEntity be;
    private final ContainerLevelAccess access;

    public GodDropConfigMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodDropBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodDropConfigMenu(int containerId, Inventory playerInv, GodDropBlockEntity be)
    {
        super(Godofthings.GOD_DROP_CONFIG_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 6 个面模式 DataSlot，方向顺序 = Direction.values() = DOWN,UP,NORTH,SOUTH,WEST,EAST
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            this.addDataSlot(new DataSlot()
            {
                @Override
                public int get()
                {
                    return be.getFaceMode(Direction.values()[idx]);
                }

                @Override
                public void set(int value)
                {
                    be.setFaceMode(Direction.values()[idx], value);
                }
            });
        }
    }

    public GodDropBlockEntity getBlockEntity()
    {
        return be;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId >= 0 && buttonId < 6)
        {
            be.cycleFaceMode(Direction.values()[buttonId]);
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 6 && player instanceof ServerPlayer serverPlayer)
        {
            // 返回主掉落机界面
            serverPlayer.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return be.getDisplayName();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodDropMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_DROP.get());
    }

    // 无槽位，快速移动无操作
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
