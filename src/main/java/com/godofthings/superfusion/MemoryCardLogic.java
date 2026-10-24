package com.godofthings.superfusion;

import mekanism.api.IConfigCardAccess;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.tier.BaseTier;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import com.godofthings.superfusion.util.NetworkItemSource;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 内存卡逻辑（移植自 Mekanism-Card 的 {@code MemoryCard}）。
 * <p>
 * 上游该物品负责「复制机器配置 → 粘贴到同类机器」，融合卡的「内存复制/粘贴」与「全量模式」
 * 都直接调用它的静态方法。这里把物品身份去掉，只留静态逻辑：
 * 数据仍然存在物品的 {@code CUSTOM_DATA} 组件里（键名与上游完全一致，因此旧存档/旧卡兼容）。
 */
public final class MemoryCardLogic
{
    /** 机器数据在 CUSTOM_DATA 里的根键（与上游一致，不能改） */
    private static final String KEY_MACHINE_DATA = "MachineData";
    private static final String KEY_SOURCE_BLOCK = "SourceBlockType";
    private static final String KEY_BLOCK_TYPE = "BlockType";
    private static final String KEY_CONFIG_DATA = "ConfigData";
    private static final String KEY_MACHINE_COUNT = "MachineCount";

    private MemoryCardLogic()
    {
    }

    public static boolean hasData(ItemStack stack)
    {
        return readTag(stack).contains(KEY_MACHINE_DATA);
    }

