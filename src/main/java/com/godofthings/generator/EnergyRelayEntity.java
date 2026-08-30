package com.godofthings.generator;

import com.godofthings.Godofthings;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyRelayEntity extends BlockEntity implements MenuProvider {
   public static final long CAPACITY = Long.MAX_VALUE;
   private final LazyOptional<RelayEnergyStorage> energyCap = LazyOptional.of(() -> new RelayEnergyStorage(this));
   public long energy = 0L;
   public boolean wirelessOn = false;
   public int wirelessInterval = 5;
   public int wirelessRange = 1;
   public int transferRepeat = 1;
   public boolean transferDown = true;
   public boolean transferUp = true;
   public boolean transferNorth = true;
   public boolean transferSouth = true;
   public boolean transferWest = true;
   public boolean transferEast = true;
   public long scanCursor = 0L;
   public final Map<BlockPos, Direction> wirelessTargets = new HashMap<>();
   private int findIndex = 0;

   public EnergyRelayEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)Godofthings.ENERGY_RELAY_BE.get(), pos, state);
   }

   public void serverTick() {
      Level level = this.getLevel();
      if (level != null && !level.isClientSide) {
         // 无线充电（优先于有线输电，避免电量被六面输电先排空）：
         // 先给范围内生物携带的可充电物品充电，再隔空向范围内机器输电
         if (this.wirelessOn) {
            this.wirelessChargeEntities();
            this.scanWirelessSlice();
            this.wirelessTransfer();
         }

         this.outputToSides();
         this.setChanged();
      }
   }

   /** 无线充电：给无线范围内生物（玩家/怪物）携带的所有可充电物品充电 */
   private void wirelessChargeEntities() {
      Level level = this.getLevel();
      if (level != null && this.energy > 0L) {
         EnergyGenTool.chargeEntitiesInWirelessRange(level, this.getBlockPos(), this.wirelessRange, () -> this.energy, consumed -> this.energy -= consumed);
      }
   }

   private void outputToSides() {
      Level level = this.getLevel();
      if (level != null && this.energy > 0L) {
         Direction[] directions = Direction.values();

         for (int rep = 0; rep < this.transferRepeat && this.energy > 0L; rep++) {
            for (int i = 0; i < directions.length; i++) {
               if (this.energy <= 0L) {
                  return;
               }

               this.findIndex = (this.findIndex + 1) % directions.length;
               Direction direction = directions[this.findIndex];
               if (this.isTransferEnabled(direction)) {
                  BlockPos pos = this.getBlockPos().relative(direction);
                  if (level.isLoaded(pos)) {
                     BlockEntity entity = level.getBlockEntity(pos);
                     if (entity != null) {
                        entity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite())
                           .resolve()
                           .filter(IEnergyStorage::canReceive)
                           .ifPresent(storage -> {
                              int maxOutput = EnergyGenTool.suitInt(this.energy);
                              int result = storage.receiveEnergy(maxOutput, false);
                              if (result < 0) {
                                 result = 0;
                              }

                              if (result > maxOutput) {
                                 result = maxOutput;
                              }

                              if (result > 0) {
                                 this.energy -= (long)result;
                              }
                           });
                     }
                  }
               }
            }
         }
      }
   }

   private void scanWirelessSlice() {
      Level level = this.getLevel();
      if (level != null) {
         int range = EnergyGenTool.normalizeWirelessRange(this.wirelessRange);
         int half = range >> 1;
         BlockPos pos = this.getBlockPos();
         int originX = pos.getX() >> 4 << 4;
         int originZ = pos.getZ() >> 4 << 4;
         int minX = originX - half * 16;
         int minZ = originZ - half * 16;
         int width = range * 16;
         int minY = level.getMinBuildHeight();
         long layerSize = (long)width * (long)width;
         long volume = layerSize * (long)level.getHeight();
         this.wirelessTargets.keySet().removeIf(bp -> {
            int bx = bp.getX() >> 4;
            int bz = bp.getZ() >> 4;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            return Math.abs(bx - cx) > half || Math.abs(bz - cz) > half;
         });
         long slices = Math.max(1L, (long)Math.max(1, this.wirelessInterval) * 20L);
         long sliceSize = Math.max(1L, (volume + slices - 1L) / slices);
         long start = this.scanCursor;
         long end = Math.min(volume, start + sliceSize);
         this.scanLinearRange(level, minX, minZ, width, minY, layerSize, start, end);
         this.scanCursor = end >= volume ? 0L : end;
      }
   }

   private void scanLinearRange(Level level, int minX, int minZ, int width, int minY, long layerSize, long from, long to) {
      if (from < to) {
         BlockPos pos = this.getBlockPos();
         int cMinX = minX >> 4;
         int cMaxX = minX + width - 1 >> 4;
         int cMinZ = minZ >> 4;
         int cMaxZ = minZ + width - 1 >> 4;

         for (int cx = cMinX; cx <= cMaxX; cx++) {
            for (int cz = cMinZ; cz <= cMaxZ; cz++) {
               if (level.isLoaded(new BlockPos(cx << 4, pos.getY(), cz << 4))) {
                  LevelChunk chunk = level.getChunk(cx, cz);
                  if (chunk != null) {
                     for (Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                        BlockPos bp = entry.getKey();
                        if (!bp.equals(this.worldPosition)) {
                           long idx = (long)(bp.getX() - minX) + (long)(bp.getZ() - minZ) * (long)width + (long)(bp.getY() - minY) * layerSize;
                           if (idx >= from && idx < to) {
                              this.refreshWirelessTarget(bp, entry.getValue());
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void refreshWirelessTarget(BlockPos bp, BlockEntity target) {
      for (Direction dir : Direction.values()) {
         if (target.getCapability(ForgeCapabilities.ENERGY, dir).resolve().<Boolean>map(IEnergyStorage::canReceive).orElse(false)) {
            this.wirelessTargets.put(bp.immutable(), dir);
            return;
         }
      }

      this.wirelessTargets.remove(bp);
   }

   private void wirelessTransfer() {
      Level level = this.getLevel();
      if (level != null && this.energy > 0L) {
         int repeat = Math.max(1, this.transferRepeat);

         for (int rep = 0; rep < repeat && this.energy > 0L; rep++) {
            for (Entry<BlockPos, Direction> entry : this.wirelessTargets.entrySet()) {
               if (this.energy <= 0L) {
                  return;
               }

               BlockPos targetPos = entry.getKey();
               if (level.isLoaded(targetPos)) {
                  BlockEntity target = level.getBlockEntity(targetPos);
                  if (target != null && target != this) {
                     target.getCapability(ForgeCapabilities.ENERGY, entry.getValue()).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
                        int maxOutput = EnergyGenTool.suitInt(this.energy);
                        int result = storage.receiveEnergy(maxOutput, false);
                        if (result < 0) {
                           result = 0;
                        }

                        if (result > maxOutput) {
                           result = maxOutput;
                        }

                        if (result > 0) {
                           this.energy -= (long)result;
                        }
                     });
                  }
               }
            }
         }
      }
   }

   public boolean isTransferEnabled(Direction direction) {
      return switch (direction) {
         case DOWN -> this.transferDown;
         case UP -> this.transferUp;
         case NORTH -> this.transferNorth;
         case SOUTH -> this.transferSouth;
         case WEST -> this.transferWest;
         case EAST -> this.transferEast;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   @Nonnull
   public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
      return capability == ForgeCapabilities.ENERGY ? this.energyCap.cast() : super.getCapability(capability, direction);
   }

   @Nonnull
   public Component getDisplayName() {
      return Component.translatable("block.godofthings.energy_relay");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
      return new EnergyRelayMenu(id, inv, this.worldPosition);
   }

   public void saveAdditional(@Nonnull CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putLong("energy", this.energy);
      nbt.putBoolean("wirelessOn", this.wirelessOn);
      nbt.putInt("wirelessInterval", this.wirelessInterval);
      nbt.putInt("wirelessRange", this.wirelessRange);
      nbt.putInt("transferRepeat", this.transferRepeat);
      nbt.putBoolean("transferDown", this.transferDown);
      nbt.putBoolean("transferUp", this.transferUp);
      nbt.putBoolean("transferNorth", this.transferNorth);
      nbt.putBoolean("transferSouth", this.transferSouth);
      nbt.putBoolean("transferWest", this.transferWest);
      nbt.putBoolean("transferEast", this.transferEast);
   }

   public void load(@Nonnull CompoundTag nbt) {
      super.load(nbt);
      if (nbt.contains("energy", 4)) {
         this.energy = Math.min(Long.MAX_VALUE, EnergyGenTool.suit(nbt.getLong("energy")));
      }

      if (nbt.contains("wirelessOn", 1)) {
         this.wirelessOn = nbt.getBoolean("wirelessOn");
      }

      if (nbt.contains("wirelessInterval", 3)) {
         this.wirelessInterval = Math.max(1, nbt.getInt("wirelessInterval"));
      }

      if (nbt.contains("wirelessRange", 3)) {
         this.wirelessRange = Math.max(1, nbt.getInt("wirelessRange"));
      }

      if (nbt.contains("transferRepeat", 3)) {
         this.transferRepeat = Math.max(1, nbt.getInt("transferRepeat"));
      }

      if (nbt.contains("transferDown", 1)) {
         this.transferDown = nbt.getBoolean("transferDown");
      }

      if (nbt.contains("transferUp", 1)) {
         this.transferUp = nbt.getBoolean("transferUp");
      }

      if (nbt.contains("transferNorth", 1)) {
         this.transferNorth = nbt.getBoolean("transferNorth");
      }

      if (nbt.contains("transferSouth", 1)) {
         this.transferSouth = nbt.getBoolean("transferSouth");
      }

      if (nbt.contains("transferWest", 1)) {
         this.transferWest = nbt.getBoolean("transferWest");
      }

      if (nbt.contains("transferEast", 1)) {
         this.transferEast = nbt.getBoolean("transferEast");
      }
   }
}
