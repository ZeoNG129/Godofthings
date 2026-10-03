package com.godofthings.beef.utils;

import com.godofthings.beef.api.enums.tool.ToolTypeMode;
import com.godofthings.beef.core.component.UComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;

/**
 * 「目标工具」判定与背包检索工具。
 *
 * <p><b>本次死代码清理</b>：本类原先还承载牛排工具框架的一大票掉落 / 状态功能
 * （战利品大爆发、觉醒粉、斩首、生物捕捉、自定义药水效果），它们的唯一调用方都在
 * 工具本体或工具事件处理器里，已随工具框架一并删除。</p>
 *
 * <p>保留下来的只有「目标工具」这一层：外部 mod <b>omnitools</b> 的扳手只要带上本模组的
 * {@code godofthings:tool_type = OMNITOOL_MODE} 数据组件，同样算目标工具 ——
 * 玩家保护（无敌 / 高级隐身）整层判定都建立在这上面，因此不能删。
 * 原来还有一个「物品是否继承 {@code EndlessBeafItem}」的分支，该基类已删除，分支永远为假，已移除。</p>
 */
public class UselessItemUtils {

    /**
     * 检查物品是否是目标工具（特定模式的 omnitools 扳手）。
     */
    private static boolean isTargetTool(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        // 检查是否是omnitools扳手且处于正确模式
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        return itemId.getNamespace().equals("omnitools")
                && itemStack.get(UComponents.CurrentToolTypeComponent) == ToolTypeMode.OMNITOOL_MODE;
    }

    /**
     * 从玩家的主手和副手中查找目标工具
     * 返回包含目标物品和对应手的Optional
     */
    public static Optional<SimpleImmutableEntry<ItemStack, InteractionHand>> findTargetToolInHands(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        // 检查主手
        if (isTargetTool(mainHandItem)) {
            return Optional.of(new SimpleImmutableEntry<>(mainHandItem, InteractionHand.MAIN_HAND));
        }

        // 检查副手
        if (isTargetTool(offHandItem)) {
            return Optional.of(new SimpleImmutableEntry<>(offHandItem, InteractionHand.OFF_HAND));
        }

        return Optional.empty();
    }

    public static boolean hasTargetToolInInventory(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }

        return isTargetTool(player.getMainHandItem())
                || isTargetTool(player.getOffhandItem())
                || player.getInventory().items.stream().anyMatch(UselessItemUtils::isTargetTool);
    }

    /**
     * 玩家身上是否带着指定物品（主手 / 副手 / 主背包）。
     * <p>
     * 注意 {@code player.getInventory().items} 只有主背包那 36 格，<b>不含副手</b>
     * （副手在 {@code getInventory().offhand} 里）。凡是「玩家是否携带」的判定都必须走这里，
     * 否则物品放进副手就会失效。
     */
    public static boolean hasItemInInventory(Player player, Item item) {
        if (player == null || player.getInventory() == null || item == null) {
            return false;
        }

        return player.getMainHandItem().is(item)
                || player.getOffhandItem().is(item)
                || player.getInventory().items.stream().anyMatch(stack -> stack.is(item));
    }

    public static boolean hasInvulnerabilityEnabledTargetToolInInventory(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }

        return isInvulnerabilityEnabledTargetTool(player.getMainHandItem())
                || isInvulnerabilityEnabledTargetTool(player.getOffhandItem())
                || player.getInventory().items.stream().anyMatch(UselessItemUtils::isInvulnerabilityEnabledTargetTool);
    }

    public static boolean hasAdvancedStealthEnabledTargetToolInInventory(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }

        return isAdvancedStealthEnabledTargetTool(player.getMainHandItem())
                || isAdvancedStealthEnabledTargetTool(player.getOffhandItem())
                || player.getInventory().items.stream().anyMatch(UselessItemUtils::isAdvancedStealthEnabledTargetTool);
    }

    public static boolean enableInvulnerabilityForAdvancedStealth(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }

        boolean changed = enableInvulnerabilityForAdvancedStealth(player.getMainHandItem());
        changed |= enableInvulnerabilityForAdvancedStealth(player.getOffhandItem());
        for (ItemStack itemStack : player.getInventory().items) {
            changed |= enableInvulnerabilityForAdvancedStealth(itemStack);
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    private static boolean isInvulnerabilityEnabledTargetTool(ItemStack itemStack) {
        if (!isTargetTool(itemStack)) {
            return false;
        }

        // 没有显式写组件时，omnitools 扳手按「未开启」处理（原实现在此回退为「是 EndlessBeafItem 即默认开启」，
        // 该基类已删除）。
        return itemStack.getOrDefault(UComponents.BeefInvulnerabilityEnabledComponent.get(), false);
    }

    private static boolean isAdvancedStealthEnabledTargetTool(ItemStack itemStack) {
        return isTargetTool(itemStack)
                && itemStack.getOrDefault(UComponents.BeefAdvancedStealthEnabledComponent.get(), false);
    }

    private static boolean enableInvulnerabilityForAdvancedStealth(ItemStack itemStack) {
        if (!isAdvancedStealthEnabledTargetTool(itemStack)
                || itemStack.getOrDefault(UComponents.BeefInvulnerabilityEnabledComponent.get(), false)) {
            return false;
        }
        itemStack.set(UComponents.BeefInvulnerabilityEnabledComponent.get(), true);
        return true;
    }
}
