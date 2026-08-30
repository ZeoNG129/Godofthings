package com.direwolf20.justdirethings.common.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

public class JustDireFluidTank extends FluidTank {
	public JustDireFluidTank(int capacity) {
		super(capacity);
	}

	public JustDireFluidTank(int capacity, Predicate<FluidStack> validator) {
		super(capacity, validator);
	}

	public CompoundTag serializeToNBT() {
		return writeToNBT(new CompoundTag());
	}

	public void deserializeFromNBT(CompoundTag nbt) {
		readFromNBT(nbt);
	}
}
