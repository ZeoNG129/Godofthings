package com.godofthings.toolbelt.network;

import com.godofthings.toolbelt.BeltFinder;
import com.godofthings.toolbelt.ToolBeltConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求交换主手物品与皮带中的指定槽位（-1 表示塞进空槽）。
 */
public class SwapItems
{
    public int swapWith;

    public SwapItems(int windowId)
    {
        this.swapWith = windowId;
    }

    public static void encode(SwapItems msg, FriendlyByteBuf buf)
    {
        buf.writeInt(msg.swapWith);
    }

    public static SwapItems decode(FriendlyByteBuf buf)
    {
        return new SwapItems(buf.readInt());
    }

    public static void handle(SwapItems msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> swapItem(msg.swapWith, ctx.getSender()));
        ctx.setPacketHandled(true);
    }

    public static void swapItem(int swapWith, Player player)
    {
        if (player == null)
            return;

        BeltFinder.findBelt(player).ifPresent((getter) -> {
            ItemStack stack = getter.getBelt();
            if (stack.getCount() <= 0)
                return;

            ItemStack inHand = player.getMainHandItem();

            if (!ToolBeltConfig.isItemStackAllowed(inHand))
                return;

            IItemHandlerModifiable cap = (IItemHandlerModifiable) (
                    stack.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                            .orElseThrow(() -> new RuntimeException("No inventory!")));
            if (swapWith < 0)
            {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemHandlerHelper.insertItem(cap, inHand, false));
            }
            else
            {
                ItemStack inSlot = cap.getStackInSlot(swapWith);
                player.setItemInHand(InteractionHand.MAIN_HAND, inSlot);
                cap.setStackInSlot(swapWith, inHand);
            }
            getter.syncToClients();
        });
    }
}
