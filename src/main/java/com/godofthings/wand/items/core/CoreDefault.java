package com.godofthings.wand.items.core;

import net.minecraft.resources.ResourceLocation;
import com.godofthings.wand.ConstructionWand;
import com.godofthings.wand.api.IWandAction;
import com.godofthings.wand.api.IWandCore;
import com.godofthings.wand.wand.action.ActionConstruction;

public class CoreDefault implements IWandCore
{
    @Override
    public int getColor() {
        return -1;
    }

    @Override
    public IWandAction getWandAction() {
        return new ActionConstruction();
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ConstructionWand.loc("default");
    }
}
