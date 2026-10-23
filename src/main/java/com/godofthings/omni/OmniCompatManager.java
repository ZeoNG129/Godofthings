package com.godofthings.omni;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 适配器注册表与按物品缓存（原样移植自 Omni Upgrades 的 {@code CompatManager}）。 */
public class OmniCompatManager {

    private static final List<UpgradeCompat> COMPATS = new ArrayList<>();
    private static final Map<Item, UpgradeCompat> COMPAT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Item, Boolean> INVALID_CACHE = new ConcurrentHashMap<>();

    static {
        // 与测试整合包对齐：AE2 系（覆盖 AE2 / AE2 Lightning Tech）+ 通用机械 + 工业先锋 + 气动工艺。
        // 每个适配器内部用 ModList.isLoaded 守护，对应模组不在就自动失效。
        COMPATS.add(new OmniAe2Compat());
        COMPATS.add(new OmniMekanismCompat());
        COMPATS.add(new OmniIndustrialForegoingCompat());
        COMPATS.add(new OmniPneumaticCraftCompat());
    }

    public static UpgradeCompat getCompatFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (INVALID_CACHE.containsKey(item)) {
            return null;
        }
        UpgradeCompat cached = COMPAT_CACHE.get(item);
        if (cached != null) {
            return cached;
        }
        for (UpgradeCompat compat : COMPATS) {
            if (compat.isUpgradeItem(stack)) {
                COMPAT_CACHE.put(item, compat);
                return compat;
            }
        }
        INVALID_CACHE.put(item, Boolean.TRUE);
        return null;
    }

    private OmniCompatManager() {
    }
}
