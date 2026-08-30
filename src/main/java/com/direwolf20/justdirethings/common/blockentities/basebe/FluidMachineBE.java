package com.direwolf20.justdirethings.common.blockentities.basebe;

import com.direwolf20.justdirethings.common.capabilities.JustDireFluidTank;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public interface FluidMachineBE {
	default int getMaxMB() {
		return 8000;
	}

	ContainerData getFluidContainerData();

	JustDireFluidTank getFluidTank();

	default FluidStack getFluidStack() {
		return getFluidTank().getFluid();
	}

	default void setFluidStack(Fluid fluid, int amt) {
		getFluidTank().setFluid(new FluidStack(fluid, amt));
	}

	default int getAmountStored() {
		return getFluidTank().getFluidAmount();
	}

	default void setAmountStored(int value) {
		FluidStack fluid = getFluidTank().getFluid();
		if (!fluid.isEmpty())
			fluid.setAmount(value);
	}

	default boolean isFull() {
		return getFluidStack().getAmount() >= getMaxMB();
	}
}
