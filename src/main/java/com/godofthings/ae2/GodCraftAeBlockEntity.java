package com.godofthings.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.godofthings.block.entity.GodCraftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 神之合成的 AE 版方块实体：产物自动输出进 AE；锁定模板时从 AE 网络拉原料补齐合成格。
 * <p>
 * <b>本类硬引用 appeng 类型，只在 AE2 已安装时由 {@link AeSoftDepend#blockEntity} 反射构造。</b>
 */
public class GodCraftAeBlockEntity extends GodCraftBlockEntity implements IGridConnectedBlockEntity
{
    private final AeGridNode aeNode = new AeGridNode(this);

    public GodCraftAeBlockEntity(BlockPos pos, BlockState state)
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

    /** 把输出槽产物推入 AE 网络（节流由 tick 控制）。 */
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
        ItemStack stack = getOutputSlot().getStackInSlot(0);
        if (stack.isEmpty())
        {
            return;
        }
        long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
        if (inserted > 0)
        {
            getOutputSlot().extractItem(0, (int) inserted, false);
        }
    }

    /** 从 AE 网络拉取锁定模板原料，补齐合成格（开启 AE + 锁定模板时自动合成）。 */
    @Override
    protected void aeAutoCraft()
    {
        if (!isAeEnabled() || !aeNode.isActive() || !isLocked())
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
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack tpl = getLockedItem(i);
            if (tpl.isEmpty())
            {
                continue;
            }
            ItemStack cur = getInputSlots().getStackInSlot(i);
            int have = (!cur.isEmpty() && ItemStack.isSameItem(cur, tpl)) ? cur.getCount() : 0;
            int need = tpl.getCount() - have;
            if (need <= 0)
            {
                continue;
            }
            AEItemKey key = AEItemKey.of(tpl);
            long extracted = inv.extract(key, need, Actionable.MODULATE, source);
            if (extracted > 0)
            {
                ItemStack got = tpl.copyWithCount((int) extracted);
                getInputSlots().insertItem(i, got, false);
            }
        }
    }
}