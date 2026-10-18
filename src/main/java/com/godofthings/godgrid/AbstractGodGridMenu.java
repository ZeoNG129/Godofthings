package com.godofthings.godgrid;

import com.godofthings.item.GodAcceleratorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 六台神之机器共用的网格容器：17 输入 + 51 输出 + 1 加速槽 + 玩家物品栏。
 *
 * <p>界面上的三个按钮（侧面配置 / AE / 排序）通过 {@link #clickMenuButton} 走原版按钮包，
 * 不额外开网络包。删除的部分：Mekanism 式升级槽、PB 升级槽、多流体槽、能量条 —— 本框架
 * 一律不提，机器要实现的能力全走 {@link GodGridHost}。</p>
 */
public abstract class AbstractGodGridMenu extends AbstractContainerMenu
{
    /** 服务端按钮 id（沿用熔炉既有的 6 / 10，排序新增 12） */
    public static final int BTN_FACE_CONFIG = 6;
    public static final int BTN_AE_TOGGLE = 10;
    public static final int BTN_SORT = 12;

    /** 加速槽在容器内的槽位序号 */
    public static final int SLOT_ACCEL = GodGridLayout.MACHINE_SLOTS;
    /** 玩家物品栏第一个槽位的序号 */
    public static final int SLOT_PLAYER_START = GodGridLayout.MACHINE_SLOTS + 1;

    protected final BlockEntity blockEntity;
    protected final GodGridHost host;
    private final ContainerLevelAccess access;
    private int cachedAe;
    private int cachedSortMode;

    protected AbstractGodGridMenu(MenuType<?> type, int containerId, Inventory playerInv, BlockEntity be)
    {
        super(type, containerId);
        this.blockEntity = be;
        this.host = (GodGridHost) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.cachedAe = host.gridAeEnabled() ? 1 : 0;
        this.cachedSortMode = host.gridSortMode();

        IItemHandler items = host.gridItems();
        for (int i = 0; i < GodGridLayout.INPUT_COUNT; i++)
        {
            addSlot(new SlotItemHandler(items, i, GodGridLayout.inputX(i), GodGridLayout.INPUT_Y));
        }
        for (int j = 0; j < GodGridLayout.OUTPUT_COUNT; j++)
        {
            final int handlerIndex = GodGridLayout.INPUT_COUNT + j;
            addSlot(new SlotItemHandler(items, handlerIndex,
                    GodGridLayout.outputX(j), GodGridLayout.outputY(j))
            {
                // 输出格只能取走：外部 Shift 点击与手动放置都走 mayPlace
                @Override
                public boolean mayPlace(ItemStack stack)
                {
                    return false;
                }
            });
        }
        addSlot(new SlotItemHandler(host.gridAccel(), 0,
                GodGridLayout.accelX(), GodGridLayout.ACCEL_Y)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return stack.getItem() instanceof GodAcceleratorItem;
            }
        });

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        GodGridLayout.INVENTORY_X + col * GodGridLayout.SLOT,
                        GodGridLayout.INVENTORY_Y + row * GodGridLayout.SLOT));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInv, col,
                    GodGridLayout.INVENTORY_X + col * GodGridLayout.SLOT,
                    GodGridLayout.INVENTORY_Y + GodGridLayout.HOTBAR_OFFSET));
        }

        addDataSlot(new DataSlot()
        {
            @Override
            public int get()
            {
                return host.gridAeEnabled() ? 1 : 0;
            }

            @Override
            public void set(int value)
            {
                cachedAe = value;
            }
        });
        addDataSlot(new DataSlot()
        {
            @Override
            public int get()
            {
                return host.gridSortMode();
            }

            @Override
            public void set(int value)
            {
                cachedSortMode = value;
            }
        });
    }

    public BlockEntity getBlockEntity()
    {
        return blockEntity;
    }

    /** AE 并网开关（客户端渲染用，兼容既有调用方） */
    public boolean isAeEnabled()
    {
        return cachedAe == 1;
    }

    /** 当前排序模式（0=名称 1=模组 2=数量） */
    public int sortMode()
    {
        return cachedSortMode;
    }

    /** 本菜单对应的方块（stillValid 校验用） */
    protected abstract Block gridBlock();

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, gridBlock());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId == BTN_AE_TOGGLE)
        {
            host.gridToggleAe();
            broadcastChanges();
            return true;
        }
        if (buttonId == BTN_SORT)
        {
            cachedSortMode = host.gridSortOutputs();
            broadcastChanges();
            return true;
        }
        if (buttonId == BTN_FACE_CONFIG && player instanceof ServerPlayer serverPlayer)
        {
            host.gridOpenFaceConfig(serverPlayer);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem())
        {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_PLAYER_START)
        {
            if (!moveItemStackTo(stack, SLOT_PLAYER_START, this.slots.size(), true))
            {
                return ItemStack.EMPTY;
            }
        }
        else if (!moveItemStackTo(stack, SLOT_ACCEL, SLOT_ACCEL + 1, false)
                && !moveItemStackTo(stack, 0, GodGridLayout.INPUT_COUNT, false))
        {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
        {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        else
        {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount())
        {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
