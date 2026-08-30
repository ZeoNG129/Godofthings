package com.godofthings.generator;

import net.minecraftforge.energy.IEnergyStorage;

public class RelayEnergyStorage implements IEnergyStorage {
   private final EnergyRelayEntity owner;

   public RelayEnergyStorage(EnergyRelayEntity owner) {
      this.owner = owner;
   }

   public int getEnergyStored() {
      return EnergyGenTool.suitInt(this.owner.energy);
   }

   public int receiveEnergy(int maxReceive, boolean simulate) {
      if (this.canReceive() && maxReceive > 0) {
         long space = Long.MAX_VALUE - this.owner.energy;
         if (space <= 0L) {
            return 0;
         } else {
            int accepted = (int)Math.min((long)maxReceive, space);
            if (accepted <= 0) {
               return 0;
            } else {
               if (!simulate) {
                  this.owner.energy += (long)accepted;
                  this.owner.setChanged();
               }

               return accepted;
            }
         }
      } else {
         return 0;
      }
   }

   public int extractEnergy(int maxExtract, boolean simulate) {
      if (this.canExtract() && maxExtract > 0) {
         int maxOutput = EnergyGenTool.suitInt(this.owner.energy);
         if (maxOutput <= 0) {
            return 0;
         } else {
            int ret = Math.min(maxOutput, maxExtract);
            if (!simulate) {
               this.owner.energy -= (long)ret;
               this.owner.setChanged();
            }

            return ret;
         }
      } else {
         return 0;
      }
   }

   public int getMaxEnergyStored() {
      return EnergyGenTool.suitInt(Long.MAX_VALUE);
   }

   public boolean canExtract() {
      return true;
   }

   public boolean canReceive() {
      return true;
   }
}
