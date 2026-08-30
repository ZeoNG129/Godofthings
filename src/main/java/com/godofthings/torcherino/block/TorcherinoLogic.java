package com.godofthings.torcherino.block;

import com.godofthings.torcherino.Torcherino;
import com.godofthings.torcherino.api.TierSupplier;
import com.godofthings.torcherino.block.entity.TorcherinoBlockEntity;
import com.godofthings.torcherino.config.TorcherinoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;

/**
 * 加速火把逻辑：交互 / 相邻更新 / 放置 / 方块实体 tick 分发。
 */
public final class TorcherinoLogic
{
    private TorcherinoLogic() {}

    public static InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (world.isClientSide || hand == InteractionHand.OFF_HAND)
        {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof TorcherinoBlockEntity blockEntity)
        {
            blockEntity.openTorcherinoScreen((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    public static void neighborUpdate(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving,
                                      Consumer<TorcherinoBlockEntity> func)
    {
        if (world.isClientSide)
        {
            return;
        }
        if (world.getBlockEntity(pos) instanceof TorcherinoBlockEntity blockEntity)
        {
            func.accept(blockEntity);
        }
    }

    public static void onPlaced(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, Block block)
    {
        if (world.isClientSide)
        {
            return;
        }
        if (world.getBlockEntity(pos) instanceof TorcherinoBlockEntity blockEntity)
        {
            if (stack.hasCustomHoverName())
            {
                blockEntity.setCustomName(stack.getHoverName());
            }
            if (!TorcherinoConfig.INSTANCE.online_mode.isEmpty())
            {
                blockEntity.setOwner(placer == null ? "" : placer.getStringUUID());
            }
        }
        if (TorcherinoConfig.INSTANCE.log_placement)
        {
            String prefix = placer == null ? "Something" : placer.getDisplayName().getString() + "(" + placer.getStringUUID() + ")";
            Torcherino.LOGGER.info("[Torcherino] {} placed a {} at {}, {}, {}.", prefix,
                    BuiltInRegistries.BLOCK.getKey(block), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (state.getBlock() instanceof TierSupplier)
        {
            try
            {
                return (level1, pos, state1, entity) -> TorcherinoBlockEntity.tick(level1, pos, state1, (TorcherinoBlockEntity) entity);
            }
            catch (ClassCastException e)
            {
                return null;
            }
        }
        return null;
    }
}
