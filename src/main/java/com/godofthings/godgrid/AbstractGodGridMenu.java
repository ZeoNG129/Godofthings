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

    /** 网格槽是否是自管 long 槽（六台神之机器均为 true；true 才会走数量同步与差额结算的点击逻辑） */
    private final boolean longSlots;
    /** 自管 long 槽的数量缓存（客户端渲染大数字用；-1 = 不显示），由低 16 / 高 15 两个 DataSlot 重组 */
    private final long[] cachedAmounts = filled(GodGridLayout.MACHINE_SLOTS, -1L);
    /** 双通道数量同步的两半（低 16 位 / 高 15 位），每次 set 后重组进 cachedAmounts */
    private final int[] cachedAmountLows = new int[GodGridLayout.MACHINE_SLOTS];
    private final int[] cachedAmountHighs = new int[GodGridLayout.MACHINE_SLOTS];

    protected AbstractGodGridMenu(MenuType<?> type, int containerId, Inventory playerInv, BlockEntity be)
    {
        super(type, containerId);
        this.blockEntity = be;
        this.host = (GodGridHost) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.cachedAe = host.gridAeEnabled() ? 1 : 0;
        this.cachedSortMode = host.gridSortMode();

        IItemHandler items = host.gridItems();
        // 自管 long 槽换用专用槽位包装：对外视图 ≤99，点击按「视图差额」结算，不丢量
        LongSlotHandler longItems = items instanceof LongSlotHandler handler ? handler : null;
        this.longSlots = longItems != null;
        for (int i = 0; i < GodGridLayout.INPUT_COUNT; i++)
        {
            if (longItems != null)
            {
                addSlot(new LongSlotHandler.LongSlot(longItems, i,
                        GodGridLayout.inputX(i), GodGridLayout.INPUT_Y));
            }
            else
            {
                addSlot(new SlotItemHandler(items, i, GodGridLayout.inputX(i), GodGridLayout.INPUT_Y));
            }
        }
        for (int j = 0; j < GodGridLayout.OUTPUT_COUNT; j++)
        {
            final int handlerIndex = GodGridLayout.INPUT_COUNT + j;
            final int slotX = GodGridLayout.outputX(j);
            final int slotY = GodGridLayout.outputY(j);
            Slot outputSlot;
            if (longItems != null)
            {
                outputSlot = new LongSlotHandler.LongSlot(longItems, handlerIndex, slotX, slotY)
                {
                    // 输出格只能取走：外部 Shift 点击与手动放置都走 mayPlace
                    @Override
                    public boolean mayPlace(ItemStack stack)
                    {
                        return false;
                    }
                };
            }
            else
            {
                outputSlot = new SlotItemHandler(items, handlerIndex, slotX, slotY)
                {
                    // 输出格只能取走：外部 Shift 点击与手动放置都走 mayPlace
                    @Override
                    public boolean mayPlace(ItemStack stack)
                    {
                        return false;
                    }
                };
            }
            addSlot(outputSlot);
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

        // 自管 long 槽的数量同步：每个机器槽两个 DataSlot（低 16 位 / 高 15 位），客户端按
        // (high << 16) | low 重组为 long，int 上限 2,147,483,647（31 位）正好无损覆盖。
        // 只有声明了 gridHasLongAmounts() 的机器才开（六台神之机器均为 true），其它机器零变化。
        // DataSlot 的值在 ClientboundContainerSetDataPacket 里按 signed short 传输：低 16 位在
        // 客户端可能读成负数，重组时用 & 0xFFFF 还原；高 15 位恒 ≤ 0x7FFF 不受影响。
        // 下发时机：原版 ServerPlayer 每 tick 调 broadcastChanges()，轮询每个 DataSlot.get() 与上次
        // 值比较，变了才发包 —— 方块实体改数量（onContentsChanged → setChanged）后下一 tick 自动同步。
        if (host.gridHasLongAmounts())
        {
            for (int i = 0; i < GodGridLayout.MACHINE_SLOTS; i++)
            {
                final int machineSlot = i;
                // 先 low 后 high：两半都到齐后重组值即为服务端真实数量
                addDataSlot(new DataSlot()
                {
                    @Override
                    public int get()
                    {
                        return (int) (host.gridAmount(machineSlot) & 0xFFFFL);
                    }

                    @Override
                    public void set(int value)
                    {
                        cachedAmountLows[machineSlot] = value & 0xFFFF;
                        cachedAmounts[machineSlot] =
                                combineAmount(cachedAmountLows[machineSlot], cachedAmountHighs[machineSlot]);
                    }
                });
                addDataSlot(new DataSlot()
                {
                    @Override
                    public int get()
                    {
                        return (int) ((host.gridAmount(machineSlot) >>> 16) & 0x7FFFL);
                    }

                    @Override
                    public void set(int value)
                    {
                        cachedAmountHighs[machineSlot] = value & 0x7FFF;
                        cachedAmounts[machineSlot] =
                                combineAmount(cachedAmountLows[machineSlot], cachedAmountHighs[machineSlot]);
                    }
                });
            }
        }
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

    /** 某个机器槽的真实存量（客户端已同步的精确值；-1 = 不显示） */
    public long gridAmount(int machineSlot)
    {
        return machineSlot >= 0 && machineSlot < cachedAmounts.length ? cachedAmounts[machineSlot] : -1L;
    }

    private static long[] filled(int size, long value)
    {
        long[] array = new long[Math.max(0, size)];
        java.util.Arrays.fill(array, value);
        return array;
    }

    /**
     * 低 16 位 + 高 15 位重组为 long。注册顺序先 low 后 high，两半都到齐后即为服务端真实数量；
     * 变化跨两半时中间态最多显示一 tick（下一半到达即修正）。
     */
    private static long combineAmount(int low, int high)
    {
        return ((long) high << 16) | (low & 0xFFFFL);
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
            // 只有真的有面配置界面的机器才处理这个 id（六台网格机器现已全部支持；
            // 以后新增的机器若没实现面配置，返回 false，界面也已隐藏页签，不会留「点了没反应」的死按钮）
            if (!host.gridSupportsFaceConfig())
            {
                return false;
            }
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
        // 自管 long 槽：getItem() 只是「视图」（≤99 的副本），直接交给 moveItemStackTo 会把
        // 已搬走的那部分复制出来 → 先真正取出再搬运，搬不动的原样塞回槽里
        if (index < SLOT_PLAYER_START && slot instanceof LongSlotHandler.LongSlot longSlot)
        {
            return quickMoveOutOfLongSlot(longSlot);
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
                && !moveIntoMachineSlots(stack))
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

    /**
     * 玩家物品栏 → 机器输入槽。
     *
     * <p>vanilla 的 {@link #moveItemStackTo} 合堆时依赖「{@code Slot#getItem()} 返回活堆」，
     * 而自管 long 槽返回的是 ≤99 的副本：vanilla 在副本上 grow 后调 {@code setChanged()} 对我们无效，
     * 结果是「玩家手里的被扣掉、槽里没增加」。所以自管 long 槽一律走 {@code insertItem}（累加语义）。</p>
     */
    private boolean moveIntoMachineSlots(ItemStack stack)
    {
        if (!this.longSlots)
        {
            // 其它五台机器保持原来的整段 moveItemStackTo（合堆优先、再找空槽），行为零变化
            return moveItemStackTo(stack, 0, GodGridLayout.INPUT_COUNT, false);
        }
        boolean moved = false;
        for (int i = 0; i < GodGridLayout.INPUT_COUNT && !stack.isEmpty(); i++)
        {
            Slot slot = this.slots.get(i);
            if (slot instanceof LongSlotHandler.LongSlot longSlot)
            {
                if (!slot.mayPlace(stack))
                {
                    continue;
                }
                ItemStack leftover = longSlot.longHandler().insertItem(longSlot.slotIndex(), stack, false);
                int amount = stack.getCount() - leftover.getCount();
                if (amount > 0)
                {
                    stack.shrink(amount);
                    moved = true;
                }
            }
            else if (moveItemStackTo(stack, i, i + 1, false))
            {
                moved = true;
            }
        }
        return moved;
    }

    /**
     * 从自管 long 槽 Shift 快捷移动：一次只搬一份「视图」（≤99，等价于原来一次搬一堆），
     * 先真正取出再搬进玩家物品栏；放不下的部分原样塞回机器槽，绝不复制、不丢失。
     * 始终返回空，避免存量很大时 {@code doClick} 的 while 循环反复调用造成卡顿。
     */
    private ItemStack quickMoveOutOfLongSlot(LongSlotHandler.LongSlot slot)
    {
        ItemStack taken = slot.remove(Math.max(1, slot.getItem().getCount()));
        if (taken.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        if (!moveItemStackTo(taken, SLOT_PLAYER_START, this.slots.size(), true) || !taken.isEmpty())
        {
            // 一件都没搬动 / 只搬走一部分：余量原样塞回机器槽
            slot.longHandler().insertItem(slot.slotIndex(), taken, false);
        }
        return ItemStack.EMPTY;
    }
}
