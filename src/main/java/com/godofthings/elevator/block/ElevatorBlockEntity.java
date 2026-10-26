package com.godofthings.elevator.block;

import com.godofthings.elevator.ElevatorBlockBase;
import com.godofthings.elevator.ElevatorRegistry;
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

/**
 * 电梯方块实体：保存「伪装成哪个方块」。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.level.ElevatorBlockEntityBase}
 * 与 {@code com.vsngarcia.neoforge.tile.ElevatorBlockEntity}（MIT），<b>逐行对照编写</b>。</p>
 *
 * <p>[本地改动] {@code markUpdated()} 里比上游少了「辅助光源管理器」与「模型数据刷新」两步——
 * 本版本 NeoForge 没有 {@code getAuxLightManager}，也没有 {@code ModelDataProvider}；
 * 伪装的渲染改由 {@code ElevatorBakedModel} 直接读本实体完成（见客户端渲染类）。
 * 方块更新 + 光引擎重算仍然保留，保证外观与亮度遮挡即刻生效。</p>
 */
public class ElevatorBlockEntity extends BlockEntity implements MenuProvider
{
    /** 伪装成的方块状态；null = 未伪装（显示电梯本体的颜色）。 */
    private BlockState heldState;

    public ElevatorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ElevatorRegistry.ELEVATOR_BLOCK_ENTITY.get(), pos, state);
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

    private void markUpdated()
    {
        setChanged();
        if (level == null)
        {
            return;
        }
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
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
