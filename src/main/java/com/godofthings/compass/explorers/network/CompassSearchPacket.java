package com.godofthings.compass.explorers.network;

import java.util.ArrayList;
import java.util.List;

import com.godofthings.Godofthings;
import com.godofthings.compass.explorers.items.ExplorersCompassItem;
import com.godofthings.compass.explorers.util.ItemUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：开始搜索某个结构（或某个结构集里的全部结构）。
 *
 * <p>移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0。
 * 上游是 {@code SimpleChannel.registerMessage(i, CompassSearchPacket.class, ...)}；
 * 这里换成 NeoForge 1.21 的 {@link CustomPacketPayload} + {@link StreamCodec}，
 * 编解码顺序与原 {@code toBytes} / 构造器逐字段一致。</p>
 *
 * <p>服务端只认「玩家手上真的拿着探险者指南针」：网络数据不可信，手上没有就整个丢掉。</p>
 */
public record CompassSearchPacket(ResourceLocation groupKey, List<ResourceLocation> structureKeys, BlockPos pos)
        implements CustomPacketPayload
{
    public static final Type<CompassSearchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "explorers_compass_search"));

    public static final StreamCodec<FriendlyByteBuf, CompassSearchPacket> CODEC =
            StreamCodec.ofMember(CompassSearchPacket::write, CompassSearchPacket::read);

    public static CompassSearchPacket read(FriendlyByteBuf buf)
    {
        ResourceLocation groupKey = buf.readResourceLocation();
        int numStructures = buf.readInt();
        List<ResourceLocation> structureKeys = new ArrayList<>(Math.max(0, numStructures));
        for (int i = 0; i < numStructures; i++)
        {
            structureKeys.add(buf.readResourceLocation());
        }
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        return new CompassSearchPacket(groupKey, structureKeys, new BlockPos(x, y, z));
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(groupKey);
        buf.writeInt(structureKeys.size());
        for (ResourceLocation key : structureKeys)
        {
            buf.writeResourceLocation(key);
        }
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    public static void handle(CompassSearchPacket packet, IPayloadContext context)
    {
        if (context.flow().isServerbound())
        {
            context.enqueueWork(() ->
            {
                ItemStack stack = ItemUtils.getHeldExplorersCompass(context.player());
                if (!stack.isEmpty())
                {
                    ExplorersCompassItem compass = (ExplorersCompassItem) stack.getItem();
                    compass.searchForStructure(context.player().level(), context.player(), packet.groupKey(),
                            packet.structureKeys(), packet.pos(), stack);
                }
            });
        }
    }

    @Override
    public Type<CompassSearchPacket> type()
    {
        return TYPE;
    }
}
