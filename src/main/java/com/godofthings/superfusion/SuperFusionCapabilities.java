package com.godofthings.superfusion;

import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 超级融合卡的物品能量能力（移植自 Mekanism-Card 的 ModCapabilities）。
 * <p>
 * 上游给「等级安装器」和「超级融合卡」两个物品都挂了同一份处理器；本模组只移植融合卡，
 * 因此只注册这一件。同时注册 Mekanism 的 STRICT_ENERGY 与 NeoForge 的 EnergyStorage，
 * 前者用于卡自己扣电，后者让其它模组的充电器（如能量立方、充电台）能给卡充电。
 * <p>
 * 该注册由 {@code SuperFusionRegistry.registerCapabilities} 在 Mekanism 存在时才调用。
 */
public final class SuperFusionCapabilities
{
    private static final long MAX_ENERGY = 200_000L;

    /** 卡内储能上限（供物品能量条与界面读取） */
    public static long maxEnergy()
    {
        return MAX_ENERGY;
    }

    private SuperFusionCapabilities()
    {
    }

    public static void register(RegisterCapabilitiesEvent event)
    {
        event.registerItem(Capabilities.STRICT_ENERGY.item(), (stack, ctx) -> new CardEnergyHandler(stack),
                SuperFusionRegistry.requireItem().get());
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM,
                (stack, ctx) -> new CardEnergyHandler(stack),
                SuperFusionRegistry.requireItem().get());
    }

    /** 把卡内的 int 组件包装成两种能量接口（读写都落在数据组件上，因此会随物品保存） */
    public static final class CardEnergyHandler implements IStrictEnergyHandler, IEnergyStorage
    {
        private final ItemStack stack;

        public CardEnergyHandler(ItemStack stack)
        {
            this.stack = stack;
        }

        @Override
        public int getEnergyContainerCount()
        {
            return 1;
        }

        @Override
        public long getEnergy(int container)
        {
            return stack.getOrDefault(SuperFusionComponents.ENERGY.get(), 0);
        }

        @Override
        public void setEnergy(int container, long energy)
        {
            stack.set(SuperFusionComponents.ENERGY.get(), (int) Math.min(energy, MAX_ENERGY));
        }

        @Override
        public long getMaxEnergy(int container)
        {
            return MAX_ENERGY;
        }

        @Override
        public long getNeededEnergy(int container)
        {
            return MAX_ENERGY - getEnergy(container);
        }

        @Override
        public long insertEnergy(int container, long amount, Action action)
        {
            long current = getEnergy(container);
            long toInsert = Math.min(amount, MAX_ENERGY - current);
            if (toInsert <= 0)
            {
                return amount;
            }
            if (action.execute())
            {
                setEnergy(container, current + toInsert);
            }
            return amount - toInsert;
        }

        @Override
        public long extractEnergy(int container, long amount, Action action)
        {
            long current = getEnergy(container);
            long toExtract = Math.min(amount, current);
            if (toExtract <= 0)
            {
                return 0;
            }
            if (action.execute())
            {
                setEnergy(container, current - toExtract);
            }
            return toExtract;
        }

        @Override
        public long insertEnergy(long amount, Action action)
        {
            return insertEnergy(0, amount, action);
        }

        @Override
        public long extractEnergy(long amount, Action action)
        {
            return extractEnergy(0, amount, action);
        }

        @Override
        public int getMaxEnergyStored()
        {
            return (int) MAX_ENERGY;
        }

        @Override
        public int getEnergyStored()
        {
            return (int) getEnergy(0);
        }

        @Override
        public boolean canExtract()
        {
            return true;
        }

        @Override
        public boolean canReceive()
        {
            return true;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate)
        {
            if (simulate)
            {
                return (int) Math.min(maxExtract, getEnergy(0));
            }
            return (int) extractEnergy(0, maxExtract, Action.EXECUTE);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate)
        {
            long current = getEnergy(0);
            long toReceive = Math.min(maxReceive, MAX_ENERGY - current);
            if (toReceive <= 0)
            {
                return 0;
            }
            if (!simulate)
            {
                setEnergy(0, current + toReceive);
            }
            return (int) toReceive;
        }
    }
}
