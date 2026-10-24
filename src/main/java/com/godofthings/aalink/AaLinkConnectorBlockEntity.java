package com.godofthings.aalink;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.util.AECableType;

/**
 * 无线网格桥接中枢（移植自 AALink 的 WirelessConnectorBlockEntity）。
 *
 * <p>核心功能：把本方块放进 ME 网络里（放好后其 in-world 网格节点暴露在六个面，
 * 与周围线缆/控制器自动建连），然后玩家在任意地方放置任意能连接 AE 网络的设备
 * （如输入总线），这些设备会被自动接入本方块所在的那个 ME 网络——无视距离、无视维度。</p>
 *
 * <p>实现思路：本方块承载一个 {@link IManagedGridNode}（in-world，六面暴露）。
 * 另有 {@link AaLinkBridge} 在 NeoForge 事件总线上监听方块放置事件：每当一个
 * AE 设备被放置，就把该设备节点与本文方块节点通过 {@link GridHelper#createConnection}
 * 逻辑建连（不依赖物理位置，天然无视距离与维度），从而使该设备并入本网络。</p>
 */
public class AaLinkConnectorBlockEntity extends BlockEntity implements IInWorldGridNodeHost
{
    private final IManagedGridNode mainNode;
    private boolean nodeCreated;

    public AaLinkConnectorBlockEntity(BlockPos pos, BlockState state)
    {
        super(AaLinkRegistry.WIRELESS_CONNECTOR_BE.get(), pos, state);
        // 创建 in-world 网格节点：暴露在全部六个面，放置后与相邻 ME 设备自动建连，
        // 从而「放在网络里」即接入该 ME 网格。
        this.mainNode = GridHelper.createManagedNode(this, new NodeListener())
                .setInWorldNode(true)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .setIdlePowerUsage(0.0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AaLinkConnectorBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.ensureNodeCreated();
    }

    private void ensureNodeCreated()
    {
        if (this.nodeCreated || this.level == null || this.level.isClientSide)
        {
            return;
        }
        this.nodeCreated = true;
        this.mainNode.create(this.level, this.worldPosition);
        // 登记为桥接中枢，供 AaLinkBridge 将任意放置的 AE 设备接入本网络。
        AaLinkBridge.registerHub(this.mainNode.getNode());
        // 中枢节点（重新）创建后，把本维度已桥接设备重新接入网络：
        // 中枢区块卸载→重载时，其它仍加载中的设备不会触发放置/区块加载事件，需在此主动重连。
        if (this.level instanceof ServerLevel serverLevel)
        {
            AaLinkBridge.requeueLinked(serverLevel);
        }
    }

    public IGridNode getHubNode()
    {
        return this.mainNode.getNode();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.mainNode.loadFromNBT(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        this.mainNode.saveToNBT(tag);
    }

    @Override
    public void setRemoved()
    {
        if (this.nodeCreated && this.mainNode.getNode() != null)
        {
            AaLinkBridge.unregisterHub(this.mainNode.getNode());
            this.mainNode.destroy();
        }
        super.setRemoved();
    }

    // ---------------- IInWorldGridNodeHost ----------------

    @Override
    public IGridNode getGridNode(Direction direction)
    {
        return this.mainNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction direction)
    {
        return AECableType.SMART;
    }

    /** 网格节点监听器：节点状态变化时标记方块为已变更以便存档。 */
    private static final class NodeListener implements IGridNodeListener<AaLinkConnectorBlockEntity>
    {
        @Override
        public void onSaveChanges(AaLinkConnectorBlockEntity owner, IGridNode node)
        {
            owner.setChanged();
        }
    }
}
