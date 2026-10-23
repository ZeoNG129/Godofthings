package com.godofthings.doll.blockentity;

import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TwoIDollDisplayBlockEntity extends BlockEntity {
   private ItemStack doll = ItemStack.EMPTY;
   private boolean spinning;
   private float rotationOffset;
   private long spinStartedAt;

   public TwoIDollDisplayBlockEntity(BlockPos pos, BlockState state) {
      super(DollRegistry.TWO_I_DOLL_DISPLAY_BE.get(), pos, state);
   }

   public void setDoll(ItemStack stack) {
      this.doll = stack.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get()) ? stack.copyWithCount(1) : ItemStack.EMPTY;
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public ItemStack copyDoll() {
      return this.doll.isEmpty() ? new ItemStack((ItemLike)DollRegistry.TWO_I_DOLL_ITEM.get()) : this.doll.copyWithCount(1);
   }

   public boolean isSpinning() {
      return this.spinning;
   }

   public float rotationAt(double gameTime) {
      return (float)(((double)this.rotationOffset + (this.spinning ? Math.max(0.0, gameTime - (double)this.spinStartedAt) * 1.8 : 0.0)) % 360.0);
   }

   public void toggleSpinning() {
      if (this.level != null && !this.level.isClientSide) {
         this.rotationOffset = this.rotationAt((double)this.level.getGameTime());
         this.spinStartedAt = this.level.getGameTime();
         this.spinning = !this.spinning;
         this.setChanged();
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      if (!this.doll.isEmpty()) {
         tag.put("Doll", this.doll.save(registries));
      }

      tag.putBoolean("Spinning", this.spinning);
      tag.putFloat("SpinAngle", this.rotationOffset);
      tag.putLong("SpinStart", this.spinStartedAt);
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      ItemStack restored = tag.contains("Doll") ? ItemStack.parseOptional(registries, tag.getCompound("Doll")) : ItemStack.EMPTY;
      this.doll = restored.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get()) ? restored.copyWithCount(1) : ItemStack.EMPTY;
      this.spinning = tag.getBoolean("Spinning");
      float angle = tag.getFloat("SpinAngle");
      this.rotationOffset = Float.isFinite(angle) ? angle % 360.0F : 0.0F;
      this.spinStartedAt = tag.getLong("SpinStart");
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
