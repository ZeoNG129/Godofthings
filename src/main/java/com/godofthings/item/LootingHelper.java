package com.godofthings.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 掠夺（抢劫）附魔的强度换算与应用。
 *
 * <p>这两段逻辑原本在 {@code GodSwordItem} 里（神之剑的「抢劫」功能，由 J 键面板调强度）。
 * v5.1.2 神之剑整体删除后，**神之砍杀**（{@code GodSlaughterBlockEntity}）仍在用它们
 * （按面板上的抢夺强度给模拟玩家手里的剑写掠夺附魔），因此搬到这里独立保留，
 * 行为与原来逐字一致。</p>
 */
public final class LootingHelper {

    private LootingHelper() {}

    /** 抢劫强度 → 掠夺附魔等级（指数增长：每 +30 翻倍，0 关闭，≥240 封顶 255）。 */
    private static int lootingLevel(int power)
    {
        if (power <= 0)
        {
            return 0;
        }
        if (power >= 240)
        {
            return 255;
        }
        return (int) Math.pow(2, power / 30.0);
    }

    /** 按强度 power（0~300）指数映射到掠夺附魔等级，power=0 时移除附魔。 */
    public static void applyLooting(ItemStack stack, ServerLevel level, int power)
    {
        Holder<Enchantment> looting = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.LOOTING);
        int lvl = lootingLevel(power);
        if (lvl > 0)
        {
            stack.enchant(looting, lvl);
        }
        else
        {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(h -> h.is(Enchantments.LOOTING)));
        }
    }
}
