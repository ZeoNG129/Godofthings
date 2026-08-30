package com.godofthings.generator;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * 能量发电机的 FE 能力实现：只能输出、不能输入。
 */
public class EnergyConnection implements IEnergyStorage
{
    private final EnergyGeneratorEntity owner;

    public EnergyConnection(EnergyGeneratorEntity owner)
    {
        this.owner = owner;
    }

    @Override
    public int getEnergyStored()
    {
        return EnergyGenTool.suitInt(owner.energy);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    {
        int maxOutput = EnergyGenTool.suitInt(owner.energy);
        if (maxOutput <= 0 || maxExtract <= 0)
        {
            return 0;
        }
        int ret = Math.min(maxOutput, maxExtract);
        if (!simulate)
        {
            owner.energy -= ret;
            owner.setChanged();
        }
        return ret;
    }

    @Override
    public int getMaxEnergyStored()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract()
    {
        return true;
    }

    @Override
    public boolean canReceive()
    {
        return false;
    }
}
