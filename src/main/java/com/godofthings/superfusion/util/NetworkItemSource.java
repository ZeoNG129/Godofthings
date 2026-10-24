package com.godofthings.superfusion.util;

import com.godofthings.superfusion.ExtraTierIntegration;
import mekanism.api.Action;
import mekanism.api.Upgrade;
import mekanism.api.inventory.qio.IQIOFrequency;
import mekanism.common.attachments.FrequencyAware;
import mekanism.common.item.interfaces.IUpgradeItem;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.registries.MekanismItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 物品来源聚合（移植自 Mekanism-Card 的 {@code NetworkItemSource}）。
 * <p>
 * 融合卡装升级、装安装器时，材料可以从三个地方取：玩家背包、绑定的 ME 网络、绑定的 QIO 频率。
 * 这个类把三者统一成 {@link #count(Item)} / {@link #consume(Item, int)} 两个口径，
 * 调用方不必关心材料具体在哪。
 * <p>
 * ME 与 QIO 都是软依赖：没装 AE2 就没有 ME 分支，没绑 QIO 频率就没有 QIO 分支。
 */
public final class NetworkItemSource
{
    private final Player player;
    @Nullable
    private final Object aeStorage;
    @Nullable
    private final IQIOFrequency qioFrequency;

    private NetworkItemSource(Player player, @Nullable Object aeStorage, @Nullable IQIOFrequency qioFrequency)
    {
        this.player = player;
        this.aeStorage = aeStorage;
        this.qioFrequency = qioFrequency;
    }

    public static NetworkItemSource create(Level level, Player player, ItemStack toolStack)
    {
        return new NetworkItemSource(player, getBoundAeNetwork(level, toolStack), getBoundQioNetwork(toolStack));
    }

    public boolean hasExternalStorage()
    {
        return aeStorage != null || qioFrequency != null;
    }

    public long count(Item item)
    {
        return countInInventory(item) + countInAe(item) + countInQio(item);
    }

    public boolean has(Item item, int amount)
    {
        return isCreative() || amount <= 0 || count(item) >= amount;
    }

    public boolean hasAll(Map<Item, Integer> required)
    {
        if (isCreative())
        {
            return true;
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet())
        {
            if (!has(entry.getKey(), entry.getValue()))
            {
                return false;
            }
        }
        return true;
    }

    public boolean consume(Item item, int amount)
    {
        if (isCreative() || amount <= 0)
        {
            return true;
        }
        if (!has(item, amount))
        {
            return false;
        }

        int remaining = amount;
        remaining -= consumeFromInventory(item, remaining);
        if (remaining > 0)
        {
            remaining -= safeLongToInt(consumeFromAe(item, remaining));
        }
        if (remaining > 0)
        {
            remaining -= safeLongToInt(consumeFromQio(item, remaining));
        }
        return remaining == 0;
    }

    public boolean consumeAll(Map<Item, Integer> required)
    {
        if (isCreative())
        {
            return true;
        }
        if (!hasAll(required))
        {
            return false;
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet())
        {
            if (!consume(entry.getKey(), entry.getValue()))
            {
                return false;
            }
        }
        return true;
    }

    public int countUpgrade(Upgrade upgrade)
    {
        Item item = getUpgradeItem(upgrade);
        return item == null ? 0 : safeLongToInt(count(item));
    }

    public boolean hasUpgrade(Upgrade upgrade, int amount)
    {
        Item item = getUpgradeItem(upgrade);
        return item != null && has(item, amount);
    }

    public boolean consumeUpgrade(Upgrade upgrade, int amount)
    {
        Item item = getUpgradeItem(upgrade);
        return item != null && consume(item, amount);
    }

    @Nullable
    public Upgrade findFirstUpgrade()
    {
        for (ItemStack stack : player.getInventory().items)
        {
            if (stack.getItem() instanceof IUpgradeItem upgradeItem)
            {
                return upgradeItem.getUpgradeType(stack);
            }
        }
        for (Upgrade upgrade : Upgrade.values())
        {
            Item item = getUpgradeItem(upgrade);
            if (item != null && countInAe(item) > 0)
            {
                return upgrade;
            }
        }
        for (Upgrade upgrade : Upgrade.values())
        {
            Item item = getUpgradeItem(upgrade);
            if (item != null && countInQio(item) > 0)
            {
                return upgrade;
            }
        }
        return null;
    }

    /**
     * 升级类型 → 对应物品。
     * <p>
     * 先查 Mekanism 原生；查不到再问第三方模组（Empowered Core / Extras / EvolvedMekanism），
     * 口径与上游一致。
     */
    @Nullable
    public static Item getUpgradeItem(Upgrade upgrade)
    {
        switch (upgrade)
        {
            case SPEED:
                return MekanismItems.SPEED_UPGRADE.get();
            case ENERGY:
                return MekanismItems.ENERGY_UPGRADE.get();
            case FILTER:
                return MekanismItems.FILTER_UPGRADE.get();
            case CHEMICAL:
                return MekanismItems.CHEMICAL_UPGRADE.get();
            case MUFFLING:
                return MekanismItems.MUFFLING_UPGRADE.get();
            case ANCHOR:
                return MekanismItems.ANCHOR_UPGRADE.get();
            case STONE_GENERATOR:
                return MekanismItems.STONE_GENERATOR_UPGRADE.get();
            default:
                Item empowered = getEmpoweredUpgradeItem(upgrade);
                if (empowered != null)
                {
                    return empowered;
                }
                Item extras = ExtraTierIntegration.getExtraUpgradeItem(upgrade);
                if (extras != null)
                {
                    return extras;
                }
                return ExtraTierIntegration.getEMUpgradeItem(upgrade);
        }
    }

    @Nullable
    private static Item getEmpoweredUpgradeItem(Upgrade upgrade)
    {
        if (!ModList.get().isLoaded("mekanism_empowered_core"))
        {
            return null;
        }
        try
        {
            Class<?> registry = Class.forName(
                    "dev.lapis256.mekanism_empowered.core.common.upgrade.UpgradeItemRegistry");
            Object holder = registry.getMethod("getItem", Upgrade.class).invoke(null, upgrade);
            if (holder instanceof net.minecraft.core.Holder<?> itemHolder
                    && itemHolder.value() instanceof Item item)
            {
                return item;
            }
        }
        catch (ReflectiveOperationException ignored)
        {
        }
        return null;
    }

    private boolean isCreative()
    {
        return player.getAbilities().instabuild;
    }

    private long countInInventory(Item item)
    {
        long count = 0;
        for (ItemStack stack : player.getInventory().items)
        {
            if (stack.getItem() == item)
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int consumeFromInventory(Item item, int amount)
    {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item)
            {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (stack.isEmpty())
                {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        return amount - remaining;
    }

    // ===== ME 网络（反射调用，避免 AE2 未装时类加载失败）=====

    private long countInAe(Item item)
    {
        if (aeStorage == null)
        {
            return 0;
        }
        try
        {
            Class<?> clazz = Class.forName("com.godofthings.superfusion.util.AeItemBridge");
            Method method = clazz.getDeclaredMethod("getItemCount", Object.class, Item.class);
            Object result = method.invoke(null, aeStorage, item);
            return result instanceof Number number ? number.longValue() : 0;
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    private long consumeFromAe(Item item, int amount)
    {
        if (aeStorage == null)
        {
            return 0;
        }
        try
        {
            Class<?> clazz = Class.forName("com.godofthings.superfusion.util.AeItemBridge");
            Method method = clazz.getDeclaredMethod("consumeItemCount", Object.class, Item.class, int.class);
            Object result = method.invoke(null, aeStorage, item, amount);
            return result instanceof Number number ? number.longValue() : 0;
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    @Nullable
    private static Object getBoundAeNetwork(Level level, ItemStack stack)
    {
        if (!ModList.get().isLoaded("ae2"))
        {
            return null;
        }
        try
        {
            Class<?> clazz = Class.forName("com.godofthings.superfusion.util.AeItemBridge");
            Method method = clazz.getDeclaredMethod("getBoundAeNetwork", Level.class, ItemStack.class);
            return method.invoke(null, level, stack);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    // ===== QIO 频率 =====

    @Nullable
    private static IQIOFrequency getBoundQioNetwork(ItemStack stack)
    {
        try
        {
            var freqComponent = MekanismDataComponents.QIO_FREQUENCY.get();
            mekanism.common.attachments.FrequencyAware<mekanism.common.content.qio.QIOFrequency> aware =
                    stack.get(freqComponent);
            if (aware == null)
            {
                return null;
            }
            mekanism.common.content.qio.QIOFrequency freq = aware.getFrequency(stack, freqComponent);
            return freq != null && freq.isValid() ? freq : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private long countInQio(Item item)
    {
        if (qioFrequency == null)
        {
            return 0;
        }
        return qioFrequency.getStored(new ItemStack(item));
    }

    private long consumeFromQio(Item item, int amount)
    {
        if (qioFrequency == null)
        {
            return 0;
        }
        return qioFrequency.massExtract(new ItemStack(item), amount, Action.EXECUTE);
    }

    private static int safeLongToInt(long value)
    {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(value, 0);
    }

    /** 供 QIO 绑定逻辑复用的频率类型（融合卡潜行右键 QIO 仪表盘时用） */
    public static FrequencyType<?> qioFrequencyType()
    {
        return FrequencyType.QIO;
    }
}