    private static CompoundTag readTag(ItemStack stack)
    {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void writeTag(ItemStack stack, CompoundTag tag)
    {
        if (tag.isEmpty())
        {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
        else
        {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** 卡里记录的升级等级（已按源机器数量折算成单机数量） */
    public static Map<Upgrade, Integer> getStoredUpgradeLevels(ItemStack stack)
    {
        CompoundTag tag = readTag(stack);
        Map<Upgrade, Integer> levels = new EnumMap<>(Upgrade.class);
        if (!tag.contains(KEY_MACHINE_DATA))
        {
            return levels;
        }
        CompoundTag machineData = tag.getCompound(KEY_MACHINE_DATA);
        int sourceMachineCount = Math.max(1, machineData.getInt(KEY_MACHINE_COUNT));
        ListTag upgradeList = machineData.getList(SerializationConstants.UPGRADES,
                net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < upgradeList.size(); index++)
        {
            CompoundTag upgradeTag = upgradeList.getCompound(index);
            Upgrade upgrade = findStoredUpgrade(upgradeTag);
            if (upgrade != null)
            {
                int amount = Math.min(upgrade.getMax(),
                        upgradeTag.getInt(SerializationConstants.AMOUNT) / sourceMachineCount);
                if (amount > 0)
                {
                    levels.put(upgrade, amount);
                }
            }
        }
        return levels;
    }

    /** 卡里记录的源方块（用于精确匹配机器种类） */
    @Nullable
    public static Block getStoredSourceBlock(ItemStack stack)
    {
        CompoundTag tag = readTag(stack);
        if (!tag.contains(KEY_MACHINE_DATA))
        {
            return null;
        }
        String sourceBlockType = tag.getCompound(KEY_MACHINE_DATA).getString(KEY_SOURCE_BLOCK);
        ResourceLocation id = ResourceLocation.tryParse(sourceBlockType);
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }

    @Nullable
    private static Upgrade findStoredUpgrade(CompoundTag upgradeTag)
    {
        if (upgradeTag.contains(SerializationConstants.TYPE))
        {
            int typeOrdinal = upgradeTag.getInt(SerializationConstants.TYPE);
            Upgrade[] values = Upgrade.values();
            if (typeOrdinal >= 0 && typeOrdinal < values.length)
            {
                return values[typeOrdinal];
            }
        }
        if (upgradeTag.contains("UpgradeName"))
        {
            String name = upgradeTag.getString("UpgradeName");
            for (Upgrade upgrade : Upgrade.values())
            {
                if (upgrade.getSerializedName().equals(name))
                {
                    return upgrade;
                }
            }
        }
        return null;
    }

    /**
     * 判断两个方块是否属于同一机器家族（同等级不同变体视为同类）。
     * <p>
     * 上游按注册名去掉等级后缀后比较；这里同样处理：{@code mekanism:basic_energy_cube} 与
     * {@code mekanism:ultimate_energy_cube} 视为同族。
     */
    public static boolean isSameMachineFamily(@Nullable Block source, Block target)
    {
        if (source == null)
        {
            return false;
        }
        if (source == target)
        {
            return true;
        }
        String sourceName = BuiltInRegistries.BLOCK.getKey(source).getPath();
        String targetName = BuiltInRegistries.BLOCK.getKey(target).getPath();
        return stripTierPrefix(sourceName).equals(stripTierPrefix(targetName));
    }

    private static String stripTierPrefix(String path)
    {
        // 照抄上游 normalizeMachineFamily：循环剥前缀（第三方等级可能叠多层，如 creative_ultimate_）
        String normalized = path.toLowerCase(java.util.Locale.ROOT);
        boolean changed;
        do
        {
            changed = false;
            for (String prefix : TIER_PREFIXES)
            {
                if (normalized.startsWith(prefix))
                {
                    normalized = normalized.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
        }
        while (changed);
        return normalized;
    }

    private static final String[] TIER_PREFIXES = {
            "basic_", "advanced_", "elite_", "ultimate_", "absolute_", "supreme_", "cosmic_", "infinite_",
            "overclocked_", "quantum_", "dense_", "multiversal_", "creative_"
    };

    /** 复制机器配置到卡里（潜行右键机器） */
    public static void handleCopy(Level level, BlockPos pos, Player player, ItemStack stack)
    {
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof IConfigCardAccess machine))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.not_mekanism")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        Block targetBlock = level.getBlockState(pos).getBlock();
        CompoundTag data = new CompoundTag();

        ListTag upgradeList = new ListTag();
        if (be instanceof TileEntityMekanism tile)
        {
            TileComponentUpgrade comp = tile.getComponent();
            if (comp != null)
            {
                for (Upgrade upgrade : Upgrade.values())
                {
                    int upgradeLevel = comp.getUpgrades(upgrade);
                    if (upgradeLevel > 0)
                    {
                        CompoundTag upgradeTag = new CompoundTag();
                        upgradeTag.putInt(SerializationConstants.TYPE, upgrade.ordinal());
                        upgradeTag.putString("UpgradeName", upgrade.getSerializedName());
                        upgradeTag.putInt(SerializationConstants.AMOUNT, upgradeLevel);
                        upgradeList.add(upgradeTag);
                    }
                }
            }
        }
        data.put(SerializationConstants.UPGRADES, upgradeList);

        CompoundTag configData = machine.getConfigurationData(level.registryAccess(), player);
        if (configData != null && !configData.isEmpty())
        {
            data.put(KEY_CONFIG_DATA, configData);
            data.putString(KEY_BLOCK_TYPE, BuiltInRegistries.BLOCK.getKey(targetBlock).toString());
        }

        data.putInt(KEY_MACHINE_COUNT, 1);
        data.putString(KEY_SOURCE_BLOCK, BuiltInRegistries.BLOCK.getKey(targetBlock).toString());

        CompoundTag tag = readTag(stack);
        tag.put(KEY_MACHINE_DATA, data);
        writeTag(stack, tag);

        player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.copied",
                        1, upgradeList.size())
                .withStyle(ChatFormatting.GREEN), true);
    }

    /** 清空卡里的机器数据（潜行右键空气） */
    public static void handleClearMachineData(Player player, ItemStack stack)
    {
        CompoundTag tag = readTag(stack);
        if (!tag.contains(KEY_MACHINE_DATA))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_data")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        tag.remove(KEY_MACHINE_DATA);
        writeTag(stack, tag);
        player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.cleared")
                .withStyle(ChatFormatting.YELLOW), true);
    }

    /** 把卡里记录的升级配置粘贴到机器上（内存复制/粘贴模式） */
    public static void handlePaste(Level level, BlockPos pos, Player player, ItemStack stack)
    {
        CompoundTag tag = readTag(stack);
        if (!tag.contains(KEY_MACHINE_DATA))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_data")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof IConfigCardAccess))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.not_mekanism")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        CompoundTag machineData = tag.getCompound(KEY_MACHINE_DATA);
        String sourceBlockType = machineData.getString(KEY_SOURCE_BLOCK);
        String targetBlockType = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();

        if (!sourceBlockType.isEmpty() && !sourceBlockType.equals(targetBlockType))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.type_mismatch")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        pasteToMachines(level, List.of(pos), player, stack, machineData, true);
    }

