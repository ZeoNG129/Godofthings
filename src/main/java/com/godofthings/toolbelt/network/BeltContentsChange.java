package com.godofthings.toolbelt.network;

import com.godofthings.toolbelt.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步某个实体穿戴的皮带（用于渲染）。
 */
public class BeltContentsChange
{
    public int player;
    public String where;
    public int slot;
    public ItemStack stack;

    public BeltContentsChange(LivingEntity player, String where, int slot, ItemStack stack)
    {
        this.player = player.getId();
        this.where = where;
        this.slot = slot;
        this.stack = stack.copy();
    }

    private BeltContentsChange()
    {
    }

    public static void encode(BeltContentsChange msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.player);
        buf.writeUtf(msg.where);
        buf.writeVarInt(msg.slot);
        buf.writeItem(msg.stack);
    }

    public static BeltContentsChange decode(FriendlyByteBuf buf)
    {
        BeltContentsChange msg = new BeltContentsChange();
        msg.player = buf.readVarInt();
        msg.where = buf.readUtf();
        msg.slot = buf.readVarInt();
        msg.stack = buf.readItem();
        return msg;
    }

    public static void handle(BeltContentsChange msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ClientPacketHandlers.handleBeltContentsChange(msg);
        ctx.setPacketHandled(true);
    }
}
