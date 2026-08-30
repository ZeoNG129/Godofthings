package com.godofthings.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/**
 * 神之护甲（神之头/神之甲/神之腿/神之鞋）。
 * 无法破坏，效果由 {@link com.godofthings.handler.GodArmorHandler} 处理。
 */
public class GodArmorItem extends ArmorItem
{
    public GodArmorItem(Type type, Properties properties)
    {
        super(ArmorMaterials.NETHERITE, type, properties.setNoRepair());
    }

    /** 无法破坏：永不消耗耐久 */
    @Override
    public boolean isDamageable(ItemStack stack)
    {
        return false;
    }
}
