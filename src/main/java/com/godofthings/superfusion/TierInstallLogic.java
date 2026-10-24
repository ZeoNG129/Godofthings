package com.godofthings.superfusion;

import com.godofthings.superfusion.util.NetworkItemSource;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 等级安装逻辑（移植自 Mekanism-Card 的 {@code UltimateTierInstaller}）。
 * <p>
 * 上游该类本身是一个物品（终极等级安装器），但融合卡的「等级安装」模式直接复用了它的
 * 全部方法。本模组只移植融合卡，因此这里把它降级成纯逻辑类：
 * 去掉物品身份、工具提示与 {@code useOn} 入口，只保留「把机器升到指定等级」的核心流程。
 * <p>
 * 能量消耗口径与上游一致：每次升级扣 1000 FE，创造模式免费。
 */
public final class TierInstallLogic
{
    /** 单次升级扣电（FE） */
    public static final int ENERGY_PER_UPGRADE = 1_000;

    private TierInstallLogic()
    {
    }

    /** 目标方块（含多方块从属块）是否可升级 */
    public static boolean isUpgradeTarget(Level world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        if (state.is(MekanismBlocks.BOUNDING_BLOCK))
        {
            BlockPos mainPos = BlockBounding.getMainBlockPos(world, pos);
            if (mainPos == null)
            {
                return false;
            }
            state = world.getBlockState(mainPos);
        }
        return Attribute.get(state.getBlockHolder(), AttributeUpgradeable.class) != null;
    }

    /**
     * 把单个方块一路升到终极等级（上游 {@code handleSingleUpgrade} 的核心部分）。
     *
     * @return 是否成功
     */
    public static boolean upgradeSingleToUltimate(Level world, BlockPos pos, Player player, ItemStack cardStack)
    {
        BlockState state = world.getBlockState(pos);

        if (state.is(MekanismBlocks.BOUNDING_BLOCK))
        {
            BlockPos mainPos = BlockBounding.getMainBlockPos(world, pos);
            if (mainPos != null)
            {
                pos = mainPos;
                state = world.getBlockState(mainPos);
            }
        }

        Holder<Block> block = state.getBlockHolder();
        if (Attribute.get(block, AttributeUpgradeable.class) == null)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.not_upgradeable")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        BaseTier currentTier = Attribute.getBaseTier(block);
        if (!isBelowUltimate(currentTier) || ExtraTierIntegration.getAdvancedTierName(block) != null)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.already_ultimate")
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        BlockEntity tile = WorldUtils.getTileEntity(world, pos);
        if (tile instanceof TileEntityMekanism tileMek && !tileMek.playersUsing.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.machine_in_use")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!(tile instanceof ITierUpgradable tierUpgradable))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.not_tier_upgradable")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!tierUpgradable.canBeUpgraded())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.cannot_upgrade")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        boolean creative = player.isCreative();
        IStrictEnergyHandler energyHandler = Capabilities.STRICT_ENERGY.getCapability(cardStack);
        if (!creative && (energyHandler == null || energyHandler.getEnergy(0) < ENERGY_PER_UPGRADE))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_energy")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        int steps = performStrictUpgradeToUltimate(world, pos, state);
        if (steps <= 0)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.cannot_upgrade")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        if (!creative)
        {
            energyHandler.extractEnergy(0, ENERGY_PER_UPGRADE, Action.EXECUTE);
        }
        player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.success", steps)
                .withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    public static boolean isBelowUltimate(@Nullable BaseTier tier)
    {
        return tier == null || tier == BaseTier.BASIC || tier == BaseTier.ADVANCED || tier == BaseTier.ELITE;
    }

    /** 取方块的有效基础等级（Extras 注入的等级按终极处理，与上游一致） */
    public static BaseTier getEffectiveBaseTier(Holder<Block> block)
    {
        BaseTier baseTier = Attribute.getBaseTier(block);
        if (baseTier == null && ExtraTierIntegration.getAdvancedTierName(block) != null)
        {
            return BaseTier.ULTIMATE;
        }
        return baseTier;
    }

