package com.godofthings.torchmaster.common.logic.entityblocking.megatorch;

import com.godofthings.torchmaster.Torchmaster;
import com.godofthings.torchmaster.TorchmasterConfig;
import com.godofthings.torchmaster.common.ModBlocks;
import com.godofthings.torchmaster.common.logic.DistanceLogics;
import com.godofthings.torchmaster.common.logic.entityblocking.IEntityBlockingLight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MegatorchEntityBlockingLight implements IEntityBlockingLight {
   public static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
   private final BlockPos pos;

   public MegatorchEntityBlockingLight(BlockPos pos) {
      this.pos = pos;
   }

   @Override
   public boolean shouldBlockEntity(Entity entity, BlockPos pos) {
      return Torchmaster.MegaTorchFilterRegistry.containsEntity(EntityType.getKey(entity.getType()))
         && DistanceLogics.Cubic
            .isPositionInRange(
               (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), this.pos, (Integer)TorchmasterConfig.GENERAL.megaTorchRadius.get()
            );
   }

   @Override
   public boolean shouldBlockVillageSiege(BlockPos pos) {
      return DistanceLogics.Cubic
         .isPositionInRange(
            (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), this.pos, (Integer)TorchmasterConfig.GENERAL.megaTorchRadius.get()
         );
   }

   @Override
   public String getLightSerializerKey() {
      return "megatorch";
   }

   @Override
   public boolean cleanupCheck(Level level) {
      return level.isLoaded(this.pos) && level.getBlockState(this.pos).getBlock() != ModBlocks.blockMegaTorch.get();
   }

   @Override
   public String getName() {
      return "Mega Torch";
   }

   @Override
   public BlockPos getPos() {
      return this.pos;
   }
}
