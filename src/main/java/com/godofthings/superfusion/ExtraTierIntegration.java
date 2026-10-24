package com.godofthings.superfusion;

import mekanism.api.Upgrade;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 额外等级兼容层（合并移植自 Mekanism-Card 的 {@code ExtrasIntegration} 与 {@code EvolvedIntegration}）。
 * <p>
 * 融合卡的等级安装/全量模式不只升到原版终极，还会继续往第三方模组注入的等级升：
 * <ul>
 *   <li><b>Mekanism Extras</b>（{@code mekanism_extras}）：走独立的 {@code ExtraAttributeUpgradeable}，
 *       等级为 absolute → supreme → cosmic → infinite；</li>
 *   <li><b>EvolvedMekanism</b>（{@code evolvedmekanism}）：通过 Mixin 往 Mekanism 的 {@code BaseTier}
 *       注入 overclocked → quantum → dense → multiversal → creative，复用原生 {@code AttributeUpgradeable}。</li>
 * </ul>
 * 两者都用反射访问，因此编译期不需要它们的 jar，运行期没装就整体降级为「只升到原版终极」。
 */
public final class ExtraTierIntegration
{
    private static final Logger LOGGER = LoggerFactory.getLogger("godofthings/superfusion");

    // ===== Mekanism Extras =====
    private static final String EXTRAS_MOD_ID = "mekanism_extras";
    public static final String TIER_ABSOLUTE = "absolute";
    public static final String TIER_SUPREME = "supreme";
    public static final String TIER_COSMIC = "cosmic";
    public static final String TIER_INFINITE = "infinite";

    // ===== EvolvedMekanism =====
    private static final String EVOLVED_MOD_ID = "evolvedmekanism";
    public static final String TIER_OVERCLOCKED = "overclocked";
    public static final String TIER_QUANTUM = "quantum";
    public static final String TIER_DENSE = "dense";
    public static final String TIER_MULTIVERSAL = "multiversal";
    public static final String TIER_CREATIVE = "creative";

    private static final List<String> EM_TIER_ORDER = List.of(
            TIER_OVERCLOCKED, TIER_QUANTUM, TIER_DENSE, TIER_MULTIVERSAL, TIER_CREATIVE);

    private static volatile boolean extrasInitialized = false;
    private static volatile boolean extrasAvailable = false;
    private static volatile boolean evolvedInitialized = false;
    private static volatile boolean evolvedAvailable = false;

    // Extras 反射缓存
    private static Class<?> advancedTierClass;
    private static Method getAdvancedTierMethod;
    private static Method extraAttributeGetUpgradeableMethod;
    private static Method extraAttributeUpgradeResultMethod;
    private static final Map<String, Object> ADVANCED_TIER_VALUES = new HashMap<>();
    private static final Map<String, Item> EXTRAS_INSTALLER_ITEMS = new HashMap<>();
    private static final Map<Upgrade, Item> EXTRA_UPGRADE_ITEMS = new HashMap<>();

    // Evolved 反射缓存
    private static Class<?> emBaseTierClass;
    private static Class<?> emItemsClass;
    private static Class<?> emUpgradesClass;
    private static final Map<String, BaseTier> EM_TIERS = new LinkedHashMap<>();
    private static final Map<String, Item> EM_INSTALLER_ITEMS = new LinkedHashMap<>();
    private static final Map<Upgrade, Item> EM_UPGRADE_ITEMS = new LinkedHashMap<>();

    private ExtraTierIntegration()
    {
    }

    // ===================== Mekanism Extras =====================

    public static boolean isExtrasLoaded()
    {
        if (!ModList.get().isLoaded(EXTRAS_MOD_ID))
        {
            return false;
        }
        if (!extrasInitialized)
        {
            synchronized (ExtraTierIntegration.class)
            {
                if (!extrasInitialized)
                {
                    tryInitExtras();
                }
            }
        }
        return extrasAvailable;
    }

