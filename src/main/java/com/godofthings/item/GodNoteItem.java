package com.godofthings.item;

import com.godofthings.network.GodNoteMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 神之便签：记事本物品。
 *
 * <p>右键即打开记事本界面（与「N 键」「/godnote」三条入口等价）。内容是<b>玩家个人</b>的，
 * 存在存档里跟着玩家走 —— 物品只是个入口，把便签丢给别人不会把内容带走。</p>
 */
public class GodNoteItem extends Item
{
    public GodNoteItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            // 先下发最新内容，再让客户端开界面（顺序不能反，否则界面里可能还是旧数据）
            GodNoteMessages.sendSync(serverPlayer);
            GodNoteMessages.sendOpenScreen(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag)
    {
        tooltipComponents.add(Component.translatable("tooltip.godofthings.god_note.1")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.godofthings.god_note.2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
