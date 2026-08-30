package com.direwolf20.justdirethings.common.containers.basecontainers;

import com.direwolf20.justdirethings.common.blockentities.basebe.BaseMachineBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.FilterableBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.FluidMachineBE;
import com.direwolf20.justdirethings.common.containers.handlers.FilterBasicHandler;
import com.direwolf20.justdirethings.common.containers.slots.FilterBasicSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMachineContainer extends BaseContainer {
	public int FILTER_SLOTS = 0;
	public int MACHINE_SLOTS = 0;
	public BaseMachineBE baseMachineBE;
	public FilterBasicHandler filterHandler;
	public ItemStackHandler machineHandler;
	protected Player player;
	protected BlockPos pos;
	public ContainerData fluidData;

	public BaseMachineContainer(@Nullable MenuType<?> menuType, int windowId, Inventory playerInventory,
			BlockPos blockPos) {
		super(menuType, windowId);
		this.pos = blockPos;
		this.player = playerInventory.player;
		BlockEntity blockEntity = player.level().getBlockEntity(pos);
		if (blockEntity instanceof BaseMachineBE baseMachineBE) {
			this.baseMachineBE = baseMachineBE;
			this.MACHINE_SLOTS = baseMachineBE.MACHINE_SLOTS;
		}
		if (MACHINE_SLOTS > 0)
			addMachineSlots();
		if (blockEntity instanceof FilterableBE filterableBE) {
			filterHandler = filterableBE.getFilterHandler();
			FILTER_SLOTS = filterHandler.getSlots();
			addFilterSlots();
		}
		if (blockEntity instanceof FluidMachineBE fluidMachineBE) {
			fluidData = fluidMachineBE.getFluidContainerData();
			addDataSlots(fluidData);
		}
	}

	// Override this if you want the slot layout to be different...
	public void addFilterSlots() {
		addFilterSlots(filterHandler, 0, 8, 54, FILTER_SLOTS, 18);
	}

	// Override this if you want the slot layout to be different...
	public void addMachineSlots() {
		machineHandler = baseMachineBE.getMachineHandler();
		addSlotRange(machineHandler, 0, 80, 35, MACHINE_SLOTS, 18);
	}

	public int getFluidAmount() {
		if (fluidData == null)
			return 0;
		return ((fluidData.get(2) << 16) | fluidData.get(1));
	}

	public FluidStack getFluidStack() {
		if (fluidData == null)
			return FluidStack.EMPTY;
		return new FluidStack(BuiltInRegistries.FLUID.byId(fluidData.get(0)), getFluidAmount());
	}

	@Override
	public void broadcastChanges() {
		// If the block entity at our position was replaced (e.g. by AE2 annihilation +
		// formation planes), close this stale container before any slot interaction can
		// duplicate items by draining the old BE while the new BE still holds the NBT.
		if (baseMachineBE != null && player.level().getBlockEntity(pos) != baseMachineBE) {
			player.closeContainer();
			return;
		}
		super.broadcastChanges();
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		if (baseMachineBE != null && player.level().getBlockEntity(pos) != baseMachineBE)
			return;
		if (baseMachineBE instanceof FilterableBE && slotId >= MACHINE_SLOTS && slotId < MACHINE_SLOTS + FILTER_SLOTS)
			return;
		super.clicked(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		if (MACHINE_SLOTS > 0) {
			Slot slot = this.slots.get(index);
			if (slot.hasItem()) {
				ItemStack currentStack = slot.getItem();
				if (index < MACHINE_SLOTS) { // Machine Slots to Player Inventory
					if (!this.moveItemStackTo(currentStack, MACHINE_SLOTS + FILTER_SLOTS,
							MACHINE_SLOTS + FILTER_SLOTS + Inventory.INVENTORY_SIZE, true)) {
						return ItemStack.EMPTY;
					}
				}
				if (index >= MACHINE_SLOTS + FILTER_SLOTS) { // Player Inventory to Machine Slots
					if (!this.moveItemStackTo(currentStack, 0, MACHINE_SLOTS, false)) {
						// No-Op, pass along to Filter Handler below
					}
				}

				if (currentStack.isEmpty()) {
					slot.set(ItemStack.EMPTY);
				} else {
					slot.setChanged();
				}

				if (currentStack.getCount() == itemstack.getCount()) {
					return ItemStack.EMPTY;
				}

				slot.onTake(playerIn, currentStack);
			}
		}
		if (baseMachineBE instanceof FilterableBE) {
			Slot slot = this.slots.get(index);
			if (slot.hasItem()) {
				if (index >= MACHINE_SLOTS + FILTER_SLOTS) { // Only do this if we click from the players inventory
					ItemStack currentStack = slot.getItem().copy();
					currentStack.setCount(1);
					ItemStack result = quickMoveBasicFilter(currentStack, MACHINE_SLOTS, FILTER_SLOTS);
					if (!result.isEmpty())
						baseMachineBE.setChanged(); // clears filterCache and marks chunk dirty
					return result;
				}
			}
		}
		return itemstack;
	}

	protected int addFilterSlots(IItemHandler handler, int index, int x, int y, int amount, int dx) {
		for (int i = 0; i < amount; i++) {
			if (handler instanceof FilterBasicHandler) // This should always be true, but can't hurt to check!
				addSlot(new FilterBasicSlot(handler, index, x, y));
			else
				addSlot(new SlotItemHandler(handler, index, x, y));
			x += dx;
			index++;
		}
		return index;
	}

}
