package com.godofthings.item;

import com.godofthings.menu.GodBlackBoxMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 神之黑盒：右键打开配置界面（开关 + 白名单过滤槽位）。
 * <p>
 * 开启后放在物品栏/背包中，拾取物品先入黑盒：白名单外物品销毁、白名单内物品无堆叠上限保留。
 */
public class GodBlackBoxItem extends Item
{
    public GodBlackBoxItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            // 黑盒所在物品栏槽：主手=当前选中快捷栏槽(0-8)，副手=-1
            int boxSlot = hand == InteractionHand.MAIN_HAND ? serverPlayer.getInventory().selected : -1;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new GodBlackBoxMenu(id, inv, boxSlot),
                    Component.translatable("gui.godofthings.black_box.title")),
                    buf -> buf.writeVarInt(boxSlot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || level.isClientSide)
        {
            return InteractionResult.PASS;
        }
        // 蹲下右键可存储容器：把黑盒内物品转移到目标容器（直到装满，剩余保留在黑盒）
        if (player.isShiftKeyDown())
        {
            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, context.getClickedPos(), context.getClickedFace());
            if (handler != null)
            {
                ItemStack box = context.getItemInHand();
                if (BlackBoxData.transferTo(box, handler, level.registryAccess()))
                {
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
        // 非蹲下右键容器：交给方块默认交互（如打开箱子）
        return InteractionResult.PASS;
    }
}
