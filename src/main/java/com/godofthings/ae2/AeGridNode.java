package com.godofthings.ae2;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.util.AECableType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * AE2 网格节点封装：让机器像 AE 设备一样直接并网（线缆直连、占一个频道），
 * 并可把产物主动推入 AE 网络（非「存储总线当箱子读」）。
 * <p>
 * 方块实体持有本类实例，实现 {@link IInWorldGridNodeHost} / {@link IActionHost} 并委托，
 * 在 onLoad 调 {@link #create}、setRemoved 调 {@link #destroy}；tick 时用 {@link #getStorage()}
 * 拿到网络存储，把产物 {@code MEStorage.insert(...)} 进网络。
 */
public class AeGridNode implements IInWorldGridNodeHost, IActionHost
{
    private final BlockEntity be;
    @Nullable
    private IManagedGridNode mainNode;

    public AeGridNode(BlockEntity be)
    {
        this.be = be;
    }

    private IManagedGridNode getMainNode()
    {
        if (mainNode == null)
        {
            mainNode = GridHelper.createManagedNode(be, new GridNodeListener());
            mainNode.setFlags(GridFlags.REQUIRE_CHANNEL); // 占一个频道
            mainNode.setInWorldNode(true); // 关键：世界内节点，AE 线缆才能连接
            mainNode.setExposedOnSides(EnumSet.allOf(Direction.class));
            mainNode.setIdlePowerUsage(1.0);
            // 网络工具/控制器里显示机器方块图标（而非默认线缆）
            mainNode.setVisualRepresentation(be.getBlockState().getBlock().asItem());
        }
        return mainNode;
    }

    /** onLoad 时调用：创建网格节点（服务端）。 */
    public void create(Level level, BlockPos pos)
    {
        if (level != null && !level.isClientSide)
        {
            getMainNode().create(level, pos);
        }
    }

    /** setRemoved 时调用：销毁网格节点。 */
    public void destroy()
    {
        if (mainNode != null)
        {
            mainNode.destroy();
        }
    }

    public boolean isActive()
    {
        IGridNode node = getMainNode().getNode();
        return node != null && node.isActive();
    }

    /** 拿到 AE 网络存储服务（未并网返回 null）。 */
    @Nullable
    public IStorageService getStorage()
    {
        IGridNode node = getMainNode().getNode();
        if (node == null || node.getGrid() == null)
        {
            return null;
        }
        return node.getGrid().getStorageService();
    }

    public IActionSource actionSource()
    {
        return IActionSource.ofMachine(this);
    }

    // ---- IInWorldGridNodeHost ----

    @Override
    public IGridNode getGridNode(Direction side)
    {
        return getMainNode().getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction side)
    {
        return AECableType.SMART;
    }

    // ---- IActionHost ----

    @Override
    public IGridNode getActionableNode()
    {
        return getMainNode().getNode();
    }

    private static class GridNodeListener implements IGridNodeListener<BlockEntity>
    {
        @Override
        public void onSaveChanges(BlockEntity be, IGridNode node)
        {
            be.setChanged();
        }
    }
}
