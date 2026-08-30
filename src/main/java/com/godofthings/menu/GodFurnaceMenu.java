package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodFurnaceBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

public class GodFurnaceMenu extends AbstractContainerMenu
{
    private final GodFurnaceBlockEntity be;
    private final ContainerLevelAccess access;

    // 客户端构造：从 extraData 读取 BlockPos，再查客户端 BE 副本
    public GodFurnaceMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodFurnaceBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // 服务端/本地构造
    public GodFurnaceMenu(int containerId, Inventory playerInv, GodFurnaceBlockEntity be)
    {
        super(Godofthings.GOD_FURNACE_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 6 输入槽（上一行）+ 6 输出槽（下一行）对称布局
        for (int i = 0; i < GodFurnaceBlockEntity.INPUT_SLOT_COUNT; i++)
        {
            this.addSlot(new SlotItemHandler(be.getItemHandler(), i, 8 + i * 18, 17));
        }
        for (int i = 0; i < GodFurnaceBlockEntity.OUTPUT_SLOT_COUNT; i++)
        {
            final int outSlot = GodFurnaceBlockEntity.OUTPUT_SLOT_START + i;
            this.addSlot(new SlotItemHandler(be.getItemHandler(), outSlot, 8 + i * 18, 53)
            {
                // 输出格只能取走，不能手动放入
                @Override
                public boolean mayPlace(ItemStack stack)
                {
                    return false;
                }
            });
        }

        // 玩家物品栏 3x9 + 快捷栏 1x9
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public GodFurnaceBlockEntity getBlockEntity()
    {
        return be;
    }

    // 客户端点击「配置」按钮 → ServerboundContainerButtonClickPacket(containerId, 6)
    // 服务端在此打开独立的配置界面
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId == 6 && player instanceof ServerPlayer serverPlayer)
        {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("gui.godofthings.face_config");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodFurnaceConfigMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_FURNACE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < GodFurnaceBlockEntity.TOTAL_SLOTS)
            {
                // 从方块移到玩家物品栏
                if (!this.moveItemStackTo(stack, GodFurnaceBlockEntity.TOTAL_SLOTS, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 从玩家物品栏移到输入槽（输出槽 mayPlace=false 自动拒绝；不能熔炼的也会被过滤）
                if (!this.moveItemStackTo(stack, 0, GodFurnaceBlockEntity.INPUT_SLOT_COUNT, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty())
            {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
            if (stack.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }
}
