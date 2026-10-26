package com.godofthings.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * 用于「把手上方块当作伪装」的伪放置上下文。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.util.FakeUseContext}（MIT），逐行照抄：
 * 点到的位置直接取实际命中的方块位置，让被手持方块以为自己要放在电梯那一格，
 * 从而算出正确的朝向 / 半砖上下等状态。</p>
 */
public class FakeUseContext extends BlockPlaceContext
{
    public FakeUseContext(Player player, InteractionHand handIn, BlockHitResult hit)
    {
        super(new UseOnContext(player, handIn, hit));
    }

    @NotNull
    @Override
    public BlockPos getClickedPos()
    {
        return getHitResult().getBlockPos();
    }
}
