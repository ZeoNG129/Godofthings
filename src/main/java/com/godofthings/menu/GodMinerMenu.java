package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodMinerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GodMinerMenu extends AbstractContainerMenu
{
    private final GodMinerBlockEntity be;
    private final ContainerLevelAccess access;

    public GodMinerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodMinerBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodMinerMenu(int containerId, Inventory playerInv, GodMinerBlockEntity be)
    {
        super(Godofthings.GOD_MINER_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 无储存槽：挖掘产物直接进入内置无限储存，仅显示玩家物品栏
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 210));
        }

        // 运行 / 半径 / 当前深度 / 效率 / 液体量 / 内置储存堆数
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.isRunning() ? 1 : 0; }
            @Override
            public void set(int value) { be.setRunning(value != 0); }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getRadius(); }
            @Override
            public void set(int value) { be.setRadius(value); }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getCurrentY(); }
            @Override
            public void set(int value) { be.setCurrentY(value); }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getEfficiencyLevel(); }
            @Override
            public void set(int value) { /* 只读 */ }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getFluidAmount(); }
            @Override
            public void set(int value) { /* 只读 */ }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getStorageCount(); }
            @Override
            public void set(int value) { /* 只读 */ }
        });
    }

    public GodMinerBlockEntity getBlockEntity()
    {
        return be;
    }

    // 按钮：0=开始/停止；1-6=半径调整（-100/-10/-1/+1/+10/+100）
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        // 服务端守卫：玩家必须在方块 8 格内且同维度，防止异常客户端/跨维度操控
        if (be.getLevel() == null || be.getLevel().dimension() != player.level().dimension()
                || player.distanceToSqr(be.getBlockPos().getX() + 0.5,
                be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5) > 8.0 * 8.0)
        {
            return false;
        }
        if (buttonId == 0)
        {
            // 服务端权威翻转运行状态；客户端乐观更新会由 broadcastChanges 的 DataSlot 同步收敛
            be.setRunning(!be.isRunning());
            this.broadcastChanges();
            return true;
        }
        if (buttonId >= 1 && buttonId <= 6)
        {
            int delta = switch (buttonId)
            {
                case 1 -> -100;
                case 2 -> -10;
                case 3 -> -1;
                case 4 -> 1;
                case 5 -> 10;
                default -> 100;
            };
            be.setRadius(be.getRadius() + delta); // setRadius 内部已 clamp 到 1..1600
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_MINER.get());
    }

    // 无储存槽，快速移动无操作
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
