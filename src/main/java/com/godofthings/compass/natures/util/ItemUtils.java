package com.godofthings.compass.natures.util;

import com.godofthings.compass.natures.NaturesCompassData;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 找「玩家手上的自然罗盘」（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 */
public final class ItemUtils
{
    private ItemUtils()
    {
    }

    /** 主手优先、其次副手；都没有就返回空栈 */
    public static ItemStack getHeldNatureCompass(Player player)
    {
        return getHeldItem(player, NaturesCompassData::isCompass);
    }

    public static ItemStack getHeldItem(Player player, Item item)
    {
        return getHeldItem(player, stack -> stack.getItem() == item);
    }

    /**
     * 主手优先、其次副手；都不匹配就返回 {@link ItemStack#EMPTY}。
     *
     * <p>上游这里是直接比 {@code Item} 实例的；本模组没有「一个物品实例被多处注册」的情况，
     * 但为了方便复用改成谓词版本（{@link #getHeldNatureCompass} 走的就是它）。</p>
     */
    public static ItemStack getHeldItem(Player player, java.util.function.Predicate<ItemStack> predicate)
    {
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && predicate.test(main))
        {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && predicate.test(off))
        {
            return off;
        }
        return ItemStack.EMPTY;
    }
}