    private static void tryInitExtras()
    {
        extrasInitialized = true;
        try
        {
            advancedTierClass = Class.forName("com.jerry.mekextras.api.tier.AdvancedTier");
            Class<?> extraAttributeClass = Class.forName("com.jerry.mekextras.common.block.attribute.ExtraAttribute");
            Class<?> extraAttributeUpgradeableClass =
                    Class.forName("com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeable");
            Class<?> extraItemsClass = Class.forName("com.jerry.mekextras.common.registries.ExtraItems");
            Class<?> extraUpgradeClass = Class.forName("com.jerry.mekextras.api.ExtraUpgrade");

            Object[] tiers = advancedTierClass.getEnumConstants();
            Method getLowerName = advancedTierClass.getMethod("getLowerName");
            for (Object tier : tiers)
            {
                String lower = (String) getLowerName.invoke(tier);
                ADVANCED_TIER_VALUES.put(lower, tier);
            }

            getAdvancedTierMethod = extraAttributeClass.getMethod("getAdvancedTier", Holder.class);

            Class<?> attributeClass = Class.forName("mekanism.common.block.attribute.Attribute");
            extraAttributeGetUpgradeableMethod = attributeClass.getMethod("get", Holder.class, Class.class);

            extraAttributeUpgradeResultMethod = extraAttributeUpgradeableClass.getMethod(
                    "upgradeResult", BlockState.class, advancedTierClass);

            cacheExtrasInstaller(TIER_ABSOLUTE, extraItemsClass, "ABSOLUTE_TIER_INSTALLER");
            cacheExtrasInstaller(TIER_SUPREME, extraItemsClass, "SUPREME_TIER_INSTALLER");
            cacheExtrasInstaller(TIER_COSMIC, extraItemsClass, "COSMIC_TIER_INSTALLER");
            cacheExtrasInstaller(TIER_INFINITE, extraItemsClass, "INFINITE_TIER_INSTALLER");

            cacheExtrasUpgrade("STACK", "STACK", extraUpgradeClass, extraItemsClass);
            cacheExtrasUpgrade("IONIC_MEMBRANE", "IONIC_MEMBRANE", extraUpgradeClass, extraItemsClass);
            cacheExtrasUpgrade("CREATIVE", "CREATIVE", extraUpgradeClass, extraItemsClass);

            extrasAvailable = true;
            LOGGER.info("[God of Things] Mekanism Extras integration enabled for super fusion card");
        }
        catch (Exception e)
        {
            extrasAvailable = false;
            LOGGER.warn("[God of Things] Failed to initialize Mekanism Extras integration", e);
        }
    }

    private static void cacheExtrasInstaller(String tierName, Class<?> itemsClass, String fieldName)
    {
        Item item = readRegistryObjectField(itemsClass, fieldName);
        if (item != null)
        {
            EXTRAS_INSTALLER_ITEMS.put(tierName, item);
        }
    }

    private static void cacheExtrasUpgrade(String upgradeFieldName, String itemFieldName,
                                           Class<?> upgradesClass, Class<?> itemsClass)
    {
        try
        {
            Field upgradeField = upgradesClass.getField(upgradeFieldName);
            Upgrade upgrade = (Upgrade) upgradeField.get(null);
            Item item = readRegistryObjectField(itemsClass, itemFieldName);
            if (upgrade != null && item != null)
            {
                EXTRA_UPGRADE_ITEMS.put(upgrade, item);
            }
        }
        catch (Exception e)
        {
            LOGGER.debug("[God of Things] Failed to cache Extras upgrade item for {}", upgradeFieldName, e);
        }
    }

    /** 反射读「静态字段 → RegistryObject → get() → Item」这一模式（Extras 与 Evolved 共用） */
    @Nullable
    private static Item readRegistryObjectField(Class<?> holderClass, String fieldName)
    {
        try
        {
            Field field = holderClass.getField(fieldName);
            Object registryObj = field.get(null);
            Method getMethod = registryObj.getClass().getMethod("get");
            return (Item) getMethod.invoke(registryObj);
        }
        catch (Exception e)
        {
            LOGGER.debug("[God of Things] Failed to read registry field {}", fieldName, e);
            return null;
        }
    }

    // ===================== EvolvedMekanism =====================

    public static boolean isEvolvedLoaded()
    {
        if (!ModList.get().isLoaded(EVOLVED_MOD_ID))
        {
            return false;
        }
        if (!evolvedInitialized)
        {
            synchronized (ExtraTierIntegration.class)
            {
                if (!evolvedInitialized)
                {
                    tryInitEvolved();
                }
            }
        }
        return evolvedAvailable;
    }