    /** 框选批量粘贴 */
    public static void handleBatchPaste(Level level, Collection<BlockPos> positions, Player player, ItemStack stack)
    {
        CompoundTag tag = readTag(stack);
        if (!tag.contains(KEY_MACHINE_DATA))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_data")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        CompoundTag machineData = tag.getCompound(KEY_MACHINE_DATA);
        String sourceBlockType = machineData.getString(KEY_SOURCE_BLOCK);
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos targetPos : new LinkedHashSet<>(positions))
        {
            if (!level.hasChunkAt(targetPos))
            {
                continue;
            }
            String targetBlockType = BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).getBlock()).toString();
            if (level.getBlockEntity(targetPos) instanceof IConfigCardAccess
                    && (sourceBlockType.isEmpty() || sourceBlockType.equals(targetBlockType)))
            {
                targets.add(targetPos);
            }
        }
        if (targets.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.paste_failed")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        pasteToMachines(level, targets, player, stack, machineData, true);
    }

    /** 全量模式：先把目标机器升到卡里记录的等级，再把升级与配置一并粘贴 */
    public static void handleFullPaste(Level level, BlockPos pos, Player player, ItemStack stack)
    {
        handleBatchFullPaste(level, List.of(pos), player, stack);
    }

    public static void handleBatchFullPaste(Level level, Collection<BlockPos> positions, Player player,
                                            ItemStack stack)
    {
        CompoundTag tag = readTag(stack);
        if (!tag.contains(KEY_MACHINE_DATA))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_data")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        CompoundTag machineData = tag.getCompound(KEY_MACHINE_DATA);
        Block sourceBlock = getStoredSourceBlock(stack);
        BaseTier desiredTier = sourceBlock == null ? null
                : TierInstallLogic.getEffectiveBaseTier(sourceBlock.builtInRegistryHolder());
        String desiredAdvancedTier = sourceBlock == null ? null
                : ExtraTierIntegration.getAdvancedTierName(sourceBlock.builtInRegistryHolder());
        if (sourceBlock == null || desiredTier == null)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_tier")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        boolean fuzzy = SuperFusionItem.isFuzzy(stack);
        List<BlockPos> upgradedTargets = new ArrayList<>();
        for (BlockPos targetPos : new LinkedHashSet<>(positions))
        {
            if (!level.hasChunkAt(targetPos))
            {
                continue;
            }
            BlockEntity target = level.getBlockEntity(targetPos);
            if (!(target instanceof TileEntityMekanism)
                    || !fuzzy && (!(target instanceof IConfigCardAccess)
                    || !isSameMachineFamily(sourceBlock, level.getBlockState(targetPos).getBlock())))
            {
                continue;
            }
            if (TierInstallLogic.upgradeToTier(level, targetPos, player, stack, desiredTier, desiredAdvancedTier))
            {
                upgradedTargets.add(targetPos);
            }
        }
        if (upgradedTargets.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.paste_failed")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        pasteToMachines(level, upgradedTargets, player, stack, machineData, !fuzzy);
    }

    /**
     * 把升级与（可选）配置写进目标机器。
     * <p>
     * 升级物品按「背包 → ME 网络 → QIO」的顺序扣，非创造模式缺料会中止。
     */
    private static void pasteToMachines(Level level, List<BlockPos> connectedMachines, Player player,
                                        ItemStack stack, CompoundTag machineData, boolean includeConfiguration)
    {
        Map<Upgrade, Integer> perMachineUpgrade = new EnumMap<>(Upgrade.class);
        if (machineData.contains(SerializationConstants.UPGRADES))
        {
            ListTag upgradeList = machineData.getList(SerializationConstants.UPGRADES,
                    net.minecraft.nbt.Tag.TAG_COMPOUND);
            int originalMachineCount = Math.max(1, machineData.getInt(KEY_MACHINE_COUNT));
            for (int i = 0; i < upgradeList.size(); i++)
            {
                CompoundTag upgradeTag = upgradeList.getCompound(i);
                int totalAmount = upgradeTag.getInt(SerializationConstants.AMOUNT);
                Upgrade upgrade = findStoredUpgrade(upgradeTag);
                if (upgrade != null && totalAmount > 0)
                {
                    int amountPerMachine = Math.min(totalAmount / originalMachineCount, upgrade.getMax());
                    perMachineUpgrade.put(upgrade, amountPerMachine);
                }
            }
        }

        boolean creative = player.getAbilities().instabuild;
        NetworkItemSource itemSource = NetworkItemSource.create(level, player, stack);

        // 非创造模式先整体校验材料是否够（不够就整批不做，避免装一半），照抄上游 getNeededUpgrades 口径
        if (!creative && machineData.contains(SerializationConstants.UPGRADES))
        {
            Map<Upgrade, Integer> needed = getNeededUpgrades(level, connectedMachines, perMachineUpgrade);
            if (!needed.isEmpty() && !hasEnoughUpgrades(itemSource, needed))
            {
                player.displayClientMessage(Component.translatable(
                                "message.godofthings.super_fusion.not_enough_upgrades")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
        }

        // 照抄上游：先 addUpgrades 记账，全部装完再统一扣料（顺序反了会出现「扣了料没装上」）
        Map<Upgrade, Integer> consumedUpgrades = new EnumMap<>(Upgrade.class);
        int affected = 0;
        for (BlockPos pos : connectedMachines)
        {
            if (!(level.getBlockEntity(pos) instanceof TileEntityMekanism machine))
            {
                continue;
            }
            boolean changed = false;
            TileComponentUpgrade comp = machine.getComponent();
            if (machineData.contains(SerializationConstants.UPGRADES) && comp != null)
            {
                for (Map.Entry<Upgrade, Integer> entry : perMachineUpgrade.entrySet())
                {
                    Upgrade upgrade = entry.getKey();
                    if (!comp.supports(upgrade))
                    {
                        continue;
                    }
                    int current = comp.getUpgrades(upgrade);
                    int toAdd = Math.min(entry.getValue() - current, upgrade.getMax() - current);
                    if (toAdd > 0)
                    {
                        int added = comp.addUpgrades(upgrade, toAdd);
                        if (added > 0)
                        {
                            changed = true;
                            if (!creative)
                            {
                                consumedUpgrades.merge(upgrade, added, Integer::sum);
                            }
                        }
                    }
                }
            }
            if (includeConfiguration && machineData.contains(KEY_CONFIG_DATA)
                    && machine instanceof IConfigCardAccess configAccess)
            {
                // 照抄上游两步：set 只写入，configurationDataSet 才让机器真正应用并保存配置
                configAccess.setConfigurationData(level.registryAccess(), player,
                        machineData.getCompound(KEY_CONFIG_DATA));
                configAccess.configurationDataSet();
                changed = true;
            }
            if (changed)
            {
                affected++;
            }
        }

        if (!creative && !consumedUpgrades.isEmpty())
        {
            for (Map.Entry<Upgrade, Integer> entry : consumedUpgrades.entrySet())
            {
                itemSource.consumeUpgrade(entry.getKey(), entry.getValue());
            }
        }

        if (affected > 0)
        {
            player.displayClientMessage(Component.translatable(
                            creative ? "message.godofthings.super_fusion.pasted_creative"
                                     : "message.godofthings.super_fusion.pasted",
                    affected)
                    .withStyle(ChatFormatting.GREEN), true);
        }
        else
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.paste_failed")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /** 各机器按档案还差多少升级（照抄上游 getNeededUpgrades，含上限裁剪） */
    private static Map<Upgrade, Integer> getNeededUpgrades(Level level, List<BlockPos> connectedMachines,
                                                           Map<Upgrade, Integer> perMachineUpgrade)
    {
        Map<Upgrade, Integer> needed = new EnumMap<>(Upgrade.class);
        for (BlockPos machinePos : connectedMachines)
        {
            if (!(level.getBlockEntity(machinePos) instanceof TileEntityMekanism machine))
            {
                continue;
            }
            TileComponentUpgrade comp = machine.getComponent();
            if (comp == null)
            {
                continue;
            }
            for (Map.Entry<Upgrade, Integer> entry : perMachineUpgrade.entrySet())
            {
                Upgrade upgrade = entry.getKey();
                if (!comp.supports(upgrade))
                {
                    continue;
                }
                int current = comp.getUpgrades(upgrade);
                int toAdd = Math.min(entry.getValue() - current, upgrade.getMax() - current);
                if (toAdd > 0)
                {
                    needed.merge(upgrade, toAdd, Integer::sum);
                }
            }
        }
        return needed;
    }

    private static boolean hasEnoughUpgrades(NetworkItemSource itemSource, Map<Upgrade, Integer> required)
    {
        for (Map.Entry<Upgrade, Integer> entry : required.entrySet())
        {
            if (!itemSource.hasUpgrade(entry.getKey(), entry.getValue()))
            {
                return false;
            }
        }
        return true;
    }

    /** 融合卡实现了 QIO 频率绑定（潜行右键 QIO 仪表盘），这里给出频率类型 */
    public static FrequencyType<?> getFrequencyType()
    {
        return FrequencyType.QIO;
    }
}
