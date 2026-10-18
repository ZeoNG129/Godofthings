package com.godofthings.belt.network;

import com.godofthings.belt.ConfigData;
import com.godofthings.Godofthings;
import com.godofthings.belt.BeltRegistration;
import com.godofthings.belt.common.Screens;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenBeltSlotInventory implements CustomPacketPayload
{
    public static final ResourceLocation ID = BeltRegistration.location("open_belt_slot_inventory");
    public static final Type<OpenBeltSlotInventory> TYPE = new Type<>(ID);
    public static final OpenBeltSlotInventory INSTANCE = new OpenBeltSlotInventory();
    public static final StreamCodec<ByteBuf, OpenBeltSlotInventory> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private OpenBeltSlotInventory()
    {
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public void handle(IPayloadContext context)
    {
        if (ConfigData.customBeltSlotEnabled)
        {
            context.enqueueWork(() -> Screens.openSlotScreen(context.player()));
        }
    }
}
