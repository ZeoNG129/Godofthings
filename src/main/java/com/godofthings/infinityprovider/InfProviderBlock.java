package com.godofthings.infinityprovider;

import appeng.block.AEBaseEntityBlock;
import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.util.InteractionUtil;
import appeng.util.Platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 自定义样板供应器方块的通用基类（移植自 InfinityPatternProvider 的 {@code CustomPatternProviderBlock}）：
 * 右键打开界面 / 扳手旋转推动面 / 绑定方块实体，供四档供应器方块共用。
 */
public abstract class InfProviderBlock<T extends AEBaseBlockEntity> extends AEBaseEntityBlock<T> {

    public InfProviderBlock(Properties props) {
        super(props);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack heldItem, BlockState blockState, Level level,
            BlockPos blockPosition, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        ItemInteractionResult superResult =
                super.useItemOn(heldItem, blockState, level, blockPosition, player, interactionHand, blockHitResult);
        if (superResult != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return superResult;
        }
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        T blockEntity = this.getBlockEntity(level, blockPosition);
        if (blockEntity == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemInteractionResult wrenchResult = this.checkPlayerInteractionWhenUseItem(
                blockEntity, heldItem, level, blockPosition, blockHitResult, player);
        if (wrenchResult != null) {
            return wrenchResult;
        }
        if (!level.isClientSide) {
            this.openGui(blockEntity, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPosition,
            Player player, BlockHitResult blockHitResult) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS;
        }
        T blockEntity = this.getBlockEntity(level, blockPosition);
        if (blockEntity != null) {
            if (!level.isClientSide) {
                this.openGui(blockEntity, player);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public abstract void openGui(BlockEntity blockEntity, Player player);

    @Nullable
    public ItemInteractionResult checkPlayerInteractionWhenUseItem(T blockEntity, ItemStack itemStack,
            Level world, BlockPos blockPosition, BlockHitResult blockHitResult, Player player) {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PatternProviderBlock.PUSH_DIRECTION);
    }

    /** 扳手点面旋转推动方向（与 AE2 样板供应器一致的循环规则）。 */
    public void setSide(Level level, BlockPos blockPosition, Direction facingDirection) {
        BlockState currentState = level.getBlockState(blockPosition);
        Direction pushSide = currentState.getValue(PatternProviderBlock.PUSH_DIRECTION).getDirection();
        PushDirection newPushDirection;
        if (pushSide == facingDirection.getOpposite()) {
            newPushDirection = PushDirection.fromDirection(facingDirection);
        } else if (pushSide == facingDirection) {
            newPushDirection = PushDirection.ALL;
        } else if (pushSide == null) {
            newPushDirection = PushDirection.fromDirection(facingDirection.getOpposite());
        } else {
            newPushDirection = PushDirection.fromDirection(Platform.rotateAround(pushSide, facingDirection));
        }
        level.setBlockAndUpdate(blockPosition,
                currentState.setValue(PatternProviderBlock.PUSH_DIRECTION, newPushDirection));
    }

    /** 绑定方块实体（AE2 特有流程，commonSetup 里调用）。 */
    public static <T extends AEBaseBlockEntity> void bindBlockAndEntity(
            AEBaseEntityBlock<T> block, BlockEntityType<T> blockEntityType, Class<T> blockEntityClass) {
        BlockEntityTicker<T> serverTicker = null;
        if (ServerTickingBlockEntity.class.isAssignableFrom(blockEntityClass)) {
            serverTicker = (level, pos, state, entity) -> ((ServerTickingBlockEntity) entity).serverTick();
        }
        BlockEntityTicker<T> clientTicker = null;
        if (ClientTickingBlockEntity.class.isAssignableFrom(blockEntityClass)) {
            clientTicker = (level, pos, state, entity) -> ((ClientTickingBlockEntity) entity).clientTick();
        }
        block.setBlockEntity(blockEntityClass, blockEntityType, clientTicker, serverTicker);
    }
}
