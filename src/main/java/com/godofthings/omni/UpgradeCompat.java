package com.godofthings.omni;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 全能升级的模组适配器接口（原样移植自 Omni Upgrades 的 {@code IUpgradeCompat}）。
 *
 * <p>每个受支持模组提供一个实现：如何识别自己的升级卡、如何识别机器、
 * 如何按该模组自己的安装规则把插件塞进去。移植时按本机环境只保留 AE2
 * 适配器（它天然覆盖 AE2 与 AE2 Lightning Tech 的全部可升级机器）。</p>
 */
public interface UpgradeCompat {

    /** 手持物品是否是本模组族可批量安装的升级插件。 */
    boolean isUpgradeItem(ItemStack stack);

    /** 目标位置是否是本模组族的机器。 */
    boolean isMachine(Level level, BlockPos pos);

    /** 机器当前是否还能接受这个插件（未达上限）。 */
    boolean canMachineAcceptUpgrade(Level level, BlockPos pos, ItemStack upgrade);

    default boolean canMachineAcceptUpgrade(Level level, BlockPos pos, ItemStack upgrade, @Nullable Vec3 clickPos) {
        return this.canMachineAcceptUpgrade(level, pos, upgrade);
    }

    /** 往机器里装一个插件；成功时扣减 {@code upgrade} 并返回 true。 */
    boolean installUpgrade(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack upgrade);

    default boolean installUpgrade(Player player, InteractionHand hand, Level level, BlockPos pos,
            @Nullable Vec3 clickPos, ItemStack upgrade) {
        return this.installUpgrade(player, hand, level, pos, upgrade);
    }

    /** 高亮线框颜色（RGB 0-255），跟随各模组主题色。 */
    int[] getColor(ItemStack stack);

    Component getDisplayName(ItemStack stack);

    /** 多方块机器把部件坐标解析回主机坐标（如 Mekanism 的 bounding block）；默认原样返回。 */
    default BlockPos resolveMainPos(Level level, BlockPos pos) {
        return pos;
    }

    /** 找不到可安装目标时是否放行原版/其他模组的右键逻辑（AE2 部分机器需要）。 */
    default boolean passThroughOnNoTarget() {
        return false;
    }
}
