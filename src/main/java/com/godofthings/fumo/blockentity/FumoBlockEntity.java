/*
 * 照抄自 AE2 Lightning Tech Reborn（作者 MOAKIEE、CystrySU、gjmhmm8、_leng、TedXenon、MHanHanBing）
 * 的 fumo 玩偶系统，仅改动包名与命名空间，并裁掉本模组未移植的「超维猪咪」分支。
 *
 * 许可：本文件源码沿用上游的 GNU LGPL 3.0 —— 见仓库 LICENSES/AE2LT-LGPL-3.0.txt。
 * 相关素材（models/block/hoyoog_fumo.json 那份玩家模型）沿用上游的 CC BY-NC-SA 3.0 ——
 * 见 LICENSES/AE2LT-ASSETS-CC-BY-NC-SA-3.0.md（署名 / 禁止商用 / 相同方式共享）。
 * 本模组的贴图 textures/block/hoyoog_fumo.png 不是上游素材，是作者自己的皮肤。
 */
package com.godofthings.fumo.blockentity;

import com.godofthings.fumo.registry.ModFumos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FumoBlockEntity extends BlockEntity {

    public static final float SPIN_DEGREES_PER_TICK = 6.0F;

    private static final String TAG_SPINNING = "Spinning";

    private boolean spinning;
    private float yRot;
    private float prevYRot;

    public FumoBlockEntity(BlockPos pos, BlockState state) {
        super(ModFumos.FUMO.get(), pos, state);
    }

    public boolean isSpinning() {
        return spinning;
    }

    public float getRenderYRot(float partialTick) {
        return prevYRot + (yRot - prevYRot) * partialTick;
    }

    public void toggleSpinning() {
        spinning = !spinning;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FumoBlockEntity be) {
        be.prevYRot = be.yRot;
        if (be.spinning) {
            be.yRot += SPIN_DEGREES_PER_TICK;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_SPINNING, spinning);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spinning = tag.getBoolean(TAG_SPINNING);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean(TAG_SPINNING, spinning);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
