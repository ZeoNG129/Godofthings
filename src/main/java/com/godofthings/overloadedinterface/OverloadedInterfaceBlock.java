package com.godofthings.overloadedinterface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.core.definitions.AEBlocks;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;

/**
 * 过载接口方块（移植自 AE2LT 的 OverloadedInterfaceBlock）。
 *
 * <p>沿用 AE2 样板供应器的 PUSH_DIRECTION 方块状态：扳手右键可切换
 * ALL / OPPOSITE / 指定面等推拉方向，决定普通模式（非无线）下与哪一面交互。</p>
 */
public class OverloadedInterfaceBlock extends AEBaseEntityBlock<OverloadedInterfaceBlockEntity>
{
    public OverloadedInterfaceBlock(Properties properties)
    {
        super(properties.forceSolidOn());
        this.registerDefaultState(this.defaultBlockState()
                .setValue(PatternProviderBlock.PUSH_DIRECTION, PushDirection.ALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(PatternProviderBlock.PUSH_DIRECTION);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (InteractionUtil.canWrenchRotate(heldItem))
        {
            // 直接借用 AE2 样板供应器的 setSide 实现（all / opposite / 点击面 / 旋转 循环）
            AEBlocks.PATTERN_PROVIDER.block().setSide(level, pos, hit.getDirection());
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        // 非扳手：交回 AE2 基类处理（它自己会判右键开界面）
        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult)
    {
        var be = this.getBlockEntity(level, pos);
        if (be != null)
        {
            if (!level.isClientSide())
            {
                be.openMenu(player, MenuLocators.forBlockEntity(be));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
