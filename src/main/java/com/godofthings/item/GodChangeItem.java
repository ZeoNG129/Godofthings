package com.godofthings.item;

import com.godofthings.menu.GodChangeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 神之更改：手持右键打开 GUI，可调整时间和天气。
 */
public class GodChangeItem extends Item
{
    public GodChangeItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            // NeoForge 1.21.1：NetworkHooks.openScreen → 玩家扩展 openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)
            serverPlayer.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("container.godofthings.god_change");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodChangeMenu(containerId);
                }
            }, buf -> {});
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
