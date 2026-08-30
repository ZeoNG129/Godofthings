package com.godofthings.torchmaster.common.logic.entityblocking.dreadlamp;

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

public class DreadLampEntityBlockingLight implements IEntityBlockingLight {
   public static final VoxelShape SHAPE = Block.box(1.0, 1.0, 1.0, 15.0, 15.0, 15.0);
   private final BlockPos pos;

   public DreadLampEntityBlockingLight(BlockPos pos) {
      this.pos = pos;
   }

   @Override
   public boolean shouldBlockEntity(Entity entity, BlockPos pos) {
      return Torchmaster.DreadLampFilterRegistry.containsEntity(EntityType.getKey(entity.getType()))
         && DistanceLogics.Cubic
            .isPositionInRange(
               (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), this.pos, (Integer)TorchmasterConfig.GENERAL.dreadLampRadius.get()
            );
   }

   @Override
   public boolean shouldBlockVillageSiege(BlockPos pos) {
      return false;
   }

   @Override
   public String getLightSerializerKey() {
      return "dreadlamp";
   }

   @Override
   public boolean cleanupCheck(Level level) {
      return level.isLoaded(this.pos) && level.getBlockState(this.pos).getBlock() != ModBlocks.blockDreadLamp.get();
   }

   @Override
   public String getName() {
      return "Dread Lamp";
   }

   @Override
   public BlockPos getPos() {
      return this.pos;
   }
}
