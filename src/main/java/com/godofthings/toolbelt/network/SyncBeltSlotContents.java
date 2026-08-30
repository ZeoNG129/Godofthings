package com.godofthings.toolbelt.network;

import com.godofthings.toolbelt.client.ClientPacketHandlers;
import com.godofthings.toolbelt.customslots.IExtensionSlot;
import com.godofthings.toolbelt.slot.BeltExtensionSlot;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步某玩家自定义皮带栏的内容。
 */
public class SyncBeltSlotContents
{
    public final NonNullList<ItemStack> stacks = NonNullList.create();
    public int entityId;

    public SyncBeltSlotContents(Player player, BeltExtensionSlot extension)
    {
        this.entityId = player.getId();
        extension.getSlots().stream().map(IExtensionSlot::getContents).forEach(stacks::add);
    }

    private SyncBeltSlotContents()
    {
    }

    public static void encode(SyncBeltSlotContents msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.stacks.size());
        for (ItemStack stack : msg.stacks)
        {
            buf.writeItem(stack);
        }
    }

    public static SyncBeltSlotContents decode(FriendlyByteBuf buf)
    {
        SyncBeltSlotContents msg = new SyncBeltSlotContents();
        msg.entityId = buf.readVarInt();
        int numStacks = buf.readVarInt();
        for (int i = 0; i < numStacks; i++)
        {
            msg.stacks.add(buf.readItem());
        }
        return msg;
    }

    public static void handle(SyncBeltSlotContents msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ClientPacketHandlers.handleBeltSlotContents(msg);
        ctx.setPacketHandled(true);
    }
}
