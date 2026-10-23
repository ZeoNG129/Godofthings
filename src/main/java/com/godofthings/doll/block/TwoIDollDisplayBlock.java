package com.godofthings.doll.block;

import com.godofthings.doll.blockentity.TwoIDollDisplayBlockEntity;
import com.godofthings.doll.item.TwoIDollAcquisition;
import com.godofthings.doll.item.TwoIDollItem;
import com.godofthings.doll.init.DollRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TwoIDollDisplayBlock extends Block implements EntityBlock {
   public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
   private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);

   public TwoIDollDisplayBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(ROTATION, 0));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{ROTATION});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      int rotation = Math.floorMod(Math.round(context.getRotation() / 22.5F) + 8, 16);
      return (BlockState)this.defaultBlockState().setValue(ROTATION, rotation);
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(ROTATION, rotation.rotate((Integer)state.getValue(ROTATION), 16));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return (BlockState)state.setValue(ROTATION, mirror.mirror((Integer)state.getValue(ROTATION), 16));
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TwoIDollDisplayBlockEntity(pos, state);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      ItemStack owned = stack.copyWithCount(1);
      if (placer instanceof Player player) {
         TwoIDollItem.claimOrCheckOwner(owned, player);
      }

      if (level.getBlockEntity(pos) instanceof TwoIDollDisplayBlockEntity display) {
         display.setDoll(owned);
      }
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return level.getBlockEntity(pos) instanceof TwoIDollDisplayBlockEntity display ? display.copyDoll() : new ItemStack((ItemLike)DollRegistry.TWO_I_DOLL_ITEM.get());
   }

   protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      return List.of(
         builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TwoIDollDisplayBlockEntity display
            ? display.copyDoll()
            : new ItemStack((ItemLike)DollRegistry.TWO_I_DOLL_ITEM.get())
      );
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      this.interact(state, level, pos, player);
      return ItemInteractionResult.SUCCESS;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return this.interact(state, level, pos, player);
   }

   private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player) {
      if (!level.isClientSide) {
         if (level.getBlockEntity(pos) instanceof TwoIDollDisplayBlockEntity display) {
            ItemStack owned = display.copyDoll();
            if (!TwoIDollItem.claimOrCheckOwner(owned, player)) {
               return InteractionResult.FAIL;
            }

            if (!ItemStack.isSameItemSameComponents(owned, display.copyDoll())) {
               display.setDoll(owned);
            }
         }

         if (player.isShiftKeyDown()) {
            ItemStack doll = this.getCloneItemStack(level, pos, state);
            ItemStack acquired = doll.copy();
            if (level.removeBlock(pos, false)) {
               boolean inserted = player.getInventory().add(doll);
               if (!inserted && !doll.isEmpty()) {
                  popResource(level, pos, doll);
               }

               player.getInventory().setChanged();
               if (inserted && player instanceof ServerPlayer serverPlayer) {
                  TwoIDollAcquisition.notifyAcquired(serverPlayer, acquired);
               }
            }
         } else if (level.getBlockEntity(pos) instanceof TwoIDollDisplayBlockEntity display) {
            display.toggleSpinning();
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}
