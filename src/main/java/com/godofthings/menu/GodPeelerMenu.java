package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodPeelerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 神之去皮界面（v5.15.1：与神之熔炉相同布局）——
 * 9 输入槽（上一行）+ 9 输出槽（下一行，一一对应）+ 右侧神之加速槽 + 玩家物品栏。
 * 机器逻辑无需客户端同步值，纯 vanilla 菜单同步。
 */
public class GodPeelerMenu extends AbstractContainerMenu
{
    private final GodPeelerBlockEntity be;
    private final ContainerLevelAccess access;
    private int cachedAeEnabled = 1;

    public GodPeelerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodPeelerBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodPeelerMenu(int containerId, Inventory playerInv, GodPeelerBlockEntity be)
    {
        super(Godofthings.GOD_PEELER_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 9 输入槽（上一行）+ 9 输出槽（下一行）一一对应（坐标同神之熔炉）
        for (int i = 0; i < GodPeelerBlockEntity.INPUT_SLOT_COUNT; i++)
        {
            this.addSlot(new SlotItemHandler(be.getItemHandler(), i, 8 + i * 18, 17));
        }
        for (int i = 0; i < GodPeelerBlockEntity.OUTPUT_SLOT_COUNT; i++)
        {
            final int outSlot = GodPeelerBlockEntity.OUTPUT_SLOT_START + i;
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

        // 神之加速槽（只接受神之加速，最多 64 个），右侧（坐标同神之熔炉）
        this.addSlot(new SlotItemHandler(be.getAccelSlot(), 0, 178, 35));

        // AE 接入开关状态（客户端同步；v5.15.9 加入并网）
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.isAeEnabled() ? 1 : 0; }

            @Override
            public void set(int value) { cachedAeEnabled = value; }
        });

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

    public GodPeelerBlockEntity getBlockEntity()
    {
        return be;
    }

    /** AE 接入开关状态（客户端渲染用）。 */
    public boolean isAeEnabled()
    {
        return cachedAeEnabled == 1;
    }

    // 客户端点击右上角齿轮 → ServerboundContainerButtonClickPacket(containerId, 6)
    // 服务端在此打开面配置界面（与神之熔炉同一套）
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId == 10)
        {
            // AE 接入开关（界面右侧竖排按钮，与神之熔炉同一画法与按钮位）
            be.toggleAeEnabled();
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 6 && player instanceof ServerPlayer serverPlayer)
        {
            // 1.21.1：NetworkHooks.openScreen → IPlayerExtension.openMenu(provider, Consumer<RegistryFriendlyByteBuf>)
            serverPlayer.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("container.godofthings.god_peeler");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodPeelerConfigMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_PEELER.get());
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
            if (index < GodPeelerBlockEntity.TOTAL_SLOTS + 1)
            {
                // 从机器（9 输入 + 9 输出 + 1 加速槽）移到玩家物品栏
                if (!this.moveItemStackTo(stack, GodPeelerBlockEntity.TOTAL_SLOTS + 1, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 从玩家物品栏：先试加速槽（只收神之加速），再试输入槽（输出槽 mayPlace=false 自动拒绝）
                if (!this.moveItemStackTo(stack, GodPeelerBlockEntity.TOTAL_SLOTS, GodPeelerBlockEntity.TOTAL_SLOTS + 1, false)
                        && !this.moveItemStackTo(stack, 0, GodPeelerBlockEntity.INPUT_SLOT_COUNT, false))
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
