package com.godofthings.superfusion;

import com.godofthings.superfusion.util.NetworkItemSource;
import mekanism.api.Upgrade;
import mekanism.common.inventory.slot.UpgradeInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 模块（升级）配置逻辑（移植自 Mekanism-Card 的 {@code MassUpgradeConfigurator}）。
 * <p>
 * 融合卡的「安装升级」模式：把卡里记录的升级档案同步到目标机器上。
 * 两种操作模式：
 * <ul>
 *   <li>{@link Mode#INSTALL}：按卡里的档案把机器升级数量补齐/削平（多退少补，退掉的卡进背包）；</li>
 *   <li>{@link Mode#CLEAR}：把机器上的升级全部卸下（物品退回玩家背包）。</li>
 * </ul>
 * 中键点击则是「装满」：把玩家身上/网络里有的升级按上限一次性装到机器上。
 */
public final class ModuleConfigLogic
{
    /** 操作模式 */
    public enum Mode
    {
        INSTALL("gui.godofthings.super_fusion.module.install", ChatFormatting.GREEN),
        CLEAR("gui.godofthings.super_fusion.module.clear", ChatFormatting.RED);

        public final String translationKey;
        public final ChatFormatting color;

        Mode(String translationKey, ChatFormatting color)
        {
            this.translationKey = translationKey;
            this.color = color;
        }

        public Component getDisplayName()
        {
            return Component.translatable(translationKey);
        }
    }

    private ModuleConfigLogic()
    {
    }

    /** 读当前操作模式（组件缺省为安装） */
    public static Mode getCurrentMode(ItemStack stack)
    {
        return stack.getOrDefault(SuperFusionComponents.MODULE_CLEAR_MODE.get(), false) ? Mode.CLEAR : Mode.INSTALL;
    }

    public static void toggleMode(ItemStack stack, Player player)
    {
        setMode(stack, player, getCurrentMode(stack) == Mode.INSTALL ? Mode.CLEAR : Mode.INSTALL);
    }

    public static void setMode(ItemStack stack, Player player, Mode mode)
    {
        stack.set(SuperFusionComponents.MODULE_CLEAR_MODE.get(), mode == Mode.CLEAR);
        player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.mode_switched",
                mode.getDisplayName()).withStyle(mode.color), true);
    }

    /** 按卡里的档案同步一批机器的升级 */
    public static void handlePositions(Level level, Collection<BlockPos> positions, Player player, ItemStack toolStack)
    {
        NetworkItemSource itemSource = NetworkItemSource.create(level, player, toolStack);
        Mode mode = getCurrentMode(toolStack);
        Map<Upgrade, Integer> storedLevels = Map.of();
        if (mode == Mode.INSTALL)
        {
            if (!MemoryCardLogic.hasData(toolStack))
            {
                player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_data")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            storedLevels = MemoryCardLogic.getStoredUpgradeLevels(toolStack);
            Map<Upgrade, Integer> required = getRequiredUpgrades(level, positions, storedLevels);
            if (!player.isCreative() && !hasRequiredUpgrades(itemSource, required))
            {
                player.displayClientMessage(Component.translatable(
                                "message.godofthings.super_fusion.not_enough_upgrades")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        int affectedMachines = 0;
        int totalAmount = 0;
        for (BlockPos pos : new LinkedHashSet<>(positions))
        {
            if (!level.hasChunkAt(pos))
            {
                continue;
            }
            TileComponentUpgrade comp = getUpgradeComponent(level, pos);
            if (comp == null)
            {
                continue;
            }
            int amount = mode == Mode.INSTALL
                    ? syncUpgradeProfile(comp, storedLevels, itemSource, player)
                    : clearAllUpgrades(comp, player);
            if (amount > 0)
            {
                affectedMachines++;
                totalAmount += amount;
            }
        }
        feedbackDetailed(player, mode, affectedMachines, totalAmount);
    }

    private static void feedbackDetailed(Player player, Mode mode, int affectedMachines, int totalAmount)
    {
        if (totalAmount > 0)
        {
            String actionKey = mode == Mode.INSTALL
                    ? "message.godofthings.super_fusion.sync_profile"
                    : "message.godofthings.super_fusion.clear_all";
            player.displayClientMessage(Component.translatable(actionKey, totalAmount, affectedMachines)
                    .withStyle(mode == Mode.INSTALL ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        }
        else
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.operation_none")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private static Map<Upgrade, Integer> getRequiredUpgrades(Level level, Collection<BlockPos> positions,
                                                             Map<Upgrade, Integer> storedLevels)
    {
        Map<Upgrade, Integer> required = new EnumMap<>(Upgrade.class);
        for (BlockPos pos : new LinkedHashSet<>(positions))
        {
            TileComponentUpgrade comp = getUpgradeComponent(level, pos);
            if (comp == null)
            {
                continue;
            }
            for (Upgrade upgrade : Upgrade.values())
            {
                if (!comp.supports(upgrade))
                {
                    continue;
                }
                int target = storedLevels.getOrDefault(upgrade, 0);
                int missing = Math.max(0, target - comp.getUpgrades(upgrade));
                if (missing > 0)
                {
                    required.merge(upgrade, missing, Integer::sum);
                }
            }
        }
        return required;
    }

    private static boolean hasRequiredUpgrades(NetworkItemSource itemSource, Map<Upgrade, Integer> required)
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

    /** 把机器的升级数量调整到档案值（超出部分卸下退回玩家） */
    private static int syncUpgradeProfile(TileComponentUpgrade comp, Map<Upgrade, Integer> storedLevels,
                                          NetworkItemSource itemSource, Player player)
    {
        int changed = 0;
        for (Upgrade upgrade : Upgrade.values())
        {
            if (!comp.supports(upgrade))
            {
                continue;
            }
            int target = storedLevels.getOrDefault(upgrade, 0);
            while (comp.getUpgrades(upgrade) > target)
            {
                int before = comp.getUpgrades(upgrade);
                comp.removeUpgrade(upgrade, false);
                int removed = before - comp.getUpgrades(upgrade);
                if (removed <= 0)
                {
                    break;
                }
                handleRemovedUpgrade(comp, player);
                changed += removed;
            }
            int missing = target - comp.getUpgrades(upgrade);
            if (missing > 0 && itemSource.consumeUpgrade(upgrade, missing))
            {
                changed += comp.addUpgrades(upgrade, missing);
            }
        }
        return changed;
    }

    /** 卸下机器上全部升级，退回玩家 */
    private static int clearAllUpgrades(TileComponentUpgrade comp, Player player)
    {
        int totalRemoved = 0;
        for (Upgrade upgradeType : Upgrade.values())
        {
            while (comp.getUpgrades(upgradeType) > 0)
            {
                int before = comp.getUpgrades(upgradeType);
                comp.removeUpgrade(upgradeType, false);
                int removed = before - comp.getUpgrades(upgradeType);
                if (removed <= 0)
                {
                    break;
                }
                handleRemovedUpgrade(comp, player);
                totalRemoved += removed;
            }
        }
        return totalRemoved;
    }

    /** 卸下的升级物品从输出槽挪到玩家背包（放不下就丢地上） */
    private static void handleRemovedUpgrade(TileComponentUpgrade comp, Player player)
    {
        UpgradeInventorySlot outputSlot = comp.getUpgradeOutputSlot();
        ItemStack stack = outputSlot.getStack();
        if (!stack.isEmpty())
        {
            if (!player.getInventory().add(stack))
            {
                player.drop(stack, false);
            }
            outputSlot.setStack(ItemStack.EMPTY);
        }
    }

    /** 中键「装满」：把能装的全装上（受玩家现有升级物品数量限制） */
    public static void handleMiddleClick(Level level, BlockPos pos, Player player, ItemStack toolStack)
    {
        handleMiddleClickPositions(level, List.of(pos), player, toolStack);
    }

    public static void handleMiddleClickPositions(Level level, Collection<BlockPos> positions, Player player,
                                                  ItemStack toolStack)
    {
        NetworkItemSource itemSource = NetworkItemSource.create(level, player, toolStack);
        int totalInstalled = 0;
        int totalMachines = 0;
        for (BlockPos machinePos : new LinkedHashSet<>(positions))
        {
            TileComponentUpgrade comp = getUpgradeComponent(level, machinePos);
            if (comp == null)
            {
                continue;
            }
            int machineInstalled = installAllUpgrades(comp, itemSource);
            if (machineInstalled > 0)
            {
                totalInstalled += machineInstalled;
                totalMachines++;
            }
        }
        if (totalInstalled > 0)
        {
            player.displayClientMessage(Component.translatable(
                            "message.godofthings.super_fusion.middle_click_all", totalInstalled, totalMachines)
                    .withStyle(ChatFormatting.GREEN), true);
        }
        else
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_upgrades")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private static int installAllUpgrades(TileComponentUpgrade comp, NetworkItemSource itemSource)
    {
        int totalInstalled = 0;
        for (Upgrade upgradeType : Upgrade.values())
        {
            if (!comp.supports(upgradeType))
            {
                continue;
            }
            int current = comp.getUpgrades(upgradeType);
            int max = upgradeType.getMax();
            if (current >= max)
            {
                continue;
            }
            int toInstall = Math.min(max - current, itemSource.countUpgrade(upgradeType));
            if (toInstall <= 0)
            {
                continue;
            }
            if (!itemSource.consumeUpgrade(upgradeType, toInstall))
            {
                continue;
            }
            totalInstalled += comp.addUpgrades(upgradeType, toInstall);
        }
        return totalInstalled;
    }

    @Nullable
    private static TileComponentUpgrade getUpgradeComponent(Level level, BlockPos pos)
    {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof TileEntityMekanism mekTile ? mekTile.getComponent() : null;
    }
}
