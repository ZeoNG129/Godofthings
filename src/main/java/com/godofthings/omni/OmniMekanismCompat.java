package com.godofthings.omni;

import mekanism.api.Upgrade;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.item.interfaces.IUpgradeItem;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.IUpgradeTile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.fml.ModList;

/**
 * 通用机械适配器（原样移植自 Omni Upgrades 的 {@code MekanismCompat}）：
 * 升级插件走 {@code TileComponentUpgrade}，等级安装器走原版 {@code useOn}；
 * 多方块的 bounding block 会解析回主机坐标。
 */
public class OmniMekanismCompat implements UpgradeCompat {

    public static final boolean IS_LOADED = ModList.get().isLoaded("mekanism");

    @Override
    public boolean isUpgradeItem(ItemStack stack) {
        return IS_LOADED && (stack.getItem() instanceof IUpgradeItem
                || stack.getItem() instanceof ItemTierInstaller);
    }

    @Override
    public boolean isMachine(Level level, BlockPos pos) {
        if (!IS_LOADED) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IUpgradeTile || be instanceof ITierUpgradable;
    }

    @Override
    public boolean canMachineAcceptUpgrade(Level level, BlockPos pos, ItemStack upgrade) {
        if (!IS_LOADED) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (upgrade.getItem() instanceof IUpgradeItem upgradeItem) {
            Upgrade type = upgradeItem.getUpgradeType(upgrade);
            if (be instanceof IUpgradeTile upgradeTile) {
                TileComponentUpgrade component = upgradeTile.getComponent();
                return component != null && component.supports(type)
                        && component.getUpgrades(type) < type.getMax();
            }
        } else if (upgrade.getItem() instanceof ItemTierInstaller installer) {
            if (!(be instanceof ITierUpgradable)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            Holder<Block> block = state.getBlockHolder();
            AttributeUpgradeable upgradeableBlock =
                    (AttributeUpgradeable) Attribute.get(block, AttributeUpgradeable.class);
            if (upgradeableBlock != null) {
                BaseTier baseTier = Attribute.getBaseTier(block);
                return baseTier == installer.getFromTier() && baseTier != installer.getToTier();
            }
        }
        return false;
    }

    @Override
    public boolean installUpgrade(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack upgrade) {
        if (!IS_LOADED) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (upgrade.getItem() instanceof IUpgradeItem upgradeItem) {
            Upgrade type = upgradeItem.getUpgradeType(upgrade);
            if (be instanceof IUpgradeTile upgradeTile) {
                TileComponentUpgrade component = upgradeTile.getComponent();
                if (component != null && component.supports(type)) {
                    int added = component.addUpgrades(type, upgrade.getCount());
                    if (added > 0) {
                        if (!player.isCreative()) {
                            upgrade.shrink(added);
                        }
                        return true;
                    }
                }
            }
        } else if (upgrade.getItem() instanceof ItemTierInstaller) {
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            UseOnContext context = new UseOnContext(player, hand, hitResult);
            InteractionResult result = upgrade.getItem().useOn(context);
            return result.consumesAction();
        }
        return false;
    }

    @Override
    public int[] getColor(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
            return upgradeItem.getUpgradeType(stack).getColor().getRgbCode();
        }
        return stack.getItem() instanceof ItemTierInstaller installer
                ? installer.getToTier().getRgbCode()
                : new int[] { 255, 255, 255 };
    }

    @Override
    public Component getDisplayName(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
            Upgrade type = upgradeItem.getUpgradeType(stack);
            return Component.translatable(type.getTranslationKey()).withColor(type.getColor().getPackedColor());
        }
        return stack.getItem() instanceof ItemTierInstaller installer
                ? stack.getHoverName().copy().withColor(installer.getToTier().getPackedColor())
                : stack.getHoverName();
    }

    @Override
    public BlockPos resolveMainPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(MekanismBlocks.BOUNDING_BLOCK)) {
            BlockPos mainPos = BlockBounding.getMainBlockPos(level, pos);
            if (mainPos != null) {
                return mainPos;
            }
        }
        return pos;
    }
}
