package com.godofthings.elevator.block;

import com.godofthings.elevator.ElevatorBlockBase;
import com.godofthings.elevator.ElevatorRegistry;
import com.godofthings.elevator.client.ElevatorBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 电梯方块实体：保存「伪装成哪个方块」。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.level.ElevatorBlockEntityBase}
 * 与 {@code com.vsngarcia.neoforge.tile.ElevatorBlockEntity}（MIT），<b>逐行对照编写</b>。</p>
 *
 * <p>[本地改动] {@code markUpdated()} 里比上游少了「辅助光源管理器」一步（伪装方块的发光同步），
 * 因为 {@code ElevatorBlock} 侧没有移植 {@code getLightEmission(state, level, pos)} /
 * {@code hasDynamicLightEmission}（本版本无这些重载）。</p>
 *
 * <h2>外观是怎么画出来的（v7.18.3 起）</h2>
 * 不再用方块实体渲染器。伪装状态通过 {@link #getModelData()} 送进 baked model
 * （{@link ElevatorBakedModel#HELD_STATE}），由 {@link ElevatorBakedModel#getQuads} 直接把伪装方块的
 * quad 拷进电梯的静态区块几何。—— 与上游完全一致。
 * <br>因此每次状态变化都必须调 {@link #setChanged()} 与 {@code requestModelDataUpdate()}，
 * 否则区块不会重烘焙，外观就不跟着变。
 */
public class ElevatorBlockEntity extends BlockEntity implements MenuProvider
{
    /** 伪装成的方块状态；null = 未伪装（显示电梯本体的颜色）。 */
    private BlockState heldState;

    public ElevatorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ElevatorRegistry.ELEVATOR_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * 把伪装状态交给 baked model。
     *
     * <p>移植自上游 {@code com.vsngarcia.neoforge.tile.ElevatorBlockEntity#getModelData()}。
     * 这是伪装外观能画出来的唯一数据通道 —— 渲染侧只读这份 ModelData，不读方块实体。</p>
     */
    @Override
    public ModelData getModelData()
    {
        return ModelData.builder()
                .with(ElevatorBakedModel.HELD_STATE, heldState)
                .build();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider holder)
    {
        super.loadAdditional(tag, holder);
        if (tag.contains("held_id"))
        {
            BlockState state = NbtUtils.readBlockState(
                    this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup(),
                    tag.getCompound("held_id"));
            heldState = isValidState(state) ? state : null;
        }
        else
        {
            heldState = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider holder)
    {
        super.saveAdditional(tag, holder);
        if (heldState != null)
        {
            tag.put("held_id", NbtUtils.writeBlockState(heldState));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider holder)
    {
        return saveWithId(holder);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider holder)
    {
        handleUpdateTag(pkt.getTag(), holder);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider holder)
    {
        super.handleUpdateTag(tag, holder);
        markUpdated();
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.elevator.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player)
    {
        return new ElevatorContainer(id, this.worldPosition, player);
    }

    public void setHeldState(BlockState state)
    {
        this.heldState = state;
        markUpdated();
    }

    /**
     * 刷新方块外观。
     *
     * <p>移植自上游 {@code setChanged()}：除了通知客户端方块实体数据，还要 {@code requestModelDataUpdate()}
     * 让区块重新烘焙模型数据 —— 少了这一步，伪装状态改了但外观不跟着变。</p>
     */
    private void markUpdated()
    {
        setChanged();
        if (level == null)
        {
            return;
        }
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        if (level.isClientSide)
        {
            requestModelDataUpdate();
            return;
        }
        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        getBlockState().updateNeighbourShapes(level, worldPosition, 2);
        level.getLightEngine().checkBlock(worldPosition);
    }

    public BlockState getHeldState()
    {
        return heldState;
    }

    /** 设置伪装并广播；返回是否真的改变了。 */
    public boolean setCamoAndUpdate(BlockState newState)
    {
        if (heldState == newState)
        {
            return false;
        }
        if (!isValidState(newState))
        {
            return false;
        }
        setHeldState(newState);
        if (getLevel() != null)
        {
            getLevel().playSound(null, getBlockPos(), ElevatorRegistry.CAMOUFLAGE_SOUND.get(),
                    SoundSource.BLOCKS, 1F, 1F);
        }
        return true;
    }

    /**
     * 能不能伪装成这个状态。
     *
     * <p>规则与上游一致：非空气、非电梯自身、渲染形状为 MODEL（排除箱子这类靠实体的方块）、
     * 且有碰撞箱。</p>
     */
    public static boolean isValidState(BlockState state)
    {
        if (state == null)
        {
            return true;
        }
        if (state.getBlock() == Blocks.AIR)
        {
            return false;
        }
        if (state.getBlock() instanceof ElevatorBlockBase)
        {
            return false;
        }
        if (state.getRenderShape() != RenderShape.MODEL)
        {
            return false;
        }
        return !state.getCollisionShape(null, null).isEmpty();
    }
}
