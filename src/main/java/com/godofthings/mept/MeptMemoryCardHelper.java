package com.godofthings.mept;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPart;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.items.tools.MemoryCardItem;
import appeng.items.tools.NetworkToolItem;
import appeng.me.helpers.PlayerSource;
import appeng.parts.AEBasePart;
import appeng.util.SettingsFrom;

import java.util.HashMap;
import java.util.Map;

/**
 * AE2 内存卡设置应用助手（照抄上游 MemoryCardHelper）。
 */
public class MeptMemoryCardHelper
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 玩家副手是否持有已配置的 AE2 内存卡。照抄上游。 */
    public static boolean hasConfiguredMemoryCard(Player player)
    {
        ItemStack offHandStack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offHandStack.isEmpty())
        {
            return false;
        }
        if (!(offHandStack.getItem() instanceof IMemoryCard))
        {
            return false;
        }

        var settingsSource = offHandStack.get(AEComponents.EXPORTED_SETTINGS_SOURCE);
        return settingsSource != null;
    }

    /** 取副手内存卡。照抄上游。 */
    public static ItemStack getOffHandMemoryCard(Player player)
    {
        ItemStack offHandStack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof IMemoryCard)
        {
            return offHandStack;
        }
        return ItemStack.EMPTY;
    }

    /** 统计玩家背包+网络工具里某物品的数量。照抄上游。 */
    private static int countItemInPlayerAndNetworkTool(Player player, Item item)
    {
        int count = player.getInventory().countItem(item);

        var networkTool = NetworkToolItem.findNetworkToolInv(player);
        if (networkTool != null)
        {
            for (var stack : networkTool.getInventory())
            {
                if (stack.getItem() == item)
                {
                    count += stack.getCount();
                }
            }
        }

        return count;
    }

    /** 统计内存卡数据里存了多少样板。照抄上游。 */
    private static int countPatternsInMemoryCard(DataComponentMap components)
    {
        var patterns = components.get(AEComponents.EXPORTED_PATTERNS);
        if (patterns == null || patterns == ItemContainerContents.EMPTY)
        {
            return 0;
        }

        int count = 0;
        for (var stack : patterns.nonEmptyItems())
        {
            if (!stack.isEmpty())
            {
                count++;
            }
        }
        return count;
    }

    /** 取内存卡数据里的升级需求。照抄上游。 */
    private static Map<Item, Integer> getUpgradesInMemoryCard(DataComponentMap components)
    {
        Map<Item, Integer> upgrades = new HashMap<>();

        var exportedUpgrades = components.get(AEComponents.EXPORTED_UPGRADES);
        if (exportedUpgrades == null)
        {
            return upgrades;
        }

        for (var stack : exportedUpgrades.upgrades())
        {
            if (!stack.isEmpty())
            {
                upgrades.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        return upgrades;
    }

    /** 从 AE 网络预取空白样板到背包。照抄上游。 */
    private static int preFetchBlankPatternsFromNetwork(Player player, IGrid grid, DataComponentMap components)
    {
        if (player.getAbilities().instabuild)
        {
            return 0;
        }

        int patternsNeeded = countPatternsInMemoryCard(components);
        if (patternsNeeded <= 0)
        {
            return 0;
        }

        int existingBlankPatterns = countItemInPlayerAndNetworkTool(player, AEItems.BLANK_PATTERN.asItem());
        int needToFetch = patternsNeeded - existingBlankPatterns;

        if (needToFetch <= 0)
        {
            return 0;
        }

        var storage = grid.getStorageService().getInventory();
        var src = new PlayerSource(player);
        var blankPatternKey = AEItemKeyCompat.of(AEItems.BLANK_PATTERN.asItem());

        long available = storage.extract(blankPatternKey, needToFetch, Actionable.SIMULATE, src);
        if (available <= 0)
        {
            return 0;
        }

        long extracted = storage.extract(blankPatternKey, Math.min(available, needToFetch), Actionable.MODULATE, src);
        if (extracted > 0)
        {
            ItemStack blankPatterns = AEItems.BLANK_PATTERN.stack((int) extracted);
            player.getInventory().placeItemBackInInventory(blankPatterns);
        }

        return (int) extracted;
    }

    /** 从 AE 网络预取升级卡到背包。照抄上游。 */
    private static int preFetchUpgradesFromNetwork(Player player, IGrid grid, DataComponentMap components,
            Map<Item, Integer> fetchedOut)
    {
        if (player.getAbilities().instabuild)
        {
            return 0;
        }

        Map<Item, Integer> desiredUpgrades = getUpgradesInMemoryCard(components);
        if (desiredUpgrades.isEmpty())
        {
            return 0;
        }

        var storage = grid.getStorageService().getInventory();
        var src = new PlayerSource(player);
        int totalFetched = 0;

        for (var entry : desiredUpgrades.entrySet())
        {
            Item upgradeItem = entry.getKey();
            int needed = entry.getValue();

            int existing = countItemInPlayerAndNetworkTool(player, upgradeItem);
            int needToFetch = needed - existing;

            if (needToFetch <= 0)
            {
                continue;
            }

            var upgradeKey = AEItemKeyCompat.of(upgradeItem);
            if (upgradeKey == null)
            {
                continue;
            }

            long available = storage.extract(upgradeKey, needToFetch, Actionable.SIMULATE, src);
            if (available <= 0)
            {
                continue;
            }

            long extracted = storage.extract(upgradeKey, Math.min(available, needToFetch), Actionable.MODULATE, src);
            if (extracted > 0)
            {
                ItemStack upgradeStack = new ItemStack(upgradeItem, (int) extracted);
                player.getInventory().placeItemBackInInventory(upgradeStack);
                totalFetched += (int) extracted;
                if (fetchedOut != null)
                {
                    fetchedOut.merge(upgradeItem, (int) extracted, Integer::sum);
                }
            }
        }

        return totalFetched;
    }

    /** 预取全部所需材料（空白样板 + 升级）。照抄上游。 */
    private static Map<Item, Integer> preFetchAllFromNetwork(Player player, IGrid grid, DataComponentMap components)
    {
        Map<Item, Integer> fetched = new HashMap<>();
        int patterns = preFetchBlankPatternsFromNetwork(player, grid, components);
        if (patterns > 0)
        {
            fetched.merge(AEItems.BLANK_PATTERN.asItem(), patterns, Integer::sum);
        }
        preFetchUpgradesFromNetwork(player, grid, components, fetched);
        return fetched;
    }

    /** 预取的材料退回 AE 网络。照抄上游。 */
    private static void returnPreFetched(Player player, IGrid grid, Map<Item, Integer> fetched)
    {
        if (grid == null || fetched == null || fetched.isEmpty())
        {
            return;
        }
        var storage = grid.getStorageService().getInventory();
        var src = new PlayerSource(player);
        for (var entry : fetched.entrySet())
        {
            var key = AEItemKeyCompat.of(entry.getKey());
            if (key == null)
            {
                continue;
            }
            int remaining = entry.getValue();
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++)
            {
                ItemStack slotStack = inv.getItem(i);
                if (slotStack.getItem() == entry.getKey())
                {
                    int take = Math.min(remaining, slotStack.getCount());
                    long inserted = storage.insert(key, take, Actionable.MODULATE, src);
                    if (inserted <= 0)
                    {
                        break;
                    }
                    slotStack.shrink((int) inserted);
                    remaining -= (int) inserted;
                }
            }
        }
    }

    /** 内存卡资源检查结果。照抄上游。 */
    public static class ResourceCheckResult
    {
        public final boolean sufficient;
        private final Map<Item, Integer> missingItems;

        public ResourceCheckResult(boolean sufficient, Map<Item, Integer> missingItems)
        {
            this.sufficient = sufficient;
            this.missingItems = missingItems != null ? missingItems : new HashMap<>();
        }

        public String getMissingItemsMessage()
        {
            if (missingItems.isEmpty())
            {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (var entry : missingItems.entrySet())
            {
                if (sb.length() > 0)
                {
                    sb.append(", ");
                }
                sb.append(entry.getValue()).append("x ").append(entry.getKey().getDescription().getString());
            }
            return sb.toString();
        }
    }

    /** 检查网络资源是否足够应用内存卡设置。照抄上游。 */
    public static ResourceCheckResult checkResourcesForMultipleBlocks(Player player, IGrid grid, int blockCount)
    {
        ItemStack memoryCardStack = getOffHandMemoryCard(player);
        if (memoryCardStack.isEmpty() || blockCount <= 0)
        {
            return new ResourceCheckResult(true, null);
        }

        var components = memoryCardStack.getComponents();
        var settingsSource = memoryCardStack.get(AEComponents.EXPORTED_SETTINGS_SOURCE);
        if (settingsSource == null)
        {
            return new ResourceCheckResult(true, null);
        }

        if (player.getAbilities().instabuild)
        {
            return new ResourceCheckResult(true, null);
        }

        Map<Item, Integer> missingItems = new HashMap<>();

        // 检查空白样板
        int patternsPerBlock = countPatternsInMemoryCard(components);
        if (patternsPerBlock > 0)
        {
            long totalPatternsNeeded = (long) patternsPerBlock * blockCount;
            long available = countItemInPlayerAndNetworkTool(player, AEItems.BLANK_PATTERN.asItem());

            if (grid != null)
            {
                var storage = grid.getStorageService().getInventory();
                var blankPatternKey = AEItemKeyCompat.of(AEItems.BLANK_PATTERN.asItem());
                var src = new PlayerSource(player);
                available += storage.extract(blankPatternKey, Integer.MAX_VALUE, Actionable.SIMULATE, src);
            }

            if (available < totalPatternsNeeded)
            {
                missingItems.put(AEItems.BLANK_PATTERN.asItem(),
                        (int) Math.min(Integer.MAX_VALUE, totalPatternsNeeded - available));
            }
        }

        // 检查升级
        Map<Item, Integer> upgradesPerBlock = getUpgradesInMemoryCard(components);
        for (var entry : upgradesPerBlock.entrySet())
        {
            Item upgradeItem = entry.getKey();
            long totalNeeded = (long) entry.getValue() * blockCount;
            long available = countItemInPlayerAndNetworkTool(player, upgradeItem);

            if (grid != null)
            {
                var storage = grid.getStorageService().getInventory();
                var upgradeKey = AEItemKeyCompat.of(upgradeItem);
                if (upgradeKey != null)
                {
                    var src = new PlayerSource(player);
                    available += storage.extract(upgradeKey, Integer.MAX_VALUE, Actionable.SIMULATE, src);
                }
            }

            if (available < totalNeeded)
            {
                missingItems.put(upgradeItem, (int) Math.min(Integer.MAX_VALUE, totalNeeded - available));
            }
        }

        return new ResourceCheckResult(missingItems.isEmpty(), missingItems);
    }

    /** 把内存卡设置应用到已放置的方块实体。照抄上游。 */
    public static boolean applyMemoryCardToBlock(Player player, Level level, BlockPos pos, boolean showMessage,
            IGrid grid)
    {
        ItemStack memoryCardStack = getOffHandMemoryCard(player);
        if (memoryCardStack.isEmpty())
        {
            return false;
        }

        IMemoryCard memoryCard = (IMemoryCard) memoryCardStack.getItem();

        var settingsSource = memoryCardStack.get(AEComponents.EXPORTED_SETTINGS_SOURCE);
        if (settingsSource == null)
        {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null)
        {
            return false;
        }

        var components = memoryCardStack.getComponents();

        Map<Item, Integer> fetched = grid != null
                ? preFetchAllFromNetwork(player, grid, components)
                : null;

        try
        {
            if (be instanceof AEBaseBlockEntity aeBlockEntity)
            {
                Component blockName = aeBlockEntity.getName();
                if (blockName.equals(settingsSource))
                {
                    aeBlockEntity.importSettings(SettingsFrom.MEMORY_CARD, components, player);
                    if (showMessage)
                    {
                        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                    }
                    return true;
                }
            }

            MemoryCardItem.importGenericSettingsAndNotify(be, components, player);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to apply memory card settings to block at {}", pos, e);
            returnPreFetched(player, grid, fetched);
        }

        return false;
    }

    /** 把内存卡设置应用到已放置的部件。照抄上游。 */
    public static boolean applyMemoryCardToPart(Player player, IPart part, boolean showMessage, IGrid grid)
    {
        ItemStack memoryCardStack = getOffHandMemoryCard(player);
        if (memoryCardStack.isEmpty())
        {
            return false;
        }

        IMemoryCard memoryCard = (IMemoryCard) memoryCardStack.getItem();

        var settingsSource = memoryCardStack.get(AEComponents.EXPORTED_SETTINGS_SOURCE);
        if (settingsSource == null)
        {
            return false;
        }

        var components = memoryCardStack.getComponents();

        Map<Item, Integer> fetched = grid != null
                ? preFetchAllFromNetwork(player, grid, components)
                : null;

        try
        {
            if (part instanceof AEBasePart aePart)
            {
                Component partName = aePart.getName();
                if (partName.equals(settingsSource))
                {
                    aePart.importSettings(SettingsFrom.MEMORY_CARD, components, player);
                    if (showMessage)
                    {
                        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                    }
                    return true;
                }
            }

            MemoryCardItem.importGenericSettingsAndNotify(part, components, player);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to apply memory card settings to part", e);
            returnPreFetched(player, grid, fetched);
        }

        return false;
    }

    /** AEItemKey.of 的简单包装（上游直接用 AEItemKey.of，这里保持同样写法但集中一处便于空安全） */
    private static final class AEItemKeyCompat
    {
        private AEItemKeyCompat()
        {
        }

        static appeng.api.stacks.AEItemKey of(Item item)
        {
            return appeng.api.stacks.AEItemKey.of(new ItemStack(item));
        }
    }
}
