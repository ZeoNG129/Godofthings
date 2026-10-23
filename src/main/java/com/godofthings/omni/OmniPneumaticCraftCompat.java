package com.godofthings.omni;

import me.desht.pneumaticcraft.api.upgrade.IUpgradeItem;
import me.desht.pneumaticcraft.api.upgrade.PNCUpgrade;
import me.desht.pneumaticcraft.common.upgrades.ApplicableUpgradesDB;
import me.desht.pneumaticcraft.common.upgrades.IUpgradeHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 气动工艺适配器（原样移植自 Omni Upgrades 的 {@code PneumaticCraftCompat}）：
 * 升级插件走 PNC 的 {@code ApplicableUpgradesDB} 上限表 + 升级栏物品处理器。
 */
public class OmniPneumaticCraftCompat implements UpgradeCompat {

    public static final boolean IS_LOADED = ModList.get().isLoaded("pneumaticcraft");

    @Override
    public boolean isUpgradeItem(ItemStack stack) {
        return IS_LOADED && stack.getItem() instanceof IUpgradeItem;
    }

    @Override
    public boolean isMachine(Level level, BlockPos pos) {
        return IS_LOADED && level.getBlockEntity(pos) instanceof IUpgradeHolder;
    }

    @Override
    public boolean canMachineAcceptUpgrade(Level level, BlockPos pos, ItemStack upgrade) {
        if (!IS_LOADED || !(level.getBlockEntity(pos) instanceof IUpgradeHolder holder)) {
            return false;
        }
        PNCUpgrade pncUpgrade = PNCUpgrade.from(upgrade);
        if (pncUpgrade == null) {
            return false;
        }
        int maxAllowed = ApplicableUpgradesDB.getInstance().getMaxUpgrades(level.getBlockEntity(pos), pncUpgrade);
        if (maxAllowed <= 0) {
            return false;
        }
        IItemHandler handler = holder.getUpgradeHandler();
        int currentCount = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (!inSlot.isEmpty() && PNCUpgrade.from(inSlot) == pncUpgrade) {
                currentCount += inSlot.getCount();
            }
        }
        if (currentCount >= maxAllowed) {
            return false;
        }
        return ItemHandlerHelper.insertItem(handler, upgrade.copyWithCount(1), true).isEmpty();
    }

    @Override
    public boolean installUpgrade(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack upgrade) {
        if (!IS_LOADED || !(level.getBlockEntity(pos) instanceof IUpgradeHolder holder)) {
            return false;
        }
        PNCUpgrade pncUpgrade = PNCUpgrade.from(upgrade);
        if (pncUpgrade == null) {
            return false;
        }
        int maxAllowed = ApplicableUpgradesDB.getInstance().getMaxUpgrades(level.getBlockEntity(pos), pncUpgrade);
        if (maxAllowed <= 0) {
            return false;
        }
        IItemHandler handler = holder.getUpgradeHandler();
        int currentCount = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (!inSlot.isEmpty() && PNCUpgrade.from(inSlot) == pncUpgrade) {
                currentCount += inSlot.getCount();
            }
        }
        int allowedToInsert = maxAllowed - currentCount;
        if (allowedToInsert <= 0) {
            return false;
        }
        int amountToInsert = Math.min(upgrade.getCount(), allowedToInsert);
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, upgrade.copyWithCount(amountToInsert), false);
        int actuallyInserted = amountToInsert - remainder.getCount();
        if (actuallyInserted <= 0) {
            return false;
        }
        if (!player.isCreative()) {
            upgrade.shrink(actuallyInserted);
        }
        holder.onUpgradesChanged();
        return true;
    }

    @Override
    public int[] getColor(ItemStack stack) {
        return new int[] { 110, 110, 110 };
    }

    @Override
    public Component getDisplayName(ItemStack stack) {
        int[] rgb = getColor(stack);
        int color = rgb[0] << 16 | rgb[1] << 8 | rgb[2];
        return stack.getHoverName().copy().withColor(color);
    }
}
