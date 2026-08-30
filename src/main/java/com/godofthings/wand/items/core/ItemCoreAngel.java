package com.godofthings.wand.items.core;

import net.minecraft.resources.ResourceLocation;
import com.godofthings.wand.ConstructionWand;
import com.godofthings.wand.api.IWandAction;
import com.godofthings.wand.wand.action.ActionAngel;

public class ItemCoreAngel extends ItemCore
{
    public ItemCoreAngel(Properties properties) {
        super(properties);
    }

    @Override
    public int getColor() {
        return 0xE9B115;
    }

    @Override
    public IWandAction getWandAction() {
        return new ActionAngel();
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ConstructionWand.loc("core_angel");
    }
}
