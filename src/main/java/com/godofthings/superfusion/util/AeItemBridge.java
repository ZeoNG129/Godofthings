package com.godofthings.superfusion.util;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AE2 桥接（移植自 Mekanism-Card 的 {@code AE2Integration}）。
 * <p>
 * 融合卡可以「潜行右键无线访问点」把自己绑到 ME 网络，之后装升级/装安装器时材料就能直接从网络里扣。
 * 绑定点存在物品的 {@code ae2:wireless_link_target} 组件里。
 * <p>
 * 本类只有 AE2 存在时才会被 {@link NetworkItemSource} 通过反射调用；AE2 是本模组的硬前置，
 * 因此这里可以放心直接引用 AE2 类。
 */
public final class AeItemBridge
{
    private static final Logger LOGGER = LoggerFactory.getLogger("godofthings/superfusion");

    private AeItemBridge()
    {
    }

    /** 取卡绑定的 ME 网络存储；没绑或访问点失效返回 null */
    @Nullable
    public static Object getBoundAeNetwork(Level world, ItemStack stack)
    {
        if (!(world instanceof ServerLevel serverLevel))
        {
            return null;
        }
        GlobalPos linkedPos = stack.get(AEComponents.WIRELESS_LINK_TARGET);
        if (linkedPos == null)
        {
            return null;
        }
        ServerLevel linkedLevel = serverLevel.getServer().getLevel(linkedPos.dimension());
        if (linkedLevel == null)
        {
            return null;
        }
        BlockEntity be = linkedLevel.getBlockEntity(linkedPos.pos());
        if (!(be instanceof IWirelessAccessPoint accessPoint))
        {
            return null;
        }
        IGrid grid = accessPoint.getGrid();
        if (grid == null || !accessPoint.isActive())
        {
            return null;
        }
        IStorageService storageService = grid.getStorageService();
        return storageService == null ? null : storageService.getInventory();
    }

    /** 网络里某物品的存量 */
    public static long getItemCount(Object aeStorage, Item item)
    {
        if (!(aeStorage instanceof MEStorage storage))
        {
            return 0;
        }
        try
        {
            AEItemKey key = AEItemKey.of(item);
            if (key == null)
            {
                return 0;
            }
            KeyCounter available = storage.getAvailableStacks();
            return available.get(key);
        }
        catch (Exception e)
        {
            LOGGER.error("[God of Things] Failed to read AE2 storage for {}", item, e);
            return 0;
        }
    }

    /** 从网络里抽取指定数量，返回实际抽到的数量 */
    public static long consumeItemCount(Object aeStorage, Item item, int amount)
    {
        if (!(aeStorage instanceof MEStorage storage))
        {
            return 0;
        }
        try
        {
            AEItemKey key = AEItemKey.of(item);
            if (key == null)
            {
                return 0;
            }
            return storage.extract(key, amount, Actionable.MODULATE, IActionSource.empty());
        }
        catch (Exception e)
        {
            LOGGER.error("[God of Things] Failed to extract from AE2 storage for {}", item, e);
            return 0;
        }
    }
}
