package com.godofthings.wand.items.core;

import net.minecraft.resources.ResourceLocation;
import com.godofthings.wand.ConstructionWand;
import com.godofthings.wand.api.IWandAction;
import com.godofthings.wand.wand.action.ActionDestruction;

public class ItemCoreDestruction extends ItemCore
{
    public ItemCoreDestruction(Properties properties) {
        super(properties);
    }

    @Override
    public int getColor() {
        return 0xFF0000;
    }

    @Override
    public IWandAction getWandAction() {
        return new ActionDestruction();
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ConstructionWand.loc("core_destruction");
    }
}