    private static void tryInitEvolved()
    {
        evolvedInitialized = true;
        try
        {
            emBaseTierClass = Class.forName("fr.iglee42.evolvedmekanism.tiers.EMBaseTier");
            emItemsClass = Class.forName("fr.iglee42.evolvedmekanism.registries.EMItems");
            emUpgradesClass = Class.forName("fr.iglee42.evolvedmekanism.registries.EMUpgrades");

            cacheEMTier(TIER_OVERCLOCKED, "OVERCLOCKED");
            cacheEMTier(TIER_QUANTUM, "QUANTUM");
            cacheEMTier(TIER_DENSE, "DENSE");
            cacheEMTier(TIER_MULTIVERSAL, "MULTIVERSAL");

            cacheEMInstaller(TIER_OVERCLOCKED, "OVERCLOCKED_TIER_INSTALLER");
            cacheEMInstaller(TIER_QUANTUM, "QUANTUM_TIER_INSTALLER");
            cacheEMInstaller(TIER_DENSE, "DENSE_TIER_INSTALLER");
            cacheEMInstaller(TIER_MULTIVERSAL, "MULTIVERSAL_TIER_INSTALLER");
            cacheEMInstaller(TIER_CREATIVE, "CREATIVE_TIER_INSTALLER");

            try
            {
                Field upgradeField = emUpgradesClass.getField("RADIOACTIVE_UPGRADE");
                Upgrade upgrade = (Upgrade) upgradeField.get(null);
                Item item = readRegistryObjectField(emItemsClass, "RADIOACTIVE_UPGRADE");
                if (upgrade != null && item != null)
                {
                    EM_UPGRADE_ITEMS.put(upgrade, item);
                }
            }
            catch (Exception e)
            {
                LOGGER.debug("[God of Things] Failed to cache EM radioactive upgrade", e);
            }

            evolvedAvailable = true;
            LOGGER.info("[God of Things] EvolvedMekanism integration enabled for super fusion card");
        }
        catch (Exception e)
        {
            evolvedAvailable = false;
            LOGGER.warn("[God of Things] Failed to initialize EvolvedMekanism integration", e);
        }
    }

    private static void cacheEMTier(String tierName, String fieldName)
    {
        try
        {
            Field field = emBaseTierClass.getField(fieldName);
            BaseTier tier = (BaseTier) field.get(null);
            if (tier != null)
            {
                EM_TIERS.put(tierName, tier);
            }
        }
        catch (Exception e)
        {
            LOGGER.debug("[God of Things] Failed to cache EM tier {}", tierName, e);
        }
    }

    private static void cacheEMInstaller(String tierName, String fieldName)
    {
        Item item = readRegistryObjectField(emItemsClass, fieldName);
        if (item != null)
        {
            EM_INSTALLER_ITEMS.put(tierName, item);
        }
    }

    // ===================== 对外统一接口 =====================

