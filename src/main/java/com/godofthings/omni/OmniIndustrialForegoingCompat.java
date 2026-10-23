package com.godofthings.omni;

import com.buuz135.industrial.item.addon.AddonItem;
import com.hrznstudio.titanium.block.tile.MachineTile;
import com.hrznstudio.titanium.component.inventory.SidedInventoryComponent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 工业先锋适配器（原样移植自 Omni Upgrades 的 {@code IndustrialForegoingCompat}）：
 * AddonItem（增幅插件）装入 MachineTile 的增幅栏；同类型高等级插件可替换低等级的。
 */
public class OmniIndustrialForegoingCompat implements UpgradeCompat {

    public static final boolean IS_LOADED = ModList.get().isLoaded("industrialforegoing");

    private static final Map<Item, String> BASE_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<Item, Integer> TIER_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean isUpgradeItem(ItemStack stack) {
        return IS_LOADED && stack.getItem() instanceof AddonItem;
    }

    @Override
    public boolean isMachine(Level level, BlockPos pos) {
        return IS_LOADED && level.getBlockEntity(pos) instanceof MachineTile;
    }

    @Override
    public boolean canMachineAcceptUpgrade(Level level, BlockPos pos, ItemStack upgrade) {
        if (!IS_LOADED || !(level.getBlockEntity(pos) instanceof MachineTile<?> machineTile)) {
            return false;
        }
        SidedInventoryComponent augmentInv = machineTile.getAugmentInventory();
        String playerBase = getBaseName(upgrade);
        int playerTier = getTier(upgrade);
        boolean hasSameType = false;
        for (int i = 0; i < augmentInv.getSlots(); i++) {
            ItemStack installed = augmentInv.getStackInSlot(i);
            if (!installed.isEmpty() && installed.getItem() instanceof AddonItem
                    && playerBase.equals(getBaseName(installed))) {
                hasSameType = true;
                return playerTier > getTier(installed);
            }
        }
        if (!hasSameType && machineTile.canAcceptAugment(upgrade)) {
            for (int ix = 0; ix < augmentInv.getSlots(); ix++) {
                if (augmentInv.getStackInSlot(ix).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean installUpgrade(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack upgrade) {
        if (!IS_LOADED || !(level.getBlockEntity(pos) instanceof MachineTile<?> machineTile)) {
            return false;
        }
        SidedInventoryComponent augmentInv = machineTile.getAugmentInventory();
        String playerBase = getBaseName(upgrade);
        int playerTier = getTier(upgrade);
        boolean hasSameType = false;
        for (int i = 0; i < augmentInv.getSlots(); i++) {
            ItemStack installed = augmentInv.getStackInSlot(i);
            if (!installed.isEmpty() && installed.getItem() instanceof AddonItem
                    && playerBase.equals(getBaseName(installed))) {
                hasSameType = true;
                if (playerTier > getTier(installed)) {
                    augmentInv.setStackInSlot(i, ItemStack.EMPTY);
                    if (machineTile.canAcceptAugment(upgrade)) {
                        augmentInv.setStackInSlot(i, upgrade.copyWithCount(1));
                        if (!player.isCreative()) {
                            upgrade.shrink(1);
                        }
                        ItemHandlerHelper.giveItemToPlayer(player, installed);
                        return true;
                    }
                    augmentInv.setStackInSlot(i, installed);
                    return false;
                }
                return false;
            }
        }
        if (!hasSameType && machineTile.canAcceptAugment(upgrade)) {
            for (int ix = 0; ix < augmentInv.getSlots(); ix++) {
                if (augmentInv.getStackInSlot(ix).isEmpty()) {
                    augmentInv.setStackInSlot(ix, upgrade.copyWithCount(1));
                    if (!player.isCreative()) {
                        upgrade.shrink(1);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int[] getColor(ItemStack stack) {
        return new int[] { 135, 206, 235 };
    }

    @Override
    public Component getDisplayName(ItemStack stack) {
        int[] rgb = getColor(stack);
        int color = rgb[0] << 16 | rgb[1] << 8 | rgb[2];
        return stack.getHoverName().copy().withColor(color);
    }

    // 工业先锋的插件 id 形如 speed_tier_1 / speed_tier_2：去掉 _tier_ 后
    // 最后一段下划线之前是同类型名、之后是等级数。
    private static String getBaseName(ItemStack stack) {
        return BASE_NAME_CACHE.computeIfAbsent(stack.getItem(), item -> {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            path = path.replace("_tier_", "_");
            int index = path.lastIndexOf('_');
            return index != -1 ? path.substring(0, index) : path;
        });
    }

    private static int getTier(ItemStack stack) {
        return TIER_CACHE.computeIfAbsent(stack.getItem(), item -> {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            path = path.replace("_tier_", "_");
            int index = path.lastIndexOf('_');
            if (index != -1) {
                try {
                    return Integer.parseInt(path.substring(index + 1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            return 0;
        });
    }
}