    /**
     * 把目标方块升到指定等级（融合卡「全量模式」与「等级安装」模式共用）。
     * <p>
     * 照抄上游 {@code upgradeToTier}：需要哪种安装器就从背包 / ME 网络 / QIO 里扣哪种，
     * 缺料会给出缺料提示。
     */
    public static boolean upgradeToTier(Level world, BlockPos pos, Player player, ItemStack handStack,
                                        BaseTier desiredTier, @Nullable String desiredAdvancedTier)
    {
        BlockState state = world.getBlockState(pos);
        Holder<Block> block = state.getBlockHolder();
        BaseTier currentTier = getEffectiveBaseTier(block);
        String currentAdvancedTier = ExtraTierIntegration.getAdvancedTierName(block);
        if (desiredAdvancedTier == null)
        {
            if (currentAdvancedTier != null || currentTier == desiredTier
                    || currentTier != null && currentTier.ordinal() > desiredTier.ordinal())
            {
                return true;
            }
        }
        else
        {
            int desiredAdvancedIndex = ExtraTierIntegration.getTierIndex(desiredAdvancedTier);
            int currentAdvancedIndex = ExtraTierIntegration.getTierIndex(currentAdvancedTier);
            if (desiredAdvancedIndex == Integer.MIN_VALUE)
            {
                return false;
            }
            if (currentAdvancedIndex >= desiredAdvancedIndex)
            {
                return true;
            }
            if (currentTier != null && currentTier.ordinal() > desiredTier.ordinal())
            {
                return false;
            }
        }

        boolean needsBaseUpgrade = currentTier != desiredTier;
        boolean needsAdvancedUpgrade = desiredAdvancedTier != null;
        AttributeUpgradeable baseUpgradeable = Attribute.get(block, AttributeUpgradeable.class);
        Object extraUpgradeable = ExtraTierIntegration.getExtraAttributeUpgradeable(block);
        if (needsBaseUpgrade && baseUpgradeable == null
                || needsAdvancedUpgrade && !needsBaseUpgrade && extraUpgradeable == null
                || !(WorldUtils.getTileEntity(world, pos) instanceof ITierUpgradable tierUpgradable)
                || needsBaseUpgrade && !tierUpgradable.canBeUpgraded())
        {
            return false;
        }

        List<Item> requiredInstallers = new ArrayList<>();
        if (needsBaseUpgrade)
        {
            List<Item> baseInstallers = getRequiredInstallersToTier(currentTier, desiredTier);
            if (baseInstallers.isEmpty())
            {
                return false;
            }
            requiredInstallers.addAll(baseInstallers);
        }
        if (needsAdvancedUpgrade)
        {
            List<Item> advancedInstallers = ExtraTierIntegration.getRequiredInstallersToTier(
                    currentAdvancedTier, desiredAdvancedTier);
            if (advancedInstallers.isEmpty())
            {
                return false;
            }
            requiredInstallers.addAll(advancedInstallers);
        }
        if (requiredInstallers.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.tier_unsupported")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        boolean creative = player.isCreative();
        IStrictEnergyHandler energyHandler = Capabilities.STRICT_ENERGY.getCapability(handStack);
        if (!creative && (energyHandler == null || energyHandler.getEnergy(0) < ENERGY_PER_UPGRADE))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_energy")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        NetworkItemSource itemSource = NetworkItemSource.create(world, player, handStack);
        if (!creative && !hasRequiredInstallers(requiredInstallers, itemSource))
        {
            showMissingInstallersMessage(player, requiredInstallers, itemSource);
            return false;
        }

        int steps = needsBaseUpgrade
                ? performTierUpgradeTo(world, pos, state, currentTier, desiredTier)
                : 0;
        if (needsAdvancedUpgrade)
        {
            BlockState advancedState = world.getBlockState(pos);
            steps += performExtrasTierUpgradeTo(world, pos, advancedState, desiredAdvancedTier);
        }
        Holder<Block> finalBlock = world.getBlockState(pos).getBlockHolder();
        BaseTier finalTier = getEffectiveBaseTier(finalBlock);
        String finalAdvancedTier = ExtraTierIntegration.getAdvancedTierName(finalBlock);
        if (steps <= 0 || finalTier != desiredTier
                || desiredAdvancedTier != null && !desiredAdvancedTier.equals(finalAdvancedTier))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.tier_failed")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!creative)
        {
            consumeInstallers(itemSource, requiredInstallers);
            energyHandler.extractEnergy(0, ENERGY_PER_UPGRADE, Action.EXECUTE);
        }
        return true;
    }

    private static List<Item> getRequiredInstallersToTier(@Nullable BaseTier currentTier, BaseTier desiredTier)
    {
        List<Item> installers = new ArrayList<>();
        BaseTier tier = currentTier;
        while (tier != desiredTier)
        {
            BaseTier next = getNextTierToward(tier);
            if (next == null || next.ordinal() > desiredTier.ordinal())
            {
                return List.of();
            }
            Item installer = getInstallerForTransition(tier, next);
            if (installer == null)
            {
                return List.of();
            }
            installers.add(installer);
            tier = next;
        }
        return installers;
    }

    @Nullable
    private static BaseTier getNextTierToward(@Nullable BaseTier currentTier)
    {
        BaseTier standardNext = getNextTier(currentTier);
        if (standardNext != null)
        {
            return standardNext;
        }
        return currentTier == null ? null : ExtraTierIntegration.getNextTier(currentTier);
    }

    @Nullable
    private static Item getInstallerForTransition(@Nullable BaseTier currentTier, BaseTier nextTier)
    {
        Item standard = getInstallerForNextTier(currentTier);
        if (standard != null)
        {
            return standard;
        }
        String tierName = ExtraTierIntegration.getTierLowerName(nextTier);
        return tierName == null ? null : ExtraTierIntegration.getTierInstallerItem(tierName);
    }

    @Nullable
    private static Item getInstallerForNextTier(@Nullable BaseTier currentTier)
    {
        if (currentTier == null)
        {
            return MekanismItems.BASIC_TIER_INSTALLER.get();
        }
        return switch (currentTier)
        {
            case BASIC -> MekanismItems.ADVANCED_TIER_INSTALLER.get();
            case ADVANCED -> MekanismItems.ELITE_TIER_INSTALLER.get();
            case ELITE -> MekanismItems.ULTIMATE_TIER_INSTALLER.get();
            default -> null;
        };
    }

    @Nullable
    private static BaseTier getNextTier(@Nullable BaseTier currentTier)
    {
        if (currentTier == null)
        {
            return BaseTier.BASIC;
        }
        return switch (currentTier)
        {
            case BASIC -> BaseTier.ADVANCED;
            case ADVANCED -> BaseTier.ELITE;
            case ELITE -> BaseTier.ULTIMATE;
            default -> null;
        };
    }

    private static boolean hasRequiredInstallers(List<Item> requiredInstallers, NetworkItemSource itemSource)
    {
        for (Item installer : requiredInstallers)
        {
            if (!itemSource.has(installer, 1))
            {
                return false;
            }
        }
        return true;
    }

    private static void showMissingInstallersMessage(Player player, List<Item> requiredInstallers,
                                                     NetworkItemSource itemSource)
    {
        Item missing = null;
        for (Item installer : requiredInstallers)
        {
            if (!itemSource.has(installer, 1))
            {
                missing = installer;
                break;
            }
        }
        if (missing != null)
        {
            player.displayClientMessage(Component.translatable(
                            "message.godofthings.super_fusion.missing_installer", missing.getDescription())
                    .withStyle(ChatFormatting.RED), true);
        }
        else if (!itemSource.hasExternalStorage())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_network")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private static void consumeInstallers(NetworkItemSource itemSource, List<Item> requiredInstallers)
    {
        for (Item installer : requiredInstallers)
        {
            itemSource.consume(installer, 1);
        }
    }

    /** 一路升到终极（不改动数据组件，纯世界操作；返回实际升了几步） */
    private static int performStrictUpgradeToUltimate(Level world, BlockPos pos, BlockState initialState)
    {
        List<BlockState> path = new ArrayList<>();
        BlockState probe = initialState;
        Set<Block> seenBlocks = new HashSet<>();

        for (int i = 0; i < 16; i++)
        {
            Holder<Block> probeBlock = probe.getBlockHolder();
            BaseTier probeTier = Attribute.getBaseTier(probeBlock);
            if (probeTier == BaseTier.ULTIMATE && ExtraTierIntegration.getAdvancedTierName(probeBlock) == null)
            {
                break;
            }
            if (!isBelowUltimate(probeTier) || ExtraTierIntegration.getAdvancedTierName(probeBlock) != null
                    || !seenBlocks.add(probe.getBlock()))
            {
                return 0;
            }
            AttributeUpgradeable upgradeable = Attribute.get(probeBlock, AttributeUpgradeable.class);
            if (upgradeable == null)
            {
                return 0;
            }
            BlockState next = upgradeable.upgradeResult(probe, BaseTier.ULTIMATE);
            if (next == probe)
            {
                return 0;
            }
            BaseTier nextTier = Attribute.getBaseTier(next.getBlockHolder());
            if (ExtraTierIntegration.getAdvancedTierName(next.getBlockHolder()) != null
                    || nextTier != BaseTier.ULTIMATE && !isBelowUltimate(nextTier))
            {
                return 0;
            }
            path.add(next);
            probe = next;
        }

        if (path.isEmpty() || Attribute.getBaseTier(probe.getBlockHolder()) != BaseTier.ULTIMATE
                || ExtraTierIntegration.getAdvancedTierName(probe.getBlockHolder()) != null)
        {
            return 0;
        }

        int steps = 0;
        for (BlockState nextState : path)
        {
            if (!applyNextState(world, pos, nextState))
            {
                return steps;
            }
            steps++;
        }

        Holder<Block> finalBlock = world.getBlockState(pos).getBlockHolder();
        return Attribute.getBaseTier(finalBlock) == BaseTier.ULTIMATE
                && ExtraTierIntegration.getAdvancedTierName(finalBlock) == null ? steps : 0;
    }

    private static int performTierUpgradeTo(Level world, BlockPos pos, BlockState initialState,
                                            @Nullable BaseTier startTier, BaseTier desiredTier)
    {
        BlockState currentState = initialState;
        BaseTier currentTier = startTier;
        int steps = 0;
        while (currentTier != desiredTier)
        {
            AttributeUpgradeable upgradeable = Attribute.get(currentState.getBlockHolder(), AttributeUpgradeable.class);
            if (upgradeable == null)
            {
                break;
            }
            BlockState nextState = upgradeable.upgradeResult(currentState, desiredTier);
            if (currentState == nextState)
            {
                break;
            }
            if (!applyNextState(world, pos, nextState))
            {
                break;
            }
            currentState = nextState;
            currentTier = getEffectiveBaseTier(currentState.getBlockHolder());
            steps++;
            if (currentTier == null || currentTier.ordinal() > desiredTier.ordinal())
            {
                break;
            }
        }
        return steps;
    }

    /**
     * 把方块换成下一等级的状态，并把旧方块实体的升级数据搬过去。
     * <p>
     * 这段是上游 {@code performStrictUpgradeToUltimate} / {@code performTierUpgradeTo} /
     * {@code performExtrasTierUpgradeTo} 里重复出现三次的同一段，这里抽出来复用。
     */
    private static boolean applyNextState(Level world, BlockPos pos, BlockState nextState)
    {
        BlockEntity currentTile = WorldUtils.getTileEntity(world, pos);
        if (!(currentTile instanceof ITierUpgradable upgradable))
        {
            return false;
        }
        IUpgradeData upgradeData = upgradable.getUpgradeData(world.registryAccess());
        if (upgradeData == null)
        {
            return false;
        }
        AttributeHasBounding nextBounding = Attribute.get(nextState, AttributeHasBounding.class);
        if (nextBounding != null && !nextBounding.handle(world, pos, nextState, pos,
                (level, boundingPos, mainPos) ->
                {
                    Optional<BlockState> blockState = WorldUtils.getBlockState(level, boundingPos);
                    if (blockState.isEmpty())
                    {
                        return false;
                    }
                    BlockState boundingState = blockState.get();
                    return boundingState.canBeReplaced()
                            || boundingState.is(MekanismBlocks.BOUNDING_BLOCK)
                            && mainPos.equals(BlockBounding.getMainBlockPos(level, boundingPos));
                }))
        {
            return false;
        }
        if (!world.setBlockAndUpdate(pos, nextState))
        {
            return false;
        }
        if (nextBounding != null)
        {
            nextBounding.placeBoundingBlocks(world, pos, nextState);
        }
        TileEntityMekanism upgradedTile = WorldUtils.getTileEntity(TileEntityMekanism.class, world, pos);
        if (upgradedTile == null)
        {
            return false;
        }
        if (currentTile instanceof ITileDirectional directional && directional.isDirectional())
        {
            upgradedTile.setFacing(directional.getDirection(), false);
        }
        upgradedTile.parseUpgradeData(world.registryAccess(), upgradeData);
        upgradedTile.sendUpdatePacket();
        upgradedTile.setChanged();
        upgradedTile.invalidateCapabilitiesFull();
        return true;
    }

    private static int performExtrasTierUpgradeTo(Level world, BlockPos pos, BlockState initialState,
                                                  String desiredAdvancedTier)
    {
        BlockState currentState = initialState;
        int steps = 0;

        while (true)
        {
            Holder<Block> currentBlock = currentState.getBlockHolder();
            String currentAdvancedTier = ExtraTierIntegration.getAdvancedTierName(currentBlock);
            if (desiredAdvancedTier.equals(currentAdvancedTier)
                    || ExtraTierIntegration.getTierIndex(currentAdvancedTier)
                    > ExtraTierIntegration.getTierIndex(desiredAdvancedTier))
            {
                break;
            }

            String nextTierName = currentAdvancedTier == null
                    ? ExtraTierIntegration.TIER_ABSOLUTE
                    : ExtraTierIntegration.getNextTierName(currentAdvancedTier);
            if (nextTierName == null)
            {
                break;
            }
            Object upgradeable = ExtraTierIntegration.getExtraAttributeUpgradeable(currentBlock);
            if (upgradeable == null)
            {
                break;
            }
            BlockState nextState = ExtraTierIntegration.upgradeResult(upgradeable, currentState, nextTierName);
            if (nextState == null || currentState == nextState)
            {
                break;
            }
            if (!applyNextState(world, pos, nextState))
            {
                break;
            }
            currentState = nextState;
            steps++;
        }

        return steps;
    }

    /** 批量升级（框选 / 连锁）：逐个目标调用 {@link #upgradeToTier} */
    public static void upgradeBatchToTier(Level level, Collection<BlockPos> positions, Player player,
                                          ItemStack stack, BaseTier desiredTier,
                                          @Nullable String desiredAdvancedTier, boolean fuzzy, Block sourceBlock)
    {
        boolean hadTypeMismatch = false;
        int upgraded = 0;
        for (BlockPos targetPos : new LinkedHashSet<>(positions))
        {
            if (!level.hasChunkAt(targetPos)
                    || !(level.getBlockEntity(targetPos) instanceof TileEntityMekanism))
            {
                continue;
            }
            if (!fuzzy && !MemoryCardLogic.isSameMachineFamily(sourceBlock,
                    level.getBlockState(targetPos).getBlock()))
            {
                hadTypeMismatch = true;
                continue;
            }
            if (upgradeToTier(level, targetPos, player, stack, desiredTier, desiredAdvancedTier))
            {
                upgraded++;
            }
        }
        if (upgraded > 0)
        {
            player.displayClientMessage(Component.translatable(
                            "message.godofthings.super_fusion.selection_success", upgraded)
                    .withStyle(ChatFormatting.GREEN), true);
        }
        else if (hadTypeMismatch)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.tier_type_mismatch")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }
}
