package com.godofthings.generator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class EnergyRelayBlock extends Block implements EntityBlock {
   public EnergyRelayBlock(Properties properties) {
      super(properties);
   }

   public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
      return new EnergyRelayEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
      return (l, p, s, tile) -> this.tick(l, tile);
   }

   private <T extends BlockEntity> void tick(Level level, T tile) {
      if (!level.isClientSide && tile instanceof EnergyRelayEntity relay) {
         relay.serverTick();
      }
   }

   @Nonnull
   public InteractionResult use(
      @Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit
   ) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else if (level.getBlockEntity(pos) instanceof EnergyRelayEntity relay) {
         if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, relay, pos);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.FAIL;
      }
   }
}
