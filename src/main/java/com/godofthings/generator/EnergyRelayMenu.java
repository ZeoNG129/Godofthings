package com.godofthings.generator;

import com.godofthings.Godofthings;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class EnergyRelayMenu extends AbstractContainerMenu {
   public static final int BUTTON_WIRELESS = 0;
   public static final int BUTTON_INTERVAL_DOWN = 1;
   public static final int BUTTON_INTERVAL_UP = 2;
   public static final int BUTTON_RANGE_DOWN = 3;
   public static final int BUTTON_RANGE_UP = 4;
   public static final int BUTTON_REPEAT_DOWN = 5;
   public static final int BUTTON_REPEAT_UP = 6;
   public static final int BUTTON_TRANSFER_DOWN = 7;
   public static final int BUTTON_TRANSFER_UP = 8;
   public static final int BUTTON_TRANSFER_NORTH = 9;
   public static final int BUTTON_TRANSFER_SOUTH = 10;
   public static final int BUTTON_TRANSFER_WEST = 11;
   public static final int BUTTON_TRANSFER_EAST = 12;
   public final EnergyRelayEntity entity;
   private long clientEnergy;
   private boolean clientWirelessOn;
   private int clientInterval;
   private int clientRange;
   private int clientRepeat;
   private boolean clientTransferDown;
   private boolean clientTransferUp;
   private boolean clientTransferNorth;
   private boolean clientTransferSouth;
   private boolean clientTransferWest;
   private boolean clientTransferEast;

   public EnergyRelayMenu(int id, Inventory playerInventory, BlockPos pos) {
      super((MenuType)Godofthings.ENERGY_RELAY_MENU.get(), id);
      this.entity = playerInventory.player.level().getBlockEntity(pos) instanceof EnergyRelayEntity relay ? relay : null;
      this.addDataSlot(makeDataSlot(() -> hiWord(this.energyOrZero()), v -> this.clientEnergy = mergeLong(v, loWord(this.clientEnergy))));
      this.addDataSlot(makeDataSlot(() -> loWord(this.energyOrZero()), v -> this.clientEnergy = mergeLong(hiWord(this.clientEnergy), v)));
      this.addDataSlot(makeDataSlot(() -> this.wirelessOn() ? 1 : 0, v -> this.clientWirelessOn = v != 0));
      this.addDataSlot(makeDataSlot(() -> this.intervalOrZero(), v -> this.clientInterval = v));
      this.addDataSlot(makeDataSlot(() -> EnergyGenTool.normalizeWirelessRange(this.rangeOrZero()), v -> this.clientRange = v));
      this.addDataSlot(makeDataSlot(() -> this.repeatOrZero(), v -> this.clientRepeat = v));
      this.addDataSlot(makeDataSlot(() -> this.face(Direction.DOWN) ? 1 : 0, v -> this.clientTransferDown = v != 0));
      this.addDataSlot(makeDataSlot(() -> this.face(Direction.UP) ? 1 : 0, v -> this.clientTransferUp = v != 0));
      this.addDataSlot(makeDataSlot(() -> this.face(Direction.NORTH) ? 1 : 0, v -> this.clientTransferNorth = v != 0));
      this.addDataSlot(makeDataSlot(() -> this.face(Direction.SOUTH) ? 1 : 0, v -> this.clientTransferSouth = v != 0));
      this.addDataSlot(makeDataSlot(() -> this.face(Direction.WEST) ? 1 : 0, v -> this.clientTransferWest = v != 0));
      this.addDataSlot(makeDataSlot(() -> this.face(Direction.EAST) ? 1 : 0, v -> this.clientTransferEast = v != 0));
   }

   private boolean isServer() {
      return this.entity != null && this.entity.getLevel() != null && !this.entity.getLevel().isClientSide;
   }

   private long energyOrZero() {
      return this.isServer() ? this.entity.energy : 0L;
   }

   private boolean wirelessOn() {
      return this.isServer() ? this.entity.wirelessOn : false;
   }

   private int intervalOrZero() {
      return this.isServer() ? this.entity.wirelessInterval : 0;
   }

   private int rangeOrZero() {
      return this.isServer() ? this.entity.wirelessRange : 0;
   }

   private int repeatOrZero() {
      return this.isServer() ? this.entity.transferRepeat : 0;
   }

   private boolean face(Direction direction) {
      return this.isServer() && this.entity.isTransferEnabled(direction);
   }

   public long getEnergy() {
      return this.isServer() ? this.entity.energy : this.clientEnergy;
   }

   public long getMax() {
      return Long.MAX_VALUE;
   }

   public boolean isWirelessOn() {
      return this.isServer() ? this.entity.wirelessOn : this.clientWirelessOn;
   }

   public int getInterval() {
      return this.isServer() ? this.entity.wirelessInterval : this.clientInterval;
   }

   public int getRange() {
      return this.isServer() ? EnergyGenTool.normalizeWirelessRange(this.entity.wirelessRange) : this.clientRange;
   }

   public int getRepeat() {
      return this.isServer() ? this.entity.transferRepeat : this.clientRepeat;
   }

   public boolean isFaceEnabled(Direction direction) {
      if (this.isServer()) {
         return this.entity.isTransferEnabled(direction);
      } else {
         return switch (direction) {
            case DOWN -> this.clientTransferDown;
            case UP -> this.clientTransferUp;
            case NORTH -> this.clientTransferNorth;
            case SOUTH -> this.clientTransferSouth;
            case WEST -> this.clientTransferWest;
            case EAST -> this.clientTransferEast;
            default -> throw new IncompatibleClassChangeError();
         };
      }
   }

   public boolean clickMenuButton(@Nonnull Player player, int id) {
      if (this.entity != null && !player.level().isClientSide) {
         switch (id) {
            case 0:
               this.entity.wirelessOn = !this.entity.wirelessOn;
               break;
            case 1:
               this.entity.wirelessInterval = Math.max(1, this.entity.wirelessInterval - 1);
               break;
            case 2:
               this.entity.wirelessInterval = Math.min(3600, this.entity.wirelessInterval + 1);
               break;
            case 3:
               this.entity.wirelessRange = stepRange(this.entity.wirelessRange, -1);
               break;
            case 4:
               this.entity.wirelessRange = stepRange(this.entity.wirelessRange, 1);
               break;
            case 5:
               this.entity.transferRepeat = Math.max(1, this.entity.transferRepeat - 1);
               break;
            case 6:
               this.entity.transferRepeat = Math.min(256, this.entity.transferRepeat + 1);
               break;
            case 7:
               this.entity.transferDown = !this.entity.transferDown;
               break;
            case 8:
               this.entity.transferUp = !this.entity.transferUp;
               break;
            case 9:
               this.entity.transferNorth = !this.entity.transferNorth;
               break;
            case 10:
               this.entity.transferSouth = !this.entity.transferSouth;
               break;
            case 11:
               this.entity.transferWest = !this.entity.transferWest;
               break;
            case 12:
               this.entity.transferEast = !this.entity.transferEast;
               break;
            default:
               return false;
         }

         this.entity.setChanged();
         return true;
      } else {
         return false;
      }
   }

   private static int stepRange(int current, int delta) {
      if (delta > 0) {
         return current >= 7 ? 1 : current + 2;
      } else {
         return current <= 1 ? 7 : current - 2;
      }
   }

   public boolean stillValid(@Nonnull Player player) {
      return this.entity == null ? false : this.entity.getLevel() != null && this.entity.getLevel().getBlockEntity(this.entity.getBlockPos()) == this.entity;
   }

   @Nonnull
   public ItemStack quickMoveStack(@Nonnull Player player, int index) {
      return ItemStack.EMPTY;
   }

   private static DataSlot makeDataSlot(final IntSupplier getter, final IntConsumer setter) {
      return new DataSlot() {
         public int get() {
            return getter.getAsInt();
         }

         public void set(int value) {
            setter.accept(value);
         }
      };
   }

   private static int hiWord(long value) {
      return (int)(value >> 32);
   }

   private static int loWord(long value) {
      return (int)(value & 4294967295L);
   }

   private static long mergeLong(int hi, int lo) {
      return (long)hi << 32 | (long)lo & 4294967295L;
   }
}
