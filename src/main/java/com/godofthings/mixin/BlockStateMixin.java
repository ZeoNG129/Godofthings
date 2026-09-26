package com.godofthings.mixin;

import com.godofthings.armor.skill.ArmorSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 万物可掘（终极节点）：让基岩等原版不可破坏方块变得可挖。
 * <p>
 * <b>移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code BlockStateMixin}</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）。
 *
 * <h3>做法</h3>
 * 原版 {@code getDestroyProgress} 对不可破坏方块返回 0（永远挖不动）。
 * 这里在其返回值上注入：<b>返回 0 且玩家已开启「万物可掘」并手持镐子时</b>，
 * 改用<b>黑曜石的挖掘进度</b>（原版自己的算法）→ 客户端破坏动画与服务端破坏流程都能跑通。
 *
 * <h3>要点</h3>
 * <ul>
 *   <li>目标类必须是 {@link BlockBehaviour.BlockStateBase}——{@code getDestroyProgress} 定义在父类，
 *       {@code BlockState} 未重写，Mixin 到 BlockState 会找不到方法（{@code require = 0} 下静默失败，
 *       基岩进度永远是 0）。这是参考模组踩过的坑。</li>
 *   <li>正常可破坏方块（返回值 &gt; 0）直接返回，零干预。</li>
 *   <li>客户端与服务端两侧都会计算挖掘进度，故两侧都注入；技能状态两侧分别读
 *       （服务端读玩家存档，客户端读镜像）。</li>
 *   <li>{@code require = 0}：注入失败只降级（基岩挖不动），绝不崩游戏。</li>
 * </ul>
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateMixin
{
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true, require = 0)
    private void godofthings$breakUnbreakable(Player player, BlockGetter level, BlockPos pos,
                                             CallbackInfoReturnable<Float> cir)
    {
        if (cir.getReturnValueF() > 0.0F)
        {
            return; // 正常可破坏方块：不干预
        }
        if (player == null)
        {
            return;
        }
        // 技能开启（且穿齐全套）+ 手持镐子 → 按黑曜石速度挖掘
        if (!player.getMainHandItem().is(net.minecraft.tags.ItemTags.PICKAXES))
        {
            return;
        }
        boolean enabled = player.level().isClientSide
                ? com.godofthings.armor.skill.ArmorSkillData.clientEnabled(ArmorSkills.ULT_BREAK_ALL)
                : com.godofthings.handler.ArmorSkillHandler.isActive(player)
                        && com.godofthings.armor.skill.ArmorSkillData.isEnabled(player, ArmorSkills.ULT_BREAK_ALL);
        if (enabled)
        {
            cir.setReturnValue(Blocks.OBSIDIAN.defaultBlockState().getDestroyProgress(player, level, pos));
        }
    }
}
