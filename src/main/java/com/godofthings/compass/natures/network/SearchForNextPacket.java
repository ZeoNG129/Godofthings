package com.godofthings.compass.natures.network;

import com.godofthings.Godofthings;
import com.godofthings.compass.natures.items.NaturesCompassItem;
import com.godofthings.compass.natures.util.ItemUtils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：搜索同一个群系的下一个实例（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 */
public record SearchForNextPacket() implements CustomPacketPayload
{
    public static final Type<SearchForNextPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "natures_compass_search_for_next"));

    public static final StreamCodec<FriendlyByteBuf, SearchForNextPacket> CODEC =
            StreamCodec.ofMember(SearchForNextPacket::write, SearchForNextPacket::read);

    public static SearchForNextPacket read(FriendlyByteBuf buf)
    {
        return new SearchForNextPacket();
    }

    public void write(FriendlyByteBuf buf)
    {
    }

    public static void handle(SearchForNextPacket packet, IPayloadContext context)
    {
        if (context.flow().isServerbound())
        {
            context.enqueueWork(() ->
            {
                ItemStack stack = ItemUtils.getHeldNatureCompass(context.player());
                if (!stack.isEmpty())
                {
                    NaturesCompassItem natureCompass = (NaturesCompassItem) stack.getItem();
                    natureCompass.searchForNextBiome((ServerLevel) context.player().level(), context.player(),
                            context.player().blockPosition(), stack);
                }
            });
        }
    }

    @Override
    public Type<SearchForNextPacket> type()
    {
        return TYPE;
    }
}
