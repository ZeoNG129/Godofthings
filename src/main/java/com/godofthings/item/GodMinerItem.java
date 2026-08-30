package com.godofthings.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 神之矿机物品。
 * 继承 DiggerItem 使其属于工具类（挖掘类附魔分类），因此可以通过铁砧/附魔书
 * 直接附上原版的效率、时运、精准采集。右键可放置方块。
 */
public class GodMinerItem extends DiggerItem
{
    private final Block block;

    public GodMinerItem(Block block, Properties properties)
    {
        super(0.0F, 0.0F, Tiers.NETHERITE, BlockTags.MINEABLE_WITH_PICKAXE, properties);
        this.block = block;
    }

    public Block getBlock()
    {
        return block;
    }

    /** 附魔能力（决定铁砧消耗与可否附魔） */
    @Override
    public int getEnchantmentValue()
    {
        return 20;
    }

    /** 物品名称强制中文，不随游戏语言变化 */
    @Override
    public Component getName(ItemStack stack)
    {
        return Component.translatable("block.godofthings.god_miner");
    }

    // 右键放置方块（简化的方块放置逻辑）
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos target = clicked.canBeReplaced(placeContext) ? pos : pos.relative(context.getClickedFace());
        if (!level.getBlockState(target).canBeReplaced(placeContext))
        {
            return InteractionResult.FAIL;
        }

        BlockState placeState = block.getStateForPlacement(placeContext);
        if (placeState == null)
        {
            return InteractionResult.FAIL;
        }
        if (!level.setBlock(target, placeState, 11))
        {
            return InteractionResult.FAIL;
        }

        // 服务端放置时读取物品附魔（效率/时运/精准采集）写入方块实体
        if (!level.isClientSide)
        {
            block.setPlacedBy(level, target, placeState, player, stack);
        }

        SoundType sound = placeState.getSoundType();
        level.playSound(player, target, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (player != null && !player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
