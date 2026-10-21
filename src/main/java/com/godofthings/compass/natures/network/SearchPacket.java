package com.godofthings.compass.natures.network;

import com.godofthings.Godofthings;
import com.godofthings.compass.natures.items.NaturesCompassItem;
import com.godofthings.compass.natures.util.ItemUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：开始搜索某个群系（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>服务端只认「玩家手上真的拿着自然罗盘」：网络数据不可信，手上没有就整个丢掉。</p>
 */
public record SearchPacket(ResourceLocation biomeKey, BlockPos pos) implements CustomPacketPayload
{
    public static final Type<SearchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "natures_compass_search"));

    public static final StreamCodec<FriendlyByteBuf, SearchPacket> CODEC =
            StreamCodec.ofMember(SearchPacket::write, SearchPacket::read);

    public static SearchPacket read(FriendlyByteBuf buf)
    {
        return new SearchPacket(buf.readResourceLocation(), buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(biomeKey);
        buf.writeBlockPos(pos);
    }

    public static void handle(SearchPacket packet, IPayloadContext context)
    {
        if (context.flow().isServerbound())
        {
            context.enqueueWork(() ->
            {
                ItemStack stack = ItemUtils.getHeldNatureCompass(context.player());
                if (!stack.isEmpty())
                {
                    NaturesCompassItem natureCompass = (NaturesCompassItem) stack.getItem();
                    natureCompass.searchForBiome((ServerLevel) context.player().level(), context.player(),
                            packet.biomeKey, packet.pos, stack);
                }
            });
        }
    }

    @Override
    public Type<SearchPacket> type()
    {
        return TYPE;
    }
}
