package com.godofthings.belt.network;

import com.godofthings.Godofthings;
import com.godofthings.belt.BeltRegistration;
import com.godofthings.belt.client.ClientPacketHandlers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeltContentsChange(
        int player,
        String where,
        int slot,
        ItemStack stack
) implements CustomPacketPayload
{
    public static final ResourceLocation ID = BeltRegistration.location("belt_contents_change");
    public static final Type<BeltContentsChange> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BeltContentsChange> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BeltContentsChange::player,
            ByteBufCodecs.STRING_UTF8, BeltContentsChange::where,
            ByteBufCodecs.VAR_INT, BeltContentsChange::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, BeltContentsChange::stack,
            BeltContentsChange::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public void handle(IPayloadContext context)
    {
        ClientPacketHandlers.handleBeltContentsChange(this);
    }
}
