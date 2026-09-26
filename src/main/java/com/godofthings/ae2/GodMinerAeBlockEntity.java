package com.godofthings.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.godofthings.block.entity.GodMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 神之矿机的 AE 版方块实体。
 * <p>
 * <b>本类硬引用 appeng 类型，只在 AE2 已安装时由 {@link AeSoftDepend#blockEntity} 反射构造。</b>
 */
public class GodMinerAeBlockEntity extends GodMinerBlockEntity implements IGridConnectedBlockEntity
{
    private final AeGridNode aeNode = new AeGridNode(this);

    public GodMinerAeBlockEntity(BlockPos pos, BlockState state)
    {
        super(pos, state);
    }

    @Override
    public IManagedGridNode getMainNode()
    {
        return aeNode.getMainNode();
    }

    @Override
    public void saveChanges()
    {
        setChanged();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        aeNode.create(getLevel(), getBlockPos());
    }

    @Override
    public void setRemoved()
    {
        aeNode.destroy();
        super.setRemoved();
    }
    /** 把内置储存产物推入 AE 网络（节流由 tick 控制）。 */
    @Override
    protected void pushOutputToAe()
    {
        if (!isAeEnabled() || !aeNode.isActive())
        {
            return;
        }
        IStorageService storage = aeNode.getStorage();
        if (storage == null)
        {
            return;
        }
        MEStorage inv = storage.getInventory();
        IActionSource source = aeNode.actionSource();
        for (int slot = 0; slot < getItemHandler().getSlots(); slot++)
        {
            ItemStack stack = getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
            if (inserted > 0)
            {
                getItemHandler().extractItem(slot, (int) inserted, false);
            }
        }
    }
}