    /** 方块当前的 Extras 等级名（lower case）；不是 Extras 方块返回 null */
    @Nullable
    public static String getAdvancedTierName(Holder<Block> blockHolder)
    {
        if (!isExtrasLoaded())
        {
            return null;
        }
        try
        {
            Object tier = getAdvancedTierMethod.invoke(null, blockHolder);
            if (tier == null)
            {
                return null;
            }
            Method getLowerName = advancedTierClass.getMethod("getLowerName");
            return (String) getLowerName.invoke(tier);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** 方块的 Extras 可升级属性实例；不是 Extras 可升级方块返回 null */
    @Nullable
    public static Object getExtraAttributeUpgradeable(Holder<Block> blockHolder)
    {
        if (!isExtrasLoaded())
        {
            return null;
        }
        try
        {
            Class<?> upgradeableClass =
                    Class.forName("com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeable");
            return extraAttributeGetUpgradeableMethod.invoke(null, blockHolder, upgradeableClass);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** 调用 Extras 的 upgradeResult 得到目标等级的 BlockState */
    @Nullable
    public static BlockState upgradeResult(Object upgradeable, BlockState current, String targetTierName)
    {
        if (!isExtrasLoaded() || upgradeable == null)
        {
            return null;
        }
        Object targetTier = ADVANCED_TIER_VALUES.get(targetTierName);
        if (targetTier == null)
        {
            return null;
        }
        try
        {
            return (BlockState) extraAttributeUpgradeResultMethod.invoke(upgradeable, current, targetTier);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** Extras 等级对应的安装器物品 */
    @Nullable
    public static Item getTierInstallerItem(String tierName)
    {
        if (EXTRAS_INSTALLER_ITEMS.containsKey(tierName))
        {
            return EXTRAS_INSTALLER_ITEMS.get(tierName);
        }
        if (isExtrasLoaded())
        {
            return EXTRAS_INSTALLER_ITEMS.get(tierName);
        }
        return isEvolvedLoaded() ? EM_INSTALLER_ITEMS.get(tierName) : null;
    }

    /** Extras 新增的升级（STACK / IONIC_MEMBRANE / CREATIVE）对应物品 */
    @Nullable
    public static Item getExtraUpgradeItem(Upgrade upgrade)
    {
        return isExtrasLoaded() ? EXTRA_UPGRADE_ITEMS.get(upgrade) : null;
    }

    /** 下一个 Extras 等级名 */
    @Nullable
    public static String getNextTierName(String currentTierName)
    {
        return switch (currentTierName)
        {
            case TIER_ABSOLUTE -> TIER_SUPREME;
            case TIER_SUPREME -> TIER_COSMIC;
            case TIER_COSMIC -> TIER_INFINITE;
            default -> null;
        };
    }

    /** Extras 等级序号（越大越高），未知返回 Integer.MIN_VALUE */
    public static int getTierIndex(@Nullable String tierName)
    {
        if (tierName == null)
        {
            return -1;
        }
        return switch (tierName)
        {
            case TIER_ABSOLUTE -> 0;
            case TIER_SUPREME -> 1;
            case TIER_COSMIC -> 2;
            case TIER_INFINITE -> 3;
            default -> Integer.MIN_VALUE;
        };
    }

    /** 从当前 Extras 等级升到目标等级所需的安装器链 */
    public static List<Item> getRequiredInstallersToTier(@Nullable String currentTierName, String desiredTierName)
    {
        int currentIndex = getTierIndex(currentTierName);
        int desiredIndex = getTierIndex(desiredTierName);
        if (currentIndex == Integer.MIN_VALUE || desiredIndex == Integer.MIN_VALUE || currentIndex >= desiredIndex)
        {
            return List.of();
        }
        List<Item> installers = new ArrayList<>();
        for (int index = currentIndex + 1; index <= desiredIndex; index++)
        {
            String tierName = switch (index)
            {
                case 0 -> TIER_ABSOLUTE;
                case 1 -> TIER_SUPREME;
                case 2 -> TIER_COSMIC;
                case 3 -> TIER_INFINITE;
                default -> null;
            };
            Item installer = tierName == null ? null : getTierInstallerItem(tierName);
            if (installer == null)
            {
                return List.of();
            }
            installers.add(installer);
        }
        return installers;
    }

    /** 下一个 EvolvedMekanism 等级的 BaseTier */
    @Nullable
    public static BaseTier getNextTier(BaseTier current)
    {
        if (!isEvolvedLoaded())
        {
            return null;
        }
        if (current == BaseTier.ULTIMATE)
        {
            return EM_TIERS.get(TIER_OVERCLOCKED);
        }
        if (current == BaseTier.CREATIVE)
        {
            return null;
        }
        String name = getTierLowerName(current);
        if (name == null)
        {
            return null;
        }
        int idx = EM_TIER_ORDER.indexOf(name);
        if (idx < 0 || idx >= EM_TIER_ORDER.size() - 1)
        {
            return null;
        }
        return getEMTier(EM_TIER_ORDER.get(idx + 1));
    }

    /** EvolvedMekanism 等级名（lower case） */
    @Nullable
    public static String getTierLowerName(BaseTier tier)
    {
        if (!isEvolvedLoaded())
        {
            return null;
        }
        for (Map.Entry<String, BaseTier> entry : EM_TIERS.entrySet())
        {
            if (entry.getValue() == tier)
            {
                return entry.getKey();
            }
        }
        return tier == BaseTier.CREATIVE ? TIER_CREATIVE : null;
    }

    @Nullable
    private static BaseTier getEMTier(String name)
    {
        return TIER_CREATIVE.equals(name) ? BaseTier.CREATIVE : EM_TIERS.get(name);
    }

    /** EvolvedMekanism 新增的升级（RADIOACTIVE）对应物品 */
    @Nullable
    public static Item getEMUpgradeItem(Upgrade upgrade)
    {
        return isEvolvedLoaded() ? EM_UPGRADE_ITEMS.get(upgrade) : null;
    }

    /**
     * 统一的「按等级名取安装器」入口：先查 Extras，再查 EvolvedMekanism。
     * <p>
     * 上游把这两件事分散在两个类里（{@code ExtrasIntegration.getTierInstallerItem} /
     * {@code EvolvedIntegration.getTierInstallerItem}），调用点也是分开的。这里合并成一个，
     * 让 {@link TierInstallLogic} 不必关心目标等级来自哪个模组。
     */
    @Nullable
    public static Item resolveTierInstaller(String tierName)
    {
        Item extras = isExtrasLoaded() ? EXTRAS_INSTALLER_ITEMS.get(tierName) : null;
        if (extras != null)
        {
            return extras;
        }
        return isEvolvedLoaded() ? EM_INSTALLER_ITEMS.get(tierName) : null;
    }

    /** 任一额外等级模组是否可用（用于工具提示提示） */
    public static boolean isAnyLoaded()
    {
        return isExtrasLoaded() || isEvolvedLoaded();
    }

    /** 方块是否支持 EvolvedMekanism 的继续升级（供提示用） */
    public static boolean supportsEvolvedMekanism(Holder<Block> blockHolder, BlockState state)
    {
        if (!isEvolvedLoaded())
        {
            return false;
        }
        AttributeUpgradeable upgradeable = Attribute.get(blockHolder, AttributeUpgradeable.class);
        BaseTier overclocked = EM_TIERS.get(TIER_OVERCLOCKED);
        if (upgradeable == null || overclocked == null)
        {
            return false;
        }
        try
        {
            return upgradeable.upgradeResult(state, overclocked) != state;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
